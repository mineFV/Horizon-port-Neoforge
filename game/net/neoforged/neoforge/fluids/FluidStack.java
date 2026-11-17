package net.neoforged.neoforge.fluids;

import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.level.material.Fluids;
import net.neoforged.neoforge.common.MutableDataComponentHolder;

/**
 * The stand-in for NeoForge's FluidStack: a fluid and an amount, observed in a server (`standins --horizon-src`).
 * Data components aren't modelled.
 */
public final class FluidStack implements MutableDataComponentHolder {
    public static final FluidStack EMPTY = new FluidStack((Void) null);

    private final Fluid fluid;
    private int amount;

    private FluidStack(Void nothing) {
        this.fluid = null;
    }

    public FluidStack(Fluid fluid, int amount) {
        this.fluid = fluid;
        this.amount = amount;
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
        return isEmpty() ? EMPTY : new FluidStack(fluid, amount);
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
        return a.getFluid() == b.getFluid();
    }

    public static int hashFluidAndComponents(FluidStack stack) {
        return stack == null ? 0 : 31 * (31 + stack.getFluid().hashCode());
    }

    @Override
    public String toString() {
        return getAmount() + " " + getFluid().horizonId();
    }
}
