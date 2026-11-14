# The translator's model of Java's library

What `translate` puts in place of the classes of Java's library that a mod uses: written here, from the Java SE
API specification, never copied from a JDK. It is compiled by `build.sh` (`javac --patch-module java.base=lib`)
and translated to Rust with the mod, by the same translator; a method marked `native` is written in Rust by
`Library.java` instead. A member the model lacks is a member the translator refuses, with why.

Messages of the exceptions the model throws follow the JDK the tests run on (`test/translate/`), which compare
them with the real thing. Two are known to differ, and say so in the module: a NullPointerException has no
message (the JDK builds one from the bytecode), nor has a ClassCastException (the JDK's names class loaders).
