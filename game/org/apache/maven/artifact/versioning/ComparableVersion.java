package org.apache.maven.artifact.versioning;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.List;
import java.util.Locale;

/**
 * The stand-in for Maven's ComparableVersion, from Maven's documented version order ("POM Reference", version order
 * specification), checked against Maven's own answers (`standins`, probe/ModProbe.java): a version is a list of
 * numbers and qualifiers, cut at '.', '-' and where digits and letters meet; trailing nulls (0, "", "final", "ga",
 * "release") are dropped; qualifiers order alpha < beta < milestone < rc = cr < snapshot < "" = final = ga = release
 * < sp, then any other, alphabetically.
 */
public class ComparableVersion implements Comparable<ComparableVersion> {
    private static final List<String> QUALIFIERS = List.of("alpha", "beta", "milestone", "rc", "snapshot", "", "sp");
    private static final String RELEASE = String.valueOf(QUALIFIERS.indexOf(""));

    private final String value;
    private final ListItem items;
    private final String canonical;

    public ComparableVersion(String version) {
        this.value = version;
        this.items = parse(version.toLowerCase(Locale.ROOT));
        this.canonical = items.toString();
    }

    private interface Item {
        int compareTo(Item item);

        boolean isNull();
    }

    /** A number, of any size: its digits without leading zeros ("0" for zero); BigInteger isn't in the model. */
    private static final class IntItem implements Item {
        final String digits;

        IntItem(String text) {
            int i = 0;
            while (i < text.length() - 1 && text.charAt(i) == '0') i++;
            digits = text.substring(i);
        }

        boolean zero() {
            return digits.equals("0");
        }

        int compare(IntItem other) {
            if (digits.length() != other.digits.length()) return Integer.compare(digits.length(), other.digits.length());
            int c = digits.compareTo(other.digits);
            return Integer.signum(c);
        }

        @Override
        public int compareTo(Item item) {
            if (item == null) return zero() ? 0 : 1;
            if (item instanceof IntItem i) return compare(i);
            return 1;
        }

        @Override
        public boolean isNull() {
            return zero();
        }

        @Override
        public boolean equals(Object o) {
            return o instanceof IntItem i && digits.equals(i.digits);
        }

        /** Observed: an int's as itself, a long's folded, else BigInteger's (31 times over its 32-bit words). */
        @Override
        public int hashCode() {
            if (digits.length() <= 18) {
                long v = Long.parseLong(digits);
                return v <= Integer.MAX_VALUE ? (int) v : Long.hashCode(v);
            }
            if (digits.length() == 19 && digits.compareTo("9223372036854775807") <= 0) return Long.hashCode(Long.parseLong(digits));
            // BigInteger's: its magnitude's 32-bit words, most significant first.
            int[] decimal = new int[digits.length()];
            for (int i = 0; i < decimal.length; i++) decimal[i] = digits.charAt(i) - '0';
            java.util.ArrayList<Integer> words = new java.util.ArrayList<>();
            boolean any = true;
            while (any) {
                long rem = 0;
                any = false;
                for (int i = 0; i < decimal.length; i++) {
                    long cur = rem * 10 + decimal[i];
                    decimal[i] = (int) (cur >>> 32);
                    rem = cur & 0xFFFFFFFFL;
                    if (decimal[i] != 0) any = true;
                }
                // decimal holds the quotient by 2^32 in base 10 digits that may exceed 9: normalise.
                long carry = 0;
                for (int i = decimal.length - 1; i >= 0; i--) {
                    long d = decimal[i] + carry;
                    decimal[i] = (int) (d % 10);
                    carry = d / 10;
                }
                words.add(0, (int) rem);
            }
            int hash = 0;
            for (int w : words) hash = (int) (31 * hash + (w & 0xFFFFFFFFL));
            return hash;
        }

        @Override
        public String toString() {
            return digits;
        }
    }

    private static final class StringItem implements Item {
        final String value;

        StringItem(String value, boolean followedByDigit) {
            if (followedByDigit && value.length() == 1) {
                value = switch (value.charAt(0)) {
                    case 'a' -> "alpha";
                    case 'b' -> "beta";
                    case 'm' -> "milestone";
                    default -> value;
                };
            }
            this.value = switch (value) {
                case "ga", "final", "release" -> "";
                case "cr" -> "rc";
                default -> value;
            };
        }

