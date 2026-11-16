package com.example.tickers;

import net.minecraft.world.level.block.entity.BlockEntityTicker;

/** Made up: a block whose ticker is one method reference, made in two places (both run the same method). */
public final class OneTicker {
    public static BlockEntityTicker<OneTicker> ticker(boolean server) {
        return server ? OneTicker::serverTick : null;
    }

    public static BlockEntityTicker<OneTicker> again() {
        return OneTicker::serverTick;
    }

    static void serverTick(Object level, Object pos, Object state, OneTicker entity) {}
}
