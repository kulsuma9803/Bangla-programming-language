# কথন (Kothon) Compiler — Architecture Overview

## 1. Pipeline (Data Flow)

```
  .bpl source file
        │
        ▼
  ┌───────────┐     List<Token>     ┌───────────┐     ProgramNode      ┌───────────────────┐
  │   Lexer   │ ──────────────────► │  Parser   │ ───────────────────► │ SemanticAnalyzer   │
  └───────────┘                     └───────────┘                     └────────────────────┘
        │                                 │                                     │
        ▼                                 ▼                                     ▼
   Token, TokenType                  ast.* (AST nodes)                SymbolTable, Symbol
        │                                 │                                     │
        └─────────────────┬───────────────┴──────────────────┬──────────────────┘
                           ▼                                  ▼
                     ErrorReporter (shared)         (Console output: tokens / AST / symbol table)
```

Every phase writes into the **same** `ErrorReporter` instance, so a
single run of the "Full Pipeline" option collects lexical, syntax, and
semantic errors together and reports them as one combined result —
without any phase crashing the program.

## 2. Package Responsibilities

| Package     | Responsibility                                                        |
|-------------|-------------------------------------------------------------------------|
| `lexer`     | Source text → token stream. Bangla keyword recognition, Bangla digits, string/number literals, comments, unknown-character recovery. |
| `ast`       | Plain data classes — one class per grammar construct (Declaration, Assignment, If, Binary, etc). No logic, just structure. |
| `parser`    | Token stream → AST, via recursive descent. Encodes operator precedence directly in the grammar (`expression → term → factor`). Recovers from syntax errors by skipping to the next `;` or `}`. |
| `semantic`  | Walks the AST once, maintaining a **scoped** symbol table. Infers a type for every expression node and reports mismatches, undefined variables, duplicate declarations, and use-before-initialization. |
| `utils`     | Cross-cutting concerns: `ErrorReporter` (shared error sink across all phases) and `FileLoader` (UTF-8 file reading). |

## 3. Class Relationships (UML-style, textual)

```
                    ┌────────────┐
                    │  ASTNode   │  (abstract)
                    └─────┬──────┘
        ┌──────────┬──────┼──────┬───────────┬────────────┬─────────────┐
        ▼          ▼      ▼      ▼           ▼            ▼             ▼
  ProgramNode  Number  String  Variable  BinaryNode   UnaryNode    BooleanNode
                Node     Node    Node                                    
        │
        ▼
  ┌─────────────┬───────────────┬───────────┬──────────┬───────────┐
  ▼             ▼               ▼           ▼          ▼           ▼
Declaration  Assignment      PrintNode   IfNode     BlockNode
   Node          Node
```

```
  Parser ──uses──► Token, TokenType (from lexer)
  Parser ──builds──► ASTNode subclasses (from ast)
  Parser ──reports to──► ErrorReporter (from utils)

  SemanticAnalyzer ──walks──► ASTNode tree
  SemanticAnalyzer ──owns──► SymbolTable
  SymbolTable ──contains many──► Symbol
  SemanticAnalyzer ──reports to──► ErrorReporter
```

## 4. Symbol Table Design

`SymbolTable` holds a **stack of scopes** (`List<Map<String, Symbol>>`).
- `pushScope()` / `popScope()` are called on entry/exit of every `{ }`
  block (e.g. an if/else branch), so a variable declared inside a block
  is invisible outside it.
- `resolve(name)` searches from the innermost scope outward, so inner
  blocks can still see outer (e.g. global) variables — standard lexical
  scoping.

```
Scope stack example for:

ধরি সংখ্যা ক = 1;
যদি (ক == 1) {
    ধরি সংখ্যা খ = 2;   <- খ only visible inside this block
}
// খ is NOT visible here

Scopes: [ {ক: Symbol} ]                    <- before if
        [ {ক: Symbol}, {খ: Symbol} ]        <- inside if-block
        [ {ক: Symbol} ]                    <- after block closes (popScope)
```

## 5. Error Recovery Strategy

| Phase     | Failure mode                  | Recovery                                             |
|-----------|--------------------------------|--------------------------------------------------------|
| Lexer     | Unrecognized character          | Report, skip the character, keep tokenizing             |
| Parser    | Unexpected token / missing token | Report, `synchronize()` to next `;` or `}`, resume    |
| Semantic  | Type mismatch / undefined var    | Report, keep analyzing rest of program (no early exit) |

This is why running any test file with intentional errors (see
`tests/*/`) never crashes the program — every error is caught, reported
through `ErrorReporter`, and compilation continues to completion.
