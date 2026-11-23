package net.minecraft.core.particles;

/**
 * The stand-in for the game's ParticleType: whether it ignores the client's particle limit, observed in a server
 * (`standins --horizon-src`, probe/ChunkProbe.java). Not here: its codecs, the registry it is in.
 */
public abstract class ParticleType<T extends ParticleOptions> {
    private final boolean overrideLimiter;

    protected ParticleType(boolean overrideLimiter) {
        this.overrideLimiter = overrideLimiter;
    }

    public boolean getOverrideLimiter() {
        return overrideLimiter;
    }
}
