package net.neoforged.neoforge.capabilities;

/** The stand-in for NeoForge's ICapabilityProvider. */
@FunctionalInterface
public interface ICapabilityProvider<O, C, T> {
    T getCapability(O object, C context);
}
