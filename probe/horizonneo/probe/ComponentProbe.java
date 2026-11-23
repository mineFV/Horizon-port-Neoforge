package horizonneo.probe;

import java.util.function.Supplier;
import net.minecraft.core.component.DataComponentMap;
import net.minecraft.core.component.DataComponentType;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.Rarity;
import net.minecraft.world.item.component.ItemAttributeModifiers;
import net.minecraft.world.item.component.ItemLore;
import net.minecraft.world.item.enchantment.ItemEnchantments;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

/**
 * Data components: their types, an item's (from its properties: items of the probe's own, registered as a mod's), and a
 * stack's, changed, compared, written and read. In a server only (GameProbe's "server" argument), after ItemProbe,
 * which gives the stand-ins the game's ids.
 */
final class ComponentProbe {
    private ComponentProbe() {}

    static final DeferredRegister<Item> ITEMS = DeferredRegister.create(Registries.ITEM, "horizonprobe");

    static final DeferredHolder<Item, Item> PLAIN = ITEMS.register("plain", () -> new Item(new Item.Properties()));
    static final DeferredHolder<Item, Item> STACK16 = ITEMS.register("stack16", () -> new Item(new Item.Properties().stacksTo(16)));
    static final DeferredHolder<Item, Item> DURABLE = ITEMS.register("durable", () -> new Item(new Item.Properties().durability(100)));
    static final DeferredHolder<Item, Item> EPIC = ITEMS.register("epic", () -> new Item(new Item.Properties().component(DataComponents.RARITY, Rarity.EPIC).stacksTo(1)));
    static final DeferredHolder<Item, Item> TOUGH = ITEMS.register("tough", () -> new Item(new Item.Properties().fireResistant().durability(30).setNoRepair().craftRemainder(Items.DIAMOND)));
    static final DeferredHolder<Item, Item> RARE = ITEMS.register("rare", () -> new Item(new Item.Properties().rarity(Rarity.RARE).stacksTo(5).rarity(Rarity.UNCOMMON)));
    /** A bow, and a plain item made with the same properties: what a bow's constructor adds to them. */
    static final DeferredHolder<Item, net.minecraft.world.item.BowItem> BOW = ITEMS.register("probe_bow",
            () -> new net.minecraft.world.item.BowItem(new Item.Properties().durability(384)));
    static final DeferredHolder<Item, Item> NOT_BOW = ITEMS.register("probe_not_bow", () -> new Item(new Item.Properties().durability(384)));
    static final DeferredHolder<Item, Item> CUSTOM = ITEMS.register("custom", () -> new Item(new Item.Properties().durability(60)) {
        @Override
        public int getMaxStackSize(ItemStack stack) {
            return 3;
        }

        @Override
        public int getMaxDamage(ItemStack stack) {
            return 50;
        }

        @Override
        public int getDamage(ItemStack stack) {
            return 7;
        }
    });

