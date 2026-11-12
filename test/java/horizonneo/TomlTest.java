package horizonneo;

import java.util.List;
import java.util.Map;

/** The TOML reader, on the forms mod metadata uses and on text that isn't TOML. */
final class TomlTest {
    private TomlTest() {}

    public static void main(String[] args) throws Exception {
        Checks c = new Checks("TOML");

        Map<String, Object> r = Toml.parse("""
                # a comment
                modLoader = "javafml"   # after a value
                loaderVersion='[1,)'
                count = 42
                ratio = 1.5
                big = 1_000
                hex = 0xFF
                minus = -7
                on = true
                off = false
                when = 2026-11-12T10:00:00Z
                """).root();
        c.same("a basic string", "javafml", r.get("modLoader"));
        c.same("a literal string", "[1,)", r.get("loaderVersion"));
        c.same("an integer", 42L, r.get("count"));
        c.same("a float", 1.5, r.get("ratio"));
        c.same("underscores in a number", 1000L, r.get("big"));
        c.same("a hexadecimal number", 255L, r.get("hex"));
        c.same("a negative number", -7L, r.get("minus"));
        c.same("true", true, r.get("on"));
        c.same("false", false, r.get("off"));
        c.same("a date stays text", "2026-11-12T10:00:00Z", r.get("when"));
        c.same("the file's order is kept", List.of("modLoader", "loaderVersion", "count", "ratio", "big", "hex", "minus", "on", "off", "when"),
                List.copyOf(r.keySet()));

        // Strings.
        r = Toml.parse("""
                escapes = "tab\\there \\"q\\" \\\\ \\u00e9 \\U0001F600"
                literal = 'C:\\path\\n'
                multi = \"\"\"
                First line.
                Second "quoted" line.\\
                     joined.\"\"\"
                raw = '''
                keeps \\n and "quotes"
                '''
                quoted = \"\"\"\"x\"\"\"\"
                empty = ""
                """).root();
        c.same("escapes", "tab\there \"q\" \\ \u00e9 \uD83D\uDE00", r.get("escapes"));
        c.same("a literal string keeps its backslashes", "C:\\path\\n", r.get("literal"));
        c.same("a multi-line string: first line end dropped, `\\` joins lines", "First line.\nSecond \"quoted\" line.joined.", r.get("multi"));
        c.same("a multi-line literal string", "keeps \\n and \"quotes\"\n", r.get("raw"));
        c.same("quotes just inside a multi-line string", "\"x\"", r.get("quoted"));
        c.same("an empty string", "", r.get("empty"));

        r = Toml.parse("\uFEFFa = 1\r\nb = '''\r\nx\r\ny'''\r\nc = \"z\"\r\n").root();
        c.same("a byte order mark and Windows line ends", List.of(1L, "x\ny", "z"), List.copyOf(r.values()));
        c.same("no line end at the end of the file", "v", Toml.parse("k = \"v\"").root().get("k"));
        c.yes("an empty file is an empty table", Toml.parse("").root().isEmpty());

        // Tables and arrays of tables.
        r = Toml.parse("""
                top = "level"

                [[mods]]
                modId = "one"

                [[mods]]
                modId = "two"

                [[dependencies.one]]
                modId = "neoforge"
                [[ dependencies . one ]]
                modId = "minecraft"

                [features.one]
                javaVersion = "[21,)"

                [modproperties."my.mod"]
                key = "v"
                nested.deep = 3
                """).root();
        List<Map<String, Object>> mods = Toml.tables(r.get("mods"));
        c.same("an array of tables", List.of("one", "two"), mods.stream().map(m -> Toml.text(m, "modId")).toList());
        List<Map<String, Object>> deps = Toml.tables(Toml.table(r.get("dependencies")).get("one"));
        c.same("an array of tables under a dotted name", List.of("neoforge", "minecraft"), deps.stream().map(m -> Toml.text(m, "modId")).toList());
        c.same("a table under a dotted name", "[21,)", Toml.text(Toml.table(Toml.table(r.get("features")).get("one")), "javaVersion"));
        Map<String, Object> props = Toml.table(Toml.table(r.get("modproperties")).get("my.mod"));
        c.same("a quoted name keeps its dot", "v", Toml.text(props, "key"));
        c.same("a dotted key makes a table", 3L, Toml.table(props.get("nested")).get("deep"));
        c.same("keys before the first table stay at the top", "level", r.get("top"));

        // Arrays and inline tables.
        r = Toml.parse("""
                services = []
                requiredMods = [ "a", "b", ]   # a trailing comma
                spread = [
                  1,   # a comment inside
                  2,

                  3
                ]
                nested = [[1, 2], ["x"]]
                point = { x = 1, y = "two", list = [true, false] }
                rows = [ { id = "a" }, { id = "b" } ]
                """).root();
        c.same("an empty array", List.of(), r.get("services"));
        c.same("an array with a trailing comma", List.of("a", "b"), Toml.texts(r.get("requiredMods")));
        c.same("an array over several lines", List.of(1L, 2L, 3L), r.get("spread"));
        c.same("arrays in an array", List.of(List.of(1L, 2L), List.of("x")), r.get("nested"));
        c.same("an inline table", Map.of("x", 1L, "y", "two", "list", List.of(true, false)), r.get("point"));
        c.same("inline tables in an array", List.of("a", "b"), Toml.tables(r.get("rows")).stream().map(m -> Toml.text(m, "id")).toList());
        c.same("texts() leaves out what isn't text", List.of("a"), Toml.texts(List.of("a", 1L, true)));
        c.same("text() of something that isn't text", null, Toml.text(r, "spread"));
        c.yes("table() of something that isn't a table", Toml.table(r.get("spread")).isEmpty());

        // Looser than TOML, on purpose.
        r = Toml.parse("[[dependencies.${mod_id}]]\nmodId = \"x\"\n").root();
        c.yes("a header left over from a build template still reads", Toml.table(r.get("dependencies")).containsKey("${mod_id}"));
        Toml.Doc twice = Toml.parse("a = 1\na = 1\n");
        c.same("a key set twice to the same value: kept", 1L, twice.root().get("a"));
        c.same("... with a warning naming the line", List.of("line 2: `a` is set twice, to the same value"), twice.warnings());

        // Not TOML: refused, with the line.
        c.refuses("a key set twice to different values", "line 2: `a` is set twice, to different values", () -> Toml.parse("a = 1\na = 2\n"));
        c.refuses("a string never closed", "line 1: this string is never closed", () -> Toml.parse("a = \"open\nb = 1\n"));
        c.refuses("a multi-line string never closed", "line 2: this string is never closed", () -> Toml.parse("a = 1\nb = '''\nx\ny\n"));
        c.refuses("text without quotes", "line 2: can't read the value `oops`", () -> Toml.parse("a = 1\nb = oops\n"));
        c.refuses("something after a value", "line 1: unexpected `y` after the value", () -> Toml.parse("a = \"x\" y\n"));
        c.refuses("a header never closed", "line 1: expected `]`", () -> Toml.parse("[a\nb = 1\n"));
        c.refuses("an array header never closed", "line 1: expected `]]`", () -> Toml.parse("[[a]\nb = 1\n"));
        c.refuses("no key", "line 1: a key is missing", () -> Toml.parse("= 1\n"));
        c.refuses("no equals sign", "line 1: expected `=` after the key `a`", () -> Toml.parse("a 1\n"));
        c.refuses("no value", "line 1: a value is missing", () -> Toml.parse("a = \n"));
        c.refuses("an unknown escape", "isn't an escape TOML has", () -> Toml.parse("a = \"\\q\"\n"));
        c.refuses("a table over a value", "line 2: `a` already has a value, so it can't be a table", () -> Toml.parse("a = 1\n[a]\n"));
        c.refuses("a table over an array of tables", "line 2: `a` already has a value", () -> Toml.parse("[[a]]\n[a]\n"));
        c.refuses("an array of tables over a table", "line 2: `a` already has a value, so it can't be an array of tables", () -> Toml.parse("[a]\n[[a]]\n"));
        c.refuses("nesting in a value", "line 2: `a` is a value, so nothing can be nested in it", () -> Toml.parse("a = 1\n[a.b]\n"));
        c.refuses("an array never closed", "this array is never closed", () -> Toml.parse("a = [1, 2\n"));
        c.refuses("the line of a problem inside an array", "line 6: can't read the value `x`", () -> Toml.parse("a = 1\n\n\nb = [1,\n 2,\n x]\n"));
        c.refuses("a number too big", "can't read the number", () -> Toml.parse("a = 99999999999999999999\n"));

        System.exit(c.done());
    }
}
