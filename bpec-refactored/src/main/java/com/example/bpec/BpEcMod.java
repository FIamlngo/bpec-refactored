package com.example.bpec;

import com.example.bpec.command.BpCommand;
import com.example.bpec.command.EcCommand;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class BpEcMod implements ModInitializer {

    public static final String MOD_ID = "bpec";
    public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

    @Override
    public void onInitialize() {
        // Load backpack from disk when a player joins
        ServerPlayConnectionEvents.JOIN.register((handler, sender, server) ->
                BackpackManager.onPlayerJoin(handler.player));

        // Save backpack to disk when a player disconnects
        ServerPlayConnectionEvents.DISCONNECT.register((handler, server) ->
                BackpackManager.onPlayerLeave(handler.player));

        // Register commands
        CommandRegistrationCallback.EVENT.register((dispatcher, registryAccess, environment) -> {
            BpCommand.register(dispatcher);
            EcCommand.register(dispatcher);
        });

        LOGGER.info("[BPEC] /bp and /ec commands registered.");
    }
}
