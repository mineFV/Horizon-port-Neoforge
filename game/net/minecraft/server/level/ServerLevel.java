package net.minecraft.server.level;

import net.minecraft.world.level.Level;
import net.minecraft.world.level.WorldGenLevel;

/** The stand-in for the game's ServerLevel: the level a server's block entities tick in (HorizonBridge makes it). */
public class ServerLevel extends Level implements WorldGenLevel {
    /** HorizonBridge's: the game's own constructor takes the server, which isn't modelled. */
    public ServerLevel() {
        super(false);
    }

    /** Not modelled: what closing a level does is the game's. */
    @Override
    public native void close();

    /**
     * Observed (`dump`'s exporter): "ServerLevel[" and the level's name "]". The name is the server's default one,
     * "world": the name of the world Horizon runs isn't read (game/README.md).
     */
    @Override
    public String toString() {
        return "ServerLevel[world]";
    }
}
