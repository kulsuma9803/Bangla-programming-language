package codegen;

/**
 * Base class — every TAC instruction inherits from this.
 *
 * This mirrors the object-oriented TAC design taught in Class 4:
 * each *kind* of instruction (BinOp, Copy, Print, ...) gets its own
 * small class instead of a raw formatted string, and toString()
 * controls how it is printed.
 */
public abstract class TACInstr {
}
