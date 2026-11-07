package pl.autozmianka;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.screen.ingame.GenericContainerScreen;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.component.type.LoreComponent;
import net.minecraft.item.ItemStack;
import net.minecraft.screen.GenericContainerScreenHandler;
import net.minecraft.screen.slot.SlotActionType;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/** Petla auto-losowania: klika przycisk w menu, czyta wynik, powtarza az trafi cel. */
public final class AutoRoller {
    private AutoRoller() {
    }

    private static final Pattern PERCENT = Pattern.compile("(\\d{1,3}(?:[.,]\\d+)?)\\s*%");

    private static boolean running;
    private static boolean waitingResult;
    private static int used;
    private static int waitTicks;
    private static int chatPercent = -1;
    private static int unreadable;
    private static Runnable statusCallback;

    public static boolean isRunning() {
        return running;
    }

    public static void setStatusCallback(Runnable callback) {
        statusCallback = callback;
    }

    private static void notifyStatus() {
        if (statusCallback != null) statusCallback.run();
    }

    public static void say(MinecraftClient c, String msg, Formatting color) {
        if (c.player != null) {
            c.player.sendMessage(Text.literal("[AutoZmianka] " + msg).formatted(color), false);
        }
    }

    private static GenericContainerScreenHandler handlerOf(MinecraftClient c) {
        if (c.currentScreen instanceof GenericContainerScreen screen) {
            return screen.getScreenHandler();
        }
        return null;
    }

    public static boolean matches(int value) {
        return AzConfig.exact ? value == AzConfig.target : value >= AzConfig.target;
    }

    /** Wyciaga procent z nazwy i opisu przedmiotu. Zwraca -1, gdy nie ma. */
    public static int readPercent(ItemStack stack) {
        if (stack.isEmpty()) return -1;
        LoreComponent lore = stack.get(DataComponentTypes.LORE);
        if (lore != null) {
            for (Text line : lore.lines()) {
                int v = parse(line.getString());
                if (v >= 0) return v;
            }
        }
        return parse(stack.getName().getString());
    }

    public static int parse(String text) {
        Matcher m = PERCENT.matcher(text);
        if (m.find()) {
            return (int) Math.round(Double.parseDouble(m.group(1).replace(',', '.')));
        }
        return -1;
    }

    public static void onChat(String message) {
        if (running && waitingResult) {
            int v = parse(message);
            if (v >= 0) chatPercent = v;
        }
    }

    public static void toggle(MinecraftClient c) {
        if (running) {
            stop(c, "Zatrzymano.", Formatting.YELLOW);
        } else {
            start(c);
        }
    }

    public static void start(MinecraftClient c) {
        GenericContainerScreenHandler h = handlerOf(c);
        if (h == null || c.player == null) return;

        int size = h.getRows() * 9;
        if (AzConfig.buttonSlot >= size || AzConfig.resultSlot >= size) {
            say(c, "Numer slotu poza menu (to menu ma sloty 0-" + (size - 1) + ").", Formatting.RED);
            return;
        }

        int current = readPercent(h.getSlot(AzConfig.resultSlot).getStack());
        if (current >= 0 && matches(current)) {
            say(c, "Miecz juz ma " + current + "%.", Formatting.GREEN);
            return;
        }

        used = 0;
        waitTicks = 0;
        waitingResult = false;
        chatPercent = -1;
        unreadable = 0;
        running = true;
        notifyStatus();
        say(c, "Start: cel " + (AzConfig.exact ? "" : ">= ") + AzConfig.target + "%, limit "
                + AzConfig.maxRerolls + " zmianek.", Formatting.AQUA);
    }

    public static void stop(MinecraftClient c, String reason, Formatting color) {
        boolean wasRunning = running;
        running = false;
        waitingResult = false;
        notifyStatus();
        if (wasRunning) say(c, reason, color);
    }

    public static void tick(MinecraftClient c) {
        if (!running) return;

        GenericContainerScreenHandler h = handlerOf(c);
        if (h == null || c.player == null || c.interactionManager == null) {
            stop(c, "Menu zamkniete, zatrzymano.", Formatting.YELLOW);
            return;
        }

        if (waitTicks > 0) {
            waitTicks--;
            return;
        }

        if (waitingResult) {
            waitingResult = false;
            int v = readPercent(h.getSlot(AzConfig.resultSlot).getStack());
            if (v < 0) v = chatPercent;

            if (v >= 0) {
                unreadable = 0;
                if (matches(v)) {
                    stop(c, "Trafiono " + v + "% po " + used + " zmiankach!", Formatting.GREEN);
                    return;
                }
            } else {
                unreadable++;
                if (unreadable >= 3) {
                    stop(c, "Nie moge odczytac procentu. Kliknij Skanuj i ustaw wlasciwy slot wyniku.",
                            Formatting.RED);
                    return;
                }
            }

            c.player.sendMessage(Text.literal("Zmianka " + used + "/" + AzConfig.maxRerolls + ": "
                    + (v >= 0 ? v + "%" : "?")).formatted(Formatting.GRAY), true);

            if (used >= AzConfig.maxRerolls) {
                stop(c, "Zuzyto limit " + used + " zmianek. Ostatni wynik: " + (v >= 0 ? v + "%" : "?"),
                        Formatting.YELLOW);
                return;
            }
        }

        chatPercent = -1;
        c.interactionManager.clickSlot(h.syncId, AzConfig.buttonSlot, 0, SlotActionType.PICKUP, c.player);
        used++;
        waitingResult = true;
        waitTicks = Math.max(4, AzConfig.delayTicks);
    }

    /** Wypisuje na czacie zawartosc menu, zeby latwo znalezc numery slotow. */
    public static void scan(MinecraftClient c) {
        GenericContainerScreenHandler h = handlerOf(c);
        if (h == null || c.player == null) return;

        int size = h.getRows() * 9;
        say(c, "Skan menu (sloty 0-" + (size - 1) + "):", Formatting.AQUA);
        for (int i = 0; i < size; i++) {
            ItemStack stack = h.getSlot(i).getStack();
            if (stack.isEmpty()) continue;

            StringBuilder sb = new StringBuilder("[" + i + "] " + stack.getName().getString());
            LoreComponent lore = stack.get(DataComponentTypes.LORE);
            if (lore != null) {
                int shown = 0;
                for (Text line : lore.lines()) {
                    String s = line.getString().trim();
                    if (s.isEmpty()) continue;
                    sb.append(shown == 0 ? " | " : " / ").append(s);
                    if (++shown >= 3) break;
                }
            }
            c.player.sendMessage(Text.literal(sb.toString()).formatted(Formatting.GRAY), false);
        }
    }
}
