package net.minecraft;

/** The stand-in for the game's Util: description ids and make (observed, `standins --horizon-src`). */
public class Util {
    public static String makeDescriptionId(String type, net.minecraft.resources.ResourceLocation id) {
        if (id == null) return type + ".unregistered_sadface";
        return type + "." + id.getNamespace() + "." + id.getPath().replace('/', '.');
    }

    public static <T> T make(java.util.function.Supplier<T> maker) {
        return maker.get();
    }

    public static <T> T make(T object, java.util.function.Consumer<? super T> change) {
        change.accept(object);
        return object;
    }
}
