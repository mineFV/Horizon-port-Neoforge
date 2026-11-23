package horizonneo.probe;

import it.unimi.dsi.fastutil.ints.IntArrayList;
import it.unimi.dsi.fastutil.ints.IntArrays;
import it.unimi.dsi.fastutil.ints.IntIterator;
import it.unimi.dsi.fastutil.ints.IntList;
import it.unimi.dsi.fastutil.ints.IntListIterator;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.function.Supplier;

/**
 * fastutil's IntArrayList: its array's growth (elements().length), errors and their texts, boxed methods, iterators,
 * spliterator, sort, equality, order and texts. A part of it as a view isn't asked: the stand-in refuses it.
 */
final class IntListProbe {
    private IntListProbe() {}

    static void p(String key, Supplier<Object> call) {
        GameProbe.p(key, call);
    }

    /** What running call gives, or the class and message of what it throws. */
    static String t(Supplier<Object> call) {
        try {
            Object o = call.get();
            return o instanceof int[] a ? java.util.Arrays.toString(a) : String.valueOf(o);
        } catch (NullPointerException e) {
            // Its message is built by the JVM from the bytecode (local variables' names): not compared.
            return e.getClass().getName();
        } catch (RuntimeException e) {
            return e.getClass().getName() + ": " + e.getMessage();
        }
    }

    static String push(IntArrayList l, int k) {
        l.push(k);
        return l.toString();
    }

    static String grown(IntArrayList l) {
        return l + "/" + l.elements().length;
    }

