package ast;

public class BooleanNode extends ASTNode {
    public final boolean value;

    public BooleanNode(boolean value, int line, int column) {
        this.value = value;
        this.line = line;
        this.column = column;
    }

    @Override
    public String describe() {
        return "Boolean(" + value + ")";
    }
}