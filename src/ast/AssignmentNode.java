package ast;

/** x = expression; */
public class AssignmentNode extends ASTNode {
    public final String varName;
    public final ASTNode value;

    public AssignmentNode(String varName, ASTNode value, int line, int column) {
        this.varName = varName;
        this.value = value;
        this.line = line;
        this.column = column;
    }

    @Override
    public String describe() {
        return "Assignment(" + varName + ")";
    }
}