    static void run() {
        net.minecraft.core.HolderLookup.Provider r = GameProbe.REGISTRIES;
        DataComponentType<?>[] types = {DataComponents.MAX_STACK_SIZE, DataComponents.MAX_DAMAGE, DataComponents.DAMAGE, DataComponents.LORE, DataComponents.ENCHANTMENTS,
            DataComponents.REPAIR_COST, DataComponents.ATTRIBUTE_MODIFIERS, DataComponents.RARITY};
        for (DataComponentType<?> t : types) p("component type " + t, () -> t + " " + t.isTransient() + " " + (t.codec() != null) + " " + (t.codecOrThrow() != null));
        p("component no codec", () -> DataComponentType.<Integer>builder().cacheEncoding().build());
        p("component values", () -> ItemLore.EMPTY + " " + ItemEnchantments.EMPTY + " " + ItemAttributeModifiers.EMPTY + " " + java.util.Arrays.toString(Rarity.values())
                + " " + Rarity.EPIC.getSerializedName() + " " + ItemEnchantments.EMPTY.size() + " " + ItemEnchantments.EMPTY.isEmpty() + " " + ItemLore.EMPTY.equals(ItemLore.EMPTY)
                + " " + ItemEnchantments.EMPTY.equals(ItemEnchantments.EMPTY) + " " + ItemLore.EMPTY.hashCode() + " " + ItemAttributeModifiers.EMPTY.hashCode() + " " + ItemEnchantments.EMPTY.hashCode());
        p("component common", () -> DataComponents.COMMON_ITEM_COMPONENTS + " " + DataComponents.COMMON_ITEM_COMPONENTS.size() + " " + DataComponents.COMMON_ITEM_COMPONENTS.keySet()
                + " " + DataComponents.COMMON_ITEM_COMPONENTS.has(DataComponents.DAMAGE) + " " + DataComponents.COMMON_ITEM_COMPONENTS.get(DataComponents.MAX_STACK_SIZE));
        p("component map empty", () -> DataComponentMap.EMPTY.getClass().getName() + " " + DataComponentMap.EMPTY.size() + " " + DataComponentMap.EMPTY.isEmpty() + " " + (DataComponentMap.builder().build() == DataComponentMap.EMPTY));
        p("component map built", () -> {
            DataComponentMap m = DataComponentMap.builder().set(DataComponents.DAMAGE, 1).set(DataComponents.MAX_DAMAGE, 4).set(DataComponents.DAMAGE, 2).build();
            StringBuilder b = new StringBuilder().append(m).append(' ').append(m.get(DataComponents.DAMAGE)).append(' ').append(m.keySet()).append(' ').append(m.size());
            for (Object typed : m) b.append(' ').append(typed);
            return b;
        });
        p("component map copied", () -> DataComponentMap.builder().addAll(DataComponents.COMMON_ITEM_COMPONENTS).set(DataComponents.REPAIR_COST, 4).build());

        p("component levels", () -> ItemEnchantments.EMPTY.getLevel(null) + " " + (Items.DIAMOND.asItem() == Items.DIAMOND) + " " + (PLAIN.get().asItem() == PLAIN.get()));
        p("component repair", () -> TOUGH.get().isRepairable(new ItemStack(TOUGH.get())) + " " + DURABLE.get().isRepairable(new ItemStack(DURABLE.get())) + " "
                + PLAIN.get().isRepairable(new ItemStack(PLAIN.get())) + " " + new ItemStack(TOUGH.get()).has(DataComponents.FIRE_RESISTANT) + " " + new ItemStack(TOUGH.get()).get(DataComponents.FIRE_RESISTANT)
                + " " + DataComponents.FIRE_RESISTANT);
        Item[] items = {Items.AIR, Items.DIAMOND, Items.STONE, PLAIN.get(), STACK16.get(), DURABLE.get(), EPIC.get(), CUSTOM.get(), RARE.get(), TOUGH.get(), BOW.get(), NOT_BOW.get()};
        p("component bow", () -> (BOW.get() instanceof net.minecraft.world.item.ProjectileWeaponItem) + " " + BOW.get().getClass().getName() + " " + BOW.get().getDefaultMaxStackSize()
                + " " + BOW.get().isRepairable(new ItemStack(BOW.get())) + " " + BOW.get().getDescriptionId() + " " + new ItemStack(BOW.get()).getMaxDamage()
                + " " + new ItemStack(BOW.get()).getMaxStackSize());
        // A map of eight types or more iterates in its types' identity hashes' order, which changes from run to run: the
        // types are sorted.
        for (Item item : items) {
            p("component item " + item, () -> (item.components().size() < 8 ? item.components().toString() : sorted(item.components())) + " " + item.getDefaultMaxStackSize());
        }
        Item[] all = {Items.AIR, Items.DIAMOND, Items.STONE, Items.DIAMOND_SWORD, PLAIN.get(), STACK16.get(), DURABLE.get(), EPIC.get(), CUSTOM.get()};
        for (Item item : all) {
            p("component stack " + item, () -> {
                ItemStack s = new ItemStack(item, 2);
                return s.getMaxStackSize() + " " + s.isStackable() + " " + s.isDamageableItem() + " " + s.isDamaged() + " " + s.getDamageValue() + " " + s.getMaxDamage()
                        + " " + s.get(DataComponents.DAMAGE) + " " + s.has(DataComponents.MAX_DAMAGE) + " " + s.getOrDefault(DataComponents.DAMAGE, 7) + " " + s.getComponentsPatch()
                        + " " + s.getComponentsPatch().isEmpty() + " " + s.get(DataComponents.MAX_STACK_SIZE) + " " + s.get(DataComponents.REPAIR_COST);
            });
        }
        p("component empty stack", () -> (ItemStack.EMPTY.getComponents() == DataComponentMap.EMPTY) + " " + ItemStack.EMPTY.getMaxStackSize() + " " + ItemStack.EMPTY.get(DataComponents.MAX_STACK_SIZE)
                + " " + (ItemStack.EMPTY.getPrototype() == DataComponentMap.EMPTY) + " " + ItemStack.EMPTY.getComponentsPatch() + " " + (new ItemStack(Items.DIAMOND, 0).getComponents() == DataComponentMap.EMPTY)
                + " " + new ItemStack(Items.DIAMOND, 0).getMaxStackSize() + " " + new ItemStack(Items.DIAMOND, 0).getComponentsPatch());
        p("component change", () -> {
            ItemStack s = new ItemStack(Items.DIAMOND);
            StringBuilder b = new StringBuilder();
            b.append(s.set(DataComponents.MAX_STACK_SIZE, 16)).append(' ').append(s.getMaxStackSize()).append(' ').append(s.getComponentsPatch()).append(' ').append(s.getComponents());
            b.append(' ').append(s.set(DataComponents.MAX_STACK_SIZE, 64)).append(' ').append(s.getComponentsPatch().isEmpty());
            b.append(' ').append((Object) s.remove(DataComponents.MAX_STACK_SIZE)).append(' ').append(s.get(DataComponents.MAX_STACK_SIZE)).append(' ').append(s.getMaxStackSize()).append(' ').append(s.getComponentsPatch());
            b.append(' ').append(s.getComponents()).append(' ').append(s.getComponents().size()).append(' ').append(s.getComponents().keySet());
            b.append(' ').append((Object) s.remove(DataComponents.MAX_DAMAGE)).append(' ').append(s.getComponentsPatch());
            b.append(' ').append(s.set(DataComponents.DAMAGE, 3)).append(' ').append(s.isDamageableItem()).append(' ').append(s.getDamageValue()).append(' ').append(s.getComponentsPatch());
            b.append(' ').append(s.set(DataComponents.MAX_STACK_SIZE, 64)).append(' ').append(s.getComponentsPatch());
            b.append(' ').append((Object) s.remove(DataComponents.MAX_STACK_SIZE)).append(' ').append((Object) s.remove(DataComponents.MAX_STACK_SIZE)).append(' ').append(s.getComponentsPatch());
            b.append(' ').append(s.set(DataComponents.MAX_STACK_SIZE, 20)).append(' ').append((Object) s.remove(DataComponents.MAX_STACK_SIZE)).append(' ').append(s.getComponentsPatch());
            b.append(' ').append(s.set(DataComponents.MAX_DAMAGE, 20)).append(' ').append((Object) s.remove(DataComponents.MAX_DAMAGE)).append(' ').append((Object) s.remove(DataComponents.MAX_DAMAGE)).append(' ').append(s.getComponentsPatch());
            b.append(' ').append(s.set(DataComponents.REPAIR_COST, null)).append(' ').append(s.getComponentsPatch()).append(' ').append(s.get(DataComponents.REPAIR_COST));
            return b;
        });
        p("component suppliers", () -> {
            ItemStack s = new ItemStack(Items.DIAMOND);
            Supplier<DataComponentType<Integer>> cost = () -> DataComponents.REPAIR_COST;
            StringBuilder b = new StringBuilder();
            b.append(s.set(cost, 5)).append(' ').append(s.get(cost)).append(' ').append(s.has(cost)).append(' ').append(s.getOrDefault(cost, 9));
            b.append(' ').append(s.update(DataComponents.REPAIR_COST, 0, x -> x + 1)).append(' ').append(s.get(DataComponents.REPAIR_COST));
            b.append(' ').append(s.update(DataComponents.REPAIR_COST, 0, 10, Integer::sum)).append(' ').append(s.get(DataComponents.REPAIR_COST));
            b.append(' ').append((Object) s.remove(cost)).append(' ').append(s.get(cost)).append(' ').append(s.getComponentsPatch());
            return b;
        });
        int[] damages = {-3, 0, 5, 100, 500};
        for (int d : damages) {
            p("component damage " + d, () -> {
                ItemStack s = new ItemStack(DURABLE.get());
                s.setDamageValue(d);
                StringBuilder b = new StringBuilder().append(s.getDamageValue()).append(' ').append(s.get(DataComponents.DAMAGE)).append(' ').append(s.isDamaged()).append(' ').append(s.isStackable());
                s.set(DataComponents.DAMAGE, d * 3);
                b.append(' ').append(s.getDamageValue()).append(' ').append(s.get(DataComponents.DAMAGE)).append(' ').append(s.getComponentsPatch());
                ItemStack c = new ItemStack(CUSTOM.get());
                c.setDamageValue(d);
                return b.append(' ').append(c.get(DataComponents.DAMAGE)).append(' ').append(c.getDamageValue()).append(' ').append(c.isDamaged()).append(' ').append(c.getMaxDamage());
            });
        }
        p("component compare", () -> {
            ItemStack a = new ItemStack(Items.DIAMOND), b = a.copy(), c = new ItemStack(Items.DIAMOND);
            b.set(DataComponents.REPAIR_COST, 3);
            StringBuilder o = new StringBuilder().append(ItemStack.isSameItemSameComponents(a, b)).append(' ').append(ItemStack.matches(a, b)).append(' ').append(ItemStack.hashItemAndComponents(a) == ItemStack.hashItemAndComponents(b));
            c.set(DataComponents.REPAIR_COST, 3);
            o.append(' ').append(ItemStack.isSameItemSameComponents(b, c)).append(' ').append(ItemStack.matches(b, c)).append(' ').append(ItemStack.hashItemAndComponents(b) == ItemStack.hashItemAndComponents(c));
            c.set(DataComponents.REPAIR_COST, 0);
            o.append(' ').append(ItemStack.isSameItemSameComponents(a, c)).append(' ').append(b.copy().getComponentsPatch()).append(' ').append(b.copyWithCount(5).getComponentsPatch()).append(' ').append(a.getComponentsPatch());
            ItemStack d = new ItemStack(Items.DIAMOND);
            d.set(DataComponents.REPAIR_COST, 3);
            d.set(DataComponents.MAX_DAMAGE, 9);
            ItemStack e = new ItemStack(Items.DIAMOND);
            e.set(DataComponents.MAX_DAMAGE, 9);
            e.set(DataComponents.REPAIR_COST, 3);
            return o.append(' ').append(ItemStack.isSameItemSameComponents(d, e)).append(' ').append(d.getComponentsPatch()).append(' ').append(e.getComponentsPatch());
        });
        p("component save", () -> {
            ItemStack a = new ItemStack(Items.DIAMOND, 3);
            a.set(DataComponents.REPAIR_COST, 3);
            ItemStack b = new ItemStack(Items.DIAMOND);
            b.remove(DataComponents.MAX_STACK_SIZE);
            ItemStack c = new ItemStack(DURABLE.get());
            c.setDamageValue(5);
            ItemStack d = new ItemStack(Items.DIAMOND);
            d.set(DataComponents.MAX_STACK_SIZE, 16);
            d.set(DataComponents.MAX_DAMAGE, 7);
            return a.save(r) + " " + b.save(r) + " " + c.save(r) + " " + d.save(r);
        });
        int[] sizes = {0, 99, 100};
        for (int n : sizes) {
            p("component save size " + n, () -> {
                ItemStack s = new ItemStack(Items.DIAMOND);
                s.set(DataComponents.MAX_STACK_SIZE, n);
                return s.save(r);
            });
        }
        p("component save damage", () -> {
            ItemStack s = new ItemStack(Items.DIAMOND);
            s.set(DataComponents.MAX_DAMAGE, 0);
            return s.save(r);
        });
        Object[][] patches = {{"minecraft:repair_cost", 3}, {"!minecraft:max_stack_size", "{}"}, {"minecraft:max_stack_size", 16}, {"minecraft:max_stack_size", 100},
            {"minecraft:damage", 5}, {"minecraft:nope", 1}, {"minecraft:repair_cost", "x"}, {"minecraft:repair_cost", (byte) 2}, {"minecraft:repair_cost", -1},
            {"minecraft:max_damage", 0}, {"minecraft:max_damage", 10, "minecraft:max_stack_size", 1}, {"minecraft:max_damage", 10, "minecraft:damage", 2},
            {"!minecraft:max_damage", "{}"}, {"!minecraft:nope", "{}"}, {"Nope", 1}, {}};
        String[] ids = {"minecraft:diamond", "horizonprobe:durable"};
        for (String id : ids) {
            for (Object[] patch : patches) {
                p("component parse " + id + " " + java.util.Arrays.toString(patch), () -> {
                    CompoundTag t = new CompoundTag();
                    t.putString("id", id);
                    t.putInt("count", 1);
                    CompoundTag c = new CompoundTag();
                    for (int i = 0; i + 1 < patch.length; i += 2) {
                        String k = (String) patch[i];
                        Object v = patch[i + 1];
                        if ("{}".equals(v)) c.put(k, new CompoundTag());
                        else if (v instanceof String text) c.putString(k, text);
                        else if (v instanceof Byte bt) c.putByte(k, bt);
                        else c.putInt(k, (Integer) v);
                    }
                    t.put("components", c);
                    return ItemStack.parse(r, t).map(s -> s + " " + s.getComponentsPatch()).orElse("none");
                });
            }
        }
        p("component parse not a compound", () -> {
            CompoundTag t = new CompoundTag();
            t.putString("id", "minecraft:diamond");
            t.putInt("components", 3);
            return ItemStack.parse(r, t).map(Object::toString).orElse("none");
        });
        p("component holder of stack", () -> new ItemStack(Items.DIAMOND_SWORD).getMaxDamage() + " " + new ItemStack(Items.DIAMOND_SWORD).getMaxStackSize());
    }

    static String sorted(DataComponentMap m) {
        java.util.List<String> all = new java.util.ArrayList<>();
        for (DataComponentType<?> t : m.keySet()) all.add(t + "=>" + m.get(t));
        java.util.Collections.sort(all);
        return all.toString();
    }

    static void p(String key, Supplier<Object> call) {
        GameProbe.p(key, call);
    }
}
