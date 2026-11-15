# The translator's model of Java's library

What `translate` puts in place of the classes of Java's library that a mod uses: written here, from the Java SE
API specification, never copied from a JDK. It is compiled by `build.sh` (`javac --patch-module java.base=lib`)
and translated to Rust with the mod, by the same translator; a method marked `native` is written in Rust by
`Library.java` instead. A member the model lacks is a member the translator refuses, with why.

Messages of the exceptions the model throws follow the JDK the tests run on (`test/translate/`), which compare
them with the real thing. Two are known to differ, and say so in the module: a NullPointerException has no
message (the JDK builds one from the bytecode), nor has a ClassCastException (the JDK's names class loaders).

Collections are written from how the JDK's behave, where a program can see it: a HashMap (and HashSet,
LinkedHashMap's table) iterates in the JDK's order, from the same table sizes, hash spreading, bins, growth and
splits, and computeIfAbsent, compute and merge put a new key at the start of its bin as the JDK's do. What differs,
and why:

- a bin of nine keys or more in a table of 64 or more is a red-black tree in the JDK, which iterates its root
  first; the model keeps it a list. The JDK's tree orders keys of no common Comparable class by their class names
  and identity hashes, which the model has no Class objects for, and which change from run to run anyway;
- Set.of, Set.copyOf, Map.of, Map.ofEntries and Map.copyOf iterate in the order their elements were given: the
  JDK's order is salted at random when the JVM starts, so no program can count on it;
- sorting (Arrays.sort, List.sort, Collections.sort) gives the JDK's result, as any stable sort does, but asks the
  comparator about other pairs: a comparator that counts its calls, or isn't consistent, can tell;
- a sublist of List.of is a copy (the JDK's is a view; neither can change), and the views Collections'
  unmodifiable maps give of their entries are new objects each time they are iterated.

Not modelled: Class objects (EnumMap, EnumSet, toArray's runtime types beyond copying an array's own class), streams,
spliterators, list iterators, reversed views, TreeMap, ArrayDeque, concurrent collections.

