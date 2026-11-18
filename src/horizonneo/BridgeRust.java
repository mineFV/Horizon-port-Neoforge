package horizonneo;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

/**
 * Horizon's side of the bridge (CLAUDE.md "Step (d)", decision 4): the Rust module a bundle's block-entity tick calls.
 * It keeps the mod's translated object for each position (made by the block's newBlockEntity, loaded from Horizon's
 * saved compound, given its ticker once), ticks it, and writes what the mod saves back into the compound. The object
 * is made again from the compound whenever the compound isn't the one this module last wrote (the world was
 * reloaded, another block took the place, something else changed it): Horizon's save stays the truth.
 *
 * Everything Java goes through HorizonBridge's functions in the translation (`functions` names them), one value at a
 * time; the NBT types are Horizon's (`horizon_world::nbt`).
 */
final class BridgeRust {
    private BridgeRust() {}

    static final String BRIDGE = "net/minecraft/world/level/block/entity/HorizonBridge";

    /** HorizonBridge's methods the module calls, by name and descriptor. */
    static final List<String[]> NEEDED = List.of(
            new String[] {"bus", "()Lnet/neoforged/bus/api/IEventBus;"},
            new String[] {"container", "(Ljava/lang/String;Ljava/lang/String;Ljava/lang/String;Lnet/neoforged/bus/api/IEventBus;)Lnet/neoforged/fml/ModContainer;"},
            new String[] {"known", "(Ljava/lang/String;Ljava/lang/String;)V"},
            new String[] {"capabilities", "(Lnet/neoforged/bus/api/IEventBus;Z)V"},
            new String[] {"configCount", "()I"},
            new String[] {"configKind", "(I)Ljava/lang/String;"},
            new String[] {"configFile", "(I)Ljava/lang/String;"},
            new String[] {"configText", "(I)Ljava/lang/String;"},
            new String[] {"configLoad", "(Ljava/lang/String;Ljava/lang/String;Z)Z"},
            new String[] {"make", "(Ljava/lang/String;III)Lnet/minecraft/world/level/block/entity/BlockEntity;"},
            new String[] {"ticker", "(Lnet/minecraft/world/level/block/entity/BlockEntity;)Lnet/minecraft/world/level/block/entity/BlockEntityTicker;"},
            new String[] {"tickWith", "(Lnet/minecraft/world/level/block/entity/BlockEntityTicker;Lnet/minecraft/world/level/block/entity/BlockEntity;)V"},
            new String[] {"load", "(Lnet/minecraft/world/level/block/entity/BlockEntity;Lnet/minecraft/nbt/CompoundTag;)V"},
            new String[] {"save", "(Lnet/minecraft/world/level/block/entity/BlockEntity;)Lnet/minecraft/nbt/CompoundTag;"},
            new String[] {"compound", "()Lnet/minecraft/nbt/CompoundTag;"},
            new String[] {"put", "(Lnet/minecraft/nbt/CompoundTag;Ljava/lang/String;Lnet/minecraft/nbt/Tag;)V"},
            new String[] {"byteTag", "(I)Lnet/minecraft/nbt/Tag;"},
            new String[] {"shortTag", "(I)Lnet/minecraft/nbt/Tag;"},
            new String[] {"intTag", "(I)Lnet/minecraft/nbt/Tag;"},
            new String[] {"longTag", "(J)Lnet/minecraft/nbt/Tag;"},
            new String[] {"floatTag", "(F)Lnet/minecraft/nbt/Tag;"},
            new String[] {"doubleTag", "(D)Lnet/minecraft/nbt/Tag;"},
            new String[] {"stringTag", "(Ljava/lang/String;)Lnet/minecraft/nbt/Tag;"},
            new String[] {"list", "()Lnet/minecraft/nbt/ListTag;"},
            new String[] {"add", "(Lnet/minecraft/nbt/ListTag;Lnet/minecraft/nbt/Tag;)Z"},
            new String[] {"bytes", "(I)[B"},
            new String[] {"setByte", "([BII)V"},
            new String[] {"byteArray", "([B)Lnet/minecraft/nbt/Tag;"},
            new String[] {"ints", "(I)[I"},
            new String[] {"setInt", "([III)V"},
            new String[] {"intArray", "([I)Lnet/minecraft/nbt/Tag;"},
            new String[] {"longs", "(I)[J"},
            new String[] {"setLong", "([JIJ)V"},
            new String[] {"longArray", "([J)Lnet/minecraft/nbt/Tag;"},
            new String[] {"kind", "(Lnet/minecraft/nbt/Tag;)I"},
            new String[] {"asInt", "(Lnet/minecraft/nbt/Tag;)I"},
            new String[] {"asLong", "(Lnet/minecraft/nbt/Tag;)J"},
            new String[] {"asFloat", "(Lnet/minecraft/nbt/Tag;)F"},
            new String[] {"asDouble", "(Lnet/minecraft/nbt/Tag;)D"},
            new String[] {"text", "(Lnet/minecraft/nbt/Tag;)Ljava/lang/String;"},
            new String[] {"size", "(Lnet/minecraft/nbt/Tag;)I"},
            new String[] {"keyAt", "(Lnet/minecraft/nbt/CompoundTag;I)Ljava/lang/String;"},
            new String[] {"get", "(Lnet/minecraft/nbt/CompoundTag;Ljava/lang/String;)Lnet/minecraft/nbt/Tag;"},
            new String[] {"at", "(Lnet/minecraft/nbt/Tag;I)Lnet/minecraft/nbt/Tag;"});

