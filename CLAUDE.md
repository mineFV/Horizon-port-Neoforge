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
1. `dump`: the mod's real facts (blocks, items, block entities, menus, fluids), read in a NeoForge server;
2. the content mod from those facts: properties, states, shapes, stack sizes, the blocks the carry left out;
3. the machines wired on Horizon's block-entity, container and screen hook points;
4. **our own translator**, from bytecode to Rust, written from scratch here. It replaces horizon-port's
   translation of the mod's code (2% of Mekanism's methods). horizon-port keeps the licence check, the Mixin
   matching and the Fabric path.

It is separate code that **calls** other programs and never contains theirs:

- `horizon-port.jar` (Horizon-Translator): licence check, decompiling, Mixin matching, Java to Rust (until ours), `hooks`, `check`;
- Horizon's `native-hooks.json`: which hook point stands for which NeoForge event;
- Horizon's `horizon-port-pack` and `cargo-horizon-mod`: the content mod (`.hmod`), and checking a `.hmod` or `.hnmod`.

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

The tool's commands: `metadata`, `license`, `analyze`, `port`, `check`, `pack`. The tests need
`horizon-port.jar`; the ones that pack a `.hmod` or check a `.hnmod` are skipped, with a note, without Horizon's tools.

## Rules that are not up for interpretation

1. **Horizon-Translator has no licence file.** Never copy, adapt or modify its code, here or in its clone; only
   run its jar. Reading it to learn what it does is fine. Look for a `LICENSE` there at the start of a session:
   if one has appeared, tell the maintainer before doing anything with it.
2. **A converted mod is never compiled or run on your own.** `port`, `check` and `pack` compile nothing.
   `check --compile` (cargo type-checks the bundle) only when the maintainer asks for that run. Never start
   `horizon-launcher native-build` or pass `--trust-native-code`: Horizon's docs make that the player's decision.
   One exception, decided by the maintainer (2026-11-14): `dump` loads the original NeoForge mod in a NeoForge
   server, in a folder under `essais/`, to read its registries. Run it only when the maintainer asks for that
   mod; accepting Minecraft's EULA for that server is the maintainer's to do, never yours.
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
| `Content`, `HorizonTools` | The jar's data and assets as a content mod, through Horizon's own tools |
| `Check`, `RustText` | `check`: a bundle against a Horizon source, by reading; `--compile` hands it to horizon-port's check |
| `Pack` | `pack`: the bundle as a `.hnmod`, written as Horizon's `docs/api/mod-files.md` says |
| `Report` | `report.md` and `report.json` |
| `Toml`, `Json`, `Translator` | Small readers, and the one place that starts `horizon-port.jar` |

## How the code and the tests are written

- Final package-private classes, records for data, no reflection, no third-party library. A comment says why.
- What the tool prints is plain and owns up to what it didn't read: `ok:`, `problem:`, `note:`, `not checked:`.
- Unit tests are plain programs (`test/java/horizonneo/*Test.java`, counted by `Checks`). Command-line tests are
  in `test/run-test.sh`, with `exits`, `says`, `lacks`, `is_file`, `no_file`.
- The fixture is written by `test/make-fixture.sh`: made-up mods and stand-ins for NeoForge, the game and
  Horizon's source. Nothing in it comes from a real mod. When a real jar shows a bug, reproduce its shape in
  the fixture, then fix it.
- The tests compile nothing but their own Java: `check --compile` runs against a stand-in for horizon-port.
- Every change comes with its test, and `--help` says what a command does and what it doesn't.

## Found the hard way

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
- `analyze --neoforge-jar` has only seen stand-ins, never NeoForge's own jars.
