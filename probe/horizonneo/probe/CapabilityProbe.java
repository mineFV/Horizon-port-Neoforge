package horizonneo.probe;

import java.util.function.Supplier;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.neoforged.neoforge.capabilities.BlockCapability;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.capabilities.EntityCapability;
import net.neoforged.neoforge.capabilities.ItemCapability;
import net.neoforged.neoforge.capabilities.RegisterCapabilitiesEvent;
import net.neoforged.neoforge.fluids.capability.IFluidHandler;
import net.neoforged.neoforge.registries.DeferredBlock;
import net.neoforged.neoforge.registries.DeferredRegister;

/**
 * Capabilities: NeoForge's, made ones, and providers registered for the probe's own item and block. In a server the
 * registration event is NeoForge's (InServer listens to it on the probe's bus); for the stand-ins, given() fires theirs.
 */
final class CapabilityProbe {
    private CapabilityProbe() {}

    static final DeferredRegister.Blocks BLOCKS = DeferredRegister.createBlocks("horizonprobe");
    static final DeferredBlock<Block> BLOCK = BLOCKS.register("block", () -> new Block(BlockBehaviour.Properties.of()));

    static final ItemCapability<String, Void> WORDS = ItemCapability.createVoid(ResourceLocation.fromNamespaceAndPath("horizonprobe", "words"), String.class);
    static final ItemCapability<String, Integer> COUNTED = ItemCapability.create(ResourceLocation.fromNamespaceAndPath("horizonprobe", "counted"), String.class, Integer.class);
    static final BlockCapability<String, Direction> SIDES = BlockCapability.createSided(ResourceLocation.fromNamespaceAndPath("horizonprobe", "sides"), String.class);

    /** The providers, registered by whichever event there is. */
    static void register(RegisterCapabilitiesEvent e) {
        e.registerItem(WORDS, (stack, nothing) -> stack.getCount() > 1 ? "many " + stack.getCount() : null, ComponentProbe.PLAIN.get());
        e.registerItem(WORDS, (stack, nothing) -> "second " + stack.getCount() + " " + nothing, ComponentProbe.PLAIN.get(), ComponentProbe.STACK16.get());
        e.registerItem(COUNTED, (stack, n) -> stack.getItem() + " " + n, ComponentProbe.PLAIN.get());
        e.registerBlock(SIDES, (level, pos, state, be, side) -> side == Direction.UP ? null : "side " + side + " " + pos + " " + state.getBlock() + " " + (level == null) + " " + (be == null), BLOCK.get());
        e.registerBlock(SIDES, (level, pos, state, be, side) -> "fallback " + side, BLOCK.get());
        e.setProxyable(SIDES);
        RESULTS.append(e.isItemRegistered(WORDS, ComponentProbe.PLAIN.get())).append(' ').append(e.isItemRegistered(WORDS, Items.DIAMOND)).append(' ')
                .append(e.isBlockRegistered(SIDES, BLOCK.get())).append(' ').append(e.isItemRegistered(COUNTED, ComponentProbe.STACK16.get()));
        try {
            e.registerItem(WORDS, (stack, nothing) -> "none");
        } catch (RuntimeException x) {
            RESULTS.append(' ').append(x.getClass().getName()).append(": ").append(x.getMessage());
        }
        try {
            e.registerBlock(SIDES, (level, pos, state, be, side) -> "none");
        } catch (RuntimeException x) {
            RESULTS.append(' ').append(x.getClass().getName()).append(": ").append(x.getMessage());
        }
    }

    static final StringBuilder RESULTS = new StringBuilder();

    /** The stand-ins' own event, fired here (HorizonCapabilities isn't the game's: absent in a server). */
    private static void given() {
        try {
            Object event = Class.forName("net.neoforged.neoforge.capabilities.HorizonCapabilities").getMethod("event").invoke(null);
            register((RegisterCapabilitiesEvent) event);
        } catch (ClassNotFoundException e) {
            // The game's own classes: NeoForge fired it.
        } catch (ReflectiveOperationException e) {
            throw new IllegalStateException(e);
        }
    }