    /** The function each needed method is in the translation; null (with `missing` filled) when one isn't translated. */
    static Map<String, String> functions(JarRust.Result translation, List<String> missing) {
        Map<String, String> out = new TreeMap<>();
        if (!translation.rust().contains("pub fn jexport_mod(")) missing.add("the mod's own object (its one @Mod class, whose constructor is translated)");
        for (String[] m : NEEDED) {
            String f = translation.functions().get(new JarRust.Id(BRIDGE, m[0], m[1]));
            if (f == null) missing.add("HorizonBridge." + m[0]);
            else out.put(m[0], f);
        }
        return missing.isEmpty() ? out : null;
    }

    /**
     * The game's fluid ids, observed in NeoForge 21.1.266's fluid registry (`standins --horizon-src`, horizon-ids.txt):
     * `dump`'s facts don't have them.
     */
    static final String FLUIDS = "minecraft:empty,minecraft:flowing_lava,minecraft:flowing_water,minecraft:lava,minecraft:water";

    /** As module(java, info, f, given), the game's ids not given but its fluids': what needs them (ItemStack.parse) throws. */
    static String module(String java, ModInfo info, Map<String, String> f) {
        return module(java, info, f, Map.of(), false);
    }

    /**
     * The module's text. java: the translation's module, as the bridge module names it (`super::port_x_java`); info:
     * the mod's metadata; given: the game's ids, by registry, as `dump` read them (Machines.given); its fluids are
     * these observed (FLUIDS) when not given.
     */
    static String module(String java, ModInfo info, Map<String, String> f, Map<String, String> ids, boolean configFiles) {
        Map<String, String> all = new java.util.LinkedHashMap<>();
        all.put("fluid", FLUIDS);
        all.putAll(ids);
        StringBuilder given = new StringBuilder();
        for (Map.Entry<String, String> e : all.entrySet()) {
            if (given.length() > 0) given.append("\n    ");
            given.append("{known}(text(").append(Machines.rust(e.getKey())).append("), text(").append(Machines.rust(e.getValue())).append(")).map_err(|e| j::jdescribe(&e))?;");
        }
        String t = TEMPLATE.replace("{CONFIG_FILES}", String.valueOf(configFiles)).replace("{GIVEN}", given).replace("{JAVA}", java).replace("{MOD}", info.id())
                .replace("{MOD_ID_TEXT}", Machines.rust(info.id())).replace("{MOD_VERSION_TEXT}", Machines.rust(info.version()))
                .replace("{MOD_NAME_TEXT}", Machines.rust(info.name()));
        for (Map.Entry<String, String> e : f.entrySet()) t = t.replace("{" + e.getKey() + "}", "j::" + e.getValue());
        return t;
    }

