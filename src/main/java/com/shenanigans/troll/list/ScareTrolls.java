package com.shenanigans.troll.list;

import com.shenanigans.troll.Category;
import com.shenanigans.troll.Troll;
import com.shenanigans.troll.TrollRegistry;
import com.shenanigans.util.Fx;
import com.shenanigans.util.Sequence;
import com.shenanigans.util.Text;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.EntityEffect;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.potion.PotionEffectType;

import java.util.List;

/** Weird screens, jumpscares and creepy sounds. All sounds here are only heard by the target. */
public final class ScareTrolls {

    private ScareTrolls() {
    }

    private static final List<Sound> ANNOYING = List.of(
            Sound.ENTITY_GOAT_SCREAMING_AMBIENT, Sound.ENTITY_CHICKEN_EGG, Sound.ENTITY_VILLAGER_NO,
            Sound.BLOCK_NOTE_BLOCK_DIDGERIDOO, Sound.ENTITY_CAT_HISS, Sound.ENTITY_PIG_AMBIENT,
            Sound.BLOCK_ANVIL_LAND, Sound.ENTITY_GHAST_HURT, Sound.BLOCK_NOTE_BLOCK_BELL,
            Sound.ENTITY_PARROT_IMITATE_CREEPER, Sound.ENTITY_LLAMA_SPIT, Sound.BLOCK_GLASS_BREAK);

