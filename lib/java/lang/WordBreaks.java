package java.lang;

/**
 * The word boundaries of the JDK's BreakIterator.getWordInstance (its rule-based iterator's legacy rules), for
 * String.toLowerCase's final sigma alone: a word is letters with single in-word marks between them (dashes, the
 * apostrophe, the quote, the period, the underscore, the soft hyphen), a number digits with single in-number marks
 * (the comma among them); words and numbers that follow each other stay together, a number may take a sign before
 * (currency, '#', '.') and after ('%'...); runs of spaces, of kanji, katakana and hiragana stay together;
 * non-spacing, enclosing and format marks never start a word (but a non-spacing or enclosing one after an in-word
 * mark ends it). Thai is
 * letters. Checked against the JDK's on texts made at random (the test's, and 120 000 more). Not the JDK's API.
 */
final class WordBreaks {
    private WordBreaks() {}

    private static final int OTHER = 0, LET = 1, DGT = 2, KANJI = 3, KATA = 4, HIRA = 5, DIACRIT = 6, WS = 8;

    private static int kind(int c) {
        if (c == 0x3005 || (c >= 0x4e00 && c <= 0x9fa5) || (c >= 0xf900 && c <= 0xfa2d)) return KANJI;
        if ((c >= 0x30a1 && c <= 0x30fa) || c == 0x30fd || c == 0x30fe) return KATA;
        if ((c >= 0x3041 && c <= 0x3094) || c == 0x309d || c == 0x309e) return HIRA;
        if ((c >= 0x3099 && c <= 0x309c) || c == 0x30fb || c == 0x30fc) return DIACRIT;
        int t = Character.getType(c);
        if ((t >= Character.UPPERCASE_LETTER && t <= Character.OTHER_LETTER) || t == Character.COMBINING_SPACING_MARK) return LET;
        if (t >= Character.DECIMAL_DIGIT_NUMBER && t <= Character.OTHER_NUMBER) return DGT;
        if (t == Character.SPACE_SEPARATOR || c == '\t') return WS;
        return OTHER;
    }

    private static boolean ignored(int c) {
        // The soft hyphen, a format mark, is an in-word mark to these rules.
        if (c == 0x00ad) return false;
        int t = Character.getType(c);
        return t == Character.NON_SPACING_MARK || t == Character.ENCLOSING_MARK || t == Character.FORMAT;
    }

    private static boolean midWord(int c) {
        int t = Character.getType(c);
        return t == Character.DASH_PUNCTUATION || t == Character.CONNECTOR_PUNCTUATION || c == 0x00ad || c == 0x2027 || c == '"' || c == '\'' || c == '.';
    }

    private static boolean midNum(int c) {
        return c == '"' || c == '\'' || c == ',' || c == 0x066b || c == '.';
    }

    private static boolean preNum(int c) {
        return (Character.getType(c) == Character.CURRENCY_SYMBOL && c != 0x00a2) || c == '#' || c == '.';
    }

    private static boolean postNum(int c) {
        return c == '%' || c == '&' || c == 0x00a2 || c == 0x066a || c == 0x2030 || c == 0x2031;
    }

    private static boolean lineSeparator(int c) {
        return c == '\n' || c == '\f' || c == 0x2028 || c == 0x2029;
    }

    /**
     * The significant code points (their indexes in s), the marks never started on folded into the one before;
     * marked[k]: the k-th has non-spacing or enclosing marks folded in (an in-word or in-number mark with such marks
     * after it ends the word).
     */
    private static int[] significant(char[] s, int[] count, boolean[] marked) {
        int[] at = new int[s.length + 1];
        int n = 0;
        for (int i = 0; i < s.length; ) {
            int c = codePointAt(s, i);
            if (n == 0 || !ignored(c)) {
                at[n++] = i;
            } else if (Character.getType(c) != Character.FORMAT) {
                marked[n - 1] = true;
            }
            i += Character.charCount(c);
        }
        at[n] = s.length;
        count[0] = n;
        return at;
    }

