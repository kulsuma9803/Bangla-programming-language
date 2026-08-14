package parser;

import ast.*;
import lexer.Token;
import lexer.TokenType;
import utils.ErrorReporter;

import java.util.ArrayList;
import java.util.List;

/**
 * Recursive-descent parser for কথন (Kothon).
 *
 * Grammar reference: docs/GRAMMAR.md
 *
 * Error recovery strategy: when a statement fails to parse, the parser
 * reports the error and skips tokens until the next ';' or '}' (or EOF),
 * then keeps parsing the rest of the program. This means one syntax
 * error never stops the whole compilation (satisfies "no runtime
 * crashes" and "basic syntax error recovery" requirements).
 */
public class Parser {

    private final List<Token> tokens;
    private final ErrorReporter errors;
    private int current = 0;

    public Parser(List<Token> tokens, ErrorReporter errors) {
        this.tokens = tokens;
        this.errors = errors;
    }

    public ProgramNode parseProgram() {
        List<ASTNode> statements = new ArrayList<>();
        while (!isAtEnd()) {
            ASTNode stmt = statement();
            if (stmt != null) statements.add(stmt);
        }
        return new ProgramNode(statements);
    }

    // ---------------- statements ----------------

    private ASTNode statement() {
        try {
            if (check(TokenType.DHORI)) return declaration();
            if (check(TokenType.DEKHAO)) return printStatement();
            if (check(TokenType.JODI)) return ifStatement();
            if (check(TokenType.LBRACE)) return block();
            if (check(TokenType.IDENTIFIER)) return assignment();

            // Unknown start of statement -> syntax error, recover.
            Token bad = peek();
            errors.report("Syntax", "Unexpected token '" + bad.lexeme + "' — expected a statement", bad.line, bad.column);
            synchronize();
            return null;
        } catch (ParseError pe) {
            synchronize();
            return null;
        }
    }

    // ধরি সংখ্যা x = 5; | ধরি বাক্য নাম;
    private ASTNode declaration() {
        Token dhoriTok = advance(); // consume ধরি
        Token typeTok;
        if (check(TokenType.SONGKHA) || check(TokenType.BAKKO)) {
            typeTok = advance();
        } else {
            throw error(peek(), "Expected type 'সংখ্যা' or 'বাক্য' after 'ধরি'");
        }
        Token nameTok = consume(TokenType.IDENTIFIER, "Expected variable name after type");

        ASTNode initializer = null;
        if (match(TokenType.ASSIGN)) {
            initializer = expression();
        }
        consume(TokenType.SEMICOLON, "Expected ';' after declaration");
        return new DeclarationNode(typeTok.lexeme, nameTok.lexeme, initializer, dhoriTok.line, dhoriTok.column);
    }

    // x = expression;
    private ASTNode assignment() {
        Token nameTok = advance(); // identifier
        consume(TokenType.ASSIGN, "Expected '=' in assignment");
        ASTNode value = expression();
        consume(TokenType.SEMICOLON, "Expected ';' after assignment");
        return new AssignmentNode(nameTok.lexeme, value, nameTok.line, nameTok.column);
    }

    // দেখাও(expression);
    private ASTNode printStatement() {
        Token dekhaoTok = advance();
        consume(TokenType.LPAREN, "Expected '(' after 'দেখাও'");
        ASTNode expr = expression();
        consume(TokenType.RPAREN, "Expected ')' after expression");
        consume(TokenType.SEMICOLON, "Expected ';' after print statement");
        return new PrintNode(expr, dekhaoTok.line, dekhaoTok.column);
    }

    // যদি (condition) { ... } [ নাহলে { ... } ]
    private ASTNode ifStatement() {
        Token jodiTok = advance();
        consume(TokenType.LPAREN, "Expected '(' after 'যদি'");
        ASTNode condition = condition();
        consume(TokenType.RPAREN, "Expected ')' after condition");
        BlockNode thenBranch = block();
        BlockNode elseBranch = null;
        if (match(TokenType.NAHOLE)) {
            elseBranch = block();
        }
        return new IfNode(condition, thenBranch, elseBranch, jodiTok.line, jodiTok.column);
    }

    private BlockNode block() {
        Token lbrace = consume(TokenType.LBRACE, "Expected '{' to start a block");
        List<ASTNode> statements = new ArrayList<>();
        while (!check(TokenType.RBRACE) && !isAtEnd()) {
            ASTNode stmt = statement();
            if (stmt != null) statements.add(stmt);
        }
        consume(TokenType.RBRACE, "Expected '}' to close block");
        return new BlockNode(statements, lbrace.line, lbrace.column);
    }

    // ---------------- conditions (logical) ----------------

    private ASTNode condition() {
        return orCondition();
    }

    private ASTNode orCondition() {
        ASTNode left = andCondition();
        while (check(TokenType.OTHOBA)) {
            Token op = advance();
            ASTNode right = andCondition();
            left = new BinaryNode(left, op.lexeme, right, op.line, op.column);
        }
        return left;
    }

