package com.shenanigans.command;

import com.shenanigans.ShenanigansPlugin;
import com.shenanigans.gui.TrollMenu;
import com.shenanigans.listener.WandListener;
import com.shenanigans.troll.Category;
import com.shenanigans.troll.Troll;
import com.shenanigans.troll.TrollManager;
import com.shenanigans.util.Text;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.TextComponent;
import net.kyori.adventure.text.event.ClickEvent;
import net.kyori.adventure.text.event.HoverEvent;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabExecutor;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.UUID;

/**
 * /troll                         open the menu
 * /troll &lt;player&gt; &lt;troll&gt; [args]  use a troll (also works as /troll &lt;troll&gt; &lt;player&gt;)
 * /troll * &lt;troll&gt;               troll everyone
 * /troll stop &lt;player|*&gt; [troll]   stop toggle trolls
 * /troll list | active | wand | reload | help
 */
public final class TrollCommand implements TabExecutor {

    private static final List<String> SUBCOMMANDS = List.of("help", "gui", "list", "active", "stop", "wand", "reload");

    private final ShenanigansPlugin plugin;

    public TrollCommand(ShenanigansPlugin plugin) {
        this.plugin = plugin;
    }

    private TrollManager manager() {
        return plugin.manager();
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!sender.hasPermission("shenanigans.use")) {
            Text.send(sender, "no-permission");
            return true;
        }
        if (args.length == 0) {
            if (sender instanceof Player player) {
                TrollMenu.openPlayers(plugin, player, 0);
            } else {
                help(sender, label);
            }
            return true;
        }

