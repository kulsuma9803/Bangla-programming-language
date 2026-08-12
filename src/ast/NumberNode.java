package ast;

public class NumberNode extends ASTNode {
    public final int value;

    public NumberNode(int value, int line, int column) {
        this.value = value;
        this.line = line;
        this.column = column;
    }

    @Override
    public String describe() {
        return "Number(" + value + ")";
    }
}
