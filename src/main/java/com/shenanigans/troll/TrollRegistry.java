package com.shenanigans.troll;

import com.shenanigans.troll.list.ChatTrolls;
import com.shenanigans.troll.list.EffectTrolls;
import com.shenanigans.troll.list.FakeOutTrolls;
import com.shenanigans.troll.list.InventoryTrolls;
import com.shenanigans.troll.list.MobTrolls;
import com.shenanigans.troll.list.MovementTrolls;
import com.shenanigans.troll.list.ScareTrolls;
import com.shenanigans.troll.list.WorldTrolls;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/** The list of every troll the plugin knows about. */
public final class TrollRegistry {

    private final Map<String, Troll> trolls = new LinkedHashMap<>();

    public void registerDefaults() {
        FakeOutTrolls.register(this);
        ScareTrolls.register(this);
        MovementTrolls.register(this);
        InventoryTrolls.register(this);
        ChatTrolls.register(this);
        WorldTrolls.register(this);
        MobTrolls.register(this);
        EffectTrolls.register(this);
    }

    public void add(Category category, Troll.Builder builder) {
        Troll troll = builder.build(category);
        if (trolls.containsKey(troll.id())) {
            throw new IllegalStateException("Duplicate troll id: " + troll.id());
        }
        trolls.put(troll.id(), troll);
    }

    public Troll get(String id) {
        return id == null ? null : trolls.get(id.toLowerCase(Locale.ROOT));
    }

    public Collection<Troll> all() {
        return Collections.unmodifiableCollection(trolls.values());
    }

    public List<Troll> byCategory(Category category) {
        List<Troll> list = new ArrayList<>();
        for (Troll troll : trolls.values()) {
            if (category == null || troll.category() == category) {
                list.add(troll);
            }
        }
        return list;
    }

    public List<String> ids() {
        return new ArrayList<>(trolls.keySet());
    }

    public int size() {
        return trolls.size();
    }
}
