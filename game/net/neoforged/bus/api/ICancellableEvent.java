package net.neoforged.bus.api;

/**
 * The stand-in for NeoForge's ICancellableEvent: an event a listener may cancel, which the listeners that don't take
 * cancelled events then don't get (HorizonEventBus, observed with `standins --horizon-src`).
 */
public interface ICancellableEvent {
    default void setCanceled(boolean canceled) {
        ((Event) this).isCanceled = canceled;
    }

    default boolean isCanceled() {
        return ((Event) this).isCanceled;
    }
}
