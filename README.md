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

- `horizon-port.jar`, built from an unmodified clone and run as a program of its own;
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

## Tests

```bash
bash test/run-test.sh
```

It builds the tool, writes a made-up NeoForge mod ("Pocket Compass", `test/make-fixture.sh`), packs it
into jars and checks what the tool says about each. No real mod, no game file, nothing downloaded.
The licence checks need `horizon-port.jar` (above).

## Rules of this repository

- No mod jars, game files or decompiled code are committed, ever. `.gitignore` refuses `*.jar` and `*.class`.
- Decompiled code stays out of the bundles this tool writes, and is deleted when a conversion ends.
- A converted mod is never compiled or run by this tool on its own.
- No guessed correspondences: a mapping is switched on only when it is certain.
