# horizon-port-neoforge

Converts **NeoForge** mods into [Horizon](https://github.com/horizon-rust/horizon) native-mod bundles, in the
format [Horizon-Translator](https://github.com/merenotnoonereal/Horizon-Translator)'s Fabric converter
(`horizon-port`) writes, with the same guarantees: what isn't certain is written commented out with a
`TODO` and the reason, and a bundle builds and plays like vanilla until its `TODO`s are filled in.

Horizon is Minecraft: Java Edition 26.3 rewritten in Rust. It runs no Java, so a NeoForge jar never runs
in it as it is.

## How it relates to Horizon-Translator

Horizon-Translator has no licence file, so none of its code is copied or changed here. This is separate
code that **calls** it:

- `horizon-port.jar`, built from an unmodified clone and run as a program of its own (its licence
  check, and its `port`: Vineflower, the Mixin matching, the Java to Rust translation);
- later, Horizon's own `horizon-port-pack` and `native-hooks.json`, which are MIT.

`ModFrontend` is the seam. A loader's frontend reads a jar into data that doesn't depend on the loader
(`ModInfo`), and everything after it works on that. If Horizon-Translator gets a licence, the NeoForge
frontend can move into its `mod-converter/` behind the same seam.

## Build

Needs a JDK 21 or newer, nothing else.

```bash
bash build.sh    # build/horizon-port-neoforge.jar
```

## Commands

### `metadata`: what the jar says about itself

```bash
java -jar build/horizon-port-neoforge.jar metadata some-mod.jar [--json]
```

Reads `META-INF/neoforge.mods.toml` and prints the mod (id, name, version), its licence and authors, its
dependencies, its Mixin configs and its access transformers; with `--json`, the same as JSON. Nothing
else in the jar is read. `${file.jarVersion}` is filled in from the jar manifest's
`Implementation-Version`, as NeoForge does.

A jar's mod is its first `[[mods]]` entry. Jars for Forge, or for NeoForge before 20.5
(`META-INF/mods.toml`), aren't read. A jar made for both NeoForge and Fabric needs `--loader neoforge`.

### `license`: may this mod be decompiled and ported?

```bash
java -jar build/horizon-port-neoforge.jar license some-mod.jar [--i-am-the-author NAME]
```

The answer is horizon-port's, by its own rules, unchanged: open licences (MIT, Apache, GPL, LGPL, MPL...)
allow it, with their conditions; All Rights Reserved, no-derivatives, unknown and missing licences
don't, unless `NAME` is one of the mod's authors. The exit code is 0 when it is allowed and 3 when it
isn't. It reads what the jar says about itself; it isn't legal advice.

How a NeoForge jar gets horizon-port's answer without horizon-port being changed: it is handed a
stand-in jar holding only a `fabric.mod.json` written from the mod's own metadata (its `license` and
`authors`, word for word) and the jar's licence files as they are. No class and no other file of the
mod goes into it, and it is deleted as soon as horizon-port has answered. Nothing here classifies a
licence.

- **Only what the mod really declares counts.** A missing or blank `license`, or a build placeholder
  nobody filled in (`${mod_license}`), is no licence: refused, unless a licence file in the jar is open.
- **Licence files count too, and the strictest wins.** A `LICENSE` that says "all rights reserved"
  refuses a mod whose metadata says MIT.
- **Authors.** NeoForge's `authors` is one free-form text. The whole text is an author, and so is each
  name between commas in it; nothing looser.
- **A jar made for both loaders** (`--loader neoforge`) is also checked on its own `fabric.mod.json`,
  and both have to allow it.
- **A Fabric jar** goes to horizon-port as it is, and its answer comes back as it is.

`horizon-port.jar` is Horizon-Translator's converter, built in its clone
(`bash mod-converter/build.sh`). It is found through `--translator`, else the `HORIZON_PORT_JAR`
variable, else a `Horizon-Translator/` folder beside this tool.

### `analyze`: what the mod does

```bash
java -jar build/horizon-port-neoforge.jar analyze some-mod.jar [--out DIR] [--neoforge-jar JAR[:JAR...]]
```

Writes `DIR/report.md` and `DIR/report.json` (default `DIR`: `out/<mod id>`). It reads the jar's class
files and resources: names, annotations, and the order of the calls in each method. Nothing is
decompiled and nothing of the mod is run. It finds:

| What | How it is read |
|---|---|
| Entry points | Each `@Mod` class, the side its `dist` names, and what its constructor is handed. |
| Event listeners | `@SubscribeEvent` methods (of an `@EventBusSubscriber` class, or of a class or object given to `register`) and `addListener` with a lambda or a method reference. For each: its event, its bus, its side, its priority. |
| Registries | Each `DeferredRegister`, what is registered with it, and the class of each thing. |
| Configs | Each `registerConfig`: its type and its spec. |
| Mixins | The classes the Mixin configs list, with their targets and injectors, and Mixin classes no config lists. |
| Access transformers | Their entries. |
| The rest of the code | Classes that extend the game's, and the other parts of NeoForge's API it uses (payloads, capabilities, attachments...). |

- **A listener's bus.** NeoForge posts an event on the mod bus when its class implements
  `IModBusEvent`, and on the game bus otherwise. That is written in NeoForge's own jars, not in the
  mod's. So a bus is given when the mod's code shows it (the listener is added to
  `NeoForge.EVENT_BUS`, or to the bus a `@Mod` constructor is handed), when NeoForge's documentation
  names the event, or when NeoForge's jars are given with `--neoforge-jar` and the event's ancestry
  is read from them. Otherwise it is "not known". It is never guessed from an event's name.
- **What it can't read, it says.** A handler kept in a variable, a bus passed on to another method,
  a `DeferredRegister` that isn't kept in a static field: the report says what wasn't read.
- **Statuses.** Each part gets one of horizon-port's statuses (`auto`, `content`, `needs-mapping`,
  `needs-translation`, `unsupported`), or `info` when there is nothing to convert. Nothing is `auto`
  or `content` unless it is known to carry over. `analyze` alone matches nothing against Horizon:
  listeners and Mixins are `needs-mapping`. `port` gives each Mixin handler its real status.
