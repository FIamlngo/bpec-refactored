package com.example.bpec.command;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import net.minecraft.screen.CraftingScreenHandler;
import net.minecraft.screen.SimpleNamedScreenHandlerFactory;
import net.minecraft.server.command.CommandManager;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;

public class CraftCommand {

    public static void register(CommandDispatcher<ServerCommandSource> dispatcher) {
        dispatcher.register(
            CommandManager.literal("craft")
                .executes(ctx -> openCraftingTable(ctx.getSource()))
        );
        dispatcher.register(
            CommandManager.literal("cr")
                .executes(ctx -> openCraftingTable(ctx.getSource()))
        );
    }

    private static int openCraftingTable(ServerCommandSource source) throws CommandSyntaxException {
        ServerPlayerEntity player = source.getPlayerOrThrow();

        player.openHandledScreen(new SimpleNamedScreenHandlerFactory(
            (syncId, playerInventory, p) ->
                new CraftingScreenHandler(syncId, playerInventory),
            Text.literal("Crafting Table")
        ));

        return 1;
    }
}