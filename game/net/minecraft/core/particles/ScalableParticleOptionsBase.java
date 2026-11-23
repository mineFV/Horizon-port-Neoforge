package net.minecraft.core.particles;

/** The stand-in for the game's ScalableParticleOptionsBase: a particle's scale, observed in a server. */
public abstract class ScalableParticleOptionsBase implements ParticleOptions {
    public static final float MIN_SCALE = 0.01F;
    public static final float MAX_SCALE = 4.0F;
    private final float scale;

    public ScalableParticleOptionsBase(float scale) {
        this.scale = net.minecraft.util.Mth.clamp(scale, MIN_SCALE, MAX_SCALE);
    }

    public float getScale() {
        return scale;
    }
}
