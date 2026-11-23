package net.minecraft.core.particles;

/**
 * The stand-in for the game's DustParticleOptions: redstone's dust (red, scale 1) and others, their color and scale,
 * observed in a server (`standins --horizon-src`, probe/ChunkProbe.java). Not here: its codecs.
 */
public class DustParticleOptions extends ScalableParticleOptionsBase {
    public static final org.joml.Vector3f REDSTONE_PARTICLE_COLOR = new org.joml.Vector3f(1.0F, 0.0F, 0.0F);
    public static final DustParticleOptions REDSTONE = new DustParticleOptions(REDSTONE_PARTICLE_COLOR, 1.0F);
    private final org.joml.Vector3f color;

    public DustParticleOptions(org.joml.Vector3f color, float scale) {
        super(scale);
        this.color = color;
    }

    @Override
    public ParticleType<DustParticleOptions> getType() {
        return ParticleTypes.DUST;
    }

    public org.joml.Vector3f getColor() {
        return color;
    }
}
