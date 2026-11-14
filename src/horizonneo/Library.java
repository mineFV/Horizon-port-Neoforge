package horizonneo;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

/**
 * The translator's model of Java's library (lib/ in this repository: our own Java, written from the Java SE API
 * specification and compiled by build.sh into this tool's jar under model/). Its classes are translated with the
 * mod's, by the same translator; its `native` methods are written here, in Rust.
 */
final class Library {
    private Library() {}

    /** The model's classes, by internal name, from this tool's jar. */
    static Map<String, ClassData> classes() throws IOException {
        Map<String, ClassData> out = new TreeMap<>();
        try (InputStream index = Library.class.getResourceAsStream("/model/index.txt")) {
            if (index == null) throw new IOException("this tool's jar carries no model of Java's library (build it with build.sh)");
            for (String line : new String(index.readAllBytes(), StandardCharsets.UTF_8).split("\n")) {
                String name = line.strip();
                if (name.isEmpty()) continue;
                try (InputStream in = Library.class.getResourceAsStream("/model/" + name)) {
                    if (in == null) throw new IOException("the model's " + name + " is missing from this tool's jar");
                    ClassData c = ClassData.parse(in.readAllBytes());
                    out.put(c.name, c);
                }
            }
        }
        return out;
    }

    /** A native method of the model: the Rust function's name and its text. */
    record Native(String name, String rust) {}

    /** The model's native methods, by owner.name+descriptor. */
    static final Map<String, Native> NATIVES = Map.ofEntries(
            Map.entry("java/lang/Object.hashCode()I", new Native("jnative_object_hash_code", """
                    /// Object.hashCode: the object's identity, a non-negative int as the JVM gives (its value is its own).
                    #[allow(dead_code)]
                    fn jnative_object_hash_code(this: Ref) -> JR<i32> {
                        let o = jobj(&this)?;
                        Ok(((Arc::as_ptr(o) as usize >> 3) as i32) & 0x7fffffff)
                    }
                    """)),
            Map.entry("java/lang/Object.jClassName()Ljava/lang/String;", new Native("jnative_object_class_name", """
                    /// getClass().getName(): the binary name of the object's class.
                    #[allow(dead_code)]
                    fn jnative_object_class_name(this: Ref) -> JR<Ref> {
                        let c = jobj(&this)?.class;
                        Ok(jstring(jclass_name(c)))
                    }
                    """)),
            Map.entry("java/lang/String.intern()Ljava/lang/String;", new Native("jnative_string_intern", """
                    /// String.intern: one string of each text, the literals' among them.
                    #[allow(dead_code)]
                    fn jnative_string_intern(this: Ref) -> JR<Ref> {
                        let units = jstring_units(&this)?;
                        Ok(jintern(&units))
                    }
                    """)),
            Map.entry("java/lang/String.floatText(F)Ljava/lang/String;", new Native("jnative_float_text", """
                    /// Float.toString.
                    #[allow(dead_code)]
                    fn jnative_float_text(f: f32) -> JR<Ref> {
                        Ok(jstring(&jfloat_text(f)))
                    }
                    """)),
            Map.entry("java/lang/String.doubleText(D)Ljava/lang/String;", new Native("jnative_double_text", """
                    /// Double.toString.
                    #[allow(dead_code)]
                    fn jnative_double_text(d: f64) -> JR<Ref> {
                        Ok(jstring(&jdouble_text(d)))
                    }
                    """)),
            Map.entry("java/lang/Float.floatToIntBits(F)I", new Native("jnative_float_bits", """
                    /// Float.floatToIntBits: every NaN as the canonical one.
                    #[allow(dead_code)]
                    fn jnative_float_bits(f: f32) -> JR<i32> {
                        Ok(if f.is_nan() { 0x7fc00000 } else { f.to_bits() as i32 })
                    }
                    """)),
            Map.entry("java/lang/Float.floatToRawIntBits(F)I", new Native("jnative_float_raw_bits", """
                    #[allow(dead_code)]
                    fn jnative_float_raw_bits(f: f32) -> JR<i32> {
                        Ok(f.to_bits() as i32)
                    }
                    """)),
            Map.entry("java/lang/Float.intBitsToFloat(I)F", new Native("jnative_float_from_bits", """
                    #[allow(dead_code)]
                    fn jnative_float_from_bits(b: i32) -> JR<f32> {
                        Ok(f32::from_bits(b as u32))
                    }
                    """)),
            Map.entry("java/lang/Double.doubleToLongBits(D)J", new Native("jnative_double_bits", """
                    /// Double.doubleToLongBits: every NaN as the canonical one.
                    #[allow(dead_code)]
                    fn jnative_double_bits(d: f64) -> JR<i64> {
                        Ok(if d.is_nan() { 0x7ff8000000000000 } else { d.to_bits() as i64 })
                    }
                    """)),
            Map.entry("java/lang/Double.doubleToRawLongBits(D)J", new Native("jnative_double_raw_bits", """
                    #[allow(dead_code)]
                    fn jnative_double_raw_bits(d: f64) -> JR<i64> {
                        Ok(d.to_bits() as i64)
                    }
                    """)),
            Map.entry("java/lang/Double.longBitsToDouble(J)D", new Native("jnative_double_from_bits", """
                    #[allow(dead_code)]
                    fn jnative_double_from_bits(b: i64) -> JR<f64> {
                        Ok(f64::from_bits(b as u64))
                    }
                    """)),
            Map.entry("[.clone()Ljava/lang/Object;", new Native("jnative_array_clone", """
                    /// An array's clone: a new array of its class with its elements (a shallow copy).
                    #[allow(dead_code)]
                    fn jnative_array_clone(this: Ref) -> JR<Ref> {
                        let o = jobj(&this)?;
                        let slots = jlock(&o.slots).clone();
                        Ok(Some(Arc::new(Obj { class: o.class, slots: Mutex::new(slots) })))
                    }
                    """)));

