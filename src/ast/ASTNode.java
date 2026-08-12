package ast;
public abstract class ASTNode {
    public int line;
    public int column;

    public abstract String describe();
}
