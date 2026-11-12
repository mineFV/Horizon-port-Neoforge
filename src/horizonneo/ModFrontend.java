package horizonneo;

/**
 * A mod loader's side of the converter. It reads a jar the way that loader does and hands over what
 * the rest needs, in a form that doesn't depend on the loader.
 *
 * NeoForge is the only frontend here: Fabric jars go to horizon-port as they are. Later milestones add
 * what else a frontend finds (entry points, event listeners, registries).
 */
interface ModFrontend {
    /** The loader's name, as `--loader` takes it. */
    String loader();

    /** The mod's own description of itself. */
    ModInfo info();
}
