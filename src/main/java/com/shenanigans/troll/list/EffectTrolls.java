package com.shenanigans.troll.list;

import com.shenanigans.troll.Category;
import com.shenanigans.troll.Troll;
import com.shenanigans.troll.TrollRegistry;
import com.shenanigans.util.Fx;
import com.shenanigans.util.Sequence;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.potion.PotionEffectType;

/** Potion effects and status trolls. None of these can kill anyone. */
public final class EffectTrolls {

    private EffectTrolls() {
    }

    public static void register(TrollRegistry r) {
        Category c = Category.EFFECTS;

        r.add(c, Troll.instant("blind", "Blackout", Material.INK_SAC,
                        "Blindness + darkness. They can barely see their own hands.")
                .does(ctx -> {
                    int ticks = ctx.cfgInt("seconds", 10) * 20;
                    Fx.effect(ctx.target(), PotionEffectType.BLINDNESS, ticks, 0);
                    Fx.effect(ctx.target(), PotionEffectType.DARKNESS, ticks, 0);
                }));

        r.add(c, Troll.instant("starve", "Starve", Material.ROTTEN_FLESH,
                        "Their hunger bar drops to zero instantly.")
                .does(ctx -> {
                    Player p = ctx.target();
                    p.setFoodLevel(0);
                    p.setSaturation(0f);
                    Fx.sound(p, Sound.ENTITY_PLAYER_BURP, 1f, 0.5f);
                }));

        r.add(c, Troll.instant("heartattack", "Heart Attack", Material.REDSTONE_BLOCK,
                        "Drops them to half a heart (never kills) with a heartbeat sound. Run, Forrest, run!")
                .does(ctx -> {
                    Player p = ctx.target();
                    if (p.getHealth() > 1.0) {
                        p.setHealth(1.0);
                    }
                    p.playHurtAnimation(0f);
                    Sequence.on(ctx.plugin(), p)
                            .at(0, pl -> Fx.sound(pl, Sound.ENTITY_WARDEN_HEARTBEAT, 1f, 1f))
                            .at(12, pl -> Fx.sound(pl, Sound.ENTITY_WARDEN_HEARTBEAT, 1f, 1.2f))
                            .at(22, pl -> Fx.sound(pl, Sound.ENTITY_WARDEN_HEARTBEAT, 1f, 1.4f))
                            .run();
                }));

        r.add(c, Troll.instant("ouch", "Phantom Pain", Material.SWEET_BERRIES,
                        "They flinch and hear the hurt sound like they got hit, but take no damage. Do it a few times.")
                .does(ctx -> Sequence.on(ctx.plugin(), ctx.target())
                        .at(0, MobHitFx::hit)
                        .at(10, MobHitFx::hit)
                        .at(25, MobHitFx::hit)
                        .run()));

        r.add(c, Troll.instant("lazy", "Lazy Day", Material.CAMPFIRE,
                        "Heavy slowness and mining fatigue. Everything takes forever.")
                .does(ctx -> {
                    int ticks = ctx.cfgInt("seconds", 20) * 20;
                    Fx.effect(ctx.target(), PotionEffectType.SLOWNESS, ticks, 3);
                    Fx.effect(ctx.target(), PotionEffectType.MINING_FATIGUE, ticks, 2);
                }));

        r.add(c, Troll.instant("dizzy", "Dizzy", Material.FERMENTED_SPIDER_EYE,
                        "Strong nausea that makes their screen warp and wobble.")
                .does(ctx -> Fx.effect(ctx.target(), PotionEffectType.NAUSEA, ctx.cfgInt("seconds", 15) * 20, 1)));

        r.add(c, Troll.instant("hunger", "Hungry Hungry", Material.PUFFERFISH,
                        "The Hunger effect so their food bar drains super fast.")
                .does(ctx -> Fx.effect(ctx.target(), PotionEffectType.HUNGER, ctx.cfgInt("seconds", 30) * 20, 9)));
    }

    /** Small helper so the steps above read nicely. */
    private static final class MobHitFx {
        private static void hit(Player p) {
            p.playHurtAnimation(Fx.rng().nextFloat() * 360f);
            Fx.sound(p, Sound.ENTITY_PLAYER_HURT, 1f, 1f);
        }
    }
}
