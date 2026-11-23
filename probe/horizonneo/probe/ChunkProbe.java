package horizonneo.probe;

import net.minecraft.world.level.chunk.status.ChunkStatus;

/** The game's chunk statuses, as GameProbe observes the rest (see there): FULL, the one a mod asks a level for. */
final class ChunkProbe {
    private ChunkProbe() {}

    static void run() {
        p("chunk status full", () -> ChunkStatus.FULL.getName() + " " + ChunkStatus.FULL + " " + ChunkStatus.FULL.getIndex()
                + " " + ChunkStatus.FULL.getClass().getName());
        p("dimension heights", () -> net.minecraft.world.level.dimension.DimensionType.BITS_FOR_Y + " " + net.minecraft.world.level.dimension.DimensionType.MIN_HEIGHT
                + " " + net.minecraft.world.level.dimension.DimensionType.Y_SIZE + " " + net.minecraft.world.level.dimension.DimensionType.MAX_Y
                + " " + net.minecraft.world.level.dimension.DimensionType.MIN_Y + " " + net.minecraft.world.level.dimension.DimensionType.WAY_ABOVE_MAX_Y
                + " " + net.minecraft.world.level.dimension.DimensionType.WAY_BELOW_MIN_Y);
        // The particles a mod names: redstone's dust, smoke and portal, their settings and kinds.
        p("particles", () -> {
            net.minecraft.core.particles.DustParticleOptions r = net.minecraft.core.particles.DustParticleOptions.REDSTONE;
            StringBuilder b = new StringBuilder();
            b.append(r.getColor().x()).append(',').append(r.getColor().y()).append(',').append(r.getColor().z()).append(' ').append(r.getScale()).append(' ')
                    .append(r.getType() == net.minecraft.core.particles.ParticleTypes.DUST).append(' ')
                    .append(net.minecraft.core.particles.DustParticleOptions.REDSTONE_PARTICLE_COLOR == r.getColor()).append(' ')
                    .append(net.minecraft.core.particles.ScalableParticleOptionsBase.MIN_SCALE).append(' ').append(net.minecraft.core.particles.ScalableParticleOptionsBase.MAX_SCALE);
            for (float scale : new float[] {10f, 0f, -1f, 0.5f, Float.NaN, Float.POSITIVE_INFINITY}) {
                b.append(' ').append(new net.minecraft.core.particles.DustParticleOptions(new org.joml.Vector3f(0.5f, 0.25f, 1f), scale).getScale());
            }
            b.append(' ').append(net.minecraft.core.particles.ParticleTypes.SMOKE.getType() == net.minecraft.core.particles.ParticleTypes.SMOKE)
                    .append(net.minecraft.core.particles.ParticleTypes.SMOKE.getOverrideLimiter()).append(net.minecraft.core.particles.ParticleTypes.PORTAL.getOverrideLimiter())
                    .append(net.minecraft.core.particles.ParticleTypes.DUST.getOverrideLimiter()).append(' ')
                    .append(net.minecraft.core.particles.ParticleTypes.SMOKE != net.minecraft.core.particles.ParticleTypes.PORTAL).append(' ').append(r.getClass().getName())
                    .append(' ').append(net.minecraft.core.particles.ParticleTypes.SMOKE.getClass().getName()).append(' ')
                    .append(net.minecraft.core.particles.ParticleTypes.DUST.getClass().getName());
            return b;
        });
        // The blocks' sound types a mod names, their sounds, and the custom stats' ids.
        p("sound types", () -> {
            StringBuilder b = new StringBuilder();
            net.minecraft.world.level.block.SoundType[] types = {net.minecraft.world.level.block.SoundType.SAND, net.minecraft.world.level.block.SoundType.GRASS,
                net.minecraft.world.level.block.SoundType.DEEPSLATE};
            for (net.minecraft.world.level.block.SoundType t : types) {
                b.append(t.getVolume()).append(' ').append(t.getPitch()).append(' ').append(t.volume).append(' ').append(t.pitch);
                for (net.minecraft.sounds.SoundEvent e : new net.minecraft.sounds.SoundEvent[] {t.getBreakSound(), t.getStepSound(), t.getPlaceSound(), t.getHitSound(), t.getFallSound()}) {
                    b.append(' ').append(e.getLocation()).append('/').append(e.getRange(1.0F)).append('/').append(e.getRange(2.0F));
                }
                b.append("; ");
            }
            b.append(net.minecraft.world.level.block.SoundType.SAND.getBreakSound() == net.minecraft.sounds.SoundEvents.SAND_BREAK).append(' ')
                    .append(net.minecraft.world.level.block.SoundType.GRASS.getFallSound() == net.minecraft.sounds.SoundEvents.GRASS_FALL).append(' ')
                    .append(net.minecraft.world.level.block.SoundType.DEEPSLATE.getHitSound() == net.minecraft.sounds.SoundEvents.DEEPSLATE_HIT).append(' ')
                    .append(net.minecraft.stats.Stats.OPEN_CHEST).append(' ').append(net.minecraft.stats.Stats.OPEN_BARREL);
            return b;
        });
        p("block box", () -> ShapeProbe.describe(net.minecraft.world.level.block.Block.box(2, 0, 2, 14, 13, 14)) + " | "
                + ShapeProbe.describe(net.minecraft.world.level.block.Block.box(0, 0, 0, 16, 16, 16)) + " | " + ShapeProbe.describe(net.minecraft.world.level.block.Block.box(1, 1, 1, 15, 3, 15)));
        // The game's folders: under the folder it runs in, in a server and here alike.
        p("fml paths", () -> {
            StringBuilder b = new StringBuilder();
            java.nio.file.Path here = java.nio.file.Path.of("").toAbsolutePath();
            for (net.neoforged.fml.loading.FMLPaths f : net.neoforged.fml.loading.FMLPaths.values()) {
                b.append(f).append(' ').append(f.relative()).append(' ').append(f.get().equals(here.resolve(f.relative()).normalize()))
                        .append(' ').append(f.get().isAbsolute()).append(';');
            }
            return b;
        });
        p("fml paths made", () -> {
            java.nio.file.Path relative = java.nio.file.Path.of("horizonprobe-made", "inner");
            java.nio.file.Path made = net.neoforged.fml.loading.FMLPaths.getOrCreateGameRelativePath(relative);
            String said = made.equals(java.nio.file.Path.of("").toAbsolutePath().resolve(relative)) + " " + java.nio.file.Files.isDirectory(made)
                    + " " + net.neoforged.fml.loading.FMLPaths.getOrCreateGameRelativePath(relative).equals(made);
            try {
                java.nio.file.Files.delete(made);
                java.nio.file.Files.delete(made.getParent());
            } catch (java.io.IOException e) {
                return said + " (not deleted: " + e + ")";
            }
            return said;
        });
    }

    static void p(String key, java.util.function.Supplier<Object> call) {
        GameProbe.p(key, call);
    }
}
