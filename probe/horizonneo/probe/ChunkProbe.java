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
