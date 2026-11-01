package com.shenanigans.troll.list;

import com.shenanigans.troll.Category;
import com.shenanigans.troll.Troll;
import com.shenanigans.troll.TrollRegistry;
import com.shenanigans.util.Fx;
import com.shenanigans.util.Sequence;
import com.shenanigans.util.Text;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.event.HoverEvent;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.entity.Player;

import java.util.List;
import java.util.Locale;

/** Fake bans, fake crashes, fake OP, fake deaths... everything that LOOKS real but isn't. */
public final class FakeOutTrolls {

    private FakeOutTrolls() {
    }

    private static final List<String> DEATH_KEYS = List.of(
            "death.attack.cactus",                 // was pricked to death
            "death.attack.anvil",                  // was squashed by a falling anvil
            "death.attack.sweetBerryBush",         // was poked to death by a sweet berry bush
            "death.attack.outOfWorld",             // fell out of the world
            "death.attack.starve",                 // starved to death
            "death.attack.lightningBolt",          // was struck by lightning
            "death.attack.drown",                  // drowned
            "death.attack.flyIntoWall",            // experienced kinetic energy
            "death.attack.cramming",               // was squished too much
            "death.attack.dryout",                 // died from dehydration
            "death.attack.freeze",                 // froze to death
            "death.attack.stalagmite",             // was impaled on a stalagmite
            "death.fell.accident.generic",         // fell from a high place
            "death.attack.inWall",                 // suffocated in a wall
            "death.attack.hotFloor"                // discovered the floor was lava
    );

