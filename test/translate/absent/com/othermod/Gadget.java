package com.othermod;

/** Another mod's class: Guarded is compiled against it, and it isn't there when Guarded runs. */
public class Gadget {
    public static int count = 1;
    public int size;

    public static int value() {
        return 2;
    }
}
