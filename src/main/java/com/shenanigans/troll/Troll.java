package com.shenanigans.troll;

import org.bukkit.Material;

import java.util.List;
import java.util.Locale;
import java.util.function.Consumer;

/**
 * One troll. There are two kinds:
 * <ul>
 *   <li>INSTANT - happens once (fake ban, launch, demo screen...)</li>
 *   <li>TOGGLE - stays on until you stop it or its time runs out (freeze, uwu chat, tiny...)</li>
 * </ul>
 * Trolls are created with the little builder at the bottom, see the classes in the "list" package.
 */
public final class Troll {

    public enum Kind { INSTANT, TOGGLE }

    private final String id;
    private final String name;
    private final String description;
    private final Material icon;
    private final Kind kind;
    private final Category category;
    private final String usage;
    private final Consumer<TrollContext> action;
    private final Consumer<ActiveTroll> onStart;
    private final Consumer<ActiveTroll> onTick;
    private final Consumer<ActiveTroll> onStop;
    private final int tickPeriod;
    private final int defaultSeconds;
    private final List<String> conflicts;

    private Troll(Builder b, Category category) {
        this.id = b.id;
        this.name = b.name;
        this.description = b.description;
        this.icon = b.icon;
        this.kind = b.kind;
        this.category = category;
        this.usage = b.usage;
        this.action = b.action;
        this.onStart = b.onStart;
        this.onTick = b.onTick;
        this.onStop = b.onStop;
        this.tickPeriod = b.tickPeriod;
        this.defaultSeconds = b.defaultSeconds;
        this.conflicts = List.copyOf(b.conflicts);
    }

    public String id() { return id; }
    public String name() { return name; }
    public String description() { return description; }
    public Material icon() { return icon; }
    public Kind kind() { return kind; }
    public Category category() { return category; }
    /** Optional arguments shown in help, e.g. "[message]". Empty if none. */
    public String usage() { return usage; }
    public Consumer<TrollContext> action() { return action; }
    public Consumer<ActiveTroll> onStart() { return onStart; }
    public Consumer<ActiveTroll> onTick() { return onTick; }
    public Consumer<ActiveTroll> onStop() { return onStop; }
    public int tickPeriod() { return tickPeriod; }
    public int defaultSeconds() { return defaultSeconds; }
    public List<String> conflicts() { return conflicts; }
    public boolean isToggle() { return kind == Kind.TOGGLE; }

    public static Builder instant(String id, String name, Material icon, String description) {
        return new Builder(id, name, icon, description, Kind.INSTANT);
    }

    public static Builder toggle(String id, String name, Material icon, String description) {
        return new Builder(id, name, icon, description, Kind.TOGGLE);
    }

    public static final class Builder {
        private final String id;
        private final String name;
        private final Material icon;
        private final String description;
        private final Kind kind;
        private String usage = "";
        private Consumer<TrollContext> action;
        private Consumer<ActiveTroll> onStart;
        private Consumer<ActiveTroll> onTick;
        private Consumer<ActiveTroll> onStop;
        private int tickPeriod = 0;
        private int defaultSeconds = 0;
        private List<String> conflicts = List.of();

        private Builder(String id, String name, Material icon, String description, Kind kind) {
            this.id = id.toLowerCase(Locale.ROOT);
            this.name = name;
            this.icon = icon;
            this.description = description;
            this.kind = kind;
        }

        /** Shown in /troll help, e.g. "[message]". */
        public Builder usage(String usage) {
            this.usage = usage;
            return this;
        }

        /** What an INSTANT troll does. */
        public Builder does(Consumer<TrollContext> action) {
            this.action = action;
            return this;
        }

        /** Runs once when a TOGGLE troll is switched on. */
        public Builder onStart(Consumer<ActiveTroll> onStart) {
            this.onStart = onStart;
            return this;
        }

        /** Runs every {@code ticks} ticks while a TOGGLE troll is on (20 ticks = 1 second). */
        public Builder every(int ticks, Consumer<ActiveTroll> onTick) {
            this.tickPeriod = Math.max(1, ticks);
            this.onTick = onTick;
            return this;
        }

        /** Runs when a TOGGLE troll is switched off, times out, or the player leaves. Undo things here! */
        public Builder onStop(Consumer<ActiveTroll> onStop) {
            this.onStop = onStop;
            return this;
        }

        /** Default length in seconds (0 = until stopped). Can be changed in config.yml under "durations". */
        public Builder seconds(int seconds) {
            this.defaultSeconds = seconds;
            return this;
        }

        /** Other toggle trolls that get switched off when this one starts. */
        public Builder conflictsWith(String... ids) {
            this.conflicts = List.of(ids);
            return this;
        }

        Troll build(Category category) {
            if (kind == Kind.INSTANT && action == null) {
                throw new IllegalStateException("Instant troll " + id + " has no action");
            }
            return new Troll(this, category);
        }
    }
}
