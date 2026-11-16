package net.minecraft.nbt;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/**
 * The stand-in for the game's CompoundTag, observed (`standins`): a HashMap of tags (its keys iterate in a HashMap's
 * order), written with its keys sorted, read with a default for a missing key or another kind.
 */
public class CompoundTag implements Tag {
    private final Map<String, Tag> tags;

    public CompoundTag() {
        this(new HashMap<>());
    }

    /** Observed: the same as CompoundTag(), its keys in the same order. */
    public CompoundTag(int size) {
        this(new HashMap<>());
    }

    private CompoundTag(Map<String, Tag> tags) {
        this.tags = tags;
    }

    public Set<String> getAllKeys() {
        return tags.keySet();
    }

    @Override
    public byte getId() {
        return 10;
    }

    public int size() {
        return tags.size();
    }

    /** Observed: what the key held before; a null tag throws, naming the key. */
    public Tag put(String key, Tag tag) {
        if (tag == null) throw new IllegalArgumentException("Invalid null NBT value with key " + key);
        return tags.put(key, tag);
    }

    public void putByte(String key, byte v) {
        tags.put(key, ByteTag.valueOf(v));
    }

    public void putShort(String key, short v) {
        tags.put(key, ShortTag.valueOf(v));
    }

    public void putInt(String key, int v) {
        tags.put(key, IntTag.valueOf(v));
    }

    public void putLong(String key, long v) {
        tags.put(key, LongTag.valueOf(v));
    }

    /** Observed: four ints, the most significant first. */
    public void putUUID(String key, UUID v) {
        long m = v.getMostSignificantBits(), l = v.getLeastSignificantBits();
        tags.put(key, new IntArrayTag(new int[] {(int) (m >> 32), (int) m, (int) (l >> 32), (int) l}));
    }

    /** Observed: a missing key throws a NullPointerException; another kind, or another length, an IllegalArgumentException. */
    public UUID getUUID(String key) {
        Tag t = get(key);
        String name = kindName(t.getId());
        if (!(t instanceof IntArrayTag a)) throw new IllegalArgumentException("Expected UUID-Tag to be of type INT[], but found " + name + ".");
        int[] v = a.getAsIntArray();
        if (v.length != 4) throw new IllegalArgumentException("Expected UUID-Array to be of length 4, but found " + v.length + ".");
        return new UUID((long) v[0] << 32 | v[1] & 0xFFFFFFFFL, (long) v[2] << 32 | v[3] & 0xFFFFFFFFL);
    }

    /** Observed in getUUID's messages: each kind's name. */
    private static String kindName(byte id) {
        return switch (id) {
            case 1 -> "BYTE";
            case 2 -> "SHORT";
            case 3 -> "INT";
            case 4 -> "LONG";
            case 5 -> "FLOAT";
            case 6 -> "DOUBLE";
            case 7 -> "BYTE[]";
            case 8 -> "STRING";
            case 9 -> "LIST";
            case 10 -> "COMPOUND";
            case 11 -> "INT[]";
            case 12 -> "LONG[]";
            default -> "END";
        };
    }

    public boolean hasUUID(String key) {
        Tag t = get(key);
        return t instanceof IntArrayTag a && a.getAsIntArray().length == 4;
    }

    public void putFloat(String key, float v) {
        tags.put(key, FloatTag.valueOf(v));
    }

    public void putDouble(String key, double v) {
        tags.put(key, DoubleTag.valueOf(v));
    }

    public void putString(String key, String v) {
        tags.put(key, StringTag.valueOf(v));
    }

    /** Observed: the tag holds the array given, not a copy. */
    public void putByteArray(String key, byte[] v) {
        tags.put(key, new ByteArrayTag(v));
    }

    public void putIntArray(String key, int[] v) {
        tags.put(key, new IntArrayTag(v));
    }

    public void putLongArray(String key, long[] v) {
        tags.put(key, new LongArrayTag(v));
    }

    public void putBoolean(String key, boolean v) {
        tags.put(key, ByteTag.valueOf(v));
    }

    public Tag get(String key) {
        return tags.get(key);
    }

    /** Observed: the tag's kind, 0 when there is none. */
    public byte getTagType(String key) {
        Tag t = tags.get(key);
        return t == null ? 0 : t.getId();
    }

    public boolean contains(String key) {
        return tags.containsKey(key);
    }

