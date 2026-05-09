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

public class BackpackManager {

    private static final Map<UUID, SimpleInventory> inventories = new HashMap<>();
    private static final String DATA_FOLDER = "bpec_data";

    public static SimpleInventory getBackpack(ServerPlayerEntity player) {
        return inventories.computeIfAbsent(player.getUuid(), uuid -> new SimpleInventory(54));
    }

    public static void onPlayerJoin(ServerPlayerEntity player) {
        SimpleInventory inv = new SimpleInventory(54);
        loadFromDisk(player, inv);
        inventories.put(player.getUuid(), inv);
        BpEcMod.LOGGER.info("[BPEC] Loaded backpack for {} ({} items)", player.getName().getString(), countItems(inv));
    }

    public static void onPlayerLeave(ServerPlayerEntity player) {
        SimpleInventory inv = inventories.get(player.getUuid());
        if (inv != null) {
            BpEcMod.LOGGER.info("[BPEC] Saving on leave for {} ({} items)", player.getName().getString(), countItems(inv));
            saveToDisk(player, inv);
            inventories.remove(player.getUuid());
        } else {
            BpEcMod.LOGGER.warn("[BPEC] onPlayerLeave: no inventory found for {}", player.getName().getString());
        }
    }

    public static void savePlayer(ServerPlayerEntity player) {
        SimpleInventory inv = inventories.get(player.getUuid());
        if (inv != null) {
            BpEcMod.LOGGER.info("[BPEC] Periodic save for {} ({} items)", player.getName().getString(), countItems(inv));
            saveToDisk(player, inv);
        } else {
            BpEcMod.LOGGER.warn("[BPEC] savePlayer: no inventory found for {}", player.getName().getString());
        }
    }

    private static int countItems(SimpleInventory inv) {
        int count = 0;
        for (int i = 0; i < inv.size(); i++) {
            if (!inv.getStack(i).isEmpty()) count++;
        }
        return count;
    }

    private static Path getDataPath(ServerPlayerEntity player) {
        Path dataDir;
        try {
            dataDir = player.getServerWorld().getServer()
                    .getSavePath(net.minecraft.util.WorldSavePath.ROOT)
                    .resolve(DATA_FOLDER);
            BpEcMod.LOGGER.info("[BPEC] Data path: {}", dataDir);
            Files.createDirectories(dataDir);
        } catch (Exception e) {
            BpEcMod.LOGGER.error("[BPEC] Could not create data directory: {}", e.getMessage());
            dataDir = Path.of(DATA_FOLDER);
        }
        return dataDir.resolve(player.getUuid() + ".dat");
    }

    private static void saveToDisk(ServerPlayerEntity player, SimpleInventory inv) {
        try {
            Path path = getDataPath(player);
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
            NbtIo.writeCompressed(root, path);
            BpEcMod.LOGGER.info("[BPEC] Saved {} to {}", player.getName().getString(), path);
        } catch (Exception e) {
            BpEcMod.LOGGER.error("[BPEC] Failed to save backpack for {}: {}", player.getName().getString(), e.getMessage(), e);
        }
    }

    private static void loadFromDisk(ServerPlayerEntity player, SimpleInventory inv) {
        try {
            Path path = getDataPath(player);
            BpEcMod.LOGGER.info("[BPEC] Looking for save at: {}", path);
            if (!Files.exists(path)) {
                BpEcMod.LOGGER.info("[BPEC] No save file found for {}", player.getName().getString());
                return;
            }
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
            BpEcMod.LOGGER.info("[BPEC] Loaded {} items for {}", countItems(inv), player.getName().getString());
        } catch (Exception e) {
            BpEcMod.LOGGER.error("[BPEC] Failed to load backpack for {}: {}", player.getName().getString(), e.getMessage(), e);
        }
    }
}