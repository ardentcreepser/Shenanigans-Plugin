package com.shenanigans.troll.list;

import com.shenanigans.troll.ActiveTroll;
import com.shenanigans.troll.Category;
import com.shenanigans.troll.Troll;
import com.shenanigans.troll.TrollRegistry;
import com.shenanigans.util.Fx;
import com.shenanigans.util.Sequence;
import com.shenanigans.util.TempBlocks;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.World;
import org.bukkit.WorldBorder;
import org.bukkit.block.Block;
import org.bukkit.block.data.BlockData;
import org.bukkit.block.data.Openable;
import org.bukkit.entity.FallingBlock;
import org.bukkit.entity.Player;
import org.bukkit.entity.TNTPrimed;

import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

/** Cages, fake ores, haunted doors, fake TNT and block-breaking trolls. */
public final class WorldTrolls {

    private WorldTrolls() {
    }

    private static final Set<Material> STONE_LIKE = EnumSet.of(
            Material.STONE, Material.GRANITE, Material.DIORITE, Material.ANDESITE, Material.TUFF,
            Material.DEEPSLATE, Material.COBBLESTONE, Material.COBBLED_DEEPSLATE);

    public static void register(TrollRegistry r) {
        Category c = Category.WORLD;

        r.add(c, Troll.instant("cage", "Cage", Material.GLASS,
                        "Traps them in a glass cage for a while. Only fills empty space, nothing gets destroyed.")
                .usage("[block]")
                .does(ctx -> {
                    Player p = ctx.target();
                    Material material = Material.matchMaterial(ctx.arg(0, ctx.cfgString("block", "GLASS")));
                    if (material == null || !material.isBlock() || !material.isSolid()) {
                        material = Material.GLASS;
                    }
                    Block feet = p.getLocation().getBlock();
                    p.teleport(feet.getLocation().add(0.5, 0, 0.5).setDirection(p.getLocation().getDirection()));
                    long ticks = ctx.cfgInt("seconds", 15) * 20L;
                    BlockData data = material.createBlockData();
                    TempBlocks temp = ctx.plugin().tempBlocks();
                    for (int dx = -1; dx <= 1; dx++) {
                        for (int dz = -1; dz <= 1; dz++) {
                            for (int dy = -1; dy <= 2; dy++) {
                                boolean wall = Math.abs(dx) == 1 || Math.abs(dz) == 1;
                                boolean floorOrRoof = dy == -1 || dy == 2;
                                if (wall || floorOrRoof) {
                                    temp.place(feet.getRelative(dx, dy, dz), data, ticks);
                                }
                            }
                        }
                    }
                    Fx.sound(p, Sound.BLOCK_IRON_DOOR_CLOSE, 1f, 0.6f);
                }));

        r.add(c, Troll.instant("webtrap", "Web Trap", Material.COBWEB,
                        "Surrounds them with cobwebs that disappear after a bit.")
                .does(ctx -> {
                    Player p = ctx.target();
                    Block feet = p.getLocation().getBlock();
                    long ticks = ctx.cfgInt("seconds", 10) * 20L;
                    BlockData web = Material.COBWEB.createBlockData();
                    for (int dx = -1; dx <= 1; dx++) {
                        for (int dz = -1; dz <= 1; dz++) {
                            for (int dy = 0; dy <= 1; dy++) {
                                ctx.plugin().tempBlocks().place(feet.getRelative(dx, dy, dz), web, ticks);
                            }
                        }
                    }
                    Fx.sound(p, Sound.ENTITY_SPIDER_AMBIENT, 1f, 0.8f);
                }));

        r.add(c, Troll.instant("anvilrain", "Anvil Rain", Material.ANVIL,
                        "Anvils fall from the sky on them! They don't leave blocks behind. Damage is capped in the config.")
                .does(ctx -> {
                    int count = ctx.cfgInt("count", 10);
                    boolean hurt = ctx.cfgBool("hurt", true);
                    int maxDamage = ctx.cfgInt("max-damage", 4);
                    Sequence seq = Sequence.on(ctx.plugin(), ctx.target());
                    for (int i = 0; i < count; i++) {
                        seq.at(i * 5L, p -> {
                            Location spot = Fx.around(p.getLocation(), Fx.rng().nextDouble(0, 1.5)).add(0, 12, 0);
                            FallingBlock anvil = p.getWorld().spawn(spot, FallingBlock.class, fb -> {
                                fb.setBlockData(Material.ANVIL.createBlockData());
                                fb.setDropItem(false);
                                fb.setHurtEntities(hurt);
                                fb.setDamagePerBlock(1f);
                                fb.setMaxDamage(maxDamage);
                            });
                            ctx.plugin().manager().markHarmless(anvil);
                            ctx.plugin().manager().removeLater(anvil, 200);
                        });
                    }
                    seq.run();
                }));

        r.add(c, Troll.instant("fakediamonds", "Fool's Diamonds", Material.DIAMOND_ORE,
                        "Stone around them turns into diamond ore... but only on THEIR screen. Mining it gives nothing.")
                .does(ctx -> {
                    Player p = ctx.target();
                    int radius = ctx.cfgInt("radius", 6);
                    double chance = ctx.cfgDouble("chance", 0.25);
                    List<Location> faked = new ArrayList<>();
                    Block center = p.getLocation().getBlock();
                    for (int dx = -radius; dx <= radius; dx++) {
                        for (int dy = -radius; dy <= radius; dy++) {
                            for (int dz = -radius; dz <= radius; dz++) {
                                Block b = center.getRelative(dx, dy, dz);
                                if (!STONE_LIKE.contains(b.getType()) || Fx.rng().nextDouble() > chance) {
                                    continue;
                                }
                                boolean deep = b.getType() == Material.DEEPSLATE || b.getType() == Material.COBBLED_DEEPSLATE;
                                p.sendBlockChange(b.getLocation(),
                                        (deep ? Material.DEEPSLATE_DIAMOND_ORE : Material.DIAMOND_ORE).createBlockData());
                                faked.add(b.getLocation());
                            }
                        }
                    }
                    if (faked.isEmpty()) {
                        ctx.fail("There's no stone near " + p.getName());
                        return;
                    }
                    Bukkit.getScheduler().runTaskLater(ctx.plugin(), () -> {
                        if (p.isOnline()) {
                            for (Location loc : faked) {
                                p.sendBlockChange(loc, loc.getBlock().getBlockData());
                            }
                        }
                    }, ctx.cfgInt("seconds", 60) * 20L);
                }));

        r.add(c, Troll.toggle("border", "Tiny World Border", Material.BARRIER,
                        "A world border appears just around them (only for them). They can't walk out of it.")
                .seconds(30)
                .onStart(at -> {
                    Player p = at.player();
                    WorldBorder border = Bukkit.createWorldBorder();
                    border.setCenter(p.getLocation().getBlockX() + 0.5, p.getLocation().getBlockZ() + 0.5);
                    border.setSize(at.ctx().cfgDouble("size", 3.0));
                    border.setWarningDistance(0);
                    p.setWorldBorder(border);
                    Fx.sound(p, Sound.BLOCK_BEACON_ACTIVATE, 1f, 0.6f);
                })
                .onStop(at -> {
                    Player p = at.player();
                    if (p != null) {
                        p.setWorldBorder(null);
                    }
                }));

        r.add(c, Troll.toggle("haunt", "Haunted", Material.SOUL_LANTERN,
                        "Doors and trapdoors near them open and close by themselves, with spooky noises.")
                .seconds(60)
                .every(25, WorldTrolls::hauntTick));

        r.add(c, Troll.instant("tnt", "Fake TNT", Material.TNT,
                        "Lit TNT appears at their feet... and goes boom. Only it's fake: no damage, no broken blocks.")
                .does(ctx -> {
                    Player p = ctx.target();
                    TNTPrimed tnt = p.getWorld().spawn(p.getLocation(), TNTPrimed.class,
                            t -> t.setFuseTicks(ctx.cfgInt("fuse-ticks", 50)));
                    ctx.plugin().manager().markHarmless(tnt);
                }));

        r.add(c, Troll.instant("lightning", "Lightning", Material.LIGHTNING_ROD,
                        "Strikes lightning on them. Harmless by default (\"real: true\" in the config to make it hurt).")
                .does(ctx -> {
                    Player p = ctx.target();
                    if (ctx.cfgBool("real", false)) {
                        p.getWorld().strikeLightning(p.getLocation());
                    } else {
                        p.getWorld().strikeLightningEffect(p.getLocation());
                    }
                }));

        r.add(c, Troll.instant("fire", "Set on Fire", Material.FLINT_AND_STEEL,
                        "Sets them on fire for a few seconds.")
                .does(ctx -> ctx.target().setFireTicks(ctx.cfgInt("seconds", 5) * 20)));

        r.add(c, Troll.toggle("dirtdrops", "Dirt Drops", Material.DIRT,
                "Every block they mine drops... dirt. Diamond ore? Dirt."));

        r.add(c, Troll.toggle("nomine", "Butter Pickaxe", Material.GOLDEN_PICKAXE,
                        "They can't break any blocks. The blocks just pop back like lag.")
                .seconds(60));

        r.add(c, Troll.toggle("noplace", "Slippery Blocks", Material.SCAFFOLDING,
                        "They can't place blocks. Every block just vanishes back into their hand.")
                .seconds(60));

        r.add(c, Troll.toggle("regen", "Blocks Grow Back", Material.MOSS_BLOCK,
                        "Blocks they break come right back a second later (and drop nothing).")
                .seconds(120));

        r.add(c, Troll.toggle("boompick", "Explosive Mining", Material.NETHERITE_PICKAXE,
                        "Every block they break goes off like TNT. Only visually, nothing else gets destroyed.")
                .seconds(120));
    }

