# The translator's stand-ins for the game

What `translate` puts in place of the game's classes (Minecraft's, NeoForge's) that a mod uses, as decided by the
maintainer on 2026-11-16 (CLAUDE.md, "Step (d)"). `lib/` models Java's library; this folder does the same for the
game, compiled with it by `build.sh` and translated with the mod by the same translator.

Where a stand-in's behaviour comes from, and nothing else:

- **Horizon's own code** (MIT), which rewrites the game in Rust: each member says which file it follows;
- the game's **public API**, as `--server` reads it from a NeoForge server's jars: names, descriptors, supertypes
  and which members a class declares. Never its code: no stand-in is written from Mojang's or NeoForge's
  bytecode or decompiled source;
- **what the game answers**, observed (decided by the maintainer, 2026-11-16): `standins --server` compiles one
  program (probe/, `GameProbe`) against the server's jars and against the stand-ins, runs both and compares what
  they print, line by line. The game's classes run as a library, with no server, world or mod. A behaviour taken
  from observation says so, and the probe calls it on inputs that pin it down (extremes included).
  On NeoForge 21.1.266 (2026-11-16): 1135 answers, all the same.

A member whose behaviour neither gives for certain is left out (a mod's call to it isn't translated), or, when the
real class declares it over one it inherits (`equals`, `hashCode`, `toString`), it is declared `native` with no Rust
behind it: the translator then refuses a call to it, rather than running the inherited one.

A stand-in declares the real class's superclass and interfaces, so that a type test gives the game's answer. With
`--server`, `translate` checks each stand-in against the real class (`Library.checkStandIns`) and leaves out one
whose supertypes differ, or that would inherit a method the real class declares.

What differs from the game, where a program can see it: nothing the probe asks (19601 answers on NeoForge
21.1.266, 2026-11-16), which includes which object each method gives back (this one or a new one, of which
class), caches (which numbers' tags are the same object), the texts of a mod's own subclasses (named and
anonymous), NBT's texts (SNBT) and orders, error messages, and the extremes where arithmetic wraps. A
NullPointerException's message isn't compared: the JVM builds it from the bytecode (lib/README.md).

What is here: positions and directions (net/minecraft/core: Vec3i, BlockPos and its MutableBlockPos, Direction
with Axis and AxisDirection) and NBT (net/minecraft/nbt: the tags of numbers, texts, lists, arrays and compounds,
and NbtUtils' positions), ids (net/minecraft/resources/ResourceLocation, with its exception), ByIdMap's continuous
maps and Mth's arithmetic (not its sine, cosine and atan2, which the game reads from tables built with Math.sin
and Math.asin, nor its random numbers). They cover what Mekanism calls of them, but for the members below.

What isn't here, and what it costs: BlockPos's getCenter, betweenClosed and codecs, Direction's getRotation and
codecs, MutableBlockPos's other setters, NBT's codecs (NbtOps), its readers and writers (DataOutput, visitors),
TagType, ResourceLocation's codecs and its readers (StringReader), ByIdMap.sparse. A call to one isn't translated; and when a virtual call through a wider type (Object, List...) may reach
an object whose class lacks it, the methods that make such objects drop instead. A stand-in is worth as much as
the API it covers.
