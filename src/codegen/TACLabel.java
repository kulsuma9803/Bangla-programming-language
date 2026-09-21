package codegen;

/**
 * L:
 * A jump target. কথন needs if-else/while (which the Class 4 toy
 * language didn't have), so this class — together with TACGoto and
 * TACIfFalseGoto — extends the same one-class-per-kind TAC design
 * to cover textbook-style label/jump control flow.
 */
public class TACLabel extends TACInstr {
    public final String name;

    public TACLabel(String name) {
        this.name = name;
    }

    @Override
    public String toString() {
        return name + ":";
    }
}

