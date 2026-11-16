package com.example.tickers;

import net.minecraft.world.level.block.entity.BlockEntityTicker;

/** Made up: a ticker that carries a value along, which the bytecode doesn't give. */
public final class Capturing {
    public static BlockEntityTicker<Capturing> ticker(int speed) {
        return (level, pos, state, entity) -> entity.spin(speed);
    }

    void spin(int speed) {}
}