- **Mixin configs.** The ones `neoforge.mods.toml` lists. One named only in the jar's manifest
  (`MixinConfigs`) is reported, and its classes are taken as not applied.

A Fabric jar goes to horizon-port's own `analyze` as it is.

### `port`: the bundle

```bash
java -jar build/horizon-port-neoforge.jar port some-mod.jar --horizon-src ../horizon [--out DIR]
```

Writes a Horizon native-mod bundle to `DIR` (default `out/<mod id>-horizon`): `mixins/<id>/` with
`horizon-mixin.toml`, `runtime.rs`, `game_side.rs` and a helper per injection, `todo/` with notes on
what is commented out, `PORTING.md`, and `report/`. A folder that is already there isn't written over.

What happens, in order:

1. **The licence check** (`license`, above). Refused: exit code 3, nothing decompiled, nothing written.
2. **The analysis** (`analyze`, above).
3. **horizon-port's own `port`, unchanged, on a copy of the jar.** It decompiles the copy with
   Vineflower, matches the Mixins against the Horizon source given and translates the Java that
   doesn't need the game to Rust. The copy is the mod's jar with a `fabric.mod.json` written from
   its metadata, so that horizon-port reads it; see `StandIn.java` for the four things that differ
   from the mod's jar (Mixin configs as plain JSON; no Mixin class that no config lists; no jar
   inside the jar; no `fabric.mod.json` of the jar's own).
4. **The bundle is gone over for a NeoForge mod** (`Bundle.java`), and `PORTING.md` gets a section,
   "From a NeoForge mod", that says what was changed and what isn't in the bundle:
   - The decompiled Java that horizon-port copies into the helpers as comments is taken out. Each
     helper still names the Java file it stands in for.
   - An injection whose target function isn't in the Horizon source given is commented out, with
     why. horizon-port's table of hook points was written for one Horizon; yours may be another.
   - The settings page horizon-port switches on when a class extends one of the game's screens is
     commented out, unless the mod names NeoForge's `IConfigScreenFactory`. A class's name isn't a
     correspondence.
   - The `.hnmod` horizon-port packs when it sees nothing left is removed.
5. **The copy and the decompiled source are deleted**, whether it worked or not.

`report/report.md` and `report.json` are `analyze`'s, with what the conversion made: the injections
that are on and off, each Mixin handler's status and where it is in the bundle, and horizon-port's
count of the methods it translated. `report/horizon-port-report.md` is horizon-port's own report.

- **Not converted yet:** event listeners and registries. They are in the report and in `PORTING.md`
  as left to do.
- **Nothing is compiled or run**, by this tool or by horizon-port's `port`. Building a bundle is
  yours to decide (`PORTING.md`, "Build it").
- **The Horizon commit** the bundle names is `--horizon-revision`, else the source's
  `.horizon-revision`, else git's `HEAD` of that folder (it says so when the folder has changes
  that aren't committed).
- **Java's stack.** horizon-port is run with `-Xss512m`: on Horizon's current source its comment
  stripper overflows Java's default stack (`StackOverflowError`). That is true of horizon-port on a
  Fabric mod too; run it as `java -Xss512m -jar horizon-port.jar ...` there.

A Fabric jar goes to horizon-port's own `port` as it is, with that stack.

## Tests

```bash
bash test/run-test.sh
```

It builds the tool, writes three made-up NeoForge mods (`test/make-fixture.sh`: "Pocket Compass", a
second one of things done oddly, a third with nothing left to finish) with stand-ins for the game,
NeoForge and Horizon's source, packs them into jars and checks what the tool says about each and
what `port` writes. No real mod, no game file, nothing of Horizon's, nothing downloaded. The licence
and `port` checks need `horizon-port.jar` (above); `port` runs Vineflower from that clone's `tools/`.

## Rules of this repository

- No mod jars, game files or decompiled code are committed, ever. `.gitignore` refuses `*.jar` and `*.class`.
- Decompiled code stays out of the bundles this tool writes, and is deleted when a conversion ends.
- A converted mod is never compiled or run by this tool on its own.
- No guessed correspondences: a mapping is switched on only when it is certain.

## Licence

MIT: see [LICENSE](LICENSE). That is this tool's own code. It covers nothing of Horizon-Translator's,
which this tool runs and doesn't contain, and nothing of the mods it converts: a bundle is a port of
its mod, under that mod's licence (`PORTING.md` in each bundle has its conditions).
