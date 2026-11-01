package com.shenanigans.util;

import net.kyori.adventure.title.Title;
import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.util.Vector;

import java.util.List;
import java.util.concurrent.ThreadLocalRandom;

/** Small effect helpers used all over the trolls. */
public final class Fx {

    private Fx() {
    }

    public static ThreadLocalRandom rng() {
        return ThreadLocalRandom.current();
    }

    public static <T> T pick(List<T> list) {
        return list.get(rng().nextInt(list.size()));
    }

    @SafeVarargs
    public static <T> T pick(T... values) {
        return values[rng().nextInt(values.length)];
    }

    /** Play a sound only this player can hear, right where they are. */
    public static void sound(Player player, Sound sound, float volume, float pitch) {
        player.playSound(player.getLocation(), sound, volume, pitch);
    }

    /** Play a sound only this player can hear, at a location. */
    public static void soundAt(Player player, Location at, Sound sound, float volume, float pitch) {
        player.playSound(at, sound, volume, pitch);
    }

    /** A spot a few blocks behind the player (where they aren't looking). */
    public static Location behind(Player player, double distance) {
        Location loc = player.getLocation();
        Vector dir = loc.getDirection().setY(0);
        if (dir.lengthSquared() < 1.0E-4) {
            dir = new Vector(0, 0, 1);
        }
        dir.normalize().multiply(-distance);
        return loc.clone().add(dir);
    }

    /** A random spot on a circle around a location. */
    public static Location around(Location center, double radius) {
        double angle = rng().nextDouble(Math.PI * 2);
        return center.clone().add(Math.cos(angle) * radius, 0, Math.sin(angle) * radius);
    }

    public static void title(Player player, String title, String subtitle, int fadeIn, int stay, int fadeOut) {
        player.showTitle(Title.title(Text.mm(title), Text.mm(subtitle), fadeIn, stay, fadeOut));
    }

    public static void actionBar(Player player, String miniMessage) {
        player.sendActionBar(Text.mm(miniMessage));
    }

    /** Give a potion effect without particles. */
    public static void effect(Player player, PotionEffectType type, int ticks, int amplifier) {
        player.addPotionEffect(new PotionEffect(type, ticks, amplifier, false, false, true));
    }

    /** Looks and sounds like an explosion but doesn't hurt anything. */
    public static void fakeExplosion(Location loc) {
        World world = loc.getWorld();
        world.spawnParticle(Particle.EXPLOSION_EMITTER, loc, 1);
        world.playSound(loc, Sound.ENTITY_GENERIC_EXPLODE, 4f, 0.8f + rng().nextFloat() * 0.4f);
    }

    /** True if the player is standing on something solid. */
    public static boolean isGrounded(Player player) {
        return player.getLocation().subtract(0, 0.05, 0).getBlock().getType().isSolid();
    }

    /** Small puff of particles at a location. */
    public static void puff(Location loc, Particle particle, int count) {
        loc.getWorld().spawnParticle(particle, loc, count, 0.3, 0.3, 0.3, 0.02);
    }
}
