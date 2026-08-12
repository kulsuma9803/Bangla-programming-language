package ast;


public class StringNode extends ASTNode {
    public final String value;

    public StringNode(String value, int line, int column) {
        this.value = value;
        this.line = line;
        this.column = column;
    }

    @Override
    public String describe() {
        return "String(\"" + value + "\")";
    }
}
