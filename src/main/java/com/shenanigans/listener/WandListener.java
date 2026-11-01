package com.shenanigans.listener;

import com.shenanigans.ShenanigansPlugin;
import com.shenanigans.troll.Troll;
import com.shenanigans.util.Fx;
import com.shenanigans.util.Items;
import com.shenanigans.util.Text;
import org.bukkit.FluidCollisionMode;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.event.Event;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerInteractEntityEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;
import org.bukkit.util.RayTraceResult;
import org.bukkit.util.Vector;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * The Troll Wand: point at a player (even far away) and right-click to troll them.
 * Sneak + right-click = next troll, sneak + left-click = previous troll.
 */
public final class WandListener implements Listener {

    private static final String WAND_TAG = "wand";

    private final ShenanigansPlugin plugin;
    private final Map<UUID, Long> cooldowns = new HashMap<>();

    public WandListener(ShenanigansPlugin plugin) {
        this.plugin = plugin;
    }

    public static ItemStack createWand(ShenanigansPlugin plugin, Troll troll) {
        ItemStack wand = Items.make(Material.BLAZE_ROD,
                "<gradient:#ff5f6d:#ffc371><bold>Troll Wand</bold></gradient> <gray>(<yellow>" + troll.name() + "</yellow>)",
                "<gray>Point at a player and right-click",
                "<gray>to use <yellow>" + troll.name() + "</yellow> on them.",
                "<dark_gray>Works from up to 60 blocks away.",
                "",
                "<gray>Sneak + right-click: <white>next troll",
                "<gray>Sneak + left-click: <white>previous troll");
        Items.glow(wand, true);
        Items.tag(wand, plugin.key(WAND_TAG), troll.id());
        return wand;
    }

    @EventHandler(priority = EventPriority.HIGH)
    public void onInteract(PlayerInteractEvent event) {
        if (event.getHand() != EquipmentSlot.HAND) {
            return;
        }
        ItemStack item = event.getItem();
        String trollId = Items.readTag(item, plugin.key(WAND_TAG));
        if (trollId == null) {
            return;
        }
        event.setUseInteractedBlock(Event.Result.DENY);
        event.setUseItemInHand(Event.Result.DENY);

        Player player = event.getPlayer();
        if (!player.hasPermission("shenanigans.wand")) {
            Text.send(player, "no-permission");
            return;
        }

        Action action = event.getAction();
        if (action == Action.PHYSICAL) {
            return;
        }

        long now = System.currentTimeMillis();
        Long last = cooldowns.get(player.getUniqueId());
        if (last != null && now - last < 300) {
            return;
        }
        cooldowns.put(player.getUniqueId(), now);

        Troll current = plugin.registry().get(trollId);
        if (player.isSneaking()) {
            cycle(player, current, action.isRightClick() ? 1 : -1);
            return;
        }
        if (!action.isRightClick()) {
            return;
        }
        if (current == null) {
            Text.send(player, "unknown-troll", Text.ph("troll", trollId));
            return;
        }

        Location eye = player.getEyeLocation();
        Vector direction = eye.getDirection();
        RayTraceResult hit = player.getWorld().rayTrace(eye, direction, 60, FluidCollisionMode.NEVER, true, 0.5,
                entity -> entity instanceof Player other && !other.equals(player));
        if (hit == null || !(hit.getHitEntity() instanceof Player target)) {
            Fx.actionBar(player, "<red>No player in sight!");
            Fx.sound(player, Sound.BLOCK_DISPENSER_FAIL, 0.6f, 1.4f);
            return;
        }

        // A little magic beam so it looks cool
        Location to = target.getLocation().add(0, 1, 0);
        double distance = eye.distance(to);
        Vector step = to.toVector().subtract(eye.toVector()).normalize().multiply(0.5);
        Location point = eye.clone();
        for (double d = 0; d < distance; d += 0.5) {
            point.add(step);
            player.spawnParticle(Particle.WITCH, point, 1, 0, 0, 0, 0);
        }
        Fx.sound(player, Sound.ENTITY_EVOKER_CAST_SPELL, 0.6f, 1.5f);

        plugin.manager().apply(player, target, current, new String[0]);
    }

    @EventHandler(priority = EventPriority.HIGH)
    public void onInteractEntity(PlayerInteractEntityEvent event) {
        ItemStack item = event.getPlayer().getInventory().getItem(event.getHand());
        if (Items.readTag(item, plugin.key(WAND_TAG)) != null) {
            event.setCancelled(true); // the PlayerInteractEvent above does the work
        }
    }

    private void cycle(Player player, Troll current, int direction) {
        List<Troll> options = new ArrayList<>();
        for (Troll troll : plugin.registry().all()) {
            if (!plugin.isTrollDisabled(troll) && plugin.manager().canUse(player, troll)) {
                options.add(troll);
            }
        }
        if (options.isEmpty()) {
            return;
        }
        int index = current == null ? -1 : options.indexOf(current);
        int next = Math.floorMod(index + direction, options.size());
        Troll chosen = options.get(next);
        player.getInventory().setItemInMainHand(createWand(plugin, chosen));
        Fx.actionBar(player, "<gray>Wand: <yellow>" + chosen.name() + " <dark_gray>(" + chosen.category().displayName() + ")");
        Fx.sound(player, Sound.UI_BUTTON_CLICK, 0.6f, 1.6f);
    }
}
