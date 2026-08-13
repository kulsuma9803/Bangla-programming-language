package lexer;

import utils.ErrorReporter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
public class Lexer {

    private static final Map<String, TokenType> KEYWORDS = new HashMap<>();
    static {
        KEYWORDS.put("ধরি", TokenType.DHORI);
        KEYWORDS.put("সংখ্যা", TokenType.SONGKHA);
        KEYWORDS.put("বাক্য", TokenType.BAKKO);
        KEYWORDS.put("দেখাও", TokenType.DEKHAO);
        KEYWORDS.put("যদি", TokenType.JODI);
        KEYWORDS.put("নাহলে", TokenType.NAHOLE);
        KEYWORDS.put("সত্য", TokenType.SOTTO);
        KEYWORDS.put("মিথ্যা", TokenType.MITTHA);
        KEYWORDS.put("এবং", TokenType.EBONG);
        KEYWORDS.put("অথবা", TokenType.OTHOBA);
        KEYWORDS.put("না", TokenType.NA);
    }

    private final String source;
    private final ErrorReporter errors;
    private final List<Token> tokens = new ArrayList<>();

    private int pos = 0;
    private int line = 1;
    private int col = 1;

    public Lexer(String source, ErrorReporter errors) {
        this.source = source;
        this.errors = errors;
    }

    public List<Token> tokenize() {
        while (!isAtEnd()) {
            skipWhitespaceAndComments();
            if (isAtEnd()) break;

            int startLine = line, startCol = col;
            char c = peek();

            if (isDigit(c)) {
                tokens.add(number(startLine, startCol));
            } else if (isIdentifierStart(c)) {
                tokens.add(identifierOrKeyword(startLine, startCol));
            } else if (c == '"') {
                tokens.add(string(startLine, startCol));
            } else {
                Token t = operatorOrDelimiter(startLine, startCol);
                if (t != null) {
                    tokens.add(t);
                } else {
                    // Unknown character: already consumed inside operatorOrDelimiter;
                    // report and continue (error recovery, never crashes).
                    errors.report("Lexical", "Unrecognized character '" + c + "'", startLine, startCol);
                }
            }
        }
        tokens.add(new Token(TokenType.EOF, "", line, col));
        return tokens;
    }


    private boolean isAtEnd() {
        return pos >= source.length();
    }

    private char peek() {
        return isAtEnd() ? '\0' : source.charAt(pos);
    }

    private char peekNext() {
        return (pos + 1 >= source.length()) ? '\0' : source.charAt(pos + 1);
    }

    private char advance() {
        char c = source.charAt(pos++);
        if (c == '\n') {
            line++;
            col = 1;
        } else {
            col++;
        }
        return c;
    }

    private boolean match(char expected) {
        if (isAtEnd() || source.charAt(pos) != expected) return false;
        advance();
        return true;
    }

    private void skipWhitespaceAndComments() {
        while (!isAtEnd()) {
            char c = peek();
            if (c == ' ' || c == '\t' || c == '\r' || c == '\n') {
                advance();
            } else if (c == '/' && peekNext() == '/') {
                while (!isAtEnd() && peek() != '\n') advance();
            } else {
                break;
            }
        }
    }

    private boolean isDigit(char c) {
        return (c >= '0' && c <= '9') || (c >= '\u09E6' && c <= '\u09EF'); // Bangla digits ০-৯
    }

    private boolean isBanglaLetter(char c) {
        return c >= '\u0980' && c <= '\u09FF';
    }

    private boolean isIdentifierStart(char c) {
        return Character.isLetter(c) || c == '_' || isBanglaLetter(c);
    }

    private boolean isIdentifierPart(char c) {
        return isIdentifierStart(c) || isDigit(c);
    }

    private Token number(int startLine, int startCol) {
        StringBuilder sb = new StringBuilder();
        while (!isAtEnd() && isDigit(peek())) {
            sb.append(advance());
        }
        return new Token(TokenType.INT_LITERAL, sb.toString(), startLine, startCol);
    }

    private Token identifierOrKeyword(int startLine, int startCol) {
        StringBuilder sb = new StringBuilder();
        while (!isAtEnd() && isIdentifierPart(peek())) {
            sb.append(advance());
        }
        String text = sb.toString();
        TokenType type = KEYWORDS.getOrDefault(text, TokenType.IDENTIFIER);
        return new Token(type, text, startLine, startCol);
    }

    private Token string(int startLine, int startCol) {
        advance(); // consume opening quote
        StringBuilder sb = new StringBuilder();
        while (!isAtEnd() && peek() != '"') {
            if (peek() == '\n') {
                // unterminated string on this line
                errors.report("Lexical", "Unterminated string literal", startLine, startCol);
                return new Token(TokenType.STRING_LITERAL, sb.toString(), startLine, startCol);
            }
            sb.append(advance());
        }
        if (isAtEnd()) {
            errors.report("Lexical", "Unterminated string literal (reached end of file)", startLine, startCol);
        } else {
            advance(); // consume closing quote
        }
        return new Token(TokenType.STRING_LITERAL, sb.toString(), startLine, startCol);
    }

    private Token operatorOrDelimiter(int startLine, int startCol) {
        char c = advance();
        switch (c) {
            case '+': return new Token(TokenType.PLUS, "+", startLine, startCol);
            case '-': return new Token(TokenType.MINUS, "-", startLine, startCol);
            case '*': return new Token(TokenType.STAR, "*", startLine, startCol);
            case '/': return new Token(TokenType.SLASH, "/", startLine, startCol);
            case '%': return new Token(TokenType.PERCENT, "%", startLine, startCol);
            case '(': return new Token(TokenType.LPAREN, "(", startLine, startCol);
            case ')': return new Token(TokenType.RPAREN, ")", startLine, startCol);
            case '{': return new Token(TokenType.LBRACE, "{", startLine, startCol);
            case '}': return new Token(TokenType.RBRACE, "}", startLine, startCol);
            case ';': return new Token(TokenType.SEMICOLON, ";", startLine, startCol);
            case '=':
                if (match('=')) return new Token(TokenType.EQ, "==", startLine, startCol);
                return new Token(TokenType.ASSIGN, "=", startLine, startCol);
            case '!':
                if (match('=')) return new Token(TokenType.NEQ, "!=", startLine, startCol);
                return null;
            case '<':
                if (match('=')) return new Token(TokenType.LE, "<=", startLine, startCol);
                return new Token(TokenType.LT, "<", startLine, startCol);
            case '>':
                if (match('=')) return new Token(TokenType.GE, ">=", startLine, startCol);
                return new Token(TokenType.GT, ">", startLine, startCol);
            default:
                return null; // unknown character: already consumed by advance() above
        }
    }
}
