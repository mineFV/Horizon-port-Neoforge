package net.neoforged.neoforge.fluids;

import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Rarity;

/**
 * The stand-in for NeoForge's FluidType: a fluid's description and its properties (light, density, temperature,
 * viscosity, rarity) and sounds, from its Properties (observed, `standins --horizon-src`). What it does in a world
 * (placing, vaporizing, its bucket) isn't modelled: those throw.
 */
public class FluidType {
    public static final int BUCKET_VOLUME = 1000;

    private String descriptionId;
    private final int lightLevel;
    private final int density;
    private final int temperature;
    private final int viscosity;
    private final Rarity rarity;
    private final java.util.Map<net.neoforged.neoforge.common.SoundAction, net.minecraft.sounds.SoundEvent> sounds;

    public FluidType(Properties properties) {
        this.descriptionId = properties.descriptionId;
        this.lightLevel = properties.lightLevel;
        this.density = properties.density;
        this.temperature = properties.temperature;
        this.viscosity = properties.viscosity;
        this.rarity = properties.rarity;
        this.sounds = new java.util.HashMap<>(properties.sounds);
    }

    /** Observed: only NeoForge's empty type is air. */
    public final boolean isAir() {
        return this == net.neoforged.neoforge.common.NeoForgeMod.EMPTY_TYPE.value();
    }

    /** Its properties' sound for action; null when they gave none. */
    public net.minecraft.sounds.SoundEvent getSound(net.neoforged.neoforge.common.SoundAction action) {
        return sounds.get(action);
    }

    public net.minecraft.sounds.SoundEvent getSound(net.minecraft.world.entity.player.Player player, net.minecraft.world.level.BlockGetter getter, net.minecraft.core.BlockPos pos,
            net.neoforged.neoforge.common.SoundAction action) {
        return getSound(action);
    }

    public net.minecraft.world.item.ItemStack getBucket(FluidStack stack) {
        throw new UnsupportedOperationException("a fluid's bucket isn't modelled");
    }

    public boolean canBePlacedInLevel(net.minecraft.world.level.BlockAndTintGetter getter, net.minecraft.core.BlockPos pos, FluidStack stack) {
        throw new UnsupportedOperationException("placing a fluid isn't modelled");
    }

    public net.minecraft.world.level.material.FluidState getStateForPlacement(net.minecraft.world.level.BlockAndTintGetter getter, net.minecraft.core.BlockPos pos, FluidStack stack) {
        throw new UnsupportedOperationException("placing a fluid isn't modelled");
    }

    public boolean isVaporizedOnPlacement(net.minecraft.world.level.Level level, net.minecraft.core.BlockPos pos, FluidStack stack) {
        throw new UnsupportedOperationException("a fluid vaporizing isn't modelled");
    }

    public void onVaporize(net.minecraft.world.entity.player.Player player, net.minecraft.world.level.Level level, net.minecraft.core.BlockPos pos, FluidStack stack) {
        throw new UnsupportedOperationException("a fluid vaporizing isn't modelled");
    }

    public Component getDescription() {
        return Component.translatable(getDescriptionId());
    }

    /** Observed: its properties' or, when they gave none, "fluid_type." and its registered id. */
    public String getDescriptionId() {
        if (descriptionId == null) descriptionId = net.minecraft.Util.makeDescriptionId("fluid_type", net.neoforged.neoforge.registries.HorizonRegistries.idOf(this));
        return descriptionId;
    }

    public int getLightLevel() {
        return lightLevel;
    }

    public int getDensity() {
        return density;
    }

    public int getTemperature() {
        return temperature;
    }

    public int getViscosity() {
        return viscosity;
    }

    public Rarity getRarity() {
        return rarity;
    }

    public Component getDescription(FluidStack stack) {
        return Component.translatable(getDescriptionId(stack));
    }

    public String getDescriptionId(FluidStack stack) {
        return getDescriptionId();
    }

    public int getLightLevel(FluidStack stack) {
        return getLightLevel();
    }

    public int getDensity(FluidStack stack) {
        return getDensity();
    }

    public int getTemperature(FluidStack stack) {
        return getTemperature();
    }

    public int getViscosity(FluidStack stack) {
        return getViscosity();
    }

    public Rarity getRarity(FluidStack stack) {
        return getRarity();
    }

    /** Observed: its registered id, else "Unregistered FluidType". */
    @Override
    public String toString() {
        net.minecraft.resources.ResourceLocation id = net.neoforged.neoforge.registries.HorizonRegistries.idOf(this);
        return id != null ? id.toString() : "Unregistered FluidType";
    }

    /** The stand-in for FluidType.Properties: its defaults observed. */
    public static final class Properties {
        String descriptionId;
        int lightLevel = 0;
        int density = 1000;
        int temperature = 300;
        int viscosity = 1000;
        Rarity rarity = Rarity.COMMON;
        final java.util.Map<net.neoforged.neoforge.common.SoundAction, net.minecraft.sounds.SoundEvent> sounds = new java.util.HashMap<>();

        private Properties() {}

        public static Properties create() {
            return new Properties();
        }

        public Properties descriptionId(String id) {
            descriptionId = id;
            return this;
        }

        public Properties lightLevel(int level) {
            lightLevel = level;
            return this;
        }

        public Properties density(int d) {
            density = d;
            return this;
        }

        public Properties temperature(int t) {
            temperature = t;
            return this;
        }

        public Properties viscosity(int v) {
            viscosity = v;
            return this;
        }

        public Properties sound(net.neoforged.neoforge.common.SoundAction action, net.minecraft.sounds.SoundEvent sound) {
            sounds.put(action, sound);
            return this;
        }

        public Properties rarity(Rarity r) {
            rarity = r;
            return this;
        }
    }
}
