package net.neoforged.bus.api;

import java.util.function.Consumer;

/** The stand-in for NeoForge's IEventBus: what a mod gives it (HorizonEventBus keeps it). */
public interface IEventBus {
    void register(Object target);

    <T extends Event> void addListener(Consumer<T> listener);

    <T extends Event> void addListener(Class<T> type, Consumer<T> listener);

    <T extends Event> void addListener(EventPriority priority, Consumer<T> listener);

    <T extends Event> void addListener(EventPriority priority, Class<T> type, Consumer<T> listener);

    <T extends Event> void addListener(EventPriority priority, boolean receiveCanceled, Consumer<T> listener);

    <T extends Event> void addListener(EventPriority priority, boolean receiveCanceled, Class<T> type, Consumer<T> listener);

    <T extends Event> void addListener(boolean receiveCanceled, Consumer<T> listener);

    <T extends Event> void addListener(boolean receiveCanceled, Class<T> type, Consumer<T> listener);

    void unregister(Object target);

    <T extends Event> T post(T event);

    <T extends Event> T post(EventPriority priority, T event);

    void start();
}
