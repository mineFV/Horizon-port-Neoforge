package horizonneo.probe;

import java.util.function.Function;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

/**
 * Registering by a function of the value's id, as Mekanism's own registers do: what the function is given, that
 * NeoForge's register by a supplier hands on to it (which a subclass overriding it sees), and a name given twice.
 */
final class RegisterProbe {
    private RegisterProbe() {}

    static final StringBuilder CALLS = new StringBuilder();

    /** A register that says when its register by a function runs. */
    static final class Recording extends DeferredRegister<String> {
        Recording() {
            super(BuiltInProbe.WORDS_KEY, "horizonprobe");
        }

        @Override
        public <I extends String> DeferredHolder<String, I> register(String name, Function<ResourceLocation, ? extends I> func) {
            CALLS.append("function ").append(name).append("; ");
            return super.register(name, func);
        }
    }

    static final DeferredRegister<String> BY_ID = DeferredRegister.create(BuiltInProbe.WORDS_KEY, "horizonprobe");
    static final DeferredHolder<String, String> MADE_BY_ID = BY_ID.register("by_id", id -> "made for " + id);
    static final Recording RECORDING = new Recording();
    static final DeferredHolder<String, String> RECORDED = RECORDING.register("recorded", () -> "recorded-value");

    static void run() {
        p("register by id", () -> CALLS + "| " + MADE_BY_ID.getId() + " " + MADE_BY_ID.get() + " " + RECORDED.getId() + " " + RECORDED.get()
                + " " + BY_ID.getEntries().size() + " " + RECORDING.getEntries().size() + " " + RECORDED.getClass().getName());
        p("register twice", () -> {
            DeferredRegister<String> twice = DeferredRegister.create(BuiltInProbe.WORDS_KEY, "horizonprobe");
            DeferredHolder<String, String> first = twice.register("twice", () -> "one");
            try {
                twice.register("twice", id -> "two");
                return "registered twice";
            } catch (RuntimeException e) {
                return e.getClass().getName() + ": " + e.getMessage() + " | " + twice.getEntries().size() + " " + first.getId();
            }
        });
        p("register null", () -> {
            DeferredRegister<String> nulls = DeferredRegister.create(BuiltInProbe.WORDS_KEY, "horizonprobe");
            StringBuilder b = new StringBuilder();
            for (Runnable r : new Runnable[] {() -> nulls.register("no_function", (Function<ResourceLocation, String>) null),
                    () -> nulls.register(null, id -> "x")}) {
                try {
                    r.run();
                    b.append("registered; ");
                } catch (RuntimeException e) {
                    b.append(e.getClass().getName()).append("; ");
                }
            }
            return b.append(nulls.getEntries().size());
        });
    }

    static void p(String key, java.util.function.Supplier<Object> call) {
        GameProbe.p(key, call);
    }
}