    static void run() {
        // A capability's cache at a position: what it keeps of what it is made with, and what it refuses. Asking it isn't
        // compared (the stand-ins don't model a level's capabilities at a position).
        p("capability cache", () -> {
            net.minecraft.server.level.ServerLevel level = GameProbe.level();
            BlockPos.MutableBlockPos at = new BlockPos.MutableBlockPos(3, 70, 5);
            var cache = net.neoforged.neoforge.capabilities.BlockCapabilityCache.create(Capabilities.ItemHandler.BLOCK, level, at, Direction.UP);
            at.set(9, 9, 9);
            var later = net.neoforged.neoforge.capabilities.BlockCapabilityCache.create(Capabilities.EnergyStorage.BLOCK, level, at.immutable(), null, () -> false, () -> {});
            StringBuilder b = new StringBuilder().append(cache.pos()).append(' ').append(cache.pos().getClass().getName()).append(' ').append(cache.context())
                    .append(' ').append(cache.level() == level).append(' ').append(later.pos()).append(' ').append(later.context()).append(' ').append(later.level() == level)
                    .append(' ').append(cache.getClass().getName());
            for (Runnable r : new Runnable[] {() -> net.neoforged.neoforge.capabilities.BlockCapabilityCache.create(null, level, at, Direction.UP),
                    () -> net.neoforged.neoforge.capabilities.BlockCapabilityCache.create(Capabilities.ItemHandler.BLOCK, null, at, Direction.UP),
                    () -> net.neoforged.neoforge.capabilities.BlockCapabilityCache.create(Capabilities.ItemHandler.BLOCK, level, null, Direction.UP),
                    () -> net.neoforged.neoforge.capabilities.BlockCapabilityCache.create(Capabilities.ItemHandler.BLOCK, level, at, Direction.UP, null, () -> {}),
                    () -> net.neoforged.neoforge.capabilities.BlockCapabilityCache.create(Capabilities.ItemHandler.BLOCK, level, at, Direction.UP, () -> true, null),
                    () -> net.neoforged.neoforge.capabilities.BlockCapabilityCache.create(Capabilities.ItemHandler.BLOCK, level, at, null)}) {
                try {
                    r.run();
                    b.append(" made");
                } catch (RuntimeException e) {
                    b.append(' ').append(e.getClass().getSimpleName());
                }
            }
            return b;
        });
        given();
        p("capability registered", () -> RESULTS);
        Object[] caps = {Capabilities.EnergyStorage.BLOCK, Capabilities.EnergyStorage.ENTITY, Capabilities.EnergyStorage.ITEM, Capabilities.FluidHandler.BLOCK,
            Capabilities.FluidHandler.ENTITY, Capabilities.FluidHandler.ITEM, Capabilities.ItemHandler.BLOCK, Capabilities.ItemHandler.ENTITY,
            Capabilities.ItemHandler.ENTITY_AUTOMATION, Capabilities.ItemHandler.ITEM, WORDS, COUNTED, SIDES};
        for (int i = 0; i < caps.length; i++) {
            Object c = caps[i];
            p("capability " + i, () -> {
                net.neoforged.neoforge.capabilities.BaseCapability<?, ?> b = (net.neoforged.neoforge.capabilities.BaseCapability<?, ?>) c;
                return b.getClass().getName() + " " + b.name() + " " + b.typeClass().getName() + " " + b.contextClass().getName();
            });
        }
        p("capability proxyable", () -> Capabilities.EnergyStorage.BLOCK.isProxyable() + " " + SIDES.isProxyable() + " " + Capabilities.ItemHandler.BLOCK.isProxyable()
                + " " + Capabilities.FluidHandler.BLOCK.isProxyable());
        p("capability same", () -> (ItemCapability.createVoid(ResourceLocation.fromNamespaceAndPath("horizonprobe", "words"), String.class) == WORDS) + " "
                + (BlockCapability.createSided(ResourceLocation.fromNamespaceAndPath("neoforge", "energy"), net.neoforged.neoforge.energy.IEnergyStorage.class) == Capabilities.EnergyStorage.BLOCK)
                + " " + (EntityCapability.createSided(ResourceLocation.fromNamespaceAndPath("neoforge", "energy"), net.neoforged.neoforge.energy.IEnergyStorage.class) == Capabilities.EnergyStorage.ENTITY));
        p("capability other classes", () -> ItemCapability.create(ResourceLocation.fromNamespaceAndPath("horizonprobe", "words"), Integer.class, Void.class));
        p("capability other context", () -> BlockCapability.createVoid(ResourceLocation.fromNamespaceAndPath("horizonprobe", "sides"), String.class));
        p("capability all", () -> BlockCapability.getAll().contains(SIDES) + " " + ItemCapability.getAll().contains(WORDS) + " " + EntityCapability.getAll().contains(Capabilities.EnergyStorage.ENTITY));
        int[] counts = {1, 3};
        for (int n : counts) {
            p("capability item " + n, () -> {
                ItemStack plain = new ItemStack(ComponentProbe.PLAIN.get(), n);
                return plain.getCapability(WORDS) + " | " + WORDS.getCapability(plain, null) + " | " + plain.getCapability(COUNTED, 7) + " | "
                        + new ItemStack(ComponentProbe.STACK16.get(), n).getCapability(WORDS) + " | " + new ItemStack(Items.DIAMOND, n).getCapability(WORDS)
                        + " | " + new ItemStack(Items.DIAMOND, n).getCapability(Capabilities.EnergyStorage.ITEM) + " | " + plain.getCapability(Capabilities.ItemHandler.ITEM);
            });
        }
        p("capability empty", () -> ItemStack.EMPTY.getCapability(WORDS) + " " + new ItemStack(ComponentProbe.PLAIN.get(), 0).getCapability(WORDS));
        Direction[] sides = {null, Direction.UP, Direction.NORTH};
        for (Direction side : sides) {
            p("capability block " + side, () -> SIDES.getCapability(null, new BlockPos(1, 2, 3), BLOCK.get().defaultBlockState(), null, side));
        }
        p("stack is", () -> {
            ItemStack plain = new ItemStack(ComponentProbe.PLAIN.get());
            return plain.is(ComponentProbe.PLAIN.get()) + " " + plain.is(Items.DIAMOND) + " " + plain.is(ComponentProbe.PLAIN) + " " + plain.is(ComponentProbe.PLAIN.get().builtInRegistryHolder())
                    + " " + ItemStack.EMPTY.is(Items.AIR) + " " + ItemStack.EMPTY.is(Items.AIR.builtInRegistryHolder()) + " " + new ItemStack(Items.DIAMOND).is(Items.DIAMOND.builtInRegistryHolder())
                    + " " + plain.is(Items.DIAMOND.builtInRegistryHolder());
        });
        p("fluid actions", () -> IFluidHandler.FluidAction.EXECUTE.execute() + " " + IFluidHandler.FluidAction.EXECUTE.simulate() + " "
                + IFluidHandler.FluidAction.SIMULATE.execute() + " " + IFluidHandler.FluidAction.SIMULATE.simulate() + " " + java.util.Arrays.toString(IFluidHandler.FluidAction.values()));
    }

    static void p(String key, Supplier<Object> call) {
        GameProbe.p(key, call);
    }
}
