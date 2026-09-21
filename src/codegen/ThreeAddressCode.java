package codegen;

import java.util.ArrayList;
import java.util.List;

/**
 * Holds the Three-Address Code (TAC) instructions produced by
 * TACGenerator — one flat, ordered list of textbook-style
 * "x = y op z" / "goto L" / "ifFalse x goto L" / "L:" instructions.
 *
 * This is the compiler's intermediate representation (IR): a
 * language-independent, linear form of the program that sits between
 * the AST (front-end) and the generated target code (back-end).
 *
 * Each instruction is now an object (TACBinOp, TACCopy, TACPrint, ...)
 * rather than a pre-formatted string, matching the object-oriented
 * TAC design taught in Class 4 — toString() on each instruction
 * class controls how it prints, and callers can also inspect an
 * instruction's fields (dest, left, op, right, ...) directly instead
 * of re-parsing text.
 */
public class ThreeAddressCode {
    public final List<TACInstr> instructions = new ArrayList<>();

    public void emit(TACInstr instr) {
        instructions.add(instr);
    }
}