    static int codePointAt(char[] s, int i) {
        char c = s[i];
        if (Character.isHighSurrogate(c) && i + 1 < s.length && Character.isLowSurrogate(s[i + 1])) return Character.toCodePoint(c, s[i + 1]);
        return c;
    }

    /**
     * Whether i (an index of s) is a word boundary, as the JDK's isBoundary answers: from the rules read forwards,
     * but just after a pair of surrogates (not at the start) always, since it backs up to the pair's low half and
     * reads on from it alone.
     */
    static boolean isBoundary(char[] s, int i) {
        if (i <= 0 || i >= s.length) return true;
        if (i >= 3 && Character.isLowSurrogate(s[i - 1]) && Character.isHighSurrogate(s[i - 2])) return true;
        int[] count = new int[1];
        boolean[] marked = new boolean[s.length + 1];
        int[] at = significant(s, count, marked);
        int n = count[0];
        int[] cp = new int[n];
        for (int k = 0; k < n; k++) cp[k] = codePointAt(s, at[k]);
        MARKED.set(marked);
        for (int k = 0; k < n; ) {
            int next = k + tokenLength(cp, k, n);
            if (at[next] == i) return true;
            if (at[next] > i) return false;
            k = next;
        }
        return false;
    }

    /** How many significant code points from k the longest rule takes (one at least). */
    private static int tokenLength(int[] cp, int k, int n) {
        int best = 1;
        best = Math.max(best, wordsAndNumbers(cp, k, n, false) - k);
        if (preNum(cp[k])) best = Math.max(best, wordsAndNumbers(cp, k + 1, n, true) - k);
        int w = k;
        while (w < n && kind(cp[w]) == WS) w++;
        if (w < n && cp[w] == '\r') w++;
        if (w < n && lineSeparator(cp[w])) w++;
        best = Math.max(best, w - k);
        for (int run : new int[] {KATA, HIRA, KANJI}) {
            int r = k;
            while (r < n && (kind(cp[r]) == run || (run <= HIRA && run >= KATA && kind(cp[r]) == DIACRIT))) r++;
            best = Math.max(best, r - k);
        }
        return best;
    }

    /** The end of words and numbers alternating from k (a number first when afterSign), with a number's sign after. */
    private static int wordsAndNumbers(int[] cp, int k, int n, boolean afterSign) {
        int i = k, end = afterSign ? k : -1;
        int last = 0;
        boolean first = true;
        while (i < n) {
            int c = kind(cp[i]);
            if (c == LET && !(afterSign && first) && last != LET) {
                i = word(cp, i, n);
                last = cp[i - 1] == 0x0964 || cp[i - 1] == 0x0965 ? -1 : LET;
            } else if (c == DGT && last != DGT) {
                i = number(cp, i, n);
                last = DGT;
            } else {
                break;
            }
            first = false;
            end = i;
            if (last == -1) {
                // A word that ends with a danda: only a number may follow.
                last = LET;
            }
        }
        if (last == DGT && i < n && postNum(cp[i])) end = i + 1;
        return end < 0 ? k : end;
    }

    /** The marks after each significant code point, for the text isBoundary reads (one thread's at a time). */
    private static final ThreadLocal<boolean[]> MARKED = new ThreadLocal<>();

    private static int word(int[] cp, int i, int n) {
        boolean[] marked = MARKED.get();
        while (i < n && kind(cp[i]) == LET) i++;
        while (i + 1 < n && midWord(cp[i]) && !marked[i] && kind(cp[i + 1]) == LET) {
            i++;
            while (i < n && kind(cp[i]) == LET) i++;
        }
        if (i < n && (cp[i] == 0x0964 || cp[i] == 0x0965)) i++;
        return i;
    }

    private static int number(int[] cp, int i, int n) {
        boolean[] marked = MARKED.get();
        while (i < n && kind(cp[i]) == DGT) i++;
        while (i + 1 < n && midNum(cp[i]) && !marked[i] && kind(cp[i + 1]) == DGT) {
            i++;
            while (i < n && kind(cp[i]) == DGT) i++;
        }
        return i;
    }
}
