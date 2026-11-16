package com.example.tickers;

import net.minecraft.world.level.block.entity.BlockEntityTicker;

/** Made up: a block with a ticker for each side: which one NeoForge gave can't be told from the class alone. */
public final class TwoTickers {
    public static BlockEntityTicker<TwoTickers> ticker(boolean server) {
        return server ? TwoTickers::serverTick : TwoTickers::clientTick;
    }

    static void serverTick(Object level, Object pos, Object state, TwoTickers entity) {}

    static void clientTick(Object level, Object pos, Object state, TwoTickers entity) {}
}
