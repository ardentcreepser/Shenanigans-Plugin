package com.shenanigans.util;

import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.Plugin;

import java.io.File;
import java.io.IOException;
import java.util.Base64;
import java.util.Map;
import java.util.logging.Level;

/**
 * Safely hides a player's inventory for the "Inventory Panic" troll.
 * Items are saved to plugins/Shenanigans/vault/&lt;uuid&gt;.yml BEFORE the inventory is cleared,
 * so nothing is ever lost - not even if the server crashes. They're given back on stop or on next join.
 */
public final class InventoryVault {

    private final Plugin plugin;
    private final File folder;

    public InventoryVault(Plugin plugin) {
        this.plugin = plugin;
        this.folder = new File(plugin.getDataFolder(), "vault");
    }

    private File file(Player player) {
        return new File(folder, player.getUniqueId() + ".yml");
    }

    public boolean hasStash(Player player) {
        return file(player).exists();
    }

    /** Save and clear the inventory. Returns false (and does nothing) if saving failed. */
    public boolean stash(Player player) {
        File file = file(player);
        if (file.exists()) {
            return false;
        }
        if (!folder.exists() && !folder.mkdirs()) {
            plugin.getLogger().warning("Could not create vault folder");
            return false;
        }
        ItemStack[] contents = player.getInventory().getContents();
        YamlConfiguration yaml = new YamlConfiguration();
        yaml.set("player", player.getName());
        yaml.set("items", Base64.getEncoder().encodeToString(ItemStack.serializeItemsAsBytes(contents)));
        try {
            yaml.save(file);
        } catch (IOException ex) {
            plugin.getLogger().log(Level.WARNING, "Could not save inventory of " + player.getName(), ex);
            return false;
        }
        player.getInventory().clear();
        return true;
    }

    /** Give the saved items back. Anything they picked up in the meantime is kept too. */
    public void restore(Player player) {
        File file = file(player);
        if (!file.exists()) {
            return;
        }
        YamlConfiguration yaml = YamlConfiguration.loadConfiguration(file);
        String data = yaml.getString("items");
        if (data == null) {
            plugin.getLogger().warning("Vault file for " + player.getName() + " is empty, leaving it alone: " + file.getName());
            return;
        }
        ItemStack[] saved;
        try {
            saved = ItemStack.deserializeItemsFromBytes(Base64.getDecoder().decode(data));
        } catch (Exception ex) {
            plugin.getLogger().log(Level.SEVERE, "Could not read vault file for " + player.getName()
                    + " - it was NOT deleted: " + file.getName(), ex);
            return;
        }

        ItemStack[] pickedUp = player.getInventory().getContents();
        player.getInventory().setContents(saved);
        for (ItemStack item : pickedUp) {
            if (item == null || item.isEmpty()) {
                continue;
            }
            Map<Integer, ItemStack> leftover = player.getInventory().addItem(item);
            for (ItemStack rest : leftover.values()) {
                player.getWorld().dropItemNaturally(player.getLocation(), rest);
            }
        }
        if (!file.delete()) {
            plugin.getLogger().warning("Could not delete vault file " + file.getName());
        }
    }
}
