package semantic;

import ast.*;
import utils.ErrorReporter;
public class SemanticAnalyzer {

    public static final String TYPE_INT = "সংখ্যা";
    public static final String TYPE_STRING = "বাক্য";
    public static final String TYPE_BOOL = "boolean";   // internal only, not user-declarable
    public static final String TYPE_ERROR = "error";     // sentinel after a reported error

    private final SymbolTable symbols = new SymbolTable();
    private final ErrorReporter errors;

    public SemanticAnalyzer(ErrorReporter errors) {
        this.errors = errors;
    }

    public void analyze(ProgramNode program) {
        for (ASTNode stmt : program.statements) {
            checkStatement(stmt);
        }
    }

    /** Global-scope symbols, exposed so Main can print a symbol-table summary after analysis. */
    public java.util.Collection<Symbol> getGlobalSymbols() {
        return symbols.getGlobalScopeSymbols();
    }

    // ---------------- statements ----------------

    private void checkStatement(ASTNode node) {
        if (node == null) return;

        if (node instanceof DeclarationNode) {
            checkDeclaration((DeclarationNode) node);
        } else if (node instanceof AssignmentNode) {
            checkAssignment((AssignmentNode) node);
        } else if (node instanceof PrintNode) {
            inferType(((PrintNode) node).expression); // any type is printable
        } else if (node instanceof IfNode) {
            checkIf((IfNode) node);
        } else if (node instanceof BlockNode) {
            checkBlock((BlockNode) node);
        }
    }

    private void checkDeclaration(DeclarationNode decl) {
        boolean isNew = symbols.declare(decl.varName, decl.typeName, decl.initializer != null);
        if (!isNew) {
            errors.report("Semantic",
                    "Duplicate variable declaration: '" + decl.varName + "' is already declared in this scope",
                    decl.line, decl.column);
        }
        if (decl.initializer != null) {
            String exprType = inferType(decl.initializer);
            if (!exprType.equals(TYPE_ERROR) && !exprType.equals(decl.typeName) && !isCoercible(exprType, decl.typeName)) {
                errors.report("Semantic",
                        "Type mismatch: cannot assign value of type '" + exprType +
                                "' to variable '" + decl.varName + "' of type '" + decl.typeName + "'",
                        decl.line, decl.column);
            }
        }
    }

    private void checkAssignment(AssignmentNode assign) {
        Symbol sym = symbols.resolve(assign.varName);
        if (sym == null) {
            errors.report("Semantic",
                    "Undefined variable: '" + assign.varName + "' was never declared with 'ধরি'",
                    assign.line, assign.column);
            inferType(assign.value); // still walk RHS to catch further errors
            return;
        }
        String exprType = inferType(assign.value);
        if (!exprType.equals(TYPE_ERROR) && !exprType.equals(sym.type) && !isCoercible(exprType, sym.type)) {
            errors.report("Semantic",
                    "Type mismatch: cannot assign '" + exprType + "' to variable '" +
                            assign.varName + "' of type '" + sym.type + "'",
                    assign.line, assign.column);
        }
        sym.initialized = true;
    }

    private void checkIf(IfNode ifNode) {
        String condType = inferType(ifNode.condition);
        if (!condType.equals(TYPE_ERROR) && !condType.equals(TYPE_BOOL)) {
            errors.report("Semantic",
                    "Condition of 'যদি' must be a boolean expression (comparison, সত্য/মিথ্যা, বা এবং/অথবা/না), got '" + condType + "'",
                    ifNode.line, ifNode.column);
        }
        checkBlock(ifNode.thenBranch);
        if (ifNode.elseBranch != null) checkBlock(ifNode.elseBranch);
    }

    private void checkBlock(BlockNode block) {
        symbols.pushScope();
        for (ASTNode stmt : block.statements) {
            checkStatement(stmt);
        }
        symbols.popScope();
    }

    // ---------------- expressions: type inference ----------------

