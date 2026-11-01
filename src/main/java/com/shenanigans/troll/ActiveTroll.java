package com.shenanigans.troll;

import com.shenanigans.ShenanigansPlugin;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/** A toggle troll that is currently switched on for someone. */
public final class ActiveTroll {

    private final TrollContext ctx;
    private final UUID targetId;
    private final long durationTicks;
    private final Map<String, Object> data = new HashMap<>();
    private long age;
    private String abortReason;

    public ActiveTroll(TrollContext ctx, long durationTicks) {
        this.ctx = ctx;
        this.targetId = ctx.target().getUniqueId();
        this.durationTicks = durationTicks;
    }

    public TrollContext ctx() { return ctx; }
    public Troll troll() { return ctx.troll(); }
    public ShenanigansPlugin plugin() { return ctx.plugin(); }
    public UUID targetId() { return targetId; }
    /** 0 = runs until stopped. */
    public long durationTicks() { return durationTicks; }
    /** How many ticks this troll has been running. */
    public long age() { return age; }

    void tickAge() { age++; }

    /** The trolled player, or null if they left. */
    public Player player() {
        return Bukkit.getPlayer(targetId);
    }

    @SuppressWarnings("unchecked")
    public <T> T get(String key) {
        return (T) data.get(key);
    }

    @SuppressWarnings("unchecked")
    public <T> T getOr(String key, T fallback) {
        Object value = data.get(key);
        return value == null ? fallback : (T) value;
    }

    public void put(String key, Object value) {
        data.put(key, value);
    }

    /** Call from onStart to cancel the troll before it begins. */
    public void abort(String reason) {
        this.abortReason = reason;
    }

    public String abortReason() {
        return abortReason;
    }
}
