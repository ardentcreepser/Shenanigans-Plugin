package com.shenanigans.util;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.data.BlockData;
import org.bukkit.plugin.Plugin;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

/**
 * Places blocks that go away by themselves (cages, cobwebs...).
 * It only ever replaces air or replaceable stuff like grass/water, so it never destroys builds,
 * and it puts the original block back afterwards.
 */
public final class TempBlocks {

    private record Entry(BlockData original, Material placed) {
    }

    private final Plugin plugin;
    private final Map<Location, Entry> placed = new HashMap<>();

    public TempBlocks(Plugin plugin) {
        this.plugin = plugin;
    }

    /** @return true if the block was placed */
    public boolean place(Block block, BlockData data, long ticks) {
        if (!(block.isEmpty() || block.isReplaceable())) {
            return false;
        }
        Location key = block.getLocation();
        if (placed.containsKey(key)) {
            return false;
        }
        placed.put(key, new Entry(block.getBlockData(), data.getMaterial()));
        block.setBlockData(data, false);
        Bukkit.getScheduler().runTaskLater(plugin, () -> restore(key), ticks);
        return true;
    }

    public boolean isTemporary(Block block) {
        return placed.containsKey(block.getLocation());
    }

    private void restore(Location key) {
        Entry entry = placed.remove(key);
        if (entry == null) {
            return;
        }
        Block block = key.getBlock();
        // Only undo it if our block is still there (or got broken). Never overwrite something a player built.
        if (block.getType() == entry.placed() || block.isEmpty()) {
            block.setBlockData(entry.original(), false);
        }
    }

    public void restoreAll() {
        for (Location key : new ArrayList<>(placed.keySet())) {
            restore(key);
        }
    }
}
