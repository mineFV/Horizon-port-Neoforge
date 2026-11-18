// Runs the bridge (BridgeRust's bridge.rs) on the translation of the made-up Counter mod (testmod/counter), as
// Horizon's block-entity tick would: TRANSLATED_DIR holds translated.rs and bridge.rs (written with --bridge super::java).
use horizon_world::nbt::{Compound, JavaString, Tag};

#[allow(dead_code, unused_mut, unused_variables, unused_parens, unused_assignments, unreachable_code, non_snake_case, clippy::all)]
mod java {
    include!(concat!(env!("TRANSLATED_DIR"), "/translated.rs"));
}

mod bridge {
    include!(concat!(env!("TRANSLATED_DIR"), "/bridge.rs"));
}

fn shown(c: &Compound) -> String {
    let mut parts = Vec::new();
    for (k, v) in c {
        parts.push(format!("{}={:?}", String::from_utf16_lossy(&k.0), v));
    }
    parts.join(" ")
}

/// A made-up world: stone below y = 2, air above, the Counter block at the positions ticked, with its `lit` kept;
/// how many questions it was asked, and how many times it was lit.
static ASKED: std::sync::atomic::AtomicUsize = std::sync::atomic::AtomicUsize::new(0);
static LIT: std::sync::Mutex<Option<std::collections::HashMap<(i32, i32, i32), bool>>> = std::sync::Mutex::new(None);
static LIT_ON: std::sync::atomic::AtomicUsize = std::sync::atomic::AtomicUsize::new(0);
const COUNTERS: [(i32, i32, i32); 2] = [(1, 2, 3), (4, 5, 6)];

fn world(question: &[u16]) -> Option<Vec<u16>> {
    ASKED.fetch_add(1, std::sync::atomic::Ordering::Relaxed);
    let q = String::from_utf16_lossy(question);
    let words: Vec<&str> = q.split(' ').collect();
    let mut lit = LIT.lock().unwrap();
    let lit = lit.get_or_insert_with(std::collections::HashMap::new);
    let answer = match words.as_slice() {
        ["block", x, y, z] => {
            let at = (x.parse::<i32>().ok()?, y.parse::<i32>().ok()?, z.parse::<i32>().ok()?);
            if COUNTERS.contains(&at) {
                format!("counter:counter[lit={}]", lit.get(&at).copied().unwrap_or(false))
            } else if at.1 < 2 {
                "minecraft:stone".to_string()
            } else {
                "minecraft:air".to_string()
            }
        }
        ["set", x, y, z, "lit", value, "3"] => {
            let at = (x.parse::<i32>().ok()?, y.parse::<i32>().ok()?, z.parse::<i32>().ok()?);
            let on = *value == "true";
            let was = lit.insert(at, on).unwrap_or(false);
            if on && !was {
                LIT_ON.fetch_add(1, std::sync::atomic::Ordering::Relaxed);
            }
            (was != on).to_string()
        }
        _ => return None,
    };
    Some(answer.encode_utf16().collect())
}

fn ticks(at: [i32; 3], n: usize, nbt: &mut Compound) -> usize {
    let mut changed = 0;
    for _ in 0..n {
        if let Some(next) = bridge::tick("counter:counter", "counter:counter", at, nbt, &mut world) {
            *nbt = next;
            changed += 1;
        }
    }
    changed
}

fn main() {
    // A new block entity: 45 ticks, saved every tick (its ticks change each time).
    let mut nbt = Compound::new();
    nbt.insert(JavaString::from("horizon"), Tag::Int(7));
    let changed = ticks([1, 2, 3], 45, &mut nbt);
    println!("new: {} changes; {}", changed, shown(&nbt));
    // The world reloaded elsewhere: a block entity loaded from that compound goes on counting.
    let mut moved = nbt.clone();
    ticks([4, 5, 6], 25, &mut moved);
    println!("loaded: {}", shown(&moved));
    // Its compound changed by something else: made again from it.
    let mut edited = nbt.clone();
    edited.insert(JavaString::from("ticks"), Tag::Int(1000));
    ticks([1, 2, 3], 5, &mut edited);
    println!("edited: {}", shown(&edited));
    println!("asked: {}; lit {} times; lit now at (1, 2, 3): {:?}", ASKED.load(std::sync::atomic::Ordering::Relaxed), LIT_ON.load(std::sync::atomic::Ordering::Relaxed),
        LIT.lock().unwrap().as_ref().and_then(|m| m.get(&(1, 2, 3)).copied()));
    // Every kind of tag, to the mod's and back.
    let mut all = Compound::new();
    all.insert(JavaString::from("b"), Tag::Byte(-3));
    all.insert(JavaString::from("s"), Tag::Short(-300));
    all.insert(JavaString::from("i"), Tag::Int(70000));
    all.insert(JavaString::from("l"), Tag::Long(1 << 40));
    all.insert(JavaString::from("f"), Tag::Float(2.5f32.to_bits()));
    all.insert(JavaString::from("d"), Tag::Double((-1e10f64).to_bits()));
    all.insert(JavaString(vec![0xD800, 0x61]), Tag::String(JavaString(vec![0xDC00, 0x7A])));
    all.insert(JavaString::from("ba"), Tag::ByteArray(vec![0, 255, 128]));
    all.insert(JavaString::from("ia"), Tag::IntArray(vec![i32::MIN, 3]));
    all.insert(JavaString::from("la"), Tag::LongArray(vec![i64::MAX]));
    all.insert(JavaString::from("li"), Tag::List(vec![Tag::Int(1), Tag::Int(2)]));
    let mut inner = Compound::new();
    inner.insert(JavaString::from("x"), Tag::List(vec![]));
    all.insert(JavaString::from("c"), Tag::Compound(inner));
    let back = bridge::to_java(&Tag::Compound(all.clone())).and_then(|j| bridge::from_java(&j));
    println!("round trip: {}", match back { Ok(Tag::Compound(c)) => (c == all).to_string(), other => format!("{:?}", other.err().map(|e| java::jdescribe(&e))) });
    // Its config files, as Horizon hands them over (API 397): listed with the text of a new one; read again with
    // another value, the mod counts its seconds by it; a file that isn't the mod's is said not to be.
    for (kind, file, text) in bridge::configs() {
        println!("config: {} {} {:?}", kind, file, text);
    }
    let found = bridge::config("counter-common.toml", "ticks_per_second = 10\n", true);
    let missing = bridge::config("nope.toml", "", false);
    let mut fast = Compound::new();
    ticks([7, 8, 9], 45, &mut fast);
    println!("reloaded: {} {}; {}", found, missing, shown(&fast));
}
