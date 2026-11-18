package net.minecraft.core;

/** The stand-in for the game's SectionPos: its coordinates' arithmetic (its instances aren't here), observed. */
public class SectionPos extends Vec3i {
    public static final int SECTION_BITS = 4;
    public static final int SECTION_SIZE = 16;
    public static final int SECTION_MASK = 15;
    public static final int SECTION_HALF_SIZE = 8;
    public static final int SECTION_MAX_INDEX = 15;

    private SectionPos(int x, int y, int z) {
        super(x, y, z);
    }

    public static int posToSectionCoord(double pos) {
        return blockToSectionCoord(net.minecraft.util.Mth.floor(pos));
    }

    public static int blockToSectionCoord(int block) {
        return block >> 4;
    }

    public static int blockToSectionCoord(double block) {
        return net.minecraft.util.Mth.floor(block) >> 4;
    }

    public static int sectionRelative(int block) {
        return block & 15;
    }

    public static int sectionToBlockCoord(int section) {
        return section << 4;
    }

    public static int sectionToBlockCoord(int section, int offset) {
        return sectionToBlockCoord(section) + offset;
    }

    public static int x(long packed) {
        return (int) (packed << 0 >> 42);
    }

    public static int y(long packed) {
        return (int) (packed << 44 >> 44);
    }

    public static int z(long packed) {
        return (int) (packed << 22 >> 42);
    }
}
