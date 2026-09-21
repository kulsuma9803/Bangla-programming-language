package codegen;

/**
 * দেখাও(src)
 * The কথন equivalent of Class 4's "print src" instruction — displayed
 * using the language's own দেখাও keyword instead of the English
 * "call ... " form, so the TAC listing reads fully in বাংলা.
 */
public class TACPrint extends TACInstr {
    public final String src;

    public TACPrint(String src) {
        this.src = src;
    }

    @Override
    public String toString() {
        return "দেখাও(" + src + ")";
    }
}
