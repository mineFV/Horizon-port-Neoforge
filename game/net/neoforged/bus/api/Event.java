package net.neoforged.bus.api;

/** The stand-in for NeoForge's Event: whether it was cancelled (ICancellableEvent), which the bus reads (HorizonEventBus). */
public abstract class Event {
    boolean isCanceled;

    protected Event() {}
}
