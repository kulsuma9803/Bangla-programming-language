package codegen;

/**
 * যাও L
 * Unconditional jump to a label ("যাও" = "go to"). See TACLabel for
 * why this exists alongside the Class-4-style instruction classes.
 */
public class TACGoto extends TACInstr {
    public final String label;

    public TACGoto(String label) {
        this.label = label;
    }

    @Override
    public String toString() {
        return "যাও " + label;
    }
}
