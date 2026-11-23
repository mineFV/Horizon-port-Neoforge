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
        // Several elements at once are checked before any is added; one by one, those before a null are added.
        p("guava list adds null", () -> {
            ImmutableList.Builder<String> b = ImmutableList.builder();
            StringBuilder r = new StringBuilder();
            for (Runnable add : new Runnable[] {() -> b.add("a", null, "c"), () -> b.addAll(java.util.Arrays.asList("d", null, "f")),
                    () -> b.addAll(java.util.Arrays.asList("g", null).iterator()), () -> b.addAll((Iterable<String>) null)}) {
                try {
                    add.run();
                    r.append("added; ");
                } catch (NullPointerException e) {
                    r.append("NullPointerException ").append(e.getMessage() != null && e.getMessage().startsWith("Cannot ") ? "" : e.getMessage()).append("; ");
                }
            }
            return r.append(b.build());
        });
        tables();
    }

    /** Guava's HashBasedTable: cells put, replaced and removed, asked of with null, its views, texts and equality. */
    static void tables() {
        p("guava table", () -> {
            com.google.common.collect.Table<String, Integer, String> t = com.google.common.collect.HashBasedTable.create();
            StringBuilder b = new StringBuilder();
            b.append(t).append(' ').append(t.isEmpty()).append(' ').append(t.size()).append(' ').append(t.getClass().getName()).append(" | ");
            b.append(t.put("b", 2, "x")).append(' ').append(t.put("a", 1, "y")).append(' ').append(t.put("b", 1, "z")).append(' ').append(t.put("b", 2, "w")).append(' ');
            b.append(t.put("c", 3, "v")).append(" | ").append(t).append(' ').append(t.size()).append(' ').append(t.isEmpty()).append(" | ");
            b.append(t.get("b", 2)).append(' ').append(t.get("b", 9)).append(' ').append(t.get("q", 1)).append(' ').append(t.get(null, 1)).append(' ').append(t.get("a", null)).append(" | ");
            b.append(t.contains("a", 1)).append(t.contains("a", 2)).append(t.contains(null, 1)).append(' ').append(t.containsRow("c")).append(t.containsRow("q")).append(t.containsRow(null))
                    .append(' ').append(t.containsColumn(1)).append(t.containsColumn(7)).append(t.containsColumn(null)).append(' ').append(t.containsValue("z")).append(t.containsValue("x"))
                    .append(t.containsValue(null)).append(" | ");
            b.append(t.cellSet()).append(' ').append(t.values()).append(' ').append(t.cellSet() == t.cellSet()).append(t.values() == t.values()).append(' ')
                    .append(t.cellSet().size()).append(' ').append(t.values().size()).append(' ').append(t.cellSet().iterator().next().getClass().getName()).append(" | ");
            com.google.common.collect.Table.Cell<String, Integer, String> first = t.cellSet().iterator().next();
            b.append(first.getRowKey()).append(first.getColumnKey()).append(first.getValue()).append(' ').append(first.hashCode()).append(' ')
                    .append(first.equals(com.google.common.collect.Tables.immutableCell("b", 2, "w"))).append(first.equals(com.google.common.collect.Tables.immutableCell("b", 2, "x")))
                    .append(' ').append(t.cellSet().contains(first)).append(t.cellSet().contains(com.google.common.collect.Tables.immutableCell("b", 2, "x"))).append(t.cellSet().contains("b"))
                    .append(' ').append(t.values().contains("v")).append(t.values().contains("x")).append(" | ");
            b.append(t.hashCode()).append(' ').append(t.equals(t)).append(' ');
            com.google.common.collect.Table<String, Integer, String> u = com.google.common.collect.HashBasedTable.create();
            u.put("c", 3, "v");
            u.put("a", 1, "y");
            u.put("b", 2, "w");
            u.put("b", 1, "z");
            b.append(t.equals(u)).append(u.equals(t)).append(' ').append(t.hashCode() == u.hashCode()).append(' ').append(u).append(' ').append(t.equals("t")).append(" | ");
            b.append(t.remove("a", 1)).append(' ').append(t.remove("a", 1)).append(' ').append(t.remove(null, 1)).append(' ').append(t.remove("b", null)).append(' ').append(t.remove("b", 7))
                    .append(' ').append(t).append(' ').append(t.containsRow("a")).append(' ').append(t.size()).append(" | ");
            java.util.Iterator<String> values = t.values().iterator();
            values.next();
            values.remove();
            b.append(t).append(' ');
            java.util.Iterator<com.google.common.collect.Table.Cell<String, Integer, String>> cells = t.cellSet().iterator();
            while (cells.hasNext()) {
                if (cells.next().getRowKey().equals("b")) cells.remove();
            }
            b.append(t).append(' ').append(t.containsRow("b")).append(' ').append(t.size()).append(" | ");
            t.values().clear();
            b.append(t).append(t.isEmpty());
            t.put("n", 0, "m");
            t.clear();
            b.append(' ').append(t).append(' ').append(t.size());
            return b;
        });
        p("guava table nulls", () -> {
            com.google.common.collect.Table<String, String, String> t = com.google.common.collect.HashBasedTable.create();
            StringBuilder b = new StringBuilder();
            for (String[] args : new String[][] {{null, "c", "v"}, {"r", null, "v"}, {"r", "c", null}}) {
                try {
                    b.append(t.put(args[0], args[1], args[2]));
                } catch (NullPointerException e) {
                    b.append("NPE");
                }
                b.append(' ');
            }
            return b.append(t.size());
        });
        p("guava cells", () -> {
            com.google.common.collect.Table.Cell<String, String, String> c = com.google.common.collect.Tables.immutableCell("r", null, "v");
            return c + " " + c.hashCode() + " " + c.equals(com.google.common.collect.Tables.immutableCell("r", null, "v")) + " " + c.getClass().getName();
        });
    }

    static void p(String key, java.util.function.Supplier<Object> call) {
        GameProbe.p(key, call);
    }
}
