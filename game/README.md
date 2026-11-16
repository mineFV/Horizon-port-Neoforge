# The translator's stand-ins for the game

What `translate` puts in place of the game's classes (Minecraft's, NeoForge's) that a mod uses, as decided by the
maintainer on 2026-11-16 (CLAUDE.md, "Step (d)"). `lib/` models Java's library; this folder does the same for the
game, compiled with it by `build.sh` and translated with the mod by the same translator.

Where a stand-in's behaviour comes from, and nothing else:

- **Horizon's own code** (MIT), which rewrites the game in Rust: each member says which file it follows;
- the game's **public API**, as `--server` reads it from a NeoForge server's jars: names, descriptors, supertypes
  and which members a class declares. Never its code: no stand-in is written from Mojang's or NeoForge's
  bytecode or decompiled source.

A member whose behaviour neither gives for certain is left out (a mod's call to it isn't translated), or, when the
real class declares it over one it inherits (`equals`, `hashCode`, `toString`), it is declared `native` with no Rust
behind it: the translator then refuses a call to it, rather than running the inherited one.

A stand-in declares the real class's superclass and interfaces, so that a type test gives the game's answer. With
`--server`, `translate` checks each stand-in against the real class (`Library.checkStandIns`) and leaves out one
whose supertypes differ, or that would inherit a method the real class declares.

What differs from the game, where a program can see it:

- a position's methods (`above`, `relative`...) give a new object each time: whether the game ever hands back the
  same object where it gives an equal one isn't in Horizon's code, so a mod that compares positions with `==` could
  tell. `offset(int, int, int)` is left out for the same reason (an offset of nothing may be the same object);
- Vec3i is abstract: a mod's own `new Vec3i` isn't translated (see Vec3i.java).

Left out for now, and what it costs: `Direction.toString` (nothing in Horizon gives the game's text for a
direction). The world is closed, and nearly every mod calls `toString` on some Object (a text joined with one), which
may then be a Direction: so no Direction can be made, and nothing that uses one is translated.
