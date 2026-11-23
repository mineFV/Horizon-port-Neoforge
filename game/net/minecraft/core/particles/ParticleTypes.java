package net.minecraft.core.particles;

/**
 * The stand-in for the game's ParticleTypes: the kinds a mod names (Mekanism's: smoke, portal, dust), one object each,
 * whether they ignore the particle limit observed in a server. Not here: the others, the registry they are in.
 */
public class ParticleTypes {
    public static final SimpleParticleType SMOKE = new SimpleParticleType(false);
    public static final SimpleParticleType PORTAL = new SimpleParticleType(false);
    public static final ParticleType<DustParticleOptions> DUST = new ParticleType<DustParticleOptions>(false) {
    };

    public ParticleTypes() {}
}
