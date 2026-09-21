package codegen;

/**
 * dest = left op right
 * Example:  t2 = t0 + t1
 * All three operand fields are strings — a temp name, a variable
 * name, or a literal, exactly as in the Class 4 TAC design.
 */
public class TACBinOp extends TACInstr {
    public final String dest;
    public final String left;
    public final String op;
    public final String right;

    public TACBinOp(String dest, String left, String op, String right) {
        this.dest = dest;
        this.left = left;
        this.op = op;
        this.right = right;
    }

    @Override
    public String toString() {
        return dest + " = " + left + " " + op + " " + right;
    }
}
