package com.shenanigans.util;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.function.Consumer;

/**
 * Run several steps on a player with delays, like a little script:
 * <pre>
 * Sequence.on(plugin, player)
 *     .at(0, p -> ...)      // right away
 *     .at(40, p -> ...)     // 2 seconds later
 *     .run();
 * </pre>
 * Steps are skipped if the player has left.
 */
public final class Sequence {

    private record Step(long tick, Consumer<Player> action) {
    }

    private final Plugin plugin;
    private final UUID target;
    private final List<Step> steps = new ArrayList<>();

    private Sequence(Plugin plugin, Player target) {
        this.plugin = plugin;
        this.target = target.getUniqueId();
    }

    public static Sequence on(Plugin plugin, Player target) {
        return new Sequence(plugin, target);
    }

    /** Add a step that runs {@code tick} ticks after run() (20 ticks = 1 second). */
    public Sequence at(long tick, Consumer<Player> action) {
        steps.add(new Step(tick, action));
        return this;
    }

    public void run() {
        for (Step step : steps) {
            Runnable task = () -> {
                Player player = Bukkit.getPlayer(target);
                if (player != null && player.isOnline()) {
                    step.action().accept(player);
                }
            };
            if (step.tick() <= 0) {
                task.run();
            } else {
                Bukkit.getScheduler().runTaskLater(plugin, task, step.tick());
            }
        }
    }
}
