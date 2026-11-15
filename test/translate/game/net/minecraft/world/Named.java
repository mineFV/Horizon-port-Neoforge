package net.minecraft.world;

/** Made up for the translator's tests: a stand-in for an interface of the game, in a stand-in server's libraries. */
public interface Named {
    String name();

    default int weight() {
        return 1;
    }
}
