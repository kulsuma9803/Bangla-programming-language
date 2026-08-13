package ast;

public class IfNode extends ASTNode {
    public final ASTNode condition;
    public final BlockNode thenBranch;
    public final BlockNode elseBranch; 

    public IfNode(ASTNode condition, BlockNode thenBranch, BlockNode elseBranch, int line, int column) {
        this.condition = condition;
        this.thenBranch = thenBranch;
        this.elseBranch = elseBranch;
        this.line = line;
        this.column = column;
    }

    @Override
    public String describe() {
        return "If" + (elseBranch != null ? "-Else" : "");
    }
}
