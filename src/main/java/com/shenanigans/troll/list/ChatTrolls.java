package com.shenanigans.troll.list;

import com.shenanigans.troll.Category;
import com.shenanigans.troll.Troll;
import com.shenanigans.troll.TrollRegistry;
import com.shenanigans.util.Fx;
import com.shenanigans.util.Text;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.TextColor;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.ThreadLocalRandom;

/**
 * Chat trolls. The toggles here are applied in TrollListener when the player chats
 * (see {@link #transform}).
 */
public final class ChatTrolls {

    private ChatTrolls() {
    }

    /** Toggle ids that rewrite the player's chat message, in the order they're applied. */
    public static final List<String> REWRITERS = List.of("silly", "scramble", "backwards", "uwu", "caps");

    private static final List<String> UWU_FACES = List.of(" uwu", " owo", " >w<", " ^w^", " (◕ᴥ◕)", " x3", " rawr~");

    public static void register(TrollRegistry r) {
        Category c = Category.CHAT;

        r.add(c, Troll.toggle("uwu", "UwU Chat", Material.PINK_WOOL,
                "Evewything they type gets uwu-ified. Hewwo! >w<"));

        r.add(c, Troll.toggle("backwards", "Backwards Chat", Material.REPEATER,
                ".sdrawkcab tuo semoc yllacigam yas yeht gnihtyrevE"));

        r.add(c, Troll.toggle("caps", "CAPS LOCK", Material.REDSTONE_LAMP,
                "EVERYTHING THEY SAY IS SHOUTED!!!"));

        r.add(c, Troll.toggle("scramble", "Word Scramble", Material.BOOK,
                "Their words come out in a random order. Yoda would be proud."));

        r.add(c, Troll.toggle("silly", "Embarrassing Chat", Material.CAKE,
                "Whatever they type gets replaced with something embarrassing from silly-phrases in the config."));

        r.add(c, Troll.toggle("shadowmute", "Shadow Mute", Material.GRAY_DYE,
                "They can still chat and see their own messages... but nobody else can. They'll wonder why everyone's ignoring them."));

        r.add(c, Troll.instant("chatwipe", "Chat Wipe", Material.SPONGE,
                        "Pushes their whole chat off the screen with blank lines.")
                .does(ctx -> {
                    Player p = ctx.target();
                    for (int i = 0; i < 120; i++) {
                        p.sendMessage(Component.empty());
                    }
                    p.sendMessage(Text.mm(ctx.cfgString("after", "<gray><italic>where did your chat go?")));
                }));

        r.add(c, Troll.instant("spam", "Rainbow Spam", Material.FIREWORK_STAR,
                        "Floods their chat with rainbow \"TROLLED\" messages.")
                .usage("[word]")
                .does(ctx -> {
                    Player p = ctx.target();
                    String word = ctx.joined(ctx.cfgString("word", "TROLLED"));
                    ThreadLocalRandom rng = ThreadLocalRandom.current();
                    for (int i = 0; i < 40; i++) {
                        StringBuilder line = new StringBuilder();
                        for (int w = 0; w < rng.nextInt(1, 6); w++) {
                            line.append(word).append(' ');
                        }
                        p.sendMessage(Component.text(line.toString().trim(),
                                TextColor.color(rng.nextInt(0x1000000))));
                    }
                    Fx.sound(p, Sound.ENTITY_CHICKEN_EGG, 1f, 1.5f);
                }));
    }

    /** Apply one rewrite troll to a chat message. */
    public static String transform(String trollId, String message, String sillyPhrase) {
        return switch (trollId) {
            case "silly" -> sillyPhrase;
            case "scramble" -> scramble(message);
            case "backwards" -> new StringBuilder(message).reverse().toString();
            case "uwu" -> uwu(message);
            case "caps" -> message.toUpperCase(Locale.ROOT) + "!!!";
            default -> message;
        };
    }

    private static String scramble(String message) {
        List<String> words = new ArrayList<>(Arrays.asList(message.split(" ")));
        Collections.shuffle(words);
        return String.join(" ", words);
    }

    private static String uwu(String message) {
        String out = message
                .replace("r", "w").replace("l", "w")
                .replace("R", "W").replace("L", "W")
                .replace("na", "nya").replace("ne", "nye").replace("no", "nyo")
                .replace("ove", "uv");
        ThreadLocalRandom rng = ThreadLocalRandom.current();
        if (!out.isEmpty() && Character.isLetter(out.charAt(0)) && rng.nextInt(3) == 0) {
            out = out.charAt(0) + "-" + out;
        }
        return out + UWU_FACES.get(rng.nextInt(UWU_FACES.size()));
    }
}
