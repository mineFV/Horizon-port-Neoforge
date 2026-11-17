package net.neoforged.fml.loading;

import net.neoforged.api.distmarker.Dist;

/**
 * The stand-in for NeoForge's FMLEnvironment. The translated Java runs the mod's server side (the bridge ticks block
 * entities on Horizon's server), and its client side (rendering) is Horizon's own: so a dedicated server's, and a
 * production game's.
 */
public class FMLEnvironment {
    public static final Dist dist = Dist.DEDICATED_SERVER;
    public static final boolean production = true;

    private FMLEnvironment() {}
}
