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
    static final Map<String, String> THROWN = new TreeMap<>(Map.of(
            "JID_NULL_POINTER", "java/lang/NullPointerException",
            "JID_ARITHMETIC", "java/lang/ArithmeticException",
            "JID_ARRAY_INDEX", "java/lang/ArrayIndexOutOfBoundsException",
            "JID_NEGATIVE_SIZE", "java/lang/NegativeArraySizeException",
            "JID_ARRAY_STORE", "java/lang/ArrayStoreException",
            "JID_CLASS_CAST", "java/lang/ClassCastException",
            "JID_INITIALIZER", "java/lang/ExceptionInInitializerError",
            "JID_NO_CLASS", "java/lang/NoClassDefFoundError",
            "JID_ILLEGAL_MONITOR", "java/lang/IllegalMonitorStateException"));

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
