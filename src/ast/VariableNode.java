package ast;

/** Reference to a variable by name, e.g. `x` used inside an expression */
public class VariableNode extends ASTNode {
    public final String name;

    public VariableNode(String name, int line, int column) {
        this.name = name;
        this.line = line;
        this.column = column;
    }

    @Override
    public String describe() {
        return "Variable(" + name + ")";
    }
}
