package parser;

import ast.*;


public class ASTPrinter {

    public void print(ASTNode node) {
        print(node, 0);
    }

    private void print(ASTNode node, int depth) {
        if (node == null) return;
        String indent = "  ".repeat(depth);

        if (node instanceof ProgramNode) {
            ProgramNode p = (ProgramNode) node;
            System.out.println(indent + "Program");
            for (ASTNode stmt : p.statements) print(stmt, depth + 1);

        } else if (node instanceof DeclarationNode) {
            DeclarationNode d = (DeclarationNode) node;
            System.out.println(indent + "Declaration: " + d.typeName + " " + d.varName);
            if (d.initializer != null) print(d.initializer, depth + 1);

        } else if (node instanceof AssignmentNode) {
            AssignmentNode a = (AssignmentNode) node;
            System.out.println(indent + "Assignment: " + a.varName + " =");
            print(a.value, depth + 1);

        } else if (node instanceof PrintNode) {
            PrintNode pr = (PrintNode) node;
            System.out.println(indent + "Print");
            print(pr.expression, depth + 1);

        } else if (node instanceof IfNode) {
            IfNode i = (IfNode) node;
            System.out.println(indent + "If");
            System.out.println(indent + "  Condition:");
            print(i.condition, depth + 2);
            System.out.println(indent + "  Then:");
            print(i.thenBranch, depth + 2);
            if (i.elseBranch != null) {
                System.out.println(indent + "  Else:");
                print(i.elseBranch, depth + 2);
            }

        } else if (node instanceof BlockNode) {
            BlockNode b = (BlockNode) node;
            System.out.println(indent + "Block");
            for (ASTNode stmt : b.statements) print(stmt, depth + 1);

        } else if (node instanceof BinaryNode) {
            BinaryNode bin = (BinaryNode) node;
            System.out.println(indent + "Binary(" + bin.operator + ")");
            print(bin.left, depth + 1);
            print(bin.right, depth + 1);

        } else if (node instanceof UnaryNode) {
            UnaryNode u = (UnaryNode) node;
            System.out.println(indent + "Unary(" + u.operator + ")");
            print(u.operand, depth + 1);

        } else if (node instanceof NumberNode) {
            System.out.println(indent + "Number: " + utils.BanglaUtil.toBanglaNum(((NumberNode) node).value));

        } else if (node instanceof StringNode) {
            System.out.println(indent + "String: \"" + ((StringNode) node).value + "\"");

        } else if (node instanceof BooleanNode) {
            System.out.println(indent + "Boolean: " + ((BooleanNode) node).value);

        } else if (node instanceof VariableNode) {
            System.out.println(indent + "Variable: " + ((VariableNode) node).name);

        } else {
            System.out.println(indent + node.describe());
        }
    }
}
