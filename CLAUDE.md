# CLAUDE.md

Notes for AI coding agents working in this repository: what to know before changing anything. The README
is only `# WORK IN PROGRESS` for now, at the maintainer's request: what a command does is in `--help`, and
the figures of trials on real mods are kept in `essais/`, out of git.

## What this is

`horizon-port-neoforge` turns a **NeoForge** mod jar into a [Horizon](https://github.com/horizon-rust/horizon)
native-mod bundle, in the format Horizon-Translator's Fabric converter (`horizon-port`) writes.

**The goal is the whole mod** (decided by the maintainer, 2026-11-14): its content with its real properties,
its machines (block entities, menus, screens) and its logic, not only its wiring. The plan, in order:

0. the rules below, and a report that owns up to what it didn't read;
1. `dump`: the mod's real facts, read in NeoForge by Horizon's own oracle (done);
2. the content mod from those facts: `port --facts` runs horizon-port-pack's pipeline on them, then declares
   the blocks it leaves out (no vanilla template with their states) with state properties of their own (done);
3. the machines wired on Horizon's block-entity hook points, from what NeoForge gives of them (`dump`'s machines
   file): type, ticking, item slots and their faces, energy, fluid tanks (done); menus and screens are left (below);
4. **our own translator**, from bytecode to Rust, written from scratch here. It replaces horizon-port's
   translation of the mod's code (2% of Mekanism's methods). horizon-port keeps the licence check, the Mixin
   matching and the Fabric path. Steps (a) and (b) are done: `translate` writes methods of numbers with Java's exact
   semantics, and objects in a uniform model chosen by the maintainer (2026-11-14): every object is an
   `Arc<Obj>` with a class id and its fields in slots, `null` is `None`, a virtual or interface call matches on
   the class, lambdas and arrays are objects too. Step (c) has begun: a method returns a `Result` (what Java throws is
   an `Err` holding the exception), try/catch/finally and class initializers that throw are translated, and Java's
   library is a model of our own (`lib/`), translated with the mod: Object, String (texts and their joins),
   Throwable and the JVM's exceptions, Record (its ObjectMethods), Enum, the boxes with their caches, Objects,
   java.util.function, the collections (ArrayList, HashMap and LinkedHashMap in the JDK's order, the sets, List.of
   and kin, Collections, Arrays, Comparator), Optional, StringBuilder, and Class objects (one for each type, class
   constants, getClass, isInstance, names, getEnumConstants, Enum.valueOf, Integer.TYPE and kin); lambdas box, unbox and widen as
   LambdaMetafactory does; EnumMap and EnumSet; streams (Stream, IntStream, LongStream, Collectors, spliterators),
   run as the JDK runs a sequential one. Step (c) is done but for what `lib/README.md` lists. Then (d) the game's API; then its
   output goes into `port`'s bundle. Its flow is rebuilt as loops and labelled blocks.

### Step (d), the game's API: the maintainer's decisions (2026-11-16)

Nothing of the game's own code (Mojang's, NeoForge's) is translated or copied into the bundle: its classes become
stand-ins of our own, each behaviour certain (rule 4) or left out with a `TODO`.

1. **An object of the game is an `Obj` of a stand-in class in `lib/`** (ItemStack, FluidStack...), converted from
   and to Horizon's own type at a hook point. Horizon's are values it copies, so what the mod changed in place is
   written back when the hook point returns. Type tests and dispatch stay as they are.
2. **Value classes first** (Direction, BlockPos, ResourceLocation, Mth's constants, ItemStack's and FluidStack's
   plain accessors): their behaviour is taken from Horizon's own code (MIT) and public documentation, never from
   Mojang's bytecode. What isn't certain stays out.
3. **A mod's config** (`ModConfigSpec`): its defaults as `dump` reads them in NeoForge, then the player's values in
   Horizon's settings store (`horizon-hooks`' `settings.rs`, a file `horizon-<modid>.txt`).
