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
maps equipment slots (net/minecraft/world/entity/EquipmentSlot, but limit), points in doubles (net/minecraft/world/phys/Vec3, and Position) and Mth's arithmetic (not its atan2, which the game reads from tables built with Math.asin
and Math.cos, nor its random numbers). They cover what Mekanism calls of them, but for the members below.

The game's sine (Mth.sin and Mth.cos, 2026-11-24) and the points turned about an axis with it (Vec3's xRot, yRot,
zRot): its table, each of the 65536 entries the sine of i / 10430.378350470453 rounded to a double then a float, as
Horizon builds it (horizon_core::geometry::trig, MIT), the sine Horizon's correctly rounded one
(horizon_core::jmath::exact, MIT) in Java's arithmetic (net/minecraft/util/HorizonSine), so that the JVM and the
translation make the same table; observed equal to the game's, entry by entry (HotSpot's Math.sin, which the game
builds it with, is off by a bit of a double for a few arguments in a thousand, which a float doesn't keep). Which
entry an angle reads isn't Horizon's: 1.21.1 multiplies the float angle by 10430.378F in floats and cuts it to an
int (Minecraft 26.3, which Horizon follows, does it in doubles cut to a long), observed on the angles on either side
of every entry's edge, past the ints' range, NaN, infinities and 700 000 angles at random. A rotation widens the float
cosine and sine as Horizon's Vec3 does, observed.

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

fastutil (it/unimi/dsi/fastutil, 2026-11-18, decision 5): ObjectOpenHashSet and Object2ObjectOpenHashMap, their open
hash tables as fastutil's (each key at its mixed hash or the next free slot, removals shifting back, growing and
shrinking at its load factor), gone through from the last slot to the first, the null key first, and a removal while
iterating as fastutil's; their texts, hashes, views and Object2ObjectMap's defaults. Observed against fastutil itself.
The same for chars (CharOpenHashSet, and Char2ObjectArrayMap's array) and longs (LongOpenHashSet,
Long2ObjectOpenHashMap, the key 0 kept apart as fastutil keeps it), and for keys by identity (Reference2ObjectOpenHashMap,
2026-11-24: ==, and System.identityHashCode where the object map takes hashCode). Identity hashes change from run to
run, and a translated program's are its own: the probe observes the map by identity against the object map on keys
whose hash is their identity's (same texts, orders, hashes, removals while iterating), and by what no hash decides.

IntArrayList (2026-11-24): fastutil's list of ints and what it is made of (AbstractIntList, AbstractIntCollection,
IntList, IntCollection, IntIterable, IntStack, their iterators and spliterator, Stack, BidirectionalIterator), as a
List of Integers too: its array growing by half (a list made without a capacity starts at 10, fastutil's two empty
arrays told apart), errors and their texts, boxed methods (an Object that isn't an Integer is a ClassCastException,
null is in no list), its list iterator (fastutil 8.5.12's back(n), which measures how far it may go from the end:
at the end it goes back to the start and says 0, near the start it goes before it), its late-binding spliterator
splitting in halves, texts, hashes, equality and order with any list, stable sorts. Observed against fastutil, with
the JDK's own ListIterator, PrimitiveIterator and Spliterator.OfInt, which the model now has too (lib/README.md).
What differs: a part of a list as a view (subList) refuses but for the whole list (which fastutil gives as the list
itself); sorting with a comparator gives fastutil's result (any stable sort does, for a consistent comparator), but
not its pairs compared, and a comparator that throws leaves the list as it was; a subclass's clone refuses; fastutil's
own AbstractIntList's iterator, for a list of a mod's that extends it, isn't fastutil's class. Not here: the elements
in bulk (getElements, setElements...), IntComparator, IntConsumer and IntPredicate of fastutil's, replaceAll and
unstableSort of its own, other kinds of lists.

Its other collections (other primitive keys, linked and sorted ones) aren't here yet.

Random numbers (net/minecraft/util/RandomSource, net/minecraft/world/level/levelgen, 2026-11-22): the Java generator
(LegacyRandomSource, BitRandomSource's draws), after Horizon's `horizon_core::random`, and RandomSource.create's
seed from a shared counter and the clock (RandomSupport.generateUniqueSeed), after Horizon's
`horizon-client-render/src/random_source.rs`. Observed: which class each gives, the numbers drawn on seeds and bounds
chosen to pin them down, the errors and that an error draws nothing, forks and reseeding. nextGaussian refuses (it
needs Java's Math.log to the last bit). Not here: forkPositional, the thread-safe and thread-local generators,
Xoroshiro, WorldgenRandom.

Chunks (net/minecraft/world/level/chunk, 2026-11-22): whether a level has a chunk loaded (hasChunk), asked of Horizon's
world, which answers the game's hasChunk with the chunks it holds (`game/raids.rs`); ChunkStatus.FULL, its name, text
and index observed in a server; a level's getChunk null for a whole chunk not loaded and not made, as the game gives
it (observed in a server). A chunk as an object (ChunkAccess, a type here) isn't modelled: getChunk refuses where the
game would give one, and the other statuses aren't here. A block's stack for a player's pick (getCloneItemStack: a new
stack of its item; NeoForge's, with a hit and a player, running the block's own) and a state's handleNeighborChanged
(its block's neighborChanged, which does nothing unless the block says otherwise), observed in a server with blocks
of the probe's that record what they are given: code that asks whether a chunk is loaded reaches them, and while one
was missing every maker of a block or a state dropped.

The game's folders (net/neoforged/fml/loading/FMLPaths, 2026-11-23): under the one the server runs in, as Horizon's
game folder is by default: GAMEDIR the current directory, MODSDIR, CONFIGDIR and FMLCONFIG under it, and
getOrCreateGameRelativePath making a folder there, observed in a server. DimensionType's height limits (MAX_Y, MIN_Y
and the others), observed in a server; not a dimension's own type.

NeoForge's common tags (net/neoforged/neoforge/common/Tags, 2026-11-23): the keys of those a mod names (Mekanism's:
ores and three of them, two fluids' and one entity types'), their ids and registries observed in a server, each the one
key for its id. Not DamageTypes, whose type isn't modelled.

