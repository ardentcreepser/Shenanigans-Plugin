package com.shenanigans.util;

import net.kyori.adventure.text.Component;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.OfflinePlayer;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.inventory.meta.SkullMeta;
import org.bukkit.persistence.PersistentDataType;

import java.util.ArrayList;
import java.util.List;

/** Quick item building. Names and lore are MiniMessage strings. */
public final class Items {

    private Items() {
    }

    public static ItemStack make(Material material, String name, List<String> lore) {
        ItemStack item = new ItemStack(material);
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.customName(Text.item(name));
            meta.lore(toComponents(lore));
            item.setItemMeta(meta);
        }
        return item;
    }

    public static ItemStack make(Material material, String name, String... lore) {
        return make(material, name, List.of(lore));
    }

    public static ItemStack head(OfflinePlayer owner, String name, List<String> lore) {
        ItemStack item = make(Material.PLAYER_HEAD, name, lore);
        if (item.getItemMeta() instanceof SkullMeta skull) {
            skull.setOwningPlayer(owner);
            item.setItemMeta(skull);
        }
        return item;
    }

    public static ItemStack glow(ItemStack item, boolean glow) {
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.setEnchantmentGlintOverride(glow ? Boolean.TRUE : null);
            item.setItemMeta(meta);
        }
        return item;
    }

    public static ItemStack tag(ItemStack item, NamespacedKey key, String value) {
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.getPersistentDataContainer().set(key, PersistentDataType.STRING, value);
            item.setItemMeta(meta);
        }
        return item;
    }

    /** Read a tag we put on an item, or null. */
    public static String readTag(ItemStack item, NamespacedKey key) {
        if (item == null || item.isEmpty() || !item.hasItemMeta()) {
            return null;
        }
        return item.getItemMeta().getPersistentDataContainer().get(key, PersistentDataType.STRING);
    }

    private static List<Component> toComponents(List<String> lines) {
        List<Component> out = new ArrayList<>();
        for (String line : lines) {
            out.add(Text.item(line));
        }
        return out;
    }
}
