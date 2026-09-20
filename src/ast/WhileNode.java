package ast;

/** যতক্ষণ (condition) { ... } — while loop */
public class WhileNode extends ASTNode {
    public final ASTNode condition;
    public final BlockNode body;

    public WhileNode(ASTNode condition, BlockNode body, int line, int column) {
        this.condition = condition;
        this.body = body;
        this.line = line;
        this.column = column;
    }

    @Override
    public String describe() {
        return "While";
    }
}
