package ast;
public abstract class ASTNode {
    public int line;
    public int column;

    /**
     * Filled in by SemanticAnalyzer during analyze() — the static type
     * ("সংখ্যা", "বাক্য", or "boolean") this node resolves to. The Python
     * code generator reads this to decide, e.g., whether '+' needs str()
     * coercion, without re-running type inference from scratch.
     */
    public String resolvedType;

    public abstract String describe();
}
