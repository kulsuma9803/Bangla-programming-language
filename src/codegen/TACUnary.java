package codegen;

/**
 * dest = op operand
 * Example:  t3 = না t0
 *
 * কথন has unary operators (logical না, unary minus) that the
 * Class 4 toy language did not need, so this class extends the
 * same one-class-per-instruction-kind pattern to cover them.
 */
public class TACUnary extends TACInstr {
    public final String dest;
    public final String op;
    public final String operand;

    public TACUnary(String dest, String op, String operand) {
        this.dest = dest;
        this.op = op;
        this.operand = operand;
    }

    @Override
    public String toString() {
        return dest + " = " + op + " " + operand;
    }
}
