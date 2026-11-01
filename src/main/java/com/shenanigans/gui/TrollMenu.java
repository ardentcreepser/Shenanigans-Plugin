package com.shenanigans.gui;

import com.shenanigans.ShenanigansPlugin;
import com.shenanigans.troll.Category;
import com.shenanigans.troll.Troll;
import com.shenanigans.troll.TrollManager;
import com.shenanigans.util.Items;
import com.shenanigans.util.Text;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.UUID;

/**
 * The chest menus:
 * 1. Player picker (heads of everyone online)
 * 2. Troll picker for one player (category tabs on top, trolls in the middle, buttons at the bottom)
 */
public final class TrollMenu {

    private static final int ROWS = 6;
    private static final int PLAYERS_PER_PAGE = 45;
    private static final int TROLLS_PER_PAGE = 36; // slots 9..44

    private TrollMenu() {
    }

    // ------------------------------------------------------------------ player picker

    public static void openPlayers(ShenanigansPlugin plugin, Player viewer, int page) {
        MenuHolder holder = new MenuHolder();
        Inventory inv = Bukkit.createInventory(holder, ROWS * 9,
                Text.mm(plugin.getConfig().getString("menu.players-title", "<dark_gray>Pick a victim")));
        holder.setInventory(inv);
        int[] pageRef = {page};
        holder.setRefresher(() -> renderPlayers(plugin, holder, viewer, pageRef));
        renderPlayers(plugin, holder, viewer, pageRef);
        viewer.openInventory(inv);
    }

    private static void renderPlayers(ShenanigansPlugin plugin, MenuHolder holder, Player viewer, int[] pageRef) {
        holder.clear();
        TrollManager manager = plugin.manager();
        List<Player> players = new ArrayList<>(Bukkit.getOnlinePlayers());
        players.sort(Comparator.comparing(Player::getName, String.CASE_INSENSITIVE_ORDER));

        int pages = Math.max(1, (players.size() + PLAYERS_PER_PAGE - 1) / PLAYERS_PER_PAGE);
        int page = Math.max(0, Math.min(pageRef[0], pages - 1));
        pageRef[0] = page;

        for (int i = 0; i < PLAYERS_PER_PAGE; i++) {
            int index = page * PLAYERS_PER_PAGE + i;
            if (index >= players.size()) {
                break;
            }
            Player target = players.get(index);
            boolean isProtected = manager.isProtected(viewer, target);
            int active = manager.activeIds(target.getUniqueId()).size();
            List<String> lore = new ArrayList<>();
            lore.add("<gray>World: <white>" + target.getWorld().getName());
            lore.add("<gray>Active trolls: " + (active > 0 ? "<green>" : "<white>") + active);
            lore.add("");
            lore.add(isProtected ? "<red>Protected from trolls" : "<yellow>Click to troll!");
            UUID id = target.getUniqueId();
            holder.set(i, Items.head(target, (target.equals(viewer) ? "<aqua>" : "<yellow>") + target.getName()
                            + (target.equals(viewer) ? " <gray>(you)" : ""), lore),
                    isProtected ? null : (p, click) -> {
                        click(p);
                        later(plugin, () -> openTrolls(plugin, p, id, null, 0));
                    });
        }

        fillBottom(holder);
        if (page > 0) {
            holder.set(45, Items.make(Material.ARROW, "<yellow>Previous page"), (p, click) -> {
                click(p);
                pageRef[0]--;
                holder.refresh();
            });
        }
        if (page < pages - 1) {
            holder.set(53, Items.make(Material.ARROW, "<yellow>Next page"), (p, click) -> {
                click(p);
                pageRef[0]++;
                holder.refresh();
            });
        }
        holder.set(47, Items.make(Material.CLOCK, "<aqua>Refresh"), (p, click) -> {
            click(p);
            holder.refresh();
        });
        holder.set(49, Items.make(Material.BARRIER, "<red>Close"), (p, click) -> later(plugin, p::closeInventory));
        holder.set(51, Items.make(Material.BOOK, "<gold>How to use",
                "<gray>Click a head to pick who to troll.",
                "<gray>Then click a troll to use it.",
                "<gray>Glowing trolls are switched on -",
                "<gray>click them again to turn them off.",
                "",
                "<gray>Commands: <white>/troll help"), null);
    }

    // ------------------------------------------------------------------ troll picker

