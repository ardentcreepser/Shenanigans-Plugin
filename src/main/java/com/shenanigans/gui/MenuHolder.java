package com.shenanigans.gui;

import org.bukkit.entity.Player;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemStack;

import java.util.HashMap;
import java.util.Map;

/** Marks an inventory as one of our menus and remembers what each slot does when clicked. */
public final class MenuHolder implements InventoryHolder {

    @FunctionalInterface
    public interface ClickAction {
        void click(Player player, ClickType type);
    }

    private final Map<Integer, ClickAction> actions = new HashMap<>();
    private Inventory inventory;
    private Runnable refresher;

    void setInventory(Inventory inventory) {
        this.inventory = inventory;
    }

    @Override
    public Inventory getInventory() {
        return inventory;
    }

    public void set(int slot, ItemStack item, ClickAction action) {
        inventory.setItem(slot, item);
        if (action == null) {
            actions.remove(slot);
        } else {
            actions.put(slot, action);
        }
    }

    public void clear() {
        inventory.clear();
        actions.clear();
    }

    public void click(Player player, int slot, ClickType type) {
        ClickAction action = actions.get(slot);
        if (action != null) {
            action.click(player, type);
        }
    }

    void setRefresher(Runnable refresher) {
        this.refresher = refresher;
    }

    /** Redraw the menu (e.g. after a troll was toggled). */
    public void refresh() {
        if (refresher != null) {
            refresher.run();
        }
    }
}
