package com.example.bpec;

import net.minecraft.inventory.SimpleInventory;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtElement;
import net.minecraft.nbt.NbtIo;
import net.minecraft.nbt.NbtList;
import net.minecraft.nbt.NbtSizeTracker;
import net.minecraft.registry.RegistryWrapper;
import net.minecraft.server.network.ServerPlayerEntity;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

/**
 * Manages per-player backpack inventories.
 *
 * Data is stored in: <world>/bpec_data/<uuid>.dat
 * Each player gets a 54-slot inventory loaded on join, saved on disconnect/save.
 */
public class BackpackManager {

    // In-memory store: player UUID -> their 54-slot backpack
    private static final Map<UUID, SimpleInventory> inventories = new HashMap<>();

    // Folder name inside the world save directory
    private static final String DATA_FOLDER = "bpec_data";

    // -------------------------------------------------------------------------
    // Public API used by BpCommand
    // -------------------------------------------------------------------------

    public static SimpleInventory getBackpack(ServerPlayerEntity player) {
        return inventories.computeIfAbsent(player.getUuid(), uuid -> new SimpleInventory(54));
    }

    // -------------------------------------------------------------------------
    // Called when a player joins the world
    // -------------------------------------------------------------------------

    public static void onPlayerJoin(ServerPlayerEntity player) {
        SimpleInventory inv = new SimpleInventory(54);
        loadFromDisk(player, inv);
        inventories.put(player.getUuid(), inv);
    }

    // -------------------------------------------------------------------------
    // Called when a player disconnects (or the world saves)
    // -------------------------------------------------------------------------

    public static void onPlayerLeave(ServerPlayerEntity player) {
        SimpleInventory inv = inventories.get(player.getUuid());
        if (inv != null) {
            saveToDisk(player, inv);
            inventories.remove(player.getUuid());
        }
    }

    public static void savePlayer(ServerPlayerEntity player) {
    SimpleInventory inv = inventories.get(player.getUuid());
    if (inv != null) {
        saveToDisk(player, inv);
    }
}

    // -------------------------------------------------------------------------
    // Disk I/O
    // -------------------------------------------------------------------------

private static Path getDataPath(ServerPlayerEntity player) {
    Path dataDir = player.getServerWorld().getServer()
            .getSavePath(net.minecraft.util.WorldSavePath.ROOT)
            .resolve(DATA_FOLDER);
    try {
        Files.createDirectories(dataDir);
    } catch (IOException e) {
        BpEcMod.LOGGER.error("[BPEC] Could not create data directory: {}", e.getMessage());
    }
    return dataDir.resolve(player.getUuid() + ".dat");
}

    private static void saveToDisk(ServerPlayerEntity player, SimpleInventory inv) {
        try {
            RegistryWrapper.WrapperLookup registries = player.getRegistryManager();
            NbtList list = new NbtList();
            for (int slot = 0; slot < inv.size(); slot++) {
                ItemStack stack = inv.getStack(slot);
                if (!stack.isEmpty()) {
                    NbtCompound entry = new NbtCompound();
                    entry.putInt("Slot", slot);
                    entry.put("Item", stack.encode(registries));
                    list.add(entry);
                }
            }
            NbtCompound root = new NbtCompound();
            root.put("Backpack", list);
            NbtIo.writeCompressed(root, getDataPath(player));
        } catch (Exception e) {
            BpEcMod.LOGGER.error("[BPEC] Failed to save backpack for {}: {}", player.getUuid(), e.getMessage());
        }
    }

    private static void loadFromDisk(ServerPlayerEntity player, SimpleInventory inv) {
        Path path = getDataPath(player);
        if (!Files.exists(path)) return;
        try {
            RegistryWrapper.WrapperLookup registries = player.getRegistryManager();
            NbtCompound root = NbtIo.readCompressed(path, NbtSizeTracker.ofUnlimitedBytes());
            if (!root.contains("Backpack", NbtElement.LIST_TYPE)) return;
            NbtList list = root.getList("Backpack", NbtElement.COMPOUND_TYPE);
            for (int i = 0; i < list.size(); i++) {
                NbtCompound entry = list.getCompound(i);
                int slot = entry.getInt("Slot");
                if (slot >= 0 && slot < inv.size()) {
                    Optional<ItemStack> stack = ItemStack.fromNbt(registries, entry.getCompound("Item"));
                    inv.setStack(slot, stack.orElse(ItemStack.EMPTY));
                }
            }
        } catch (Exception e) {
            BpEcMod.LOGGER.error("[BPEC] Failed to load backpack for {}: {}", player.getUuid(), e.getMessage());
        }
    }
}
