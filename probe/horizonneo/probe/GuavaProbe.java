package horizonneo.probe;

import com.google.common.collect.ImmutableList;

/** Guava's immutable lists, as a mod builds them: elements, null refused, changes refused, equality, hashes, texts, errors. */
final class GuavaProbe {
    private GuavaProbe() {}

    static void run() {
        int[] sizes = {0, 1, 2, 5, 9};
        for (int n : sizes) {
            p("guava list " + n, () -> {
                ImmutableList.Builder<String> b = ImmutableList.builder();
                for (int i = 0; i < n; i++) b.add("e" + (i % 3));
                ImmutableList<String> l = b.build();
                java.util.List<String> same = new java.util.ArrayList<>(l);
                StringBuilder r = new StringBuilder().append(l).append(' ').append(l.size()).append(' ').append(l.hashCode()).append(' ').append(l.equals(same)).append(' ')
                        .append(same.equals(l)).append(' ').append(l.indexOf("e1")).append(' ').append(l.lastIndexOf("e1")).append(' ').append(l.contains("e2")).append(' ')
                        .append(l.contains(null)).append(' ').append(l.indexOf(null)).append(' ').append(l.isEmpty()).append(' ').append(java.util.Arrays.toString(l.toArray()))
                        .append(' ').append(l.asList() == l).append(' ').append(l.equals(java.util.Set.copyOf(same)));
                for (java.util.function.Supplier<Object> change : java.util.List.<java.util.function.Supplier<Object>>of(() -> l.add("x"), () -> l.remove("e0"), () -> {
                    l.clear();
                    return null;
                }, () -> l.set(0, "y"), () -> l.remove(0), () -> l.get(n), () -> l.get(-1), () -> l.subList(0, n + 1), () -> l.subList(1, 0), () -> l.subList(-1, 0), () -> {
                    java.util.Iterator<String> it = l.iterator();
                    if (it.hasNext()) it.next();
                    it.remove();
                    return null;
                }, () -> l.subList(0, Math.min(2, n)), () -> l.listIterator(n + 1))) {
                    try {
                        r.append(" | ").append(change.get());
                    } catch (RuntimeException e) {
                        r.append(" | ").append(e.getClass().getSimpleName()).append(' ').append(e.getMessage());
                    }
                }
                StringBuilder walk = new StringBuilder();
                for (String s : l) walk.append(s);
                return r.append(" | ").append(walk);
            });
        }
        p("guava list null", () -> {
            try {
                return ImmutableList.<String>builder().add("a").add((String) null).build();
            } catch (NullPointerException e) {
                return "NullPointerException " + e.getMessage();
            }
        });
        p("guava list adds", () -> ImmutableList.<String>builder().add("a", "b").addAll(java.util.List.of("c", "d")).addAll(java.util.List.of("e").iterator()).build());
    }

    static void p(String key, java.util.function.Supplier<Object> call) {
        GameProbe.p(key, call);
    }
}
