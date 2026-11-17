package horizonneo.probe;

import java.util.function.Supplier;
import net.minecraft.Util;
import net.minecraft.resources.ResourceLocation;
import org.slf4j.helpers.FormattingTuple;
import org.slf4j.helpers.MessageFormatter;

/** slf4j's formatting, the levels a server's loggers write (in a server only), and Util's description ids and make. */
final class LogProbe {
    private LogProbe() {}

    static void run() {
        Object[][] cases = {{"a {} b", "x"}, {"{}{}", "x", "y"}, {"{} {} {}", "x"}, {"no holes", "x"}, {"\\{} {}", "x"}, {"\\\\{} {}", "x", "y"},
            {"a {} b {}", "x", new RuntimeException("boom")}, {"a {}", new RuntimeException("boom")}, {"{}", (Object) new int[] {1, 2}},
            {"{}", (Object) new Object[] {"p", new String[] {"q"}}}, {"{}", null}, {"{", "x"}, {"}{}{", "x"}, {null, "x"}, {"{}"}};
        for (Object[] c : cases) {
            p("log format " + java.util.Arrays.deepToString(c), () -> {
                Object[] args = java.util.Arrays.copyOfRange(c, 1, c.length);
                FormattingTuple t = MessageFormatter.arrayFormat((String) c[0], args);
                return t.getMessage() + " | " + (t.getThrowable() == null ? "none" : t.getThrowable().getMessage()) + " | " + (t.getArgArray() == null ? "null" : t.getArgArray().length);
            });
        }
        p("log two", () -> MessageFormatter.format("{} and {}", "a", "b").getMessage() + " | " + MessageFormatter.format("{}", null).getMessage()
                + " | " + MessageFormatter.format("{}", new RuntimeException("e")).getMessage() + " | " + MessageFormatter.arrayFormat("x", null).getMessage());
        p("util ids", () -> Util.makeDescriptionId("item", ResourceLocation.parse("a:b/c/d")) + " " + Util.makeDescriptionId("block", null) + " "
                + Util.makeDescriptionId("x", ResourceLocation.withDefaultNamespace("y")));
        java.util.UUID id = new java.util.UUID(0x1234L, 0x5678L);
        p("game profile", () -> {
            com.mojang.authlib.GameProfile g = new com.mojang.authlib.GameProfile(id, "[Mekanism]");
            return g.toString().replaceFirst("@[0-9a-f]+\\[", "@?[") + " | " + g.getId() + " | " + g.getName() + " | " + g.equals(new com.mojang.authlib.GameProfile(id, "[Mekanism]")) + " | " + g.equals(new com.mojang.authlib.GameProfile(id, "x"))
                    + " | " + g.hashCode() + " | " + new com.mojang.authlib.GameProfile(id, "").hashCode();
        });
        p("game profile null id", () -> new com.mojang.authlib.GameProfile(null, "x"));
        p("game profile null name", () -> new com.mojang.authlib.GameProfile(id, null));
        p("util make", () -> Util.make(new java.util.ArrayList<String>(), l -> l.add("z")) + " " + Util.make(() -> "made"));
    }

    /** In a server: which levels its loggers write. */
    static void levels() {
        p("log levels", () -> {
            org.slf4j.Logger l = com.mojang.logging.LogUtils.getLogger();
            return l.isTraceEnabled() + " " + l.isDebugEnabled() + " " + l.isInfoEnabled() + " " + l.isWarnEnabled() + " " + l.isErrorEnabled();
        });
    }

    static void p(String key, Supplier<Object> call) {
        GameProbe.p(key, call);
    }
}
