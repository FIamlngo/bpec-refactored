package com.example.bpec.command;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import net.minecraft.screen.AnvilScreenHandler;
import net.minecraft.screen.SimpleNamedScreenHandlerFactory;
import net.minecraft.server.command.CommandManager;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.text.Text;
import net.minecraft.util.math.BlockPos;

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
        ServerWorld world = source.getWorld();
        BlockPos pos = player.getBlockPos();

        player.openHandledScreen(new SimpleNamedScreenHandlerFactory(
            (syncId, playerInventory, p) ->
                new AnvilScreenHandler(syncId, playerInventory,
                    net.minecraft.screen.ScreenHandlerContext.create(world, pos)),
            Text.literal("Anvil")
        ));

        return 1;
    }
}