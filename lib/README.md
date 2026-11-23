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
  and identity hashes, which change from run to run anyway;
- Set.of, Set.copyOf, Map.of, Map.ofEntries and Map.copyOf iterate in the order their elements were given: the
  JDK's order is salted at random when the JVM starts, so no program can count on it;
- sorting (Arrays.sort, List.sort, Collections.sort) gives the JDK's result, as any stable sort does, but asks the
  comparator about other pairs: a comparator that counts its calls, or isn't consistent, can tell;
- a sublist of List.of is a copy (the JDK's is a view; neither can change), and the views Collections'
  unmodifiable maps give of their entries are new objects each time they are iterated.

Streams run as the JDK runs a sequential one, where a program can see it: each element goes through every stage
before the next comes in, sorted takes them all in first, a short-circuiting operation (anyMatch, findFirst...)
stops asking for more; count and toArray of a stream whose size is known without running its stages don't run
them (a peek isn't called) or ask the generator for the array first; a collection is read when the terminal
operation starts, and ArrayList's and HashMap's spliterators check modCount where the JDK's do (after each
element for tryAdvance, after all for forEachRemaining). What differs, and why:

- a parallel stream runs one element after another: one of the runs a parallel stream allows;
- the spliterators don't split (trySplit gives null, which the API allows), and an IntStream's or LongStream's
  iterator and spliterator are of boxes (PrimitiveIterator and Spliterator.OfInt aren't modelled);
- a stream's stages have classes of the model's (getClass names them differently);
- Collectors.toUnmodifiableSet keeps Set.of's order (see above).

Elsewhere, where the model makes one of the runs the JDK allows rather than the JDK's own: a WeakReference (soft,
phantom) is never cleared by the collector (its object stays alive as long as it does), and nothing is ever put in
a ReferenceQueue; randomUUID's bits come from Rust's random keys (the JDK's from a SecureRandom); nanoTime counts
from the first time it is asked. The atomics hold the object's lock through each read-modify-write.

An ArrayDeque is laid out as the JDK's (17 slots at first, its growth, removal moving the shorter side), since its
iterators show it: they visit as many elements as there were when they started, and a null slot is a
ConcurrentModificationException.

String.getBytes encodes into UTF-8 (the default charset, as since Java 18), ISO-8859-1 and US-ASCII with the JDK's
replacements ('?' for a surrogate without its other half, and for a character the charset lacks); decoding isn't
modelled. UUID.nameUUIDFromBytes's MD5 is written from RFC 1321.

A seeded Random gives the JDK's numbers (its generator is the one its specification gives), and Collections.shuffle
the JDK's order with it. An IdentityHashMap iterates in an order of its own: the JDK's comes from identity hashes,
which change from run to run.

NumberFormat (2026-11-23, the maintainer's decision: the system's locale, as a NeoForge server on the same machine
formats): a locale's integer and number instances (DecimalFormat), their texts of a long and of a double, and their
getters, equals, hashCode and clone, observed against the JDK. Their data (affixes, grouping, fraction digits,
symbols) is the JDK's own for each locale (`tools/NumberFormats.java`, which build.sh runs: `numbers.rs`), checked
by formatting numbers both ways; a language with a country the JDK has no locale for has its language's, as it has.
Where Java 21 (NeoForge 1.21.1's) and Java 25 give other texts (20 languages whole, such as Arabic, and 12 keys,
en_FR among them: observed over every locale and every language with every ISO country), the model refuses.
A double's text is Double.toString's digits rounded half-even to the fraction digits, a 5 that is their last digit
rounded by the double's exact value (observed: 6 million values, Java 21 and 25 alike); -0 and what rounds to zero
below it keep their sign. From 1e16 up the JDK's digits aren't Double.toString's (it keeps an older algorithm's):
refused. toString is Object's, as Java 21's is (Java 25 names the locale).
The default locale is the JDK's FORMAT one, as it makes it from the environment on Linux (observed with locales
compiled by localedef under LOCPATH): every category glibc's setlocale(LC_ALL, "") sets (LC_ALL, else LC_*, else
LANG, else "C") must name an installed locale (in glibc's archive, by its name with the codeset normalized, or a
folder of /usr/lib/locale), or all are "C"; the format locale is LC_CTYPE's, each of its parts LC_MESSAGES's where
it has none (C.UTF-8 under LANG=de_DE gives en_DE). "C" and "POSIX" are en_US, C.UTF-8 English alone; @latin is the
Latin script, @euro and @valencia nothing; iw, ji and in are he, yi and id. Refused, saying why, when it can't be
told: another system than Linux, LOCPATH set, an alias, a path, another modifier, a locale whose less precise name
alone is installed (glibc would check its codeset), C.UTF-8 not installed (glibc may have it built in).
Not modelled: parsing, the setters, the currency and percent instances, the symbols' object, format(Object).

