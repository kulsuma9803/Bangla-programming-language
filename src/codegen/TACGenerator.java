package codegen;

import ast.*;
import utils.BanglaUtil;

/**
 * Generates textbook Three-Address Code (TAC) from a কথন AST, using
 * the same object-oriented instruction design taught in Class 4:
 * each *kind* of instruction (TACBinOp, TACCopy, TACPrint, ...) is
 * its own small class instead of a pre-formatted string, and
 * genExpr() RETURNS the name of wherever a value ended up, exactly
 * like the Class 4 gen_expr()/gen_stmt() pattern.
 *
 * Every compound expression is broken down into a sequence of simple
 * "result = operand1 op operand2" instructions using temporary
 * variables (t1, t2, ...), and every control-flow construct (if-else,
 * while) is lowered into conditional/unconditional jumps against
 * generated labels (L1, L2, ...) — exactly the flat, linear
 * intermediate representation described in classic compiler
 * construction (e.g. the Dragon Book). Control flow wasn't part of
 * the Class 4 toy language, so TACLabel/TACGoto/TACIfFalseGoto extend
 * the same one-class-per-kind pattern to cover it.
 *
 * This IR is intentionally only ever *displayed* to the user (menu
 * option for "Generate Three Address Code") — the actual Python
 * target code is generated directly from the AST by
 * PythonCodeGenerator, not reconstructed from this flat, goto-based
 * form. Rebuilding structured control flow (if/while) out of labels
 * and jumps is a much harder "control-flow reconstruction" problem
 * that real decompilers solve — it isn't needed here since the AST
 * (which already has that structure) is available directly.
 */
public class TACGenerator {

    private final ThreeAddressCode code = new ThreeAddressCode();
    private int tempCount = 0;
    private int labelCount = 0;

    private String newTemp() {
        return "t" + (++tempCount);
    }

    private String newLabel() {
        return "L" + (++labelCount);
    }

    public ThreeAddressCode generate(ProgramNode program) {
        for (ASTNode stmt : program.statements) {
            genStatement(stmt);
        }
        return code;
    }

    // ---------------- statements ----------------

    private void genStatement(ASTNode node) {
        if (node == null) return;

        if (node instanceof DeclarationNode) {
            DeclarationNode d = (DeclarationNode) node;
            if (d.initializer != null) {
                String val = genExpr(d.initializer);
                code.emit(new TACCopy(d.varName, val));
            }

        } else if (node instanceof AssignmentNode) {
            AssignmentNode a = (AssignmentNode) node;
            String val = genExpr(a.value);
            code.emit(new TACCopy(a.varName, val));

        } else if (node instanceof PrintNode) {
            PrintNode p = (PrintNode) node;
            String val = genExpr(p.expression);
            code.emit(new TACPrint(val));

        } else if (node instanceof IfNode) {
            IfNode i = (IfNode) node;
            String cond = genExpr(i.condition);
            String elseLabel = newLabel();
            String endLabel = newLabel();
            code.emit(new TACIfFalseGoto(cond, elseLabel));
            genBlock(i.thenBranch);
            code.emit(new TACGoto(endLabel));
            code.emit(new TACLabel(elseLabel));
            if (i.elseBranch != null) genBlock(i.elseBranch);
            code.emit(new TACLabel(endLabel));

        } else if (node instanceof WhileNode) {
            WhileNode w = (WhileNode) node;
            String startLabel = newLabel();
            String endLabel = newLabel();
            code.emit(new TACLabel(startLabel));
            String cond = genExpr(w.condition);
            code.emit(new TACIfFalseGoto(cond, endLabel));
            genBlock(w.body);
            code.emit(new TACGoto(startLabel));
            code.emit(new TACLabel(endLabel));

        } else if (node instanceof BlockNode) {
            genBlock((BlockNode) node);
        }
    }

    private void genBlock(BlockNode block) {
        for (ASTNode stmt : block.statements) {
            genStatement(stmt);
        }
    }

    // ---------------- expressions ----------------

    private String genExpr(ASTNode node) {
        if (node instanceof NumberNode) {
            return BanglaUtil.toBanglaNum(((NumberNode) node).value);
        }
        if (node instanceof StringNode) {
            return "\"" + ((StringNode) node).value + "\"";
        }
        if (node instanceof BooleanNode) {
            return ((BooleanNode) node).value ? "true" : "false";
        }
        if (node instanceof VariableNode) {
            return ((VariableNode) node).name;
        }
        if (node instanceof UnaryNode) {
            UnaryNode u = (UnaryNode) node;
            String operand = genExpr(u.operand);
            String t = newTemp();
            code.emit(new TACUnary(t, u.operator, operand));
            return t;
        }
        if (node instanceof BinaryNode) {
            BinaryNode b = (BinaryNode) node;
            String left = genExpr(b.left);
            String right = genExpr(b.right);
            String t = newTemp();
            code.emit(new TACBinOp(t, left, b.operator, right));
            return t;
        }
        return "?";
    }
}
