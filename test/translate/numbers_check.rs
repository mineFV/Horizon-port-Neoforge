// Runs the translation of `Numbers` on the inputs `NumbersCheck.java` gives the Java, and prints the results the
// same way: the two outputs must be the same. The translation is put in here by the test (`translated.rs`).
include!(concat!(env!("TRANSLATED_DIR"), "/translated.rs"));

const INTS: [i32; 13] = [0, 1, -1, 2, 7, 10, 11, -100, 1000000, 12345, -98765, i32::MAX, i32::MIN];
const LONGS: [i64; 9] = [0, 1, -1, 27, 97, 1 << 40, -(1 << 50) + 3, i64::MAX, i64::MIN];

fn floats() -> Vec<f32> {
    vec![0.0, -0.0, 1.5, -1.5, 2.5, -2.5, 0.49999997, 3.4e38, -7.25, f32::NAN, f32::INFINITY, f32::NEG_INFINITY, 1e-40]
}

fn doubles() -> Vec<f64> {
    vec![0.0, -0.0, 1.5, -2.5, 0.49999999999999994, 1e300, -3.75, 4.5e15, f64::NAN, f64::INFINITY, f64::NEG_INFINITY, 9.2e18, -9.3e18]
}

/// Java's floatToIntBits and doubleToLongBits: one NaN for all.
fn fb(x: f32) -> i32 {
    if x.is_nan() { 0x7fc00000 } else { x.to_bits() as i32 }
}

fn db(x: f64) -> i64 {
    if x.is_nan() { 0x7ff8000000000000 } else { x.to_bits() as i64 }
}

fn f(x: f32) -> String {
    format!("{:x}", fb(x))
}

fn d(x: f64) -> String {
    format!("{:x}", db(x))
}

/// The result, or "throws" and the exception, as `jdescribe` gives it.
fn show<T: std::fmt::Display>(r: JR<T>) -> String {
    match r {
        Ok(v) => v.to_string(),
        Err(e) => format!("throws {}", jdescribe(&e)),
    }
}

/// A text result: its characters, "null", or what it throws.
fn text(r: JR<Ref>) -> String {
    match r {
        Ok(None) => "null".to_string(),
        Ok(s) => jstring_text(&s),
        Err(e) => format!("throws {}", jdescribe(&e)),
    }
}

fn p(name: &str, args: String, result: String) {
    println!("{} {} -> {}", name, args, result);
}

/// NumberFormat's inputs, as NumbersCheck.formatDoubles makes them.
fn format_doubles() -> Vec<f64> {
    let mut out = vec![0.0, -0.0, 0.5, -0.5, 1.5, 2.5, -2.5, 3.5, 0.0005, -0.0005, 0.0015, 0.0025, 2.675, 1.0005, 9.9995, 999.9995,
        -999.9995, 1234.5678, -1234567.891, 0.001, 0.1234567, 1e-9, 1e-300, 4.9e-324, 123456789012.345, 9.999999e15,
        9.999999999999998e15, 0.49999999999999994, 1e15 + 0.5, f64::NAN, f64::INFINITY, f64::NEG_INFINITY];
    let tens = [1e-4, 1e-3, 1e-2, 1e-1, 1.0, 1e1, 1e2, 1e3, 1e4, 1e5, 1e6, 1e7, 1e8, 1e9, 1e10, 1e11, 1e12, 1e13, 1e14, 1e15];
    let mut x: i64 = 0x9E3779B97F4A7C15u64 as i64;
    for i in 0..3000 {
        x = x.wrapping_mul(6364136223846793005).wrapping_add(1442695040888963407);
        let d = if i % 3 == 0 {
            ((x as u64) >> 11) as f64 * (1.0 / 9007199254740992.0) * tens[(((x as u64) >> 3) % 20) as usize]
        } else if i % 3 == 1 {
            ((x >> 20) % 2000001) as f64 / 2000.0
        } else {
            ((x >> 20) % 20001) as f64 / 2.0 + ((x & 3) - 1) as f64 * 1e-12
        };
        out.push(if x & 4 != 0 { -d } else { d });
    }
    out
}

