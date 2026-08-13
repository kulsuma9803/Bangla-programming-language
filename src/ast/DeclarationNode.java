package ast;

public class DeclarationNode extends ASTNode {
    public final String typeName;  
    public final String varName;
    public final ASTNode initializer; // may be null if no initial value given

    public DeclarationNode(String typeName, String varName, ASTNode initializer, int line, int column) {
        this.typeName = typeName;
        this.varName = varName;
        this.initializer = initializer;
        this.line = line;
        this.column = column;
    }

    @Override
    public String describe() {
        return "Declaration(" + typeName + " " + varName + ")";
    }
}
