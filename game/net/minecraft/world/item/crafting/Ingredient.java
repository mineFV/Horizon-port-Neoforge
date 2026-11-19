package net.minecraft.world.item.crafting;

/** The stand-in for the game's Ingredient: its type alone (recipes aren't modelled): testing a stack refuses. */
public final class Ingredient implements java.util.function.Predicate<net.minecraft.world.item.ItemStack> {
    private Ingredient() {}

    @Override
    public boolean equals(Object other) {
        throw new UnsupportedOperationException("ingredients aren't modelled (horizon-port-neoforge)");
    }

    @Override
    public int hashCode() {
        throw new UnsupportedOperationException("ingredients aren't modelled (horizon-port-neoforge)");
    }

    @Override
    public String toString() {
        throw new UnsupportedOperationException("ingredients aren't modelled (horizon-port-neoforge)");
    }

    @Override
    public boolean test(net.minecraft.world.item.ItemStack stack) {
        throw new UnsupportedOperationException("ingredients aren't modelled (horizon-port-neoforge)");
    }
}
