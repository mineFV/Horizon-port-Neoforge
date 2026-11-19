package horizonneo.probe;

import net.minecraft.core.Holder;
import net.minecraft.core.Registry;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.neoforged.neoforge.registries.RegistryBuilder;

/**
 * A registry of the probe's whose values make their holders themselves (intrusive holders, as Mekanism's chemicals):
 * the holder's state when it is made, unbound, and once its value is registered; and DeferredRegister.getEntries.
 */
final class IntrusiveProbe {
    private IntrusiveProbe() {}

    static final ResourceKey<Registry<Thing>> THINGS_KEY = ResourceKey.createRegistryKey(ResourceLocation.parse("horizonprobe:things"));
    static final Registry<Thing> THINGS = new RegistryBuilder<>(THINGS_KEY).withIntrusiveHolders().create();
    static final DeferredRegister<Thing> ENTRIES = DeferredRegister.create(THINGS_KEY, "horizonprobe");
    static final DeferredHolder<Thing, Thing> ONE = ENTRIES.register("one", () -> new Thing("one"));
    static final DeferredHolder<Thing, Thing> TWO = ENTRIES.register("two", () -> new Thing("two"));
    static final StringBuilder MADE = new StringBuilder();

    static final class Thing {
        final String name;
        final Holder.Reference<Thing> holder;

        Thing(String name) {
            this.name = name;
            this.holder = THINGS.createIntrusiveHolder(this);
            MADE.append(name).append(": ").append(holder.isBound()).append(' ');
            for (java.util.function.Supplier<Object> ask : java.util.List.<java.util.function.Supplier<Object>>of(holder::key, holder::unwrapKey)) {
                try {
                    MADE.append(ask.get());
                } catch (RuntimeException e) {
                    // The game's text ends with its holder owner's identity.
                    String m = e.getMessage();
                    MADE.append(e.getClass().getSimpleName()).append(' ').append(m.substring(0, m.indexOf(" from registry ") + 14));
                }
                MADE.append(' ');
            }
            MADE.append("; ");
        }

        @Override
        public String toString() {
            return "Thing " + name;
        }
    }

    static void run() {
        p("intrusive holders", () -> {
            Thing one = ONE.get(), two = TWO.get();
            return MADE + "| " + one.holder.key() + " " + one.holder.unwrapKey() + " " + one.holder.isBound() + " " + one.holder.value() + " " + one.holder
                    + " " + (THINGS.wrapAsHolder(one) == one.holder) + " " + (THINGS.getHolder(ONE.getKey()).orElseThrow() == one.holder) + " " + two.holder.key()
                    + " " + one.holder.is(ResourceLocation.parse("horizonprobe:one")) + " " + one.holder.equals(two.holder) + " " + (one.holder.hashCode() == ONE.getKey().hashCode());
        });
        p("intrusive refused", () -> {
            try {
                return BuiltInProbe.WORDS.createIntrusiveHolder("stray");
            } catch (RuntimeException e) {
                return e.getClass().getSimpleName() + " " + e.getMessage();
            }
        });
        p("deferred entries", () -> {
            java.util.Collection<DeferredHolder<Thing, ? extends Thing>> entries = ENTRIES.getEntries();
            String changed;
            try {
                entries.clear();
                changed = "cleared";
            } catch (UnsupportedOperationException e) {
                changed = "unchangeable";
            }
            return entries.size() + " " + entries.iterator().next().getId() + " " + entries.stream().map(DeferredHolder::getId).toList() + " " + changed
                    + " " + (entries == ENTRIES.getEntries()) + " " + entries.contains(TWO);
        });
    }

    static void p(String key, java.util.function.Supplier<Object> call) {
        GameProbe.p(key, call);
    }
}
