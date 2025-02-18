package net.spell_engine.api.render;

import net.minecraft.entity.LivingEntity;
import net.spell_engine.api.effect.CustomParticleStatusEffect;
import net.spell_engine.api.spell.fx.ParticleBatch;
import net.spell_engine.fx.ParticleHelper;

import java.util.List;

public class BuffParticleSpawner implements CustomParticleStatusEffect.Spawner {
    private final ParticleBatch[] particles;

    private static ParticleBatch defaultBatch(String particleId, int particleCount, float min_speed, float max_speed) {
        return new ParticleBatch(
                particleId,
                ParticleBatch.Shape.WIDE_PIPE,
                ParticleBatch.Origin.FEET,
                null,
                particleCount,
                min_speed,
                max_speed,
                0,
                -0.2F);
    }

    public BuffParticleSpawner(List<String> particleIds, int particleCount, float min_speed, float max_speed) {
        this.particles = new ParticleBatch[particleIds.size()];
        for (int i = 0; i < particleIds.size(); i++) {
            particles[i] = defaultBatch(particleIds.get(i), particleCount, min_speed, max_speed);
        }
    }

    public BuffParticleSpawner(String particleId, int particleCount, float min_speed, float max_speed) {
        this.particles = new ParticleBatch[] { defaultBatch(particleId, particleCount, min_speed, max_speed) };
    }

    public BuffParticleSpawner(String particleId, int particleCount) {
        this(particleId, particleCount, 0.11F, 0.12F);
    }

    public BuffParticleSpawner(ParticleBatch[] particles) {
        this.particles = particles;
    }

    @Override
    public void spawnParticles(LivingEntity livingEntity, int amplifier) {
        var scaledParticles = new ParticleBatch[particles.length];
        for (int i = 0; i < particles.length; i++) {
            var copiedBatch = new ParticleBatch(particles[i]);
            copiedBatch.count *= (amplifier + 1);
            scaledParticles[i] = copiedBatch;
        }
        ParticleHelper.play(livingEntity.getWorld(), livingEntity, scaledParticles);
    }
}