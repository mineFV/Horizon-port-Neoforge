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

    /** Whether a class of the model stands in for the game's (game/), not for Java's library (lib/). */
    static boolean standIn(String name) {
        return !name.startsWith("java/") && !bridge(name);
    }

    /**
     * Whether a class of the model is this tool's own, beside the stand-ins (game/): HorizonBridge, which Horizon's
     * hook points call, and HorizonRegistries. Not the game's: not checked against it, and kept translated.
     */
    static boolean bridge(String name) {
        String simple = name.substring(name.lastIndexOf('/') + 1);
        return !name.startsWith("java/") && simple.startsWith("Horizon");
    }

    /**
     * The stand-ins for the game (game/) that differ from the real classes read with `--server` where a program can
     * tell, with why: their superclass or interfaces aren't the game's, or the real class declares a method that the
     * stand-in would inherit from a class above it instead. A stand-in the server doesn't have isn't checked.
     */
    static Map<String, String> checkStandIns(Map<String, ClassData> model, Map<String, ClassData> outside) {
        Map<String, String> out = new TreeMap<>();
        for (ClassData c : model.values()) {
            if (!standIn(c.name)) continue;
            ClassData real = outside.get(c.name);
            if (real == null) continue;
            if (!java.util.Objects.equals(real.superName, c.superName) || !new java.util.HashSet<>(real.interfaces).equals(new java.util.HashSet<>(c.interfaces))) {
                out.put(c.name, "its superclass or interfaces aren't the game's");
                continue;
            }
            for (ClassData.Method m : real.methods) {
                // A bridge (ACC_BRIDGE) only calls the method it stands for, with the same answer: not checked.
                if (m.isStatic() || (m.access() & (0x0002 | 0x0040)) != 0 || m.name().startsWith("<")) continue;
                if (declares(c, m.name(), m.descriptor())) continue;
                for (String s = c.superName; s != null && model.containsKey(s); s = model.get(s).superName) {
                    ClassData up = model.get(s);
                    if (declares(up, m.name(), m.descriptor())) {
                        out.put(c.name, "the game's class declares " + m.name() + m.descriptor() + ", which the stand-in would take from " + s.replace('/', '.'));
                        break;
                    }
                }
                if (out.containsKey(c.name)) break;
            }
        }
        return out;
    }

    private static boolean declares(ClassData c, String name, String descriptor) {
        for (ClassData.Method m : c.methods) if (m.name().equals(name) && m.descriptor().equals(descriptor)) return true;
        return false;
    }

    /** A native method of the model: the Rust function's name and its text. */
    record Native(String name, String rust) {}

    /** The model's native methods, by owner.name+descriptor. */
    static final Map<String, Native> NATIVES = Map.ofEntries(
            Map.entry("net/minecraft/world/level/HorizonWorld.ask(Ljava/lang/String;)Ljava/lang/String;", new Native("jnative_horizon_ask", """
                    /// HorizonWorld.ask: a question to Horizon's world, answered by the bridge (bridge.rs, JWORLD) on the
                    /// thread that holds the world; null when nothing answers.
                    #[allow(dead_code)]
                    fn jnative_horizon_ask(question: Ref) -> JR<Ref> {
                        let units = jstring_units(&question)?;
                        Ok(JWORLD.get().and_then(|ask| ask(&units)).map(|answer| jstring_of(&answer)).unwrap_or(None))
                    }
                    """)),
            Map.entry("net/neoforged/bus/api/HorizonEventBus.eventType(Ljava/lang/Object;)Ljava/lang/Class;", new Native("jnative_horizon_event_type", """
                    /// HorizonEventBus.eventType: a lambda's first parameter's class, as its site instantiates it (what
                    /// NeoForge's bus reads of a listener given without its event's class); null for any other object.
                    #[allow(dead_code)]
                    fn jnative_horizon_event_type(listener: Ref) -> JR<Ref> {
                        let t = jlambda_first_parameter(jobj(&listener)?.class);
                        Ok(if t == 0 { None } else { jclass_object(t) })
                    }
                    """)),
            Map.entry("com/mojang/logging/HorizonLog.write(Ljava/lang/String;Ljava/lang/String;)V", new Native("jnative_horizon_log", """
                    /// HorizonLog.write: a log line of the mod's, to Horizon's output (the standard error), with its level.
                    #[allow(dead_code)]
                    fn jnative_horizon_log(level: Ref, text: Ref) -> JR<()> {
                        let level = String::from_utf16_lossy(&jstring_units(&level)?);
                        let text = String::from_utf16_lossy(&jstring_units(&text)?);
                        eprintln!("[{}] {}", level, text);
                        Ok(())
                    }
                    """)),
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
                    """)),
            Map.entry("java/lang/Object.getClass()Ljava/lang/Class;", new Native("jnative_object_get_class", """
                    /// getClass: the one class object of the object's type.
                    #[allow(dead_code)]
                    fn jnative_object_get_class(this: Ref) -> JR<Ref> {
                        Ok(jclass_object(jobj(&this)?.class))
                    }
                    """)),
            Map.entry("java/lang/Class.jName(I)Ljava/lang/String;", new Native("jnative_class_name", """
                    /// Class.getName: the binary name, one String for each (the JVM interns them).
                    #[allow(dead_code)]
                    fn jnative_class_name(c: i32) -> JR<Ref> {
                        Ok(jintern(&jclass_name(c as u32).encode_utf16().collect::<Vec<u16>>()))
                    }
                    """)),
            Map.entry("java/lang/Class.jSimpleName(I)Ljava/lang/String;", new Native("jnative_class_simple_name", """
                    #[allow(dead_code)]
                    fn jnative_class_simple_name(c: i32) -> JR<Ref> {
                        Ok(jstring(jclass_simple(c as u32)))
                    }
                    """)),
            Map.entry("java/lang/Class.jCanonicalName(I)Ljava/lang/String;", new Native("jnative_class_canonical_name", """
                    #[allow(dead_code)]
                    fn jnative_class_canonical_name(c: i32) -> JR<Ref> {
                        Ok(jclass_canonical(c as u32).map(jstring).unwrap_or(None))
                    }
                    """)),
            Map.entry("java/lang/Class.jInstance(ILjava/lang/Object;)Z", new Native("jnative_class_instance", """
                    #[allow(dead_code)]
                    fn jnative_class_instance(c: i32, o: Ref) -> JR<bool> {
                        Ok(match &o {
                            Some(x) => jinstance_of(c as u32, x.class),
                            None => false,
                        })
                    }
                    """)),
            Map.entry("java/lang/Class.jAssignable(II)Z", new Native("jnative_class_assignable", """
                    #[allow(dead_code)]
                    fn jnative_class_assignable(c: i32, other: i32) -> JR<bool> {
                        Ok(jinstance_of(c as u32, other as u32))
                    }
                    """)),
            Map.entry("java/lang/Class.jFlags(I)I", new Native("jnative_class_flags", """
                    #[allow(dead_code)]
                    fn jnative_class_flags(c: i32) -> JR<i32> {
                        Ok(jclass_flags(c as u32))
                    }
                    """)),
            Map.entry("java/lang/Class.jSuper(I)Ljava/lang/Class;", new Native("jnative_class_super", """
                    /// getSuperclass: none for Object and an interface, Object for an array.
                    #[allow(dead_code)]
                    fn jnative_class_super(c: i32) -> JR<Ref> {
                        let s = jclass_super(c as u32);
                        Ok(if s == 0 { None } else { jclass_object(s) })
                    }
                    """)),
            Map.entry("java/lang/Class.jEnumValues(I)[Ljava/lang/Enum;", new Native("jnative_class_enum_values", """
                    /// getEnumConstants: the enum's values(), a new array each time; null for any other type.
                    #[allow(dead_code)]
                    fn jnative_class_enum_values(c: i32) -> JR<Ref> {
                        jenum_values(c as u32)
                    }
                    """)),
            Map.entry("java/lang/Class.jServices(I)I", new Native("jnative_class_services", """
                    /// ServiceLoader's: how many providers of a service the jar lists, -1 when that isn't known.
                    #[allow(dead_code)]
                    fn jnative_class_services(c: i32) -> JR<i32> {
                        Ok(jservice_count(c as u32))
                    }
                    """)),
            Map.entry("java/lang/Class.jServiceProvider(II)Ljava/lang/String;", new Native("jnative_class_service_provider", """
                    /// ServiceLoader's: a provider's name, as the jar lists it.
                    #[allow(dead_code)]
                    fn jnative_class_service_provider(c: i32, i: i32) -> JR<Ref> {
                        Ok(jstring(jservice_name(c as u32, i)))
                    }
                    """)),
            Map.entry("java/lang/Class.jServiceNew(II)Ljava/lang/Object;", new Native("jnative_class_service_new", """
                    /// ServiceLoader's: a provider made, its class initialized and its constructor run.
                    #[allow(dead_code)]
                    fn jnative_class_service_new(c: i32, i: i32) -> JR<Ref> {
                        jservice_make(c as u32, i)
                    }
                    """)),
            Map.entry("java/lang/Class.jPrimitive(C)Ljava/lang/Class;", new Native("jnative_class_primitive", """
                    /// Integer.TYPE and the like: the object of a primitive type or void, by its descriptor.
                    #[allow(dead_code)]
                    fn jnative_class_primitive(descriptor: u16) -> JR<Ref> {
                        Ok(jclass_object(jclass_primitive(descriptor as i32)))
                    }
                    """)),
            Map.entry("java/lang/System.currentTimeMillis()J", new Native("jnative_system_current_time_millis", """
                    /// System.currentTimeMillis: the machine's clock, in milliseconds since 1970.
                    #[allow(dead_code)]
                    fn jnative_system_current_time_millis() -> JR<i64> {
                        Ok(match std::time::SystemTime::now().duration_since(std::time::UNIX_EPOCH) {
                            Ok(d) => d.as_millis() as i64,
                            Err(e) => -(e.duration().as_millis() as i64),
                        })
                    }
                    """)),
            Map.entry("java/lang/System.nanoTime()J", new Native("jnative_system_nano_time", """
                    /// System.nanoTime: nanoseconds from the first time it is asked (the JDK's origin is arbitrary too).
                    #[allow(dead_code)]
                    fn jnative_system_nano_time() -> JR<i64> {
                        static START: LazyLock<std::time::Instant> = LazyLock::new(std::time::Instant::now);
                        Ok(START.elapsed().as_nanos() as i64)
                    }
                    """)),
            Map.entry("java/lang/System.jIdentity(Ljava/lang/Object;)I", new Native("jnative_system_identity", """
                    /// System.identityHashCode of an object (not null): Object.hashCode's identity.
                    #[allow(dead_code)]
                    fn jnative_system_identity(x: Ref) -> JR<i32> {
                        let o = jobj(&x)?;
                        Ok(((Arc::as_ptr(o) as usize >> 3) as i32) & 0x7fffffff)
                    }
                    """)),
            Map.entry("java/text/NumberFormat.jDefaultKey()Ljava/lang/String;", new Native("jnative_number_default_key", """
                    /// NumberFormat's default locale (Locale.Category.FORMAT) as the JDK makes it from the environment on Linux,
                    /// as "language_script_country"; "!" and why where it can't be told (lib/README.md).
                    #[allow(dead_code)]
                    fn jnative_number_default_key() -> JR<Ref> {
                        static KEY: std::sync::OnceLock<String> = std::sync::OnceLock::new();
                        Ok(jstring(KEY.get_or_init(|| match jnum_default() {
                            Ok((language, script, country)) => format!("{}_{}_{}", language, script, country),
                            Err(why) => format!("!{}", why),
                        })))
                    }
                    """)),
            Map.entry("java/text/NumberFormat.jData(Ljava/lang/String;)Ljava/lang/String;", new Native("jnative_number_data", """
                    /// A locale's number formats (numbers.rs), its language's where the key isn't kept and its country is an ISO
                    /// one; null where the model has none.
                    #[allow(dead_code)]
                    fn jnative_number_data(key: Ref) -> JR<Ref> {
                        Ok(jnum_data(&jstring_text(&key)).map(jstring).unwrap_or(None))
                    }
                    """)),
            Map.entry("java/text/DecimalFormat.jCompare(DJI)I", new Native("jnative_number_compare", """
                    /// The exact value of a double (finite, not negative) against digits × 10^-scale: -1, 0 or 1; 2 where it
                    /// can't be told in 128 bits.
                    #[allow(dead_code)]
                    fn jnative_number_compare(a: f64, digits: i64, scale: i32) -> JR<i32> {
                        if !(a.is_finite() && a >= 0.0 && digits >= 0 && (0..=20).contains(&scale)) {
                            return Ok(2);
                        }
                        let bits = a.to_bits();
                        let biased = ((bits >> 52) & 0x7ff) as i32;
                        let fraction = bits & ((1u64 << 52) - 1);
                        let (m, e) = if biased == 0 { (fraction, -1074) } else { (fraction | (1u64 << 52), biased - 1075) };
                        let ten = 10u128.pow(scale as u32);
                        let shifted = |x: u128, by: i32| if by < 128 && (x.leading_zeros() as i32) > by { Some(x << by) } else { None };
                        let (left, right) = if e >= 0 {
                            (shifted(m as u128, e).and_then(|x| x.checked_mul(ten)), Some(digits as u128))
                        } else {
                            ((m as u128).checked_mul(ten), shifted(digits as u128, -e))
                        };
                        Ok(match (left, right) {
                            (Some(l), Some(r)) => match l.cmp(&r) {
                                std::cmp::Ordering::Less => -1,
                                std::cmp::Ordering::Equal => 0,
                                std::cmp::Ordering::Greater => 1,
                            },
                            _ => 2,
                        })
                    }
                    """)),
            Map.entry("java/util/UUID.jRandom()J", new Native("jnative_uuid_random", """
                    /// 64 random bits for randomUUID: SipHash under the keys Rust draws from the system's randomness.
                    #[allow(dead_code)]
                    fn jnative_uuid_random() -> JR<i64> {
                        use std::hash::{BuildHasher, Hasher};
                        static COUNT: std::sync::atomic::AtomicU64 = std::sync::atomic::AtomicU64::new(0);
                        let mut h = std::collections::hash_map::RandomState::new().build_hasher();
                        h.write_u64(COUNT.fetch_add(1, std::sync::atomic::Ordering::Relaxed));
                        Ok(h.finish() as i64)
                    }
                    """)),
            Map.entry("java/util/concurrent/atomic/AtomicInteger.jGetAndSet(I)I", new Native("jnative_atomic_integer_get_and_set", """
                    /// AtomicInteger.getAndSet: its value is its first slot, swapped under the object's lock.
                    #[allow(dead_code)]
                    fn jnative_atomic_integer_get_and_set(this: Ref, value: i32) -> JR<i32> {
                        let mut slots = jlock(&jobj(&this)?.slots);
                        let old = jgi(slots[0].clone());
                        slots[0] = Value::I(value);
                        Ok(old)
                    }
                    """)),
            Map.entry("java/util/concurrent/atomic/AtomicInteger.jCompareAndSet(II)Z", new Native("jnative_atomic_integer_compare_and_set", """
                    #[allow(dead_code)]
                    fn jnative_atomic_integer_compare_and_set(this: Ref, expected: i32, value: i32) -> JR<bool> {
                        let mut slots = jlock(&jobj(&this)?.slots);
                        if jgi(slots[0].clone()) != expected {
                            return Ok(false);
                        }
                        slots[0] = Value::I(value);
                        Ok(true)
                    }
                    """)),
            Map.entry("java/util/concurrent/atomic/AtomicInteger.jGetAndAdd(I)I", new Native("jnative_atomic_integer_get_and_add", """
                    #[allow(dead_code)]
                    fn jnative_atomic_integer_get_and_add(this: Ref, delta: i32) -> JR<i32> {
                        let mut slots = jlock(&jobj(&this)?.slots);
                        let old = jgi(slots[0].clone());
                        slots[0] = Value::I(old.wrapping_add(delta));
                        Ok(old)
                    }
                    """)),
            Map.entry("java/util/concurrent/atomic/AtomicLong.jGetAndSet(J)J", new Native("jnative_atomic_long_get_and_set", """
                    /// AtomicLong.getAndSet: its value is its first slot, swapped under the object's lock.
                    #[allow(dead_code)]
                    fn jnative_atomic_long_get_and_set(this: Ref, value: i64) -> JR<i64> {
                        let mut slots = jlock(&jobj(&this)?.slots);
                        let old = jgj(slots[0].clone());
                        slots[0] = Value::J(value);
                        Ok(old)
                    }
                    """)),
            Map.entry("java/util/concurrent/atomic/AtomicLong.jCompareAndSet(JJ)Z", new Native("jnative_atomic_long_compare_and_set", """
                    #[allow(dead_code)]
                    fn jnative_atomic_long_compare_and_set(this: Ref, expected: i64, value: i64) -> JR<bool> {
                        let mut slots = jlock(&jobj(&this)?.slots);
                        if jgj(slots[0].clone()) != expected {
                            return Ok(false);
                        }
                        slots[0] = Value::J(value);
                        Ok(true)
                    }
                    """)),
            Map.entry("java/util/concurrent/atomic/AtomicLong.jGetAndAdd(J)J", new Native("jnative_atomic_long_get_and_add", """
                    #[allow(dead_code)]
                    fn jnative_atomic_long_get_and_add(this: Ref, delta: i64) -> JR<i64> {
                        let mut slots = jlock(&jobj(&this)?.slots);
                        let old = jgj(slots[0].clone());
                        slots[0] = Value::J(old.wrapping_add(delta));
                        Ok(old)
                    }
                    """)),
            Map.entry("java/util/concurrent/atomic/AtomicBoolean.jGetAndSet(I)I", new Native("jnative_atomic_boolean_get_and_set", """
                    /// AtomicBoolean.getAndSet: its value (0 or 1) is its first slot, swapped under the object's lock.
                    #[allow(dead_code)]
                    fn jnative_atomic_boolean_get_and_set(this: Ref, value: i32) -> JR<i32> {
                        let mut slots = jlock(&jobj(&this)?.slots);
                        let old = jgi(slots[0].clone());
                        slots[0] = Value::I(value);
                        Ok(old)
                    }
                    """)),
            Map.entry("java/util/concurrent/atomic/AtomicBoolean.jCompareAndSet(II)Z", new Native("jnative_atomic_boolean_compare_and_set", """
                    #[allow(dead_code)]
                    fn jnative_atomic_boolean_compare_and_set(this: Ref, expected: i32, value: i32) -> JR<bool> {
                        let mut slots = jlock(&jobj(&this)?.slots);
                        if jgi(slots[0].clone()) != expected {
                            return Ok(false);
                        }
                        slots[0] = Value::I(value);
                        Ok(true)
                    }
                    """)),
            Map.entry("java/util/concurrent/atomic/AtomicReference.jGetAndSet(Ljava/lang/Object;)Ljava/lang/Object;", new Native("jnative_atomic_reference_get_and_set", """
                    /// AtomicReference.getAndSet: its value is its first slot, swapped under the object's lock.
                    #[allow(dead_code)]
                    fn jnative_atomic_reference_get_and_set(this: Ref, value: Ref) -> JR<Ref> {
                        let mut slots = jlock(&jobj(&this)?.slots);
                        let old = jga(slots[0].clone());
                        slots[0] = Value::A(value);
                        Ok(old)
                    }
                    """)),
            Map.entry("java/util/concurrent/atomic/AtomicReference.jCompareAndSet(Ljava/lang/Object;Ljava/lang/Object;)Z", new Native("jnative_atomic_reference_compare_and_set", """
                    /// By identity, as the JDK's.
                    #[allow(dead_code)]
                    fn jnative_atomic_reference_compare_and_set(this: Ref, expected: Ref, value: Ref) -> JR<bool> {
                        let mut slots = jlock(&jobj(&this)?.slots);
                        let same = match (&jga(slots[0].clone()), &expected) {
                            (Some(a), Some(b)) => Arc::ptr_eq(a, b),
                            (None, None) => true,
                            _ => false,
                        };
                        if !same {
                            return Ok(false);
                        }
                        slots[0] = Value::A(value);
                        Ok(true)
                    }
                    """)),
            Map.entry("java/lang/Object.jWait(J)V", new Native("jnative_object_wait", """
                    /// Object.wait: lets the monitor go, waits for a notify on the object (or the time, or a wake-up
                    /// of its own, which Java allows), then takes the monitor back as many times as it was held.
                    #[allow(dead_code)]
                    fn jnative_object_wait(this: Ref, millis: i64) -> JR<()> {
                        let key = Arc::as_ptr(jobj(&this)?) as usize;
                        let me = std::thread::current().id();
                        let (lock, changed) = &*JMONITORS;
                        let mut m = jlock(lock);
                        let count = match m.held.get(&key) {
                            Some((owner, count)) if *owner == me => *count,
                            _ => return Err(jexception(JID_ILLEGAL_MONITOR, Some("current thread is not owner"))),
                        };
                        m.held.remove(&key);
                        changed.notify_all();
                        let seen = *m.notified.get(&key).unwrap_or(&0);
                        let until = if millis > 0 { Some(std::time::Instant::now() + std::time::Duration::from_millis(millis as u64)) } else { None };
                        while *m.notified.get(&key).unwrap_or(&0) == seen {
                            match until {
                                None => m = changed.wait(m).unwrap_or_else(|e| e.into_inner()),
                                Some(t) => {
                                    let now = std::time::Instant::now();
                                    if now >= t {
                                        break;
                                    }
                                    m = changed.wait_timeout(m, t - now).unwrap_or_else(|e| e.into_inner()).0;
                                }
                            }
                        }
                        while m.held.contains_key(&key) {
                            m = changed.wait(m).unwrap_or_else(|e| e.into_inner());
                        }
                        m.held.insert(key, (me, count));
                        Ok(())
                    }
                    """)),
            Map.entry("java/lang/Object.jNotify()V", new Native("jnative_object_notify", """
                    /// Object.notify and notifyAll: every waiter wakes (for notify, the others' is a wake-up of their own).
                    #[allow(dead_code)]
                    fn jnative_object_notify(this: Ref) -> JR<()> {
                        let key = Arc::as_ptr(jobj(&this)?) as usize;
                        let me = std::thread::current().id();
                        let (lock, changed) = &*JMONITORS;
                        let mut m = jlock(lock);
                        match m.held.get(&key) {
                            Some((owner, _)) if *owner == me => {}
                            _ => return Err(jexception(JID_ILLEGAL_MONITOR, Some("current thread is not owner"))),
                        }
                        *m.notified.entry(key).or_insert(0) += 1;
                        changed.notify_all();
                        Ok(())
                    }
                    """)),
            Map.entry("java/lang/Thread.jStart()V", new Native("jnative_thread_start", """
                    /// Thread.start: a thread of the system's, which runs the thread's run as its current thread.
                    #[allow(dead_code)]
                    fn jnative_thread_start(this: Ref) -> JR<()> {
                        let t = this.clone();
                        std::thread::Builder::new()
                            .spawn(move || {
                                JCURRENT.with(|c| *c.borrow_mut() = t.clone());
                                let _ = {CALL0}(t);
                            })
                            .expect("the system can't start a thread");
                        Ok(())
                    }
                    """)),
            Map.entry("java/lang/Thread.currentThread()Ljava/lang/Thread;", new Native("jnative_thread_current", """
                    /// Thread.currentThread: the thread's object; one made the first time for a thread the translation didn't start.
                    #[allow(dead_code)]
                    fn jnative_thread_current() -> JR<Ref> {
                        let current = JCURRENT.with(|c| c.borrow().clone());
                        if current.is_some() {
                            return Ok(current);
                        }
                        let t = {CALL0}()?;
                        JCURRENT.with(|c| *c.borrow_mut() = t.clone());
                        Ok(t)
                    }
                    """)),
            Map.entry("java/lang/Thread.jSystemName()Ljava/lang/String;", new Native("jnative_thread_system_name", """
                    /// The system's name for the current thread ("main" for the first), for a thread the translation didn't start.
                    #[allow(dead_code)]
                    fn jnative_thread_system_name() -> JR<Ref> {
                        Ok(jstring(std::thread::current().name().unwrap_or("main")))
                    }
                    """)),
            Map.entry("java/lang/Thread.jSleep(J)V", new Native("jnative_thread_sleep", """
                    #[allow(dead_code)]
                    fn jnative_thread_sleep(millis: i64) -> JR<()> {
                        std::thread::sleep(std::time::Duration::from_millis(millis as u64));
                        Ok(())
                    }
                    """)),
            Map.entry("java/lang/Thread.jPrintError(Ljava/lang/String;)V", new Native("jnative_thread_print_error", """
                    /// What a thread's uncaught exception prints, to the standard error.
                    #[allow(dead_code)]
                    fn jnative_thread_print_error(text: Ref) -> JR<()> {
                        eprintln!("{}", String::from_utf16_lossy(&jstring_units(&text)?));
                        Ok(())
                    }
                    """)),
            Map.entry("java/lang/Character.jType(I)I", new Native("jnative_char_type", """
                    #[allow(dead_code)]
                    fn jnative_char_type(cp: i32) -> JR<i32> {
                        Ok(if (0..0x110000).contains(&cp) { juni_run(JUNI_TYPE, cp) } else { 0 })
                    }
                    """)),
            Map.entry("java/lang/Character.jLower(I)I", new Native("jnative_char_lower", """
                    #[allow(dead_code)]
                    fn jnative_char_lower(cp: i32) -> JR<i32> {
                        Ok(juni_map(JUNI_LOWER, cp))
                    }
                    """)),
            Map.entry("java/lang/Character.jUpper(I)I", new Native("jnative_char_upper", """
                    #[allow(dead_code)]
                    fn jnative_char_upper(cp: i32) -> JR<i32> {
                        Ok(juni_map(JUNI_UPPER, cp))
                    }
                    """)),
            Map.entry("java/lang/Character.jProperty(II)Z", new Native("jnative_char_property", """
                    #[allow(dead_code)]
                    fn jnative_char_property(which: i32, cp: i32) -> JR<bool> {
                        if !(0..0x110000).contains(&cp) {
                            return Ok(false);
                        }
                        let table = match which {
                            0 => JUNI_LOWERCASE,
                            1 => JUNI_UPPERCASE,
                            2 => JUNI_ALPHABETIC,
                            _ => JUNI_CASED,
                        };
                        Ok(juni_run(table, cp) != 0)
                    }
                    """)),
            Map.entry("java/lang/Character.jDigit(I)I", new Native("jnative_char_digit", """
                    /// Character.digit's value in radix 36, -1 for none.
                    #[allow(dead_code)]
                    fn jnative_char_digit(cp: i32) -> JR<i32> {
                        if cp < 0 {
                            return Ok(-1);
                        }
                        let c = cp as u32;
                        let i = JUNI_DIGIT.partition_point(|e| e.0 <= c);
                        if i == 0 {
                            return Ok(-1);
                        }
                        let (first, last, value) = JUNI_DIGIT[i - 1];
                        Ok(if c <= last { value as i32 + (c - first) as i32 } else { -1 })
                    }
                    """)),
            Map.entry("java/lang/Character.jLowerText(I)Ljava/lang/String;", new Native("jnative_char_lower_text", """
                    #[allow(dead_code)]
                    fn jnative_char_lower_text(cp: i32) -> JR<Ref> {
                        Ok(juni_text(JUNI_LOWER_TEXT, cp))
                    }
                    """)),
            Map.entry("java/lang/Character.jUpperText(I)Ljava/lang/String;", new Native("jnative_char_upper_text", """
                    #[allow(dead_code)]
                    fn jnative_char_upper_text(cp: i32) -> JR<Ref> {
                        Ok(juni_text(JUNI_UPPER_TEXT, cp))
                    }
                    """)),
            Map.entry("java/lang/System.arraycopy(Ljava/lang/Object;ILjava/lang/Object;II)V", new Native("jnative_system_arraycopy", """
                    /// System.arraycopy, checked as HotSpot checks it, with its messages; the source read first (an
                    /// overlap copies as if through a buffer); an element that can't go in stops it, those before it copied.
                    #[allow(dead_code)]
                    fn jnative_system_arraycopy(src: Ref, src_pos: i32, dst: Ref, dst_pos: i32, length: i32) -> JR<()> {
                        let s = jobj(&src)?.clone();
                        let d = jobj(&dst)?.clone();
                        let (sc, dc) = (s.class, d.class);
                        const KINDS: [&str; 8] = ["boolean", "byte", "char", "short", "int", "long", "float", "double"];
                        let primitive = |c: u32| (1..=8).contains(&c);
                        if jclass_flags(sc) & 4 == 0 && !primitive(sc) {
                            return Err(jexception(JID_ARRAY_STORE, Some(&format!("arraycopy: source type {} is not an array", jclass_name(sc)))));
                        }
                        if jclass_flags(dc) & 4 == 0 && !primitive(dc) {
                            return Err(jexception(JID_ARRAY_STORE, Some(&format!("arraycopy: destination type {} is not an array", jclass_name(dc)))));
                        }
                        let elements = |c: u32| if primitive(c) { KINDS[(c - 1) as usize].to_string() } else { "object array".to_string() };
                        if (primitive(sc) || primitive(dc)) && sc != dc {
                            return Err(jexception(JID_ARRAY_STORE, Some(&format!("arraycopy: type mismatch: can not copy {}[] into {}[]", elements(sc), elements(dc)))));
                        }
                        let slen = jlock(&s.slots).len();
                        let dlen = jlock(&d.slots).len();
                        if src_pos < 0 {
                            return Err(jexception(JID_ARRAY_INDEX, Some(&format!("arraycopy: source index {} out of bounds for {}[{}]", src_pos, elements(sc), slen))));
                        }
                        if dst_pos < 0 {
                            return Err(jexception(JID_ARRAY_INDEX, Some(&format!("arraycopy: destination index {} out of bounds for {}[{}]", dst_pos, elements(dc), dlen))));
                        }
                        if length < 0 {
                            return Err(jexception(JID_ARRAY_INDEX, Some(&format!("arraycopy: length {} is negative", length))));
                        }
                        let last_src = (length as u32).wrapping_add(src_pos as u32);
                        let last_dst = (length as u32).wrapping_add(dst_pos as u32);
                        if last_src as usize > slen {
                            return Err(jexception(JID_ARRAY_INDEX, Some(&format!("arraycopy: last source index {} out of bounds for {}[{}]", last_src, elements(sc), slen))));
                        }
                        if last_dst as usize > dlen {
                            return Err(jexception(JID_ARRAY_INDEX, Some(&format!("arraycopy: last destination index {} out of bounds for {}[{}]", last_dst, elements(dc), dlen))));
                        }
                        if length == 0 {
                            return Ok(());
                        }
                        let part: Vec<Value> = jlock(&s.slots)[src_pos as usize..(src_pos + length) as usize].to_vec();
                        let (scomp, dcomp) = (jclass_component(sc), jclass_component(dc));
                        let check = !primitive(sc) && !(scomp == dcomp || jinstance_of(dcomp, scomp));
                        let mut g = jlock(&d.slots);
                        for (k, v) in part.into_iter().enumerate() {
                            if check {
                                if let Value::A(Some(o)) = &v {
                                    if !jinstance_of(dcomp, o.class) {
                                        return Err(jexception(JID_ARRAY_STORE, Some(&format!(
                                            "arraycopy: element type mismatch: can not cast one of the elements of {}[] to the type of the destination array, {}",
                                            jclass_name(scomp), jclass_name(dcomp)))));
                                    }
                                }
                            }
                            g[dst_pos as usize + k] = v;
                        }
                        Ok(())
                    }
                    """)),
            Map.entry("java/lang/Class.jComponent(I)Ljava/lang/Class;", new Native("jnative_class_component", """
                    /// getComponentType: an array's component's class object, null for any other type.
                    #[allow(dead_code)]
                    fn jnative_class_component(c: i32) -> JR<Ref> {
                        let c = c as u32;
                        if (1..=8).contains(&c) {
                            return Ok(jclass_object(jclass_primitive(b"ZBCSIJFD"[(c - 1) as usize] as i32)));
                        }
                        let comp = jclass_component(c);
                        Ok(if comp == 0 { None } else { jclass_object(comp) })
                    }
                    """)),
            Map.entry("java/util/Arrays.newArrayLike([Ljava/lang/Object;I)[Ljava/lang/Object;", new Native("jnative_array_like", """
                    /// A new array of a's class (a String[] for a String[]), of this length, all null; the length isn't negative.
                    #[allow(dead_code)]
                    fn jnative_array_like(a: Ref, length: i32) -> JR<Ref> {
                        let class = jobj(&a)?.class;
                        Ok(Some(Arc::new(Obj { class, slots: Mutex::new(vec![Value::A(None); length as usize]) })))
                    }
                    """)));

    /** The key of an array's clone, which arrays alone select (`JarRust.arraySelects`). */
    static final String ARRAY_CLONE = "[.clone()Ljava/lang/Object;";

    /** The natives that read Unicode's tables (`unicode.rs`, which build.sh writes from the JDK that builds the tool). */
    static final java.util.Set<String> UNICODE_NATIVES = java.util.Set.of("jnative_char_type", "jnative_char_lower", "jnative_char_upper", "jnative_char_property",
            "jnative_char_digit", "jnative_char_lower_text", "jnative_char_upper_text");

    /** The tables, and how the natives look things up in them. */
    static String unicodeText() {
        try (java.io.InputStream in = Library.class.getResourceAsStream("unicode.rs")) {
            if (in == null) throw new IllegalStateException("the tool was built without its Unicode tables (build.sh writes them)");
            return new String(in.readAllBytes(), java.nio.charset.StandardCharsets.UTF_8) + """

                    /// A code point's entry in a sorted table of (code point, value); the code point itself when none.
                    #[allow(dead_code)]
                    fn juni_map(t: &[(u32, u32)], cp: i32) -> i32 {
                        match t.binary_search_by_key(&(cp as u32), |e| e.0) {
                            Ok(i) => t[i].1 as i32,
                            Err(_) => cp,
                        }
                    }

                    /// The value of the run a code point (0 to 0x10FFFF) is in, in a table of the runs' first code points.
                    #[allow(dead_code)]
                    fn juni_run(t: &[(u32, u8)], cp: i32) -> i32 {
                        let i = t.partition_point(|e| e.0 <= cp as u32);
                        t[i - 1].1 as i32
                    }

                    #[allow(dead_code)]
                    fn juni_text(t: &[(u32, &[u16])], cp: i32) -> Ref {
                        match t.binary_search_by_key(&(cp as u32), |e| e.0) {
                            Ok(i) => jstring_of(t[i].1),
                            Err(_) => None,
                        }
                    }
                    """;
        } catch (java.io.IOException e) {
            throw new java.io.UncheckedIOException(e);
        }
    }

    /** The natives that read the locales' number formats (`numbers.rs`, which build.sh writes from the JDK that builds the tool). */
    static final java.util.Set<String> NUMBER_NATIVES = java.util.Set.of("jnative_number_default_key", "jnative_number_data");

    /**
     * The tables, how a key is looked up in them, and how the JDK's default format locale is made from the environment
     * on Linux, as observed (2026-11-23, Java 21 and 25, glibc 2.44): every category glibc's setlocale(LC_ALL, "")
     * sets must load, or all are "C"; the format locale is LC_CTYPE's, each of its parts LC_MESSAGES's where it has none.
     */
    static String numbersText() {
        try (java.io.InputStream in = Library.class.getResourceAsStream("numbers.rs")) {
            if (in == null) throw new IllegalStateException("the tool was built without its number formats (build.sh writes them)");
            return new String(in.readAllBytes(), java.nio.charset.StandardCharsets.UTF_8) + """

                    /// A key's record (JNUM_RECORDS); its language's where the key isn't kept, has no script and its country is
                    /// an ISO one or none. None where the model has none (record 0).
                    #[allow(dead_code)]
                    fn jnum_data(key: &str) -> Option<&'static str> {
                        let found = |k: &str| JNUM_LOCALES.binary_search_by(|e| e.0.cmp(k)).ok().map(|i| JNUM_RECORDS[JNUM_LOCALES[i].1 as usize]);
                        let record = match found(key) {
                            Some(r) => r,
                            None => {
                                let mut parts = key.splitn(3, '_');
                                let (language, script, country) = (parts.next()?, parts.next()?, parts.next()?);
                                if !script.is_empty() || (!country.is_empty() && JNUM_COUNTRIES.binary_search(&country).is_err()) {
                                    return None;
                                }
                                found(&format!("{}__", language))?
                            }
                        };
                        if record.is_empty() { None } else { Some(record) }
                    }

                    /// The categories glibc's setlocale(LC_ALL, "") sets.
                    #[allow(dead_code)]
                    const JNUM_CATEGORIES: [&str; 12] = ["LC_CTYPE", "LC_NUMERIC", "LC_TIME", "LC_COLLATE", "LC_MONETARY", "LC_MESSAGES",
                        "LC_PAPER", "LC_NAME", "LC_ADDRESS", "LC_TELEPHONE", "LC_MEASUREMENT", "LC_IDENTIFICATION"];

                    /// The JDK's format locale (language, script, country), or why it can't be told.
                    #[allow(dead_code)]
                    fn jnum_default() -> Result<(String, String, String), String> {
                        if !cfg!(target_os = "linux") {
                            return Err("the system's locale is read on Linux only".to_string());
                        }
                        if std::env::var_os("LOCPATH").is_some() {
                            return Err("LOCPATH is set".to_string());
                        }
                        let var = |n: &str| -> Result<Option<String>, String> {
                            match std::env::var_os(n) {
                                None => Ok(None),
                                Some(v) if v.is_empty() => Ok(None),
                                Some(v) => v.into_string().map(Some).map_err(|_| format!("{} isn't UTF-8", n)),
                            }
                        };
                        let name = |category: &str| -> Result<String, String> {
                            Ok(var("LC_ALL")?.or(var(category)?).or(var("LANG")?).unwrap_or_else(|| "C".to_string()))
                        };
                        let archive = jnum_archive()?;
                        let aliases = jnum_aliases();
                        let mut loaded = true;
                        for category in JNUM_CATEGORIES {
                            if !jnum_loads(&name(category)?, &archive, &aliases)? {
                                loaded = false;
                                break;
                            }
                        }
                        let (ctype, messages) = if loaded { (name("LC_CTYPE")?, name("LC_MESSAGES")?) } else { ("C".to_string(), "C".to_string()) };
                        let (f, d) = (jnum_parse(&ctype)?, jnum_parse(&messages)?);
                        let pick = |a: &str, b: &str| if a.is_empty() { b.to_string() } else { a.to_string() };
                        Ok((pick(&f.0, &d.0), pick(&f.1, &d.1), pick(&f.2, &d.2)))
                    }

                    /// A locale name's parts: language and territory, codeset, modifier.
                    #[allow(dead_code)]
                    fn jnum_explode(name: &str) -> (&str, Option<&str>, Option<&str>) {
                        let (base, modifier) = match name.split_once('@') {
                            Some((b, m)) => (b, Some(m)),
                            None => (name, None),
                        };
                        match base.split_once('.') {
                            Some((lt, c)) => (lt, Some(c), modifier),
                            None => (base, None, modifier),
                        }
                    }

                    /// glibc's normalized codeset: its letters lowered and digits, "iso" before digits alone.
                    #[allow(dead_code)]
                    fn jnum_normalize(codeset: &str) -> String {
                        let s: String = codeset.chars().filter(|c| c.is_ascii_alphanumeric()).map(|c| c.to_ascii_lowercase()).collect();
                        if !s.is_empty() && s.chars().all(|c| c.is_ascii_digit()) { format!("iso{}", s) } else { s }
                    }

                    #[allow(dead_code)]
                    fn jnum_join(lt: &str, codeset: Option<&str>, modifier: Option<&str>) -> String {
                        let mut s = lt.to_string();
                        if let Some(c) = codeset {
                            s.push('.');
                            s.push_str(c);
                        }
                        if let Some(m) = modifier {
                            s.push('@');
                            s.push_str(m);
                        }
                        s
                    }

                    /// Whether glibc loads a locale: in its archive (by the name with its codeset normalized) or a folder of
                    /// /usr/lib/locale by that name. Where only a less precise one is installed, glibc would load it if its
                    /// codeset matched, which isn't read here: why it can't be told.
                    #[allow(dead_code)]
                    fn jnum_loads(name: &str, archive: &[String], aliases: &[String]) -> Result<bool, String> {
                        if name == "C" || name == "POSIX" {
                            return Ok(true);
                        }
                        if name.contains('/') {
                            return Err(format!("the locale {} is a path", name));
                        }
                        if aliases.iter().any(|a| a == name) {
                            return Err(format!("the locale {} is an alias", name));
                        }
                        let (lt, codeset, modifier) = jnum_explode(name);
                        let normalized = codeset.map(jnum_normalize);
                        let installed = |n: &str| archive.iter().any(|a| a == n) || std::path::Path::new("/usr/lib/locale").join(n).is_dir();
                        if installed(&jnum_join(lt, normalized.as_deref(), modifier)) || installed(name) {
                            return Ok(true);
                        }
                        if lt == "C" || lt == "POSIX" {
                            return Err(format!("whether glibc has {} built in can't be told", name));
                        }
                        let language = lt.split('_').next().unwrap_or(lt);
                        for l in [lt, language] {
                            for c in [codeset, normalized.as_deref(), None] {
                                for m in [modifier, None] {
                                    let less = jnum_join(l, c, m);
                                    if less != name && installed(&less) {
                                        return Err(format!("only {}, less precise than {}, is installed", less, name));
                                    }
                                }
                            }
                        }
                        Ok(false)
                    }

                    /// The names in glibc's locale archive (its header, the names' hash table); none without one.
                    #[allow(dead_code)]
                    fn jnum_archive() -> Result<Vec<String>, String> {
                        use std::io::{Read, Seek, SeekFrom};
                        let Ok(mut file) = std::fs::File::open("/usr/lib/locale/locale-archive") else { return Ok(Vec::new()) };
                        // Up to length bytes from offset (fewer at the end of the file).
                        let mut read = |offset: u64, length: usize| -> Option<Vec<u8>> {
                            let mut bytes = Vec::new();
                            file.seek(SeekFrom::Start(offset)).ok()?;
                            (&mut file).take(length as u64).read_to_end(&mut bytes).ok()?;
                            Some(bytes)
                        };
                        let unreadable = || "glibc's locale archive isn't one this reads".to_string();
                        let header = read(0, 56).filter(|h| h.len() == 56).ok_or_else(unreadable)?;
                        let word = |b: &[u8], i: usize| u32::from_ne_bytes([b[i], b[i + 1], b[i + 2], b[i + 3]]);
                        if word(&header, 0) != 0xde020109 {
                            return Err(unreadable());
                        }
                        let (table, size) = (word(&header, 8) as u64, word(&header, 16) as usize);
                        if size > 1 << 20 {
                            return Err(unreadable());
                        }
                        let entries = read(table, size * 12).filter(|e| e.len() == size * 12).ok_or_else(unreadable)?;
                        let mut names = Vec::new();
                        for i in 0..size {
                            let at = word(&entries, i * 12 + 4) as u64;
                            if at == 0 {
                                continue;
                            }
                            let text = read(at, 256).ok_or_else(unreadable)?;
                            let end = text.iter().position(|&b| b == 0).ok_or_else(unreadable)?;
                            names.push(String::from_utf8_lossy(&text[..end]).into_owned());
                        }
                        Ok(names)
                    }

                    /// The aliases glibc may read (locale.alias): a locale named by one isn't followed.
                    #[allow(dead_code)]
                    fn jnum_aliases() -> Vec<String> {
                        let mut out = Vec::new();
                        for path in ["/usr/share/locale/locale.alias", "/etc/locale.alias"] {
                            if let Ok(text) = std::fs::read_to_string(path) {
                                for line in text.lines() {
                                    let line = line.trim();
                                    if line.is_empty() || line.starts_with('#') {
                                        continue;
                                    }
                                    if let Some(alias) = line.split_whitespace().next() {
                                        out.push(alias.to_string());
                                    }
                                }
                            }
                        }
                        out
                    }

                    /// The JDK's locale of a name setlocale gives (observed): "C" and "POSIX" are en_US, C.UTF-8 is English
                    /// alone; else language_TERRITORY, the old codes iw, ji and in as Locale makes them, @latin the Latin
                    /// script, @euro and @valencia nothing. Other names aren't followed.
                    #[allow(dead_code)]
                    fn jnum_parse(name: &str) -> Result<(String, String, String), String> {
                        if name == "C" || name == "POSIX" {
                            return Ok(("en".to_string(), String::new(), "US".to_string()));
                        }
                        let (lt, codeset, modifier) = jnum_explode(name);
                        if lt == "C" {
                            return if modifier.is_none() && codeset.map(jnum_normalize).as_deref() == Some("utf8") {
                                Ok(("en".to_string(), String::new(), String::new()))
                            } else {
                                Err(format!("the JDK's locale for {} isn't known here", name))
                            };
                        }
                        let unknown = || format!("the JDK's locale for {} isn't known here", name);
                        let (language, territory) = lt.split_once('_').ok_or_else(unknown)?;
                        if !(2..=3).contains(&language.len()) || !language.bytes().all(|b| b.is_ascii_lowercase()) {
                            return Err(unknown());
                        }
                        if territory.len() != 2 || !territory.bytes().all(|b| b.is_ascii_uppercase()) {
                            return Err(unknown());
                        }
                        let script = match modifier {
                            None | Some("euro") | Some("valencia") => "",
                            Some("latin") => "Latn",
                            Some(_) => return Err(unknown()),
                        };
                        let language = match language {
                            "iw" => "he",
                            "ji" => "yi",
                            "in" => "id",
                            l => l,
                        };
                        Ok((language.to_string(), script.to_string(), territory.to_string()))
                    }
                    """;
        } catch (java.io.IOException e) {
            throw new java.io.UncheckedIOException(e);
        }
    }

    static Native nativeOf(JarRust.Id id) {
        return NATIVES.get(id.owner() + "." + id.name() + id.descriptor());
    }

    /**
     * The model's methods a native calls back (`{CALL0}`, `{CALL1}` in its text, each the function translated from
     * it): Thread.start's native runs the new thread's run through one.
     */
    static final Map<String, List<JarRust.Id>> NATIVE_CALLS = Map.of(
            "jnative_thread_start", List.of(new JarRust.Id("java/lang/Thread", "jRunThread", "(Ljava/lang/Thread;)V")),
            "jnative_thread_current", List.of(new JarRust.Id("java/lang/Thread", "jForeign", "()Ljava/lang/Thread;")));

    static List<JarRust.Id> callsOf(JarRust.Id id) {
        Native n = nativeOf(id);
        return n == null ? List.of() : NATIVE_CALLS.getOrDefault(n.name(), List.of());
    }

    /** The exceptions the runtime throws itself, by the name of the constant that holds each one's class id. */
    static final Map<String, String> THROWN = new TreeMap<>(Map.ofEntries(
            Map.entry("JID_NULL_POINTER", "java/lang/NullPointerException"),
            Map.entry("JID_ARITHMETIC", "java/lang/ArithmeticException"),
            Map.entry("JID_ARRAY_INDEX", "java/lang/ArrayIndexOutOfBoundsException"),
            Map.entry("JID_NEGATIVE_SIZE", "java/lang/NegativeArraySizeException"),
            Map.entry("JID_ARRAY_STORE", "java/lang/ArrayStoreException"),
            Map.entry("JID_CLASS_CAST", "java/lang/ClassCastException"),
            Map.entry("JID_INITIALIZER", "java/lang/ExceptionInInitializerError"),
            Map.entry("JID_NO_CLASS", "java/lang/NoClassDefFoundError"),
            Map.entry("JID_RUNTIME", "java/lang/RuntimeException"),
            Map.entry("JID_ILLEGAL_MONITOR", "java/lang/IllegalMonitorStateException"),
            Map.entry("JID_UNSUPPORTED", "java/lang/UnsupportedOperationException")));

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
