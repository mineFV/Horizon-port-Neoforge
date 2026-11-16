package horizonneo.probe;

import java.util.UUID;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.ByteArrayTag;
import net.minecraft.nbt.ByteTag;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.DoubleTag;
import net.minecraft.nbt.FloatTag;
import net.minecraft.nbt.IntArrayTag;
import net.minecraft.nbt.IntTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.LongArrayTag;
import net.minecraft.nbt.LongTag;
import net.minecraft.nbt.NbtUtils;
import net.minecraft.nbt.NumericTag;
import net.minecraft.nbt.ShortTag;
import net.minecraft.nbt.StringTag;
import net.minecraft.nbt.Tag;

/** The game's NBT, as GameProbe observes the rest (see there): the same calls against the game and the stand-ins. */
final class NbtProbe {
    private NbtProbe() {}

    static final long[] LONGS = {0, 1, -1, 2, 127, 128, -128, -129, 255, 1023, 1024, 1025, 32767, 32768, -32769, 65535,
        Integer.MAX_VALUE, Integer.MIN_VALUE, 1L << 40, -(1L << 50) + 3, Long.MAX_VALUE, Long.MIN_VALUE};
    static final double[] DOUBLES = {0.0, -0.0, 0.5, -0.5, 1.0, -1.5, 3.4e38, 3.5e38, 1e10, -1e10, 2147483647.9,
        -2147483648.9, 9.3e18, -9.3e18, 1e-50, Double.NaN, Double.POSITIVE_INFINITY, Double.NEGATIVE_INFINITY, 0.1, 1e7, 1e-4};
    static final String[] TEXTS = {"", "a", "abc", "with space", "q\"uote", "ap'os", "both\"'", "back\\slash", "new\nline",
        "tab\t", "é ü ß", "日本", "😀", "a.b_c+d-e", "123", "-1", "1b", "true", "{}", "[x]", "key:value", "\u0000", "\u007f"};

    static void run() {
        numbers();
        texts();
        compounds();
        lists();
        arrays();
        positions();
    }

    static void numbers() {
        for (long v : LONGS) {
            String k = "number " + v;
            p(k + " byte", () -> num(ByteTag.valueOf((byte) v)) + " same " + (ByteTag.valueOf((byte) v) == ByteTag.valueOf((byte) v)));
            p(k + " short", () -> num(ShortTag.valueOf((short) v)) + " same " + (ShortTag.valueOf((short) v) == ShortTag.valueOf((short) v)));
            p(k + " int", () -> num(IntTag.valueOf((int) v)) + " same " + (IntTag.valueOf((int) v) == IntTag.valueOf((int) v)));
            p(k + " long", () -> num(LongTag.valueOf(v)) + " same " + (LongTag.valueOf(v) == LongTag.valueOf(v)));
            p(k + " equal kinds", () -> IntTag.valueOf((int) v).equals(LongTag.valueOf(v)) + " " + IntTag.valueOf((int) v).equals(IntTag.valueOf((int) v)));
        }
        for (double v : DOUBLES) {
            String k = "number " + v;
            p(k + " float", () -> num(FloatTag.valueOf((float) v)) + " same " + (FloatTag.valueOf((float) v) == FloatTag.valueOf((float) v)));
            p(k + " double", () -> num(DoubleTag.valueOf(v)) + " same " + (DoubleTag.valueOf(v) == DoubleTag.valueOf(v)));
        }
        p("byte boolean", () -> ByteTag.valueOf(true) + " " + ByteTag.valueOf(false) + " " + (ByteTag.valueOf(true) == ByteTag.ONE) + " " + (ByteTag.ZERO == ByteTag.valueOf((byte) 0)));
        p("number copy", () -> (IntTag.valueOf(5).copy() == IntTag.valueOf(5)) + " " + (DoubleTag.valueOf(2.5).copy() == DoubleTag.valueOf(2.5)));
    }