A Path (2026-11-23) is the JDK's on Linux and macOS, sun.nio.fs.UnixPath, observed: its text with each run of
slashes one and no slash at the end but the root's, its names, parent, root and file name by the slashes (the empty
path one name, the root none), normalize, resolve, toAbsolutePath against the current directory, InvalidPathException
for a NUL, its order byte by byte and its hash over its bytes. Those bytes are its text in the system's charset
(LC_CTYPE's codeset, as glibc loads it): a text that isn't ASCII is followed where that charset is UTF-8, refused
where it isn't or can't be told, as another system than a Unix one is. Files: isDirectory, and createDirectories,
refused where the system refuses (the JDK's exceptions aren't made). Not modelled: the rest of Files, the file system,
subpath, startsWith, endsWith, relativize, URIs, File.

A TreeMap (and TreeSet) is CLR's red-black tree, as the JDK's: the same keys put and removed in the same order make
the same tree, and the comparator is asked about the same pairs (the test counts them). A TreeMap made from a
sorted map is built one put at a time here, where the JDK builds it balanced at once: the same map, other pairs
compared.

A started Thread is a thread of the system's, which runs run() as its current thread; Object.wait and notify go
by the monitors' table (notify wakes every waiter: for all but one, a wake-up of their own, which Java allows). A
thread the translation didn't start gets its Thread object the first time it asks, named as the system names it.
Thread ids count from 1 for the first thread asked about (a JVM's own threads take ids of their own).

Executors' fixed pools (the single-thread one among them) start a thread for each task until they have their size,
then queue, as the JDK's; their threads are named "pool-N-thread-M". A task that throws by execute ends its thread,
and the next task gets a new one.

Unicode's tables (case, categories, digits, what is cased) are the JDK's that builds the tool: build.sh runs
`tools/UnicodeTables.java`, which writes them as Rust statics (data of its Unicode version, no code), and the
tests compare with that same JDK. String's case conversions are the root locale's, the only rules the model's
Locale has (it can't make a Turkish one): the same String back when nothing changes, ß raised to SS, İ lowered to
i and a dot, and a capital sigma lowered to its final form as the JDK decides it, by word boundaries. Those are the
JDK's BreakIterator's (its legacy rules) as its isBoundary answers them, quirks included: just after a pair of
surrogates there is always one. On 600 000 texts made at random of Greek, Latin, digits, marks, punctuation,
kana, Thai and a supplementary letter, every decision was the JDK's.

A ConcurrentHashMap is laid out as the JDK's (the tests', 25): its table's sizes (a map's size for new
ConcurrentHashMap(map), putAll one put after another), growing when the size reaches three quarters of the table,
a resize that keeps each bin's tail and puts the nodes before it in front, reversed, a bin of eight in a small
table growing it eight times, and iterators that never throw but follow a resize as the JDK's do (checked with two
resizes while one iterates). One monitor guards it all. Bins the JDK makes trees stay lists (see HashMap).

A CompletableFuture runs its dependents in the JDK's order (a stage's own, the last added first; those of a
dependent that completes in turn, before the stage's remaining ones, the first added first), wraps exceptions in
CompletionException where the JDK's does, and reports them as its join and get do. Async tasks run each on a thread
of their own: what the JDK does when its common pool has no parallelism.

String.split is the JDK's fast path alone (a separator of one plain character, or a backslash and one that isn't a
letter nor a digit): the translator lets a call in only when its separator is such a constant, loaded just before
the call. Regular expressions aren't modelled.

Not modelled: toArray's runtime types beyond copying an array's own class, Class.getComponentType, limit, skip,
takeWhile, dropWhile, iterate, generate, DoubleStream, list iterators, reversed views, the views of sorted maps
and sets (headMap, subSet, descendingMap...), the other concurrent collections, interrupts, scheduled and cached thread pools,
CompletableFuture's Async stages, timeouts and either stages.


A class's loader (Class.getClassLoader) is one object, the mod's, for the mod's own classes and the threads' context
(NeoForge loads the mod and runs the game's threads with one); Java's classes have none (null); another library's
or the game's class's loader isn't modelled and is refused. ServiceLoader serves the mod's own services from the
providers its jar lists (META-INF/services, as the JDK reads the files: what precedes a '#', trimmed, each once, in
order), from a table the translator writes: each made the first time an iterator reaches it, kept for the later
iterators, one whose constructor throws given as the JDK's ServiceConfigurationError and not tried again, all made
anew after reload (observed, and compared in `test/translate/`). The translator makes only the providers of the
services there may be a class object of; a service of another jar's, whose providers may be elsewhere (an addon's),
is refused. stream() and its Provider aren't modelled.
