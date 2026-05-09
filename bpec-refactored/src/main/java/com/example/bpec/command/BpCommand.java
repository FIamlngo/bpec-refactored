package com.example.bpec.command;

import com.example.bpec.BackpackManager;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import net.minecraft.inventory.SimpleInventory;
import net.minecraft.screen.GenericContainerScreenHandler;
import net.minecraft.screen.ScreenHandlerType;
import net.minecraft.screen.SimpleNamedScreenHandlerFactory;
import net.minecraft.server.command.CommandManager;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;

/**
 * /bp           — opens backpack with default 3 rows (27 slots)
 * /bp <rows>    — opens backpack with 1–6 rows (9–54 slots)
 *
 * The underlying inventory is always 54 slots. Changing the row count just
 * changes how many slots are visible — items outside the visible area are
 * safe and can be accessed by opening with more rows.
 *
 * No item required in inventory. Works anywhere (creative, survival, etc.).
 */
public class BpCommand {

    public static void register(CommandDispatcher<ServerCommandSource> dispatcher) {
        dispatcher.register(
            CommandManager.literal("bp")
                // /bp  (defaults to 3 rows)
                .executes(ctx -> openBackpack(ctx.getSource(), 3))
                // /bp <1-6>
                .then(
                    CommandManager.argument("rows", IntegerArgumentType.integer(1, 6))
                        .executes(ctx -> openBackpack(
                            ctx.getSource(),
                            IntegerArgumentType.getInteger(ctx, "rows")
                        ))
                )
        );
    }

    private static int openBackpack(ServerCommandSource source, int rows) throws CommandSyntaxException {
        ServerPlayerEntity player = source.getPlayerOrThrow();
        SimpleInventory backpack = BackpackManager.getBackpack(player);

        // Pick the matching vanilla screen handler type for the requested row count
        ScreenHandlerType<GenericContainerScreenHandler> type = switch (rows) {
            case 1  -> ScreenHandlerType.GENERIC_9X1;
            case 2  -> ScreenHandlerType.GENERIC_9X2;
            case 3  -> ScreenHandlerType.GENERIC_9X3;
            case 4  -> ScreenHandlerType.GENERIC_9X4;
            case 5  -> ScreenHandlerType.GENERIC_9X5;
            default -> ScreenHandlerType.GENERIC_9X6;
        };

        player.openHandledScreen(new SimpleNamedScreenHandlerFactory(
            (syncId, playerInventory, p) ->
                new GenericContainerScreenHandler(type, syncId, playerInventory, backpack, rows),
            Text.literal("Backpack")
        ));

        return 1;
    }
}
