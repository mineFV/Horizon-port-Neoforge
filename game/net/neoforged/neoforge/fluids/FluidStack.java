package net.neoforged.neoforge.fluids;

import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.level.material.Fluids;
import net.neoforged.neoforge.common.MutableDataComponentHolder;

/**
 * The stand-in for NeoForge's FluidStack: a fluid, an amount and components (none of the fluid's own: what the stack
 * set), observed in a server (`standins --horizon-src`).
 */
public final class FluidStack implements MutableDataComponentHolder {
    public static final FluidStack EMPTY = new FluidStack((Void) null);

    // The game's codecs: built as objects that don't run yet (HorizonCodecs, HorizonStreamCodecs).
    public static final com.mojang.serialization.Codec<FluidStack> CODEC = com.mojang.serialization.HorizonCodecs.game("FluidStack.CODEC");
    public static final com.mojang.serialization.Codec<FluidStack> OPTIONAL_CODEC = com.mojang.serialization.HorizonCodecs.game("FluidStack.OPTIONAL_CODEC");
    public static final net.minecraft.network.codec.StreamCodec<Object, FluidStack> STREAM_CODEC = new net.minecraft.network.codec.HorizonStreamCodecs.Opaque<>("FluidStack.STREAM_CODEC");
    public static final net.minecraft.network.codec.StreamCodec<Object, FluidStack> OPTIONAL_STREAM_CODEC = new net.minecraft.network.codec.HorizonStreamCodecs.Opaque<>("FluidStack.OPTIONAL_STREAM_CODEC");

    private final Fluid fluid;
    private int amount;
    private final net.minecraft.core.component.PatchedDataComponentMap components;

    private FluidStack(Void nothing) {
        this.fluid = null;
        this.components = new net.minecraft.core.component.PatchedDataComponentMap(net.minecraft.core.component.DataComponentMap.EMPTY);
    }

    public FluidStack(Fluid fluid, int amount) {
        this(fluid, amount, new net.minecraft.core.component.PatchedDataComponentMap(net.minecraft.core.component.DataComponentMap.EMPTY));
    }

    private FluidStack(Fluid fluid, int amount, net.minecraft.core.component.PatchedDataComponentMap components) {
        this.fluid = fluid;
        this.amount = amount;
        this.components = components;
    }

    @Override
    public net.minecraft.core.component.DataComponentMap getComponents() {
        return components;
    }

    public net.minecraft.core.component.DataComponentPatch getComponentsPatch() {
        return isEmpty() ? net.minecraft.core.component.DataComponentPatch.EMPTY : components.asPatch();
    }

    @Override
    public <T> T set(net.minecraft.core.component.DataComponentType<? super T> type, T value) {
        return components.set(type, value);
    }

    @Override
    public <T> T remove(net.minecraft.core.component.DataComponentType<? extends T> type) {
        return components.remove(type);
    }

    public boolean isEmpty() {
        return this == EMPTY || fluid == Fluids.EMPTY || amount <= 0;
    }

    public Fluid getFluid() {
        return isEmpty() ? Fluids.EMPTY : fluid;
    }

    public int getAmount() {
        return isEmpty() ? 0 : amount;
    }

    public void setAmount(int amount) {
        this.amount = amount;
    }

    public void grow(int n) {
        setAmount(getAmount() + n);
    }

    public void shrink(int n) {
        grow(-n);
    }

    public FluidStack copy() {
        return isEmpty() ? EMPTY : new FluidStack(fluid, amount, components.copy());
    }

    public FluidStack copyWithAmount(int n) {
        if (isEmpty()) return EMPTY;
        FluidStack c = copy();
        c.setAmount(n);
        return c;
    }

    public static boolean matches(FluidStack a, FluidStack b) {
        return a == b || a.getAmount() == b.getAmount() && isSameFluidSameComponents(a, b);
    }

    public static boolean isSameFluid(FluidStack a, FluidStack b) {
        return a.getFluid() == b.getFluid();
    }

    public static boolean isSameFluidSameComponents(FluidStack a, FluidStack b) {
        if (a.getFluid() != b.getFluid()) return false;
        return a.isEmpty() && b.isEmpty() || a.getComponentsPatch().equals(b.getComponentsPatch());
    }

    /** Only whether two are equal was observed (a fluid's hash is its identity's). */
    public static int hashFluidAndComponents(FluidStack stack) {
        return stack == null ? 0 : 31 * (31 + stack.getFluid().hashCode()) + stack.getComponentsPatch().hashCode();
    }

    public FluidType getFluidType() {
        return getFluid().getFluidType();
    }

    public boolean is(FluidType type) {
        return getFluidType() == type;
    }

    public boolean is(net.minecraft.tags.TagKey<Fluid> tag) {
        return getFluidHolder().is(tag);
    }

    public net.minecraft.core.Holder<Fluid> getFluidHolder() {
        return getFluid().builtInRegistryHolder();
    }

    /** Observed in a server (`standins --horizon-src`): {amount, id}; an empty stack can't be written. */
    public net.minecraft.nbt.Tag save(net.minecraft.core.HolderLookup.Provider registries) {
        if (isEmpty()) throw new IllegalStateException("Cannot encode empty FluidStack");
        net.minecraft.nbt.CompoundTag tag = new net.minecraft.nbt.CompoundTag();
        tag.putString("id", String.valueOf(getFluid().horizonId()));
        tag.putInt("amount", getAmount());
        net.minecraft.core.component.DataComponentPatch patch = getComponentsPatch();
        if (!patch.isEmpty()) tag.put("components", net.minecraft.core.component.HorizonComponentCodecs.encode(patch));
        return tag;
    }

    public net.minecraft.nbt.Tag saveOptional(net.minecraft.core.HolderLookup.Provider registries) {
        return isEmpty() ? new net.minecraft.nbt.CompoundTag() : save(registries);
    }

    /** Observed: a compound with a known fluid's id that isn't empty, and an amount above 0 (else none). */
    public static java.util.Optional<FluidStack> parse(net.minecraft.core.HolderLookup.Provider registries, net.minecraft.nbt.Tag tag) {
        if (!(tag instanceof net.minecraft.nbt.CompoundTag c) || !c.contains("id", 8) || !c.contains("amount", 99)) return java.util.Optional.empty();
        net.minecraft.resources.ResourceLocation id = net.minecraft.resources.ResourceLocation.tryParse(c.getString("id"));
        if (id == null) return java.util.Optional.empty();
        Fluid fluid = net.neoforged.neoforge.registries.HorizonRegistries.knownFluid(id.toString());
        int amount = c.getInt("amount");
        if (fluid == null || fluid == Fluids.EMPTY || amount <= 0) return java.util.Optional.empty();
        return java.util.Optional.of(new FluidStack(fluid, amount));
    }

    public static FluidStack parseOptional(net.minecraft.core.HolderLookup.Provider registries, net.minecraft.nbt.CompoundTag tag) {
        return tag.isEmpty() ? EMPTY : parse(registries, tag).orElse(EMPTY);
    }

    @Override
    public String toString() {
        return getAmount() + " " + getFluid().horizonId();
    }
}
