package com.example.bpec.command;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import net.minecraft.screen.CraftingScreenHandler;
import net.minecraft.screen.ScreenHandlerContext;
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
        // ScreenHandlerContext.EMPTY bypasses the vanilla canUse() block check,
        // which was closing the screen instantly (same root cause as the anvil fix).
        // Items placed in the grid are still returned to inventory on close —
        // that logic lives in onClosed(), not in the context check.
        player.openHandledScreen(new SimpleNamedScreenHandlerFactory(
            (syncId, playerInventory, p) ->
                new CraftingScreenHandler(syncId, playerInventory,
                    ScreenHandlerContext.EMPTY),
            Text.literal("Crafting Table")
        ));

        return 1;
    }
}