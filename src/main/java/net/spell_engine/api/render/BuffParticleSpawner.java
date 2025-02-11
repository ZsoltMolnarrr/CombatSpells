package net.spell_engine.api.render;

import net.minecraft.entity.LivingEntity;
import net.spell_engine.api.effect.CustomParticleStatusEffect;
import net.spell_engine.api.spell.fx.ParticleBatch;
import net.spell_engine.fx.ParticleHelper;

public class BuffParticleSpawner implements CustomParticleStatusEffect.Spawner {
    private final ParticleBatch particles;

    public BuffParticleSpawner(String particleId, int particleCount, float min_speed, float max_speed) {
        this.particles = new ParticleBatch(
                particleId,
                ParticleBatch.Shape.PIPE,
                ParticleBatch.Origin.FEET,
                null,
                particleCount,
                min_speed,
                max_speed,
                0,
                -0.2F);
    }

    public BuffParticleSpawner(String particleId, int particleCount) {
        this(particleId, particleCount, 0.11F, 0.12F);
    }

    @Override
    public void spawnParticles(LivingEntity livingEntity, int amplifier) {
        var scaledParticles = new ParticleBatch(particles);
        scaledParticles.count *= (amplifier + 1);
        ParticleHelper.play(livingEntity.getWorld(), livingEntity, scaledParticles);
    }
}