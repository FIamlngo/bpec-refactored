package com.example.bpec.command;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.inventory.CraftingInventory;
import net.minecraft.inventory.Inventory;
import net.minecraft.item.ItemStack;
import net.minecraft.recipe.CraftingRecipe;
import net.minecraft.recipe.RecipeEntry;
import net.minecraft.recipe.RecipeType;
import net.minecraft.recipe.ServerRecipeManager;
import net.minecraft.recipe.input.CraftingRecipeInput;
import net.minecraft.screen.CraftingScreenHandler;
import net.minecraft.screen.ScreenHandlerContext;
import net.minecraft.screen.SimpleNamedScreenHandlerFactory;
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

                    // onContentChanged fires server-side whenever any slot in the
                    // handler's tracked inventories changes — including the 3x3 grid.
                    // This is the correct hook point; updateResult is static in 1.21.x
                    // and cannot be overridden.
                    @Override
                    public void onContentChanged(Inventory inventory) {
                        super.onContentChanged(inventory);

                        // Only resolve recipes on the server
                        if (!(player instanceof ServerPlayerEntity sp)) return;

                        // Slot 0 is the result; slots 1-9 are the 3x3 grid.
                        // We need a CraftingInventory to build the recipe input.
                        // The handler exposes it via getSlot — cast the backing inv.
                        Inventory gridInv = getSlot(1).inventory;
                        if (!(gridInv instanceof CraftingInventory craftingInv)) return;

                        CraftingRecipeInput input = craftingInv.createRecipeInput();
                        Optional<RecipeEntry<CraftingRecipe>> match =
                            matchGetter.getFirstMatch(input, world);

                        if (match.isPresent()) {
                            ItemStack result = match.get().value()
                                .craft(input, sp.getRegistryManager());
                            getSlot(0).setStackNoCallbacks(result);
                        } else {
                            getSlot(0).setStackNoCallbacks(ItemStack.EMPTY);
                        }
                        sendContentUpdates();
                    }

                    @Override
                    public void onClosed(PlayerEntity pe) {
                        // Clear all slots BEFORE super so vanilla's dropInventory
                        // (which targets world pos 0,0,0 with EMPTY context) finds nothing.
                        // Slot 0 = result, slots 1-9 = 3x3 grid.
                        for (int i = 0; i <= 9; i++) {
                            ItemStack stack = getSlot(i).getStack();
                            if (!stack.isEmpty()) {
                                pe.getInventory().offerOrDrop(stack);
                                getSlot(i).setStackNoCallbacks(ItemStack.EMPTY);
                            }
                        }
                        super.onClosed(pe);
                    }
                },
            Text.literal("Crafting Table")
        ));

        return 1;
    }
}