package horizonneo.probe;

import java.util.function.Supplier;
import net.minecraft.core.SectionPos;
import net.minecraft.util.FastColor;

/** The game's colour packing and section arithmetic, on values that pin them down (extremes included). */
final class ArithProbe {
    private ArithProbe() {}

    static void run() {
        int[] colors = {0, -1, 0x12345678, 0x80FF0001, 0x7F00FF80, Integer.MIN_VALUE, Integer.MAX_VALUE, 0xFFFFFF, 0xAB000000};
        for (int a : colors) {
            p("argb " + a, () -> FastColor.ARGB32.alpha(a) + " " + FastColor.ARGB32.red(a) + " " + FastColor.ARGB32.green(a) + " " + FastColor.ARGB32.blue(a) + " "
                    + FastColor.ARGB32.opaque(a) + " " + FastColor.ARGB32.color(77, a) + " " + FastColor.ABGR32.alpha(a) + " " + FastColor.ABGR32.red(a) + " " + FastColor.ABGR32.green(a)
                    + " " + FastColor.ABGR32.blue(a) + " " + FastColor.ABGR32.transparent(a) + " " + FastColor.ABGR32.opaque(a) + " " + FastColor.ABGR32.color(9, a) + " " + FastColor.ABGR32.fromArgb32(a));
            for (int b : colors) {
                p("argb " + a + " " + b, () -> FastColor.ARGB32.multiply(a, b) + " " + FastColor.ARGB32.average(a, b) + " " + FastColor.ARGB32.lerp(0.25F, a, b) + " " + FastColor.ARGB32.lerp(-0.5F, a, b)
                        + " " + FastColor.ARGB32.lerp(1.7F, a, b));
            }
        }
        int[] parts = {0, 1, 255, 256, -1, 300, -300};
        for (int x : parts) {
            p("argb parts " + x, () -> FastColor.ARGB32.color(x, x + 1, x + 2, x + 3) + " " + FastColor.ARGB32.color(x, x, x) + " " + FastColor.ABGR32.color(x, x + 1, x + 2, x + 3));
        }
        float[] floats = {0F, 1F, 0.5F, 0.999F, 1.5F, -0.2F, Float.NaN, 0.0039F};
        for (float f : floats) {
            p("argb float " + f, () -> FastColor.as8BitChannel(f) + " " + FastColor.ARGB32.colorFromFloat(f, f, 1 - f, 0.25F));
        }
        int[] blocks = {0, 15, 16, -1, -16, -17, Integer.MAX_VALUE, Integer.MIN_VALUE, 12345};
        for (int b : blocks) {
            p("section " + b, () -> SectionPos.blockToSectionCoord(b) + " " + SectionPos.sectionRelative(b) + " " + SectionPos.sectionToBlockCoord(b) + " " + SectionPos.sectionToBlockCoord(b, 7)
                    + " " + SectionPos.blockToSectionCoord((double) b + 0.5) + " " + SectionPos.posToSectionCoord(b - 0.5));
        }
        long[] packed = {0L, -1L, 1L << 42, 123456789012345L, Long.MIN_VALUE, Long.MAX_VALUE, 0x00000FFFFF00000FL};
        for (long l : packed) {
            p("section packed " + l, () -> SectionPos.x(l) + " " + SectionPos.y(l) + " " + SectionPos.z(l));
        }
        int[][] lerps = {{0, 10}, {10, 0}, {-5, 5}, {Integer.MIN_VALUE, Integer.MAX_VALUE}, {7, 7}};
        float[] deltas = {0F, 0.5F, 0.33F, 1F, -1F, 2.5F, Float.NaN};
        for (int[] l : lerps) {
            for (float d : deltas) p("lerp int " + l[0] + " " + l[1] + " " + d, () -> net.minecraft.util.Mth.lerpInt(d, l[0], l[1]));
        }
        p("section constants", () -> SectionPos.SECTION_BITS + " " + SectionPos.SECTION_SIZE + " " + SectionPos.SECTION_MASK + " " + SectionPos.SECTION_HALF_SIZE + " " + SectionPos.SECTION_MAX_INDEX);
    }

    static void p(String key, Supplier<Object> call) {
        GameProbe.p(key, call);
    }
}
