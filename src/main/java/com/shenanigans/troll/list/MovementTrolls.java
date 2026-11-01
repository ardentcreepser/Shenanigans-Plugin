package com.shenanigans.troll.list;

import com.shenanigans.troll.ActiveTroll;
import com.shenanigans.troll.Category;
import com.shenanigans.troll.Troll;
import com.shenanigans.troll.TrollRegistry;
import com.shenanigans.util.AttributeTweaks;
import com.shenanigans.util.Fx;
import com.shenanigans.util.Sequence;
import io.papermc.paper.math.Angle;
import org.bukkit.Bukkit;
import org.bukkit.Color;
import org.bukkit.FireworkEffect;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.entity.Firework;
import org.bukkit.entity.Player;
import org.bukkit.inventory.meta.FireworkMeta;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.util.Vector;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.List;

/** Launch them, freeze them, shrink them, spin them, make them bounce... */
public final class MovementTrolls {

    private MovementTrolls() {
    }

    public static void register(TrollRegistry r) {
        Category c = Category.MOVEMENT;

        r.add(c, Troll.instant("launch", "Launch", Material.SLIME_BALL,
                        "Yeets them straight up into the sky. Fall damage is cancelled (see config).")
                .does(ctx -> {
                    Player p = ctx.target();
                    p.setVelocity(new Vector(0, ctx.cfgDouble("power", 3.0), 0));
                    ctx.plugin().manager().protectFromFall(p, 30);
                    Fx.sound(p, Sound.ENTITY_FIREWORK_ROCKET_LAUNCH, 1f, 0.8f);
                    Fx.puff(p.getLocation(), Particle.CLOUD, 30);
                }));

        r.add(c, Troll.instant("rocket", "Rocket", Material.FIRE_CHARGE,
                        "Turns them into a rocket with a smoke trail and a firework explosion at the top.")
                .does(ctx -> {
                    Player p = ctx.target();
                    ctx.plugin().manager().protectFromFall(p, 30);
                    Fx.sound(p, Sound.ENTITY_FIREWORK_ROCKET_LAUNCH, 1f, 0.6f);
                    Sequence seq = Sequence.on(ctx.plugin(), p);
                    for (int i = 0; i < 20; i++) {
                        seq.at(i, pl -> {
                            pl.setVelocity(new Vector(0, 1.6, 0));
                            pl.getWorld().spawnParticle(Particle.FIREWORK, pl.getLocation(), 6, 0.1, 0.1, 0.1, 0.05);
                            pl.getWorld().spawnParticle(Particle.FLAME, pl.getLocation(), 4, 0.1, 0.1, 0.1, 0.02);
                        });
                    }
                    seq.at(21, pl -> {
                        Firework fw = pl.getWorld().spawn(pl.getLocation(), Firework.class, f -> {
                            FireworkMeta meta = f.getFireworkMeta();
                            meta.addEffect(FireworkEffect.builder().with(FireworkEffect.Type.BALL_LARGE)
                                    .withColor(Color.RED, Color.ORANGE, Color.YELLOW).withFade(Color.WHITE)
                                    .flicker(true).trail(true).build());
                            f.setFireworkMeta(meta);
                        });
                        ctx.plugin().manager().markHarmless(fw);
                        fw.detonate();
                    });
                    seq.run();
                }));

        r.add(c, Troll.instant("skydive", "Skydive", Material.ELYTRA,
                        "Teleports them way up into the sky. They fall back down safely (fall damage cancelled).")
                .does(ctx -> {
                    Player p = ctx.target();
                    Location loc = p.getLocation();
                    double y = Math.min(loc.getY() + ctx.cfgInt("height", 150), p.getWorld().getMaxHeight() - 2);
                    Location up = loc.clone();
                    up.setY(y);
                    ctx.plugin().manager().protectFromFall(p, 60);
                    p.teleport(up);
                    Fx.title(p, "<aqua>Wheeeee!", "<gray>enjoy the view", 5, 40, 10);
                    Fx.sound(p, Sound.ENTITY_PHANTOM_SWOOP, 1f, 1f);
                }));

        r.add(c, Troll.toggle("freeze", "Freeze", Material.ICE,
                        "They can look around but can't move at all. They'll think they're lagging.")
                .onStart(at -> at.player().setVelocity(new Vector(0, 0, 0))));
        // (the actual freezing is done in TrollListener)

        r.add(c, Troll.toggle("spin", "Spin", Material.HEART_OF_THE_SEA,
                        "Spins their camera around and around and around...")
                .seconds(5)
                .every(1, at -> at.player().setRotation(Angle.relative(36f), Angle.relative(0f))));

        r.add(c, Troll.instant("flip", "180 Flip", Material.COMPASS,
                        "Instantly turns them around to face the other way.")
                .does(ctx -> ctx.target().setRotation(Angle.relative(180f), Angle.relative(0f))));

        r.add(c, Troll.toggle("lag", "Fake Lag", Material.SOUL_SAND,
                        "Every few seconds they get rubber-banded back to where they were. Very believable.")
                .seconds(30)
                .every(1, MovementTrolls::lagTick));

        r.add(c, Troll.toggle("drunk", "Drunk", Material.HONEY_BOTTLE,
                        "Wobbly camera, nausea and random stumbling.")
                .seconds(30)
                .onStart(at -> Fx.effect(at.player(), PotionEffectType.NAUSEA, PotionEffect.INFINITE_DURATION, 0))
                .every(4, at -> {
                    Player p = at.player();
                    p.setRotation(Angle.relative(Fx.rng().nextFloat(-7f, 7f)), Angle.relative(Fx.rng().nextFloat(-3f, 3f)));
                    if (Fx.rng().nextInt(8) == 0 && Fx.isGrounded(p)) {
                        Vector stumble = new Vector(Fx.rng().nextDouble(-0.4, 0.4), 0.1, Fx.rng().nextDouble(-0.4, 0.4));
                        p.setVelocity(p.getVelocity().add(stumble));
                    }
                })
                .onStop(at -> {
                    Player p = at.player();
                    if (p != null) {
                        p.removePotionEffect(PotionEffectType.NAUSEA);
                    }
                }));

        r.add(c, Troll.toggle("hiccups", "Hiccups", Material.BUBBLE_CORAL,
                        "*hic!* They randomly hop into the air with a burp.")
                .seconds(60)
                .every(20, at -> {
                    Player p = at.player();
                    if (Fx.rng().nextInt(100) < 35 && Fx.isGrounded(p)) {
                        p.setVelocity(p.getVelocity().setY(0.45));
                        Fx.sound(p, Sound.ENTITY_PLAYER_BURP, 1f, 1.6f);
                        Fx.actionBar(p, "<gray><italic>*hic!*");
                    }
                }));

        r.add(c, Troll.instant("sneeze", "Sneeze", Material.SPORE_BLOSSOM,
                        "ACHOO! They sneeze so hard they fly backwards.")
                .does(ctx -> {
                    Player p = ctx.target();
                    Vector back = p.getLocation().getDirection().setY(0).normalize().multiply(-1.3).setY(0.4);
                    p.setVelocity(back);
                    Fx.sound(p, Sound.ENTITY_PANDA_SNEEZE, 1f, 1f);
                    p.getWorld().spawnParticle(Particle.SNEEZE, p.getEyeLocation().add(p.getLocation().getDirection()), 25, 0.2, 0.2, 0.2, 0.05);
                    if (ctx.cfgBool("say-achoo", true)) {
                        p.chat("ACHOO!");
                    }
                }));

        r.add(c, Troll.instant("push", "Ghost Push", Material.PHANTOM_MEMBRANE,
                        "An invisible force shoves them in a random direction.")
                .does(ctx -> {
                    Player p = ctx.target();
                    double angle = Fx.rng().nextDouble(Math.PI * 2);
                    p.setVelocity(new Vector(Math.cos(angle) * 1.4, 0.45, Math.sin(angle) * 1.4));
                    Fx.sound(p, Sound.ENTITY_PHANTOM_FLAP, 1f, 0.6f);
                }));

        r.add(c, Troll.instant("levitate", "Levitate", Material.SHULKER_SHELL,
                        "They float up helplessly for a few seconds (and don't take fall damage after).")
                .does(ctx -> {
                    Player p = ctx.target();
                    Fx.effect(p, PotionEffectType.LEVITATION, ctx.cfgInt("seconds", 6) * 20, 1);
                    ctx.plugin().manager().protectFromFall(p, 40);
                    Fx.sound(p, Sound.ENTITY_SHULKER_SHOOT, 1f, 1f);
                }));

        r.add(c, Troll.instant("randomtp", "Random Teleport", Material.ENDER_PEARL,
                        "Teleports them to a random safe spot somewhere in the world.")
                .usage("[radius]")
                .does(ctx -> {
                    Player p = ctx.target();
                    World world = p.getWorld();
                    if (world.getEnvironment() == World.Environment.NETHER) {
                        ctx.fail("Doesn't work in the Nether (the roof gets in the way)");
                        return;
                    }
                    int radius;
                    try {
                        radius = Integer.parseInt(ctx.arg(0, String.valueOf(ctx.cfgInt("radius", 1000))));
                    } catch (NumberFormatException ex) {
                        radius = ctx.cfgInt("radius", 1000);
                    }
                    Location base = p.getLocation();
                    int x = base.getBlockX() + Fx.rng().nextInt(-radius, radius + 1);
                    int z = base.getBlockZ() + Fx.rng().nextInt(-radius, radius + 1);
                    world.getChunkAtAsync(x >> 4, z >> 4).thenAccept(chunk -> {
                        Block top = world.getHighestBlockAt(x, z);
                        if (!top.getType().isSolid()) {
                            ctx.sender().sendMessage(com.shenanigans.util.Text.mm(
                                    "<red>Random spot was unsafe (water/lava), try again."));
                            return;
                        }
                        Location dest = top.getLocation().add(0.5, 1, 0.5);
                        dest.setYaw(base.getYaw());
                        dest.setPitch(base.getPitch());
                        if (p.isOnline()) {
                            p.teleportAsync(dest);
                            Fx.sound(p, Sound.ENTITY_ENDERMAN_TELEPORT, 1f, 1f);
                        }
                    });
                }));

        r.add(c, Troll.instant("swap", "Swap Places", Material.MAGENTA_GLAZED_TERRACOTTA,
                        "Swaps their position with another random player (or one you choose).")
                .usage("[other player]")
                .does(ctx -> {
                    Player p = ctx.target();
                    Player other = ctx.args().length > 0 ? Bukkit.getPlayerExact(ctx.arg(0, "")) : null;
                    if (other == null) {
                        List<Player> others = new ArrayList<>(Bukkit.getOnlinePlayers());
                        others.remove(p);
                        if (others.isEmpty()) {
                            ctx.fail("Nobody else is online to swap with");
                            return;
                        }
                        other = Fx.pick(others);
                    }
                    if (other.equals(p)) {
                        ctx.fail("Can't swap a player with themselves");
                        return;
                    }
                    Location a = p.getLocation();
                    Location b = other.getLocation();
                    p.teleport(b);
                    other.teleport(a);
                    Fx.sound(p, Sound.ENTITY_ENDERMAN_TELEPORT, 1f, 1.2f);
                    Fx.sound(other, Sound.ENTITY_ENDERMAN_TELEPORT, 1f, 1.2f);
                }));

        // ---------------- attribute trolls (all undone automatically) ----------------

        r.add(c, attributeToggle("tiny", "Tiny", Material.BROWN_MUSHROOM,
                "Shrinks them to ant size. Everything is huge now.", "scale", 0.2, 120)
                .conflictsWith("giant"));

        r.add(c, attributeToggle("giant", "Giant", Material.BIG_DRIPLEAF,
                "Makes them HUGE. Watch them get stuck in doorways.", "scale", 3.0, 120)
                .conflictsWith("tiny"));

        r.add(c, attributeToggle("moon", "Moon Gravity", Material.END_STONE,
                "Super low gravity. Every jump goes to the moon.", "gravity", 0.012, 60));

        r.add(c, attributeToggle("nojump", "No Jumping", Material.COBWEB,
                "Their jump button stops working.", "jump_strength", 0.0, 60));

        r.add(c, Troll.toggle("shortarms", "T-Rex Arms", Material.BONE,
                        "Their reach is tiny. They can't break, place, open or hit anything that isn't right in their face.")
                .seconds(120)
                .onStart(at -> {
                    AttributeTweaks.set(at.player(), "block_interaction_range", at.ctx().cfgDouble("block-reach", 0.6));
                    AttributeTweaks.set(at.player(), "entity_interaction_range", at.ctx().cfgDouble("entity-reach", 0.0));
                })
                .onStop(at -> {
                    if (at.player() != null) {
                        AttributeTweaks.restore(at.player(), "block_interaction_range");
                        AttributeTweaks.restore(at.player(), "entity_interaction_range");
                    }
                }));

        r.add(c, attributeToggle("snail", "Snail Speed", Material.TURTLE_SCUTE,
                "They walk sooooo slooooowly.", "movement_speed", 0.025, 60)
                .conflictsWith("turbo"));

        r.add(c, attributeToggle("turbo", "Turbo Speed", Material.SUGAR,
                "They run way too fast to control.", "movement_speed", 0.4, 30)
                .conflictsWith("snail"));

        r.add(c, attributeToggle("bouncy", "Bouncy", Material.SLIME_BLOCK,
                "They bounce off the ground and walls like a rubber ball. (Minecraft 26.2+ attribute)",
                "bounciness", 0.85, 60));

        r.add(c, attributeToggle("iceskates", "Ice Skates", Material.PACKED_ICE,
                "No grip on the ground at all, everything feels like ice. (Minecraft 26.2+ attribute)",
                "friction_modifier", 0.1, 60));

        r.add(c, attributeToggle("stairmaster", "Step Master", Material.OAK_STAIRS,
                "They walk up full blocks (and walls!) like stairs, by accident.", "step_height", 2.5, 60));
    }