    public static void register(TrollRegistry r) {
        Category c = Category.FAKE;

        r.add(c, Troll.instant("fakeop", "Fake OP", Material.COMMAND_BLOCK,
                        "Shows them the real-looking \"Made you a server operator\" message. They are not op.")
                .does(ctx -> ctx.target().sendMessage(Text.adminEcho(
                        Component.translatable("commands.op.success", Component.text(ctx.target().getName()))))));

        r.add(c, Troll.instant("fakedeop", "Fake De-OP", Material.CHAIN_COMMAND_BLOCK,
                        "Tells them they are no longer a server operator. Great on actual admins.")
                .does(ctx -> ctx.target().sendMessage(Text.adminEcho(
                        Component.translatable("commands.deop.success", Component.text(ctx.target().getName()))))));

        r.add(c, Troll.instant("fakegamemode", "Fake Creative", Material.GRASS_BLOCK,
                        "\"Your game mode has been updated to Creative Mode\" ...but it hasn't.")
                .usage("[survival|creative|adventure|spectator]")
                .does(ctx -> {
                    String mode = ctx.arg(0, "creative").toLowerCase(Locale.ROOT);
                    if (!List.of("survival", "creative", "adventure", "spectator").contains(mode)) {
                        mode = "creative";
                    }
                    ctx.target().sendMessage(Component.translatable("gameMode.changed",
                            Component.translatable("gameMode." + mode)));
                }));

        r.add(c, Troll.instant("fakeban", "Fake Ban", Material.IRON_BARS,
                        "Kicks them with a real-looking \"You are banned from this server\" screen. They can rejoin.")
                .usage("[reason]")
                .does(ctx -> {
                    String reason = ctx.joined(ctx.cfgString("reason", "Being too good at the game."));
                    if (ctx.cfgBool("broadcast", true)) {
                        Bukkit.broadcast(Text.mm(ctx.cfgString("broadcast-message",
                                        "<red><player> was banned by an admin. Reason: <reason>"),
                                Text.ph("player", ctx.target().getName()), Text.ph("reason", reason)));
                    }
                    ctx.target().kick(Component.translatable("multiplayer.disconnect.banned.reason",
                            Component.text(reason)));
                }));

        r.add(c, Troll.instant("fakecrash", "Fake Crash", Material.REDSTONE,
                        "Disconnects them with a scary-looking Internal Exception error. They can rejoin.")
                .does(ctx -> {
                    List<String> messages = ctx.cfgList("messages", List.of(
                            "Internal Exception: java.net.SocketException: Connection reset"));
                    ctx.target().kick(Text.mm(ctx.pick(messages)));
                }));

        r.add(c, Troll.instant("fakejoin", "Fake Join", Material.LIME_DYE,
                        "Broadcasts \"<name> joined the game\" even though they're already here.")
                .does(ctx -> Bukkit.broadcast(Component.translatable("multiplayer.player.joined",
                        NamedTextColor.YELLOW, Component.text(ctx.target().getName())))));

        r.add(c, Troll.instant("fakeleave", "Fake Leave", Material.RED_DYE,
                        "Broadcasts \"<name> left the game\" while they're still online.")
                .does(ctx -> Bukkit.broadcast(Component.translatable("multiplayer.player.left",
                        NamedTextColor.YELLOW, Component.text(ctx.target().getName())))));

        r.add(c, Troll.instant("fakedeath", "Fake Death", Material.SKELETON_SKULL,
                        "Broadcasts a silly death message (\"was poked to death by a sweet berry bush\") while they're alive.")
                .does(ctx -> {
                    Player p = ctx.target();
                    Bukkit.broadcast(Component.translatable(ctx.pick(DEATH_KEYS), Component.text(p.getName())));
                    p.playHurtAnimation(Fx.rng().nextFloat() * 360f);
                    Fx.sound(p, Sound.ENTITY_PLAYER_HURT, 1f, 1f);
                }));

        r.add(c, Troll.instant("fakeadvancement", "Fake Advancement", Material.GOLDEN_APPLE,
                        "\"<name> has made the advancement [Certified Noob]\" for everyone to see.")
                .usage("[advancement name]")
                .does(ctx -> {
                    String title;
                    String description;
                    if (ctx.args().length > 0) {
                        title = ctx.joined("");
                        description = "Achieved by being " + ctx.target().getName();
                    } else {
                        String entry = ctx.pick(ctx.cfgList("list", List.of("Certified Noob|Die to a chicken")));
                        String[] parts = entry.split("\\|", 2);
                        title = parts[0];
                        description = parts.length > 1 ? parts[1] : "";
                    }
                    Component hover = Component.text(title, NamedTextColor.GREEN)
                            .append(Component.newline())
                            .append(Component.text(description, NamedTextColor.WHITE));
                    Component advancement = Component.text("[" + title + "]", NamedTextColor.GREEN)
                            .hoverEvent(HoverEvent.showText(hover));
                    Bukkit.broadcast(Component.translatable("chat.type.advancement.task",
                            Component.text(ctx.target().getName()), advancement));
                    Fx.sound(ctx.target(), Sound.UI_TOAST_CHALLENGE_COMPLETE, 0.6f, 1f);
                }));

        r.add(c, Troll.instant("fakehacker", "Fake Anticheat Alert", Material.DIAMOND_SWORD,
                        "Broadcasts fake anticheat alerts saying they're using hacks like KillAura or Fly.")
                .does(ctx -> {
                    String format = ctx.cfgString("format",
                            "<dark_gray>[<red>AntiCheat</red>]</dark_gray> <white><player></white> <gray>failed</gray> <red><hack></red> <gray>(VL: <vl>)</gray>");
                    String hack = ctx.pick(ctx.cfgList("hacks", List.of("KillAura", "Fly", "Reach", "X-Ray")));
                    int[] vl = {Fx.rng().nextInt(3, 8)};
                    Sequence seq = Sequence.on(ctx.plugin(), ctx.target());
                    for (int i = 0; i < 3; i++) {
                        seq.at(i * 25L, p -> {
                            vl[0] += Fx.rng().nextInt(4, 12);
                            Bukkit.broadcast(Text.mm(format, Text.ph("player", p.getName()), Text.ph("hack", hack),
                                    Text.ph("vl", String.valueOf(vl[0]))));
                        });
                    }
                    seq.run();
                }));

        r.add(c, Troll.instant("fakechat", "Fake Chat", Material.PAPER,
                        "Makes it look like they said something in chat (only a message, they don't really type it).")
                .usage("[message]")
                .does(ctx -> {
                    String message = ctx.joined(ctx.sillyPhrase());
                    Bukkit.broadcast(Component.translatable("chat.type.text",
                            Component.text(ctx.target().getName()), Component.text(message)));
                }));

        r.add(c, Troll.instant("sudo", "Sudo (Force Chat)", Material.LEAD,
                        "Makes them REALLY send a chat message. (Commands are blocked for safety.)")
                .usage("[message]")
                .does(ctx -> {
                    String message = ctx.joined(ctx.sillyPhrase()).replaceFirst("^/+", "");
                    if (message.isBlank()) {
                        ctx.fail("Message is empty");
                        return;
                    }
                    ctx.target().chat(message);
                }));

        r.add(c, Troll.instant("fakerestart", "Fake Restart", Material.CLOCK,
                        "Counts down \"Server restarting in 10 seconds\" just for them, then kicks them with \"Server closed\".")
                .does(ctx -> {
                    String alert = ctx.cfgString("message", "<red><bold>[Server]</bold> Restarting in <yellow><seconds></yellow> seconds!");
                    Sequence seq = Sequence.on(ctx.plugin(), ctx.target());
                    int[] counts = {10, 5, 3, 2, 1};
                    for (int count : counts) {
                        seq.at((10 - count) * 20L, p -> {
                            p.sendMessage(Text.mm(alert, Text.ph("seconds", String.valueOf(count))));
                            Fx.title(p, "", "<red>Restarting in <yellow>" + count, 0, 25, 5);
                            Fx.sound(p, Sound.BLOCK_NOTE_BLOCK_PLING, 1f, count <= 3 ? 1.5f : 1f);
                        });
                    }
                    seq.at(200, p -> p.kick(Component.translatable("multiplayer.disconnect.server_shutdown")));
                    seq.run();
                }));

        r.add(c, Troll.toggle("fakexp", "Fake XP Wipe", Material.EXPERIENCE_BOTTLE,
                        "Their XP bar suddenly shows level 0. It's only visual and fixes itself.")
                .seconds(20)
                .onStart(at -> {
                    Player p = at.player();
                    p.sendExperienceChange(0f, 0);
                    Fx.sound(p, Sound.ENTITY_ITEM_BREAK, 1f, 0.6f);
                })
                .every(40, at -> at.player().sendExperienceChange(0f, 0))
                .onStop(at -> {
                    Player p = at.player();
                    if (p != null) {
                        p.sendExperienceChange(p.getExp(), p.getLevel());
                    }
                }));

        r.add(c, Troll.toggle("fakelowhp", "Fake Half Heart", Material.SPIDER_EYE,
                        "Their screen shows half a heart and an empty food bar. It's fake and fixes itself.")
                .seconds(10)
                .onStart(at -> {
                    Player p = at.player();
                    p.sendHealthUpdate(1.0, 0, 0f);
                    Fx.sound(p, Sound.ENTITY_PLAYER_HURT, 1f, 0.8f);
                    p.playHurtAnimation(0f);
                })
                .every(10, at -> at.player().sendHealthUpdate(1.0, 0, 0f))
                .onStop(at -> {
                    Player p = at.player();
                    if (p != null) {
                        p.sendHealthUpdate();
                    }
                }));
    }
}