        static String comparable(String qualifier) {
            int i = QUALIFIERS.indexOf(qualifier);
            return i == -1 ? QUALIFIERS.size() + "-" + qualifier : String.valueOf(i);
        }

        @Override
        public int compareTo(Item item) {
            if (item == null) return comparable(value).compareTo(RELEASE);
            if (item instanceof StringItem s) return comparable(value).compareTo(comparable(s.value));
            return -1;
        }

        @Override
        public boolean isNull() {
            return comparable(value).compareTo(RELEASE) == 0;
        }

        @Override
        public boolean equals(Object o) {
            return o instanceof StringItem s && value.equals(s.value);
        }

        @Override
        public int hashCode() {
            return value.hashCode();
        }

        @Override
        public String toString() {
            return value;
        }
    }

    private static final class ListItem extends ArrayList<Item> implements Item {
        private static final long serialVersionUID = 1L;

        @Override
        public boolean isNull() {
            return isEmpty();
        }

        void normalize() {
            for (int i = size() - 1; i >= 0; i--) {
                Item last = get(i);
                if (last.isNull()) remove(i);
                else if (!(last instanceof ListItem)) break;
            }
        }

        @Override
        public int compareTo(Item item) {
            if (item == null) {
                for (Item i : this) {
                    int r = i.compareTo(null);
                    if (r != 0) return r;
                }
                return 0;
            }
            if (item instanceof IntItem) return -1;
            if (item instanceof StringItem) return 1;
            ListItem other = (ListItem) item;
            int n = Math.max(size(), other.size());
            for (int k = 0; k < n; k++) {
                Item l = k < size() ? get(k) : null;
                Item r = k < other.size() ? other.get(k) : null;
                int result = l == null ? (r == null ? 0 : -1 * r.compareTo(null)) : l.compareTo(r);
                if (result != 0) return result;
            }
            return 0;
        }

        @Override
        public String toString() {
            StringBuilder b = new StringBuilder();
            for (Item item : this) {
                if (b.length() > 0) b.append(item instanceof ListItem ? '-' : '.');
                b.append(item);
            }
            return b.toString();
        }
    }

    private static ListItem parse(String version) {
        ListItem items = new ListItem();
        ListItem list = items;
        Deque<ListItem> stack = new ArrayDeque<>();
        stack.push(list);
        boolean isDigit = false;
        int start = 0;
        for (int i = 0; i < version.length(); i++) {
            char c = version.charAt(i);
            if (c == '.') {
                list.add(i == start ? new IntItem("0") : item(isDigit, version.substring(start, i)));
                start = i + 1;
            } else if (c == '-') {
                list.add(i == start ? new IntItem("0") : item(isDigit, version.substring(start, i)));
                start = i + 1;
                ListItem next = new ListItem();
                list.add(next);
                list = next;
                stack.push(list);
            } else if (c >= '0' && c <= '9') {
                if (!isDigit && i > start) {
                    list.add(new StringItem(version.substring(start, i), true));
                    start = i;
                    ListItem next = new ListItem();
                    list.add(next);
                    list = next;
                    stack.push(list);
                }
                isDigit = true;
            } else {
                if (isDigit && i > start) {
                    list.add(item(true, version.substring(start, i)));
                    start = i;
                    ListItem next = new ListItem();
                    list.add(next);
                    list = next;
                    stack.push(list);
                }
                isDigit = false;
            }
        }
        if (version.length() > start) list.add(item(isDigit, version.substring(start)));
        while (!stack.isEmpty()) stack.pop().normalize();
        return items;
    }

    private static Item item(boolean isDigit, String text) {
        return isDigit ? new IntItem(text) : new StringItem(text, false);
    }

    @Override
    public int compareTo(ComparableVersion other) {
        return items.compareTo(other.items);
    }

    @Override
    public boolean equals(Object o) {
        return o instanceof ComparableVersion other && items.equals(other.items);
    }

    /** Observed: its items' hash, a list's. */
    @Override
    public int hashCode() {
        return items.hashCode();
    }

    @Override
    public String toString() {
        return value;
    }

    public String getCanonical() {
        return canonical;
    }
}
