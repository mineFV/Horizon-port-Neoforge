package com.example.numbers;

import it.unimi.dsi.fastutil.ints.IntArrayList;
import it.unimi.dsi.fastutil.ints.IntList;
import it.unimi.dsi.fastutil.ints.IntListIterator;
import it.unimi.dsi.fastutil.objects.Reference2ObjectOpenHashMap;
import java.util.List;
import java.util.Map;

/**
 * The game's libraries as a mod uses them: fastutil's list of ints (kept, then gone through, as a mod's transfers do)
 * and its map by identity.
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
}
