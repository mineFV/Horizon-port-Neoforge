package net.minecraft.core.component;

/** Not the game's: a component map's types, in their order, written as the game's set writes them ({a, b}, observed). */
final class HorizonKeys extends java.util.LinkedHashSet<DataComponentType<?>> {
    HorizonKeys(java.util.Collection<DataComponentType<?>> types) {
        super(types);
    }

    @Override
    public String toString() {
        StringBuilder b = new StringBuilder("{");
        boolean first = true;
        for (DataComponentType<?> t : this) {
            if (!first) b.append(", ");
            first = false;
            b.append(t);
        }
        return b.append('}').toString();
    }

    /** A map's text, observed: {type=>value, ...} in its types' order. */
    static String text(DataComponentMap map) {
        StringBuilder b = new StringBuilder("{");
        boolean first = true;
        for (DataComponentType<?> t : map.keySet()) {
            if (!first) b.append(", ");
            first = false;
            b.append(t).append("=>").append((Object) map.get(t));
        }
        return b.append('}').toString();
    }
}
