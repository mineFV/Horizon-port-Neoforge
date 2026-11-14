package horizonneo;

import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.List;

/**
 * A method's bytecode as instructions (the Java Virtual Machine Specification, chapter 6): every opcode, its
 * operands, and for branches and switches where they go, as code offsets. Nothing is interpreted here; the
 * translator (`MethodRust`) gives instructions their meaning.
 */
final class Bytecode {
    private Bytecode() {}

    /**
     * One instruction at code offset pc. index: a local's slot, a constant pool index, or the number pushed by
     * bipush and sipush. value: iinc's increment, newarray's type, multianewarray's dimensions. targets: where a
     * branch goes; for a switch, its default first, then each case's. keys: a switch's case values, in the
     * order of targets after the default. Unused operands are 0 or empty.
     */
    record Insn(int pc, int op, int index, int value, int[] targets, int[] keys) {
        String name() {
            return Bytecode.name(op);
        }
    }

    /** The code doesn't decode: an unknown opcode, or an instruction that runs past the end. */
    static final class Invalid extends RuntimeException {
        private static final long serialVersionUID = 1L;

        Invalid(String message) {
            super(message);
        }
    }

    // ---- opcodes ----

    static final int NOP = 0, ACONST_NULL = 1, ICONST_M1 = 2, ICONST_5 = 8, LCONST_0 = 9, LCONST_1 = 10, FCONST_0 = 11, FCONST_2 = 13,
            DCONST_0 = 14, DCONST_1 = 15, BIPUSH = 16, SIPUSH = 17, LDC = 18, LDC_W = 19, LDC2_W = 20,
            ILOAD = 21, LLOAD = 22, FLOAD = 23, DLOAD = 24, ALOAD = 25, ILOAD_0 = 26, ALOAD_3 = 45,
            IALOAD = 46, SALOAD = 53, ISTORE = 54, ASTORE = 58, ISTORE_0 = 59, ASTORE_3 = 78, IASTORE = 79, AASTORE = 83, SASTORE = 86,
            POP = 87, POP2 = 88, DUP = 89, DUP_X1 = 90, DUP_X2 = 91, DUP2 = 92, DUP2_X1 = 93, DUP2_X2 = 94, SWAP = 95,
            IADD = 96, LXOR = 131, IINC = 132, I2L = 133, I2S = 147, LCMP = 148, FCMPL = 149, FCMPG = 150, DCMPL = 151, DCMPG = 152,
            IFEQ = 153, IFLE = 158, IF_ICMPEQ = 159, IF_ICMPLE = 164, IF_ACMPEQ = 165, IF_ACMPNE = 166, GOTO = 167, JSR = 168, RET = 169,
            TABLESWITCH = 170, LOOKUPSWITCH = 171, IRETURN = 172, LRETURN = 173, FRETURN = 174, DRETURN = 175, ARETURN = 176, RETURN = 177,
            GETSTATIC = 178, PUTSTATIC = 179, GETFIELD = 180, PUTFIELD = 181, INVOKEVIRTUAL = 182, INVOKESPECIAL = 183, INVOKESTATIC = 184,
            INVOKEINTERFACE = 185, INVOKEDYNAMIC = 186, NEW = 187, NEWARRAY = 188, ANEWARRAY = 189, ARRAYLENGTH = 190, ATHROW = 191,
            CHECKCAST = 192, INSTANCEOF = 193, MONITORENTER = 194, MONITOREXIT = 195, WIDE = 196, MULTIANEWARRAY = 197, IFNULL = 198,
            IFNONNULL = 199, GOTO_W = 200, JSR_W = 201;

