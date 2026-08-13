package ast;
public class UnaryNode extends ASTNode {
    public final String operator;
    public final ASTNode operand;

    public UnaryNode(String operator, ASTNode operand, int line, int column) {
        this.operator = operator;
        this.operand = operand;
        this.line = line;
        this.column = column;
    }

    @Override
    public String describe() {
        return "Unary(" + operator + ")";
    }
}