    private String inferType(ASTNode node) {
        if (node == null) return TYPE_ERROR;

        if (node instanceof NumberNode) {
            return TYPE_INT;
        }
        if (node instanceof StringNode) {
            return TYPE_STRING;
        }
        if (node instanceof BooleanNode) {
            return TYPE_BOOL;
        }
        if (node instanceof VariableNode) {
            VariableNode v = (VariableNode) node;
            Symbol sym = symbols.resolve(v.name);
            if (sym == null) {
                errors.report("Semantic",
                        "Undefined variable: '" + v.name + "' was never declared with 'ধরি'",
                        v.line, v.column);
                return TYPE_ERROR;
            }
            if (!sym.initialized) {
                errors.report("Semantic",
                        "Variable '" + v.name + "' is used before being initialized",
                        v.line, v.column);
            }
            return sym.type;
        }
        if (node instanceof UnaryNode) {
            UnaryNode u = (UnaryNode) node;
            String operandType = inferType(u.operand);
            if (u.operator.equals("-")) {
                if (!operandType.equals(TYPE_ERROR) && !operandType.equals(TYPE_INT)) {
                    errors.report("Semantic", "Unary '-' requires সংখ্যা (Integer), got '" + operandType + "'", u.line, u.column);
                    return TYPE_ERROR;
                }
                return TYPE_INT;
            }
            if (u.operator.equals("না")) {
                if (!operandType.equals(TYPE_ERROR) && !operandType.equals(TYPE_BOOL)) {
                    errors.report("Semantic", "'না' requires a boolean expression, got '" + operandType + "'", u.line, u.column);
                    return TYPE_ERROR;
                }
                return TYPE_BOOL;
            }
            return TYPE_ERROR;
        }
        if (node instanceof BinaryNode) {
            return inferBinaryType((BinaryNode) node);
        }

        return TYPE_ERROR;
    }

    private String inferBinaryType(BinaryNode bin) {
        String op = bin.operator;
        String leftType = inferType(bin.left);
        String rightType = inferType(bin.right);

        if (op.equals("এবং") || op.equals("অথবা")) {
            if (!leftType.equals(TYPE_ERROR) && !leftType.equals(TYPE_BOOL)) {
                errors.report("Semantic", "Left side of '" + op + "' must be boolean, got '" + leftType + "'", bin.line, bin.column);
            }
            if (!rightType.equals(TYPE_ERROR) && !rightType.equals(TYPE_BOOL)) {
                errors.report("Semantic", "Right side of '" + op + "' must be boolean, got '" + rightType + "'", bin.line, bin.column);
            }
            return TYPE_BOOL;
        }

        if (isRelOp(op)) {
            if (!leftType.equals(TYPE_ERROR) && !rightType.equals(TYPE_ERROR) && !leftType.equals(rightType)) {
                errors.report("Semantic",
                        "Cannot compare '" + leftType + "' with '" + rightType + "' using '" + op + "'",
                        bin.line, bin.column);
            }
            if ((op.equals("<") || op.equals(">") || op.equals("<=") || op.equals(">=")) &&
                    !leftType.equals(TYPE_ERROR) && leftType.equals(TYPE_STRING)) {
                errors.report("Semantic", "Operator '" + op + "' is not defined for বাক্য (String)", bin.line, bin.column);
            }
            return TYPE_BOOL;
        }

        // Arithmetic operators: + - * / %
        if (op.equals("+")) {
            if (leftType.equals(TYPE_ERROR) || rightType.equals(TYPE_ERROR)) return TYPE_ERROR;
            if (leftType.equals(TYPE_INT) && rightType.equals(TYPE_INT)) return TYPE_INT;
            boolean leftOk = leftType.equals(TYPE_STRING) || leftType.equals(TYPE_INT);
            boolean rightOk = rightType.equals(TYPE_STRING) || rightType.equals(TYPE_INT);
            boolean atLeastOneString = leftType.equals(TYPE_STRING) || rightType.equals(TYPE_STRING);
            if (leftOk && rightOk && atLeastOneString) {
                return TYPE_STRING; // coerced concatenation
            }
            errors.report("Semantic",
                    "Operator '+' requires সংখ্যা and/or বাক্য operands, got '" +
                            leftType + "' and '" + rightType + "'", bin.line, bin.column);
            return TYPE_ERROR;
        }
        if (op.equals("-") || op.equals("*") || op.equals("/") || op.equals("%")) {
            if (leftType.equals(TYPE_ERROR) || rightType.equals(TYPE_ERROR)) return TYPE_ERROR;
            if (!leftType.equals(TYPE_INT) || !rightType.equals(TYPE_INT)) {
                errors.report("Semantic",
                        "Operator '" + op + "' requires সংখ্যা (Integer) operands, got '" +
                                leftType + "' and '" + rightType + "'", bin.line, bin.column);
                return TYPE_ERROR;
            }
            if ((op.equals("/") || op.equals("%")) && isLiteralZero(bin.right)) {
                errors.report("Semantic",
                        "Division by zero: the right-hand side of '" + op + "' is the literal 0",
                        bin.line, bin.column);
                return TYPE_ERROR;
            }
            return TYPE_INT;
        }

        return TYPE_ERROR;
    }

    private boolean isRelOp(String op) {
        return op.equals("==") || op.equals("!=") || op.equals("<") ||
               op.equals(">") || op.equals("<=") || op.equals(">=");
    }
    private boolean isCoercible(String fromType, String toType) {
        return fromType.equals(TYPE_INT) && toType.equals(TYPE_STRING);
    }

    private boolean isLiteralZero(ASTNode node) {
        return node instanceof NumberNode && ((NumberNode) node).value == 0;
    }
}
