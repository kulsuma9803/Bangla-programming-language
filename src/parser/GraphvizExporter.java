package parser;

import ast.*;
public class GraphvizExporter {

    private final StringBuilder sb = new StringBuilder();
    private int counter = 0;

    public String export(ProgramNode program) {
        sb.setLength(0);
        counter = 0;
        sb.append("digraph Kothon_AST {\n");
        sb.append("  node [shape=box, fontname=\"Nirmala UI\", style=filled, fillcolor=\"#eef6ff\"];\n");
        sb.append("  edge [fontname=\"Nirmala UI\"];\n");
        visit(program);
        sb.append("}\n");
        return sb.toString();
    }

    private String newId() {
        return "n" + (counter++);
    }

    private String esc(String s) {
        return s.replace("\\", "\\\\").replace("\"", "\\\"").replace("\n", " : ");
    }

    private String node(String label) {
        String id = newId();
        sb.append("  ").append(id).append(" [label=\"").append(esc(label)).append("\"];\n");
        return id;
    }

    private void edge(String from, String to) {
        sb.append("  ").append(from).append(" -> ").append(to).append(";\n");
    }

    private String visit(ASTNode node) {
        if (node == null) return node("null");

        if (node instanceof ProgramNode) {
            ProgramNode p = (ProgramNode) node;
            String id = node("Program");
            for (ASTNode stmt : p.statements) edge(id, visit(stmt));
            return id;

        } else if (node instanceof DeclarationNode) {
            DeclarationNode d = (DeclarationNode) node;
            String id = node("Declaration : " + d.typeName + " " + d.varName);
            if (d.initializer != null) edge(id, visit(d.initializer));
            return id;

        } else if (node instanceof AssignmentNode) {
            AssignmentNode a = (AssignmentNode) node;
            String id = node("Assignment : " + a.varName + " =");
            edge(id, visit(a.value));
            return id;

        } else if (node instanceof PrintNode) {
            PrintNode pr = (PrintNode) node;
            String id = node("Print");
            edge(id, visit(pr.expression));
            return id;

        } else if (node instanceof IfNode) {
            IfNode i = (IfNode) node;
            String id = node("If" + (i.elseBranch != null ? "-Else" : ""));
            String condId = node("condition");
            edge(id, condId);
            edge(condId, visit(i.condition));
            String thenId = node("then");
            edge(id, thenId);
            edge(thenId, visit(i.thenBranch));
            if (i.elseBranch != null) {
                String elseId = node("else");
                edge(id, elseId);
                edge(elseId, visit(i.elseBranch));
            }
            return id;

        } else if (node instanceof BlockNode) {
            BlockNode b = (BlockNode) node;
            String id = node("Block");
            for (ASTNode stmt : b.statements) edge(id, visit(stmt));
            return id;

        } else if (node instanceof BinaryNode) {
            BinaryNode bin = (BinaryNode) node;
            String id = node("Binary : " + bin.operator);
            edge(id, visit(bin.left));
            edge(id, visit(bin.right));
            return id;

        } else if (node instanceof UnaryNode) {
            UnaryNode u = (UnaryNode) node;
            String id = node("Unary : " + u.operator);
            edge(id, visit(u.operand));
            return id;

        } else if (node instanceof NumberNode) {
            return node("Number : " + utils.BanglaUtil.toBanglaNum(((NumberNode) node).value));

        } else if (node instanceof StringNode) {
            return node("String : \"" + ((StringNode) node).value + "\"");

        } else if (node instanceof BooleanNode) {
            return node("Boolean : " + ((BooleanNode) node).value);

        } else if (node instanceof VariableNode) {
            return node("Variable : " + ((VariableNode) node).name);
        }

        return node(node.describe());
    }
}