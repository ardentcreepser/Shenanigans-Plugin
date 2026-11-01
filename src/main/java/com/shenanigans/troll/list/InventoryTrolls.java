package com.shenanigans.troll.list;

import com.shenanigans.ShenanigansPlugin;
import com.shenanigans.troll.Category;
import com.shenanigans.troll.Troll;
import com.shenanigans.troll.TrollRegistry;
import com.shenanigans.util.Fx;
import com.shenanigans.util.Items;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.entity.Item;
import org.bukkit.entity.Player;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;
import org.bukkit.util.Vector;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;

/** Inventory trolls. Nothing here deletes items for real. */
public final class InventoryTrolls {

    private InventoryTrolls() {
    }

    public static final String HOT_POTATO_TAG = "hot_potato";

    public static void register(TrollRegistry r) {
        Category c = Category.INVENTORY;

        r.add(c, Troll.instant("butterfingers", "Butterfingers", Material.GOLD_INGOT,
                        "They drop whatever they're holding.")
                .does(ctx -> {
                    if (!dropHeldItem(ctx.target())) {
                        ctx.fail(ctx.target().getName() + " isn't holding anything");
                    }
                }));

        r.add(c, Troll.toggle("greasy", "Greasy Hands", Material.HONEYCOMB,
                        "Every now and then their held item just slips out of their hand.")
                .seconds(120)
                .every(20, at -> {
                    if (Fx.rng().nextInt(100) < 12) {
                        dropHeldItem(at.player());
                    }
                }));

        r.add(c, Troll.instant("shuffle", "Hotbar Shuffle", Material.BUNDLE,
                        "Shuffles their hotbar so nothing is where they left it. Use \"all\" to shuffle the whole inventory.")
                .usage("[hotbar|all]")
                .does(ctx -> {
                    PlayerInventory inv = ctx.target().getInventory();
                    int slots = ctx.arg(0, "hotbar").equalsIgnoreCase("all") ? 36 : 9;
                    List<ItemStack> items = new ArrayList<>();
                    for (int i = 0; i < slots; i++) {
                        items.add(inv.getItem(i));
                    }
                    Collections.shuffle(items);
                    for (int i = 0; i < slots; i++) {
                        inv.setItem(i, items.get(i));
                    }
                    inv.setHeldItemSlot(Fx.rng().nextInt(9));
                    Fx.sound(ctx.target(), Sound.ITEM_BUNDLE_DROP_CONTENTS, 1f, 1f);
                }));

        r.add(c, Troll.toggle("panic", "Inventory Panic", Material.LAVA_BUCKET,
                        "Their whole inventory vanishes! ...and comes back when it ends. Saved to disk first, so it's 100% safe.")
                .seconds(10)
                .onStart(at -> {
                    Player p = at.player();
                    ShenanigansPlugin plugin = at.plugin();
                    if (!plugin.vault().stash(p)) {
                        at.abort("Couldn't safely save their inventory (check console)");
                        return;
                    }
                    Fx.sound(p, Sound.ENTITY_ITEM_BREAK, 1f, 0.5f);
                    Fx.sound(p, Sound.BLOCK_LAVA_EXTINGUISH, 1f, 1f);
                })
                .onStop(at -> {
                    Player p = at.player();
                    if (p != null) {
                        at.plugin().vault().restore(p);
                        Fx.title(p, "<green>Phew!", "<gray>your items are back... this time", 5, 40, 10);
                        Fx.sound(p, Sound.ENTITY_PLAYER_LEVELUP, 1f, 1.4f);
                    }
                }));

        r.add(c, Troll.toggle("hotpotato", "Hot Potato", Material.BAKED_POTATO,
                        "Gives them a Hot Potato. They must DROP it before the timer ends or it blows up! (Harmless boom.)")
                .seconds(10)
                .onStart(at -> {
                    Player p = at.player();
                    ItemStack potato = Items.make(Material.BAKED_POTATO, "<gradient:red:gold><bold>HOT POTATO",
                            "<gray>Drop it before it explodes!", "<dark_gray>(press Q)");
                    Items.glow(potato, true);
                    Items.tag(potato, at.plugin().key(HOT_POTATO_TAG), "1");
                    Map<Integer, ItemStack> leftover = p.getInventory().addItem(potato);
                    if (!leftover.isEmpty()) {
                        at.abort(p.getName() + "'s inventory is full");
                        return;
                    }
                    Fx.sound(p, Sound.ENTITY_TNT_PRIMED, 1f, 1.4f);
                })
                .every(20, at -> {
                    Player p = at.player();
                    long left = (at.durationTicks() - at.age()) / 20;
                    if (left > 0) {
                        Fx.actionBar(p, "<gold>HOT POTATO! <red>Drop it! <white>" + left + "s");
                        Fx.sound(p, Sound.BLOCK_NOTE_BLOCK_HAT, 1f, 2f);
                    }
                })
                .onStop(at -> {
                    Player p = at.player();
                    if (p == null) {
                        return;
                    }
                    boolean hadIt = removeHotPotatoes(at.plugin(), p);
                    if (hadIt) {
                        Fx.fakeExplosion(p.getLocation());
                        at.plugin().manager().protectFromFall(p, 15);
                        p.setVelocity(new Vector(Fx.rng().nextDouble(-0.6, 0.6), 1.4, Fx.rng().nextDouble(-0.6, 0.6)));
                        Fx.title(p, "<red><bold>BOOM!", "<gray>should've dropped it", 0, 40, 10);
                    } else {
                        Fx.title(p, "<green>You survived!", "<gray>the potato has been defused", 5, 30, 10);
                    }
                }));

        r.add(c, Troll.instant("strip", "Wardrobe Malfunction", Material.LEATHER_CHESTPLATE,
                        "Their armor falls off into their inventory (or onto the floor if it's full).")
                .does(ctx -> {
                    Player p = ctx.target();
                    PlayerInventory inv = p.getInventory();
                    boolean any = false;
                    for (EquipmentSlot slot : Arrays.asList(EquipmentSlot.HEAD, EquipmentSlot.CHEST,
                            EquipmentSlot.LEGS, EquipmentSlot.FEET)) {
                        ItemStack piece = inv.getItem(slot);
                        if (piece.isEmpty()) {
                            continue;
                        }
                        any = true;
                        inv.setItem(slot, null);
                        for (ItemStack rest : inv.addItem(piece).values()) {
                            p.getWorld().dropItemNaturally(p.getLocation(), rest);
                        }
                    }
                    if (!any) {
                        ctx.fail(p.getName() + " isn't wearing any armor");
                        return;
                    }
                    Fx.sound(p, Sound.ITEM_ARMOR_EQUIP_LEATHER, 1f, 0.6f);
                }));
    }

