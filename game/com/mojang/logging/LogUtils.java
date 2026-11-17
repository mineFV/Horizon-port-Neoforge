package com.mojang.logging;

/** The stand-in for the game's LogUtils: a logger (HorizonLogger). */
public class LogUtils {
    public static org.slf4j.Logger getLogger() {
        return new HorizonLogger();
    }
}
