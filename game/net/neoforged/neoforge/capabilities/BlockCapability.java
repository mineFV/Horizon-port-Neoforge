package net.neoforged.neoforge.capabilities;

import java.util.ArrayList;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

/**
 * The stand-in for NeoForge's BlockCapability: one for each name, and its providers for each block, asked in the order
 * they were registered until one gives something (observed, `standins --horizon-src`).
 */
public final class BlockCapability<T, C> extends BaseCapability<T, C> {
    final Map<Block, List<IBlockCapabilityProvider<T, C>>> providers = new IdentityHashMap<>();
    private boolean proxyable;

    private BlockCapability(ResourceLocation name, Class<T> typeClass, Class<C> contextClass) {
        super(name, typeClass, contextClass);
    }

    public static <T, C> BlockCapability<T, C> create(ResourceLocation name, Class<T> typeClass, Class<C> contextClass) {
        return HorizonCapabilities.made("block", name, typeClass, contextClass, () -> new BlockCapability<>(name, typeClass, contextClass));
    }

    public static <T> BlockCapability<T, Void> createVoid(ResourceLocation name, Class<T> typeClass) {
        return create(name, typeClass, void.class);
    }

    public static <T> BlockCapability<T, Direction> createSided(ResourceLocation name, Class<T> typeClass) {
        return create(name, typeClass, Direction.class);
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    public static synchronized List<BlockCapability<?, ?>> getAll() {
        return (List) HorizonCapabilities.all(BlockCapability.class);
    }

    public boolean isProxyable() {
        return proxyable;
    }

    void setProxyable(boolean proxyable) {
        this.proxyable = proxyable;
    }

    /**
     * The first non-null answer of the providers of the state's block. Without a block entity given, the game looks for
     * the one at the position: the levels' block entities aren't modelled here, so that says so.
     */
    public T getCapability(Level level, BlockPos pos, BlockState state, BlockEntity blockEntity, C context) {
        List<IBlockCapabilityProvider<T, C>> list;
        synchronized (this) {
            list = providers.get(state.getBlock());
            list = list == null ? List.of() : new ArrayList<>(list);
        }
        if (blockEntity == null && state.getBlock() instanceof net.minecraft.world.level.block.EntityBlock && !list.isEmpty()) {
            throw new UnsupportedOperationException("finding the block entity at a position isn't modelled (horizon-port-neoforge)");
        }
        for (IBlockCapabilityProvider<T, C> p : list) {
            T found = p.getCapability(level, pos, state, blockEntity, context);
            if (found != null) return found;
        }
        return null;
    }
}
