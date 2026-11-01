package com.shenanigans.troll;

import org.bukkit.Material;

/** The tabs in the troll menu. */
public enum Category {
    FAKE("Fake-Outs", Material.NAME_TAG, "Fake bans, crashes, OPs, deaths and messages"),
    SCARES("Scares & Sounds", Material.ENDER_EYE, "Jumpscares, creepy noises and weird screens"),
    MOVEMENT("Movement & Physics", Material.FEATHER, "Launch, freeze, shrink, spin and bounce them"),
    INVENTORY("Inventory", Material.CHEST, "Mess with their items (they always come back)"),
    CHAT("Chat", Material.WRITABLE_BOOK, "UwU-ify, reverse, shadow-mute and more"),
    WORLD("World & Blocks", Material.GRASS_BLOCK, "Cages, fake ores, haunted doors and fake TNT"),
    MOBS("Mobs & Fun", Material.ZOMBIE_HEAD, "Bee swarms, animal rain, fireworks and disco"),
    EFFECTS("Effects", Material.POTION, "Blindness, hunger, heart attacks and more");

    private final String displayName;
    private final Material icon;
    private final String description;

    Category(String displayName, Material icon, String description) {
        this.displayName = displayName;
        this.icon = icon;
        this.description = description;
    }

    public String displayName() {
        return displayName;
    }

    public Material icon() {
        return icon;
    }

    public String description() {
        return description;
    }
}
