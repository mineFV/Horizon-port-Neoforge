package net.minecraft.core;

/** Made up: a stand-in for the game's BlockPos that declares a method the translator's stand-in takes from Vec3i. */
public class BlockPos extends Vec3i {
    @Override
    public int getX() {
        return 1;
    }
}
