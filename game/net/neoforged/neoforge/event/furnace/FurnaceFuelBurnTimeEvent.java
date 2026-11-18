package net.neoforged.neoforge.event.furnace;

import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.RecipeType;
import net.neoforged.bus.api.Event;
import net.neoforged.bus.api.ICancellableEvent;

/**
 * The stand-in for NeoForge's FurnaceFuelBurnTimeEvent: what a stack burns for, which listeners may change; posted on
 * NeoForge.EVENT_BUS by ItemStack.getBurnTime (observed, `standins --horizon-src`).
 */
public class FurnaceFuelBurnTimeEvent extends Event implements ICancellableEvent {
    private final ItemStack itemStack;
    private final RecipeType<?> recipeType;
    private int burnTime;

    public FurnaceFuelBurnTimeEvent(ItemStack itemStack, int burnTime, RecipeType<?> recipeType) {
        this.itemStack = itemStack;
        this.burnTime = burnTime;
        this.recipeType = recipeType;
    }

    public ItemStack getItemStack() {
        return itemStack;
    }

    public RecipeType<?> getRecipeType() {
        return recipeType;
    }

    /** Observed: a burn time of 0 or more is set, and cancels the event (the later listeners that don't take cancelled ones don't get it); a negative one is ignored. */
    public void setBurnTime(int burnTime) {
        if (burnTime < 0) return;
        this.burnTime = burnTime;
        setCanceled(true);
    }

    public int getBurnTime() {
        return burnTime;
    }
}
