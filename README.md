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

## Tests

```bash
bash test/run-test.sh
```

It builds the tool, writes a made-up NeoForge mod ("Pocket Compass", `test/make-fixture.sh`), packs it
into jars and checks what the tool says about each. No real mod, no game file, nothing downloaded.

## Rules of this repository

- No mod jars, game files or decompiled code are committed, ever. `.gitignore` refuses `*.jar` and `*.class`.
- Decompiled code stays out of the bundles this tool writes, and is deleted when a conversion ends.
- A converted mod is never compiled or run by this tool on its own.
- No guessed correspondences: a mapping is switched on only when it is certain.
