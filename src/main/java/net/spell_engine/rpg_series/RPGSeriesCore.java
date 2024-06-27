package net.spell_engine.rpg_series;

import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.fabric.api.loot.v2.LootTableEvents;
import net.spell_engine.api.loot.LootConfigV2;
import net.spell_engine.api.loot.LootHelper;
import net.spell_engine.rpg_series.config.Defaults;
import net.tinyconfig.ConfigManager;

import java.util.HashMap;

public class RPGSeriesCore {
    public static final String NAMESPACE = "rpg_series";

    public static ConfigManager<LootConfigV2> lootConfig = new ConfigManager<>
            ("loot_v2", Defaults.lootConfig)
            .builder()
            .setDirectory(NAMESPACE)
            .sanitize(true)
            .constrain(LootConfigV2::constrainValues)
            .build();


    public static void initialize() {
        lootConfig.refresh();
        LootHelper.TAG_CACHE.refresh();
        LootTableEvents.MODIFY.register((resourceManager, lootManager, id, tableBuilder, source) -> {
            LootHelper.configureV2(id, tableBuilder, lootConfig.value, new HashMap<>());
        });
        ServerLifecycleEvents.SERVER_STARTED.register((server) -> {
            LootHelper.updateTagCache(lootConfig.value);
        });
        ServerLifecycleEvents.END_DATA_PACK_RELOAD.register((server, serverResourceManager, success) -> {
            LootHelper.updateTagCache(lootConfig.value);
        });
    }
}
