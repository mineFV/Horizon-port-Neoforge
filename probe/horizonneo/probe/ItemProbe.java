package horizonneo.probe;

import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.material.Fluids;
import net.neoforged.neoforge.fluids.FluidStack;

/**
 * Items and fluids, which need the game started: asked in a server only (InServer, GameProbe's "server" argument).
 * Hashes start from objects' identities, which change from run to run: only whether two are equal is asked.
 */
final class ItemProbe {
    private ItemProbe() {}

    /**
     * The stand-ins are given the game's ids as the bridge gives them from `dump`'s facts (HorizonRegistries.known,
     * which isn't the game's: absent in a server, where the registries have them): here, those the questions name
     * that the game has ("minecraft:nope" isn't one).
     */
    /** The game's components of the items the questions name, as horizon-ids.txt has them (InServer.ids). */
    static final String COMPONENTS = "minecraft:air=max_stack_size:64,lore:empty,enchantments:empty,repair_cost:0,attribute_modifiers:empty,rarity:common,name:block.minecraft.air;minecraft:stone=max_stack_size:64,lore:empty,enchantments:empty,repair_cost:0,attribute_modifiers:empty,rarity:common,name:block.minecraft.stone;minecraft:diamond=max_stack_size:64,lore:empty,enchantments:empty,repair_cost:0,attribute_modifiers:empty,rarity:common;minecraft:diamond_sword=rarity:common,damage:0,repair_cost:0,enchantments:empty,lore:empty,max_damage:1561,max_stack_size:1,other";

    private static void given() {
        try {
            java.lang.reflect.Method known = Class.forName("net.neoforged.neoforge.registries.HorizonRegistries").getMethod("known", String.class, String.class);
            known.invoke(null, "item", "minecraft:air,minecraft:stone,minecraft:diamond,minecraft:diamond_sword");
            known.invoke(null, "block", "minecraft:air,minecraft:stone");
            known.invoke(null, "fluid", "minecraft:empty,minecraft:water,minecraft:lava");
            known.invoke(null, "component", "minecraft:max_stack_size,minecraft:max_damage,minecraft:damage,minecraft:repair_cost,minecraft:lore,minecraft:enchantments,minecraft:attribute_modifiers,minecraft:rarity");
            known.invoke(null, "components", COMPONENTS);
        } catch (ClassNotFoundException e) {
            // The game's own classes: nothing to give.
        } catch (ReflectiveOperationException e) {
            throw new IllegalStateException(e);
        }
    }