4. **The mod's block entities are made by the translation**: at the `block_entity` hook point, its translated
   constructor runs, and the game's part is a stand-in `BlockEntity` in `lib/` (level, position, state), so the
   object is in the closed world. Screens come later.
5. **Libraries shipped with the game**: stand-ins in `lib/`, from their public API, for the classes the mod uses
   (Guava's HashBasedTable, fastutil's maps and sets), an iteration order exactly theirs or the class stays out;
   slf4j's Logger writes to Horizon's output.
6. **The translation stays out of the bundle for now.** First, one translated method wired on
   `block_entity_tick`: its first run in the game, built by the maintainer (rule 2).

It is separate code that **calls** other programs and never contains theirs:

- `horizon-port.jar` (Horizon-Translator): licence check, decompiling, Mixin matching, Java to Rust (until ours), `hooks`, `check`;
- Horizon's `native-hooks.json`: which hook point stands for which NeoForge event;
- Horizon's `horizon-port-pack` and `cargo-horizon-mod`: the content mod (`.hmod`), and checking a `.hmod` or `.hnmod`;
- Horizon's `tools/neoforge_oracle.py` (Python 3.11+): a NeoForge server with the mod and Horizon's exporter, for `dump`.
  Our own exporter (`exporter/`, carried in the jar as source) goes beside it through the oracle's `--deps`.

Java 21, no dependencies, one jar.

## Folders it expects beside it

```
<parent>/
  horizon-port-neoforge/   this repository
  Horizon-Translator/      an unmodified clone; its jar is built there: bash mod-converter/build.sh
  horizon/                 a clone of horizon-rust/horizon; target/release/ holds its two tools once cargo built them
  essais/                  optional: real mods tried by hand, their bundles and logs. Never in git
```

Elsewhere: `HORIZON_PORT_JAR` (or `--translator`) names the jar, `HORIZON_TOOLS` (or `--horizon-tools`) the tools' folder.

## Commands

```bash
bash build.sh                 # build/horizon-port-neoforge.jar; warnings are errors
bash test/run-test.sh         # builds, writes the fixture, runs everything; ends with "Everything passed."
java -jar build/horizon-port-neoforge.jar --help
```

The tool's commands: `metadata`, `license`, `analyze`, `port`, `check`, `pack`, `dump`, `standins` (the game's stand-ins against a server's classes and answers), `translate` (`--server`: a NeoForge server's libraries, for the game's supertypes). The tests need
`horizon-port.jar`; the ones that pack a `.hmod` or check a `.hnmod` are skipped, with a note, without Horizon's tools.

## Rules that are not up for interpretation

1. **Horizon-Translator has no licence file.** Never copy, adapt or modify its code, here or in its clone; only
   run its jar. Reading it to learn what it does is fine. Look for a `LICENSE` there at the start of a session:
   if one has appeared, tell the maintainer before doing anything with it.
2. **A converted mod is never compiled or run on your own.** `port`, `check` and `pack` compile nothing.
   `check --compile` (cargo type-checks the bundle) only when the maintainer asks for that run. Never start
   `horizon-launcher native-build` or pass `--trust-native-code`: Horizon's docs make that the player's decision.
   One exception, decided by the maintainer (2026-11-14): `dump` has Horizon's oracle load the original NeoForge
   mod in a NeoForge server (the one the maintainer installed, linked into Horizon's `.local/neoforge-oracle/`
   so the oracle downloads nothing) to read its registries. Run it only when the maintainer asks for that mod;
   `dump` itself never accepts Minecraft's EULA: it stops when the server's eula.txt doesn't say eula=true.
   The maintainer has read the EULA and accepts it (2026-11-14): for a NeoForge server of theirs that `dump`
   uses, an agent may write eula=true in its eula.txt, and says so.
3. **Ask before downloading anything**: mods, NeoForge's installer or jars, the game, tools. Each time.
4. **No guessed correspondence.** Something is switched on only when it is certain (the catalogue names one
   hook point for the event, the target is in the source, ...). Anything else is written commented out with a
   `TODO` and the reason. When unsure whether a case is certain, it isn't.
