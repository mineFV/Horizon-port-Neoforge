package com.example.numbers;

import it.unimi.dsi.fastutil.ints.IntArrayList;
import it.unimi.dsi.fastutil.ints.IntList;
import it.unimi.dsi.fastutil.ints.IntListIterator;
import it.unimi.dsi.fastutil.objects.Reference2ObjectOpenHashMap;
import java.util.List;
import java.util.Map;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.Vec3;

/**
 * The game's libraries and arithmetic as a mod uses them: fastutil's list of ints (kept, then gone through, as a mod's
 * transfers do), its map by identity, the game's sine table and a point turned about each axis.
 */
public final class Turns {
    private Turns() {}

    /** Slots kept as ints, gone through (the even ones dropped on the way), then used as a List of Integers. */
    public static String slots(int n) {
        IntList l = new IntArrayList();
        for (int i = 0; i < n; i++) if (i % 3 != 1) l.add(i * i - 7);
        StringBuilder b = new StringBuilder();
        for (IntListIterator it = l.iterator(); it.hasNext();) {
            int s = it.nextInt();
            b.append(s).append(',');
            if (s % 2 == 0) it.remove();
        }
        List<Integer> boxed = l;
        boxed.add(0, 99);
        boxed.sort(null);
        int sum = 0;
        for (Integer x : boxed) sum += x;
        long big = boxed.stream().filter(x -> x > 20).count();
        return b + " " + l + " " + l.size() + " " + l.hashCode() + " " + sum + " " + big + " " + boxed.contains(99) + " " + boxed.indexOf(99) + " " + l.equals(List.copyOf(boxed));
    }

    /** A map by identity: equal texts are other keys (only what the identity hashes don't decide is said). */
    public static String identity(int n) {
        Reference2ObjectOpenHashMap<Object, String> m = new Reference2ObjectOpenHashMap<>();
        Object[] keys = new Object[n];
        for (int i = 0; i < n; i++) {
            keys[i] = new String("k" + i % 3);
            m.put(keys[i], "v" + i);
        }
        Map<Object, String> asMap = m;
        int found = 0;
        for (int i = 0; i < n; i++) if (asMap.get(keys[i]) != null) found++;
        String removed = n > 0 ? m.remove(keys[0]) : "-";
        return m.size() + " " + found + " " + asMap.get("k1") + " " + m.containsKey(new String("k0")) + " " + removed + " " + m.size() + " " + asMap.containsValue("v1");
    }

    /** The game's sine and cosine at the middle of each entry of the table from one to another, as a hash. */
    public static int sines(int from, int to) {
        int h = 1;
        for (int i = from; i < to; i++) {
            float f = (float) ((i + 0.5) / 10430.378350470453);
            h = 31 * h + Float.floatToRawIntBits(Mth.sin(f));
            h = 31 * h + Float.floatToRawIntBits(Mth.cos(f));
        }
        return h;
    }

    /** A point turned about each axis by the angle. */
    public static String turned(double x, double y, double z, float angle) {
        Vec3 v = new Vec3(x, y, z);
        return v.xRot(angle) + " " + v.yRot(angle) + " " + v.zRot(angle);
    }
}