    /** The key of an array's clone, which arrays alone select (`JarRust.arraySelects`). */
    static final String ARRAY_CLONE = "[.clone()Ljava/lang/Object;";

    static Native nativeOf(JarRust.Id id) {
        return NATIVES.get(id.owner() + "." + id.name() + id.descriptor());
    }

    /** The exceptions the runtime throws itself, by the name of the constant that holds each one's class id. */
    static final Map<String, String> THROWN = new TreeMap<>(Map.of(
            "JID_NULL_POINTER", "java/lang/NullPointerException",
            "JID_ARITHMETIC", "java/lang/ArithmeticException",
            "JID_ARRAY_INDEX", "java/lang/ArrayIndexOutOfBoundsException",
            "JID_NEGATIVE_SIZE", "java/lang/NegativeArraySizeException",
            "JID_ARRAY_STORE", "java/lang/ArrayStoreException",
            "JID_CLASS_CAST", "java/lang/ClassCastException",
            "JID_INITIALIZER", "java/lang/ExceptionInInitializerError",
            "JID_NO_CLASS", "java/lang/NoClassDefFoundError"));

    /** Java's Double.toString and Float.toString, from Rust's shortest digits (the same digits: the shortest that read back, the closest of those). */
    static final String NUMBER_TEXT = """

            /// The shortest digits of a finite, non-zero value and its decimal exponent: 1234.5 is ("12345", 3).
            #[allow(dead_code)]
            fn jdigits(s: String) -> (String, i32) {
                let (mantissa, exponent) = s.split_once('e').unwrap();
                let digits: String = mantissa.chars().filter(|c| c.is_ascii_digit()).collect();
                let digits = digits.trim_end_matches('0').to_string();
                (if digits.is_empty() { "0".to_string() } else { digits }, exponent.parse::<i32>().unwrap())
            }

            /// Between two shortest decimals as close to the value as each other (the value is their midpoint), Java takes
            /// the one whose last digit is even, where both read back as the value.
            #[allow(dead_code)]
            fn jeven(digits: String, exponent: i32, exact: String, reads_back: &dyn Fn(&str, i32) -> bool) -> (String, i32) {
                let (exact_digits, exact_exponent) = jdigits(exact);
                let n = digits.len();
                if exact_exponent != exponent || exact_digits.len() != n + 1 || !exact_digits.ends_with('5') {
                    return (digits, exponent);
                }
                let low = exact_digits[..n].to_string();
                let mut high: Vec<u8> = low.clone().into_bytes();
                let mut i = high.len();
                loop {
                    if i == 0 {
                        return (digits, exponent);
                    }
                    i -= 1;
                    if high[i] == b'9' {
                        high[i] = b'0';
                    } else {
                        high[i] += 1;
                        break;
                    }
                }
                let high = String::from_utf8(high).unwrap();
                for candidate in [&low, &high] {
                    let even = (candidate.as_bytes()[n - 1] - b'0') % 2 == 0;
                    if even && reads_back(candidate, exponent) {
                        return (candidate.trim_end_matches('0').to_string().max("0".to_string()), exponent);
                    }
                }
                (digits, exponent)
            }

            /// Java's layout of those digits: plain between 10^-3 and 10^7, else d.dddE±n; one digit at least after the point.
            #[allow(dead_code)]
            fn jlayout(negative: bool, digits: &str, exponent: i32) -> String {
                let mut out = String::new();
                if negative {
                    out.push('-');
                }
                if (-3..7).contains(&exponent) {
                    if exponent < 0 {
                        out.push_str("0.");
                        for _ in 0..(-exponent - 1) {
                            out.push('0');
                        }
                        out.push_str(digits);
                    } else {
                        let whole = (exponent + 1) as usize;
                        if digits.len() <= whole {
                            out.push_str(digits);
                            for _ in digits.len()..whole {
                                out.push('0');
                            }
                            out.push_str(".0");
                        } else {
                            out.push_str(&digits[..whole]);
                            out.push('.');
                            out.push_str(&digits[whole..]);
                        }
                    }
                } else {
                    out.push_str(&digits[..1]);
                    out.push('.');
                    out.push_str(if digits.len() > 1 { &digits[1..] } else { "0" });
                    out.push('E');
                    out.push_str(&exponent.to_string());
                }
                out
            }

            #[allow(dead_code)]
            fn jdouble_text(d: f64) -> String {
                if d.is_nan() {
                    return "NaN".to_string();
                }
                if d.is_infinite() {
                    return if d > 0.0 { "Infinity" } else { "-Infinity" }.to_string();
                }
                if d == 0.0 {
                    return if d.is_sign_negative() { "-0.0" } else { "0.0" }.to_string();
                }
                let (mut digits, mut exponent) = jdigits(format!("{:e}", d.abs()));
                // One digit is written as the closest of two (the JDK's rule: Double.MIN_VALUE is 4.9E-324).
                if digits.len() == 1 {
                    (digits, exponent) = jdigits(format!("{:.1e}", d.abs()));
                }
                let v = d.abs();
                (digits, exponent) = jeven(digits, exponent, format!("{:.800e}", v), &|ds: &str, e: i32| {
                    format!("{}.{}e{}", &ds[..1], if ds.len() > 1 { &ds[1..] } else { "0" }, e).parse::<f64>().map(|x| x == v).unwrap_or(false)
                });
                jlayout(d < 0.0, &digits, exponent)
            }

            #[allow(dead_code)]
            fn jfloat_text(f: f32) -> String {
                if f.is_nan() {
                    return "NaN".to_string();
                }
                if f.is_infinite() {
                    return if f > 0.0 { "Infinity" } else { "-Infinity" }.to_string();
                }
                if f == 0.0 {
                    return if f.is_sign_negative() { "-0.0" } else { "0.0" }.to_string();
                }
                let (mut digits, mut exponent) = jdigits(format!("{:e}", f.abs()));
                // One digit is written as the closest of two (the JDK's rule: Double.MIN_VALUE is 4.9E-324).
                if digits.len() == 1 {
                    (digits, exponent) = jdigits(format!("{:.1e}", f.abs()));
                }
                let v = f.abs();
                (digits, exponent) = jeven(digits, exponent, format!("{:.200e}", v), &|ds: &str, e: i32| {
                    format!("{}.{}e{}", &ds[..1], if ds.len() > 1 { &ds[1..] } else { "0" }, e).parse::<f32>().map(|x| x == v).unwrap_or(false)
                });
                jlayout(f < 0.0, &digits, exponent)
            }
            """;

    static List<String> thrownClasses() {
        return List.copyOf(THROWN.values());
    }
}
