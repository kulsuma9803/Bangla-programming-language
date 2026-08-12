# কথন (Kothon) — Formal Grammar (BNF)

## Review-1 Scope: Declaration, Assignment, Print, If-Else, Expressions

```bnf
<program>        ::= <statement>*

<statement>       ::= <declaration>
                     | <assignment>
                     | <print_stmt>
                     | <if_stmt>
                     | <block>

<declaration>     ::= <type> <identifier> "=" <expression> ";"
                     | <type> <identifier> ";"

<type>            ::= "সংখ্যা" | "বাক্য"

<assignment>      ::= <identifier> "=" <expression> ";"

<print_stmt>      ::= "দেখাও" "(" <expression> ")" ";"

<if_stmt>         ::= "যদি" "(" <condition> ")" <block>
                     | "যদি" "(" <condition> ")" <block> "নাহলে" <block>

<block>           ::= "{" <statement>* "}"

<condition>       ::= <or_condition>

<or_condition>    ::= <and_condition> ( "অথবা" <and_condition> )*

<and_condition>   ::= <not_condition> ( "এবং" <not_condition> )*

<not_condition>   ::= "না" <not_condition>
                     | <rel_condition>

<rel_condition>   ::= <expression> <rel_op> <expression>
                     | "সত্য"
                     | "মিথ্যা"
                     | "(" <condition> ")"

<rel_op>          ::= "==" | "!=" | "<" | ">" | "<=" | ">="

<expression>      ::= <term> ( ( "+" | "-" ) <term> )*

<term>            ::= <factor> ( ( "*" | "/" | "%" ) <factor> )*

<factor>          ::= <number>
                     | <string>
                     | <identifier>
                     | "(" <expression> ")"
                     | "-" <factor>

<number>          ::= <digit>+
<string>          ::= '"' <char>* '"'
<identifier>      ::= <letter> ( <letter> | <digit> )*
<letter>          ::= বাংলা বর্ণমালা (Unicode) | 'a'..'z' | 'A'..'Z' | '_'
<digit>           ::= '0'..'9'
```

## Operator Precedence (highest to lowest)
1. Unary minus `-`
2. `*` `/` `%`
3. `+` `-`
4. Relational: `==` `!=` `<` `>` `<=` `>=`
5. `না` (NOT)
6. `এবং` (AND)
7. `অথবা` (OR)

## Reserved Keywords
| Keyword | Meaning        |
|---------|----------------|
| ধরি     | let/declare    |
| সংখ্যা  | Integer type   |
| বাক্য   | String type    |
| দেখাও   | print          |
| যদি     | if             |
| নাহলে   | else           |
| সত্য    | true           |
| মিথ্যা  | false          |
| এবং     | and            |
| অথবা    | or             |
| না      | not            |

## Comments
Single-line comments start with `//` and run to end of line.