    private static final String[] NAMES = (
            "nop aconst_null iconst_m1 iconst_0 iconst_1 iconst_2 iconst_3 iconst_4 iconst_5 lconst_0 lconst_1 fconst_0 fconst_1 fconst_2 "
            + "dconst_0 dconst_1 bipush sipush ldc ldc_w ldc2_w iload lload fload dload aload iload_0 iload_1 iload_2 iload_3 lload_0 lload_1 "
            + "lload_2 lload_3 fload_0 fload_1 fload_2 fload_3 dload_0 dload_1 dload_2 dload_3 aload_0 aload_1 aload_2 aload_3 iaload laload "
            + "faload daload aaload baload caload saload istore lstore fstore dstore astore istore_0 istore_1 istore_2 istore_3 lstore_0 "
            + "lstore_1 lstore_2 lstore_3 fstore_0 fstore_1 fstore_2 fstore_3 dstore_0 dstore_1 dstore_2 dstore_3 astore_0 astore_1 astore_2 "
            + "astore_3 iastore lastore fastore dastore aastore bastore castore sastore pop pop2 dup dup_x1 dup_x2 dup2 dup2_x1 dup2_x2 swap "
            + "iadd ladd fadd dadd isub lsub fsub dsub imul lmul fmul dmul idiv ldiv fdiv ddiv irem lrem frem drem ineg lneg fneg dneg ishl "
            + "lshl ishr lshr iushr lushr iand land ior lor ixor lxor iinc i2l i2f i2d l2i l2f l2d f2i f2l f2d d2i d2l d2f i2b i2c i2s lcmp "
            + "fcmpl fcmpg dcmpl dcmpg ifeq ifne iflt ifge ifgt ifle if_icmpeq if_icmpne if_icmplt if_icmpge if_icmpgt if_icmple if_acmpeq "
            + "if_acmpne goto jsr ret tableswitch lookupswitch ireturn lreturn freturn dreturn areturn return getstatic putstatic getfield "
            + "putfield invokevirtual invokespecial invokestatic invokeinterface invokedynamic new newarray anewarray arraylength athrow "
            + "checkcast instanceof monitorenter monitorexit wide multianewarray ifnull ifnonnull goto_w jsr_w").split(" ");

    static String name(int op) {
        return op >= 0 && op < NAMES.length ? NAMES[op] : "opcode " + op;
    }

    // ---- decoding ----

