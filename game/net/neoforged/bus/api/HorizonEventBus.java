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

    /**
     * The event to each listener of its class, by priority (HIGHEST first), then in the order they were added. A listener
     * given without its event's class has its lambda's: the class its site instantiates its parameter to, as NeoForge
     * reads it; once a listener cancelled it (ICancellableEvent), only those that take cancelled events. Not modelled, and
     * refused: the objects registered (their @SubscribeEvent methods).
     */
    @Override
    public <T extends Event> T post(T event) {
        for (EventPriority priority : EventPriority.values()) post(priority, event);
        return event;
    }

    @Override
    @SuppressWarnings({"unchecked", "rawtypes"})
    public <T extends Event> T post(EventPriority priority, T event) {
        if (!registered.isEmpty()) throw new UnsupportedOperationException("the listeners of objects registered on a bus aren't modelled (horizon-port-neoforge)");
        for (Listener l : new ArrayList<>(listeners)) {
            if (l.priority() != priority) continue;
            Class<?> type = l.type() != null ? l.type() : eventType(l.consumer());
            if (type == null) throw new IllegalStateException("the event class of a listener given without it isn't known (horizon-port-neoforge)");
            if (!type.isInstance(event)) continue;
            if (event.isCanceled && !l.receiveCanceled()) continue;
            ((Consumer) l.consumer()).accept(event);
        }
        return event;
    }

    /** A lambda's first parameter's class, as its site instantiates it; null for any other object (the translation's). */
    static native Class<?> eventType(Object listener);

    @Override
    public void start() {}
}
