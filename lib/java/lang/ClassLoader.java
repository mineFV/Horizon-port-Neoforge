package java.lang;

/**
 * The model of java.lang.ClassLoader: what loaded a class, as an object to compare and to give to ServiceLoader. One
 * loader stands for the one NeoForge gives the mod's classes; nothing is loaded through it (its methods aren't
 * modelled).
 */
public abstract class ClassLoader {
    protected ClassLoader() {}

    private static final ClassLoader MOD = new Mod();

    /** The mod's classes' loader (Class.getClassLoader). Not the JDK's API: a mod, built against the JDK, can't call it. */
    static ClassLoader jMod() {
        return MOD;
    }

    /** The class of the mod's loader: the model's own. */
    private static final class Mod extends ClassLoader {
        Mod() {}
    }
}