    static void run() {
        p("int list made", () -> grown(new IntArrayList()) + " " + grown(new IntArrayList(0)) + " " + grown(new IntArrayList(7)) + " " + t(() -> new IntArrayList(-3))
                + " " + grown(new IntArrayList(List.of(4, 5, 6))) + " " + grown(new IntArrayList(new int[] {9, 8, 7, 6})) + " " + grown(new IntArrayList(new int[] {9, 8, 7, 6}, 1, 2))
                + " " + grown(new IntArrayList(new IntArrayList(new int[] {1, 2}))) + " " + grown(new IntArrayList(new ArrayList<Integer>())) + " " + grown(IntArrayList.of())
                + " " + grown(IntArrayList.of(3, 1, 2)) + " " + grown(IntArrayList.wrap(new int[] {5, 6, 7}, 2)) + " " + t(() -> IntArrayList.wrap(new int[] {5}, 2))
                + " " + t(() -> new IntArrayList(new int[] {1}, 0, 3)));
        p("int list empty arrays", () -> (new IntArrayList().elements() == IntArrays.DEFAULT_EMPTY_ARRAY) + " " + (new IntArrayList(0).elements() == IntArrays.EMPTY_ARRAY) + " "
                + (new IntArrayList().toIntArray() == IntArrays.EMPTY_ARRAY) + " " + (new IntArrayList(List.of()).elements() == IntArrays.EMPTY_ARRAY) + " "
                + (IntArrayList.of().elements() == IntArrays.DEFAULT_EMPTY_ARRAY) + " " + (new IntArrayList().clone().elements() == IntArrays.EMPTY_ARRAY));
        for (int start : new int[] {-1, 0, 1, 3, 10, 11}) {
            p("int list grows " + start, () -> {
                IntArrayList l = start < 0 ? new IntArrayList() : new IntArrayList(start);
                StringBuilder b = new StringBuilder();
                for (int i = 0; i < 40; i++) {
                    l.add(i * 3);
                    b.append(l.elements().length).append(',');
                }
                return b;
            });
            p("int list capacity " + start, () -> {
                IntArrayList l = start < 0 ? new IntArrayList() : new IntArrayList(start);
                StringBuilder b = new StringBuilder();
                l.ensureCapacity(5);
                b.append(l.elements().length).append(',');
                l.ensureCapacity(12);
                b.append(l.elements().length).append(',');
                l.add(1);
                l.add(2);
                l.trim(1);
                b.append(l.elements().length).append(',');
                l.trim();
                b.append(l.elements().length).append(',');
                l.size(6);
                b.append(grown(l)).append(',');
                l.size(1);
                b.append(grown(l)).append(',');
                l.size(30);
                b.append(grown(l)).append(',');
                l.add(0, 77);
                l.add(31, 88);
                b.append(grown(l));
                return b;
            });
        }
        p("int list index errors", () -> {
            IntArrayList l = IntArrayList.of(1, 2, 3);
            return t(() -> l.getInt(3)) + " | " + t(() -> l.getInt(-1)) + " | " + t(() -> l.set(3, 0)) + " | " + t(() -> l.set(-1, 0)) + " | " + t(() -> l.removeInt(3)) + " | "
                    + t(() -> l.removeInt(-2)) + " | " + t(() -> {
                        l.add(4, 0);
                        return l;
                    }) + " | " + t(() -> {
                        l.add(-1, 0);
                        return l;
                    }) + " | " + t(() -> l.listIterator(4)) + " | " + t(() -> l.listIterator(-1)) + " | " + t(() -> l.get(5)) + " | " + t(() -> l.remove(7)) + " | "
                    + t(() -> l.subList(2, 1)) + " | " + t(() -> l.subList(-1, 2)) + " | " + t(() -> l.subList(0, 4)) + " | " + (l.subList(0, 3) == l) + " | "
                    + t(() -> l.addAll(5, List.of(1))) + " | " + t(() -> l.addAll(-1, new IntArrayList())) + " | " + t(() -> new IntArrayList().popInt()) + " | "
                    + t(() -> new IntArrayList().topInt()) + " | " + t(() -> l.peekInt(3)) + " | " + t(() -> new IntArrayList().getFirst()) + " | " + l;
        });
        p("int list boxed", () -> {
            IntArrayList l = IntArrayList.of(5, -2, 7, 5, 0);
            StringBuilder b = new StringBuilder();
            b.append(l.get(0)).append(' ').append(l.set(1, Integer.valueOf(9))).append(' ').append(l.add(Integer.valueOf(4))).append(' ').append(l.remove(0)).append(' ').append(l);
            l.add(1, Integer.valueOf(6));
            b.append(' ').append(l).append(' ').append(l.remove((Object) 5)).append(l.remove((Object) 42)).append(l.remove((Object) null)).append(' ').append(t(() -> l.remove((Object) "x")));
            b.append(' ').append(l.contains(7)).append(l.contains((Object) 7)).append(l.contains((Object) null)).append(l.contains((Object) 8)).append(' ').append(t(() -> l.contains((Object) "x")));
            b.append(' ').append(l.indexOf((Object) 7)).append(l.lastIndexOf((Object) 4)).append(' ').append(t(() -> l.indexOf((Object) null))).append(' ').append(t(() -> l.indexOf((Object) "x")));
            b.append(' ').append(t(() -> l.add((Integer) null))).append(' ').append(l.getFirst()).append(l.getLast()).append(' ').append(l.removeLast()).append(' ').append(l);
            l.addFirst(11);
            b.append(' ').append(l).append(' ').append(java.util.Arrays.toString(l.toArray())).append(' ').append(java.util.Arrays.toString(l.toArray(new Integer[0])));
            b.append(' ').append(java.util.Arrays.toString(l.toArray(new Integer[] {0, 0, 0, 0, 0, 0, 0, 0}))).append(' ').append(push(l, 3)).append(l.popInt()).append(l.topInt()).append(l.peekInt(1));
            b.append(' ').append(l.pop()).append(l.top()).append(l.peek(0)).append(' ').append(l);
            return b;
        });
        p("int list iterator", () -> {
            IntArrayList l = new IntArrayList();
            for (int i = 0; i < 12; i++) l.add(i * i - 20);
            StringBuilder b = new StringBuilder();
            IntListIterator it = l.listIterator(3);
            b.append(it.nextIndex()).append(it.previousIndex()).append(it.previousInt()).append(it.nextInt()).append(it.nextInt()).append(' ');
            it.remove();
            b.append(t(() -> {
                it.remove();
                return "no";
            })).append(' ');
            it.add(100);
            b.append(t(() -> {
                it.set(1);
                return "no";
            })).append(' ').append(it.nextInt()).append(' ');
            it.set(-5);
            b.append(it.skip(3)).append(it.back(2)).append(' ').append(it.nextIndex()).append(' ');
            it.remove();
            b.append(l).append(' ').append(it.skip(100)).append(it.hasNext()).append(' ').append(t(() -> it.nextInt())).append(' ').append(it.back(100)).append(it.hasPrevious());
            b.append(' ').append(t(() -> it.previousInt())).append(' ').append(t(() -> it.skip(-1))).append(' ').append(t(() -> it.back(-1)));
            IntListIterator e = l.listIterator(2);
            StringBuilder rest = new StringBuilder();
            e.forEachRemaining((int x) -> rest.append(x).append(','));
            e.remove();
            IntListIterator f = l.iterator();
            f.nextInt();
            f.forEachRemaining((Integer x) -> rest.append('<').append(x).append('>'));
            b.append(' ').append(rest).append(' ').append(l).append(' ').append(f.hasNext());
            IntIterator g = l.iterator();
            b.append(' ').append(g.skip(2)).append(g.next()).append(' ').append(t(() -> g.skip(-1)));
            IntListIterator k = l.listIterator(1);
            b.append(' ').append(k.back(5)).append(k.nextIndex()).append(k.hasPrevious()).append(' ').append(t(() -> k.nextInt())).append(' ').append(t(() -> k.previousInt()));
            IntListIterator m = l.listIterator(4);
            b.append(' ').append(m.back(1)).append(m.nextIndex()).append(m.back(3)).append(m.nextIndex()).append(m.nextInt());
            java.util.Iterator<Integer> h = l.iterator();
            while (h.hasNext()) if (h.next() % 2 == 0) h.remove();
            b.append(' ').append(l);
            return b;
        });
        p("int list bulk", () -> {
            IntArrayList l = IntArrayList.of(1, 2, 3, 4, 5, 6);
            StringBuilder b = new StringBuilder();
            b.append(l.addAll(List.of(7, 8))).append(l.addAll(new IntArrayList())).append(l.addAll(new ArrayList<Integer>())).append(' ').append(grown(l));
            b.append(' ').append(l.addAll(2, IntArrayList.of(-1, -2))).append(' ').append(grown(l)).append(' ').append(l.addAll(0, List.of(9))).append(' ').append(l);
            b.append(' ').append(l.removeAll(List.of(1, 9, 100))).append(l.removeAll(IntArrayList.of(2, 2))).append(l.removeAll(List.of())).append(' ').append(l);
            b.append(' ').append(l.retainAll(List.of(-1, 3, 4, 5, 6, 7, 8))).append(l.retainAll(IntArrayList.of(3, 4, 5, 6, 7, 8, 9))).append(' ').append(l);
            b.append(' ').append(l.containsAll(List.of(3, 4))).append(l.containsAll(IntArrayList.of(3, 10))).append(l.containsAll(List.of())).append(' ').append(t(() -> l.containsAll(List.of("x"))));
            b.append(' ').append(l.removeIf((Integer x) -> x == 4)).append(l.removeIf((java.util.function.IntPredicate) x -> x > 7)).append(l.removeIf((Integer x) -> false)).append(' ').append(l);
            IntArrayList self = IntArrayList.of(1, 2, 3);
            self.addAll(1, self);
            IntArrayList self2 = IntArrayList.of(1, 2, 3);
            self2.addAll(self2);
            IntArrayList self3 = IntArrayList.of(1, 2, 2, 3);
            b.append(' ').append(self).append(self2).append(self3.removeAll(self3)).append(self3);
            IntArrayList boxedSelf = IntArrayList.of(4, 5);
            boxedSelf.addAll(1, (java.util.Collection<Integer>) new ArrayList<>(boxedSelf));
            b.append(' ').append(boxedSelf).append(' ').append(java.util.Arrays.toString(l.toIntArray())).append(' ').append(t(() -> l.toArray((int[]) null)));
            b.append(' ').append(java.util.Arrays.toString(l.toArray(new int[1]))).append(' ').append(java.util.Arrays.toString(l.toArray(new int[] {9, 9, 9, 9, 9, 9, 9})));
            l.clear();
            b.append(' ').append(grown(l)).append(l.isEmpty()).append(l.size());
            return b;
        });
        p("int list texts", () -> {
            IntArrayList a = IntArrayList.of(3, -1, 4), c = IntArrayList.of(3, -1, 4, 1), d = IntArrayList.of(3, 0);
            List<Integer> boxed = List.of(3, -1, 4);
            return a + " " + new IntArrayList() + " " + a.hashCode() + " " + boxed.hashCode() + " " + new IntArrayList().hashCode() + " " + a.equals(boxed) + boxed.equals(a) + a.equals(IntArrayList.of(3, -1, 4))
                    + a.equals(c) + a.equals(new ArrayList<>(boxed)) + a.equals(List.of(3L, -1L, 4L)) + a.equals("x") + a.equals((Object) null) + a.equals(java.util.Set.of(3)) + " " + t(() -> a.equals((IntArrayList) null)) + " " + a.compareTo(c) + c.compareTo(a)
                    + a.compareTo(d) + d.compareTo(a) + a.compareTo(a) + a.compareTo(boxed) + a.compareTo(List.of(3, -1)) + a.compareTo(List.of(3, 0, 0)) + new IntArrayList().compareTo(List.of())
                    + " " + t(() -> a.compareTo((List<Integer>) (List<?>) List.of("x"))) + " " + a.clone() + " " + (a.clone() != a) + " " + a.clone().elements().length + " " + c.clone().getClass().getName();
        });
        p("int list streams", () -> {
            IntArrayList l = IntArrayList.of(5, 3, 8, 1, 9, 2, 7);
            var s = l.spliterator();
            StringBuilder b = new StringBuilder();
            b.append(s.characteristics()).append(' ').append(s.estimateSize()).append(' ').append(s.getExactSizeIfKnown()).append(' ').append(s.hasCharacteristics(java.util.Spliterator.NONNULL));
            var half = s.trySplit();
            b.append(' ').append(half.estimateSize()).append(s.estimateSize());
            StringBuilder seen = new StringBuilder();
            half.forEachRemaining((int x) -> seen.append(x));
            seen.append('|');
            s.tryAdvance((Integer x) -> seen.append(x));
            s.forEachRemaining((Integer x) -> seen.append(x));
            b.append(' ').append(seen).append(' ').append(half.trySplit()).append(' ').append(t(() -> s.getComparator()));
            var late = l.spliterator();
            l.add(4);
            b.append(' ').append(late.estimateSize()).append(' ').append(late.skip(3)).append(late.estimateSize()).append(' ').append(t(() -> late.skip(-1)));
            var small = IntArrayList.of(1, 2, 3).spliterator();
            b.append(' ').append(small.trySplit()).append(' ').append(IntArrayList.of(1, 2, 3, 4).spliterator().trySplit().estimateSize());
            b.append(' ').append(l.stream().map(x -> x * 2).toList()).append(' ').append(l.stream().count()).append(' ').append(l.stream().filter(x -> x > 4).sorted().toList());
            StringBuilder each = new StringBuilder();
            l.forEach((Integer x) -> each.append(x));
            l.forEach((int x) -> each.append(-x));
            new IntArrayList().forEach((java.util.function.IntConsumer) null);
            b.append(' ').append(each).append(' ').append(t(() -> {
                new IntArrayList().forEach((java.util.function.Consumer<Integer>) null);
                return "no";
            }));
            return b;
        });
        p("int list sort", () -> {
            IntArrayList l = IntArrayList.of(5, -3, 8, -1, 9, 3, -5, 1, 0, 1, -9);
            StringBuilder b = new StringBuilder();
            IntArrayList a = l.clone();
            a.sort(null);
            IntArrayList r = l.clone();
            r.sort(Comparator.reverseOrder());
            IntArrayList abs = l.clone();
            abs.sort(Comparator.comparingInt((Integer x) -> Math.abs(x)));
            IntArrayList big = new IntArrayList();
            for (int i = 0; i < 300; i++) big.add((i * 7919) % 101 - 50);
            big.sort(Comparator.comparingInt((Integer x) -> x / 10));
            List<Integer> boxed = new ArrayList<>(l);
            boxed.sort(Comparator.comparingInt((Integer x) -> Math.abs(x)));
            return a + " " + r + " " + abs + " " + boxed + " " + big;
        });
    }
}
