package pl.autozmianka;

import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.message.v1.ClientReceiveMessageEvents;
import net.fabricmc.fabric.api.client.screen.v1.ScreenEvents;
import net.minecraft.client.gui.screen.ingame.GenericContainerScreen;

public class AutoZmiankaClient implements ClientModInitializer {

    @Override
    public void onInitializeClient() {
        ClientTickEvents.END_CLIENT_TICK.register(AutoRoller::tick);

        ClientReceiveMessageEvents.GAME.register((message, overlay) -> {
            if (!overlay) {
                AutoRoller.onChat(message.getString());
            }
        });

        // Panel pojawia sie w kazdym menu typu skrzynia (tez w menu serwera).
        ScreenEvents.AFTER_INIT.register((client, screen, scaledWidth, scaledHeight) -> {
            if (screen instanceof GenericContainerScreen containerScreen) {
                Overlay.addTo(containerScreen, client);
            }
        });
    }
}