    static String num(NumericTag t) {
        return t.getClass().getName() + " " + t + " " + t.getAsString() + " " + t.getId() + " " + t.hashCode() + " " + t.getAsLong() + " " + t.getAsInt()
                + " " + t.getAsShort() + " " + t.getAsByte() + " " + Double.doubleToLongBits(t.getAsDouble()) + " " + Float.floatToIntBits(t.getAsFloat())
                + " " + t.getAsNumber().getClass().getName() + ":" + t.getAsNumber();
    }

    static void texts() {
        for (String s : TEXTS) {
            String k = "text " + s.chars().mapToObj(Integer::toString).reduce("", (a, b) -> a + "." + b);
            StringTag t = StringTag.valueOf(s);
            p(k, () -> t + " | " + t.getAsString() + " | " + t.hashCode() + " " + t.getId() + " " + StringTag.quoteAndEscape(s)
                    + " " + t.equals(StringTag.valueOf(s)) + " " + (t.copy() == t) + " " + (StringTag.valueOf(s) == StringTag.valueOf(s)));
        }
    }

    static CompoundTag sample() {
        CompoundTag c = new CompoundTag();
        c.putByte("b", (byte) 3);
        c.putShort("s", (short) -300);
        c.putInt("i", 70000);
        c.putLong("l", 1L << 40);
        c.putFloat("f", 2.5f);
        c.putDouble("d", -1e10);
        c.putString("t", "text \"q\"");
        c.putBoolean("yes", true);
        c.putBoolean("no", false);
        c.putByteArray("ba", new byte[] {1, -2});
        c.putIntArray("ia", new int[] {3, -4, 5});
        c.putLongArray("la", new long[] {6, -7});
        CompoundTag inner = new CompoundTag();
        inner.putInt("x", 1);
        inner.putString("weird key", "v");
        c.put("c", inner);
        ListTag l = new ListTag();
        l.add(IntTag.valueOf(1));
        l.add(IntTag.valueOf(2));
        c.put("li", l);
        c.putUUID("u", new UUID(0x0123456789abcdefL, -0x0123456789abcdefL));
        return c;
    }

    static final String[] KEYS = {"b", "s", "i", "l", "f", "d", "t", "yes", "no", "ba", "ia", "la", "c", "li", "u", "missing"};

