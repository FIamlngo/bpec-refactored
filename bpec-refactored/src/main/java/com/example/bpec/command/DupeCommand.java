package com.example.bpec.command;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import net.minecraft.item.ItemStack;
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

        // Only dupe the item held in the main hand — no GUI overlay opened.
        ItemStack held = player.getMainHandStack();
        if (held.isEmpty()) {
            source.sendFeedback(
                () -> Text.literal("[Dupe] You are not holding anything."),
                false
            );
            return 0;
        }

        // Give (multiplier) extra copies of the held stack.
        for (int i = 0; i < multiplier; i++) {
            ItemStack copy = held.copy();
            if (!player.getInventory().insertStack(copy)) {
                // Inventory full — drop the remainder at the player's feet.
                player.dropItem(copy, false);
            }
        }

        final String itemName = held.getName().getString();
        source.sendFeedback(
            () -> Text.literal("[Dupe] Duplicated " + itemName + " ×" + multiplier),
            false
        );

        return 1;
    }
}