    public static void openTrolls(ShenanigansPlugin plugin, Player viewer, UUID targetId, Category category, int page) {
        Player target = Bukkit.getPlayer(targetId);
        if (target == null) {
            openPlayers(plugin, viewer, 0);
            return;
        }
        MenuHolder holder = new MenuHolder();
        String title = plugin.getConfig().getString("menu.trolls-title", "<dark_gray>Trolling <dark_red><player>");
        Inventory inv = Bukkit.createInventory(holder, ROWS * 9, Text.mm(title, Text.ph("player", target.getName())));
        holder.setInventory(inv);
        State state = new State(targetId, category, page);
        holder.setRefresher(() -> renderTrolls(plugin, holder, viewer, state));
        renderTrolls(plugin, holder, viewer, state);
        viewer.openInventory(inv);
    }

    /** What the troll menu is currently showing. */
    private static final class State {
        private final UUID target;
        private Category category;
        private int page;

        private State(UUID target, Category category, int page) {
            this.target = target;
            this.category = category;
            this.page = page;
        }
    }

    private static void renderTrolls(ShenanigansPlugin plugin, MenuHolder holder, Player viewer, State state) {
        Player target = Bukkit.getPlayer(state.target);
        if (target == null) {
            openPlayers(plugin, viewer, 0);
            return;
        }
        holder.clear();
        TrollManager manager = plugin.manager();

        // Category tabs (top row)
        ItemStack all = Items.make(Material.NETHER_STAR, "<gold>All trolls",
                "<gray>" + plugin.registry().size() + " trolls");
        holder.set(0, Items.glow(all, state.category == null), (p, click) -> {
            click(p);
            state.category = null;
            state.page = 0;
            holder.refresh();
        });
        Category[] categories = Category.values();
        for (int i = 0; i < categories.length && i < 8; i++) {
            Category cat = categories[i];
            List<String> lore = new ArrayList<>();
            for (String line : Text.wrap(cat.description(), 30)) {
                lore.add("<gray>" + line);
            }
            lore.add("<dark_gray>" + plugin.registry().byCategory(cat).size() + " trolls");
            ItemStack tab = Items.make(cat.icon(), "<gold>" + cat.displayName(), lore);
            holder.set(1 + i, Items.glow(tab, state.category == cat), (p, click) -> {
                click(p);
                state.category = cat;
                state.page = 0;
                holder.refresh();
            });
        }

        // Trolls (rows 2-5)
        List<Troll> trolls = plugin.registry().byCategory(state.category);
        int pages = Math.max(1, (trolls.size() + TROLLS_PER_PAGE - 1) / TROLLS_PER_PAGE);
        state.page = Math.max(0, Math.min(state.page, pages - 1));
        List<String> active = manager.activeIds(target.getUniqueId());
        for (int i = 0; i < TROLLS_PER_PAGE; i++) {
            int index = state.page * TROLLS_PER_PAGE + i;
            if (index >= trolls.size()) {
                break;
            }
            Troll troll = trolls.get(index);
            boolean isActive = active.contains(troll.id());
            holder.set(9 + i, trollItem(plugin, viewer, target, troll, isActive), (p, click) -> {
                Player t = Bukkit.getPlayer(state.target);
                if (t == null) {
                    later(plugin, () -> openPlayers(plugin, p, 0));
                    return;
                }
                click(p);
                manager.apply(p, t, troll, new String[0]);
                refreshLater(plugin, p, holder);
            });
        }

        // Bottom bar
        fillBottom(holder);
        holder.set(45, Items.make(Material.ARROW, "<yellow>Back to players"), (p, click) -> {
            click(p);
            later(plugin, () -> openPlayers(plugin, p, 0));
        });
        if (state.page > 0) {
            holder.set(47, Items.make(Material.SPECTRAL_ARROW, "<yellow>Previous page"), (p, click) -> {
                click(p);
                state.page--;
                holder.refresh();
            });
        }
        holder.set(48, Items.make(Material.EMERALD, "<green>Random troll",
                "<gray>Feeling lucky? Uses a random", "<gray>one-shot troll on them."), (p, click) -> {
            Player t = Bukkit.getPlayer(state.target);
            if (t == null) {
                return;
            }
            Troll random = randomInstant(plugin, p);
            if (random != null) {
                click(p);
                manager.apply(p, t, random, new String[0]);
                refreshLater(plugin, p, holder);
            }
        });
        List<String> headLore = new ArrayList<>();
        headLore.add("<gray>Active trolls:");
        if (active.isEmpty()) {
            headLore.add("<dark_gray> none");
        } else {
            for (String id : active) {
                Troll troll = plugin.registry().get(id);
                headLore.add("<green> ● " + (troll == null ? id : troll.name()));
            }
        }
        holder.set(49, Items.head(target, "<yellow>" + target.getName(), headLore), null);
        holder.set(50, Items.make(Material.MILK_BUCKET, "<aqua>Stop all trolls",
                "<gray>Turns off every active troll", "<gray>on " + target.getName() + "."), (p, click) -> {
            Player t = Bukkit.getPlayer(state.target);
            if (t == null) {
                return;
            }
            click(p);
            int stopped = manager.stopAll(t.getUniqueId());
            Text.send(p, "stopped-all", Text.ph("player", t.getName()), Text.ph("count", String.valueOf(stopped)));
            holder.refresh();
        });
        if (state.page < pages - 1) {
            holder.set(51, Items.make(Material.SPECTRAL_ARROW, "<yellow>Next page"), (p, click) -> {
                click(p);
                state.page++;
                holder.refresh();
            });
        }
        holder.set(53, Items.make(Material.BARRIER, "<red>Close"), (p, click) -> later(plugin, p::closeInventory));
    }

