package semantic;

import java.util.HashMap;
import java.util.Map;
public class SymbolTable {
    private final java.util.List<Map<String, Symbol>> scopes = new java.util.ArrayList<>();

    public SymbolTable() {
        pushScope(); // global scope
    }

    public void pushScope() {
        scopes.add(new HashMap<>());
    }

    public void popScope() {
        if (scopes.size() > 1) scopes.remove(scopes.size() - 1);
    }

    public boolean declare(String name, String type, boolean initialized) {
        Map<String, Symbol> current = scopes.get(scopes.size() - 1);
        if (current.containsKey(name)) return false;
        current.put(name, new Symbol(name, type, initialized));
        return true;
    }

    
    public Symbol resolve(String name) {
        for (int i = scopes.size() - 1; i >= 0; i--) {
            Symbol s = scopes.get(i).get(name);
            if (s != null) return s;
        }
        return null;
    }

    public boolean isDeclaredInCurrentScope(String name) {
        return scopes.get(scopes.size() - 1).containsKey(name);
    }

    
    public java.util.Collection<Symbol> getGlobalScopeSymbols() {
        return scopes.get(0).values();
    }
}

