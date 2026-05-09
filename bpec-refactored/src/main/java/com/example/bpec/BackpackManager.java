package com.example.bpec;

import net.fabricmc.fabric.api.attachment.v1.AttachmentRegistry;
import net.fabricmc.fabric.api.attachment.v1.AttachmentType;
import net.minecraft.inventory.SimpleInventory;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtList;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.util.Identifier;

import java.util.Optional;

public class BackpackManager {

    // Stores raw NBT with the player automatically — no file I/O needed
    public static final AttachmentType<NbtCompound> BACKPACK =
        AttachmentRegistry.<NbtCompound>builder()
            .persistent(NbtCompound.CODEC)
            .buildAndRegister(Identifier.of("bpec", "backpack"));

    public static SimpleInventory getBackpack(ServerPlayerEntity player) {
        NbtCompound tag = player.getAttached(BACKPACK);
        SimpleInventory inv = new SimpleInventory(54);
        if (tag != null) {
            deserialize(tag, inv, player);
        }
        return inv;
    }

    public static void saveBackpack(ServerPlayerEntity player, SimpleInventory inv) {
        player.setAttached(BACKPACK, serialize(inv, player));
    }

    private static NbtCompound serialize(SimpleInventory inv, ServerPlayerEntity player) {
        NbtList list = new NbtList();
        for (int slot = 0; slot < inv.size(); slot++) {
            ItemStack stack = inv.getStack(slot);
            if (!stack.isEmpty()) {
                NbtCompound entry = new NbtCompound();
                entry.putInt("Slot", slot);
                entry.put("Item", stack.encode(player.getRegistryManager()));
                list.add(entry);
            }
        }
        NbtCompound tag = new NbtCompound();
        tag.put("Items", list);
        return tag;
    }

    private static void deserialize(NbtCompound tag, SimpleInventory inv, ServerPlayerEntity player) {
        if (!(tag.get("Items") instanceof NbtList list)) return;
        for (int i = 0; i < list.size(); i++) {
            NbtCompound entry = list.getCompound(i);
            int slot = entry.getInt("Slot");
            if (slot >= 0 && slot < inv.size()) {
                Optional<ItemStack> stack = ItemStack.fromNbt(player.getRegistryManager(), entry.getCompound("Item"));
                inv.setStack(slot, stack.orElse(ItemStack.EMPTY));
            }
        }
    }
}