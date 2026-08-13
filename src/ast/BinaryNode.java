package ast;
public class BinaryNode extends ASTNode {
    public final ASTNode left;
    public final String operator;
    public final ASTNode right;

    public BinaryNode(ASTNode left, String operator, ASTNode right, int line, int column) {
        this.left = left;
        this.operator = operator;
        this.right = right;
        this.line = line;
        this.column = column;
    }

    @Override
    public String describe() {
        return "Binary(" + operator + ")";
    }
}
