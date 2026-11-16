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

/// A made-up world: stone below y = 2, air above; how many questions it was asked.
static ASKED: std::sync::atomic::AtomicUsize = std::sync::atomic::AtomicUsize::new(0);

fn world(question: &[u16]) -> Option<Vec<u16>> {
    ASKED.fetch_add(1, std::sync::atomic::Ordering::Relaxed);
    let q = String::from_utf16_lossy(question);
    let words: Vec<&str> = q.split(' ').collect();
    match words.as_slice() {
        ["block", _, y, _] => Some((if y.parse::<i32>().ok()? < 2 { "minecraft:stone" } else { "minecraft:air" }).encode_utf16().collect()),
        _ => None,
    }
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
    println!("asked: {}", ASKED.load(std::sync::atomic::Ordering::Relaxed));
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
}
