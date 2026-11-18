package horizonneo;

/** What each reason is a piece of (Blockers.piece): made-up reasons in the translator's words. */
final class BlockersTest {
    public static void main(String[] args) {
        Checks c = new Checks("Blockers");
        c.same("a call to the game: its class", "game: net.minecraft.core.SectionPos", Blockers.piece("it calls the game: net.minecraft.core.SectionPos.blockToSectionCoord(I)I"));
        c.same("a static field of the game: its class", "game: net.minecraft.world.item.DyeColor", Blockers.piece("it reads or writes a static field of the game: net.minecraft.world.item.DyeColor.BLACK"));
        c.same("an object of another library: that class", "other library: it.unimi.dsi.fastutil.objects.ObjectOpenHashSet",
                Blockers.piece("it makes an object of another library or mod: it.unimi.dsi.fastutil.objects.ObjectOpenHashSet"));
        c.same("a method the library model lacks: its class", "Java: java.lang.invoke.MethodHandles",
                Blockers.piece("it calls a method the library model doesn't have yet: java.lang.invoke.MethodHandles.lookup()Ljava/lang/invoke/MethodHandles$Lookup;"));
        c.same("a reason naming nothing: itself", "it has no body", Blockers.piece("it has no body"));
        System.exit(c.done());
    }
}
