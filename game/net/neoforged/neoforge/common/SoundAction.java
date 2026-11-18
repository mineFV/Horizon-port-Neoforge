package net.neoforged.neoforge.common;

import java.util.HashMap;
import java.util.Map;

/** The stand-in for NeoForge's SoundAction: one for each name (observed, FluidTypeProbe). */
public final class SoundAction {
    private static final Map<String, SoundAction> ACTIONS = new HashMap<>();
    private final String name;

    private SoundAction(String name) {
        this.name = name;
    }

    public static synchronized SoundAction get(String name) {
        SoundAction a = ACTIONS.get(name);
        if (a == null) {
            a = new SoundAction(name);
            ACTIONS.put(name, a);
        }
        return a;
    }

    public String name() {
        return name;
    }

    @Override
    public String toString() {
        return "SoundAction[" + name + "]";
    }
}
