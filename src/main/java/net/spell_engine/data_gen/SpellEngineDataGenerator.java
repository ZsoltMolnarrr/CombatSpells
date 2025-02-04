package net.spell_engine.data_gen;

import net.fabricmc.fabric.api.datagen.v1.DataGeneratorEntrypoint;
import net.fabricmc.fabric.api.datagen.v1.FabricDataGenerator;
import net.fabricmc.fabric.api.datagen.v1.FabricDataOutput;
import net.minecraft.registry.RegistryWrapper;
import net.spell_engine.api.data_gen.SimpleParticleGenerator;
import net.spell_engine.particle.Particles;

import java.util.ArrayList;
import java.util.concurrent.CompletableFuture;

public class SpellEngineDataGenerator implements DataGeneratorEntrypoint {
    @Override
    public void onInitializeDataGenerator(FabricDataGenerator fabricDataGenerator) {
        FabricDataGenerator.Pack pack = fabricDataGenerator.createPack();
        pack.addProvider(ParticlesGen::new);
    }

//    @Override
//    public @Nullable String getEffectiveModId() {
//        return SpellEngineMod.ID;
//    }

    public static class ParticlesGen extends SimpleParticleGenerator {
        public ParticlesGen(FabricDataOutput dataOutput, CompletableFuture<RegistryWrapper.WrapperLookup> registryLookup) {
            super(dataOutput, registryLookup);
        }

        @Override
        public void generateSimpleParticles(Builder builder) {
            for (var variant: Particles.MAGIC_FAMILY_VARIANTS.get()) {
                var textures = new ArrayList<String>();
                switch (variant.shape()) {
                    case SPELL -> {
                        for (int i = 0; i < 8; i++) {
                            textures.add("minecraft:spell_" + i);
                        }
                    }
                    case IMPACT -> {
                        textures.add("spell_engine:magic/impact_" + variant.family());
                    }
                    case SPARK -> {
                        textures.add("minecraft:generic_0");
                    }
                    case STRIPE -> {
                        textures.add("spell_engine:magic/vertical_stripe");
                    }
                }
                if (textures.isEmpty())  {
                    assert true;
                }
                builder.add(variant.id(), new SimpleParticleGenerator.ParticleData(textures));
            }
        }
    }
}