    /** A toggle troll that just sets one attribute from config (trolls.&lt;id&gt;.value) and restores it after. */
    private static Troll.Builder attributeToggle(String id, String name, Material icon, String description,
                                                 String attribute, double defaultValue, int seconds) {
        return Troll.toggle(id, name, icon, description)
                .seconds(seconds)
                .onStart(at -> {
                    AttributeTweaks.set(at.player(), attribute, at.ctx().cfgDouble("value", defaultValue));
                    Fx.sound(at.player(), Sound.ENTITY_ILLUSIONER_CAST_SPELL, 0.8f, 1.2f);
                })
                .onStop(at -> {
                    if (at.player() != null) {
                        AttributeTweaks.restore(at.player(), attribute);
                    }
                });
    }

    private static void lagTick(ActiveTroll at) {
        Player p = at.player();
        Deque<Location> history = at.get("history");
        if (history == null) {
            history = new ArrayDeque<>();
            at.put("history", history);
        }
        history.addLast(p.getLocation());
        if (history.size() > 40) {
            history.removeFirst();
        }
        if (at.age() % 50 == 0 && history.size() >= 30 && Fx.rng().nextInt(100) < 70) {
            Location back = history.peekFirst();
            if (back != null && back.getWorld().equals(p.getWorld())) {
                p.teleport(back.clone().setDirection(p.getLocation().getDirection()));
                history.clear();
            }
        }
    }
}
