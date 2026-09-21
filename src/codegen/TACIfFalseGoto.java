package codegen;

/**
 * শর্ত cond মিথ্যা হলে L-এ যাও
 * Conditional jump: if cond evaluates to false, jump to L.
 * See TACLabel for why this exists alongside the Class-4-style
 * instruction classes.
 */
public class TACIfFalseGoto extends TACInstr {
    public final String cond;
    public final String label;

    public TACIfFalseGoto(String cond, String label) {
        this.cond = cond;
        this.label = label;
    }

    @Override
    public String toString() {
        return "শর্ত " + cond + " মিথ্যা হলে " + label + "-এ যাও";
    }
}



