package com.example.bpec.command;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import net.minecraft.screen.AnvilScreenHandler;
import net.minecraft.screen.ScreenHandlerContext;
import net.minecraft.screen.SimpleNamedScreenHandlerFactory;
import net.minecraft.server.command.CommandManager;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;

public class AnvilCommand {

    public static void register(CommandDispatcher<ServerCommandSource> dispatcher) {
        dispatcher.register(
            CommandManager.literal("anvil")
                .executes(ctx -> openAnvil(ctx.getSource()))
        );
        dispatcher.register(
            CommandManager.literal("av")
                .executes(ctx -> openAnvil(ctx.getSource()))
        );
    }

    private static int openAnvil(ServerCommandSource source) throws CommandSyntaxException {
        ServerPlayerEntity player = source.getPlayerOrThrow();

        // ScreenHandlerContext.EMPTY prevents the vanilla "is there a real anvil block here?"
        // check from firing, which was causing the overlay to close instantly.
        player.openHandledScreen(new SimpleNamedScreenHandlerFactory(
            (syncId, playerInventory, p) ->
                new AnvilScreenHandler(syncId, playerInventory, ScreenHandlerContext.EMPTY),
            Text.literal("Anvil")
        ));

        return 1;
    }
}