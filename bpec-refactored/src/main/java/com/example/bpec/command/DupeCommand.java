package com.example.bpec.command;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import net.minecraft.inventory.SimpleInventory;
import net.minecraft.item.ItemStack;
import net.minecraft.screen.GenericContainerScreenHandler;
import net.minecraft.screen.ScreenHandlerType;
import net.minecraft.screen.SimpleNamedScreenHandlerFactory;
import net.minecraft.server.command.CommandManager;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;

public class DupeCommand {

    public static void register(CommandDispatcher<ServerCommandSource> dispatcher) {
        // /dupe [multiplier]
        dispatcher.register(
            CommandManager.literal("dupe")
                .executes(ctx -> dupe(ctx.getSource(), 1))
                .then(CommandManager.argument("multiplier", IntegerArgumentType.integer(1, 64))
                    .executes(ctx -> dupe(ctx.getSource(), IntegerArgumentType.getInteger(ctx, "multiplier"))))
        );
        // /duplicate [multiplier]
        dispatcher.register(
            CommandManager.literal("duplicate")
                .executes(ctx -> dupe(ctx.getSource(), 1))
                .then(CommandManager.argument("multiplier", IntegerArgumentType.integer(1, 64))
                    .executes(ctx -> dupe(ctx.getSource(), IntegerArgumentType.getInteger(ctx, "multiplier"))))
        );
    }

    private static int dupe(ServerCommandSource source, int multiplier) throws CommandSyntaxException {
        ServerPlayerEntity player = source.getPlayerOrThrow();

        // Snapshot current inventory (9 hotbar + 27 main = 36 slots, we show all 36)
        SimpleInventory display = new SimpleInventory(36);
        for (int i = 0; i < 36; i++) {
            ItemStack stack = player.getInventory().getStack(i);
            display.setStack(i, stack.isEmpty() ? ItemStack.EMPTY : stack.copy());
        }

        player.openHandledScreen(new SimpleNamedScreenHandlerFactory(
            (syncId, playerInventory, p) ->
                new GenericContainerScreenHandler(ScreenHandlerType.GENERIC_9X4, syncId, playerInventory, display, 4),
            Text.literal("Duplicate Items (×" + multiplier + ")")
        ));

        // Perform duplication immediately — give extra copies into player inventory
        dupeInventory(player, multiplier);

        source.sendFeedback(
            () -> Text.literal("[Dupe] Duplicated inventory ×" + multiplier),
            false
        );

        return 1;
    }

    /**
     * For every non-empty stack in the player's main inventory (slots 0–35),
     * give additional (multiplier - 1) × stack copies. Each copy fills up to maxCount.
     * Leftover items that don't fit are dropped.
     */
    private static void dupeInventory(ServerPlayerEntity player, int multiplier) {
        if (multiplier <= 1) return;

        // Collect stacks to give (snapshot before modification)
        ItemStack[] original = new ItemStack[36];
        for (int i = 0; i < 36; i++) {
            original[i] = player.getInventory().getStack(i).copy();
        }

        int extra = multiplier - 1;
        for (ItemStack stack : original) {
            if (stack.isEmpty()) continue;
            for (int copy = 0; copy < extra; copy++) {
                ItemStack give = stack.copy();
                // insertItem returns leftovers; drop them if any
                if (!player.getInventory().insertStack(give)) {
                    player.dropItem(give, false);
                }
            }
        }
    }
}