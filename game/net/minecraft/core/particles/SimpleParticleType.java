package net.minecraft.core.particles;

/** The stand-in for the game's SimpleParticleType: a kind of particle with no settings, its own options. */
public class SimpleParticleType extends ParticleType<SimpleParticleType> implements ParticleOptions {
    protected SimpleParticleType(boolean overrideLimiter) {
        super(overrideLimiter);
    }

    @Override
    public SimpleParticleType getType() {
        return this;
    }
}
