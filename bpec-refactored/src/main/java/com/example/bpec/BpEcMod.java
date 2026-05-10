package com.example.bpec;

import com.example.bpec.command.BpCommand;
import com.example.bpec.command.EcCommand;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.fabricmc.fabric.api.attachment.v1.AttachmentType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class BpEcMod implements ModInitializer {

    public static final String MOD_ID = "bpec";
    public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

    @Override
    public void onInitialize() {
        AttachmentType<?> ignored = BackpackManager.BACKPACK;

        CommandRegistrationCallback.EVENT.register((dispatcher, registryAccess, environment) -> {
            BpCommand.register(dispatcher);
            EcCommand.register(dispatcher);
        });
        LOGGER.info("[BPEC] /bp and /ec commands registered.");
    }
}