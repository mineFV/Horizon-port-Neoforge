package net.neoforged.neoforge.common;

/**
 * The stand-in for NeoForge's NeoForgeMod: what a mod asks of NeoForge's own content. Milk as a fluid is NeoForge's
 * content, not the mod's: here the request is kept, for Horizon's side to honour (it isn't yet).
 */
public class NeoForgeMod {
    private static boolean milkFluid;

    /** The game's fluid types, as NeoForge registers them (observed, FluidTypeProbe). */
    public static final net.minecraft.core.Holder<net.neoforged.neoforge.fluids.FluidType> EMPTY_TYPE = type("empty",
            net.neoforged.neoforge.fluids.FluidType.Properties.create().descriptionId("block.minecraft.air").density(0).temperature(0).viscosity(0));
    public static final net.minecraft.core.Holder<net.neoforged.neoforge.fluids.FluidType> WATER_TYPE = type("water",
            net.neoforged.neoforge.fluids.FluidType.Properties.create().descriptionId("block.minecraft.water"));
    public static final net.minecraft.core.Holder<net.neoforged.neoforge.fluids.FluidType> LAVA_TYPE = type("lava",
            net.neoforged.neoforge.fluids.FluidType.Properties.create().descriptionId("block.minecraft.lava").lightLevel(15).density(3000).viscosity(6000).temperature(1300));

    private static net.minecraft.core.Holder<net.neoforged.neoforge.fluids.FluidType> type(String name, net.neoforged.neoforge.fluids.FluidType.Properties properties) {
        net.minecraft.resources.ResourceKey<net.neoforged.neoforge.fluids.FluidType> key = net.minecraft.resources.ResourceKey.create(
                net.minecraft.resources.ResourceKey.createRegistryKey(net.minecraft.resources.ResourceLocation.parse("neoforge:fluid_type")),
                net.minecraft.resources.ResourceLocation.withDefaultNamespace(name));
        return net.neoforged.neoforge.registries.HorizonRegistries.bind(key, new net.neoforged.neoforge.fluids.FluidType(properties));
    }

    private NeoForgeMod() {}

    public static void enableMilkFluid() {
        milkFluid = true;
    }

    /** Not the game's: whether a mod asked for milk as a fluid. */
    public static boolean horizonMilkFluid() {
        return milkFluid;
    }
}
