package com.shenanigans.listener;

import com.shenanigans.ShenanigansPlugin;
import com.shenanigans.troll.TrollManager;
import com.shenanigans.troll.list.ChatTrolls;
import com.shenanigans.troll.list.InventoryTrolls;
import com.shenanigans.util.AttributeTweaks;
import com.shenanigans.util.Fx;
import com.shenanigans.util.Items;
import com.shenanigans.util.Text;
import io.papermc.paper.event.player.AsyncChatEvent;
import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.block.Block;
import org.bukkit.block.Container;
import org.bukkit.block.data.Bisected;
import org.bukkit.block.data.BlockData;
import org.bukkit.block.data.type.Bed;
import org.bukkit.entity.Entity;
import org.bukkit.entity.FallingBlock;
import org.bukkit.entity.Item;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockDropItemEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.entity.EntityChangeBlockEvent;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.ExplosionPrimeEvent;
import org.bukkit.event.player.PlayerDropItemEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.inventory.ItemStack;

import java.util.List;
import java.util.UUID;

/** All the event-based trolls (freeze, chat trolls, fake TNT, block trolls...) plus cleanup. */
public final class TrollListener implements Listener {

    private final ShenanigansPlugin plugin;
    private final TrollManager manager;

    public TrollListener(ShenanigansPlugin plugin) {
        this.plugin = plugin;
        this.manager = plugin.manager();
    }

    private boolean active(Player player, String trollId) {
        return manager.isActive(player.getUniqueId(), trollId);
    }

    // ------------------------------------------------------------------ join / quit cleanup

    @EventHandler(priority = EventPriority.MONITOR)
    public void onQuit(PlayerQuitEvent event) {
        manager.stopAll(event.getPlayer().getUniqueId());
    }

    @EventHandler
    public void onJoin(PlayerJoinEvent event) {
        Player player = event.getPlayer();
        // In case the server crashed in the middle of a troll
        plugin.vault().restore(player);
        AttributeTweaks.restoreAll(player);
        InventoryTrolls.removeHotPotatoes(plugin, player);

        // Anyone with "Lonely" on shouldn't see the new player either
        for (UUID id : manager.trolledPlayers()) {
            if (manager.isActive(id, "lonely")) {
                Player lonely = Bukkit.getPlayer(id);
                if (lonely != null && !lonely.equals(player)) {
                    lonely.hidePlayer(plugin, player);
                }
            }
        }
    }

    // ------------------------------------------------------------------ movement

    @EventHandler(ignoreCancelled = true)
    public void onMove(PlayerMoveEvent event) {
        if (!event.hasChangedPosition() || !active(event.getPlayer(), "freeze")) {
            return;
        }
        Location from = event.getFrom();
        Location to = event.getTo();
        event.setTo(new Location(from.getWorld(), from.getX(), from.getY(), from.getZ(), to.getYaw(), to.getPitch()));
    }

    // ------------------------------------------------------------------ damage & explosions

    @EventHandler(ignoreCancelled = true)
    public void onFall(EntityDamageEvent event) {
        if (event.getCause() == EntityDamageEvent.DamageCause.FALL
                && event.getEntity() instanceof Player player
                && manager.consumeFallProtection(player.getUniqueId())) {
            event.setCancelled(true);
        }
    }

    @EventHandler(ignoreCancelled = true)
    public void onHarmlessDamage(EntityDamageByEntityEvent event) {
        Entity damager = event.getDamager();
        // Our fireworks and fake explosions never hurt. Anvils are allowed (their damage is capped in config).
        if (manager.isHarmless(damager) && !(damager instanceof FallingBlock)) {
            event.setCancelled(true);
        }
    }

    @EventHandler(ignoreCancelled = true)
    public void onPrime(ExplosionPrimeEvent event) {
        Entity entity = event.getEntity();
        if (!manager.isHarmless(entity)) {
            return;
        }
        event.setCancelled(true);
        Fx.fakeExplosion(entity.getLocation());
        manager.forget(entity);
        entity.remove();
    }

