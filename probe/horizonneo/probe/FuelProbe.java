package horizonneo.probe;

import net.minecraft.core.component.DataComponents;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.furnace.FurnaceFuelBurnTimeEvent;
import net.neoforged.neoforge.registries.datamaps.builtin.FurnaceFuel;
import net.neoforged.neoforge.registries.datamaps.builtin.NeoForgeDataMaps;

/**
 * Burn times: the furnace fuels data map (the game's, and the probe's own data for its plain item), what a stack burns
 * for, FurnaceFuelBurnTimeEvent's listeners changing or cancelling it, and the mod that made a stack. The stand-ins are
 * given the fuels as the bridge gives `dump`'s.
 */
final class FuelProbe {
    private FuelProbe() {}

    /** As `dump` would read them: the fuels among the items asked (whole there, so the others have none). */
    static final String FUELS = "minecraft:coal=1600;minecraft:stick=100;minecraft:lava_bucket=20000;horizonprobe:plain=123";

    private static void given() {
        try {
            java.lang.reflect.Method known = Class.forName("net.neoforged.neoforge.registries.HorizonRegistries").getMethod("known", String.class, String.class);
            known.invoke(null, "furnace_fuels", FUELS);
            known.invoke(null, "remainders", REMAINDERS);
        } catch (ClassNotFoundException e) {
            // The game's own data maps.
        } catch (ReflectiveOperationException e) {
            throw new IllegalStateException(e);
        }
    }

    /** As `dump` would read them: what the items asked leave in a crafting grid. */
    static final String REMAINDERS = "minecraft:lava_bucket=minecraft:bucket";

    private static boolean active;
    private static final StringBuilder SEEN = new StringBuilder();

    private static void seen(String phase, FurnaceFuelBurnTimeEvent e) {
        if (!active) return;
        SEEN.append(phase).append(':').append(e.getItemStack().getItem()).append(':').append(e.getBurnTime()).append(':').append(e.isCanceled())
                .append(':').append(e.getRecipeType()).append(' ');
    }

