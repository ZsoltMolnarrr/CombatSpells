package net.spell_engine.rpg_series.config;

import net.spell_engine.api.loot.LootConfig;
import net.spell_engine.api.loot.LootConfigV2;

import java.util.*;
import java.util.stream.Collectors;

public class Defaults {

    public final static LootConfigV2 lootConfig;

    private static String armors(int tier) {
        return "#rpg_series:tier_" + tier + "_armors";
    }
    
    private static String weapons(int tier) {
        return "#rpg_series:tier_" + tier + "_weapons";
    }

    private static String equipment(int tier) {
        return "#rpg_series:tier_" + tier + "_equipment";
    }

    static {

        var WG = "#rpg_series:golden_weapons";
        var W0 = "#rpg_series:tier_0_weapons";
        var W1 = "#rpg_series:tier_1_weapons";
        var W2 = "#rpg_series:tier_2_weapons";
        var W3 = "#rpg_series:tier_3_weapons";
        var W4 = "#rpg_series:tier_4_weapons";
        var W5 = "#rpg_series:tier_5_weapons";
        var A1 = "#rpg_series:tier_1_armors";
        var A2 = "#rpg_series:tier_2_armors";
        var A3 = "#rpg_series:tier_3_armors";
        var X0 = "#rpg_series:tier_0_accessories";
        var X1 = "#rpg_series:tier_1_accessories";
        var X2 = "#rpg_series:tier_2_accessories";
        var X3 = "#rpg_series:tier_3_accessories";
        var X4 = "#rpg_series:tier_4_accessories";

        // Vanilla loot table injectors
        lootConfig = new LootConfigV2();
        var injectors = lootConfig.injectors;
        injectors.put("minecraft:chests/ruined_portal", new LootConfigV2.Pool()
                .rolls(2)
                .add(WG)
                .add(WG, true)
        );

        List.of("minecraft:chests/abandoned_mineshaft",
                "minecraft:chests/igloo_chest",
                "minecraft:chests/shipwreck_supply",
                "minecraft:chests/spawn_bonus_chest").
                forEach(id -> injectors.put(id, new LootConfigV2.Pool()
                        .rolls(0.5)
                        .add(W0)
                        .add(X0)
                ));


        List.of("minecraft:chests/bastion_bridge",
                "minecraft:chests/simple_dungeon",
                "minecraft:chests/stronghold_crossing",
                "minecraft:chests/buried_treasure")
                .forEach(id -> injectors.put(id, new LootConfigV2.Pool()
                        .rolls(0.5)
                        .add(W1)
                        .add(X2)
                ));

        List.of("minecraft:chests/shipwreck_treasure")
                .forEach(id -> injectors.put(id, new LootConfigV2.Pool()
                        .rolls(0.5)
                        .add(A1)
                ));

        List.of("minecraft:chests/stronghold_crossing",
                "minecraft:chests/desert_pyramid",
                "minecraft:chests/underwater_ruin_small",
                "minecraft:chests/jungle_temple",
                "minecraft:chests/pillager_outpost",
                "minecraft:chests/woodland_mansion")
                .forEach(id -> injectors.put(id, new LootConfigV2.Pool()
                        .rolls(0.5)
                        .add(W1, true)
                        .add(A1, true)
                        .add(X1, true)
                ));

        List.of("minecraft:chests/nether_bridge",
                "minecraft:chests/underwater_ruin_big")
                .forEach(id -> injectors.put(id, new LootConfigV2.Pool()
                        .rolls(0.5)
                        .add(W2)
                        .add(X2)
                ));

        List.of("minecraft:chests/bastion_other")
                .forEach(id -> injectors.put(id, new LootConfigV2.Pool()
                        .rolls(0.5)
                        .add(W1, true)
                        .add(X3)
                ));

        injectors.put("minecraft:chests/bastion_treasure", new LootConfigV2.Pool()
                .rolls(1)
                .bonus_rolls(0)
                .add(A2, true)
                .add(W3, true)
                .add(X3)
        );

        List.of("minecraft:chests/nether_bridge",
                "minecraft:chests/underwater_ruin_big")
                .forEach(id -> injectors.put(id, new LootConfigV2.Pool()
                        .rolls(0.75)
                        .add(W2)
                        .add(X2)
                ));

        List.of("minecraft:chests/ancient_city",
                "minecraft:chests/stronghold_library")
                .forEach(id -> injectors.put(id, new LootConfigV2.Pool()
                        .rolls(0.5)
                        .add(A2, true)
                        .add(X2, 2, false)
                ));

        List.of("minecraft:chests/end_city_treasure")
                .forEach(id -> injectors.put(id, new LootConfigV2.Pool()
                        .rolls(1)
                        .bonus_rolls(0)
                        .add(W4, true)
                        .add(A2, true)
                        .add(A3, true)
                        .add(X4)
                ));
    }


    @SafeVarargs
    private static <T> List<T> joinLists(List<T>... lists) {
        return Arrays.stream(lists).flatMap(Collection::stream).collect(Collectors.toList());
    }
}
