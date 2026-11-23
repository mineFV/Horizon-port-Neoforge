package net.neoforged.neoforge.capabilities;

import java.util.Objects;
import java.util.function.BooleanSupplier;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;

/**
 * The stand-in for NeoForge's BlockCapabilityCache: a block's capability at a position of a level, which NeoForge asks
 * again when the level says it may have changed. Made as NeoForge makes it (null refused, the position kept as an
 * immutable copy, observed in a server); asking it refuses, since a level's capabilities at a position (its block
 * entities) aren't modelled, nor when they change.
 */
public final class BlockCapabilityCache<T, C> {
    private final BlockCapability<T, C> capability;
    private final ServerLevel level;
    private final BlockPos pos;
    private final C context;

    private BlockCapabilityCache(BlockCapability<T, C> capability, ServerLevel level, BlockPos pos, C context) {
        this.capability = capability;
        this.level = level;
        this.pos = pos;
        this.context = context;
    }

    public static <T, C> BlockCapabilityCache<T, C> create(BlockCapability<T, C> capability, ServerLevel level, BlockPos pos, C context) {
        return create(capability, level, pos, context, () -> true, () -> {});
    }

    /** Observed: what isn't given is refused (a NullPointerException), the level last, as NeoForge registers the cache with it. */
    public static <T, C> BlockCapabilityCache<T, C> create(BlockCapability<T, C> capability, ServerLevel level, BlockPos pos, C context, BooleanSupplier isValid,
            Runnable invalidationListener) {
        Objects.requireNonNull(capability);
        Objects.requireNonNull(isValid);
        Objects.requireNonNull(invalidationListener);
        BlockPos at = pos.immutable();
        Objects.requireNonNull(level);
        return new BlockCapabilityCache<>(capability, level, at, context);
    }

    public ServerLevel level() {
        return level;
    }

    public BlockPos pos() {
        return pos;
    }

    public C context() {
        return context;
    }

    /** Not modelled: a level's capabilities at a position (its block entities), and when they change. */
    public T getCapability() {
        throw new UnsupportedOperationException("a level's capabilities at a position aren't modelled (horizon-port-neoforge): " + capability.name() + " at " + pos);
    }
}
