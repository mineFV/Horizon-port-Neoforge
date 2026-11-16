package com.mojang.datafixers.types;

import com.mojang.datafixers.kinds.App;

/** The stand-in for DataFixerUpper's Type: its supertypes alone (BlockEntityType.Builder.build takes one, mods pass null). */
public abstract class Type<A> implements App {
    protected Type() {}

    /** Not modelled: DataFixerUpper's types aren't (mods pass null). */
    @Override
    public native boolean equals(Object o);
}