    /** Observed: the kind is the one asked (so a missing key is of kind 0), or a number and 99 was asked. */
    public boolean contains(String key, int type) {
        int t = getTagType(key);
        if (t == type) return true;
        return type == 99 && t >= 1 && t <= 6;
    }

    private NumericTag number(String key) {
        return contains(key, 99) ? (NumericTag) tags.get(key) : null;
    }

    public byte getByte(String key) {
        NumericTag n = number(key);
        return n == null ? 0 : n.getAsByte();
    }

    public short getShort(String key) {
        NumericTag n = number(key);
        return n == null ? 0 : n.getAsShort();
    }

    public int getInt(String key) {
        NumericTag n = number(key);
        return n == null ? 0 : n.getAsInt();
    }

    public long getLong(String key) {
        NumericTag n = number(key);
        return n == null ? 0L : n.getAsLong();
    }

    public float getFloat(String key) {
        NumericTag n = number(key);
        return n == null ? 0.0F : n.getAsFloat();
    }

    public double getDouble(String key) {
        NumericTag n = number(key);
        return n == null ? 0.0 : n.getAsDouble();
    }

    public String getString(String key) {
        return contains(key, 8) ? tags.get(key).getAsString() : "";
    }

    /** Observed: the tag's own array (the same object each time), else a new empty one. */
    public byte[] getByteArray(String key) {
        return contains(key, 7) ? ((ByteArrayTag) tags.get(key)).getAsByteArray() : new byte[0];
    }

    public int[] getIntArray(String key) {
        return contains(key, 11) ? ((IntArrayTag) tags.get(key)).getAsIntArray() : new int[0];
    }

    public long[] getLongArray(String key) {
        return contains(key, 12) ? ((LongArrayTag) tags.get(key)).getAsLongArray() : new long[0];
    }

    /** Observed: the compound held (the same object), else a new empty one, not put. */
    public CompoundTag getCompound(String key) {
        return contains(key, 10) ? (CompoundTag) tags.get(key) : new CompoundTag();
    }

    /** Observed: the list held when it is empty or of that kind, else a new empty one, not put. */
    public ListTag getList(String key, int type) {
        if (getTagType(key) == 9) {
            ListTag l = (ListTag) tags.get(key);
            if (l.isEmpty() || l.getElementType() == type) return l;
        }
        return new ListTag();
    }

    public boolean getBoolean(String key) {
        return getByte(key) != 0;
    }

    public void remove(String key) {
        tags.remove(key);
    }

    /** Observed: the keys sorted, each bare when it is made of letters, digits and ._+- only, else quoted as a StringTag. */
    @Override
    public String toString() {
        List<String> keys = new ArrayList<>(tags.keySet());
        Collections.sort(keys);
        StringBuilder b = new StringBuilder("{");
        for (int i = 0; i < keys.size(); i++) {
            if (i > 0) b.append(',');
            String k = keys.get(i);
            b.append(bare(k) ? k : StringTag.quoteAndEscape(k)).append(':').append(tags.get(k));
        }
        return b.append('}').toString();
    }

    private static boolean bare(String k) {
        if (k.isEmpty()) return false;
        for (int i = 0; i < k.length(); i++) {
            char c = k.charAt(i);
            boolean ok = c >= 'a' && c <= 'z' || c >= 'A' && c <= 'Z' || c >= '0' && c <= '9' || c == '.' || c == '_' || c == '+' || c == '-';
            if (!ok) return false;
        }
        return true;
    }

    public boolean isEmpty() {
        return tags.isEmpty();
    }

    /** Observed: each tag copied (numbers and texts are the same objects, compounds, lists and arrays new ones). */
    @Override
    public CompoundTag copy() {
        Map<String, Tag> copied = new HashMap<>();
        for (Map.Entry<String, Tag> e : tags.entrySet()) copied.put(e.getKey(), e.getValue().copy());
        return new CompoundTag(copied);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        return o instanceof CompoundTag other && java.util.Objects.equals(tags, other.tags);
    }

    @Override
    public int hashCode() {
        return tags.hashCode();
    }

    /** Observed: each of other's tags copied in, a compound into the compound already there; gives this one. */
    public CompoundTag merge(CompoundTag other) {
        for (String key : other.tags.keySet()) {
            Tag t = other.tags.get(key);
            if (t.getId() == 10 && contains(key, 10)) getCompound(key).merge((CompoundTag) t);
            else put(key, t.copy());
        }
        return this;
    }
}