    private ASTNode andCondition() {
        ASTNode left = notCondition();
        while (check(TokenType.EBONG)) {
            Token op = advance();
            ASTNode right = notCondition();
            left = new BinaryNode(left, op.lexeme, right, op.line, op.column);
        }
        return left;
    }

    private ASTNode notCondition() {
        if (check(TokenType.NA)) {
            Token op = advance();
            ASTNode operand = notCondition();
            return new UnaryNode(op.lexeme, operand, op.line, op.column);
        }
        return relCondition();
    }

    private ASTNode relCondition() {
        if (check(TokenType.SOTTO)) {
            Token t = advance();
            return new BooleanNode(true, t.line, t.column);
        }
        if (check(TokenType.MITTHA)) {
            Token t = advance();
            return new BooleanNode(false, t.line, t.column);
        }
        if (match(TokenType.LPAREN)) {
            ASTNode inner = condition();
            consume(TokenType.RPAREN, "Expected ')' after condition");
            return inner;
        }
        ASTNode left = expression();
        if (isRelOp(peek().type)) {
            Token op = advance();
            ASTNode right = expression();
            return new BinaryNode(left, op.lexeme, right, op.line, op.column);
        }
        // A bare expression used as condition is a semantic-time type error,
        // not a syntax error — let semantic analyzer catch/report it.
        return left;
    }

    private boolean isRelOp(TokenType t) {
        return t == TokenType.EQ || t == TokenType.NEQ || t == TokenType.LT ||
               t == TokenType.GT || t == TokenType.LE || t == TokenType.GE;
    }

    // ---------------- expressions (arithmetic, correct precedence) ----------------

    private ASTNode expression() {
        ASTNode left = term();
        while (check(TokenType.PLUS) || check(TokenType.MINUS)) {
            Token op = advance();
            ASTNode right = term();
            left = new BinaryNode(left, op.lexeme, right, op.line, op.column);
        }
        return left;
    }

    private ASTNode term() {
        ASTNode left = factor();
        while (check(TokenType.STAR) || check(TokenType.SLASH) || check(TokenType.PERCENT)) {
            Token op = advance();
            ASTNode right = factor();
            left = new BinaryNode(left, op.lexeme, right, op.line, op.column);
        }
        return left;
    }

    private ASTNode factor() {
        if (check(TokenType.MINUS)) {
            Token op = advance();
            ASTNode operand = factor();
            return new UnaryNode(op.lexeme, operand, op.line, op.column);
        }
        if (check(TokenType.INT_LITERAL)) {
            Token t = advance();
            return new NumberNode(Integer.parseInt(toAsciiDigits(t.lexeme)), t.line, t.column);
        }
        if (check(TokenType.STRING_LITERAL)) {
            Token t = advance();
            return new StringNode(t.lexeme, t.line, t.column);
        }
        if (check(TokenType.IDENTIFIER)) {
            Token t = advance();
            return new VariableNode(t.lexeme, t.line, t.column);
        }
        if (match(TokenType.LPAREN)) {
            ASTNode expr = expression();
            consume(TokenType.RPAREN, "Expected ')' after expression");
            return expr;
        }
        throw error(peek(), "Expected an expression");
    }

    // Convert Bangla digits (০-৯) to ASCII digits so Integer.parseInt works.
    private String toAsciiDigits(String s) {
        StringBuilder sb = new StringBuilder();
        for (char c : s.toCharArray()) {
            if (c >= '\u09E6' && c <= '\u09EF') {
                sb.append((char) ('0' + (c - '\u09E6')));
            } else {
                sb.append(c);
            }
        }
        return sb.toString();
    }

    // ---------------- token helpers ----------------

    private boolean check(TokenType type) {
        return !isAtEnd() && peek().type == type;
    }

    private boolean match(TokenType type) {
        if (check(type)) {
            advance();
            return true;
        }
        return false;
    }

    private Token consume(TokenType type, String message) {
        if (check(type)) return advance();
        throw error(peek(), message);
    }

    private Token advance() {
        if (!isAtEnd()) current++;
        return previous();
    }

    private Token peek() {
        return tokens.get(current);
    }

    private Token previous() {
        return tokens.get(current - 1);
    }

    private boolean isAtEnd() {
        return peek().type == TokenType.EOF;
    }

    private ParseError error(Token token, String message) {
        errors.report("Syntax", message + " (got '" + token.lexeme + "')", token.line, token.column);
        return new ParseError();
    }

    /**
     * Skips tokens until it finds a likely statement boundary
     * (';' or '}') so parsing can resume — this is the required
     * "skip to semicolon or end of line" error recovery.
     */
    private void synchronize() {
        while (!isAtEnd()) {
            TokenType t = previous().type;
            if (t == TokenType.SEMICOLON || t == TokenType.RBRACE) return;
            switch (peek().type) {
                case DHORI:
                case DEKHAO:
                case JODI:
                case RBRACE:
                    return;
                default:
                    advance();
            }
        }
    }

    private static class ParseError extends RuntimeException {}
}