    /** Drop the main-hand item in front of the player. */
    public static boolean dropHeldItem(Player p) {
        PlayerInventory inv = p.getInventory();
        ItemStack held = inv.getItemInMainHand();
        if (held.isEmpty()) {
            return false;
        }
        inv.setItemInMainHand(null);
        Item drop = p.getWorld().dropItem(p.getEyeLocation(), held);
        drop.setVelocity(p.getLocation().getDirection().multiply(0.35));
        drop.setPickupDelay(30);
        Fx.sound(p, Sound.ENTITY_ITEM_PICKUP, 1f, 0.5f);
        p.getWorld().spawnParticle(Particle.FALLING_HONEY, p.getEyeLocation(), 5, 0.2, 0.2, 0.2);
        return true;
    }

    /** Remove all hot potatoes from a player's inventory. Returns true if they had one. */
    public static boolean removeHotPotatoes(ShenanigansPlugin plugin, Player p) {
        boolean found = false;
        PlayerInventory inv = p.getInventory();
        for (int i = 0; i < inv.getSize(); i++) {
            ItemStack item = inv.getItem(i);
            if (Items.readTag(item, plugin.key(HOT_POTATO_TAG)) != null) {
                inv.setItem(i, null);
                found = true;
            }
        }
        if (Items.readTag(p.getItemOnCursor(), plugin.key(HOT_POTATO_TAG)) != null) {
            p.setItemOnCursor(null);
            found = true;
        }
        return found;
    }
}
