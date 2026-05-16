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
        // /dupe          → give 1 extra copy (end up with 2 total)
        // /dupe <amount> → give <amount> extra copies
        dispatcher.register(
            CommandManager.literal("dupe")
                .executes(ctx -> dupe(ctx.getSource(), 1))
                .then(CommandManager.argument("amount", IntegerArgumentType.integer(1, 64))
                    .executes(ctx -> dupe(ctx.getSource(), IntegerArgumentType.getInteger(ctx, "amount"))))
        );
        dispatcher.register(
            CommandManager.literal("duplicate")
                .executes(ctx -> dupe(ctx.getSource(), 1))
                .then(CommandManager.argument("amount", IntegerArgumentType.integer(1, 64))
                    .executes(ctx -> dupe(ctx.getSource(), IntegerArgumentType.getInteger(ctx, "amount"))))
        );
    }

    private static int dupe(ServerCommandSource source, int amount) throws CommandSyntaxException {
        ServerPlayerEntity player = source.getPlayerOrThrow();

        ItemStack held = player.getMainHandStack();
        if (held.isEmpty()) {
            source.sendFeedback(() -> Text.literal("[Dupe] You are not holding anything."), false);
            return 0;
        }

        // Give exactly <amount> extra copies of the held item.
        for (int i = 0; i < amount; i++) {
            ItemStack copy = held.copy();
            if (!player.getInventory().insertStack(copy)) {
                player.dropItem(copy, false);
            }
        }

        final String name = held.getName().getString();
        source.sendFeedback(() -> Text.literal("[Dupe] +" + amount + " " + name), false);
        return 1;
    }
}