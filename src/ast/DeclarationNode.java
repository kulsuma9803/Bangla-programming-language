package ast;

/** ধরি সংখ্যা x = 5;  OR  ধরি বাক্য নাম; */
public class DeclarationNode extends ASTNode {
    public final String typeName;   // "সংখ্যা" or "বাক্য"
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
