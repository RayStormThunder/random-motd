package net.raystormthunder.randommotd;

import net.fabricmc.api.DedicatedServerModInitializer;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.minecraft.server.MinecraftServer;

public class ServerInitializer implements DedicatedServerModInitializer {

    @Override
    public void onInitializeServer() {
        System.out.println("Random MOTD Mod: Server Initializer Loaded");

        Main main = new Main();

        // Use the server lifecycle event to retrieve the server instance
        ServerLifecycleEvents.SERVER_STARTING.register(server -> {
            main.setServerInstance(server);
            main.onInitialize();
        });
    }
}
