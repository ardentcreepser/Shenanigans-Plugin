package com.shenanigans.util;

import com.shenanigans.ShenanigansPlugin;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.ComponentLike;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import net.kyori.adventure.text.minimessage.MiniMessage;
import net.kyori.adventure.text.minimessage.tag.resolver.Placeholder;
import net.kyori.adventure.text.minimessage.tag.resolver.TagResolver;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;
import org.bukkit.command.CommandSender;

import java.util.ArrayList;
import java.util.List;

/** Text helpers. All colors use MiniMessage: https://docs.papermc.io/adventure/minimessage/format/ */
public final class Text {

    private static final MiniMessage MM = MiniMessage.miniMessage();
    private static ShenanigansPlugin plugin;

    private Text() {
    }

    public static void init(ShenanigansPlugin instance) {
        plugin = instance;
    }

    /** Turn a MiniMessage string like "&lt;red&gt;Hi" into a Component. */
    public static Component mm(String input, TagResolver... resolvers) {
        return MM.deserialize(input == null ? "" : input, resolvers);
    }

    /** Same as mm() but without the default italics Minecraft puts on item names/lore. */
    public static Component item(String input, TagResolver... resolvers) {
        return mm(input, resolvers).decoration(TextDecoration.ITALIC, false);
    }

    public static String plain(Component component) {
        return PlainTextComponentSerializer.plainText().serialize(component);
    }

    /** A placeholder whose value is inserted as plain text (safe for player names and chat). */
    public static TagResolver ph(String key, String value) {
        return Placeholder.unparsed(key, value == null ? "" : value);
    }

    public static TagResolver comp(String key, ComponentLike value) {
        return Placeholder.component(key, value);
    }

    /** Raw message string from config (messages.&lt;key&gt;). */
    public static String raw(String key, String fallback) {
        return plugin.getConfig().getString("messages." + key, fallback);
    }

    /** A message from the config with the prefix in front. */
    public static Component msg(String key, TagResolver... resolvers) {
        String prefix = plugin.getConfig().getString("prefix", "");
        return mm(prefix + raw(key, "<red>Missing message: " + key), resolvers);
    }

    public static void send(CommandSender to, String key, TagResolver... resolvers) {
        to.sendMessage(msg(key, resolvers));
    }

    /** Looks exactly like the grey "[Server: ...]" lines ops see when a command is run. */
    public static Component adminEcho(Component inner) {
        return Component.translatable("chat.type.admin", NamedTextColor.GRAY, Component.text("Server"), inner)
                .decorate(TextDecoration.ITALIC);
    }

    /** Word-wrap a description for item lore. */
    public static List<String> wrap(String text, int width) {
        List<String> lines = new ArrayList<>();
        StringBuilder line = new StringBuilder();
        for (String word : text.split(" ")) {
            if (line.length() > 0 && line.length() + word.length() + 1 > width) {
                lines.add(line.toString());
                line.setLength(0);
            }
            if (line.length() > 0) {
                line.append(' ');
            }
            line.append(word);
        }
        if (line.length() > 0) {
            lines.add(line.toString());
        }
        return lines;
    }

    /** Escape MiniMessage tags in user-typed text. */
    public static String escape(String input) {
        return MM.escapeTags(input);
    }
}
