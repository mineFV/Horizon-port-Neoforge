package horizonneo;

import java.util.List;
import java.util.Set;

/** What `check` reads of a Rust file without a compiler: brackets, and the functions outside any block. */
final class RustTextTest {
    private RustTextTest() {}

    public static void main(String[] args) throws Exception {
        Checks c = new Checks("Rust text");

        // Comments and literals are blanked, at the same places.
        String text = "let a = \"x{\"; // }\nlet b = '{'; /* ( /* [ */ ) */ let c = r#\"\"}\"#;\n";
        String code = RustText.code(text);
        c.same("the length is kept", text.length(), code.length());
        c.same("the lines are kept", 2L, code.chars().filter(x -> x == '\n').count());
        c.yes("no bracket of a string, a character or a comment is left", !code.matches("(?s).*[{}()\\[\\]].*"));
        c.yes("the code itself is left", code.startsWith("let a = \"  \";") && code.contains("let c = r#\""));
        c.same("an escaped quote in a string", "let s = \"      \"; x", RustText.code("let s = \"a\\\"b{c\"; x"));
        c.same("a byte string", "b\"  \" y", RustText.code("b\"({\" y"));
        c.same("a raw string with more hashes", "r##\"     \"## z", RustText.code("r##\"a\"#{b\"## z"));
        c.same("a lifetime is left as it is", "fn f<'a>(x: &'a str) -> Game<'_> {}", RustText.code("fn f<'a>(x: &'a str) -> Game<'_> {}"));
        c.same("an escaped character", "let q = '  '; let n = '  '; {", RustText.code("let q = '\\''; let n = '\\n'; {"));
        c.same("a name ending in r isn't a raw string", "for\"x\"".length(), RustText.code("for\"x\"").length());
        c.same("a nested block comment", "a" + " ".repeat(13) + "b", RustText.code("a /* /* */ */ b"));

        // Brackets.
        c.same("paired", List.of(), RustText.brackets(RustText.code("fn f(a: [i32; 3]) { g((a[0])); }")));
        c.same("a string's bracket doesn't count", List.of(), RustText.brackets(RustText.code("fn f() { let s = \"}\"; let c = '}'; // }\n}")));
        c.same("never closed", List.of("line 1: `{` is never closed"), RustText.brackets(RustText.code("fn f() {\n    g();\n")));
        c.same("closes nothing", List.of("line 2: `}` closes nothing"), RustText.brackets(RustText.code("fn f() {}\n}\n")));
        c.same("closes another", List.of("line 2: `]` closes the `{` of line 1"), RustText.brackets(RustText.code("fn f() {\n    g();]\n}")));
        c.same("the wrong one", List.of("line 1: `]` closes the `(` of line 1"), RustText.brackets("g(a];"));

        // Functions outside any block, and what they take.
        List<RustText.Function> f = RustText.functions(RustText.code("""
                // fn not_this(a: i32) {}
                fn first(game: &mut crate::game::Game<'_>, pos: [i32; 3], name: Option<&str>, f: impl Fn(i32, i32) -> bool,) {
                    fn inner(a: i32) {}
                    let s = "fn nor_this()";
                }
                pub(super) fn second<'a, T: Into<String>>(x: T) -> Vec<(i32, i32)> { Vec::new() }
                fn third() {}
                """));
        c.same("their names", List.of("first", "second", "third"), f.stream().map(RustText.Function::name).toList());
        c.same("their parameters", List.of(4, 1, 0), f.stream().map(RustText.Function::parameters).toList());
        c.same("their lines", List.of(2, 6, 7), f.stream().map(RustText.Function::line).toList());
        c.same("a tuple pattern is one parameter", 2, RustText.functions("fn f((a, b): (i32, i32), c: u8) {}").get(0).parameters());
        c.same("a list that doesn't end", -1, RustText.functions("fn f(a: i32").get(0).parameters());
        c.same("no function", List.of(), RustText.functions(RustText.code("struct S; // fn x()")));

        // What a hook point takes, from its signature.
        c.same("self and the rest", Set.of("self", "pos", "canceled"), Check.taken("fn native_destroy_block(&mut self, pos: [i32; 3], canceled: &mut bool)"));
        c.same("their order", List.of("self", "a", "b"), List.copyOf(Check.taken("fn f(&self, a: Vec<(i32, i32)>, mut b: impl Fn(u8) -> u8) -> bool")));
        c.same("no receiver", Set.of("x"), Check.taken("fn f(x: u8)"));
        c.same("nothing", Set.of(), Check.taken("fn f()"));
        c.same("no signature", Set.of(), Check.taken(null));

        System.exit(c.done());
    }
}
