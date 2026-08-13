package ast;

import java.util.List;

/** Root node: the whole program is a list of top-level statements */
public class ProgramNode extends ASTNode {
    public final List<ASTNode> statements;

    public ProgramNode(List<ASTNode> statements) {
        this.statements = statements;
        this.line = 0;
        this.column = 0;
    }

    @Override
    public String describe() {
        return "Program(" + statements.size() + " stmts)";
    }
}

