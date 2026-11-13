package horizonneo;

import java.util.List;
import java.util.Map;

/** The JSON reader, on what Mixin configs and the tool's own files hold, and on text that isn't JSON. */
final class JsonTest {
    private JsonTest() {}

    public static void main(String[] args) throws Exception {
        Checks c = new Checks("JSON");

        Map<String, Object> o = Json.object(Json.parse("""
                {
                  // a comment, as Mixin configs have
                  "required": true,
                  "package": "com.example.mixin",
                  "priority": 1000,
                  "ratio": 0.5,
                  "nothing": null,
                  "client": ["A", "B",],   /* a comment between things */
                  "nested": { "list": [1, [2, 3], {"x": "y"}] },
                }
                """));
        c.same("text", "com.example.mixin", Json.text(o, "package"));
        c.same("true", true, o.get("required"));
        c.same("a whole number", 1000L, o.get("priority"));
        c.same("a fraction", 0.5, o.get("ratio"));
        c.yes("null", o.containsKey("nothing") && o.get("nothing") == null);
        c.same("an array with a comma before its end", List.of("A", "B"), o.get("client"));
        c.same("nesting", List.of(1L, List.of(2L, 3L), Map.of("x", "y")), Json.object(o.get("nested")).get("list"));
        c.same("the text's order is kept", List.of("required", "package", "priority", "ratio", "nothing", "client", "nested"), List.copyOf(o.keySet()));
        c.same("escapes", "a\"b\\c/d\n\t\u00e9", Json.parse("\"a\\\"b\\\\c\\/d\\n\\t\\u00e9\""));
        c.same("a number on its own", -12L, Json.parse(" -12 "));
        c.same("an exponent", 1500.0, Json.parse("1.5e3"));
        c.same("an empty object and an empty array", List.of(Map.of(), List.of()), Json.parse("[{}, []]"));
        c.same("a byte order mark", List.of(1L), Json.parse("\uFEFF[1]"));
        c.same("object() of something else", Map.of(), Json.object("x"));
        c.same("array() of something else", List.of(), Json.array(3L));
        c.same("text() of something else", null, Json.text(o, "priority"));
        c.same("what it writes, it reads back", o.get("nested"), Json.parse(Json.write(o.get("nested"))));

        c.refuses("text never closed", "line 1: this text is never closed", () -> Json.parse("\"abc"));
        c.refuses("a name without quotes", "line 2: expected a name in quotes", () -> Json.parse("{\n  name: 1\n}"));
        c.refuses("no colon", "expected `:` after the name `a`", () -> Json.parse("{\"a\" 1}"));
        c.refuses("something after the value", "unexpected text after the value", () -> Json.parse("{} x"));
        c.refuses("an unknown word", "can't read the value `yes`", () -> Json.parse("[yes]"));
        c.refuses("an array never closed", "the text ends too soon", () -> Json.parse("[1, 2"));
        c.refuses("nothing at all", "a value is missing", () -> Json.parse(""));
        c.refuses("an unknown escape", "isn't an escape JSON has", () -> Json.parse("\"\\q\""));
        c.refuses("a comment never closed", "this comment is never closed", () -> Json.parse("[1] /* x"));

        System.exit(c.done());
    }
}