    @EventHandler(ignoreCancelled = true)
    public void onAnvilLand(EntityChangeBlockEvent event) {
        if (!(event.getEntity() instanceof FallingBlock falling) || !manager.isHarmless(falling)) {
            return;
        }
        event.setCancelled(true);
        Location loc = falling.getLocation();
        loc.getWorld().playSound(loc, Sound.BLOCK_ANVIL_LAND, 1f, 1f);
        loc.getWorld().spawnParticle(Particle.BLOCK, loc, 20, 0.3, 0.1, 0.3, Material.ANVIL.createBlockData());
        manager.forget(falling);
        falling.remove();
    }

    // ------------------------------------------------------------------ chat

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onChat(AsyncChatEvent event) {
        Player player = event.getPlayer();
        List<String> trolls = manager.activeIds(player.getUniqueId());
        if (trolls.isEmpty()) {
            return;
        }

        boolean rewrite = false;
        for (String id : ChatTrolls.REWRITERS) {
            if (trolls.contains(id)) {
                rewrite = true;
                break;
            }
        }
        if (rewrite) {
            String text = Text.plain(event.message());
            for (String id : ChatTrolls.REWRITERS) {
                if (trolls.contains(id)) {
                    text = ChatTrolls.transform(id, text, plugin.randomSillyPhrase());
                }
            }
            event.message(Component.text(text));
        }

        if (trolls.contains("shadowmute")) {
            UUID self = player.getUniqueId();
            event.viewers().removeIf(viewer -> viewer instanceof Player other && !other.getUniqueId().equals(self));
        }
    }

    // ------------------------------------------------------------------ blocks

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onBreak(BlockBreakEvent event) {
        Player player = event.getPlayer();
        Block block = event.getBlock();

        if (plugin.tempBlocks().isTemporary(block)) {
            event.setDropItems(false); // no free glass from cages
        }

        if (active(player, "nomine")) {
            event.setCancelled(true);
            return;
        }

        if (active(player, "boompick")) {
            Location center = block.getLocation().add(0.5, 0.5, 0.5);
            center.getWorld().spawnParticle(Particle.EXPLOSION, center, 1);
            center.getWorld().playSound(center, Sound.ENTITY_GENERIC_EXPLODE, 0.6f, 1.2f + Fx.rng().nextFloat() * 0.4f);
        }

        if (active(player, "regen")) {
            BlockData data = block.getBlockData();
            if (data instanceof Bisected || data instanceof Bed || block.getState() instanceof Container) {
                return; // don't mess with doors, beds or chests
            }
            event.setDropItems(false);
            Bukkit.getScheduler().runTaskLater(plugin, () -> {
                if (block.isEmpty()) {
                    block.setBlockData(data, false);
                    Location loc = block.getLocation().add(0.5, 0.5, 0.5);
                    loc.getWorld().spawnParticle(Particle.HAPPY_VILLAGER, loc, 8, 0.4, 0.4, 0.4);
                    loc.getWorld().playSound(loc, data.getSoundGroup().getPlaceSound(), 1f, 1f);
                }
            }, 20L);
        }
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onPlace(BlockPlaceEvent event) {
        if (active(event.getPlayer(), "noplace")) {
            event.setCancelled(true);
        }
    }

    @EventHandler(ignoreCancelled = true)
    public void onBlockDrops(BlockDropItemEvent event) {
        if (!active(event.getPlayer(), "dirtdrops")) {
            return;
        }
        for (Item item : event.getItems()) {
            ItemStack old = item.getItemStack();
            item.setItemStack(new ItemStack(Material.DIRT, old.getAmount()));
        }
    }

    // ------------------------------------------------------------------ hot potato

    @EventHandler(ignoreCancelled = true)
    public void onDrop(PlayerDropItemEvent event) {
        Item drop = event.getItemDrop();
        if (Items.readTag(drop.getItemStack(), plugin.key(InventoryTrolls.HOT_POTATO_TAG)) == null) {
            return;
        }
        Location loc = drop.getLocation();
        loc.getWorld().spawnParticle(Particle.SMOKE, loc, 15, 0.2, 0.2, 0.2, 0.02);
        loc.getWorld().playSound(loc, Sound.BLOCK_FIRE_EXTINGUISH, 1f, 1.2f);
        drop.remove();
        manager.stop(event.getPlayer().getUniqueId(), "hotpotato");
    }
}