    static List<Insn> decode(byte[] code) {
        ByteBuffer in = ByteBuffer.wrap(code);
        List<Insn> out = new ArrayList<>();
        int pc = 0;
        try {
            while (pc < code.length) {
                int op = code[pc] & 0xFF;
                Insn insn;
                int size;
                if (op >= NAMES.length) throw new Invalid("unknown opcode " + op + " at " + pc);
                if (op == BIPUSH) {
                    insn = plain(pc, op, code[pc + 1]);
                    size = 2;
                } else if (op == SIPUSH) {
                    insn = plain(pc, op, in.getShort(pc + 1));
                    size = 3;
                } else if (op == LDC || (op >= ILOAD && op <= ALOAD) || (op >= ISTORE && op <= ASTORE) || op == RET) {
                    insn = plain(pc, op, code[pc + 1] & 0xFF);
                    size = 2;
                } else if (op == NEWARRAY) {
                    insn = new Insn(pc, op, 0, code[pc + 1] & 0xFF, new int[0], new int[0]);
                    size = 2;
                } else if (op == LDC_W || op == LDC2_W || (op >= GETSTATIC && op <= INVOKESTATIC) || op == NEW || op == ANEWARRAY
                        || op == CHECKCAST || op == INSTANCEOF) {
                    insn = plain(pc, op, in.getShort(pc + 1) & 0xFFFF);
                    size = 3;
                } else if (op == IINC) {
                    insn = new Insn(pc, op, code[pc + 1] & 0xFF, code[pc + 2], new int[0], new int[0]);
                    size = 3;
                } else if ((op >= IFEQ && op <= JSR) || op == IFNULL || op == IFNONNULL) {
                    insn = new Insn(pc, op, 0, 0, new int[] {pc + in.getShort(pc + 1)}, new int[0]);
                    size = 3;
                } else if (op == GOTO_W || op == JSR_W) {
                    insn = new Insn(pc, op, 0, 0, new int[] {pc + in.getInt(pc + 1)}, new int[0]);
                    size = 5;
                } else if (op == INVOKEINTERFACE || op == INVOKEDYNAMIC) {
                    insn = plain(pc, op, in.getShort(pc + 1) & 0xFFFF);
                    size = 5;
                } else if (op == MULTIANEWARRAY) {
                    insn = new Insn(pc, op, in.getShort(pc + 1) & 0xFFFF, code[pc + 3] & 0xFF, new int[0], new int[0]);
                    size = 4;
                } else if (op == WIDE) {
                    int real = code[pc + 1] & 0xFF;
                    if (real == IINC) {
                        insn = new Insn(pc, real, in.getShort(pc + 2) & 0xFFFF, in.getShort(pc + 4), new int[0], new int[0]);
                        size = 6;
                    } else if ((real >= ILOAD && real <= ALOAD) || (real >= ISTORE && real <= ASTORE) || real == RET) {
                        insn = plain(pc, real, in.getShort(pc + 2) & 0xFFFF);
                        size = 4;
                    } else {
                        throw new Invalid("wide " + name(real) + " at " + pc);
                    }
                } else if (op == TABLESWITCH || op == LOOKUPSWITCH) {
                    int at = pc + 1 + (4 - (pc + 1) % 4) % 4;
                    int fallback = pc + in.getInt(at);
                    if (op == TABLESWITCH) {
                        int low = in.getInt(at + 4), high = in.getInt(at + 8);
                        long count = (long) high - low + 1;
                        if (count < 0 || count > code.length) throw new Invalid("a tableswitch at " + pc + " is too big");
                        int[] targets = new int[(int) count + 1], keys = new int[(int) count];
                        targets[0] = fallback;
                        for (int i = 0; i < count; i++) {
                            keys[i] = low + i;
                            targets[i + 1] = pc + in.getInt(at + 12 + 4 * i);
                        }
                        insn = new Insn(pc, op, 0, 0, targets, keys);
                        size = at + 12 + 4 * (int) count - pc;
                    } else {
                        int pairs = in.getInt(at + 4);
                        if (pairs < 0 || pairs > code.length) throw new Invalid("a lookupswitch at " + pc + " is too big");
                        int[] targets = new int[pairs + 1], keys = new int[pairs];
                        targets[0] = fallback;
                        for (int i = 0; i < pairs; i++) {
                            keys[i] = in.getInt(at + 8 + 8 * i);
                            targets[i + 1] = pc + in.getInt(at + 12 + 8 * i);
                        }
                        insn = new Insn(pc, op, 0, 0, targets, keys);
                        size = at + 8 + 8 * pairs - pc;
                    }
                } else if (op >= ILOAD_0 && op <= ALOAD_3) {
                    // xload_n: as xload n.
                    insn = plain(pc, ILOAD + (op - ILOAD_0) / 4, (op - ILOAD_0) % 4);
                    size = 1;
                } else if (op >= ISTORE_0 && op <= ASTORE_3) {
                    insn = plain(pc, ISTORE + (op - ISTORE_0) / 4, (op - ISTORE_0) % 4);
                    size = 1;
                } else {
                    insn = plain(pc, op, 0);
                    size = 1;
                }
                for (int t : insn.targets()) {
                    if (t < 0 || t >= code.length) throw new Invalid(name(op) + " at " + pc + " goes outside the code (" + t + ")");
                }
                out.add(insn);
                pc += size;
            }
        } catch (IndexOutOfBoundsException e) {
            throw new Invalid("an instruction at " + pc + " runs past the end of the code");
        }
        return List.copyOf(out);
    }

    private static Insn plain(int pc, int op, int index) {
        return new Insn(pc, op, index, 0, new int[0], new int[0]);
    }
}