    static void compounds() {
        CompoundTag c = sample();
        p("compound text", c::toString);
        p("compound keys", () -> c.getAllKeys() + " " + c.size() + " " + c.isEmpty() + " " + c.getId() + " " + c.hashCode());
        for (String key : KEYS) {
            String k = "compound " + key;
            p(k + " get", () -> {
                Tag t = c.get(key);
                return t == null ? "null" : t.getClass().getName() + " " + t + " " + t.getAsString() + " " + c.getTagType(key);
            });
            p(k + " contains", () -> {
                StringBuilder s = new StringBuilder(c.contains(key) + "");
                for (int type = -1; type <= 13; type++) s.append(' ').append(c.contains(key, type));
                return s.append(' ').append(c.contains(key, 99)).append(' ').append(c.contains(key, 100));
            });
            p(k + " numbers", () -> c.getByte(key) + " " + c.getShort(key) + " " + c.getInt(key) + " " + c.getLong(key) + " " + c.getFloat(key) + " " + c.getDouble(key) + " " + c.getBoolean(key));
            p(k + " string", () -> "[" + c.getString(key) + "]");
            p(k + " arrays", () -> java.util.Arrays.toString(c.getByteArray(key)) + " " + java.util.Arrays.toString(c.getIntArray(key)) + " " + java.util.Arrays.toString(c.getLongArray(key)));
            p(k + " compound", () -> c.getCompound(key) + " " + (c.getCompound(key) == c.getCompound(key)) + " " + c.contains(key));
            p(k + " list", () -> {
                StringBuilder s = new StringBuilder();
                for (int type = 0; type <= 12; type++) s.append(c.getList(key, type)).append(' ');
                return s;
            });
            p(k + " uuid", () -> c.hasUUID(key) + " " + c.getUUID(key));
        }
        p("compound array shared", () -> (c.getIntArray("ia") == c.getIntArray("ia")) + " " + (((IntArrayTag) c.get("ia")).getAsIntArray() == c.getIntArray("ia")));
        p("compound copy", () -> {
            CompoundTag d = c.copy();
            return d.equals(c) + " " + (d.get("c") == c.get("c")) + " " + (d.get("i") == c.get("i")) + " " + (d.get("ia") == c.get("ia")) + " " + (d.hashCode() == c.hashCode());
        });
        p("compound put", () -> {
            CompoundTag d = new CompoundTag();
            Tag first = d.put("k", IntTag.valueOf(1));
            Tag second = d.put("k", StringTag.valueOf("x"));
            d.putInt("k2", 4);
            d.remove("k2");
            d.remove("absent");
            return first + " " + second + " " + d + " " + d.size();
        });
        p("compound put null", () -> {
            CompoundTag d = new CompoundTag();
            return d.put("k", null) + " " + d.size() + " " + d;
        });
        p("compound putString null", () -> {
            CompoundTag d = new CompoundTag();
            d.putString("k", null);
            return d.toString();
        });
        p("compound many", () -> {
            CompoundTag d = new CompoundTag();
            for (int i = 0; i < 40; i++) d.putInt("key" + (i * 7 % 40), i);
            return d.getAllKeys().toString() + " " + d;
        });
        p("compound quoting", () -> {
            CompoundTag d = new CompoundTag();
            for (String s : TEXTS) d.putString(s, s);
            return d.toString();
        });
        p("compound merge", () -> {
            CompoundTag a = sample();
            CompoundTag b = new CompoundTag();
            b.putInt("i", -1);
            CompoundTag bi = new CompoundTag();
            bi.putInt("y", 2);
            bi.putInt("x", 9);
            b.put("c", bi);
            b.put("li", new ListTag());
            CompoundTag r = a.merge(b);
            return (r == a) + " " + a + " " + (a.get("c") == bi) + " " + b;
        });
        p("compound equals", () -> {
            CompoundTag a = new CompoundTag(), b = new CompoundTag();
            a.putInt("x", 1);
            b.putLong("x", 1);
            CompoundTag e = new CompoundTag();
            e.putInt("x", 1);
            return a.equals(b) + " " + a.equals(e) + " " + a.equals(null) + " " + (a.hashCode() == e.hashCode()) + " " + new CompoundTag().hashCode();
        });
        p("compound empty", () -> new CompoundTag() + " " + new CompoundTag(5) + " " + new CompoundTag().getAllKeys());
        p("compound orders", () -> {
            CompoundTag a = new CompoundTag(), b = new CompoundTag(5);
            for (int i = 0; i < 40; i++) {
                a.putInt("k" + (i * 13 % 40), i);
                b.putInt("k" + (i * 13 % 40), i);
            }
            return a.getAllKeys() + " " + b.getAllKeys() + " " + a.copy().getAllKeys() + " " + (a.getAllKeys() == a.getAllKeys());
        });
        p("float equality", () -> FloatTag.valueOf(Float.NaN).equals(FloatTag.valueOf(Float.NaN)) + " " + DoubleTag.valueOf(Double.NaN).equals(DoubleTag.valueOf(Double.NaN))
                + " " + FloatTag.valueOf(0.5f).equals(FloatTag.valueOf(0.5f)) + " " + DoubleTag.valueOf(-0.0).equals(DoubleTag.valueOf(0.0)) + " " + FloatTag.valueOf(0.5f).equals(DoubleTag.valueOf(0.5)));
    }