    static void run() {
        given();
        p("fuel map", () -> NeoForgeDataMaps.FURNACE_FUELS.id() + " " + NeoForgeDataMaps.FURNACE_FUELS.registryKey());
        p("fuel record", () -> new FurnaceFuel(5) + " " + new FurnaceFuel(5).equals(new FurnaceFuel(5)) + " " + new FurnaceFuel(5).equals(new FurnaceFuel(6))
                + " " + new FurnaceFuel(5).hashCode() + " " + new FurnaceFuel(5).burnTime());
        Item[] items = {Items.COAL, Items.STICK, Items.LAVA_BUCKET, Items.DIAMOND, ComponentProbe.PLAIN.get(), ComponentProbe.STACK16.get()};
        for (Item item : items) {
            p("fuel data " + item, () -> item.builtInRegistryHolder().getData(NeoForgeDataMaps.FURNACE_FUELS) + " " + new ItemStack(item).getBurnTime(null)
                    + " " + item.getBurnTime(new ItemStack(item), null) + " " + new ItemStack(item, 5).getBurnTime(null));
        }
        p("fuel empty", () -> ItemStack.EMPTY.getBurnTime(null) + " " + new ItemStack(Items.COAL, 0).getBurnTime(null));
        NeoForge.EVENT_BUS.addListener(EventPriority.HIGH, false, FurnaceFuelBurnTimeEvent.class, e -> {
            seen("high", e);
            if (!active) return;
            Item item = e.getItemStack().getItem();
            if (item == ComponentProbe.STACK16.get()) e.setBurnTime(77);
            if (item == Items.COAL) e.setCanceled(true);
            if (item == Items.STICK) e.setBurnTime(-5);
            if (item == Items.LAVA_BUCKET) {
                e.setBurnTime(9);
                e.setCanceled(true);
            }
        });
        NeoForge.EVENT_BUS.addListener(EventPriority.LOW, false, FurnaceFuelBurnTimeEvent.class, e -> seen("low", e));
        NeoForge.EVENT_BUS.addListener(EventPriority.LOWEST, true, FurnaceFuelBurnTimeEvent.class, e -> seen("lowest", e));
        active = true;
        try {
            ItemStack[] stacks = {new ItemStack(ComponentProbe.STACK16.get()), new ItemStack(Items.COAL), new ItemStack(Items.STICK), new ItemStack(Items.LAVA_BUCKET),
                    new ItemStack(ComponentProbe.PLAIN.get()), new ItemStack(Items.DIAMOND), ItemStack.EMPTY};
            for (ItemStack s : stacks) {
                p("fuel event " + s.getItem(), () -> {
                    SEEN.setLength(0);
                    int t = s.getBurnTime(null);
                    return t + " | " + SEEN.toString().trim();
                });
            }
        } finally {
            active = false;
        }
        Item[] kept = {Items.LAVA_BUCKET, Items.COAL, Items.DIAMOND, ComponentProbe.PLAIN.get()};
        for (Item item : kept) {
            p("remainder " + item, () -> item.hasCraftingRemainingItem() + " " + item.getCraftingRemainingItem() + " " + new ItemStack(item, 3).hasCraftingRemainingItem()
                    + " " + new ItemStack(item, 3).getCraftingRemainingItem() + " " + item.getCraftingRemainingItem(new ItemStack(item)));
        }
        p("remainder empty", () -> ItemStack.EMPTY.hasCraftingRemainingItem() + " " + ItemStack.EMPTY.getCraftingRemainingItem());
        p("data map synced", () -> {
            net.neoforged.neoforge.registries.datamaps.DataMapType.Builder<Integer, Item> b = net.neoforged.neoforge.registries.datamaps.DataMapType.builder(
                    net.minecraft.resources.ResourceLocation.parse("horizonprobe:numbers"), net.minecraft.core.registries.Registries.ITEM, com.mojang.serialization.Codec.INT);
            return (b.synced(com.mojang.serialization.Codec.INT, true) == b) + " " + b.build().id();
        });
        for (net.minecraft.world.level.block.state.properties.NoteBlockInstrument i : net.minecraft.world.level.block.state.properties.NoteBlockInstrument.values()) {
            p("instrument " + i.ordinal(), () -> i + " " + i.name() + " " + i.getSerializedName() + " " + i.isTunable() + " " + i.hasCustomSound() + " " + i.worksAboveNoteBlock());
        }
        p("push reactions", () -> java.util.Arrays.toString(net.minecraft.world.level.material.PushReaction.values()));
        p("block properties", () -> {
            net.minecraft.world.level.block.state.BlockBehaviour.Properties props = net.minecraft.world.level.block.state.BlockBehaviour.Properties.of();
            return (props.isSuffocating((s, l, q) -> false) == props) + " " + (props.isViewBlocking((s, l, q) -> false) == props) + " " + (props.lightLevel(s -> 3) == props)
                    + " " + (props.instrument(net.minecraft.world.level.block.state.properties.NoteBlockInstrument.BASS) == props)
                    + " " + (props.pushReaction(net.minecraft.world.level.material.PushReaction.BLOCK) == props) + " " + (props.noCollission() == props) + " " + (props.noOcclusion() == props)
                    + " " + (props.noLootTable() == props) + " " + (props.liquid() == props) + " " + (props.replaceable() == props) + " " + (props.dynamicShape() == props)
                    + " " + (props.requiresCorrectToolForDrops() == props) + " " + (props.isValidSpawn((s, l, q, t) -> false) == props);
        });
        p("block map color", () -> {
            net.minecraft.world.level.block.state.BlockBehaviour.Properties props = net.minecraft.world.level.block.state.BlockBehaviour.Properties.of();
            return (props.mapColor(net.minecraft.world.level.material.MapColor.STONE) == props) + " " + (props.mapColor(net.minecraft.world.item.DyeColor.RED) == props)
                    + " " + (props.mapColor(state -> net.minecraft.world.level.material.MapColor.NONE) == props);
        });
        ItemStack changed = new ItemStack(ComponentProbe.PLAIN.get());
        changed.set(DataComponents.MAX_STACK_SIZE, 3);
        ItemStack[] stacks = {new ItemStack(Items.DIAMOND), new ItemStack(ComponentProbe.PLAIN.get()), changed, ItemStack.EMPTY, new ItemStack(Items.COAL, 3)};
        for (int i = 0; i < stacks.length; i++) {
            ItemStack s = stacks[i];
            p("creator " + i, () -> s.getItem().getCreatorModId(s) + " " + s.isComponentsPatchEmpty());
        }
    }

    static void p(String key, java.util.function.Supplier<Object> call) {
        GameProbe.p(key, call);
    }
}
