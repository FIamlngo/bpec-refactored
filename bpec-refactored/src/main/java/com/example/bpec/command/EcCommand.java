package com.example.bpec.command;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import net.minecraft.screen.GenericContainerScreenHandler;
import net.minecraft.screen.SimpleNamedScreenHandlerFactory;
import net.minecraft.server.command.CommandManager;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;

public class EcCommand {

    public static void register(CommandDispatcher<ServerCommandSource> dispatcher) {
        dispatcher.register(
            CommandManager.literal("ec")
                .executes(ctx -> openEnderChest(ctx.getSource()))
        );
    }

    private static int openEnderChest(ServerCommandSource source) throws CommandSyntaxException {
        ServerPlayerEntity player = source.getPlayerOrThrow();

        player.openHandledScreen(new SimpleNamedScreenHandlerFactory(
            (syncId, playerInventory, p) ->
                GenericContainerScreenHandler.createGeneric9x3(
                    syncId,
                    playerInventory,
                    player.getEnderChestInventory()
                ),
            Text.literal("Ender Chest")
        ));

        return 1;
    }
}
