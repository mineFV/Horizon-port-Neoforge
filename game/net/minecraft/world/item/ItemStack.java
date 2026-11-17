package net.minecraft.world.item;

import net.minecraft.core.component.DataComponentHolder;
import net.minecraft.world.level.ItemLike;
import net.neoforged.neoforge.common.MutableDataComponentHolder;
import net.neoforged.neoforge.common.extensions.IItemStackExtension;

/**
 * The stand-in for the game's ItemStack: an item and a count, observed in a server (`standins --horizon-src`). Data
 * components aren't modelled: a stack's are its item's defaults.
 */
public final class ItemStack implements DataComponentHolder, MutableDataComponentHolder, IItemStackExtension {
    public static final ItemStack EMPTY = new ItemStack((Void) null);

    private final Item item;
    private int count;

    private ItemStack(Void nothing) {
        this.item = null;
    }

    public ItemStack(ItemLike item) {
        this(item, 1);
    }

    public ItemStack(ItemLike item, int count) {
        this.item = (Item) item;
        this.count = count;
    }

    public boolean isEmpty() {
        return this == EMPTY || item == Items.AIR || count <= 0;
    }

    public Item getItem() {
        return isEmpty() ? Items.AIR : item;
    }

    public int getCount() {
        return isEmpty() ? 0 : count;
    }

    public void setCount(int count) {
        this.count = count;
    }

    public void grow(int n) {
        setCount(getCount() + n);
    }

    public void shrink(int n) {
        grow(-n);
    }

    public ItemStack split(int n) {
        int taken = Math.min(n, getCount());
        ItemStack part = copyWithCount(taken);
        shrink(taken);
        return part;
    }

    public ItemStack copy() {
        return isEmpty() ? EMPTY : new ItemStack(item, count);
    }

    public ItemStack copyWithCount(int n) {
        if (isEmpty()) return EMPTY;
        ItemStack c = copy();
        c.setCount(n);
        return c;
    }

    public static boolean matches(ItemStack a, ItemStack b) {
        return a == b || a.getCount() == b.getCount() && isSameItemSameComponents(a, b);
    }

    public static boolean isSameItem(ItemStack a, ItemStack b) {
        return a.getItem() == b.getItem();
    }

    public static boolean isSameItemSameComponents(ItemStack a, ItemStack b) {
        return a.getItem() == b.getItem();
    }

    public static int hashItemAndComponents(ItemStack stack) {
        return stack == null ? 0 : 31 * (31 + stack.getItem().hashCode());
    }

    @Override
    public String toString() {
        return getCount() + " " + getItem();
    }
}
