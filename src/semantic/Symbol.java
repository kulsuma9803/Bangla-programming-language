package semantic;
public class Symbol {
    public final String name;
    public final String type; // "সংখ্যা" (Integer) or "বাক্য" (String)
    public boolean initialized;

    public Symbol(String name, String type, boolean initialized) {
        this.name = name;
        this.type = type;
        this.initialized = initialized;
    }
}
