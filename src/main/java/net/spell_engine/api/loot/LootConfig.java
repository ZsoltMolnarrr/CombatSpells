package net.spell_engine.api.loot;

import net.spell_engine.item.ScrollItem;
import net.spell_engine.spellbinding.SpellBindRandomlyLootFunction;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;

public class LootConfig {
    public LinkedHashMap<String, Pool> injectors = new LinkedHashMap<>();
    public LinkedHashMap<String, Pool> regex_injectors = new LinkedHashMap<>();

    public static class Pool {
        public float rolls = 1F;
        public Pool rolls(double rolls) {
            this.rolls = (float)rolls;
            return this;
        }
        public float bonus_rolls = 0.2F;
        public Pool bonus_rolls(double bonus_roll) {
            this.bonus_rolls = (float)bonus_roll;
            return this;
        }

        public List<Entry> entries = new ArrayList<>();
        public static class Entry {
            public String id;
            public Entry(String id) {
                this.id = id;
            }
            public int weight = 1;

            public Enchant enchant = null;
            public static class Enchant { public Enchant() { }
                public float min_power = 1;
                public float max_power = 30;
                public boolean allow_treasure = true;

                public Enchant(float min_power, float max_power) {
                    this.min_power = min_power;
                    this.max_power = max_power;
                }

                public boolean isValid() {
                    return min_power > 0 && max_power > min_power;
                }
            }

            public SpellBind spell_bind = null;
            public static class SpellBind { public SpellBind() { }
                public int min = 1;
                public int max = 9;

                public boolean isValid() {
                    return min > 0 && max > min;
                }
            }

            public Entry enchant() {
                this.enchant = new Enchant();
                return this;
            }
            public Entry enchant(int min, int max) {
                this.enchant = new Enchant(min, max);
                return this;
            }
        }
        public Pool add(Entry entry) {
            this.entries.add(entry);
            return this;
        }
        public Pool add(String id) {
            return add(id, false);
        }
        public Pool add(String id, int weight) {
            return add(id, false, weight);
        }
        public Pool add(String id, boolean enchant) {
            return add(id, enchant, 0);
        }
        public Pool add(String id, boolean enchant, int weight) {
            Entry entry = new Entry(id);
            if (weight > 0) {
                entry.weight = weight;
            }
            if (enchant) {
                entry.enchant();
            }
            this.entries.add(entry);
            return this;
        }

        public Pool scroll(int tier) {
            return scroll(tier, tier);
        }
        public Pool scroll(int min, int max) {
            Entry entry = new Entry(ScrollItem.ID.toString());
            var spell_bind = new Entry.SpellBind();
            spell_bind.min = min;
            spell_bind.max = max;
            entry.spell_bind = spell_bind;
            this.entries.add(entry);
            return this;
        }

        /// Adjust weight of the last entry
        public Pool weight(int weight) {
            var entry = this.entries.getLast();
            entry.weight = weight;
            return this;
        }
    }

    public static LootConfig constrainValues(LootConfig config) {
        if (config.injectors != null) {
            for (var entry: config.injectors.entrySet()) {
                var pool = entry.getValue();
                for (var lootEntry: pool.entries) {
                    if (lootEntry.weight < 1) {
                        lootEntry.weight = 1;
                    }
                }
            }
        }
        return config;
    }
}
