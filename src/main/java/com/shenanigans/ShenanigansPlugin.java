package com.shenanigans;

import com.shenanigans.command.TrollCommand;
import com.shenanigans.gui.MenuHolder;
import com.shenanigans.listener.MenuListener;
import com.shenanigans.listener.TrollListener;
import com.shenanigans.listener.WandListener;
import com.shenanigans.troll.Troll;
import com.shenanigans.troll.TrollManager;
import com.shenanigans.troll.TrollRegistry;
import com.shenanigans.util.AttributeTweaks;
import com.shenanigans.util.InventoryVault;
import com.shenanigans.util.TempBlocks;
import com.shenanigans.util.Text;
import org.bukkit.Bukkit;
import org.bukkit.NamespacedKey;
import org.bukkit.command.PluginCommand;
import org.bukkit.entity.Player;
import org.bukkit.plugin.PluginManager;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.List;
import java.util.Locale;
import java.util.concurrent.ThreadLocalRandom;

/**
 * Shenanigans - a big pile of (mostly harmless) trolls and fun stuff.
 * <p>
 * Where to look:
 * <ul>
 *   <li>troll/list/*  - every troll lives here, grouped by category. Add your own!</li>
 *   <li>troll/TrollManager - runs trolls, turns toggles on/off, cleans up</li>
 *   <li>command/TrollCommand - /troll</li>
 *   <li>gui/TrollMenu - the chest menu</li>
 *   <li>listener/* - events (freeze, chat trolls, fake TNT, the wand...)</li>
 * </ul>
 */
public final class ShenanigansPlugin extends JavaPlugin {

    private TrollRegistry registry;
    private TrollManager manager;
    private TempBlocks tempBlocks;
    private InventoryVault vault;
    private volatile List<String> sillyPhrases = List.of("I love pineapple pizza");

    @Override
    public void onEnable() {
        saveDefaultConfig();
        Text.init(this);
        AttributeTweaks.init(this);

        registry = new TrollRegistry();
        registry.registerDefaults();
        manager = new TrollManager(this);
        tempBlocks = new TempBlocks(this);
        vault = new InventoryVault(this);
        loadSettings();

        PluginCommand command = getCommand("troll");
        if (command != null) {
            TrollCommand executor = new TrollCommand(this);
            command.setExecutor(executor);
            command.setTabCompleter(executor);
        }

        PluginManager pm = getServer().getPluginManager();
        pm.registerEvents(new TrollListener(this), this);
        pm.registerEvents(new MenuListener(), this);
        pm.registerEvents(new WandListener(this), this);

        manager.startTicker();

        // Fix anyone left mid-troll by a crash or /reload.
        for (Player player : Bukkit.getOnlinePlayers()) {
            vault.restore(player);
            AttributeTweaks.restoreAll(player);
        }

        getLogger().info("Loaded " + registry.size() + " trolls. Let the shenanigans begin!");
    }

    @Override
    public void onDisable() {
        if (manager != null) {
            manager.shutdown();
        }
        if (tempBlocks != null) {
            tempBlocks.restoreAll();
        }
        for (Player player : Bukkit.getOnlinePlayers()) {
            if (vault != null) {
                vault.restore(player);
            }
            AttributeTweaks.restoreAll(player);
            if (player.getOpenInventory().getTopInventory().getHolder(false) instanceof MenuHolder) {
                player.closeInventory();
            }
        }
    }

    public void reload() {
        reloadConfig();
        loadSettings();
    }

    private void loadSettings() {
        List<String> phrases = getConfig().getStringList("silly-phrases");
        sillyPhrases = phrases.isEmpty() ? List.of("I love pineapple pizza") : List.copyOf(phrases);
    }

    public boolean isTrollDisabled(Troll troll) {
        for (String id : getConfig().getStringList("disabled-trolls")) {
            if (id.toLowerCase(Locale.ROOT).equals(troll.id())) {
                return true;
            }
        }
        return false;
    }

    public String randomSillyPhrase() {
        List<String> list = sillyPhrases;
        return list.get(ThreadLocalRandom.current().nextInt(list.size()));
    }

    public NamespacedKey key(String name) {
        return new NamespacedKey(this, name);
    }

    public TrollRegistry registry() {
        return registry;
    }

    public TrollManager manager() {
        return manager;
    }

    public TempBlocks tempBlocks() {
        return tempBlocks;
    }

    public InventoryVault vault() {
        return vault;
    }
}