Guava (com/google/common/collect, Guava 32.1.2, the one the server runs; its installer's Guava 20 beside it under
libraries/ isn't loaded, though the probe ran on it until 2026-11-23): ImmutableList and its builder (2026-11-20, its
builder Guava 32's since: several elements checked before any is added); Table
(2026-11-23): HashBasedTable, rows and columns in the order they came, a row gone with its last cell, null keys and
values refused and asked of with null answering nothing, its views (cellSet, values) made once and removing through
their iterators, its cells (Tables.immutableCell), texts, equality and hash. Observed against Guava itself; each
class has Guava's name, supertypes and declared methods (AbstractTable, StandardTable, their views, TransformedIterator),
as the check against the server's classes asks. A row's view refuses; Tables' other tables aren't here.

Particles (net/minecraft/core/particles, 2026-11-23): the kinds a mod names (Mekanism's: smoke, portal and redstone's
dust), one object each, whether they ignore the particle limit, dust's color and its scale clamped between its bounds,
observed in a server. Not their codecs, nor the particle registry.

Blocks' sounds and stats (2026-11-23): the sound types a mod names (Mekanism's: sand, grass, deepslate), their volume,
pitch and sounds (SoundEvents': ids, ranges, one object each), Properties.sound kept, and the custom stats' ids
(opening a chest, a barrel), observed in a server. Properties.ofLegacyCopy: a copy of a block's properties, whose
states answer as the block's do (observed with the probe's blocks); a copy of a block of the game's (but air) isn't
known either, and its states refuse.

Voxel shapes (net/minecraft/world/phys/shapes, 2026-11-23): VoxelShape, ArrayVoxelShape and CubeVoxelShape (the class
a shape is of, the game's), Shapes' empty shape and block, boxes made shapes (thin ones nothing, those on a grid of
up to 1/8 a cube's, the others an array shape), joins by any BooleanOp with the game's merged boundaries, optimize,
or, joinIsNotEmpty, moving, min, max, bounds, the greedy boxes forAllBoxes and toAabbs give, and Block.box; Rotation
(composed, turning a direction or an index). After Horizon's horizon_core::geometry::shape (MIT, tested against
vanilla there), observed against the game on 1351 shapes and joins (980 joins of 14 shapes by five operations, 300
unions drawn at random and turned as Mekanism turns its blocks' shapes). getFaceShape and clip refuse; the edges,
collide and the faces' occlusion aren't here; a state's shapes asked with a CollisionContext (a type) refuse, as
its others do. WorldGenerationContext refuses: Horizon generates the world.

Registering by a function of the value's id (2026-11-23): DeferredRegister.register(String, Function), which its
register by a supplier hands on to (a subclass overriding it sees that, as Mekanism's registers do), the function
given the value's id when the value is made, a name given twice refused; observed in a server.

Lists and model keys (2026-11-23): NonNullList, a list that refuses null where it is given a value, made empty, with
a capacity, of a size filled with a default, on the elements given (their own array) or as a copy that can't be
changed, clear putting the default back; NeoForge's ModelProperty, a predicate (true of everything when made without
one). Observed, as a library and in a server.

Bows (2026-11-23): BowItem and ProjectileWeaponItem as items made from their properties, their constructors adding
nothing to them (observed in a server: a bow's components are a plain item's with the same properties). Using them
(drawing, shooting) isn't here.

A capability's cache at a position (2026-11-23): BlockCapabilityCache made as NeoForge makes it, what it is given kept
(the position as an immutable copy), null refused; observed in a server's overworld. Asking it refuses: a level's
capabilities at a position (its block entities) aren't modelled, nor when they change.

What isn't here, and what it costs: BlockPos's getCenter, betweenClosed and codecs, Direction's getRotation and
codecs, MutableBlockPos's other setters, NBT's codecs (NbtOps), its readers and writers (DataOutput, visitors),
TagType, ResourceLocation's codecs and its readers (StringReader), ByIdMap.sparse. A call to one isn't translated; and when a virtual call through a wider type (Object, List...) may reach
an object whose class lacks it, the methods that make such objects drop instead. A stand-in is worth as much as
the API it covers.
