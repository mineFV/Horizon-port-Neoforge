package horizonneo.probe;

import java.util.function.Supplier;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.CommonComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.chat.Style;
import net.minecraft.network.chat.TextColor;
import net.minecraft.network.chat.contents.TranslatableContents;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

/**
 * Texts: formats, colours, styles, literal and translated texts, their plain text and names. In a server only (the
 * game's language is the server's, English): the stand-ins are given the texts of the keys asked (given()).
 */
final class TextProbe {
    private TextProbe() {}

    /** The server's English texts of the keys asked, as it gave them; the other keys asked have none. */
    static final String TEXTS = "item.minecraft.diamond=Diamond\nblock.minecraft.stone=Stone\ngui.done=Done\nchat.type.text=<%s> %s\nitem.minecraft.diamond_sword=Diamond Sword";

    private static void given() {
        try {
            Class.forName("net.minecraft.locale.HorizonLanguage").getMethod("given", String.class, boolean.class).invoke(null, TEXTS, true);
        } catch (ClassNotFoundException e) {
            // The game's own language.
        } catch (ReflectiveOperationException e) {
            throw new IllegalStateException(e);
        }
    }

    static void run() {
        given();
        for (net.minecraft.network.chat.ClickEvent.Action a : net.minecraft.network.chat.ClickEvent.Action.values()) {
            p("click action " + a.ordinal(), () -> a + " " + a.name() + " " + a.getSerializedName() + " " + a.isAllowedFromServer());
        }
        p("click event", () -> {
            net.minecraft.network.chat.ClickEvent e = new net.minecraft.network.chat.ClickEvent(net.minecraft.network.chat.ClickEvent.Action.OPEN_URL, "https://x.y");
            net.minecraft.network.chat.ClickEvent same = new net.minecraft.network.chat.ClickEvent(net.minecraft.network.chat.ClickEvent.Action.OPEN_URL, "https://x.y");
            net.minecraft.network.chat.Style s = net.minecraft.network.chat.Style.EMPTY.withBold(true).withClickEvent(e);
            net.minecraft.network.chat.Style t = net.minecraft.network.chat.Style.EMPTY.withClickEvent(same).withBold(true);
            return e + " " + e.getAction() + " " + e.getValue() + " " + e.equals(same) + " " + (e.hashCode() == same.hashCode()) + " " + s + " " + s.equals(t) + " "
                    + (s.hashCode() == t.hashCode()) + " " + (s.getClickEvent() == e) + " " + s.getHoverEvent() + " " + s.withClickEvent(null) + " "
                    + net.minecraft.network.chat.Style.EMPTY.withClickEvent(e).isEmpty() + " " + net.minecraft.network.chat.Style.EMPTY.withClickEvent(e).withClickEvent(null).isEmpty()
                    + " " + net.minecraft.network.chat.Style.EMPTY.withItalic(true).applyTo(s) + " " + s.applyFormat(net.minecraft.ChatFormatting.RED)
                    + " " + s.applyFormat(net.minecraft.ChatFormatting.RESET);
        });
        p("style hash", () -> net.minecraft.network.chat.Style.EMPTY.withBold(true).hashCode() + " " + net.minecraft.network.chat.Style.EMPTY.hashCode() + " "
                + net.minecraft.network.chat.Style.EMPTY.withColor(0x123456).withItalic(false).hashCode());
        for (ChatFormatting f : ChatFormatting.values()) {
            p("formatting " + f.name(), () -> f + " " + f.getChar() + " " + f.getId() + " " + f.isFormat() + " " + f.isColor() + " " + f.getColor() + " " + f.getName() + " "
                    + f.getSerializedName() + " " + TextColor.fromLegacyFormat(f) + " " + (ChatFormatting.getByCode(f.getChar()) == f) + " " + ChatFormatting.getById(f.getId()));
        }
        p("formatting codes", () -> ChatFormatting.getByCode('A') + " " + ChatFormatting.getByCode('z') + " " + ChatFormatting.getById(-5) + " " + ChatFormatting.getById(99) + " " + (int) ChatFormatting.PREFIX_CODE);
        int[] rgbs = {0, 0xFF5555, 0x123456, -1, 0x1000000, 0xABCDEF};
        for (int rgb : rgbs) {
            p("text color " + rgb, () -> {
                TextColor c = TextColor.fromRgb(rgb);
                return c + " " + c.getValue() + " " + c.serialize() + " " + c.equals(TextColor.fromRgb(rgb)) + " " + c.equals(TextColor.fromLegacyFormat(ChatFormatting.RED))
                        + " " + (c.hashCode() == TextColor.fromRgb(rgb).hashCode());
            });
        }
        p("text color legacy", () -> (TextColor.fromLegacyFormat(ChatFormatting.RED) == TextColor.fromLegacyFormat(ChatFormatting.RED)) + " " + TextColor.fromLegacyFormat(ChatFormatting.BOLD)
                + " " + TextColor.fromLegacyFormat(ChatFormatting.RESET) + " " + TextColor.fromLegacyFormat(ChatFormatting.RED).equals(TextColor.fromRgb(0xFF5555)));
        p("style empty", () -> Style.EMPTY + " " + Style.EMPTY.isEmpty() + " " + Style.EMPTY.isBold() + " " + Style.EMPTY.getColor() + " " + (Style.EMPTY.withBold(null) == Style.EMPTY)
                + " " + (Style.EMPTY.withBold(true).withBold(null) == Style.EMPTY) + " " + Style.EMPTY.withBold(true).withBold(null).isEmpty());
        p("style one", () -> Style.EMPTY.withBold(true) + " | " + Style.EMPTY.withColor(ChatFormatting.RED) + " | " + Style.EMPTY.withColor(0x123456) + " | " + Style.EMPTY.withItalic(false)
                + " | " + Style.EMPTY.withUnderlined(true).withStrikethrough(true).withObfuscated(false));
        p("style formats", () -> {
            StringBuilder b = new StringBuilder();
            for (ChatFormatting f : ChatFormatting.values()) b.append(Style.EMPTY.withItalic(false).applyFormat(f)).append(" | ");
            return b;
        });
        p("style many", () -> Style.EMPTY.applyFormats(ChatFormatting.BOLD, ChatFormatting.RED, ChatFormatting.ITALIC) + " | " + Style.EMPTY.applyFormats(ChatFormatting.BOLD, ChatFormatting.RESET)
                + " | " + Style.EMPTY.withBold(true).applyTo(Style.EMPTY.withColor(ChatFormatting.BLUE).withBold(false)) + " | " + Style.EMPTY.applyTo(Style.EMPTY.withItalic(true))
                + " | " + Style.EMPTY.withColor(ChatFormatting.RED).applyFormat(ChatFormatting.GREEN));
        p("style equal", () -> Style.EMPTY.withBold(true).equals(Style.EMPTY.withBold(true)) + " " + Style.EMPTY.withBold(true).equals(Style.EMPTY.withBold(false))
                + " " + (Style.EMPTY.withBold(true).hashCode() == Style.EMPTY.withBold(true).hashCode()) + " " + Style.EMPTY.withBold(true).isBold() + " " + Style.EMPTY.withBold(false).isBold()
                + " " + Style.EMPTY.withBold(false).isEmpty() + " " + Style.EMPTY.withColor(ChatFormatting.RED).getColor());
        p("text literal", () -> Component.literal("hi") + " | " + Component.literal("") + " | " + Component.empty() + " | " + Component.literal("hi").getString()
                + " | " + Component.literal("a").append("b").append(Component.literal("c").append("d")) + " | " + Component.literal("a").append("b").append(Component.literal("c").append("d")).getString()
                + " | " + Component.literal("x").append(""));
        p("text styled", () -> Component.literal("hi").withStyle(ChatFormatting.RED) + " | " + Component.literal("hi").withStyle(ChatFormatting.BOLD, ChatFormatting.AQUA).append("x")
                + " | " + Component.literal("hi").withStyle(s -> s.withItalic(true)) + " | " + Component.literal("hi").withStyle(Style.EMPTY.withBold(true)).withStyle(Style.EMPTY.withBold(false).withColor(ChatFormatting.RED))
                + " | " + Component.literal("hi").withColor(0x10) + " | " + Component.literal("hi").withStyle(ChatFormatting.RED).getStyle());
        p("text translated", () -> Component.translatable("item.minecraft.diamond") + " | " + Component.translatable("item.minecraft.diamond").getString()
                + " | " + Component.translatable("horizonprobe.none").getString() + " | " + Component.translatableWithFallback("horizonprobe.none", "fell back").getString()
                + " | " + Component.translatableWithFallback("gui.done", "fell back").getString() + " | " + Component.translatableWithFallback("gui.done", "fell back"));
        Object[][] args = {{}, {"x"}, {"x", "y"}, {"x", "y", "z"}, {Component.literal("lit").withStyle(ChatFormatting.RED), 3}, {null, true}, {1.5, 'c'}};
        for (Object[] a : args) {
            p("text args " + java.util.Arrays.toString(a), () -> Component.translatable("chat.type.text", a) + " | " + Component.translatable("chat.type.text", a).getString());
        }
        String[] templates = {"a %s b", "%2$s then %1$s", "100%% %s", "%d", "%", "end %", "%s %s %s", "%1$s %s", "%3$s", "%0$s", "plain", "%%s", "%S"};
        for (String t : templates) {
            p("text template " + t, () -> Component.translatableWithFallback("horizonprobe.none", t, "x", "y").getString());
        }
        p("text equal", () -> Component.literal("a").equals(Component.literal("a")) + " " + Component.literal("a").equals(Component.literal("a").withStyle(ChatFormatting.RED))
                + " " + Component.translatable("k", "x").equals(Component.translatable("k", "x")) + " " + Component.translatable("k").equals(Component.translatableWithFallback("k", "f"))
                + " " + (Component.literal("a").append("b").hashCode() == Component.literal("a").append("b").hashCode()) + " " + Component.empty().equals(Component.literal(""))
                + " " + new TranslatableContents("k", null, new Object[] {"x"}) + " " + TranslatableContents.isAllowedPrimitiveArgument(3) + " " + TranslatableContents.isAllowedPrimitiveArgument('c'));
        p("text copies", () -> {
            MutableComponent m = Component.literal("a").withStyle(ChatFormatting.RED).append("b");
            return m.copy() + " | " + m.plainCopy() + " | " + m.copy().equals(m) + " | " + (m.copy().getSiblings() == m.getSiblings()) + " | " + Component.nullToEmpty(null) + " | " + Component.nullToEmpty("z");
        });
        p("text common", () -> CommonComponents.EMPTY + " | " + CommonComponents.SPACE + " | " + CommonComponents.NEW_LINE + " | " + CommonComponents.ELLIPSIS + " | " + CommonComponents.space()
                + " | " + (CommonComponents.space() == CommonComponents.SPACE));
        p("text names", () -> Items.DIAMOND.getDescriptionId() + " | " + Items.DIAMOND.getDescription() + " | " + Items.DIAMOND.getName(new ItemStack(Items.DIAMOND)) + " | "
                + new ItemStack(Items.DIAMOND).getHoverName() + " | " + new ItemStack(Items.DIAMOND).getHoverName().getString() + " | " + ComponentProbe.PLAIN.get().getDescriptionId()
                + " | " + new ItemStack(ComponentProbe.EPIC.get()).getHoverName() + " | " + CapabilityProbe.BLOCK.get().getDescriptionId() + " | " + CapabilityProbe.BLOCK.get().getName()
                + " | " + ItemStack.EMPTY.getHoverName());
    }

    static void p(String key, Supplier<Object> call) {
        GameProbe.p(key, call);
    }
}
