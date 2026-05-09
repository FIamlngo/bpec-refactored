package com.example.bpec.command;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import net.minecraft.screen.GenericContainerScreenHandler;
import net.minecraft.screen.SimpleNamedScreenHandlerFactory;
import net.minecraft.server.command.CommandManager;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;

/**
 * /ec — opens the player's ender chest inventory directly.
 *
 * Uses the real ender chest inventory so changes are reflected in actual
 * ender chests placed in the world (and vice versa).
 * No ender chest block required. Works anywhere.
 */
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
                    player.getEnderChestInventory()   // real ender chest — syncs with blocks
                ),
            Text.literal("Ender Chest")
        ));

        return 1;
    }
}
