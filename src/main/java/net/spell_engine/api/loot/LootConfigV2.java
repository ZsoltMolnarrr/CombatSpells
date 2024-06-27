package net.spell_engine.api.loot;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;

public class LootConfigV2 {
    public LinkedHashMap<String, Pool> injectors = new LinkedHashMap<>();

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
        public Pool add(String id, boolean enchant) {
            return add(id, 0, enchant);
        }
        public Pool add(String id, int weight, boolean enchant) {
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
    }

    public static LootConfigV2 constrainValues(LootConfigV2 config) {
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
