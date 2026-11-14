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
   Throwable and the JVM's exceptions so far. Next in (c): Record, Enum, boxing, Objects, collections; then (d)
   the game's API; then its output goes into `port`'s bundle, and its flow is rebuilt as loops and `if`s.

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

The tool's commands: `metadata`, `license`, `analyze`, `port`, `check`, `pack`, `dump`, `translate`. The tests need
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
| `Bytecode`, `MethodRust`, `JarRust`, `JavaWorld` | `translate`, our own translator: every opcode decoded; a method's bytecode as a Rust function (each basic block an arm of a `match` in a `loop`) and what it needs; the classes as the JVM resolves them (field slots, call selection, subtypes, class ids); the jar's methods, kept until what each needs is translated, as one module with the object model's runtime, dispatchers, type tests, class initializers and lambda classes, and why for the rest |
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
- our translator's output has been compiled and run on the test's made-up classes only. Mekanism's 4363 translated
  methods (of 24149; horizon-port translates 350) were type-checked by rustc once, at the maintainer's request
  (2026-11-14, `--emit=metadata`: no error, no warning), never run. Nothing calls them yet: the world of
  objects is closed, and a translated method called from the game with an object of the game's would break that;
- the facts read blocks without a level: what a block decides from its neighbours or its block entity (a cable's
  connections, in Mekanism) isn't in them.
