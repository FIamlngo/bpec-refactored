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

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

public class BackpackManager {

    private static final Map<UUID, SimpleInventory> inventories = new HashMap<>();
    private static final Map<UUID, Path> savePaths = new HashMap<>();
    private static final Map<UUID, RegistryWrapper.WrapperLookup> registryCache = new HashMap<>();
    private static final String DATA_FOLDER = "bpec_data";

    public static SimpleInventory getBackpack(ServerPlayerEntity player) {
        return inventories.computeIfAbsent(player.getUuid(), uuid -> new SimpleInventory(54));
    }

    public static void onPlayerJoin(ServerPlayerEntity player) {
        Path path = resolvePath(player);
        savePaths.put(player.getUuid(), path);
        registryCache.put(player.getUuid(), player.getRegistryManager()); // cache here

        SimpleInventory inv = new SimpleInventory(54);
        loadFromDisk(player.getUuid(), player.getRegistryManager(), inv);
        inventories.put(player.getUuid(), inv);
        BpEcMod.LOGGER.info("[BPEC] Joined: {} | items: {}", player.getName().getString(), countItems(inv));
    }

    public static void onPlayerLeave(ServerPlayerEntity player) {
        SimpleInventory inv = inventories.get(player.getUuid());
        RegistryWrapper.WrapperLookup registries = registryCache.get(player.getUuid());
        if (inv != null && registries != null) {
            BpEcMod.LOGGER.info("[BPEC] Leaving: {} | items: {}", player.getName().getString(), countItems(inv));
            saveToDisk(player.getUuid(), registries, inv);
            inventories.remove(player.getUuid());
            savePaths.remove(player.getUuid());
            registryCache.remove(player.getUuid());
        }
    }

    public static void savePlayer(ServerPlayerEntity player) {
        SimpleInventory inv = inventories.get(player.getUuid());
        RegistryWrapper.WrapperLookup registries = registryCache.get(player.getUuid());
        if (inv != null && registries != null) {
            BpEcMod.LOGGER.info("[BPEC] Periodic save: {} | items: {}", player.getName().getString(), countItems(inv));
            saveToDisk(player.getUuid(), registries, inv);
        }
    }

    private static Path resolvePath(ServerPlayerEntity player) {
        try {
            Path dataDir = player.getServerWorld().getServer()
                    .getSavePath(net.minecraft.util.WorldSavePath.ROOT)
                    .resolve(DATA_FOLDER);
            Files.createDirectories(dataDir);
            return dataDir.resolve(player.getUuid() + ".dat");
        } catch (Exception e) {
            BpEcMod.LOGGER.error("[BPEC] Failed to resolve path: {}", e.getMessage());
            return Path.of(DATA_FOLDER, player.getUuid() + ".dat");
        }
    }

    private static void saveToDisk(UUID uuid, RegistryWrapper.WrapperLookup registries, SimpleInventory inv) {
        Path path = savePaths.get(uuid);
        if (path == null) {
            BpEcMod.LOGGER.error("[BPEC] No cached path for {} — cannot save!", uuid);
            return;
        }
        try {
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
            BpEcMod.LOGGER.info("[BPEC] Saved {} items to {}", countItems(inv), path);
            } catch (Throwable e) {
                BpEcMod.LOGGER.error("[BPEC] Save failed for {}: {}", uuid, e.getMessage(), e);
            }
    }

    private static void loadFromDisk(UUID uuid, RegistryWrapper.WrapperLookup registries, SimpleInventory inv) {
        Path path = savePaths.get(uuid);
        if (path == null || !Files.exists(path)) {
            BpEcMod.LOGGER.info("[BPEC] No save file for {}", uuid);
            return;
        }
        try {
            NbtCompound root = NbtIo.readCompressed(path, NbtSizeTracker.ofUnlimitedBytes());
            if (!(root.get("Backpack") instanceof NbtList list)) return;
            for (int i = 0; i < list.size(); i++) {
                NbtCompound entry = list.getCompound(i);
                int slot = entry.getInt("Slot");
                if (slot >= 0 && slot < inv.size()) {
                    Optional<ItemStack> stack = ItemStack.fromNbt(registries, entry.getCompound("Item"));
                    inv.setStack(slot, stack.orElse(ItemStack.EMPTY));
                }
            }
            BpEcMod.LOGGER.info("[BPEC] Loaded {} items for {}", countItems(inv), uuid);
        } catch (Exception e) {
            BpEcMod.LOGGER.error("[BPEC] Load failed for {}: {}", uuid, e.getMessage(), e);
        }
    }

    private static int countItems(SimpleInventory inv) {
        int count = 0;
        for (int i = 0; i < inv.size(); i++)
            if (!inv.getStack(i).isEmpty()) count++;
        return count;
    }
}