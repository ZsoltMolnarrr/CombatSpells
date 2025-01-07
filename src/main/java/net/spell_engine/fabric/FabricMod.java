package net.spell_engine.fabric;

import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.entity.event.v1.ServerLivingEntityEvents;
import net.fabricmc.fabric.api.object.builder.v1.entity.FabricEntityTypeBuilder;
import net.minecraft.entity.EntityDimensions;
import net.minecraft.entity.SpawnGroup;
import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;
import net.minecraft.util.Identifier;
import net.spell_engine.SpellEngineMod;
import net.spell_engine.api.effect.RemoveOnHit;
import net.spell_engine.compat.trinkets.TrinketsCompat;
import net.spell_engine.entity.SpellCloud;
import net.spell_engine.entity.SpellProjectile;
import net.spell_engine.item.SpellEngineItems;
import net.spell_engine.utils.SoundHelper;

public class FabricMod implements ModInitializer {
    static {
        SpellProjectile.ENTITY_TYPE = Registry.register(
                Registries.ENTITY_TYPE,
                Identifier.of(SpellEngineMod.ID, "spell_projectile"),
                FabricEntityTypeBuilder.<SpellProjectile>create(SpawnGroup.MISC, SpellProjectile::new)
                        .dimensions(EntityDimensions.fixed(0.25F, 0.25F)) // dimensions in Minecraft units of the render
                        .fireImmune()
                        .trackRangeBlocks(128)
                        .trackedUpdateRate(2)
                        .build()
        );
        SpellCloud.ENTITY_TYPE = Registry.register(
                Registries.ENTITY_TYPE,
                Identifier.of(SpellEngineMod.ID, "spell_area_effect"),
                FabricEntityTypeBuilder.<SpellCloud>create(SpawnGroup.MISC, SpellCloud::new)
                        .dimensions(EntityDimensions.changing(6F, 0.5F)) // dimensions in Minecraft units of the render
                        .fireImmune()
                        .trackRangeBlocks(128)
                        .trackedUpdateRate(20)
                        .build()
        );
    }

    @Override
    public void onInitialize() {
        SpellEngineMod.init();
        SpellEngineMod.registerSpellBinding();
        SoundHelper.registerSounds();
        TrinketsCompat.init();
        SpellEngineItems.register();

        ServerLivingEntityEvents.ALLOW_DAMAGE.register((entity, source, amount) -> {
            var attacker = source.getAttacker();
            if (amount > 0 && attacker != null) {
                for (var instance : entity.getStatusEffects()) {
                    var effect = instance.getEffectType();
                    if (RemoveOnHit.shouldRemoveOnDirectHit(effect.value())) {
                        entity.removeStatusEffect(effect);
                        break;
                    }
                }
            }
            return true;
        });
    }
}