    static void lists() {
        ListTag l = new ListTag();
        p("list empty", () -> l + " " + l.size() + " " + l.isEmpty() + " " + l.getElementType() + " " + l.getId() + " " + l.hashCode());
        p("list add", () -> {
            ListTag m = new ListTag();
            StringBuilder s = new StringBuilder();
            s.append(m.add(IntTag.valueOf(1))).append(' ').append(m.getElementType()).append(' ');
            try {
                s.append(m.add(StringTag.valueOf("x")));
            } catch (RuntimeException e) {
                s.append("throws ").append(e.getClass().getName()).append(": ").append(e.getMessage());
            }
            s.append(' ').append(m.addTag(1, StringTag.valueOf("y"))).append(' ').append(m.addTag(1, IntTag.valueOf(7)));
            s.append(' ').append(m.setTag(0, LongTag.valueOf(3))).append(' ').append(m.setTag(0, IntTag.valueOf(8)));
            s.append(' ').append(m).append(' ').append(m.size());
            return s;
        });
        ListTag c = new ListTag();
        for (int i = 0; i < 3; i++) {
            CompoundTag e = new CompoundTag();
            e.putInt("n", i);
            c.add(e);
        }
        ListTag s = new ListTag();
        s.add(StringTag.valueOf("one"));
        s.add(StringTag.valueOf("two \"2\""));
        ListTag d = new ListTag();
        d.add(DoubleTag.valueOf(1.5));
        for (int i = -1; i <= 3; i++) {
            int n = i;
            p("list compounds " + n, () -> c.getCompound(n) + " " + c.getInt(n) + " " + c.getString(n) + " " + c.getDouble(n) + " " + c.getList(n) + " " + (n >= 0 && n < 3 ? c.get(n) : "-"));
            p("list strings " + n, () -> s.getCompound(n) + " " + s.getInt(n) + " " + s.getString(n) + " " + s.getFloat(n) + " " + java.util.Arrays.toString(s.getIntArray(n)));
            p("list doubles " + n, () -> d.getDouble(n) + " " + d.getFloat(n) + " " + d.getString(n) + " " + d.getShort(n));
        }
        p("list texts", () -> c + " " + s + " " + d + " " + c.hashCode() + " " + s.hashCode() + " " + c.equals(c.copy()) + " " + (c.copy().get(0) == c.get(0)));
        p("list remove", () -> {
            ListTag m = c.copy();
            Tag r = m.remove(1);
            m.clear();
            return r + " " + m + " " + m.getElementType() + " " + m.add(StringTag.valueOf("now")) + " " + m;
        });
        p("list new", () -> new ListTag(4) + " " + new ListTag(4).size() + " " + new ListTag(4).getElementType() + " " + new ListTag(4).add(IntTag.valueOf(3)));
        p("list removed to empty", () -> {
            ListTag m = new ListTag();
            m.add(IntTag.valueOf(1));
            m.remove(0);
            return m.getElementType() + " " + m.add(StringTag.valueOf("s")) + " " + m;
        });
        p("list set", () -> {
            ListTag m = new ListTag();
            m.add(IntTag.valueOf(1));
            Tag old = m.set(0, IntTag.valueOf(2));
            try {
                m.set(0, StringTag.valueOf("s"));
            } catch (RuntimeException e) {
                return old + " " + m + " throws " + e.getClass().getName() + ": " + e.getMessage();
            }
            return old + " " + m;
        });
        p("list of arrays copy", () -> {
            ListTag m = new ListTag();
            m.add(new IntArrayTag(new int[] {1}));
            m.add(new IntArrayTag(new int[] {2}));
            ListTag dc = m.copy();
            return (dc.get(0) == m.get(0)) + " " + (((IntArrayTag) dc.get(0)).getAsIntArray() == ((IntArrayTag) m.get(0)).getAsIntArray()) + " " + dc;
        });
        p("list empty kinds", () -> {
            CompoundTag ce = new CompoundTag();
            ce.put("e", new ListTag());
            return ce.getList("e", 8) + " " + (ce.getList("e", 8) == ce.get("e")) + " " + (ce.getList("e", 0) == ce.get("e"));
        });
    }

