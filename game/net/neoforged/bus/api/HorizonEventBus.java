package net.neoforged.bus.api;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

/**
 * Not NeoForge's: horizon-port-neoforge's event bus, the mod's and NeoForge.EVENT_BUS. It keeps what the mod registers,
 * with its priority, its event class when given and whether it takes cancelled events; NeoForge finds a listener's
 * event class from its generic type, which isn't known here. Nothing is fired yet: Horizon's hook points will, as
 * they are wired (post isn't modelled, so a mod's own post isn't translated).
 */
public final class HorizonEventBus implements IEventBus {
    /** A listener as given. type: its event class, null when the mod didn't say. */
    public record Listener(EventPriority priority, boolean receiveCanceled, Class<?> type, Consumer<?> consumer) {}

    private final List<Listener> listeners = new ArrayList<>();
    private final List<Object> registered = new ArrayList<>();

    public List<Listener> listeners() {
        return listeners;
    }

    @Override
    public void register(Object target) {
        registered.add(target);
    }

    @Override
    public <T extends Event> void addListener(Consumer<T> listener) {
        addListener(EventPriority.NORMAL, false, null, listener);
    }

    @Override
    public <T extends Event> void addListener(Class<T> type, Consumer<T> listener) {
        addListener(EventPriority.NORMAL, false, type, listener);
    }

    @Override
    public <T extends Event> void addListener(EventPriority priority, Consumer<T> listener) {
        addListener(priority, false, null, listener);
    }

    @Override
    public <T extends Event> void addListener(EventPriority priority, Class<T> type, Consumer<T> listener) {
        addListener(priority, false, type, listener);
    }

    @Override
    public <T extends Event> void addListener(EventPriority priority, boolean receiveCanceled, Consumer<T> listener) {
        addListener(priority, receiveCanceled, null, listener);
    }

    @Override
    public <T extends Event> void addListener(EventPriority priority, boolean receiveCanceled, Class<T> type, Consumer<T> listener) {
        listeners.add(new Listener(priority, receiveCanceled, type, listener));
    }

    @Override
    public <T extends Event> void addListener(boolean receiveCanceled, Consumer<T> listener) {
        addListener(EventPriority.NORMAL, receiveCanceled, null, listener);
    }

    @Override
    public <T extends Event> void addListener(boolean receiveCanceled, Class<T> type, Consumer<T> listener) {
        addListener(EventPriority.NORMAL, receiveCanceled, type, listener);
    }

    @Override
    public void unregister(Object target) {
        registered.remove(target);
    }

    /** Not modelled: firing needs each listener's event class, which isn't known here. */
    @Override
    public native <T extends Event> T post(T event);

    @Override
    public native <T extends Event> T post(EventPriority priority, T event);

    @Override
    public void start() {}
}