/// NumberFormat's long inputs, as NumbersCheck.formatLongs makes them.
fn format_longs() -> Vec<i64> {
    let mut out = vec![0, 1, -1, 7, 12, 123, 1234, -12345, 123456, -1234567, 9876543210, 1000, 999999, 1000000, i64::MAX, i64::MIN];
    let mut x: i64 = 0x2545F4914F6CDD1D;
    for i in 0..200 {
        x = x.wrapping_mul(6364136223846793005).wrapping_add(1442695040888963407);
        out.push(x >> (i % 64));
    }
    out
}

fn main() {
    if std::env::args().nth(1).as_deref() == Some("refusals") {
        // What the translation alone runs: a lambda whose method isn't translated refuses (Java runs it).
        p("lazyRun", String::new(), text(lazy__run()));
        return;
    }
    if std::env::args().nth(1).as_deref() == Some("paths") {
        // Paths that aren't ASCII, which depend on the system's charset: run by the test in several environments.
        p("unicodePaths", String::new(), text(folders__unicode()));
        return;
    }
    if std::env::args().nth(1).as_deref() == Some("default") {
        // The default locale's number formats: run by the test in several environments (lib/README.md).
        for (i, d0) in format_doubles().into_iter().enumerate() {
            p("defaultFormat", i.to_string(), text(formats__default_double(d0)));
        }
        for (i, v) in format_longs().into_iter().enumerate() {
            p("defaultLong", i.to_string(), text(formats__default_long(v)));
        }
        p("defaultFacts", String::new(), text(formats__default_facts()));
        return;
    }
    for &a in INTS.iter() {
        for &b in INTS.iter() {
            p("mix", format!("{} {}", a, b), show(numbers__mix(a, b)));
            p("div", format!("{} {}", a, b), show(numbers__div(a, b)));
            if b != 0 {
                p("floorMod", format!("{} {}", a, b), show(numbers__floor_mod(a, b)));
            }
        }
        for s in [0, 1, 5, 31, 32, 33, 63, 64, 65, -1] {
            p("shl", format!("{} {}", a, s), show(numbers__shl(a, s)));
        }
        p("neg", a.to_string(), show(numbers__neg(a)));
        p("sign", a.to_string(), show(numbers__sign(a)));
        p("incBoth", a.to_string(), show(numbers__inc_both(a)));
        p("days", a.to_string(), show(numbers__days(a)));
        p("sparse", a.to_string(), show(numbers__sparse(a)));
        p("toByte", a.to_string(), show(numbers__to_byte(a)));
        p("toChar", a.to_string(), show(numbers__to_char(a).map(|c| c as i32)));
        p("toShort", a.to_string(), show(numbers__to_short(a)));
        p("iabs", a.to_string(), show(numbers__iabs(a)));
        p("twice", a.to_string(), show(numbers__twice(a)));
        p("pick", a.to_string(), show(numbers__pick(a > 0, a, a.wrapping_neg())));
        p("isDigit", a.to_string(), show(numbers__is_digit((a & 0xFFFF) as u16)));
    }
    for m in 0..=13 {
        p("days", m.to_string(), show(numbers__days(m)));
    }
    for n in 0..=25 {
        p("sumTo", n.to_string(), show(numbers__sum_to(n * 1000)));
        p("fact", n.to_string(), show(numbers__fact(n)));
    }
    for c in 40..=60 {
        p("isDigit", c.to_string(), show(numbers__is_digit(c as u16)));
    }
    for &a in LONGS.iter() {
        for s in [0, 1, 13, 63, 64, 70] {
            p("lmix", format!("{} {}", a, s), show(numbers__lmix(a, s)));
            p("ushr", format!("{} {}", a, s), show(numbers__ushr(a, s)));
        }
        for &b in LONGS.iter() {
            p("cmpL", format!("{} {}", a, b), show(numbers__cmp_l(a, b)));
            if b != 0 {
                p("floorDiv", format!("{} {}", a, b), show(numbers__floor_div(a, b)));
            }
        }
        p("collatz", a.to_string(), show(numbers__collatz(a)));
        p("l2f", a.to_string(), show(numbers__l2f(a).map(fb)));
    }
    for a in floats() {
        for b in floats() {
            p("less", format!("{} {}", f(a), f(b)), show(numbers__less(a, b)));
            p("frem", format!("{} {}", f(a), f(b)), show(numbers__frem(a, b).map(fb)));
            p("fmin", format!("{} {}", f(a), f(b)), show(numbers__fmin(a, b).map(fb)));
        }
        p("f2i", f(a), show(numbers__f2i(a)));
        p("roundF", f(a), show(numbers__round_f(a)));
    }
    for a in doubles() {
        for b in doubles() {
            p("atLeast", format!("{} {}", d(a), d(b)), show(numbers__at_least(a, b)));
            p("clamp", format!("{} {}", d(a), d(b)), show(numbers__clamp(a, b, 10.0).map(db)));
        }
        p("poly", d(a), show(numbers__poly(a).map(db)));
        p("d2l", d(a), show(numbers__d2l(a)));
        p("roundD", d(a), show(numbers__round_d(a)));
        p("sig", d(a), show(numbers__sig(a).map(db)));
    }
    // Objects: built and answered with numbers inside Shapes.
    for n in 0..=12 {
        p("areas", n.to_string(), show(shapes__areas(n)));
        p("perimeters", n.to_string(), show(shapes__perimeters(n)));
        p("made", n.to_string(), show(shapes__made(n)));
        p("list", n.to_string(), show(shapes__list(n)));
        p("statics", n.to_string(), show(shapes__statics(n)));
        p("lambdas", n.to_string(), show(shapes__lambdas(n)));
        p("casts", n.to_string(), show(shapes__casts(n)));
        p("arrays", n.to_string(), show(shapes__arrays(n)));
        p("same", n.to_string(), show(shapes__same(n)));
        p("callNever", n.to_string(), show(shapes__call_never(None, n)));
    }
    // The game's positions and directions, through the translator's stand-ins (game/).
    for a in INTS {
        p("packed", a.to_string(), show(places__packed(a, a >> 1, a.wrapping_neg())));
        p("eastX", a.to_string(), show(places__east_x(a)));
        for b in INTS {
            p("compare", format!("{} {}", a, b), show(places__compare(a, b)));
        }
    }
    // Horizon's own test of the packed form (position.rs, packed_signed_limits_and_java_overflow).
    p("packed", "limits".to_string(), show(places__packed(-33554432, -2048, 33554431)));
    for l in LONGS {
        let (x, y, z) = (show(places__unpacked_x(l)), show(places__unpacked_y(l)), show(places__unpacked_z(l)));
        p("unpacked", l.to_string(), format!("{} {} {}", x, y, z));
        for d in 0..=7 {
            p("walk", format!("{} {}", l, d), show(places__walk(l, d)));
        }
    }
    for n in 0..=12 {
        p("keys", n.to_string(), text(places__keys(n)));
    }
    for n in -8..=8 {
        p("more", n.to_string(), text(places__more(n)));
    }
    for n in 0..=20 {
        p("nbt", n.to_string(), text(places__nbt(n)));
    }
    for n in -10..=10 {
        p("ids", n.to_string(), text(places__ids(n)));
    }
    for n in -12..=12 {
        p("mth", n.to_string(), text(places__mth(n)));
    }
    for n in -9..=9 {
        p("vec", n.to_string(), text(places__vec(n)));
    }
    for d in 0..=5 {
        p("opposite", d.to_string(), show(places__opposite(d)));
        p("steps", d.to_string(), show(places__steps(d)));
        p("nameLength", d.to_string(), show(places__name_length(d)));
        p("sorted", d.to_string(), text(places__sorted(d)));
    }
    // Exceptions and texts.
    for n in 0..=12 {
        p("divide", n.to_string(), text(texts__divide(10, n - 3)));
        p("finallyCounts", n.to_string(), show(texts__finally_counts(n)));
        p("finallyReturns", n.to_string(), show(texts__finally_returns(n)));
        p("jvm", n.to_string(), text(texts__jvm(n)));
        p("uncaught", n.to_string(), show(texts__uncaught(n)));
        p("causes", n.to_string(), text(texts__causes(n)));
        p("initializers", n.to_string(), text(texts__initializers(n)));
        p("rethrow", n.to_string(), text(texts__rethrow(n)));
        p("join", n.to_string(), text(texts__join(n)));
        p("literals", n.to_string(), text(texts__literals(n)));
        p("methods", n.to_string(), text(texts__methods(n)));
        p("outOfBounds", n.to_string(), text(texts__out_of_bounds(n - 2)));
    }
    // Records, enums, boxes, Objects, java.util.function; collections and Optional.
    for n in 0..=12 {
        p("records", n.to_string(), text(boxes__records(n)));
        p("enums", n.to_string(), text(boxes__enums(n)));
        p("boxes", n.to_string(), text(boxes__boxes(n)));
        p("bits", n.to_string(), text(boxes__bits(n)));
        p("objects", n.to_string(), text(boxes__objects(n)));
        p("functions", n.to_string(), text(boxes__functions(n)));
        p("lists", n.to_string(), text(gathered__lists(n)));
        p("maps", n.to_string(), text(gathered__maps(n)));
        p("sets", n.to_string(), text(gathered__sets(n)));
        p("immutables", n.to_string(), text(gathered__immutables(n)));
        p("optionals", n.to_string(), text(gathered__optionals(n)));
        p("arrayHelpers", n.to_string(), text(gathered__arrays(n)));
        p("builders", n.to_string(), text(gathered__builders(n)));
        p("classes", n.to_string(), text(gathered__classes(n)));
        p("enumCollections", n.to_string(), text(gathered__enum_collections(n)));
        p("streams", n.to_string(), text(gathered__streams(n)));
        p("misc", n.to_string(), text(gathered__misc(n)));
        p("patterns", n.to_string(), text(gathered__patterns(n)));
        p("monitors", n.to_string(), text(gathered__monitors(n)));
        p("queues", n.to_string(), text(gathered__queues(n)));
        p("bytes", n.to_string(), text(gathered__bytes(n)));
        p("randoms", n.to_string(), text(gathered__randoms(n)));
        p("trees", n.to_string(), text(gathered__trees(n)));
        p("threads", n.to_string(), text(gathered__threads(n)));
        p("executors", n.to_string(), text(gathered__executors(n)));
        p("unicode", n.to_string(), text(gathered__unicode(n)));
        p("concurrentMaps", n.to_string(), text(gathered__concurrent_maps(n)));
        p("futures", n.to_string(), text(gathered__futures(n)));
        p("extras", n.to_string(), text(gathered__extras(n)));
        p("splits", n.to_string(), text(gathered__splits(n)));
    }
    for d0 in doubles() {
        p("doubleText", d(d0), text(texts__double_text(d0)));
    }
    for f0 in floats() {
        p("floatText", f(f0), text(texts__float_text(f0)));
    }
    let edges = [1e-3, 9.999999999999998e-4, 1e7, 9999999.999999998, 1e-323, 4.9e-324, f64::MAX, f64::MIN_POSITIVE, 0.1, 0.3, 2e22, 1.0e23, 5e-324];
    for d0 in edges {
        p("doubleText", d(d0), text(texts__double_text(d0)));
    }
    // The same random values on both sides: a 64-bit linear congruential generator, read as doubles and floats.
    let mut x: i64 = 0x2545F4914F6CDD1D;
    for _ in 0..3000 {
        x = x.wrapping_mul(6364136223846793005).wrapping_add(1442695040888963407);
        let d0 = f64::from_bits(x as u64);
        let f0 = f32::from_bits(((x as u64) >> 32) as u32);
        p("doubleText", d(d0), text(texts__double_text(d0)));
        p("floatText", f(f0), text(texts__float_text(f0)));
    }
    p("services", String::new(), text(services__report()));
    p("loaders", String::new(), text(services__loaders()));
    p("sides", "server".to_string(), text(sides__server()));
    p("sides", "none".to_string(), text(sides__none()));
    p("logs", String::new(), text(sides__logs()));
    for (i, d0) in format_doubles().into_iter().enumerate() {
        p("formats", i.to_string(), text(formats__doubles(d0)));
    }
    for (i, v) in format_longs().into_iter().enumerate() {
        p("formatLongs", i.to_string(), text(formats__longs(v)));
    }
    p("formatFacts", String::new(), text(formats__facts()));
    let count = folders__count().unwrap();
    for i in 0..count {
        p("pathParts", i.to_string(), text(folders__parts(i)));
        for j in 0..count {
            p("pathPair", format!("{} {}", i, j), text(folders__pair(i, j)));
        }
    }
    p("pathErrors", String::new(), text(folders__errors()));
    p("lazy", String::new(), text(lazy__made()));
    for n in [0, 1, 2, 5, 12, 40] {
        p("slots", n.to_string(), text(turns__slots(n)));
    }
    for n in [0, 1, 7, 30] {
        p("identity", n.to_string(), text(turns__identity(n)));
    }
    if let Ok(made) = std::env::var("HORIZONNEO_MADE") {
        p("made", String::new(), text(folders__made(jstring(&made))));
    }
}