    static void run() {
        given();
        p("items", () -> Items.AIR + " " + Items.DIAMOND + " " + Items.STONE + " " + Items.DIAMOND_SWORD + " " + (Items.DIAMOND == Items.DIAMOND));
        p("item empty", () -> ItemStack.EMPTY + " " + ItemStack.EMPTY.isEmpty() + " " + ItemStack.EMPTY.getItem() + " " + ItemStack.EMPTY.getCount() + " " + (ItemStack.EMPTY.copy() == ItemStack.EMPTY));
        int[] counts = {-2, 0, 1, 5, 64, 65, 1000};
        for (int n : counts) {
            p("item stack " + n, () -> {
                ItemStack s = new ItemStack(Items.DIAMOND, n);
                return s + " " + s.isEmpty() + " " + s.getItem() + " " + s.getCount() + " " + new ItemStack(Items.AIR, n).isEmpty() + " " + new ItemStack(Items.AIR, n).getCount();
            });
            p("item change " + n, () -> {
                ItemStack s = new ItemStack(Items.STONE, 10);
                ItemStack part = s.split(n);
                StringBuilder b = new StringBuilder().append(part).append(' ').append(s).append(' ').append(part.isEmpty());
                s.grow(n);
                b.append(' ').append(s);
                s.shrink(n * 2);
                b.append(' ').append(s).append(' ').append(s.isEmpty()).append(' ').append(s.getItem());
                s.setCount(n);
                b.append(' ').append(s).append(' ').append(s.isEmpty());
                ItemStack c = s.copy();
                b.append(' ').append(c == s).append(' ').append(c).append(' ').append(s.copyWithCount(n + 1)).append(' ').append(ItemStack.EMPTY.copyWithCount(3));
                return b;
            });
        }
        ItemStack[] stacks = {ItemStack.EMPTY, new ItemStack(Items.DIAMOND), new ItemStack(Items.DIAMOND, 2), new ItemStack(Items.STONE), new ItemStack(Items.AIR, 4), new ItemStack(Items.DIAMOND, 0)};
        for (int i = 0; i < stacks.length; i++) {
            for (int j = 0; j < stacks.length; j++) {
                ItemStack a = stacks[i], b = stacks[j];
                p("item compare " + i + " " + j, () -> ItemStack.matches(a, b) + " " + ItemStack.isSameItem(a, b) + " " + ItemStack.isSameItemSameComponents(a, b)
                        + " " + (ItemStack.hashItemAndComponents(a) == ItemStack.hashItemAndComponents(b)));
            }
        }
        net.minecraft.core.HolderLookup.Provider r = GameProbe.REGISTRIES;
        p("item holder", () -> {
            net.minecraft.core.Holder<net.minecraft.world.item.Item> h = new ItemStack(Items.DIAMOND, 2).getItemHolder();
            return h + " " + (h.value() == Items.DIAMOND) + " " + (h == Items.DIAMOND.builtInRegistryHolder()) + " " + ItemStack.EMPTY.getItemHolder()
                    + " " + Items.DIAMOND.builtInRegistryHolder().key() + " " + Items.DIAMOND.builtInRegistryHolder().is(net.minecraft.resources.ResourceLocation.parse("minecraft:diamond"))
                    + " " + (Items.DIAMOND.builtInRegistryHolder().hashCode() == Items.DIAMOND.builtInRegistryHolder().key().hashCode())
                    + " " + (h.hashCode() == System.identityHashCode(h)) + " " + h.equals(h) + " " + h.equals(Items.STONE.builtInRegistryHolder());
        });
        for (int n : counts) {
            p("item save " + n, () -> new ItemStack(Items.DIAMOND, n).saveOptional(r) + " " + new ItemStack(Items.STONE, n).save(r));
        }
        p("item save empty", () -> ItemStack.EMPTY.saveOptional(r) + " " + ItemStack.EMPTY.save(r));
        Object[][] tags = {{"id", "minecraft:diamond", "count", 3}, {"id", "minecraft:diamond"}, {"id", "minecraft:nope", "count", 2}, {}, {"count", 5},
            {"id", "minecraft:air", "count", 4}, {"id", "minecraft:diamond", "count", 200}, {"id", "minecraft:diamond", "count", 0}, {"id", "minecraft:diamond", "count", -3},
            {"id", "minecraft:diamond", "count", (byte) 3}, {"id", "minecraft:diamond", "count", "3"}, {"id", "Diamond", "count", 1}};
        for (Object[] t : tags) {
            p("item parse " + java.util.Arrays.toString(t), () -> {
                net.minecraft.nbt.CompoundTag c = tag(t);
                return ItemStack.parseOptional(r, c) + " " + ItemStack.parse(r, c).map(Object::toString).orElse("none");
            });
        }
        p("fluids", () -> Fluids.EMPTY + " " + Fluids.WATER + " " + Fluids.LAVA + " " + (Fluids.WATER == Fluids.WATER));
        p("fluid empty", () -> FluidStack.EMPTY + " " + FluidStack.EMPTY.isEmpty() + " " + FluidStack.EMPTY.getFluid() + " " + FluidStack.EMPTY.getAmount() + " " + (FluidStack.EMPTY.copy() == FluidStack.EMPTY));
        for (int n : counts) {
            p("fluid stack " + n, () -> {
                FluidStack s = new FluidStack(Fluids.WATER, n);
                StringBuilder b = new StringBuilder().append(s).append(' ').append(s.isEmpty()).append(' ').append(s.getFluid()).append(' ').append(s.getAmount());
                b.append(' ').append(new FluidStack(Fluids.EMPTY, n).isEmpty()).append(' ').append(new FluidStack(Fluids.EMPTY, n).getAmount());
                FluidStack c = s.copy();
                c.grow(n);
                b.append(' ').append(c).append(' ').append(s.copyWithAmount(n + 1)).append(' ').append(c == s);
                c.shrink(n * 3);
                b.append(' ').append(c).append(' ').append(c.isEmpty()).append(' ').append(c.getFluid());
                c.setAmount(n);
                return b.append(' ').append(c).append(' ').append(c.isEmpty());
            });
        }
        p("fluid holder", () -> new FluidStack(Fluids.WATER, 5).getFluidHolder() + " " + (new FluidStack(Fluids.WATER, 5).getFluidHolder().value() == Fluids.WATER)
                + " " + FluidStack.EMPTY.getFluidHolder());
        for (int n : counts) {
            p("fluid save " + n, () -> new FluidStack(Fluids.WATER, n).saveOptional(r) + " " + new FluidStack(Fluids.LAVA, n).save(r));
        }
        p("fluid save empty", () -> FluidStack.EMPTY.saveOptional(r) + " " + FluidStack.EMPTY.save(r));
        Object[][] fluidTags = {{"id", "minecraft:water", "amount", 1000}, {"id", "minecraft:water"}, {"id", "minecraft:nope", "amount", 2}, {}, {"amount", 5},
            {"id", "minecraft:empty", "amount", 4}, {"id", "minecraft:lava", "amount", 0}, {"id", "minecraft:lava", "amount", -3}, {"id", "minecraft:water", "amount", (byte) 3}};
        for (Object[] t : fluidTags) {
            p("fluid parse " + java.util.Arrays.toString(t), () -> {
                net.minecraft.nbt.CompoundTag c = tag(t);
                return FluidStack.parseOptional(r, c) + " " + FluidStack.parse(r, c).map(Object::toString).orElse("none");
            });
        }
        FluidStack[] fluids = {FluidStack.EMPTY, new FluidStack(Fluids.WATER, 1000), new FluidStack(Fluids.WATER, 500), new FluidStack(Fluids.LAVA, 1000), new FluidStack(Fluids.EMPTY, 10)};
        for (int i = 0; i < fluids.length; i++) {
            for (int j = 0; j < fluids.length; j++) {
                FluidStack a = fluids[i], b = fluids[j];
                p("fluid compare " + i + " " + j, () -> FluidStack.matches(a, b) + " " + FluidStack.isSameFluid(a, b) + " " + FluidStack.isSameFluidSameComponents(a, b)
                        + " " + (FluidStack.hashFluidAndComponents(a) == FluidStack.hashFluidAndComponents(b)));
            }
        }
    }

    /** A compound of key, value pairs: texts as strings, bytes as bytes, others as ints. */
    static net.minecraft.nbt.CompoundTag tag(Object[] pairs) {
        net.minecraft.nbt.CompoundTag c = new net.minecraft.nbt.CompoundTag();
        for (int i = 0; i + 1 < pairs.length; i += 2) {
            String k = (String) pairs[i];
            Object v = pairs[i + 1];
            if (v instanceof String text) c.putString(k, text);
            else if (v instanceof Byte b) c.putByte(k, b);
            else c.putInt(k, (Integer) v);
        }
        return c;
    }

    static void p(String key, java.util.function.Supplier<Object> call) {
        GameProbe.p(key, call);
    }
}
