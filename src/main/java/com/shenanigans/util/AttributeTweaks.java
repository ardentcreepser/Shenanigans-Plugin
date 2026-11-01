package com.shenanigans.util;

import org.bukkit.NamespacedKey;
import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeInstance;
import org.bukkit.entity.Player;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.plugin.Plugin;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Changes player attributes (size, gravity, speed...) and remembers the original value
 * ON the player, so even if the server crashes mid-troll it gets fixed next time they join.
 */
public final class AttributeTweaks {

    private static final Map<String, Attribute> ATTRIBUTES = new LinkedHashMap<>();
    private static Plugin plugin;

    static {
        ATTRIBUTES.put("scale", Attribute.SCALE);
        ATTRIBUTES.put("gravity", Attribute.GRAVITY);
        ATTRIBUTES.put("jump_strength", Attribute.JUMP_STRENGTH);
        ATTRIBUTES.put("movement_speed", Attribute.MOVEMENT_SPEED);
        ATTRIBUTES.put("block_interaction_range", Attribute.BLOCK_INTERACTION_RANGE);
        ATTRIBUTES.put("entity_interaction_range", Attribute.ENTITY_INTERACTION_RANGE);
        ATTRIBUTES.put("bounciness", Attribute.BOUNCINESS);
        ATTRIBUTES.put("friction_modifier", Attribute.FRICTION_MODIFIER);
        ATTRIBUTES.put("step_height", Attribute.STEP_HEIGHT);
    }

    private AttributeTweaks() {
    }

    public static void init(Plugin instance) {
        plugin = instance;
    }

    private static NamespacedKey key(String name) {
        return new NamespacedKey(plugin, "original_" + name);
    }

    /** Set an attribute (by name, see the list above), saving the original value first. */
    public static void set(Player player, String name, double value) {
        Attribute attribute = ATTRIBUTES.get(name);
        if (attribute == null) {
            throw new IllegalArgumentException("Unknown attribute " + name);
        }
        AttributeInstance instance = player.getAttribute(attribute);
        if (instance == null) {
            return;
        }
        PersistentDataContainer pdc = player.getPersistentDataContainer();
        NamespacedKey key = key(name);
        if (!pdc.has(key, PersistentDataType.DOUBLE)) {
            pdc.set(key, PersistentDataType.DOUBLE, instance.getBaseValue());
        }
        instance.setBaseValue(value);
    }

    /** Put an attribute back to what it was before we touched it. */
    public static void restore(Player player, String name) {
        Attribute attribute = ATTRIBUTES.get(name);
        PersistentDataContainer pdc = player.getPersistentDataContainer();
        NamespacedKey key = key(name);
        Double original = pdc.get(key, PersistentDataType.DOUBLE);
        if (original == null || attribute == null) {
            return;
        }
        AttributeInstance instance = player.getAttribute(attribute);
        if (instance != null) {
            instance.setBaseValue(original);
        }
        pdc.remove(key);
    }

    public static void restoreAll(Player player) {
        for (String name : ATTRIBUTES.keySet()) {
            restore(player, name);
        }
    }
}
