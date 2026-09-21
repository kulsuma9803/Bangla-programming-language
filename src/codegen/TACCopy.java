package codegen;

/**
 * dest = src
 * Used for two purposes, exactly as in the Class 4 design:
 *   1. Storing a literal/temp into a variable:  বয়স = t2
 *   2. A plain copy with no operation involved.
 */
public class TACCopy extends TACInstr {
    public final String dest;
    public final String src;

    public TACCopy(String dest, String src) {
        this.dest = dest;
        this.src = src;
    }

    @Override
    public String toString() {
        return dest + " = " + src;
    }
}
