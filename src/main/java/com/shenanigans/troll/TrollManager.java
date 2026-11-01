package com.shenanigans.troll;

import com.shenanigans.ShenanigansPlugin;
import com.shenanigans.util.Text;
import net.kyori.adventure.text.minimessage.tag.resolver.TagResolver;
import org.bukkit.Bukkit;
import org.bukkit.command.CommandSender;
import org.bukkit.command.ConsoleCommandSender;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitTask;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Level;

/**
 * Runs trolls: checks permissions, starts/stops toggle trolls, ticks them, and cleans up after them.
 */
public final class TrollManager {

    private final ShenanigansPlugin plugin;
    private final Map<UUID, Map<String, ActiveTroll>> active = new ConcurrentHashMap<>();
    private final Map<UUID, Long> fallProtection = new ConcurrentHashMap<>();
    /** Entities we spawned that must not really hurt anyone / blow anything up (fake TNT, fireworks, anvils...). */
    private final Set<UUID> harmless = ConcurrentHashMap.newKeySet();
    /** Entities we spawned that get removed later (and on shutdown). */
    private final Set<UUID> temporary = ConcurrentHashMap.newKeySet();
    private BukkitTask ticker;

    public TrollManager(ShenanigansPlugin plugin) {
        this.plugin = plugin;
    }

    public void startTicker() {
        ticker = Bukkit.getScheduler().runTaskTimer(plugin, this::tick, 1L, 1L);
    }

    // ------------------------------------------------------------------ using trolls

    /**
     * Use a troll on someone. Handles permissions, bypass, toggling and messages.
     *
     * @return true if something happened
     */
    public boolean apply(CommandSender sender, Player target, Troll troll, String[] args) {
        return apply(sender, target, troll, args, false);
    }

    /**
     * @param quiet true = don't send the result to the sender (used when trolling everyone at once)
     */
    public boolean apply(CommandSender sender, Player target, Troll troll, String[] args, boolean quiet) {
        if (plugin.isTrollDisabled(troll)) {
            reply(sender, quiet, "troll-disabled", Text.ph("troll", troll.name()));
            return false;
        }
        if (!canUse(sender, troll)) {
            reply(sender, quiet, "no-permission");
            return false;
        }
        if (isProtected(sender, target)) {
            reply(sender, quiet, "protected", Text.ph("player", target.getName()));
            return false;
        }

        if (troll.isToggle()) {
            if (isActive(target.getUniqueId(), troll.id())) {
                stop(target.getUniqueId(), troll.id());
                reply(sender, quiet, "toggled-off", Text.ph("troll", troll.name()), Text.ph("player", target.getName()));
                notifyStaff(sender, target, troll, "off");
                return true;
            }
            return start(sender, target, troll, args, quiet);
        }

        TrollContext ctx = new TrollContext(plugin, sender, target, troll, args);
        try {
            troll.action().accept(ctx);
        } catch (Exception ex) {
            plugin.getLogger().log(Level.WARNING, "Troll '" + troll.id() + "' failed", ex);
            reply(sender, quiet, "troll-error", Text.ph("troll", troll.name()));
            return false;
        }
        if (ctx.failure() != null) {
            reply(sender, quiet, "troll-failed", Text.ph("troll", troll.name()), Text.ph("reason", ctx.failure()));
            return false;
        }
        reply(sender, quiet, "applied", Text.ph("troll", troll.name()), Text.ph("player", target.getName()));
        notifyStaff(sender, target, troll, null);
        return true;
    }

    private void reply(CommandSender sender, boolean quiet, String key, TagResolver... resolvers) {
        if (!quiet) {
            Text.send(sender, key, resolvers);
        }
    }

    private boolean start(CommandSender sender, Player target, Troll troll, String[] args, boolean quiet) {
        UUID id = target.getUniqueId();
        for (String other : troll.conflicts()) {
            stop(id, other);
        }

        int seconds = plugin.getConfig().getInt("durations." + troll.id(), troll.defaultSeconds());
        TrollContext ctx = new TrollContext(plugin, sender, target, troll, args);
        ActiveTroll at = new ActiveTroll(ctx, Math.max(0, seconds) * 20L);
        // Put it in the map first so listeners already see it as active.
        active.computeIfAbsent(id, k -> new ConcurrentHashMap<>()).put(troll.id(), at);

        try {
            if (troll.onStart() != null) {
                troll.onStart().accept(at);
            }
        } catch (Exception ex) {
            plugin.getLogger().log(Level.WARNING, "Troll '" + troll.id() + "' failed to start", ex);
            removeQuietly(id, troll.id());
            reply(sender, quiet, "troll-error", Text.ph("troll", troll.name()));
            return false;
        }

        if (at.abortReason() != null) {
            removeQuietly(id, troll.id());
            reply(sender, quiet, "troll-failed", Text.ph("troll", troll.name()), Text.ph("reason", at.abortReason()));
            return false;
        }

        String length = seconds > 0 ? seconds + "s" : Text.raw("until-stopped", "until you stop it");
        reply(sender, quiet, "toggled-on", Text.ph("troll", troll.name()), Text.ph("player", target.getName()),
                Text.ph("duration", length));
        notifyStaff(sender, target, troll, "on");
        return true;
    }

    public boolean canUse(CommandSender sender, Troll troll) {
        return sender.hasPermission("shenanigans.troll." + troll.id()) || sender.hasPermission("shenanigans.troll.*");
    }

    public boolean isProtected(CommandSender sender, Player target) {
        if (!target.hasPermission("shenanigans.bypass")) {
            return false;
        }
        if (sender instanceof ConsoleCommandSender) {
            return false;
        }
        if (sender instanceof Player p && p.getUniqueId().equals(target.getUniqueId())) {
            return false; // you can always troll yourself
        }
        return !sender.hasPermission("shenanigans.bypass.override");
    }