    static final String TEMPLATE = """
            // Horizon's side of the bridge to the mod's translated Java, written by horizon-port-neoforge (BridgeRust):
            // each block entity of the mod's is an object of the translation, kept for its position, made by its block's
            // newBlockEntity and loaded from the compound Horizon saves; each tick runs the ticker its block gave, then
            // writes what the mod saves back into that compound. The object is made again from the compound when the
            // compound isn't the one written here last (a world loaded, another block in its place).
            use std::cell::RefCell;
            use std::collections::{BTreeSet, HashMap};
            use std::sync::mpsc::{Receiver, Sender, channel};
            use std::sync::{LazyLock, Mutex, MutexGuard};

            use horizon_world::nbt::{Compound, JavaString, Tag};

            use {JAVA} as j;

            struct Kept {
                id: String,
                object: j::Ref,
                ticker: j::Ref,
                /// The compound as written here last, and the keys the mod's own save gave.
                saved: Compound,
                keys: BTreeSet<JavaString>,
            }

            static KEPT: LazyLock<Mutex<HashMap<[i32; 3], Kept>>> = LazyLock::new(|| Mutex::new(HashMap::new()));
            static SAID: LazyLock<Mutex<BTreeSet<String>>> = LazyLock::new(|| Mutex::new(BTreeSet::new()));

            fn lock<T>(m: &Mutex<T>) -> MutexGuard<'_, T> {
                m.lock().unwrap_or_else(|e| e.into_inner())
            }

            /// A problem, said once.
            fn said(text: String) {
                if lock(&SAID).insert(text.clone()) {
                    eprintln!("[{MOD}] {}", text);
                }
            }

            fn text(s: &str) -> j::Ref {
                j::jbridge_string(&s.encode_utf16().collect::<Vec<u16>>())
            }

            /// The mod, loaded once as NeoForge loads it (its object made, given its event bus): its registrations
            /// happen then. What went wrong, if anything.
            /// Whether Horizon hands the mod its config files (`native_register_configs` and `native_config_loading`
            /// wired): else its configs are loaded as new ones, each value its default, with the mod.
            const CONFIG_FILES: bool = {CONFIG_FILES};

            static LOADED: LazyLock<Result<(), String>> = LazyLock::new(|| {
                {GIVEN}
                let bus = {bus}().map_err(|e| j::jdescribe(&e))?;
                let container = {container}(text({MOD_ID_TEXT}), text({MOD_VERSION_TEXT}), text({MOD_NAME_TEXT}), bus.clone()).map_err(|e| j::jdescribe(&e))?;
                j::jexport_mod(bus.clone(), container).map_err(|e| j::jdescribe(&e))?;
                {capabilities}(bus, !CONFIG_FILES).map_err(|e| j::jdescribe(&e))
            });

            // The mod's Java runs on a thread of its own. What it asks of the world (HorizonWorld.ask) goes to the thread
            // that ticks, which holds Horizon's world and answers, until the tick is done: no world is shared.

            /// What the Java thread tells the ticking thread: a question to the world, or the tick's end.
            enum Told {
                Ask(Vec<u16>),
                Done(Box<dyn std::any::Any + Send>),
            }

            type Job = Box<dyn FnOnce() -> Box<dyn std::any::Any + Send> + Send>;

            struct Thread {
                jobs: Sender<Job>,
                told: Receiver<Told>,
                answers: Sender<Option<Vec<u16>>>,
            }

            static THREAD: LazyLock<Mutex<Option<Thread>>> = LazyLock::new(|| Mutex::new(None));

            thread_local! {
                /// On the Java thread: where its questions go, and where their answers come from.
                static ASKING: RefCell<Option<(Sender<Told>, Receiver<Option<Vec<u16>>>)>> = const { RefCell::new(None) };
            }

            /// HorizonWorld.ask's answer (j::JWORLD): asked of the ticking thread; None on any other thread.
            fn ask(question: &[u16]) -> Option<Vec<u16>> {
                ASKING.with(|asking| {
                    let asking = asking.borrow();
                    let (told, answers) = asking.as_ref()?;
                    told.send(Told::Ask(question.to_vec())).ok()?;
                    answers.recv().ok().flatten()
                })
            }

            fn spawn() -> Option<Thread> {
                let (jobs, waiting) = channel::<Job>();
                let (telling, told) = channel::<Told>();
                let (answers, answered) = channel::<Option<Vec<u16>>>();
                let _ = j::JWORLD.set(ask);
                std::thread::Builder::new()
                    .name("{MOD} java".to_string())
                    .spawn(move || {
                        ASKING.with(|asking| *asking.borrow_mut() = Some((telling.clone(), answered)));
                        for job in waiting {
                            let done = job();
                            if telling.send(Told::Done(done)).is_err() {
                                break;
                            }
                        }
                    })
                    .ok()?;
                Some(Thread { jobs, told, answers })
            }

            /// One server tick of the block entity `id` at `pos`, which block `block` makes, saved as `nbt`: the compound
            /// to save in its place when the mod changed it. `world` answers what the mod's Java asks of the world.
            pub fn tick(block: &str, id: &str, pos: [i32; 3], nbt: &Compound, world: &mut dyn FnMut(&[u16]) -> Option<Vec<u16>>) -> Option<Compound> {
                let (block, id, nbt) = (block.to_string(), id.to_string(), nbt.clone());
                run(move || tick_here(&block, &id, pos, &nbt), world).flatten()
            }

            /// The configs the mod registers (ModContainer.registerConfig), each its kind ("common", "client", "server",
            /// "startup"), its file in `config/` and the text of a new one, as NeoForge writes it: the mod is loaded
            /// first, as when the game starts.
            pub fn configs() -> Vec<(String, String, String)> {
                run(configs_here, &mut |_| None).unwrap_or_default()
            }

            fn configs_here() -> Vec<(String, String, String)> {
                if let Err(e) = &*LOADED {
                    said(format!("the mod couldn't be loaded: {}", e));
                    return Vec::new();
                }
                let string = |r: j::JR<j::Ref>| r.ok().and_then(|s| j::jbridge_units(&s)).map(|u| String::from_utf16_lossy(&u));
                let n = match {configCount}() {
                    Ok(n) => n,
                    Err(e) => {
                        said(format!("the mod's configs couldn't be listed: {}", j::jdescribe(&e)));
                        return Vec::new();
                    }
                };
                let mut out = Vec::new();
                for i in 0..n {
                    match (string({configKind}(i)), string({configFile}(i)), string({configText}(i))) {
                        (Some(kind), Some(file), Some(text)) => out.push((kind, file, text)),
                        _ => said(format!("the mod's config {} couldn't be written", i)),
                    }
                }
                out
            }

            /// A config file of the mod's read (`reloading`: read again after it changed): the mod's values from its
            /// text, then ModConfigEvent.Loading (or Reloading) on its bus. False when the mod registers no such file.
            pub fn config(file: &str, content: &str, reloading: bool) -> bool {
                let (file, content) = (file.to_string(), content.to_string());
                run(move || config_here(&file, &content, reloading), &mut |_| None).unwrap_or(false)
            }

            fn config_here(file: &str, content: &str, reloading: bool) -> bool {
                if let Err(e) = &*LOADED {
                    said(format!("the mod couldn't be loaded: {}", e));
                    return false;
                }
                match {configLoad}(text(file), text(content), reloading) {
                    Ok(found) => found,
                    Err(e) => {
                        said(format!("its config {} couldn't be read: {}", file, j::jdescribe(&e)));
                        false
                    }
                }
            }

            /// `job` on the mod's Java thread, `world` answering what it asks: its result, or None when the thread stopped.
            fn run<R: Send + 'static>(job: impl FnOnce() -> R + Send + 'static, world: &mut dyn FnMut(&[u16]) -> Option<Vec<u16>>) -> Option<R> {
                let mut thread = lock(&THREAD);
                if thread.is_none() {
                    *thread = spawn();
                }
                let job: Job = Box::new(move || Box::new(job()) as Box<dyn std::any::Any + Send>);
                let result = match thread.as_ref() {
                    None => Err(()),
                    Some(t) => {
                        if t.jobs.send(job).is_err() {
                            Err(())
                        } else {
                            loop {
                                match t.told.recv() {
                                    Ok(Told::Ask(question)) => {
                                        if t.answers.send(world(&question)).is_err() {
                                            break Err(());
                                        }
                                    }
                                    Ok(Told::Done(done)) => break Ok(done),
                                    Err(_) => break Err(()),
                                }
                            }
                        }
                    }
                };
                match result {
                    Ok(done) => done.downcast::<R>().ok().map(|r| *r),
                    Err(()) => {
                        said("the mod's Java thread stopped: it is started again at the next call".to_string());
                        *thread = None;
                        None
                    }
                }
            }

            /// The tick itself, on the Java thread.
            fn tick_here(block: &str, id: &str, pos: [i32; 3], nbt: &Compound) -> Option<Compound> {
                if let Err(e) = &*LOADED {
                    said(format!("the mod couldn't be loaded: {}", e));
                    return None;
                }
                let mut kept = lock(&KEPT);
                let fresh = kept.get(&pos).map_or(true, |k| k.id != id || &k.saved != nbt);
                if fresh {
                    kept.remove(&pos);
                    match make(block, id, pos, nbt) {
                        Ok(Some(k)) => {
                            kept.insert(pos, k);
                        }
                        Ok(None) => {
                            said(format!("{} at {:?}: its block {} made no block entity", id, pos, block));
                            return None;
                        }
                        Err(e) => {
                            said(format!("{} at {:?} couldn't be made: {}", id, pos, j::jdescribe(&e)));
                            return None;
                        }
                    }
                }
                let k = kept.get_mut(&pos)?;
                if k.ticker.is_none() {
                    return None;
                }
                let run = {tickWith}(k.ticker.clone(), k.object.clone()).and_then(|()| {save}(k.object.clone())).and_then(|tag| from_java(&tag));
                let saved = match run {
                    Ok(Tag::Compound(c)) => c,
                    Ok(_) => return None,
                    Err(e) => {
                        // The game would stop the server with a crash report; here the block entity is made again
                        // from its compound at the next tick, and what it threw is said once.
                        said(format!("{} at {:?} threw: {}", id, pos, j::jdescribe(&e)));
                        kept.remove(&pos);
                        return None;
                    }
                };
                let mut next = nbt.clone();
                for key in &k.keys {
                    if !saved.contains_key(key) {
                        next.remove(key);
                    }
                }
                k.keys = saved.keys().cloned().collect();
                next.extend(saved);
                k.saved = next.clone();
                if &next != nbt {
                    Some(next)
                } else {
                    None
                }
            }

            /// The mod's block entity for this place, loaded from Horizon's compound (with its id and position, as the
            /// game's saved one has them), and the ticker its block gives it on the server.
            fn make(block: &str, id: &str, pos: [i32; 3], nbt: &Compound) -> j::JR<Option<Kept>> {
                let object = {make}(text(block), pos[0], pos[1], pos[2])?;
                if object.is_none() {
                    return Ok(None);
                }
                let mut whole = nbt.clone();
                whole.insert(JavaString::from("id"), Tag::String(JavaString::from(id)));
                whole.insert(JavaString::from("x"), Tag::Int(pos[0]));
                whole.insert(JavaString::from("y"), Tag::Int(pos[1]));
                whole.insert(JavaString::from("z"), Tag::Int(pos[2]));
                {load}(object.clone(), to_java(&Tag::Compound(whole))?)?;
                let ticker = {ticker}(object.clone())?;
                Ok(Some(Kept { id: id.to_string(), object, ticker, saved: nbt.clone(), keys: BTreeSet::new() }))
            }

            /// A tag of Horizon's as the mod's.
            pub fn to_java(tag: &Tag) -> j::JR<j::Ref> {
                Ok(match tag {
                    Tag::End => None,
                    Tag::Byte(v) => {byteTag}(*v as i32)?,
                    Tag::Short(v) => {shortTag}(*v as i32)?,
                    Tag::Int(v) => {intTag}(*v)?,
                    Tag::Long(v) => {longTag}(*v)?,
                    Tag::Float(bits) => {floatTag}(f32::from_bits(*bits))?,
                    Tag::Double(bits) => {doubleTag}(f64::from_bits(*bits))?,
                    Tag::String(s) => {stringTag}(j::jbridge_string(&s.0))?,
                    Tag::ByteArray(v) => {
                        let a = {bytes}(v.len() as i32)?;
                        for (i, b) in v.iter().enumerate() {
                            {setByte}(a.clone(), i as i32, *b as i8 as i32)?;
                        }
                        {byteArray}(a)?
                    }
                    Tag::IntArray(v) => {
                        let a = {ints}(v.len() as i32)?;
                        for (i, x) in v.iter().enumerate() {
                            {setInt}(a.clone(), i as i32, *x)?;
                        }
                        {intArray}(a)?
                    }
                    Tag::LongArray(v) => {
                        let a = {longs}(v.len() as i32)?;
                        for (i, x) in v.iter().enumerate() {
                            {setLong}(a.clone(), i as i32, *x)?;
                        }
                        {longArray}(a)?
                    }
                    Tag::List(items) => {
                        let list = {list}()?;
                        for item in items {
                            let element = to_java(item)?;
                            {add}(list.clone(), element)?;
                        }
                        list
                    }
                    Tag::Compound(c) => {
                        let compound = {compound}()?;
                        for (key, value) in c {
                            let element = to_java(value)?;
                            {put}(compound.clone(), j::jbridge_string(&key.0), element)?;
                        }
                        compound
                    }
                })
            }

            /// A tag of the mod's as Horizon's.
            pub fn from_java(tag: &j::Ref) -> j::JR<Tag> {
                if tag.is_none() {
                    return Ok(Tag::End);
                }
                let units = |s: j::Ref| JavaString(j::jbridge_units(&s).unwrap_or_default());
                Ok(match {kind}(tag.clone())? {
                    1 => Tag::Byte({asInt}(tag.clone())? as i8),
                    2 => Tag::Short({asInt}(tag.clone())? as i16),
                    3 => Tag::Int({asInt}(tag.clone())?),
                    4 => Tag::Long({asLong}(tag.clone())?),
                    5 => Tag::Float({asFloat}(tag.clone())?.to_bits()),
                    6 => Tag::Double({asDouble}(tag.clone())?.to_bits()),
                    7 => {
                        let n = {size}(tag.clone())?;
                        let mut v = Vec::with_capacity(n as usize);
                        for i in 0..n {
                            v.push({asInt}({at}(tag.clone(), i)?)? as u8);
                        }
                        Tag::ByteArray(v)
                    }
                    8 => Tag::String(units({text}(tag.clone())?)),
                    9 => {
                        let n = {size}(tag.clone())?;
                        let mut v = Vec::with_capacity(n as usize);
                        for i in 0..n {
                            v.push(from_java(&{at}(tag.clone(), i)?)?);
                        }
                        Tag::List(v)
                    }
                    10 => {
                        let n = {size}(tag.clone())?;
                        let mut c = Compound::new();
                        for i in 0..n {
                            let key = {keyAt}(tag.clone(), i)?;
                            let value = from_java(&{get}(tag.clone(), key.clone())?)?;
                            c.insert(units(key), value);
                        }
                        Tag::Compound(c)
                    }
                    11 => {
                        let n = {size}(tag.clone())?;
                        let mut v = Vec::with_capacity(n as usize);
                        for i in 0..n {
                            v.push({asInt}({at}(tag.clone(), i)?)?);
                        }
                        Tag::IntArray(v)
                    }
                    12 => {
                        let n = {size}(tag.clone())?;
                        let mut v = Vec::with_capacity(n as usize);
                        for i in 0..n {
                            v.push({asLong}({at}(tag.clone(), i)?)?);
                        }
                        Tag::LongArray(v)
                    }
                    _ => Tag::End,
                })
            }
            """;
}
