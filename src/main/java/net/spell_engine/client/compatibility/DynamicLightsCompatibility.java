package net.spell_engine.client.compatibility;

import dev.lambdaurora.lambdynlights.api.DynamicLightHandlers;
import dev.lambdaurora.lambdynlights.api.DynamicLightsInitializer;
import dev.lambdaurora.lambdynlights.api.item.ItemLightSourceManager;
import net.spell_engine.entity.SpellCloud;
import net.spell_engine.entity.SpellProjectile;

public class DynamicLightsCompatibility implements DynamicLightsInitializer {
    @Override
    public void onInitializeDynamicLights(ItemLightSourceManager itemLightSourceManager) {
        System.out.println("Spell Engine: Initializing Dynamic Lights compatibility...");
        DynamicLightHandlers.registerDynamicLightHandler(SpellProjectile.ENTITY_TYPE, entity -> {
            var luminance = 0;
            var data = entity.projectileData();
            if (data != null && data.client_data != null) {
                luminance = data.client_data.light_level;
            }
            return luminance;
        });

        DynamicLightHandlers.registerDynamicLightHandler(SpellCloud.ENTITY_TYPE, entity -> {
            var luminance = 0;
            var data = entity.getCloudData();
            if (data != null && data.client_data != null) {
                luminance = data.client_data.light_level;
            }
            return luminance;
        });
    }
}
