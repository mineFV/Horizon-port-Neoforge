package horizonneo.probe;

import java.util.function.Supplier;
import net.minecraft.world.item.Rarity;
import net.neoforged.neoforge.fluids.FluidType;

/** NeoForge's fluid types: their properties' defaults, descriptions and texts (in a server). */
final class FluidTypeProbe {
    private FluidTypeProbe() {}

    static void run() {
        p("fluid type default", () -> {
            FluidType t = new FluidType(FluidType.Properties.create());
            return t.getLightLevel() + " " + t.getDensity() + " " + t.getTemperature() + " " + t.getViscosity() + " " + t.getRarity() + " " + t.getDescriptionId() + " " + t.getDescription() + " " + t;
        });
        p("fluid type set", () -> {
            FluidType t = new FluidType(FluidType.Properties.create().descriptionId("block.x.y").lightLevel(7).density(-3).temperature(1300).viscosity(6000).rarity(Rarity.EPIC));
            return t.getLightLevel() + " " + t.getDensity() + " " + t.getTemperature() + " " + t.getViscosity() + " " + t.getRarity() + " " + t.getDescriptionId() + " " + t.getDescription() + " " + t
                    + " " + FluidType.BUCKET_VOLUME;
        });
        p("fluid type air", () -> net.neoforged.neoforge.common.NeoForgeMod.EMPTY_TYPE.value().isAir() + " " + net.neoforged.neoforge.common.NeoForgeMod.WATER_TYPE.value().isAir()
                + " " + new FluidType(FluidType.Properties.create()).isAir());
        p("fluid type sounds", () -> {
            net.minecraft.sounds.SoundEvent fill = net.minecraft.sounds.SoundEvent.createVariableRangeEvent(net.minecraft.resources.ResourceLocation.parse("x:fill"));
            FluidType t = new FluidType(FluidType.Properties.create().sound(net.neoforged.neoforge.common.SoundActions.BUCKET_FILL, fill));
            return (t.getSound(net.neoforged.neoforge.common.SoundActions.BUCKET_FILL) == fill) + " " + t.getSound(net.neoforged.neoforge.common.SoundActions.BUCKET_EMPTY)
                    + " " + (t.getSound(null, null, null, net.neoforged.neoforge.common.SoundActions.BUCKET_FILL) == fill) + " " + net.neoforged.neoforge.common.SoundActions.FLUID_VAPORIZE
                    + " " + net.neoforged.neoforge.common.SoundActions.CAULDRON_DRIP.name() + " " + (net.neoforged.neoforge.common.SoundAction.get("bucket_fill") == net.neoforged.neoforge.common.SoundActions.BUCKET_FILL)
                    + " " + net.neoforged.neoforge.common.SoundAction.get("other");
        });
        p("sound events", () -> {
            net.minecraft.sounds.SoundEvent v = net.minecraft.sounds.SoundEvent.createVariableRangeEvent(net.minecraft.resources.ResourceLocation.parse("x:v"));
            net.minecraft.sounds.SoundEvent f = net.minecraft.sounds.SoundEvent.createFixedRangeEvent(net.minecraft.resources.ResourceLocation.parse("x:f"), 7.5F);
            return v.getLocation() + " " + v.getRange(0.5F) + " " + v.getRange(1F) + " " + v.getRange(3F) + " " + f.getLocation() + " " + f.getRange(0.5F) + " " + f.getRange(3F);
        });
        p("flowing fluids", () -> (net.minecraft.world.level.material.Fluids.EMPTY instanceof net.minecraft.world.level.material.FlowingFluid) + " "
                + net.minecraft.world.level.material.Fluids.WATER + " " + net.minecraft.world.level.material.Fluids.FLOWING_WATER + " " + net.minecraft.world.level.material.Fluids.LAVA
                + " " + net.minecraft.world.level.material.Fluids.FLOWING_LAVA + " " + net.minecraft.world.level.material.Fluids.FLOWING_WATER.getFluidType()
                + " " + (net.minecraft.core.registries.BuiltInRegistries.FLUID.get(net.minecraft.resources.ResourceLocation.parse("minecraft:flowing_lava")) == net.minecraft.world.level.material.Fluids.FLOWING_LAVA));
        p("fluid type empty", () -> describe(net.neoforged.neoforge.common.NeoForgeMod.EMPTY_TYPE));
        p("fluid type water", () -> describe(net.neoforged.neoforge.common.NeoForgeMod.WATER_TYPE));
        p("fluid type lava", () -> describe(net.neoforged.neoforge.common.NeoForgeMod.LAVA_TYPE));
        p("fluid of types", () -> {
            return (net.minecraft.world.level.material.Fluids.EMPTY.getFluidType() == net.neoforged.neoforge.common.NeoForgeMod.EMPTY_TYPE.value()) + " "
                    + (net.minecraft.world.level.material.Fluids.WATER.getFluidType() == net.neoforged.neoforge.common.NeoForgeMod.WATER_TYPE.value()) + " "
                    + (net.minecraft.world.level.material.Fluids.LAVA.getFluidType() == net.neoforged.neoforge.common.NeoForgeMod.LAVA_TYPE.value()) + " "
                    + net.minecraft.core.registries.BuiltInRegistries.FLUID.get(net.minecraft.resources.ResourceLocation.parse("minecraft:flowing_water")).getFluidType() + " "
                    + net.minecraft.core.registries.BuiltInRegistries.FLUID.get(net.minecraft.resources.ResourceLocation.parse("minecraft:flowing_lava")).getFluidType();
        });
        p("fluid stack names", () -> {
            net.neoforged.neoforge.fluids.FluidStack w = new net.neoforged.neoforge.fluids.FluidStack(net.minecraft.world.level.material.Fluids.WATER, 5);
            net.neoforged.neoforge.fluids.FluidStack h = new net.neoforged.neoforge.fluids.FluidStack(net.minecraft.world.level.material.Fluids.LAVA.builtInRegistryHolder(), 3);
            return w.getHoverName() + " " + h.getHoverName() + " " + net.neoforged.neoforge.fluids.FluidStack.EMPTY.getHoverName() + " " + h + " " + w.isComponentsPatchEmpty()
                    + " " + w.is(net.minecraft.world.level.material.Fluids.WATER) + " " + w.is(net.minecraft.world.level.material.Fluids.FLOWING_WATER) + " "
                    + h.is(net.minecraft.world.level.material.Fluids.LAVA.builtInRegistryHolder()) + " " + h.is(net.minecraft.world.level.material.Fluids.WATER.builtInRegistryHolder());
        });
        p("fluid stack type", () -> {
            net.neoforged.neoforge.fluids.FluidStack s = new net.neoforged.neoforge.fluids.FluidStack(net.minecraft.world.level.material.Fluids.WATER, 5);
            return s.getFluidType() + " " + s.is(net.neoforged.neoforge.common.NeoForgeMod.WATER_TYPE.value()) + " " + s.is(net.neoforged.neoforge.common.NeoForgeMod.LAVA_TYPE.value())
                    + " " + net.neoforged.neoforge.fluids.FluidStack.EMPTY.getFluidType() + " " + s.getFluidType().getDescription();
        });
    }

    private static String describe(net.minecraft.core.Holder<FluidType> h) {
        FluidType t = h.value();
        return t.getLightLevel() + " " + t.getDensity() + " " + t.getTemperature() + " " + t.getViscosity() + " " + t.getRarity() + " " + t.getDescriptionId() + " " + t
                + " " + h.unwrapKey().map(Object::toString).orElse("none");
    }

    static void p(String key, Supplier<Object> call) {
        GameProbe.p(key, call);
    }
}