5. **Decompiled code stays out**: out of the bundle, out of git, deleted when a conversion ends. No mod jar,
   class file, game file or prepared resource is ever committed (`.gitignore` refuses `*.jar`, `*.class`, `*-src/`).
6. **The Fabric path is not ours to change.** A Fabric jar goes to `horizon-port` as it is. The Translator's own
   tests (`bash mod-converter/test/run-test.sh` in its clone) must still pass after any change here.
7. **Git**: small commits that say one thing; don't push unless asked.
8. **Language**: code, comments, README and commits are in English. The maintainer writes in French: answer in French.

## Where things are

| File (`src/horizonneo/`) | What it does |
|---|---|
| `Main` | The command line; a Fabric jar is handed to horizon-port unchanged |
| `ModJar`, `ModsToml`, `NeoForgeFrontend`, `ModInfo`, `ModFrontend` | Reading `META-INF/neoforge.mods.toml`; `ModFrontend` is the seam between a loader and the rest |
| `LicenceCheck` | `license`, by horizon-port's rules, on a stand-in for the metadata |
| `ClassData`, `Analyzer`, `Analysis`, `BusFacts` | `analyze`: listeners, registries, configs, Mixins, read from class files without decompiling |
| `StandIn` | The copy of the jar that horizon-port is given (a `fabric.mod.json`, strict JSON Mixin configs) |
| `Port` | `port`: the whole conversion, in a temporary folder that is always deleted |
| `Bundle` | Goes over horizon-port's bundle: strips decompiled Java, turns off what isn't certain, rewrites `PORTING.md` |
| `EventTable`, `Wiring` | Horizon's catalogue as an event table; listeners put on hook points, or commented out with why |
| `Declarations` | Blocks the carry left out, declared from the facts on a plain template with state properties of their own |
| `Content`, `HorizonTools` | The jar's data and assets as a content mod, through Horizon's own tools |
| `Check`, `RustText` | `check`: a bundle against a Horizon source, by reading; `--compile` hands it to horizon-port's check |
| `Dump` | `dump`: links the maintainer's NeoForge server into Horizon's oracle folder, compiles our machines exporter against its jars, runs Horizon's oracle with it |
| `Machines` | The block entities in the machines file, on Horizon's block-entity hook points; `report/machines.md` |
| `exporter/` (not in `src/`) | The NeoForge mod `dump` compiles at run time: places each block with a block entity and writes `<facts>.machines.json` |
| `lib/` (not in `src/`), `Library` | The translator's model of Java's library: our own Java, from the API specification, compiled by `build.sh` with `--patch-module java.base=lib` into the jar's `model/`; its `native` methods in Rust |
| `StandIns`, `probe/` (not in `src/`) | `standins`: the stand-ins' signatures against a server's classes, and one probe compiled against both and run, their answers compared |
| `game/` (not in `src/`) | The translator's stand-ins for the game's classes (step d): our own Java, from Horizon's code and the game's public API, compiled with `lib/`; checked against the real classes with `--server` (`Library.checkStandIns`). Its README says where each behaviour comes from and what differs |
| `tools/UnicodeTables.java` (not in `src/`) | Run by `build.sh`: Unicode's tables from the JDK that builds the tool, as Rust statics (`unicode.rs` in the jar) that the model's Character and String read |
| `Bytecode`, `MethodRust`, `JarRust`, `JavaWorld` | `translate`, our own translator: every opcode decoded; a method's bytecode as a Rust function (its blocks as labelled loops and blocks, or as arms of a `match` in a `loop` when the flow is irreducible) and what it needs; the classes as the JVM resolves them (field slots, call selection, subtypes, class ids); the jar's methods, kept until what each needs is translated, as one module with the object model's runtime, dispatchers, type tests, class initializers and lambda classes, and why for the rest |
| `Pack` | `pack`: the bundle as a `.hnmod`, written as Horizon's `docs/api/mod-files.md` says |
| `Report` | `report.md` and `report.json` |
| `Toml`, `Json`, `Translator` | Small readers, and the one place that starts `horizon-port.jar` |