    private void notifyStaff(CommandSender sender, Player target, Troll troll, String state) {
        String action = state == null ? troll.name() : troll.name() + " (" + state + ")";
        if (plugin.getConfig().getBoolean("log-to-console", true) && !(sender instanceof ConsoleCommandSender)) {
            plugin.getLogger().info(sender.getName() + " used " + action + " on " + target.getName());
        }
        if (!plugin.getConfig().getBoolean("notify-staff", true)) {
            return;
        }
        for (Player staff : Bukkit.getOnlinePlayers()) {
            if (staff.equals(sender) || staff.equals(target) || !staff.hasPermission("shenanigans.notify")) {
                continue;
            }
            Text.send(staff, "notify", Text.ph("sender", sender.getName()), Text.ph("troll", action),
                    Text.ph("player", target.getName()));
        }
    }

    // ------------------------------------------------------------------ active toggles

    public boolean isActive(UUID player, String trollId) {
        Map<String, ActiveTroll> map = active.get(player);
        return map != null && map.containsKey(trollId);
    }

    public ActiveTroll getActive(UUID player, String trollId) {
        Map<String, ActiveTroll> map = active.get(player);
        return map == null ? null : map.get(trollId);
    }

    public List<String> activeIds(UUID player) {
        Map<String, ActiveTroll> map = active.get(player);
        return map == null ? Collections.emptyList() : new ArrayList<>(map.keySet());
    }

    /** Players that have at least one toggle troll on them. */
    public Set<UUID> trolledPlayers() {
        return Collections.unmodifiableSet(active.keySet());
    }

    /** Switch a toggle troll off (runs its cleanup). */
    public boolean stop(UUID player, String trollId) {
        Map<String, ActiveTroll> map = active.get(player);
        if (map == null) {
            return false;
        }
        ActiveTroll at = map.remove(trollId);
        if (map.isEmpty()) {
            active.remove(player);
        }
        if (at == null) {
            return false;
        }
        try {
            if (at.troll().onStop() != null) {
                at.troll().onStop().accept(at);
            }
        } catch (Exception ex) {
            plugin.getLogger().log(Level.WARNING, "Troll '" + trollId + "' failed to stop cleanly", ex);
        }
        return true;
    }

    private void removeQuietly(UUID player, String trollId) {
        Map<String, ActiveTroll> map = active.get(player);
        if (map != null) {
            map.remove(trollId);
            if (map.isEmpty()) {
                active.remove(player);
            }
        }
    }

    /** Switch off every toggle troll on a player. Returns how many were stopped. */
    public int stopAll(UUID player) {
        int count = 0;
        for (String id : activeIds(player)) {
            if (stop(player, id)) {
                count++;
            }
        }
        return count;
    }

    private void tick() {
        for (UUID id : new ArrayList<>(active.keySet())) {
            Player player = Bukkit.getPlayer(id);
            if (player == null || !player.isOnline()) {
                stopAll(id);
                continue;
            }
            Map<String, ActiveTroll> map = active.get(id);
            if (map == null) {
                continue;
            }
            for (ActiveTroll at : new ArrayList<>(map.values())) {
                at.tickAge();
                Troll troll = at.troll();
                if (troll.onTick() != null && troll.tickPeriod() > 0 && at.age() % troll.tickPeriod() == 0) {
                    try {
                        troll.onTick().accept(at);
                    } catch (Exception ex) {
                        plugin.getLogger().log(Level.WARNING, "Troll '" + troll.id() + "' crashed while running, stopping it", ex);
                        stop(id, troll.id());
                        continue;
                    }
                }
                if (at.durationTicks() > 0 && at.age() >= at.durationTicks() && isActive(id, troll.id())) {
                    stop(id, troll.id());
                }
            }
        }
        long now = System.currentTimeMillis();
        fallProtection.values().removeIf(until -> until < now);
    }

    public void shutdown() {
        if (ticker != null) {
            ticker.cancel();
        }
        for (UUID id : new ArrayList<>(active.keySet())) {
            stopAll(id);
        }
        for (UUID id : new ArrayList<>(temporary)) {
            Entity entity = Bukkit.getEntity(id);
            if (entity != null) {
                entity.remove();
            }
        }
        temporary.clear();
        harmless.clear();
    }

    // ------------------------------------------------------------------ helpers for trolls

    /** Cancel the next fall damage this player takes (within the time limit). */
    public void protectFromFall(Player player, int seconds) {
        if (plugin.getConfig().getBoolean("protect-from-fall-damage", true)) {
            fallProtection.put(player.getUniqueId(), System.currentTimeMillis() + seconds * 1000L);
        }
    }

    /** Used by the listener: true (and used up) if this fall should be cancelled. */
    public boolean consumeFallProtection(UUID player) {
        Long until = fallProtection.remove(player);
        return until != null && until >= System.currentTimeMillis();
    }

    public void markHarmless(Entity entity) {
        harmless.add(entity.getUniqueId());
    }

    public boolean isHarmless(Entity entity) {
        return harmless.contains(entity.getUniqueId());
    }

    public void forget(Entity entity) {
        harmless.remove(entity.getUniqueId());
        temporary.remove(entity.getUniqueId());
    }

    /** Remove an entity we spawned after some ticks. */
    public void removeLater(Entity entity, long ticks) {
        temporary.add(entity.getUniqueId());
        Bukkit.getScheduler().runTaskLater(plugin, () -> {
            if (entity.isValid()) {
                entity.remove();
            }
            forget(entity);
        }, ticks);
    }
}