    public static void register(TrollRegistry r) {
        Category c = Category.SCARES;

        r.add(c, Troll.instant("demo", "Demo Screen", Material.MAP,
                        "Pops up the \"You're playing the Demo version\" screen. Classic.")
                .does(ctx -> ctx.target().showDemoScreen()));

        r.add(c, Troll.instant("credits", "End Credits", Material.DRAGON_EGG,
                        "Shows the End Poem and credits like they just beat the game.")
                .does(ctx -> ctx.target().showWinScreen()));

        r.add(c, Troll.instant("guardian", "Elder Guardian", Material.PRISMARINE_SHARD,
                        "The Elder Guardian ghost face flies into their screen with that sound.")
                .does(ctx -> ctx.target().showElderGuardian(false)));

        r.add(c, Troll.instant("jumpscare", "Jumpscare", Material.CARVED_PUMPKIN,
                        "Darkness... silence... then a screaming face. Turn your volume down. Or don't.")
                .does(ctx -> Sequence.on(ctx.plugin(), ctx.target())
                        .at(0, p -> {
                            Fx.effect(p, PotionEffectType.DARKNESS, 80, 0);
                            Fx.sound(p, Sound.ENTITY_WARDEN_HEARTBEAT, 1f, 1f);
                        })
                        .at(20, p -> Fx.sound(p, Sound.ENTITY_WARDEN_HEARTBEAT, 1f, 1.1f))
                        .at(35, p -> {
                            p.showElderGuardian(true);
                            Fx.sound(p, Sound.ENTITY_GHAST_SCREAM, 1f, 0.6f);
                            Fx.sound(p, Sound.ENTITY_WARDEN_ROAR, 1f, 1.2f);
                            Fx.sound(p, Sound.ENTITY_ENDERMAN_SCREAM, 1f, 0.5f);
                            Fx.title(p, "<dark_red><bold>BOO!", "", 0, 20, 10);
                        })
                        .run()));

        r.add(c, Troll.instant("herobrine", "Herobrine Visit", Material.WITHER_SKELETON_SKULL,
                        "Herobrine \"joins\", whispers creepy stuff, lightning strikes behind them, then he leaves. Only they see it.")
                .does(ctx -> Sequence.on(ctx.plugin(), ctx.target())
                        .at(0, p -> p.sendMessage(Component.translatable("multiplayer.player.joined",
                                NamedTextColor.YELLOW, Component.text("Herobrine"))))
                        .at(40, p -> {
                            Location spot = Fx.behind(p, 8);
                            p.getWorld().strikeLightningEffect(spot);
                        })
                        .at(80, p -> p.sendMessage(herobrineSays("I see you.")))
                        .at(120, p -> {
                            Fx.effect(p, PotionEffectType.BLINDNESS, 60, 0);
                            Fx.sound(p, Sound.AMBIENT_CAVE, 1f, 0.7f);
                        })
                        .at(130, p -> Fx.soundAt(p, Fx.behind(p, 2), Sound.BLOCK_GRAVEL_STEP, 1f, 0.8f))
                        .at(140, p -> Fx.soundAt(p, Fx.behind(p, 1.5), Sound.BLOCK_GRAVEL_STEP, 1f, 0.8f))
                        .at(150, p -> Fx.soundAt(p, Fx.behind(p, 1), Sound.BLOCK_GRAVEL_STEP, 1f, 0.8f))
                        .at(180, p -> p.sendMessage(herobrineSays("Look behind you.")))
                        .at(220, p -> {
                            Fx.sound(p, Sound.ENTITY_ENDERMAN_STARE, 1f, 0.5f);
                            Fx.title(p, "", "<dark_red><obfuscated>herobrine was here", 5, 30, 10);
                        })
                        .at(260, p -> p.sendMessage(Component.translatable("multiplayer.player.left",
                                NamedTextColor.YELLOW, Component.text("Herobrine"))))
                        .run()));

        r.add(c, Troll.instant("title", "Big Title", Material.OAK_SIGN,
                        "Puts a giant title on their screen. Type your own or use the default.")
                .usage("[text]")
                .does(ctx -> {
                    String title = ctx.args().length > 0 ? Text.escape(ctx.joined(""))
                            : ctx.cfgString("default-title", "<gradient:red:gold>You've been trolled!");
                    String subtitle = ctx.args().length > 0 ? "" : ctx.cfgString("default-subtitle", "<gray>get rekt");
                    Fx.title(ctx.target(), title, subtitle, 10, 60, 20);
                    Fx.sound(ctx.target(), Sound.ENTITY_PLAYER_LEVELUP, 1f, 1f);
                }));

        r.add(c, Troll.instant("totem", "Fake Totem Pop", Material.TOTEM_OF_UNDYING,
                        "Plays the Totem of Undying animation in their face like they just almost died.")
                .does(ctx -> {
                    Player p = ctx.target();
                    p.playEffect(EntityEffect.PROTECTED_FROM_DEATH);
                    p.getWorld().spawnParticle(Particle.TOTEM_OF_UNDYING, p.getLocation().add(0, 1, 0), 60, 0.5, 0.8, 0.5, 0.4);
                }));

        r.add(c, Troll.instant("virus", "Fake Virus Download", Material.SCULK_SENSOR,
                        "Shows a fake \"Downloading free_robux.exe\" progress bar, then \"VIRUS INSTALLED\".")
                .does(ctx -> {
                    String file = ctx.cfgString("file-name", "free_robux.exe");
                    Sequence seq = Sequence.on(ctx.plugin(), ctx.target());
                    for (int step = 0; step <= 20; step++) {
                        int percent = step * 5;
                        seq.at(step * 6L, p -> {
                            int bars = percent / 5;
                            String bar = "<green>" + "|".repeat(bars) + "<dark_gray>" + "|".repeat(20 - bars);
                            Fx.actionBar(p, "<gray>Downloading <red>" + Text.escape(file) + "</red> " + bar + " <white>" + percent + "%");
                            Fx.sound(p, Sound.BLOCK_NOTE_BLOCK_HAT, 0.6f, 0.5f + percent / 100f);
                        });
                    }
                    seq.at(135, p -> {
                        Fx.title(p, "<dark_red><bold>VIRUS INSTALLED", "<gray>your pc is now 100% more trolled", 5, 60, 20);
                        Fx.sound(p, Sound.ENTITY_VILLAGER_NO, 1f, 0.6f);
                        Fx.sound(p, Sound.BLOCK_BEACON_DEACTIVATE, 1f, 1f);
                    });
                    seq.run();
                }));

        // ---------------- sounds (only the target hears these) ----------------

        r.add(c, Troll.instant("hiss", "Creeper Hiss", Material.GUNPOWDER,
                        "Plays a creeper hiss right behind them. Watch them run.")
                .does(ctx -> Fx.soundAt(ctx.target(), Fx.behind(ctx.target(), 1.5), Sound.ENTITY_CREEPER_PRIMED, 1f, 1f)));

        r.add(c, Troll.instant("cave", "Cave Noise", Material.COBBLED_DEEPSLATE,
                        "One of those creepy cave ambience sounds. Nobody knows what they are.")
                .does(ctx -> Fx.sound(ctx.target(), Sound.AMBIENT_CAVE, 1f, Fx.rng().nextFloat(0.6f, 1.1f))));

        r.add(c, Troll.instant("knock", "Knock Knock", Material.OAK_DOOR,
                        "Someone knocks on a door near them. Three times.")
                .does(ctx -> {
                    Location spot = Fx.around(ctx.target().getLocation(), 4);
                    Sequence.on(ctx.plugin(), ctx.target())
                            .at(0, p -> Fx.soundAt(p, spot, Sound.ENTITY_ZOMBIE_ATTACK_WOODEN_DOOR, 0.8f, 1.3f))
                            .at(9, p -> Fx.soundAt(p, spot, Sound.ENTITY_ZOMBIE_ATTACK_WOODEN_DOOR, 0.8f, 1.3f))
                            .at(18, p -> Fx.soundAt(p, spot, Sound.ENTITY_ZOMBIE_ATTACK_WOODEN_DOOR, 0.8f, 1.3f))
                            .run();
                }));

        r.add(c, Troll.instant("footsteps", "Footsteps", Material.LEATHER_BOOTS,
                        "Footsteps creep up behind them, getting closer and closer...")
                .does(ctx -> {
                    Sequence seq = Sequence.on(ctx.plugin(), ctx.target());
                    for (int i = 0; i < 8; i++) {
                        double distance = 6 - i * 0.7;
                        seq.at(i * 9L, p -> Fx.soundAt(p, Fx.behind(p, distance), Sound.BLOCK_STONE_STEP, 1f, 0.9f));
                    }
                    seq.run();
                }));

        r.add(c, Troll.instant("explosion", "Explosion Sound", Material.TNT_MINECART,
                        "A huge explosion goes off next to them. Nothing actually explodes.")
                .does(ctx -> {
                    Player p = ctx.target();
                    Fx.soundAt(p, Fx.around(p.getLocation(), 3), Sound.ENTITY_GENERIC_EXPLODE, 1f, 0.9f);
                }));

        r.add(c, Troll.instant("ghast", "Ghast Scream", Material.GHAST_TEAR,
                        "A ghast screams in their ears. Works best in the Overworld.")
                .does(ctx -> Fx.sound(ctx.target(), Sound.ENTITY_GHAST_SCREAM, 1f, 1f)));

        r.add(c, Troll.instant("enderman", "Enderman Stare", Material.ENDERMAN_SPAWN_EGG,
                        "The \"you looked at an enderman\" noise, then a teleport sound right behind them.")
                .does(ctx -> Sequence.on(ctx.plugin(), ctx.target())
                        .at(0, p -> Fx.sound(p, Sound.ENTITY_ENDERMAN_STARE, 1f, 1f))
                        .at(40, p -> Fx.soundAt(p, Fx.behind(p, 1), Sound.ENTITY_ENDERMAN_TELEPORT, 1f, 1f))
                        .run()));

        r.add(c, Troll.toggle("noise", "Noise Spam", Material.NOTE_BLOCK,
                        "A wall of random annoying sounds (goat screams, villager no's...).")
                .seconds(10)
                .every(4, at -> {
                    Player p = at.player();
                    Fx.sound(p, Fx.pick(ANNOYING), 0.8f, Fx.rng().nextFloat(0.5f, 2f));
                }));
    }

    private static Component herobrineSays(String message) {
        return Component.translatable("chat.type.text", Component.text("Herobrine"), Component.text(message));
    }
}
