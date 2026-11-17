package net.neoforged.neoforge.common;

import net.neoforged.bus.api.HorizonEventBus;
import net.neoforged.bus.api.IEventBus;

/** The stand-in for NeoForge's NeoForge: its game event bus (HorizonEventBus keeps what is given to it). */
public class NeoForge {
    public static final IEventBus EVENT_BUS = new HorizonEventBus();

    public NeoForge() {}
}
