package com.example.bpec.command;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
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

        player.openHandledScreen(new SimpleNamedScreenHandlerFactory(
            (syncId, playerInventory, p) ->
                new AnvilScreenHandler(syncId, playerInventory, ScreenHandlerContext.EMPTY) {
                    @Override
                    public void onClosed(PlayerEntity pe) {
                        // Drain all 3 slots (input L, input R, output) into the player
                        // BEFORE calling super. With EMPTY context, super's dropInventory()
                        // would try to drop items at world pos (0,0,0) — clearing first
                        // ensures it finds empty slots and does nothing harmful.
                        for (int i = 0; i < 3; i++) {
                            ItemStack stack = getSlot(i).getStack();
                            if (!stack.isEmpty()) {
                                pe.getInventory().offerOrDrop(stack);
                                getSlot(i).setStackNoCallbacks(ItemStack.EMPTY);
                            }
                        }
                        super.onClosed(pe);
                    }
                },
            Text.literal("Anvil")
        ));

        return 1;
    }
}