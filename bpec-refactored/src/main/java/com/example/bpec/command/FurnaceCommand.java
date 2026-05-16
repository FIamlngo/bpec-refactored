package com.example.bpec.command;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import net.minecraft.inventory.SimpleInventory;
import net.minecraft.item.ItemStack;
import net.minecraft.recipe.RecipeEntry;
import net.minecraft.recipe.RecipeType;
import net.minecraft.recipe.ServerRecipeManager;
import net.minecraft.recipe.SmeltingRecipe;
import net.minecraft.recipe.input.SingleStackRecipeInput;
import net.minecraft.screen.FurnaceScreenHandler;
import net.minecraft.screen.PropertyDelegate;
import net.minecraft.screen.SimpleNamedScreenHandlerFactory;
import net.minecraft.screen.slot.SlotActionType;
import net.minecraft.server.command.CommandManager;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.text.Text;

import java.util.Optional;

public class FurnaceCommand {

    public static void register(CommandDispatcher<ServerCommandSource> dispatcher) {
        dispatcher.register(
            CommandManager.literal("furnace")
                .executes(ctx -> openFurnace(ctx.getSource()))
        );
        dispatcher.register(
            CommandManager.literal("fu")
                .executes(ctx -> openFurnace(ctx.getSource()))
        );
    }

    private static int openFurnace(ServerCommandSource source) throws CommandSyntaxException {
        ServerPlayerEntity player = source.getPlayerOrThrow();
        ServerWorld serverWorld = source.getWorld();

        // Build the match getter once; captured by the anonymous FurnaceScreenHandler subclass.
        ServerRecipeManager.MatchGetter<SingleStackRecipeInput, SmeltingRecipe> matchGetter =
            ServerRecipeManager.createCachedMatchGetter(RecipeType.SMELTING);

        SimpleInventory furnaceInv = new SimpleInventory(3);
        PropertyDelegate props = new PropertyDelegate() {
            private final int[] data = {200, 200, 200, 200}; // full fuel + full progress visually
            @Override public int get(int index) { return data[index]; }
            @Override public void set(int index, int value) { data[index] = value; }
            @Override public int size() { return 4; }
        };

        player.openHandledScreen(new SimpleNamedScreenHandlerFactory(
            (syncId, playerInventory, p) -> new FurnaceScreenHandler(syncId, playerInventory, furnaceInv, props) {

                /** Instantly smelt everything in slot 0 into slot 2. No fuel required. */
                private void tryInstantSmelt() {
                    ItemStack input = furnaceInv.getStack(0);
                    if (input.isEmpty()) return;

                    // MC 1.21.x: RecipeManager no longer has getFirstMatch(type, input, world/registry).
                    // Use ServerRecipeManager.createCachedMatchGetter which returns a MatchGetter whose
                    // getFirstMatch takes only (RecipeInput, ServerWorld).
                    Optional<RecipeEntry<SmeltingRecipe>> recipeOpt =
                        matchGetter.getFirstMatch(new SingleStackRecipeInput(input), serverWorld);

                    if (recipeOpt.isEmpty()) return;

                    ItemStack result = recipeOpt.get().value().craft(
                        new SingleStackRecipeInput(input), player.getRegistryManager());
                    if (result.isEmpty()) return;

                    ItemStack currentOutput = furnaceInv.getStack(2);

                    int maxSmelt = input.getCount();
                    if (!currentOutput.isEmpty()) {
                        if (!ItemStack.areItemsAndComponentsEqual(currentOutput, result)) return;
                        int room = currentOutput.getMaxCount() - currentOutput.getCount();
                        maxSmelt = Math.min(maxSmelt, room);
                    }
                    if (maxSmelt <= 0) return;

                    furnaceInv.removeStack(0, maxSmelt);
                    if (currentOutput.isEmpty()) {
                        ItemStack out = result.copy();
                        out.setCount(maxSmelt);
                        furnaceInv.setStack(2, out);
                    } else {
                        currentOutput.increment(maxSmelt);
                    }
                    furnaceInv.markDirty();
                }

                @Override
                public void onSlotClick(int slotIndex, int button, SlotActionType actionType,
                                        net.minecraft.entity.player.PlayerEntity pe) {
                    super.onSlotClick(slotIndex, button, actionType, pe);
                    tryInstantSmelt();
                }

                @Override
                public ItemStack quickMove(net.minecraft.entity.player.PlayerEntity pe, int index) {
                    ItemStack moved = super.quickMove(pe, index);
                    tryInstantSmelt();
                    return moved;
                }
            },
            Text.literal("Furnace")
        ));

        return 1;
    }
}