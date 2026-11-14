package horizonneo;

import java.util.List;

/**
 * What a NeoForge mod's jar does, as read from its class files and resources: facts only, each with
 * where it was read. Nothing here says what becomes of it in Horizon; `Report` does.
 *
 * Class names are internal names (`com/example/Mod`), as class files write them.
 *
 * configScreens: the mod's classes that name NeoForge's `IConfigScreenFactory`, which is how a mod
 * says it has a settings screen of its own.
 *
 * unreadRegisters: where the mod makes or uses a DeferredRegister whose entries couldn't be read (a register
 * not kept in a static field, one reached through the mod's own wrapper). What it registers isn't in
 * `registries`, so a block or item missing from there may still be registered.
 */
record Analysis(
        Census census,
        List<EntryPoint> entryPoints,
        List<Listener> listeners,
        List<Registry> registries,
        List<ConfigUse> configs,
        List<String> configScreens,
        List<MixinClass> mixins,
        List<AccessFile> accessTransformers,
        List<GameClass> gameClasses,
        List<ApiArea> apiAreas,
        boolean neoforgeJars,
        List<String> unreadRegisters,
        List<String> warnings) {

    Analysis {
        entryPoints = List.copyOf(entryPoints);
        listeners = List.copyOf(listeners);
        registries = List.copyOf(registries);
        configs = List.copyOf(configs);
        configScreens = List.copyOf(configScreens);
        mixins = List.copyOf(mixins);
        accessTransformers = List.copyOf(accessTransformers);
        gameClasses = List.copyOf(gameClasses);
        apiAreas = List.copyOf(apiAreas);
        unreadRegisters = List.copyOf(unreadRegisters);
        warnings = List.copyOf(warnings);
    }

    /** How many class files (the mod's own), asset files and data files the jar holds. */
    record Census(int classes, int assets, int data) {}

    /**
     * A class with `@Mod`. side: "client" or "server" when its `dist` names one, else "both".
     * parameters: what its constructor is handed, by simple name. unknownParameters: those NeoForge
     * doesn't hand over, which it would refuse.
     */
    record EntryPoint(String className, String modId, String side, List<String> parameters, List<String> unknownParameters) {
        EntryPoint {
            parameters = List.copyOf(parameters);
            unknownParameters = List.copyOf(unknownParameters);
        }
    }

    /**
     * One event listener. event: the event's class, null when it couldn't be read. handlerMethod: null
     * when the handler isn't written at the call. registeredBy: how it gets on a bus, in words;
     * registered is false when nothing was found that does it. bus and side each come with where they
     * were read; sideFrom is null for "common".
     */
    record Listener(String event, String handlerClass, String handlerMethod, boolean lambda, String registeredBy, boolean registered,
                    BusFacts.Bus bus, String busFrom, String side, String sideFrom, String priority, boolean receiveCanceled,
                    List<String> notes) {
        Listener {
            notes = List.copyOf(notes);
        }
    }

    /**
     * A DeferredRegister. kind: the registry it fills, as its key's name (`BLOCK`, `SOUND_EVENT`), null
     * when it couldn't be read. onModBus: `register(bus)` was seen for it.
     */
    record Registry(String kind, String namespace, String declaredIn, String field, boolean onModBus, List<Registration> entries) {
        Registry {
            entries = List.copyOf(entries);
        }
    }

    /**
     * One thing registered. how: the method called (`registerSimpleBlock`). objectClass: the class of
     * what is registered, null when it couldn't be read; classFrom says how it was (the lambda at the
     * call, what the method always makes, or only the holder field's declared type).
     */
    record Registration(String name, String how, String objectClass, String classFrom, String holderField) {}

    /** A config handed to NeoForge: its type (CLIENT, COMMON, SERVER, STARTUP), the spec's field, and where. */
    record ConfigUse(String type, String spec, String registeredIn) {}

    /**
     * A Mixin class. config: the config that lists it, null when none does. environment: the config's
     * list it is in (mixins, client, server).
     */
    record MixinClass(String className, String config, String environment, List<String> targets, List<Injector> injectors) {
        MixinClass {
            targets = List.copyOf(targets);
            injectors = List.copyOf(injectors);
        }
    }

    /** One annotated method of a Mixin class. at: the first `@At`'s value; atTarget: its target. */
    record Injector(String annotation, String handler, List<String> methods, String at, String atTarget, boolean cancellable) {
        Injector {
            methods = List.copyOf(methods);
        }
    }

    /** An access transformer file. declared: the metadata lists it. lines: its entries, comments left out. */
    record AccessFile(String file, boolean declared, List<String> lines) {
        AccessFile {
            lines = List.copyOf(lines);
        }
    }

    /** A class of the mod that extends a class of the game. methods: the methods it declares itself. */
    record GameClass(String className, String gameSuper, List<String> methods) {
        GameClass {
            methods = List.copyOf(methods);
        }
    }

    /** A part of NeoForge's API the mod's classes name. usedBy: those classes. */
    record ApiArea(String area, List<String> usedBy) {
        ApiArea {
            usedBy = List.copyOf(usedBy);
        }
    }
}
