package lexer;

public enum TokenType {
    // Literals
    INT_LITERAL,
    STRING_LITERAL,
    IDENTIFIER,

    // Keywords
    DHORI,      // ধরি   - declare
    SONGKHA,    // সংখ্যা - Integer type
    BAKKO,      // বাক্য  - String type
    DEKHAO,     // দেখাও  - print
    JODI,       // যদি   - if
    NAHOLE,     // নাহলে - else
    SOTTO,      // সত্য  - true
    MITTHA,     // মিথ্যা - false
    EBONG,      // এবং   - and
    OTHOBA,     // অথবা  - or
    NA,         // না    - not

    // Operators
    PLUS, MINUS, STAR, SLASH, PERCENT,
    ASSIGN,          // =
    EQ, NEQ,         // == !=
    LT, GT, LE, GE,  // <  >  <=  >=

    // Delimiters
    LPAREN, RPAREN,  // ( )
    LBRACE, RBRACE,  // { }
    SEMICOLON,       // ;

    // Special
    EOF,
    ERROR
}