    private static ItemStack trollItem(ShenanigansPlugin plugin, Player viewer, Player target, Troll troll, boolean active) {
        List<String> lore = new ArrayList<>();
        for (String line : Text.wrap(troll.description(), 34)) {
            lore.add("<gray>" + Text.escape(line));
        }
        lore.add("");
        if (troll.isToggle()) {
            int seconds = plugin.getConfig().getInt("durations." + troll.id(), troll.defaultSeconds());
            lore.add("<dark_gray>Toggle" + (seconds > 0 ? " • lasts " + seconds + "s" : " • until turned off"));
        } else {
            lore.add("<dark_gray>One-shot");
        }
        lore.add("<dark_gray>/troll " + target.getName() + " " + troll.id()
                + (troll.usage().isEmpty() ? "" : " " + Text.escape(troll.usage())));

        Material icon = troll.icon();
        if (plugin.isTrollDisabled(troll)) {
            lore.add("<red>Disabled in config.yml");
            icon = Material.BARRIER;
        } else if (!plugin.manager().canUse(viewer, troll)) {
            lore.add("<red>You don't have permission");
        } else if (active) {
            lore.add("<green>● Active <gray>- click to turn off");
        } else {
            lore.add("<yellow>Click to use!");
        }
        ItemStack item = Items.make(icon, (active ? "<green>" : "<yellow>") + troll.name(), lore);
        return Items.glow(item, active);
    }

    /** A random one-shot troll the viewer is allowed to use, or null. */
    public static Troll randomInstant(ShenanigansPlugin plugin, org.bukkit.command.CommandSender sender) {
        List<Troll> options = new ArrayList<>();
        for (Troll troll : plugin.registry().all()) {
            if (!troll.isToggle() && !plugin.isTrollDisabled(troll) && plugin.manager().canUse(sender, troll)
                    && !troll.id().startsWith("fakeban") && !troll.id().equals("fakecrash")
                    && !troll.id().equals("fakerestart")) {
                options.add(troll);
            }
        }
        if (options.isEmpty()) {
            return null;
        }
        return options.get(java.util.concurrent.ThreadLocalRandom.current().nextInt(options.size()));
    }

    /** Open/close menus on the next tick - doing it inside a click event can glitch. */
    private static void later(ShenanigansPlugin plugin, Runnable task) {
        Bukkit.getScheduler().runTask(plugin, task);
    }

    private static void fillBottom(MenuHolder holder) {
        ItemStack pane = Items.make(Material.GRAY_STAINED_GLASS_PANE, " ");
        for (int slot = 45; slot < 54; slot++) {
            holder.set(slot, pane, null);
        }
    }

    private static void click(Player player) {
        player.playSound(player.getLocation(), Sound.UI_BUTTON_CLICK, 0.5f, 1.2f);
    }

    /** Redraw next tick, but only if they still have this menu open (some trolls close it, like the demo screen). */
    private static void refreshLater(ShenanigansPlugin plugin, Player viewer, MenuHolder holder) {
        Bukkit.getScheduler().runTaskLater(plugin, () -> {
            if (viewer.isOnline() && viewer.getOpenInventory().getTopInventory().getHolder(false) == holder) {
                holder.refresh();
            }
        }, 2L);
    }
}
