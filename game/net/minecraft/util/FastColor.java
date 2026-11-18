package net.minecraft.util;

/** The stand-in for the game's FastColor: colours packed in an int, ARGB and ABGR (observed, `standins`). */
public class FastColor {
    public static int as8BitChannel(float value) {
        return Mth.floor(value * 255.0F);
    }

    public static class ABGR32 {
        public static int alpha(int c) {
            return c >>> 24;
        }

        public static int red(int c) {
            return c & 255;
        }

        public static int green(int c) {
            return c >> 8 & 255;
        }

        public static int blue(int c) {
            return c >> 16 & 255;
        }

        public static int transparent(int c) {
            return c & 0xFFFFFF;
        }

        public static int opaque(int c) {
            return c | 0xFF000000;
        }

        public static int color(int alpha, int blue, int green, int red) {
            return alpha << 24 | blue << 16 | green << 8 | red;
        }

        public static int color(int alpha, int bgr) {
            return alpha << 24 | bgr & 0xFFFFFF;
        }

        public static int fromArgb32(int c) {
            return c & 0xFF00FF00 | (c & 0xFF0000) >> 16 | (c & 255) << 16;
        }
    }

    public static class ARGB32 {
        public static int alpha(int c) {
            return c >>> 24;
        }

        public static int red(int c) {
            return c >> 16 & 255;
        }

        public static int green(int c) {
            return c >> 8 & 255;
        }

        public static int blue(int c) {
            return c & 255;
        }

        public static int color(int alpha, int red, int green, int blue) {
            return alpha << 24 | red << 16 | green << 8 | blue;
        }

        public static int color(int red, int green, int blue) {
            return color(255, red, green, blue);
        }

        public static int multiply(int a, int b) {
            return color(alpha(a) * alpha(b) / 255, red(a) * red(b) / 255, green(a) * green(b) / 255, blue(a) * blue(b) / 255);
        }

        public static int lerp(float t, int a, int b) {
            return color(Mth.lerpInt(t, alpha(a), alpha(b)), Mth.lerpInt(t, red(a), red(b)), Mth.lerpInt(t, green(a), green(b)), Mth.lerpInt(t, blue(a), blue(b)));
        }

        public static int opaque(int c) {
            return c | 0xFF000000;
        }

        public static int color(int alpha, int rgb) {
            return alpha << 24 | rgb & 0xFFFFFF;
        }

        public static int colorFromFloat(float alpha, float red, float green, float blue) {
            return color(as8BitChannel(alpha), as8BitChannel(red), as8BitChannel(green), as8BitChannel(blue));
        }

        public static int average(int a, int b) {
            return color((alpha(a) + alpha(b)) / 2, (red(a) + red(b)) / 2, (green(a) + green(b)) / 2, (blue(a) + blue(b)) / 2);
        }
    }
}
