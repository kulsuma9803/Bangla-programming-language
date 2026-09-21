# কথন (Kothon) — Formal Grammar (BNF)

## Scope: Declaration, Assignment, Print, If-Else, While Loop, Expressions (Review 1 + Review 2)

```bnf
<program>        ::= <statement>*

<statement>       ::= <declaration>
                     | <assignment>
                     | <print_stmt>
                     | <if_stmt>
                     | <while_stmt>
                     | <block>

<declaration>     ::= <type> <identifier> "=" <expression> ";"
                     | <type> <identifier> ";"

<type>            ::= "সংখ্যা" | "বাক্য"

<assignment>      ::= <identifier> "=" <expression> ";"

<print_stmt>      ::= "দেখাও" "(" <expression> ")" ";"

<if_stmt>         ::= "যদি" "(" <condition> ")" <block>
                     | "যদি" "(" <condition> ")" <block> "নাহলে" <block>

<while_stmt>      ::= "যতক্ষণ" "(" <condition> ")" <block>

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
| যতক্ষণ  | while          |
| সত্য    | true           |
| মিথ্যা  | false          |
| এবং     | and            |
| অথবা    | or             |
| না      | not            |

## Comments
Single-line comments start with `//` and run to end of line.

## Review 2 Additions

### While Loop
`যতক্ষণ (condition) { ... }` repeats its block for as long as `condition`
evaluates to true. Semantically checked exactly like `যদি`'s condition
(must resolve to boolean — comparisons, `সত্য`/`মিথ্যা`, or `এবং`/`অথবা`/`না`
combinations).

### Target Code Generation
Two new compiler back-end stages sit after semantic analysis:

1. **Three-Address Code (TAC)** — an intermediate representation where
   every compound expression is broken into `t1 = a + b`-style
   instructions, and every `যদি`/`যতক্ষণ` is lowered into labeled jumps
   (`goto L1`, `ifFalse t1 goto L2`, `L1:`). This is generated purely
   for inspection (menu option 8) — it mirrors the classic textbook IR
   used in compiler courses.
2. **Python target code** — valid, executable Python 3 source,
   generated directly from the (type-checked) AST. Bangla identifiers
   are emitted unchanged (Python 3 allows Unicode identifiers per
   PEP 3131), সংখ্যা → `int`, বাক্য → `str`, `যদি`/`নাহলে` → `if`/`else`,
   `যতক্ষণ` → `while`. Integer→String coercion (see SemanticAnalyzer) is
   made explicit with `str(...)` calls, since Python's `+` doesn't
   auto-coerce the way কথন's does.
