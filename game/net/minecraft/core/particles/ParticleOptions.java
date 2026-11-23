package net.minecraft.core.particles;

/** The stand-in for the game's ParticleOptions: a particle's kind and settings, as a mod names them. */
public interface ParticleOptions {
    ParticleType<?> getType();
}
