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

fn p(name: &str, args: String, result: String) {
    println!("{} {} -> {}", name, args, result);
}

fn main() {
    for &a in INTS.iter() {
        for &b in INTS.iter() {
            p("mix", format!("{} {}", a, b), numbers__mix(a, b).to_string());
            p("div", format!("{} {}", a, b), numbers__div(a, b).to_string());
            if b != 0 {
                p("floorMod", format!("{} {}", a, b), numbers__floor_mod(a, b).to_string());
            }
        }
        for s in [0, 1, 5, 31, 32, 33, 63, 64, 65, -1] {
            p("shl", format!("{} {}", a, s), numbers__shl(a, s).to_string());
        }
        p("neg", a.to_string(), numbers__neg(a).to_string());
        p("sign", a.to_string(), numbers__sign(a).to_string());
        p("incBoth", a.to_string(), numbers__inc_both(a).to_string());
        p("days", a.to_string(), numbers__days(a).to_string());
        p("sparse", a.to_string(), numbers__sparse(a).to_string());
        p("toByte", a.to_string(), numbers__to_byte(a).to_string());
        p("toChar", a.to_string(), (numbers__to_char(a) as i32).to_string());
        p("toShort", a.to_string(), numbers__to_short(a).to_string());
        p("iabs", a.to_string(), numbers__iabs(a).to_string());
        p("twice", a.to_string(), numbers__twice(a).to_string());
        p("pick", a.to_string(), numbers__pick(a > 0, a, a.wrapping_neg()).to_string());
        p("isDigit", a.to_string(), numbers__is_digit((a & 0xFFFF) as u16).to_string());
    }
    for m in 0..=13 {
        p("days", m.to_string(), numbers__days(m).to_string());
    }
    for n in 0..=25 {
        p("sumTo", n.to_string(), numbers__sum_to(n * 1000).to_string());
        p("fact", n.to_string(), numbers__fact(n).to_string());
    }
    for c in 40..=60 {
        p("isDigit", c.to_string(), numbers__is_digit(c as u16).to_string());
    }
    for &a in LONGS.iter() {
        for s in [0, 1, 13, 63, 64, 70] {
            p("lmix", format!("{} {}", a, s), numbers__lmix(a, s).to_string());
            p("ushr", format!("{} {}", a, s), numbers__ushr(a, s).to_string());
        }
        for &b in LONGS.iter() {
            p("cmpL", format!("{} {}", a, b), numbers__cmp_l(a, b).to_string());
            if b != 0 {
                p("floorDiv", format!("{} {}", a, b), numbers__floor_div(a, b).to_string());
            }
        }
        p("collatz", a.to_string(), numbers__collatz(a).to_string());
        p("l2f", a.to_string(), fb(numbers__l2f(a)).to_string());
    }
    for a in floats() {
        for b in floats() {
            p("less", format!("{} {}", f(a), f(b)), numbers__less(a, b).to_string());
            p("frem", format!("{} {}", f(a), f(b)), fb(numbers__frem(a, b)).to_string());
            p("fmin", format!("{} {}", f(a), f(b)), fb(numbers__fmin(a, b)).to_string());
        }
        p("f2i", f(a), numbers__f2i(a).to_string());
        p("roundF", f(a), numbers__round_f(a).to_string());
    }
    for a in doubles() {
        for b in doubles() {
            p("atLeast", format!("{} {}", d(a), d(b)), numbers__at_least(a, b).to_string());
            p("clamp", format!("{} {}", d(a), d(b)), db(numbers__clamp(a, b, 10.0)).to_string());
        }
        p("poly", d(a), db(numbers__poly(a)).to_string());
        p("d2l", d(a), numbers__d2l(a).to_string());
        p("roundD", d(a), numbers__round_d(a).to_string());
        p("sig", d(a), db(numbers__sig(a)).to_string());
    }
    // Objects: built and answered with numbers inside Shapes.
    for n in 0..=12 {
        p("areas", n.to_string(), shapes__areas(n).to_string());
        p("perimeters", n.to_string(), shapes__perimeters(n).to_string());
        p("made", n.to_string(), shapes__made(n).to_string());
        p("list", n.to_string(), shapes__list(n).to_string());
        p("statics", n.to_string(), shapes__statics(n).to_string());
        p("lambdas", n.to_string(), shapes__lambdas(n).to_string());
        p("casts", n.to_string(), shapes__casts(n).to_string());
        p("arrays", n.to_string(), shapes__arrays(n).to_string());
        p("same", n.to_string(), shapes__same(n).to_string());
        p("callNever", n.to_string(), shapes__call_never(None, n).to_string());
    }
}
