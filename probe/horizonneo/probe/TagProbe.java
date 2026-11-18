package horizonneo.probe;

import java.util.function.Supplier;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.BlockTags;
import net.minecraft.tags.FluidTags;
import net.minecraft.tags.ItemTags;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.material.Fluids;
import net.neoforged.neoforge.fluids.FluidStack;

/**
 * Tags: their keys, and what is in them: the probe's own (its data), a "c" one and vanilla's beacon payments for items,
 * water and lava for fluids. The stand-ins are given what the server holds of these (given()), as the bridge gives
 * `dump`'s facts.
 */
final class TagProbe {
    private TagProbe() {}

    /** As the facts would list them: the tags holding the probe's values, a "c" tag whole, and the vanilla one asked. */
    static final String ITEM_TAGS = "horizonprobe:probed=horizonprobe:plain,minecraft:diamond;c:gems/diamond=minecraft:diamond;"
            + "minecraft:beacon_payment_items=minecraft:diamond,minecraft:emerald,minecraft:gold_ingot,minecraft:iron_ingot,minecraft:netherite_ingot";
    static final String BLOCK_TAGS = "horizonprobe:blocks=horizonprobe:block";
    static final String FLUID_TAGS = "c:lava=minecraft:flowing_lava,minecraft:lava;c:water=minecraft:flowing_water,minecraft:water;minecraft:lava=minecraft:flowing_lava,minecraft:lava;minecraft:water=minecraft:flowing_water,minecraft:water";

    private static void given() {
        try {
            java.lang.reflect.Method known = Class.forName("net.neoforged.neoforge.registries.HorizonRegistries").getMethod("known", String.class, String.class);
            known.invoke(null, "tags_item", ITEM_TAGS);
            known.invoke(null, "tags_block", BLOCK_TAGS);
            known.invoke(null, "tags_fluid", FLUID_TAGS);
            known.invoke(null, "namespaces", "horizonprobe");
        } catch (ClassNotFoundException e) {
            // The game's own tags.
        } catch (ReflectiveOperationException e) {
            throw new IllegalStateException(e);
        }
    }

    static void run() {
        given();
        holders();
        TagKey<Item> probed = ItemTags.create(ResourceLocation.parse("horizonprobe:probed"));
        TagKey<Item> diamonds = ItemTags.create(ResourceLocation.parse("c:gems/diamond"));
        TagKey<Item> payment = ItemTags.create(ResourceLocation.withDefaultNamespace("beacon_payment_items"));
        TagKey<Item> none = ItemTags.create(ResourceLocation.parse("horizonprobe:none"));
        p("tag key holders", () -> net.neoforged.neoforge.registries.DeferredHolder.create(net.minecraft.resources.ResourceKey.create(Registries.ITEM, ResourceLocation.withDefaultNamespace("diamond")))
                + " " + net.neoforged.neoforge.registries.DeferredHolder.create(net.minecraft.resources.ResourceKey.create(Registries.ITEM, ResourceLocation.withDefaultNamespace("diamond"))).getId());
        p("tag key", () -> probed + " | " + (ItemTags.create(ResourceLocation.parse("horizonprobe:probed")) == probed) + " | " + probed.equals(TagKey.create(Registries.ITEM, ResourceLocation.parse("horizonprobe:probed")))
                + " | " + (probed.hashCode() == ItemTags.create(ResourceLocation.parse("horizonprobe:probed")).hashCode()) + " | " + probed.isFor(Registries.ITEM) + " | " + probed.isFor(Registries.BLOCK)
                + " | " + probed.location() + " | " + probed.registry() + " | " + FluidTags.WATER + " | " + BlockTags.REPLACEABLE_BY_TREES + " | " + probed.cast(Registries.BLOCK).isPresent());
        Item[] items = {ComponentProbe.PLAIN.get(), ComponentProbe.STACK16.get(), Items.DIAMOND, Items.EMERALD};
        for (Item item : items) {
            p("tag item " + item, () -> {
                ItemStack s = new ItemStack(item);
                StringBuilder b = new StringBuilder();
                for (TagKey<Item> t : java.util.List.of(probed, diamonds, payment, none)) {
                    try {
                        b.append(s.is(t)).append(' ');
                    } catch (RuntimeException e) {
                        b.append("? ");
                    }
                }
                return b;
            });
        }
        p("tag item tags", () -> sorted(new ItemStack(ComponentProbe.PLAIN.get()).getTags()) + " " + sorted(new ItemStack(ComponentProbe.STACK16.get()).getTags())
                + " " + sorted(ComponentProbe.PLAIN.get().builtInRegistryHolder().tags()) + " " + ComponentProbe.PLAIN.is(probed) + " " + sorted(ComponentProbe.PLAIN.tags()));
        p("tag empty", () -> ItemStack.EMPTY.is(probed) + " " + ItemStack.EMPTY.is(diamonds));
        p("tag block", () -> CapabilityProbe.BLOCK.get().defaultBlockState().is(BlockTags.create(ResourceLocation.parse("horizonprobe:blocks"))) + " "
                + CapabilityProbe.BLOCK.get().defaultBlockState().is(BlockTags.REPLACEABLE_BY_TREES) + " " + sorted(CapabilityProbe.BLOCK.get().builtInRegistryHolder().tags()));
        p("tag fluid", () -> new FluidStack(Fluids.WATER, 5).is(FluidTags.WATER) + " " + new FluidStack(Fluids.WATER, 5).is(FluidTags.LAVA) + " " + new FluidStack(Fluids.LAVA, 5).is(FluidTags.LAVA)
                + " " + FluidStack.EMPTY.is(FluidTags.WATER) + " " + sorted(Fluids.WATER.builtInRegistryHolder().tags()));
    }

    static void holders() {
        p("holder members", () -> {
            net.minecraft.core.Holder<Item> h = Items.DIAMOND.builtInRegistryHolder();
            net.minecraft.core.Holder<Item> plain = ComponentProbe.PLAIN.get().builtInRegistryHolder();
            return h.getRegisteredName() + " " + h.isBound() + " " + h.is(ResourceLocation.withDefaultNamespace("diamond")) + " " + h.is(Items.STONE.builtInRegistryHolder().key())
                    + " " + h.is(Items.DIAMOND.builtInRegistryHolder().key()) + " " + h.is(k -> k.location().getPath().startsWith("dia")) + " " + h.is(h) + " " + h.is(plain)
                    + " " + plain.is(ComponentProbe.PLAIN) + " " + h.unwrapKey() + " | " + ComponentProbe.PLAIN.getRegisteredName() + " " + ComponentProbe.PLAIN.isBound()
                    + " " + ComponentProbe.PLAIN.is(plain) + " " + ComponentProbe.PLAIN.is(ComponentProbe.STACK16.get().builtInRegistryHolder()) + " " + ComponentProbe.PLAIN.unwrapKey()
                    + " " + ComponentProbe.PLAIN.is(ResourceLocation.parse("horizonprobe:plain")) + " " + ComponentProbe.PLAIN.is(k -> true) + " " + h.getKey() + " " + (h.getKey() == ((net.minecraft.core.Holder.Reference<Item>) h).key());
        });
    }

    static String sorted(java.util.stream.Stream<? extends TagKey<?>> tags) {
        java.util.List<String> all = new java.util.ArrayList<>();
        tags.forEach(t -> all.add(t.toString()));
        java.util.Collections.sort(all);
        return all.toString();
    }

    static void p(String key, Supplier<Object> call) {
        GameProbe.p(key, call);
    }
}
