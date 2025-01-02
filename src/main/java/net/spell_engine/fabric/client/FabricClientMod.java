package net.spell_engine.fabric.client;

import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.item.v1.ItemTooltipCallback;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.fabricmc.fabric.api.client.model.loading.v1.ModelLoadingPlugin;
import net.fabricmc.fabric.api.client.particle.v1.ParticleFactoryRegistry;
import net.fabricmc.fabric.api.client.rendering.v1.EntityRendererRegistry;
import net.fabricmc.fabric.api.client.rendering.v1.HudRenderCallback;
import net.minecraft.client.MinecraftClient;
import net.spell_engine.client.SpellEngineClient;
import net.spell_engine.client.gui.HudRenderHelper;
import net.spell_engine.client.gui.SpellTooltip;
import net.spell_engine.client.input.Keybindings;
import net.spell_engine.client.particle.*;
import net.spell_engine.client.render.CustomModelRegistry;
import net.spell_engine.client.render.SpellCloudRenderer;
import net.spell_engine.client.render.SpellProjectileRenderer;
import net.spell_engine.entity.SpellCloud;
import net.spell_engine.entity.SpellProjectile;
import net.spell_engine.particle.Particles;

public class FabricClientMod implements ClientModInitializer {
    @Override
    public void onInitializeClient() {
        SpellEngineClient.initialize();
        registerKeyBindings();

        // HudRenderCallback.EVENT.register - had issues, rendering my content in random order
        // Invocation of HudRenderHelper.render() moved into InGameHudMixin
//        HudRenderCallback.EVENT.register((context, tickCounter) -> {
//            if (!MinecraftClient.getInstance().options.hudHidden) {
//                HudRenderHelper.render(context, tickCounter.getTickDelta(true));
//            }
//        });

        ItemTooltipCallback.EVENT.register((itemStack, tooltipContext, tooltipType, lines) -> {
            SpellTooltip.addSpellInfo(itemStack, lines);
        });
        EntityRendererRegistry.register(SpellProjectile.ENTITY_TYPE, SpellProjectileRenderer::new);
        EntityRendererRegistry.register(SpellCloud.ENTITY_TYPE, SpellCloudRenderer::new);

        registerParticleAppearances();
    }

    private void registerParticleAppearances() {
        /* Adds our particle textures to vanilla's Texture Atlas so it can be shown properly.
         * Modify the namespace and particle id accordingly.
         *
         * This is only used if you plan to add your own textures for the particle. Otherwise, remove  this.*/
//        ClientSpriteRegistryCallback.event(PlayerScreenHandler.BLOCK_ATLAS_TEXTURE).register(((atlasTexture, registry) -> {
//            for(var entry: Particles.all()) {
//                if (entry.usesCustomTexture) {
//                    registry.register(entry.id);
//                }
//            }
//        }));

        /* Registers our particle client-side.
         * First argument is our particle's instance, created previously on ExampleMod.
         * Second argument is the particle's factory. The factory controls how the particle behaves.
         * In this example, we'll use FlameParticle's Factory.*/
        ParticleFactoryRegistry.getInstance().register(Particles.arcane_hit.particleType, SpellHitParticle.ArcaneFactory::new);
        ParticleFactoryRegistry.getInstance().register(Particles.arcane_spell.particleType, GenericSpellParticle.ArcaneSpellFactory::new);
        ParticleFactoryRegistry.getInstance().register(Particles.healing_ascend.particleType, SpellFlameParticle.HealingFactory::new);
        ParticleFactoryRegistry.getInstance().register(Particles.holy_hit.particleType, SpellHitParticle.HolyFactory::new);
        ParticleFactoryRegistry.getInstance().register(Particles.holy_ascend.particleType, SpellFlameParticle.HolyFactory::new);
        ParticleFactoryRegistry.getInstance().register(Particles.holy_spark_mini.particleType, SpellFlameParticle.HolyFactory::new);
        ParticleFactoryRegistry.getInstance().register(Particles.holy_spark.particleType, SpellSnowflakeParticle.HolyFactory::new);
        ParticleFactoryRegistry.getInstance().register(Particles.holy_spell.particleType, GenericSpellParticle.HolySpellFactory::new);
        ParticleFactoryRegistry.getInstance().register(Particles.nature_spark_mini.particleType, SpellFlameParticle.NatureFactory::new);
        ParticleFactoryRegistry.getInstance().register(Particles.nature_spark_mini_slowing.particleType, SpellFlameParticle.NatureSlowingFactory::new);
        ParticleFactoryRegistry.getInstance().register(Particles.white_spark_mini.particleType, SpellFlameParticle.WhiteFactory::new);
        ParticleFactoryRegistry.getInstance().register(Particles.fire_explosion.particleType, SpellExplosionParticle.Factory::new);
        ParticleFactoryRegistry.getInstance().register(Particles.flame.particleType, SpellFlameParticle.FlameFactory::new);
        ParticleFactoryRegistry.getInstance().register(Particles.flame_spark.particleType, SpellFlameParticle.AnimatedFlameFactory::new);
        ParticleFactoryRegistry.getInstance().register(Particles.flame_ground.particleType, SpellFlameParticle.AnimatedFlameFactory::new);
        ParticleFactoryRegistry.getInstance().register(Particles.flame_medium_a.particleType, SpellFlameParticle.MediumFlameFactory::new);
        ParticleFactoryRegistry.getInstance().register(Particles.flame_medium_b.particleType, SpellFlameParticle.MediumFlameFactory::new);
        ParticleFactoryRegistry.getInstance().register(Particles.snowflake.particleType, SpellSnowflakeParticle.FrostFactory::new);
        ParticleFactoryRegistry.getInstance().register(Particles.frost_hit.particleType, SpellHitParticle.FrostFactory::new);
        ParticleFactoryRegistry.getInstance().register(Particles.frost_shard.particleType, SpellFlameParticle.FrostShard::new);
        ParticleFactoryRegistry.getInstance().register(Particles.dripping_blood.particleType, SpellSnowflakeParticle.DrippingBloodFactory::new);
        ParticleFactoryRegistry.getInstance().register(Particles.roots.particleType, ShiftedParticle.RootsFactory::new);
        ParticleFactoryRegistry.getInstance().register(Particles.electric_arc_A.particleType, SpellFlameParticle.ElectricSparkFactory::new);
        ParticleFactoryRegistry.getInstance().register(Particles.electric_arc_B.particleType, SpellFlameParticle.ElectricSparkFactory::new);
        ParticleFactoryRegistry.getInstance().register(Particles.smoke_medium.particleType, SpellFlameParticle.SmokeFactory::new);
        ParticleFactoryRegistry.getInstance().register(Particles.weakness_smoke.particleType, SpellFlameParticle.WeaknessSmokeFactory::new);
        ParticleFactoryRegistry.getInstance().register(Particles.buff_rage.particleType, SpellFlameParticle.BuffRageFactory::new);
        ParticleFactoryRegistry.getInstance().register(Particles.sign_charge.particleType, SpellFlameParticle.RageSignFactory::new);

        ModelLoadingPlugin.register(pluginCtx -> {
            pluginCtx.addModels(CustomModelRegistry.modelIds);
        });
    }

    private void registerKeyBindings() {
        for(var keybinding: Keybindings.all()) {
            KeyBindingHelper.registerKeyBinding(keybinding);
        }
    }
}
