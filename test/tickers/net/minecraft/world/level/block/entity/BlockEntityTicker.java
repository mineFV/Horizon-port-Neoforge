package net.minecraft.world.level.block.entity;

/** Made up: a stand-in for the game's BlockEntityTicker, for Tickers' test (the real one takes a Level, a BlockPos, a BlockState). */
public interface BlockEntityTicker<T> {
    void tick(Object level, Object pos, Object state, T entity);
}
