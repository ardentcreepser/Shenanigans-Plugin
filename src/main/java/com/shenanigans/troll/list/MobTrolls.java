package com.shenanigans.troll.list;

import com.shenanigans.troll.ActiveTroll;
import com.shenanigans.troll.Category;
import com.shenanigans.troll.Troll;
import com.shenanigans.troll.TrollManager;
import com.shenanigans.troll.TrollRegistry;
import com.shenanigans.util.Fx;
import com.shenanigans.util.Sequence;
import org.bukkit.Bukkit;
import org.bukkit.Color;
import org.bukkit.FireworkEffect;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.entity.Bee;
import org.bukkit.entity.Chicken;
import org.bukkit.entity.Cow;
import org.bukkit.entity.Creeper;
import org.bukkit.entity.Firework;
import org.bukkit.entity.Item;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Pig;
import org.bukkit.entity.Player;
import org.bukkit.entity.Sheep;
import org.bukkit.entity.Silverfish;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.FireworkMeta;
import org.bukkit.inventory.meta.LeatherArmorMeta;

import java.util.EnumMap;
import java.util.List;
import java.util.Map;

/** Mob swarms, animal rain, fireworks, disco armor and other fun stuff. */
public final class MobTrolls {

    private MobTrolls() {
    }

    private static final List<Class<? extends LivingEntity>> ANIMALS = List.of(Cow.class, Pig.class, Sheep.class, Chicken.class);

    public static void register(TrollRegistry r) {
        Category c = Category.MOBS;

        r.add(c, Troll.instant("bees", "Angry Bees", Material.BEEHIVE,
                        "A swarm of very angry bees appears around them. The bees leave after a while.")
                .does(ctx -> {
                    Player p = ctx.target();
                    int count = ctx.cfgInt("count", 6);
                    long life = ctx.cfgInt("seconds", 25) * 20L;
                    for (int i = 0; i < count; i++) {
                        Bee bee = p.getWorld().spawn(Fx.around(p.getLocation(), 3).add(0, 1.5, 0), Bee.class, b -> {
                            b.setAnger(Integer.MAX_VALUE);
                            b.setTarget(p);
                            b.setRemoveWhenFarAway(true);
                        });
                        ctx.plugin().manager().removeLater(bee, life);
                    }
                    Fx.sound(p, Sound.ENTITY_BEE_LOOP_AGGRESSIVE, 1f, 1f);
                }));

        r.add(c, Troll.instant("silverfish", "Silverfish Infestation", Material.INFESTED_STONE,
                        "Silverfish pour out around them. They go away after a while.")
                .does(ctx -> {
                    Player p = ctx.target();
                    int count = ctx.cfgInt("count", 10);
                    long life = ctx.cfgInt("seconds", 25) * 20L;
                    for (int i = 0; i < count; i++) {
                        Silverfish fish = p.getWorld().spawn(Fx.around(p.getLocation(), 2.5), Silverfish.class,
                                s -> s.setTarget(p));
                        ctx.plugin().manager().removeLater(fish, life);
                    }
                    Fx.sound(p, Sound.ENTITY_SILVERFISH_AMBIENT, 1f, 1f);
                }));

        r.add(c, Troll.instant("animalrain", "Animal Rain", Material.EGG,
                        "Cows, pigs, sheep and chickens rain from the sky around them. They can't die (no free food) and vanish later.")
                .does(ctx -> {
                    int count = ctx.cfgInt("count", 16);
                    long life = ctx.cfgInt("seconds", 30) * 20L;
                    TrollManager manager = ctx.plugin().manager();
                    Sequence seq = Sequence.on(ctx.plugin(), ctx.target());
                    for (int i = 0; i < count; i++) {
                        seq.at(i * 3L, p -> {
                            Location spot = Fx.around(p.getLocation(), Fx.rng().nextDouble(1, 6)).add(0, 15, 0);
                            LivingEntity animal = p.getWorld().spawn(spot, Fx.pick(ANIMALS), a -> a.setInvulnerable(true));
                            manager.removeLater(animal, life);
                        });
                    }
                    seq.run();
                }));

        r.add(c, Troll.instant("creeper", "Creeper Surprise", Material.CREEPER_HEAD,
                        "A creeper spawns right behind them and ignites. The explosion is fake.")
                .does(ctx -> {
                    Player p = ctx.target();
                    Creeper creeper = p.getWorld().spawn(Fx.behind(p, 1.5), Creeper.class, cr -> {
                        cr.setTarget(p);
                        cr.setMaxFuseTicks(30);
                    });
                    creeper.ignite();
                    ctx.plugin().manager().markHarmless(creeper);
                    ctx.plugin().manager().removeLater(creeper, 200);
                }));

        r.add(c, Troll.instant("fireworks", "Firework Show", Material.FIREWORK_ROCKET,
                        "A little firework show just for them. Fireworks don't hurt anyone.")
                .does(ctx -> {
                    Sequence seq = Sequence.on(ctx.plugin(), ctx.target());
                    for (int i = 0; i < ctx.cfgInt("count", 10); i++) {
                        seq.at(i * 8L, p -> {
                            Location spot = Fx.around(p.getLocation(), Fx.rng().nextDouble(2, 5));
                            Firework fw = p.getWorld().spawn(spot, Firework.class, f -> {
                                FireworkMeta meta = f.getFireworkMeta();
                                meta.addEffect(FireworkEffect.builder()
                                        .with(Fx.pick(FireworkEffect.Type.values()))
                                        .withColor(randomColor(), randomColor())
                                        .withFade(randomColor())
                                        .flicker(Fx.rng().nextBoolean())
                                        .trail(Fx.rng().nextBoolean())
                                        .build());
                                meta.setPower(1);
                                f.setFireworkMeta(meta);
                            });
                            ctx.plugin().manager().markHarmless(fw);
                        });
                    }
                    seq.run();
                }));

        r.add(c, Troll.instant("diamondrain", "Diamond Rain", Material.DIAMOND,
                        "Diamonds rain down around them... but they can't pick any of them up. Pure torture.")
                .does(ctx -> {
                    Material material = Material.matchMaterial(ctx.cfgString("item", "DIAMOND"));
                    if (material == null || !material.isItem()) {
                        material = Material.DIAMOND;
                    }
                    ItemStack stack = new ItemStack(material);
                    Sequence seq = Sequence.on(ctx.plugin(), ctx.target());
                    for (int i = 0; i < ctx.cfgInt("count", 30); i++) {
                        seq.at(i * 2L, p -> {
                            Location spot = Fx.around(p.getLocation(), Fx.rng().nextDouble(0.5, 4)).add(0, 8, 0);
                            Item item = p.getWorld().dropItem(spot, stack, it -> {
                                it.setCanPlayerPickup(false);
                                it.setCanMobPickup(false);
                            });
                            ctx.plugin().manager().removeLater(item, 200);
                        });
                    }
                    seq.run();
                }));

        r.add(c, Troll.toggle("disco", "Disco Armor", Material.LEATHER_HELMET,
                        "Everyone else sees them in flashing rainbow leather armor. Their real armor isn't touched.")
                .seconds(60)
                .every(4, MobTrolls::discoTick)
                .onStop(at -> {
                    Player p = at.player();
                    if (p == null) {
                        return;
                    }
                    Map<EquipmentSlot, ItemStack> real = new EnumMap<>(EquipmentSlot.class);
                    for (EquipmentSlot slot : ARMOR_SLOTS) {
                        real.put(slot, p.getInventory().getItem(slot));
                    }
                    for (Player viewer : Bukkit.getOnlinePlayers()) {
                        if (!viewer.equals(p)) {
                            viewer.sendEquipmentChange(p, real);
                        }
                    }
                }));

        r.add(c, Troll.toggle("glow", "Spotlight", Material.GLOWSTONE_DUST,
                        "Makes them glow so everyone can see them through walls. No more hiding.")
                .seconds(60)
                .onStart(at -> at.player().setGlowing(true))
                .onStop(at -> {
                    if (at.player() != null) {
                        at.player().setGlowing(false);
                    }
                }));

        r.add(c, Troll.toggle("lonely", "Lonely", Material.TINTED_GLASS,
                        "Every other player disappears from their screen. Where did everyone go?")
                .seconds(60)
                .onStart(at -> {
                    Player p = at.player();
                    for (Player other : Bukkit.getOnlinePlayers()) {
                        if (!other.equals(p)) {
                            p.hidePlayer(at.plugin(), other);
                        }
                    }
                })
                .onStop(at -> {
                    Player p = at.player();
                    if (p == null) {
                        return;
                    }
                    for (Player other : Bukkit.getOnlinePlayers()) {
                        p.showPlayer(at.plugin(), other);
                    }
                }));
    }

