package horizonneo.probe;

import net.minecraft.world.level.chunk.status.ChunkStatus;

/** The game's chunk statuses, as GameProbe observes the rest (see there): FULL, the one a mod asks a level for. */
final class ChunkProbe {
    private ChunkProbe() {}

    static void run() {
        p("chunk status full", () -> ChunkStatus.FULL.getName() + " " + ChunkStatus.FULL + " " + ChunkStatus.FULL.getIndex()
                + " " + ChunkStatus.FULL.getClass().getName());
    }

    static void p(String key, java.util.function.Supplier<Object> call) {
        GameProbe.p(key, call);
    }
}