        switch (args[0].toLowerCase(Locale.ROOT)) {
            case "help", "?" -> help(sender, label);
            case "gui", "menu" -> gui(sender, args);
            case "list" -> list(sender, args);
            case "active" -> active(sender);
            case "stop" -> stop(sender, label, args);
            case "wand" -> wand(sender, args);
            case "reload" -> reload(sender);
            default -> troll(sender, label, args);
        }
        return true;
    }

    // ------------------------------------------------------------------ /troll <player> <troll>

    private void troll(CommandSender sender, String label, String[] args) {
        if (args.length < 2) {
            sender.sendMessage(Text.mm("<red>Usage: " + Text.escape("/" + label + " <player> <troll>") + " <gray>(or /" + label + " help)"));
            return;
        }
        String trollName;
        List<Player> targets;
        boolean trollFirst = !args[0].equals("*") && Bukkit.getPlayerExact(args[0]) == null
                && (plugin.registry().get(args[0]) != null || args[0].equalsIgnoreCase("random"));
        if (trollFirst) {
            // /troll <troll> <player>
            trollName = args[0];
            targets = resolveTargets(sender, args[1]);
            if (targets == null) {
                Text.send(sender, "player-not-found", Text.ph("player", args[1]));
                return;
            }
        } else {
            // /troll <player> <troll>
            trollName = args[1];
            targets = resolveTargets(sender, args[0]);
            if (targets == null) {
                Text.send(sender, "player-not-found", Text.ph("player", args[0]));
                return;
            }
        }
        boolean everyone = args[0].equals("*") || args[1].equals("*");
        if (everyone && !sender.hasPermission("shenanigans.all")) {
            Text.send(sender, "no-permission");
            return;
        }
        if (targets.isEmpty()) {
            Text.send(sender, "nobody-online");
            return;
        }

        String[] extra = Arrays.copyOfRange(args, 2, args.length);
        boolean random = trollName.equalsIgnoreCase("random");
        Troll troll = random ? null : plugin.registry().get(trollName);
        if (!random && troll == null) {
            Text.send(sender, "unknown-troll", Text.ph("troll", trollName));
            return;
        }

        if (targets.size() == 1) {
            Troll use = random ? TrollMenu.randomInstant(plugin, sender) : troll;
            if (use == null) {
                Text.send(sender, "no-permission");
                return;
            }
            manager().apply(sender, targets.get(0), use, extra);
            return;
        }

        int worked = 0;
        for (Player target : targets) {
            Troll use = random ? TrollMenu.randomInstant(plugin, sender) : troll;
            if (use != null && manager().apply(sender, target, use, extra, true)) {
                worked++;
            }
        }
        Text.send(sender, "applied-many", Text.ph("troll", random ? "random trolls" : troll.name()),
                Text.ph("count", String.valueOf(worked)));
    }

    /** "*" = everyone except you. Returns null if the player wasn't found. */
    private List<Player> resolveTargets(CommandSender sender, String name) {
        if (name.equals("*")) {
            List<Player> all = new ArrayList<>(Bukkit.getOnlinePlayers());
            if (sender instanceof Player self) {
                all.remove(self);
            }
            return all;
        }
        Player player = Bukkit.getPlayerExact(name);
        if (player == null) {
            player = Bukkit.getPlayer(name); // partial name
        }
        return player == null ? null : List.of(player);
    }

    // ------------------------------------------------------------------ subcommands

    private void help(CommandSender sender, String label) {
        String l = "/" + label;
        List<String> lines = List.of(
                "<gradient:#ff5f6d:#ffc371><bold>Shenanigans</bold></gradient> <gray>- " + plugin.registry().size() + " trolls",
                "<yellow>" + l + " <gray>- open the troll menu",
                "<yellow>" + Text.escape(l + " <player> <troll> [args]") + " <gray>- troll someone",
                "<yellow>" + Text.escape(l + " * <troll>") + " <gray>- troll everyone (except you)",
                "<yellow>" + Text.escape(l + " <player> random") + " <gray>- surprise them",
                "<yellow>" + Text.escape(l + " stop <player|*> [troll]") + " <gray>- turn toggle trolls off",
                "<yellow>" + l + " list [category] <gray>- every troll (hover for info, click to use)",
                "<yellow>" + l + " active <gray>- who has trolls running right now",
                "<yellow>" + l + " wand [troll] <gray>- get the point-and-click Troll Wand",
                "<yellow>" + l + " reload <gray>- reload config.yml");
        for (String line : lines) {
            sender.sendMessage(Text.mm(line));
        }
    }

    private void gui(CommandSender sender, String[] args) {
        if (!(sender instanceof Player player)) {
            Text.send(sender, "players-only");
            return;
        }
        if (args.length >= 2) {
            Player target = Bukkit.getPlayer(args[1]);
            if (target == null) {
                Text.send(sender, "player-not-found", Text.ph("player", args[1]));
                return;
            }
            TrollMenu.openTrolls(plugin, player, target.getUniqueId(), null, 0);
        } else {
            TrollMenu.openPlayers(plugin, player, 0);
        }
    }

    private void list(CommandSender sender, String[] args) {
        Category only = null;
        if (args.length >= 2) {
            only = categoryByName(args[1]);
            if (only == null) {
                sender.sendMessage(Text.mm("<red>Unknown category. Try: <white>"
                        + String.join(", ", categoryNames())));
                return;
            }
        }
        sender.sendMessage(Text.mm("<gradient:#ff5f6d:#ffc371><bold>Trolls</bold></gradient> <gray>("
                + plugin.registry().size() + ") <dark_gray>- <yellow>yellow<dark_gray> = one-shot, <aqua>aqua<dark_gray> = toggle. Hover for info, click to use."));
        for (Category category : Category.values()) {
            if (only != null && category != only) {
                continue;
            }
            List<Troll> trolls = plugin.registry().byCategory(category);
            TextComponent.Builder line = Component.text();
            line.append(Text.mm("<gold>" + category.displayName() + ": "));
            for (int i = 0; i < trolls.size(); i++) {
                Troll troll = trolls.get(i);
                if (i > 0) {
                    line.append(Component.text(", ", NamedTextColor.DARK_GRAY));
                }
                Component hover = Text.mm("<yellow>" + troll.name() + "\n<gray>" + Text.escape(troll.description())
                        + "\n<dark_gray>" + Text.escape("/troll <player> ") + troll.id()
                        + (troll.usage().isEmpty() ? "" : " " + Text.escape(troll.usage()))
                        + (plugin.isTrollDisabled(troll) ? "\n<red>Disabled in config" : ""));
                NamedTextColor color = plugin.isTrollDisabled(troll) ? NamedTextColor.DARK_GRAY
                        : troll.isToggle() ? NamedTextColor.AQUA : NamedTextColor.YELLOW;
                line.append(Component.text(troll.id(), color)
                        .hoverEvent(HoverEvent.showText(hover))
                        .clickEvent(ClickEvent.suggestCommand("/troll " + troll.id() + " ")));
            }
            sender.sendMessage(line.build());
        }
    }

    private void active(CommandSender sender) {
        boolean any = false;
        for (UUID id : manager().trolledPlayers()) {
            Player player = Bukkit.getPlayer(id);
            List<String> trolls = manager().activeIds(id);
            if (player == null || trolls.isEmpty()) {
                continue;
            }
            any = true;
            TextComponent.Builder line = Component.text();
            line.append(Text.mm("<yellow>" + player.getName() + "<gray>: "));
            for (int i = 0; i < trolls.size(); i++) {
                String trollId = trolls.get(i);
                if (i > 0) {
                    line.append(Component.text(", ", NamedTextColor.DARK_GRAY));
                }
                line.append(Component.text(trollId, NamedTextColor.AQUA)
                        .hoverEvent(HoverEvent.showText(Component.text("Click to stop", NamedTextColor.RED)))
                        .clickEvent(ClickEvent.runCommand("/troll stop " + player.getName() + " " + trollId)));
            }
            sender.sendMessage(line.build());
        }
        if (!any) {
            Text.send(sender, "no-active");
        }
    }

    private void stop(CommandSender sender, String label, String[] args) {
        if (args.length < 2) {
            sender.sendMessage(Text.mm("<red>Usage: " + Text.escape("/" + label + " stop <player|*> [troll]")));
            return;
        }
        List<Player> targets = resolveTargets(sender, args[1]);
        if (args[1].equals("*")) {
            targets = new ArrayList<>(Bukkit.getOnlinePlayers()); // stopping includes yourself
        }
        if (targets == null) {
            Text.send(sender, "player-not-found", Text.ph("player", args[1]));
            return;
        }
        int stopped = 0;
        for (Player target : targets) {
            if (args.length >= 3) {
                if (manager().stop(target.getUniqueId(), args[2].toLowerCase(Locale.ROOT))) {
                    stopped++;
                }
            } else {
                stopped += manager().stopAll(target.getUniqueId());
            }
        }
        String who = targets.size() == 1 ? targets.get(0).getName() : "everyone";
        Text.send(sender, "stopped-all", Text.ph("player", who), Text.ph("count", String.valueOf(stopped)));
    }

    private void wand(CommandSender sender, String[] args) {
        if (!(sender instanceof Player player)) {
            Text.send(sender, "players-only");
            return;
        }
        if (!player.hasPermission("shenanigans.wand")) {
            Text.send(player, "no-permission");
            return;
        }
        String id = args.length >= 2 ? args[1] : plugin.getConfig().getString("wand.default-troll", "launch");
        Troll troll = plugin.registry().get(id);
        if (troll == null) {
            Text.send(player, "unknown-troll", Text.ph("troll", id));
            return;
        }
        ItemStack wand = WandListener.createWand(plugin, troll);
        for (ItemStack rest : player.getInventory().addItem(wand).values()) {
            player.getWorld().dropItemNaturally(player.getLocation(), rest);
        }
        Text.send(player, "wand-given", Text.ph("troll", troll.name()));
    }

    private void reload(CommandSender sender) {
        if (!sender.hasPermission("shenanigans.reload")) {
            Text.send(sender, "no-permission");
            return;
        }
        plugin.reload();
        Text.send(sender, "reloaded");
    }

    // ------------------------------------------------------------------ tab completion

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        if (!sender.hasPermission("shenanigans.use")) {
            return List.of();
        }
        List<String> options = new ArrayList<>();
        if (args.length == 1) {
            options.addAll(SUBCOMMANDS);
            options.add("*");
            options.addAll(playerNames());
        } else if (args.length == 2) {
            String first = args[0].toLowerCase(Locale.ROOT);
            switch (first) {
                case "stop" -> {
                    options.add("*");
                    options.addAll(playerNames());
                }
                case "gui", "menu" -> options.addAll(playerNames());
                case "wand" -> options.addAll(plugin.registry().ids());
                case "list" -> options.addAll(categoryNames());
                case "help", "active", "reload" -> {
                }
                default -> {
                    if (plugin.registry().get(first) != null && Bukkit.getPlayerExact(args[0]) == null) {
                        options.add("*");
                        options.addAll(playerNames());
                    } else {
                        options.add("random");
                        options.addAll(plugin.registry().ids());
                    }
                }
            }
        } else if (args.length == 3) {
            String first = args[0].toLowerCase(Locale.ROOT);
            if (first.equals("stop")) {
                Player target = Bukkit.getPlayer(args[1]);
                options.addAll(target == null ? plugin.registry().ids() : manager().activeIds(target.getUniqueId()));
            } else {
                String trollId = plugin.registry().get(args[1]) != null ? args[1] : args[0];
                switch (trollId.toLowerCase(Locale.ROOT)) {
                    case "fakegamemode" -> options.addAll(List.of("survival", "creative", "adventure", "spectator"));
                    case "shuffle" -> options.addAll(List.of("hotbar", "all"));
                    case "swap" -> options.addAll(playerNames());
                    case "cage" -> options.addAll(List.of("glass", "bedrock", "oak_leaves", "slime_block", "honey_block", "obsidian"));
                    default -> {
                    }
                }
            }
        }
        String typed = args[args.length - 1].toLowerCase(Locale.ROOT);
        List<String> result = new ArrayList<>();
        for (String option : options) {
            if (option.toLowerCase(Locale.ROOT).startsWith(typed)) {
                result.add(option);
            }
        }
        return result;
    }

    private List<String> playerNames() {
        List<String> names = new ArrayList<>();
        for (Player player : Bukkit.getOnlinePlayers()) {
            names.add(player.getName());
        }
        return names;
    }

    private static List<String> categoryNames() {
        List<String> names = new ArrayList<>();
        for (Category category : Category.values()) {
            names.add(category.name().toLowerCase(Locale.ROOT));
        }
        return names;
    }

    private static Category categoryByName(String name) {
        for (Category category : Category.values()) {
            if (category.name().equalsIgnoreCase(name)) {
                return category;
            }
        }
        return null;
    }
}
