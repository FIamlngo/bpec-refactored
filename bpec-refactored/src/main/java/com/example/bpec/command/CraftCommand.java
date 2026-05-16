package com.example.bpec.command;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.inventory.CraftingInventory;
import net.minecraft.inventory.CraftingResultInventory;
import net.minecraft.inventory.SimpleInventory;
import net.minecraft.item.ItemStack;
import net.minecraft.recipe.CraftingRecipe;
import net.minecraft.recipe.RecipeEntry;
import net.minecraft.recipe.RecipeType;
import net.minecraft.recipe.ServerRecipeManager;
import net.minecraft.recipe.input.CraftingRecipeInput;
import net.minecraft.screen.CraftingScreenHandler;
import net.minecraft.screen.ScreenHandlerContext;
import net.minecraft.screen.SimpleNamedScreenHandlerFactory;
import net.minecraft.screen.slot.SlotActionType;
import net.minecraft.server.command.CommandManager;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.text.Text;

import java.util.Optional;

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
        ServerWorld world = source.getWorld();

        ServerRecipeManager.MatchGetter<CraftingRecipeInput, CraftingRecipe> matchGetter =
            ServerRecipeManager.createCachedMatchGetter(RecipeType.CRAFTING);

        player.openHandledScreen(new SimpleNamedScreenHandlerFactory(
            (syncId, playerInventory, p) ->
                new CraftingScreenHandler(syncId, playerInventory, ScreenHandlerContext.EMPTY) {

                    /** Resolve whatever is in the 3x3 grid and put the result in slot 0 (output). */
                    private void refreshResult(ServerPlayerEntity sp) {
                        // The crafting grid is slots 1-9 in a 3x3 CraftingScreenHandler.
                        // Build a CraftingInventory view from those slots.
                        CraftingInventory grid = new CraftingInventory(this, 3, 3);
                        for (int i = 0; i < 9; i++) {
                            grid.setStack(i, this.slots.get(i + 1).getStack());
                        }

                        CraftingRecipeInput input = grid.createRecipeInput();
                        Optional<RecipeEntry<CraftingRecipe>> match =
                            matchGetter.getFirstMatch(input, world);

                        ItemStack result = match.map(e ->
                            e.value().craft(input, sp.getRegistryManager())
                        ).orElse(ItemStack.EMPTY);

                        // Slot 0 is the output slot.
                        this.slots.get(0).setStack(result);
                        this.sendContentUpdates();
                    }

                    @Override
                    public void onSlotClick(int slotIndex, int button,
                                            SlotActionType actionType, PlayerEntity pe) {
                        super.onSlotClick(slotIndex, button, actionType, pe);
                        if (pe instanceof ServerPlayerEntity sp) refreshResult(sp);
                    }

                    @Override
                    public ItemStack quickMove(PlayerEntity pe, int index) {
                        ItemStack moved = super.quickMove(pe, index);
                        if (pe instanceof ServerPlayerEntity sp) refreshResult(sp);
                        return moved;
                    }
                },
            Text.literal("Crafting Table")
        ));

        return 1;
    }
}