    private static void hauntTick(ActiveTroll at) {
        Player p = at.player();
        Block center = p.getLocation().getBlock();
        List<Block> openables = new ArrayList<>();
        int radius = 7;
        for (int dx = -radius; dx <= radius; dx++) {
            for (int dy = -3; dy <= 3; dy++) {
                for (int dz = -radius; dz <= radius; dz++) {
                    Block b = center.getRelative(dx, dy, dz);
                    if (b.getBlockData() instanceof Openable) {
                        openables.add(b);
                    }
                }
            }
        }
        if (!openables.isEmpty()) {
            Block b = Fx.pick(openables);
            Openable data = (Openable) b.getBlockData();
            data.setOpen(!data.isOpen());
            b.setBlockData(data, true);
            String name = b.getType().name().toLowerCase(Locale.ROOT);
            Sound sound;
            if (name.contains("iron_door")) {
                sound = data.isOpen() ? Sound.BLOCK_IRON_DOOR_OPEN : Sound.BLOCK_IRON_DOOR_CLOSE;
            } else if (name.contains("trapdoor")) {
                sound = data.isOpen() ? Sound.BLOCK_WOODEN_TRAPDOOR_OPEN : Sound.BLOCK_WOODEN_TRAPDOOR_CLOSE;
            } else if (name.contains("gate")) {
                sound = data.isOpen() ? Sound.BLOCK_FENCE_GATE_OPEN : Sound.BLOCK_FENCE_GATE_CLOSE;
            } else {
                sound = data.isOpen() ? Sound.BLOCK_WOODEN_DOOR_OPEN : Sound.BLOCK_WOODEN_DOOR_CLOSE;
            }
            b.getWorld().playSound(b.getLocation(), sound, 1f, 0.8f);
        }
        if (Fx.rng().nextInt(3) == 0) {
            Sound spooky = Fx.pick(Sound.AMBIENT_CAVE, Sound.ENTITY_GHAST_AMBIENT, Sound.BLOCK_CHEST_OPEN,
                    Sound.ENTITY_ZOMBIE_AMBIENT, Sound.ENTITY_PHANTOM_AMBIENT);
            Fx.soundAt(p, Fx.around(p.getLocation(), 4), spooky, 0.6f, 0.7f);
        }
        if (Fx.rng().nextInt(4) == 0) {
            World world = p.getWorld();
            world.spawnParticle(Particle.SOUL, Fx.around(p.getLocation(), 2).add(0, 1, 0), 8, 0.3, 0.5, 0.3, 0.01);
        }
    }
}
