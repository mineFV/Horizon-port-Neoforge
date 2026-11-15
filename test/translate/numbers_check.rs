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

fn main() {
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
}
