package pl.autozmianka;

import net.fabricmc.fabric.api.client.screen.v1.Screens;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.screen.ingame.GenericContainerScreen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.gui.widget.ClickableWidget;
import net.minecraft.client.gui.widget.SliderWidget;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;

import java.util.List;
import java.util.function.IntConsumer;
import java.util.function.IntSupplier;

/** Panel ustawien doklejany do lewej strony menu serwera. */
public final class Overlay {
    private Overlay() {
    }

    private static final int WIDTH = 130;
    private static ButtonWidget startButton;

    public static void addTo(GenericContainerScreen screen, MinecraftClient client) {
        List<ClickableWidget> w = Screens.getButtons(screen);
        int x = 4;
        int y = 4;

        w.add(new ValueSlider(x, y, WIDTH, 20, "Cel", 0, 100, AzConfig.target, "%", v -> AzConfig.target = v));
        y += 22;
        w.add(new ValueSlider(x, y, WIDTH, 20, "Limit", 1, 500, AzConfig.maxRerolls, " zmianek",
                v -> AzConfig.maxRerolls = v));
        y += 22;
        w.add(new ValueSlider(x, y, WIDTH, 20, "Opoznienie", 4, 40, AzConfig.delayTicks, " tk",
                v -> AzConfig.delayTicks = v));
        y += 22;

        ButtonWidget modeButton = ButtonWidget.builder(modeText(), b -> {
            AzConfig.exact = !AzConfig.exact;
            b.setMessage(modeText());
        }).dimensions(x, y, WIDTH, 20).build();
        w.add(modeButton);
        y += 22;

        addSlotRow(w, x, y, "Przycisk", () -> AzConfig.buttonSlot, v -> AzConfig.buttonSlot = v);
        y += 22;
        addSlotRow(w, x, y, "Miecz", () -> AzConfig.resultSlot, v -> AzConfig.resultSlot = v);
        y += 22;

        w.add(ButtonWidget.builder(Text.literal("Skanuj menu"), b -> AutoRoller.scan(client))
                .dimensions(x, y, WIDTH, 20).build());
        y += 22;

        startButton = ButtonWidget.builder(startText(), b -> AutoRoller.toggle(client))
                .dimensions(x, y, WIDTH, 20).build();
        w.add(startButton);
        AutoRoller.setStatusCallback(() -> {
            if (startButton != null) startButton.setMessage(startText());
        });
    }

    private static Text modeText() {
        return Text.literal(AzConfig.exact ? "Tryb: dokladnie" : "Tryb: cel lub wiecej");
    }

    private static Text startText() {
        return AutoRoller.isRunning()
                ? Text.literal("STOP").formatted(Formatting.RED, Formatting.BOLD)
                : Text.literal("START").formatted(Formatting.GREEN, Formatting.BOLD);
    }

    private static void addSlotRow(List<ClickableWidget> w, int x, int y, String label,
                                   IntSupplier getter, IntConsumer setter) {
        ButtonWidget info = ButtonWidget.builder(Text.literal(label + ": slot " + getter.getAsInt()), b -> {
        }).dimensions(x + 22, y, WIDTH - 44, 20).build();

        w.add(ButtonWidget.builder(Text.literal("-"), b -> {
            setter.accept(Math.max(0, getter.getAsInt() - 1));
            info.setMessage(Text.literal(label + ": slot " + getter.getAsInt()));
        }).dimensions(x, y, 20, 20).build());

        w.add(info);

        w.add(ButtonWidget.builder(Text.literal("+"), b -> {
            setter.accept(Math.min(53, getter.getAsInt() + 1));
            info.setMessage(Text.literal(label + ": slot " + getter.getAsInt()));
        }).dimensions(x + WIDTH - 20, y, 20, 20).build());
    }

    /** Suwak liczbowy zwiazany z ustawieniem. */
    private static class ValueSlider extends SliderWidget {
        private final String label;
        private final int min;
        private final int max;
        private final String suffix;
        private final IntConsumer setter;

        ValueSlider(int x, int y, int width, int height, String label, int min, int max, int initial,
                    String suffix, IntConsumer setter) {
            super(x, y, width, height, Text.empty(), (initial - min) / (double) (max - min));
            this.label = label;
            this.min = min;
            this.max = max;
            this.suffix = suffix;
            this.setter = setter;
            updateMessage();
        }

        private int current() {
            return min + (int) Math.round(value * (max - min));
        }

        @Override
        protected void updateMessage() {
            if (label == null) return;
            setMessage(Text.literal(label + ": " + current() + suffix));
        }

        @Override
        protected void applyValue() {
            setter.accept(current());
        }
    }
}
