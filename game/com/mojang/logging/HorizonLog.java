package com.mojang.logging;

/** Not the game's: a log line to Horizon's output (the translation's native: the standard error, with its level). */
final class HorizonLog {
    private HorizonLog() {}

    static native void write(String level, String text);
}
