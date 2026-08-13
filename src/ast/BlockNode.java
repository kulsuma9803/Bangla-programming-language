package ast;

import java.util.List;

/** { statement* } — a lexical scope */
public class BlockNode extends ASTNode {
    public final List<ASTNode> statements;

    public BlockNode(List<ASTNode> statements, int line, int column) {
        this.statements = statements;
        this.line = line;
        this.column = column;
    }

    @Override
    public String describe() {
        return "Block(" + statements.size() + " stmts)";
    }
}