## How the code and the tests are written

- Final package-private classes, records for data, no reflection, no third-party library. A comment says why.
  `exporter/` is a NeoForge mod, not the tool: it uses the game's Gson, and reads two private fields of the game
  (`AbstractContainerMenu.dataSlots`, `Level.freshBlockEntities`), each with why.
- What the tool prints is plain and owns up to what it didn't read: `ok:`, `problem:`, `note:`, `not checked:`.
- Unit tests are plain programs (`test/java/horizonneo/*Test.java`, counted by `Checks`). Command-line tests are
  in `test/run-test.sh`, with `exits`, `says`, `lacks`, `is_file`, `no_file`.
- The fixture is written by `test/make-fixture.sh`: made-up mods and stand-ins for NeoForge, the game and
  Horizon's source. Nothing in it comes from a real mod. When a real jar shows a bug, reproduce its shape in
  the fixture, then fix it.
- The tests compile nothing but their own Java: `check --compile` runs against a stand-in for horizon-port. One
  exception: where `rustc` is, the translation of their own made-up class (`test/translate/`) is compiled and run
  beside the Java, on the same inputs, and the outputs must be the same. Never a real mod's translation.
- Every change comes with its test, and `--help` says what a command does and what it doesn't.

## Found the hard way

- **horizon-port-pack's `check` never reads the oracle's facts**; its pipeline does (`import`, `inventory --offline`,
  `carry <work> <key>`, with `<work>/oracle/facts.json`). And `carry` without a key takes only the mods the
  inventory classed as content: a mod it doesn't know (`unknown`) is carried only when named.
- **horizon-port-pack's carry declares a block only when a vanilla template has exactly its state properties**:
  Mekanism's machines (`active`, `facing`, `fluid_logged`) are all left out, though Horizon takes declared ones.
- **Horizon's oracle stops the server in its `ServerStartedEvent` listener**: the machines exporter listens at
  `HIGHEST` to run first. And NeoForge calls a block entity's `onLoad` only at the level's next tick, which never
  comes: the exporter calls it, then takes it off `freshBlockEntities`. Mekanism sets up its sides there.
- **Java's Double.toString and Float.toString pick the shortest digits as Rust does, but not always the same ones**:
  a value with one shortest digit is written with the closest two (4.9E-324), and between two shortest decimals as
  close as each other Java takes the one ending in an even digit, where Rust takes the higher. `Library` does both.
- **A type outside the jar and the model is still known when it is the JDK's**: `JavaWorld` reads its supertypes
  from the JDK the tool runs on (`jrt:/`). Without that, one class implementing `IntSupplier` made every question
  about `CharSequence` uncertain.