    static void arrays() {
        IntArrayTag i = new IntArrayTag(new int[] {1, -2, Integer.MIN_VALUE});
        LongArrayTag l = new LongArrayTag(new long[] {1, -2, Long.MIN_VALUE});
        ByteArrayTag b = new ByteArrayTag(new byte[] {1, -2, -128});
        p("arrays text", () -> i + " " + l + " " + b + " " + i.getId() + " " + l.getId() + " " + b.getId());
        p("arrays hash", () -> i.hashCode() + " " + l.hashCode() + " " + b.hashCode() + " " + i.equals(new IntArrayTag(new int[] {1, -2, Integer.MIN_VALUE})));
        p("arrays elements", () -> i.size() + " " + i.get(1) + " " + i.getElementType() + " " + l.get(2) + " " + b.get(0) + " " + i.get(1).getClass().getName());
        p("arrays empty", () -> new IntArrayTag(new int[0]) + " " + new LongArrayTag(new long[0]) + " " + new ByteArrayTag(new byte[0]));
        p("arrays change", () -> {
            IntArrayTag t = new IntArrayTag(new int[] {1, 2, 3});
            StringBuilder b2 = new StringBuilder();
            b2.append(t.set(1, IntTag.valueOf(20))).append(' ');
            t.add(0, IntTag.valueOf(-5));
            b2.append(t).append(' ').append(t.setTag(0, LongTag.valueOf(1L << 40))).append(' ').append(t.setTag(1, StringTag.valueOf("x")));
            b2.append(' ').append(t.addTag(4, DoubleTag.valueOf(2.7))).append(' ').append(t.remove(2)).append(' ').append(t).append(' ').append(t.size());
            t.clear();
            return b2.append(' ').append(t);
        });
        p("arrays out of range", () -> {
            IntArrayTag t = new IntArrayTag(new int[] {1, 2});
            StringBuilder b2 = new StringBuilder();
            try {
                t.add(5, IntTag.valueOf(1));
            } catch (RuntimeException e) {
                b2.append(e.getClass().getName()).append(": ").append(e.getMessage()).append(' ');
            }
            try {
                t.get(2);
            } catch (RuntimeException e) {
                b2.append(e.getClass().getName()).append(": ").append(e.getMessage()).append(' ');
            }
            try {
                t.remove(-1);
            } catch (RuntimeException e) {
                b2.append(e.getClass().getName()).append(": ").append(e.getMessage());
            }
            return b2;
        });
        p("arrays long and byte change", () -> {
            LongArrayTag la = new LongArrayTag(new long[] {1, 2});
            ByteArrayTag ba = new ByteArrayTag(new byte[] {1, 2});
            la.add(1, LongTag.valueOf(9));
            ba.add(2, ByteTag.valueOf((byte) 9));
            return la + " " + la.setTag(0, IntTag.valueOf(4)) + " " + la + " " + ba + " " + ba.setTag(0, IntTag.valueOf(300)) + " " + ba + " " + la.remove(0) + " " + ba.remove(0) + " " + la.getElementType() + " " + ba.getElementType() + " " + la.getAsLongArray().length + " " + ba.getAsByteArray().length;
        });
        p("arrays copy", () -> {
            int[] raw = {5, 6};
            IntArrayTag t = new IntArrayTag(raw);
            raw[0] = 9;
            return t + " " + (t.getAsIntArray() == raw) + " " + (t.copy().getAsIntArray() == raw);
        });
    }

    static void positions() {
        for (int[] at : GameProbe.PLACES) {
            BlockPos pos = new BlockPos(at[0], at[1], at[2]);
            p("nbt position " + pos, () -> {
                Tag t = NbtUtils.writeBlockPos(pos);
                CompoundTag c = new CompoundTag();
                c.put("p", t);
                return t.getClass().getName() + " " + t + " " + NbtUtils.readBlockPos(c, "p") + " " + NbtUtils.readBlockPos(c, "q");
            });
        }
        p("nbt position wrong", () -> {
            CompoundTag c = new CompoundTag();
            c.putIntArray("two", new int[] {1, 2});
            c.putIntArray("four", new int[] {1, 2, 3, 4});
            c.putInt("int", 5);
            return NbtUtils.readBlockPos(c, "two") + " " + NbtUtils.readBlockPos(c, "four") + " " + NbtUtils.readBlockPos(c, "int");
        });
    }

    static void p(String key, java.util.function.Supplier<Object> call) {
        GameProbe.p(key, call);
    }
}
