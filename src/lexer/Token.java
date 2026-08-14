package lexer;

import utils.BanglaUtil;

public class Token {
    public final TokenType type;
    public final String lexeme;
    public final int line;
    public final int column;

    public Token(TokenType type, String lexeme, int line, int column) {
        this.type = type;
        this.lexeme = lexeme;
        this.line = line;
        this.column = column;
    }

    @Override
    public String toString() {
        return String.format("Token(%s, '%s', line=%s, col=%s)", 
            type, lexeme, BanglaUtil.toBanglaNum(line), BanglaUtil.toBanglaNum(column));
    }
}
