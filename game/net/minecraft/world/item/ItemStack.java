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

    public net.minecraft.core.Holder<Item> getItemHolder() {
        return getItem().builtInRegistryHolder();
    }

    /**
     * Observed in a server (`standins --horizon-src`): {count, id}; an empty stack, or a count outside 1 to 99, can't
     * be written. Data components aren't modelled: none is written.
     */
    public net.minecraft.nbt.Tag save(net.minecraft.core.HolderLookup.Provider registries) {
        if (isEmpty()) throw new IllegalStateException("Cannot encode empty ItemStack");
        int n = getCount();
        if (n < 1 || n > 99) throw new IllegalStateException("Value must be within range [1;99]: " + n);
        net.minecraft.nbt.CompoundTag tag = new net.minecraft.nbt.CompoundTag();
        tag.putString("id", String.valueOf(getItem()));
        tag.putInt("count", n);
        return tag;
    }

    /** Observed: an empty compound for an empty stack. */
    public net.minecraft.nbt.Tag saveOptional(net.minecraft.core.HolderLookup.Provider registries) {
        return isEmpty() ? new net.minecraft.nbt.CompoundTag() : save(registries);
    }

    /**
     * Observed: a compound with the id of an item that isn't air (an unknown or invalid id gives none), its count 1 when
     * missing, not a number or outside 1 to 99.
     */
    public static java.util.Optional<ItemStack> parse(net.minecraft.core.HolderLookup.Provider registries, net.minecraft.nbt.Tag tag) {
        if (!(tag instanceof net.minecraft.nbt.CompoundTag c) || !c.contains("id", 8)) return java.util.Optional.empty();
        net.minecraft.resources.ResourceLocation id = net.minecraft.resources.ResourceLocation.tryParse(c.getString("id"));
        if (id == null) return java.util.Optional.empty();
        Item item = net.neoforged.neoforge.registries.HorizonRegistries.knownItem(id.toString());
        if (item == null || item == Items.AIR) return java.util.Optional.empty();
        int count = c.contains("count", 99) ? c.getInt("count") : 1;
        if (count < 1 || count > 99) count = 1;
        return java.util.Optional.of(new ItemStack(item, count));
    }

    public static ItemStack parseOptional(net.minecraft.core.HolderLookup.Provider registries, net.minecraft.nbt.CompoundTag tag) {
        return tag.isEmpty() ? EMPTY : parse(registries, tag).orElse(EMPTY);
    }

    @Override
    public String toString() {
        return getCount() + " " + getItem();
    }
}