- **A lambda's class extends Object**: a call on it selects Object's methods first, even where its interface
  redeclares one (Comparator's `equals`); and of two default methods, the one a subinterface overrides isn't a
  candidate (List's `getFirst` over SequencedCollection's). A constructor reference (`Foo::new`) makes objects as
  `new` does: one whose superclass is outside the jar can't be made, and isn't counted among the objects there are.
- **An object whose supertypes aren't all known isn't made**: one of the mod's classes implementing another mod's
  interface (JEI's) would leave every "is it a List?" about the objects there are without an answer, and kept
  thousands of methods out. `translate --server` reads the supertypes (and the declared methods, for selection) of
  the game's, NeoForge's and their libraries' classes from a server's libraries; the client's classes and other
  mods' stay unknown. A default method of an interface read that way is the game's code: a call that may select it
  isn't translated. Type tests are answered once per set of objects: asking them per method took minutes.
- **Calling a static method initializes its class** (invokestatic, JVMS 5.5), from wherever it is called: each
  translated static method of a class with an initializer (or a superclass with one) starts with it. Without it,
  one called from outside the class (the game, the test's harness) ran with its statics unset (an `assert` fired:
  `$assertionsDisabled` is set by the initializer). Mekanism went from 8160 to 7302: the methods that left were
  static ones of classes whose initializer isn't translated, which Java would have run first.
- **Class objects come from tables the translator writes**: a type's superclass, interfaces, array component and
  flags (walked by `jinstance_of`), simple and canonical names (from the InnerClasses attribute, which `ClassData`
  now reads), and each enum's `values()`. A call to getEnumConstants needs the `values()` of every enum whose class
  object there may be (a class constant, or the class of an object there is).
- **Dropping a method is for good, though what dropped it may go later**: Enum.valueOf drops while an enum whose
  `values()` isn't translated has a class constant (in its own `valueOf`), and stayed dropped once that `valueOf`
  dropped in turn. So the translator checks every method again over the objects that the methods the mod's reach
  make, frozen; a method that would make one more stays out, so each method reached still has what it needs
  (`revive`). Two ways are run and the one leaving more of the mod's methods is kept: dropping first what drops
  whatever the objects are (a call, an initializer), or not; neither is always the larger (on Mekanism, not first:
  7668 against 7572; the test's `Tagged.pick` needs it first). Letting the objects grow with what comes back, until
  they cover it, brought fewer back (7497): the objects some methods make cost many others.
- **javac compiles an enum's `values()` with System.arraycopy when Object has no `clone`** (the patched java.base
  of `lib/`): the model's Object declares `clone`, so that values() clones `$VALUES` as against the JDK.
- **Dropping the callers of a method that can't be translated is one choice; dropping the makers of its objects
  is another**, and both leave each method with what it needs. A virtual call (Consumer.accept from
  Iterable.forEach) may reach every object of its type there is: one class of the mod, made in one place, whose
  method calls the game, kept every method that iterates out. Where a virtual call may reach a class (or a lambda)
  of the mod whose method isn't translated, its makers drop instead (`poisonable`): on Mekanism, 8967 methods
  against 7594; 8871 when it was done for calls on types outside the jar alone.
- **The runtime makes the JVM's own exceptions without a constructor**: their fields are the class's layout
  (`jthrown_layout`), not message and cause alone, or a field Throwable gained (the suppressed exceptions) would
  be read past the object's slots.
- **Monitors are by object, in one table**: the thread that holds each and how many times (reentrant), waited for
  on a Condvar; a synchronized method's body runs in a closure, so that its monitor is let go however it ends. Not
  modelled: Object.wait and notify, Thread.holdsLock.
- **The flow is rebuilt as Rust's own loops and labelled blocks** (`MethodRust.Structure`, after Ramsey's "Beyond
  Relooper"): a block a back edge reaches is a `'lN: loop` its back edges continue, one two forward edges reach
  follows a `'bN: { ... }` they break out of, any other is written inside its one predecessor's branch. A block in
  a try range still runs in its closure, which sets where it went. A loop entered at two places (an irreducible
  flow) keeps the `loop { match block ... }`.
- **A native can call the model back**: `Library.NATIVE_CALLS` names the methods a native's text calls
  (`{CALL0}`...), translated and reached with it; Thread.start's native runs `Thread.jRunThread` on a new thread
  of the system's.
- **A final sigma depends on the JDK's word boundaries, as its isBoundary gives them**: the legacy rules of its
  RuleBasedBreakIterator, read forwards, except that isBoundary backs up first, and from a pair of surrogates'
  low half reads on as if the pair were cut (a boundary right after any pair but the first thing in the text).
  `lib/java/lang/WordBreaks.java` was fitted on texts made at random until 600 000 of them gave the JDK's
  decisions; the test keeps 5 200.
- **Unicode's data comes from the JDK that builds the tool** (`tools/UnicodeTables.java`, run by build.sh): its
  version is the one the tests compare with. A mod's own JVM may have another Unicode version.
- **No class outside java.* extends a class of the model's that isn't public** (a nested one of java.util's): the
  JVM puts none but the JDK's in java.*. Without it, a cast to ConcurrentHashMap's Node[] over an array of a game
  class without `--server` dropped 1188 of Mekanism's methods.
- **The JDK the tests run on is 25, and its ConcurrentHashMap isn't 21's** where it shows: a map's constructor
  sizes the table for the map, putAll doesn't size it first. Probed with reflection on its table.
- **Remembering every "is a a t?" answer filled the heap**: a pair of strings per answer, millions of them (Mekanism
  ran out of 2 GB, Guava and fastutil with it out of 8). Each type's supertypes are found once instead (its chain
  of superclasses, all of them, whether one wasn't read): Mekanism translates in 1 GB, the same methods.
- **The output is the same run after run**: the natives, kept in a Map.ofEntries (whose order is salted per JVM),
  are written sorted.
- **A call can be checked where it is made**: String.split is modelled for the JDK's fast path only, and the
  translator (`MethodRust.splitWhy`) lets a call in only when a constant separator that takes it is loaded just
  before, with no jump onto the call; a method reference to split isn't translated.
- **A final class has no subclass**: whether an object is an Integer (a cast the model's IntStream makes) has an
  answer even for an object of a class whose superclass is outside the jar.
- **javac casts a generic array to its bound**: `T[] universe = c.getEnumConstants()` with `T extends Enum` is a
  checkcast to `Enum[]`, a type test over every array there may be, which an array of a class whose superclass is
  outside the jar leaves without an answer. The model's Enum.valueOf, EnumMap and EnumSet take the constants as
  `Enum<?>[]` from `Class.jEnumUniverse`, which isn't the JDK's API (a mod, built against the JDK, can't call it).
- **HotSpot drops the message of a JVM exception it throws often at one place** (OmitStackTraceInFastThrow): the
  Java reference of `test/translate/` runs with it off.
- **Rust's `as` from a float to an int is Java's cast** (saturating, NaN to 0), but `f64::min`/`max` and `signum`
  aren't Java's on NaN and negative zero, and `Math.sin` and its kin have no exact counterpart: the translator
  writes helpers of its own for the first ones and refuses the others.
- **The translator's world is closed, and a type outside the jar hides its own supertypes**: a type test or a
  virtual call that would need them isn't translated. A type outside the jar can't be a subtype of one of the
  jar's (the game doesn't know the mod), so questions about the jar's own types stay certain. The fixed point
  only drops methods: one dropped for an uncertain question asked about an object another dropped method made
  stays dropped, which is sound but keeps out some that could be translated.
- **A face's item handler may be a proxy with its own slot numbers** (Mekanism's): where a face slot is in the
  container is measured by putting an item through it. Slots no item goes into (outputs only a machine fills)
  stay unmapped, and said.
- **Horizon opens a mod container's menu only on a block whose template opens one** (`storage.rs`: chest,
  barrel, furnace...). The declared machines are on plain templates, so no menu is wired yet. Mekanism's block
  entities aren't `MenuProvider`s either: the mod opens its menus in its own code.

- **A stand-in of the game is useless until Object's methods are modelled on it**: the world is closed, and some
  method always calls `hashCode`, `equals`, `toString` or `compareTo` on an Object (a HashMap, a text joined with
  one, a sort): an object whose own isn't translated can't be made. Unmodelled, Vec3i's hashCode took Mekanism from
  9075 to 7955 (every HashMap user dropped) until a stand-in's makers drop instead (`poisonable`); BlockPos and
  Direction couldn't be made until Vec3i.compareTo and Direction.toString were (observed in the game, `standins`).
  The same holds for every member a mod calls: one missing (Mekanism's Direction.getClockWise) keeps every Direction
  out, so a stand-in counts only once it covers the API the mod uses.

- **Run horizon-port with `-Xss512m`** on a real Horizon source: its comment stripper overflows Java's default
  stack on Horizon's long lines. `Port.STACK` does it everywhere.
- **horizon-port's `check` knows one Horizon commit** (`a46ce6898bd72016e061aaec1afc1375b384deac`, in its
  `scripts/get-horizon.sh`). On a newer source it stops before cargo. `git archive` of that commit, from a
  Horizon clone, gives a source it accepts.
- **horizon-port packs only a finished port** (no `TODO`, nothing commented out), so `Pack` writes the `.hnmod`
  itself, from Horizon's documentation, and has `cargo horizon-mod check` read it.
- **The launcher's `native-build` takes `<bundle>/mixins/<id>`**, the folder holding `horizon-mixin.toml`, or the
  folder above it. Not the bundle's root.
- **horizon-port does things a NeoForge mod doesn't mean**: it converts every `@Mixin` class, listed or not; it
  turns a settings page on when a class extends a game screen; it copies decompiled Java into comments; it
  refuses lenient JSON. `StandIn` and `Bundle` are where each is dealt with.
- **Real mods register listeners in more ways than the documentation shows**: a method marked `@SubscribeEvent`
  and also given to `addListener`, `register(FIELD)`, `register(Thing.get())`. Mods that wrap `DeferredRegister`
  in classes of their own (Mekanism) aren't read through the wrapper; the report says so.

## Not verified yet

Say so rather than assume it works:

- the listener helpers this tool writes have never been compiled against Horizon itself;
- no bundle or `.hnmod` it made has been built by the launcher or loaded in the game;
- a content `.hmod` beside a `.hnmod` of the same id hasn't been tried, and the combined `.hmod` isn't made;
- `analyze --neoforge-jar` has only seen stand-ins, never NeoForge's own jars;
- `dump` and `port --facts` have run on one real mod (Mekanism 10.7.19, NeoForge 21.1.266, Java 25, 2026-11-14):
  200 blocks, 381 items, 1578 recipes; 64 blocks and 199 items declared, 136 blocks left out with why. The tests
  run `dump` against a stand-in for the oracle, never a server;
- a block declared with state properties of its own, a state light table or shape tables has passed
  `cargo horizon-mod check`, never Horizon's loader: Mekanism's 136 such blocks haven't been loaded in the game;
- the machines exporter has run on Mekanism only (2026-11-14): 154 block entity types, 103 with item slots,
  82 with energy, 16 with tanks, no menu NeoForge could be asked for. The tests run `Machines` on a made-up
  machines file; the exporter itself only compiles and runs against a real server;
- the machine helpers passed `check` (by reading) on Mekanism's bundle, never cargo nor the game;
- the library model leaves out reflection on members, parsing numbers (Character's Unicode tables), case
  mapping (toUpperCase), limit and skip: a method that uses them isn't translated. Where its
  collections differ from the JDK's is in `lib/README.md` (a HashMap bin the JDK makes a tree, Set.of's order);
- Mekanism (2026-11-15, afternoon): 9058 of 24149 methods without `--server`, 9075 with it (NeoForge 21.1.266's
  libraries), in about 35 s and 1 GB, the same output run after run. The steps, each type-checked: 6414 (the
  collections), 8154 (no object whose supertypes aren't all known), 7302 (class objects; static methods initializing
  their class, which rightly dropped those whose initializer isn't translated), 7668 (`revive`), 8967 (the makers of
  an object a call can't run on drop, not the callers), 9067 (the rest of the library model: streams, UUID, atomics,
  threads and executors, Unicode, TreeMap, ConcurrentHashMap...), 9075 (arraycopy, BitSet, UUID.fromString, split by a plain separator...), 9143 (2026-11-16: the first stand-ins of the game, BlockPos's packed form). With `--server` there were fewer for a while:
  objects of classes implementing NeoForge's interfaces can then be made, and calls that may reach them must be
  certain. The game holds the rest back: `essais/mekanism/phase-d-notes.md` measures what each part of it is worth;
- our translator's output has been compiled and run on the test's made-up classes only. Mekanism's translation was
  type-checked by rustc (`--emit=metadata`): 4363 methods at the maintainer's request (2026-11-14), then every
  version up to 9075 on 2026-11-15, while the maintainer was away and had left the decisions to the agent: no
  error, no warning. Never run. Nothing calls them yet: the world of
  objects is closed, and a translated method called from the game with an object of the game's would break that;
- the facts read blocks without a level: what a block decides from its neighbours or its block entity (a cable's
  connections, in Mekanism) isn't in them.
