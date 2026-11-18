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

What differs from the game, where a program can see it: nothing the probe asks (20225 answers on NeoForge
21.1.266, 2026-11-16), which includes which object each method gives back (this one or a new one, of which
class), caches (which numbers' tags are the same object), the texts of a mod's own subclasses (named and
anonymous), NBT's texts (SNBT) and orders, error messages, and the extremes where arithmetic wraps. A
NullPointerException's message isn't compared: the JVM builds it from the bytecode (lib/README.md).

What is here: positions and directions (net/minecraft/core: Vec3i, BlockPos and its MutableBlockPos, Direction
with Axis and AxisDirection) and NBT (net/minecraft/nbt: the tags of numbers, texts, lists, arrays and compounds,
and NbtUtils' positions), ids (net/minecraft/resources/ResourceLocation, with its exception), ByIdMap's continuous
maps equipment slots (net/minecraft/world/entity/EquipmentSlot, but limit), points in doubles (net/minecraft/world/phys/Vec3, but xRot, yRot and zRot, and Position) and Mth's arithmetic (not its sine, cosine and atan2, which the game reads from tables built with Math.sin
and Math.asin, nor its random numbers). They cover what Mekanism calls of them, but for the members below.

Items' data components (net/minecraft/core/component, 2026-11-17): their types (those every item has, and an item's
durability), maps, patches, a stack's and an item's (from its properties), set, taken out and compared as a server does,
and written to and read from NBT for the types whose codec is a number's; other codecs are kept as objects of no
behaviour. The game's own items' components are given (HorizonRegistries.known: `dump`'s machines file, `game_ids`):
where an item of the game's has a type that isn't modelled, what lists its types says it can't. A map of eight types or
more iterates, in the game, in its types' identity hashes' order, which changes from run to run: here, in the order
they were set.

Capabilities (net/neoforged/neoforge/capabilities, 2026-11-17): one for each name and kind, NeoForge's own (energy,
fluids, items; their blocks' proxyable), and their providers for blocks, block entity types and items, asked in the
order they were registered. RegisterCapabilitiesEvent is fired on the mod's bus once the mod is loaded
(HorizonBridge.capabilities); the bus (HorizonEventBus.post) gives an event to each listener of its class, a listener
given without its class having its lambda's, as NeoForge reads it. Not here: entities, a block entity found at a
position (a block capability asked without one), a block's item, cancelling an event, the @SubscribeEvent methods of
objects registered on a bus.

Texts (net/minecraft/network/chat, 2026-11-17): literal and translated texts, their styles (colour and formats), their
texts, equality and plain text (getString), observed; items' and blocks' names (description ids: a block's item's is
its block's, given with the game's items' components). A translation's text is the language's (Language), which holds
only what was given (HorizonLanguage): Horizon's language isn't reached yet, so a key not given says so. Not here: click
and hover events, fonts, the other kinds of contents (keybinds, scores, selectors, NBT), visiting a text, its JSON.

Tags (net/minecraft/tags, 2026-11-17): TagKey (one for each), what holds a value as `dump`'s facts list it, given by the
bridge (HorizonTags): every tag holding one of the mod's values, the "c" ones and the mod's own whole, the fluids' all
(the machines file's game_ids). A value of the game's in another of the game's tags can't be answered, and says so.
Holder's members (keys, isBound, the is() of each kind, getRegisteredName, getKey), observed. A holder's tags come in
their ids' order, where the game's change from run to run. Data maps (getData) say they aren't modelled.

Codecs (com/mojang/serialization, com/mojang/datafixers, 2026-11-18): DataFixerUpper's, built as the game builds them
and keeping what they are made of (HorizonCodecs: fields, lists, maps, dispatches, records of getters and their
constructor, which RecordCodecBuilder runs at once as the game does), their texts observed (a lambda's and an identity
hash's parts change from run to run); DataResult, Lifecycle and Codec.checkRange whole. They don't encode nor decode
yet: a call to that isn't translated. The network's codecs (StreamCodec, ByteBufCodecs) are objects that say the
network isn't modelled if run; the game's codec fields (ItemStack.CODEC...) are codecs of no structure, by name.

A mod's config (net/neoforged/neoforge/common/ModConfigSpec, 2026-11-18, decision 3): its values by path, read as
NeoForge reads them (before loading, an error; once read, the same value until the cache is cleared, set or not), each
its default once the bridge loads the configs the mod registered (ModConfigEvent.Loading fired for each, in their
order). Its file, `config/<modid>-<kind>.toml`, is NeoForge's (HorizonConfigToml: a new one's text as NeoForge
writes it, read and corrected as it reads it, a number out of range its nearest end, a list its valid elements):
Horizon writes the new file, reads it and hands its text over, again when it changes (API 397).

What isn't here, and what it costs: BlockPos's getCenter, betweenClosed and codecs, Direction's getRotation and
codecs, MutableBlockPos's other setters, NBT's codecs (NbtOps), its readers and writers (DataOutput, visitors),
TagType, ResourceLocation's codecs and its readers (StringReader), ByIdMap.sparse. A call to one isn't translated; and when a virtual call through a wider type (Object, List...) may reach
an object whose class lacks it, the methods that make such objects drop instead. A stand-in is worth as much as
the API it covers.
