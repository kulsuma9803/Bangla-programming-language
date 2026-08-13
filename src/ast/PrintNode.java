package ast;

/** দেখাও(expression); */
public class PrintNode extends ASTNode {
    public final ASTNode expression;

    public PrintNode(ASTNode expression, int line, int column) {
        this.expression = expression;
        this.line = line;
        this.column = column;
    }

    @Override
    public String describe() {
        return "Print";
    }
}
