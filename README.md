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

## Build

Needs a JDK 21 or newer, nothing else.

```bash
bash build.sh    # build/horizon-port-neoforge.jar
```

## Rules of this repository

- No mod jars, game files or decompiled code are committed, ever. `.gitignore` refuses `*.jar` and `*.class`.
- Decompiled code stays out of the bundles this tool writes, and is deleted when a conversion ends.
- A converted mod is never compiled or run by this tool on its own.
- No guessed correspondences: a mapping is switched on only when it is certain.
