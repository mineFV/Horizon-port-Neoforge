package net.minecraft.world.item;

import net.minecraft.core.component.DataComponentHolder;
import net.minecraft.world.level.ItemLike;
import net.neoforged.neoforge.common.MutableDataComponentHolder;
import net.neoforged.neoforge.common.extensions.IItemStackExtension;

/**
 * The stand-in for the game's ItemStack: an item, a count and its components (the item's, and what the stack changed of
 * them), observed in a server (`standins --horizon-src`).
 */
public final class ItemStack implements DataComponentHolder, MutableDataComponentHolder, IItemStackExtension {
    public static final ItemStack EMPTY = new ItemStack((Void) null);

    private final Item item;
    private int count;
    private final net.minecraft.core.component.PatchedDataComponentMap components;

    private ItemStack(Void nothing) {
        this.item = null;
        this.components = new net.minecraft.core.component.PatchedDataComponentMap(net.minecraft.core.component.DataComponentMap.EMPTY);
    }

    public ItemStack(ItemLike item) {
        this(item, 1);
    }

    public ItemStack(ItemLike item, int count) {
        this((Item) item, count, new net.minecraft.core.component.PatchedDataComponentMap(((Item) item).components()));
    }

    public ItemStack(net.minecraft.core.Holder<Item> item) {
        this(item.value(), 1);
    }

    public ItemStack(net.minecraft.core.Holder<Item> item, int count) {
        this(item.value(), count);
    }

    public ItemStack(net.minecraft.core.Holder<Item> item, int count, net.minecraft.core.component.DataComponentPatch patch) {
        this(item.value(), count);
        components.applyPatch(patch);
    }

    private ItemStack(Item item, int count, net.minecraft.core.component.PatchedDataComponentMap components) {
        this.item = item;
        this.count = count;
        this.components = components;
    }

    @Override
    public net.minecraft.core.component.DataComponentMap getComponents() {
        return isEmpty() ? net.minecraft.core.component.DataComponentMap.EMPTY : components;
    }

    public net.minecraft.core.component.DataComponentMap getPrototype() {
        return isEmpty() ? net.minecraft.core.component.DataComponentMap.EMPTY : getItem().components();
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

    public void applyComponents(net.minecraft.core.component.DataComponentPatch patch) {
        components.applyPatch(patch);
    }

    public boolean is(Item item) {
        return getItem() == item;
    }

    /** Observed (`standins --horizon-src`): whether the holder's value is its item (its own holder, a DeferredHolder of it). */
    public boolean is(net.minecraft.core.Holder<Item> holder) {
        return holder.value() == getItem();
    }

    /**
     * Its item's name for it (observed: no custom name nor item name among the modelled components). Where its
     * components aren't all modelled, a name among them can't be ruled out: listing them says so.
     */
    public net.minecraft.network.chat.Component getHoverName() {
        getComponents().keySet();
        return getItem().getName(this);
    }

    public boolean is(net.minecraft.tags.TagKey<Item> tag) {
        return getItemHolder().is(tag);
    }

    public java.util.stream.Stream<net.minecraft.tags.TagKey<Item>> getTags() {
        return getItemHolder().tags();
    }

    public int getMaxStackSize() {
        return getItem().getMaxStackSize(this);
    }

    public boolean isStackable() {
        return getMaxStackSize() > 1 && (!isDamageableItem() || !isDamaged());
    }

    public boolean isDamageableItem() {
        return has(net.minecraft.core.component.DataComponents.MAX_DAMAGE) && has(net.minecraft.core.component.DataComponents.DAMAGE);
    }

    public boolean isDamaged() {
        return isDamageableItem() && getItem().isDamaged(this);
    }

    public int getDamageValue() {
        return getItem().getDamage(this);
    }

    public void setDamageValue(int damage) {
        getItem().setDamage(this, damage);
    }

    public int getMaxDamage() {
        return getItem().getMaxDamage(this);
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
        return isEmpty() ? EMPTY : new ItemStack(item, count, components.copy());
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
        if (a.getItem() != b.getItem()) return false;
        return a.isEmpty() && b.isEmpty() || a.getComponentsPatch().equals(b.getComponentsPatch());
    }

    /** Only whether two are equal was observed (an item's hash is its identity's). */
    public static int hashItemAndComponents(ItemStack stack) {
        return stack == null ? 0 : 31 * (31 + stack.getItem().hashCode()) + stack.getComponentsPatch().hashCode();
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
        net.minecraft.core.component.DataComponentPatch patch = getComponentsPatch();
        if (!patch.isEmpty()) tag.put("components", net.minecraft.core.component.HorizonComponentCodecs.encode(patch));
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
        ItemStack stack = new ItemStack(item, count);
        if (c.contains("components")) {
            stack.applyComponents(net.minecraft.core.component.HorizonComponentCodecs.decode(c.get("components")));
        }
        return java.util.Optional.of(stack);
    }

    public static ItemStack parseOptional(net.minecraft.core.HolderLookup.Provider registries, net.minecraft.nbt.CompoundTag tag) {
        return tag.isEmpty() ? EMPTY : parse(registries, tag).orElse(EMPTY);
    }

    @Override
    public String toString() {
        return getCount() + " " + getItem();
    }
}
