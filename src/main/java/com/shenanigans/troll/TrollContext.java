package com.shenanigans.troll;

import com.shenanigans.ShenanigansPlugin;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.List;
import java.util.concurrent.ThreadLocalRandom;

/** Everything a troll needs to know when it runs: who did it, who it's on, and any extra words typed. */
public final class TrollContext {

    private final ShenanigansPlugin plugin;
    private final CommandSender sender;
    private final Player target;
    private final Troll troll;
    private final String[] args;
    private String failure;

    public TrollContext(ShenanigansPlugin plugin, CommandSender sender, Player target, Troll troll, String[] args) {
        this.plugin = plugin;
        this.sender = sender;
        this.target = target;
        this.troll = troll;
        this.args = args == null ? new String[0] : args;
    }

    public ShenanigansPlugin plugin() { return plugin; }
    public CommandSender sender() { return sender; }
    public Player target() { return target; }
    public Troll troll() { return troll; }
    public String[] args() { return args; }

    /** All extra words joined together, or the fallback when nothing was typed. */
    public String joined(String fallback) {
        return args.length == 0 ? fallback : String.join(" ", args);
    }

    public String arg(int index, String fallback) {
        return index < args.length ? args[index] : fallback;
    }

    // ---- config shortcuts: trolls.<id>.<path> ----

    private String path(String key) {
        return "trolls." + troll.id() + "." + key;
    }

    public int cfgInt(String key, int def) { return plugin.getConfig().getInt(path(key), def); }
    public double cfgDouble(String key, double def) { return plugin.getConfig().getDouble(path(key), def); }
    public boolean cfgBool(String key, boolean def) { return plugin.getConfig().getBoolean(path(key), def); }
    public String cfgString(String key, String def) { return plugin.getConfig().getString(path(key), def); }

    public List<String> cfgList(String key, List<String> def) {
        List<String> list = plugin.getConfig().getStringList(path(key));
        return list.isEmpty() ? def : list;
    }

    /** A random line from silly-phrases in the config. */
    public String sillyPhrase() {
        return plugin.randomSillyPhrase();
    }

    public <T> T pick(List<T> list) {
        return list.get(ThreadLocalRandom.current().nextInt(list.size()));
    }

    /** Call this to say the troll couldn't happen (the reason is shown to whoever used it). */
    public void fail(String reason) {
        this.failure = reason;
    }

    public String failure() {
        return failure;
    }
}
