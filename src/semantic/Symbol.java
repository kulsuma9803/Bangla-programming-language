package semantic;

public class Symbol {
    public final String name;
    public final String type; 
    public boolean initialized;

    public Symbol(String name, String type, boolean initialized) {
        this.name = name;
        this.type = type;
        this.initialized = initialized;
    }
}

