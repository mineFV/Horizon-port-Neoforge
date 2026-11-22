package net.minecraft.world.level.chunk.status;

/**
 * The stand-in for the game's ChunkStatus: FULL alone, the status of a chunk a level holds whole, which a mod asks a
 * level for (LevelReader.getChunk). Its name, text and index observed in a server (`standins`,
 * probe/horizonneo/probe/ChunkProbe.java). Not here: the generation's statuses before it, their order and heightmaps.
 */
public class ChunkStatus {
    public static final ChunkStatus FULL = new ChunkStatus("minecraft:full", 11);

    private final String name;
    private final int index;

    private ChunkStatus(String name, int index) {
        this.name = name;
        this.index = index;
    }

    public int getIndex() {
        return index;
    }

    public String getName() {
        return name;
    }

    @Override
    public String toString() {
        return name;
    }
}