    private static final List<EquipmentSlot> ARMOR_SLOTS = List.of(
            EquipmentSlot.HEAD, EquipmentSlot.CHEST, EquipmentSlot.LEGS, EquipmentSlot.FEET);

    private static void discoTick(ActiveTroll at) {
        Player p = at.player();
        Color color = randomColor();
        Map<EquipmentSlot, ItemStack> fake = new EnumMap<>(EquipmentSlot.class);
        fake.put(EquipmentSlot.HEAD, dyed(Material.LEATHER_HELMET, color));
        fake.put(EquipmentSlot.CHEST, dyed(Material.LEATHER_CHESTPLATE, color));
        fake.put(EquipmentSlot.LEGS, dyed(Material.LEATHER_LEGGINGS, color));
        fake.put(EquipmentSlot.FEET, dyed(Material.LEATHER_BOOTS, color));
        for (Player viewer : p.getWorld().getPlayers()) {
            if (!viewer.equals(p)) {
                viewer.sendEquipmentChange(p, fake);
            }
        }
    }

    private static ItemStack dyed(Material material, Color color) {
        ItemStack item = new ItemStack(material);
        if (item.getItemMeta() instanceof LeatherArmorMeta meta) {
            meta.setColor(color);
            item.setItemMeta(meta);
        }
        return item;
    }

    private static Color randomColor() {
        return Color.fromRGB(Fx.rng().nextInt(256), Fx.rng().nextInt(256), Fx.rng().nextInt(256));
    }
}
