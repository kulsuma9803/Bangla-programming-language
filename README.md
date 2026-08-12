# কথন (Kothon) — A Bangla Programming Language & Compiler

**Course:** CSE-4114 Compiler Design and Construction Sessional
**Milestone:** Review 1 (Lexer → Parser → AST → Semantic Analysis, up to IF-ELSE)

## What is কথন?

কথন ("Kothon" — meaning *speech / narration*) is an originally invented toy
programming language whose keywords and syntax are written in Bangla. The
goal is to make programming concepts approachable for Bangla speakers
learning to code, and to demonstrate every stage of a real compiler
front-end.

### Example program
```
ধরি সংখ্যা বয়স = 18;
ধরি সংখ্যা সীমা = 15 + 3 * 2;
ধরি বাক্য বার্তা = "আপনি প্রাপ্তবয়স্ক";

দেখাও(বয়স);

যদি (বয়স >= সীমা) {
    দেখাও(বার্তা);
} নাহলে {
    দেখাও("আপনি এখনো প্রাপ্তবয়স্ক নন");
}
```

Full grammar: [`docs/GRAMMAR.md`](docs/GRAMMAR.md) (BNF).

## What Makes This Implementation Stand Out

Beyond the minimum Review-1 requirements, this compiler also includes:

- **✅ Automated Test Suite (menu option 7)** — discovers every `.bpl`
  file under `tests/`, runs it through the full pipeline, and checks
  whether it passed/failed as its filename promises (e.g. `type_mismatch.bpl`
  is expected to error; `valid_semantic.bpl` is expected not to). Prints
  a PASS/FAIL table plus a summary line (`Total = 15 | Passed = 15 | Failed = 0`).
  This is effectively a lightweight regression suite — run it any time
  after a code change to instantly confirm nothing broke.
- **🌳 AST → Graphviz visualization (menu option 5)** — exports any parsed
  program as a `.dot` graph (`output/ast.dot`). Render it with
  `dot -Tpng output/ast.dot -o output/ast.png` (or paste it into
  https://dreampuf.github.io/GraphvizOnline/) to get a full visual tree
  diagram of the AST — great for the presentation and Final Report's
  Compiler Design section.
- **💬 Interactive REPL (menu option 6)** — type কথন code line-by-line and
  run it through the full pipeline on demand (`চালাও`), without
  editing a file. Perfect for answering "what if I write X?" questions
  live during a review.
- **🎓 Real-world sample program** (`tests/review1_demo/grade_calculator.bpl`)
  — a student grading system with multi-level nested if-else, built
  specifically to support the Pitch requirement ("real-world relevance").
- **Global Symbol Table visualization** — running the Semantic Analyzer
  or Full Pipeline prints an aligned table of every declared variable,
  its type, and whether it's initialized (not required by the spec, but
  makes the symbol-table concept visible during a live demo).
- **Use-before-initialization detection** — using a declared-but-not-yet-assigned
  variable is caught as a semantic error, not just undefined-variable checks.
- **`+` operator overloading** — works for both Integer addition and
  String concatenation, with a clear error if types don't match.
- **Full logical operator support** — `এবং` (and), `অথবা` (or), `না` (not)
  combine with relational operators (`==`, `<`, `>=`, etc.) for compound
  conditions, not just single comparisons.
- **Arbitrary nesting** — if-else blocks can nest to any depth, each with
  its own lexical scope (variables declared inside don't leak out).
- **Bangla digit support** — numeric literals can be written with either
  ASCII digits (`0-9`) or Bangla digits (`০-৯`).
- **Bordered, phase-labeled console output** — every run (Lexer / Parser
  / Semantic / Full Pipeline) prints a clean, titled report, whether it
  passes or fails, instead of a raw dump — designed to look presentable
  during a live review.
- **15 test `.bpl` files** covering every valid and invalid case per
  phase (vs. the minimum of "some tests").
- **`docs/ARCHITECTURE.md`** — a textual UML/data-flow diagram of the
  whole compiler, ready to drop into the Final Report's "Compiler Design"
  section.

## Review-1 Feature Checklist

| Requirement (from course spec)                     | Status |
|------------------------------------------------------|:---:|
| Two data types with type checking (সংখ্যা, বাক্য)     | ✅ |
| Arithmetic ops with correct precedence (`* / %` before `+ -`) | ✅ |
| Assignment statements                                 | ✅ |
| IF-ELSE conditional statement                          | ✅ |
| Basic syntax error recovery (skip to `;` / `}`)        | ✅ |
| No runtime crashes on malformed input                  | ✅ |
| WHILE loop, target codegen                              | Planned for Review 2 |

## Project Structure

```
BanglaCompiler_Review1/
├── README.md
├── docs/
│   ├── GRAMMAR.md             # Full BNF grammar
│   └── ARCHITECTURE.md        # Data-flow + UML-style class diagrams
├── src/
│   ├── Main.java              # Console menu / pipeline driver
│   ├── lexer/                 # Token, TokenType, Lexer
│   ├── parser/                # Parser, ASTPrinter
│   ├── ast/                   # All AST node classes
│   ├── semantic/              # SemanticAnalyzer, Symbol, SymbolTable, SymbolTablePrinter
│   └── utils/                 # ErrorReporter, FileLoader
├── tests/
│   ├── lexer/                 # valid + invalid-character test cases
│   ├── parser/                # valid + missing-identifier/brace + nested-if-else
│   ├── semantic/              # valid + undefined/duplicate/type-mismatch/
│   │                          #   uninitialized-variable/complex-expression/string-concat
│   └── review1_demo/          # demo_program.bpl for live demo
└── output/                    # (reserved for future codegen output)
```

## How to Build & Run

Requires JDK 17+ (uses only core `java.*`, no external dependencies).

```bash
# From the project root:
find src -name "*.java" > sources.txt
javac -encoding UTF-8 -d out @sources.txt

# Run (use UTF-8 stdout so Bangla text prints correctly in the terminal):
java -Dstdout.encoding=UTF-8 -cp out Main
```

You'll get a menu:
```
===== Bangla Compiler =====
1. Run Lexer
2. Run Parser
3. Run Semantic Analyzer
4. Run Full Pipeline
5. Export AST as Graphviz (.dot)
6. REPL Mode (Interactive)
7. Run Automated Test Suite (all tests/ files)
8. Exit
Choose:
```
Enter a `.bpl` file path when prompted (or press Enter to use the bundled
demo at `tests/review1_demo/demo_program.bpl`), then pick a phase.

## Demo Flow
```
Input (.bpl) → Lexer → Tokens → Parser → AST → Semantic Analyzer → Symbol Table
```

## Team Contribution (suggested split)
| Member | Owns |
|---|---|
| Member 1 | `lexer/` |
| Member 2 | `parser/`, `ast/` |
| Member 3 | `semantic/` |
| Member 4 | `Main.java`, `utils/`, `tests/`, `README.md` |

Every member should still understand the full pipeline — review policy
requires any member to explain any part of the demoed feature.

## Design Notes
- **Error recovery**: the parser's `synchronize()` skips tokens to the next
  `;` or `}` after a syntax error, so one mistake doesn't halt compilation.
- **Scoped symbol table**: variables declared inside a `{ }` block (e.g.
  inside যদি/নাহলে) don't leak into the outer scope.
- **Type system**: `+` is overloaded for both Integer addition and String
  concatenation; `- * / %` are Integer-only; comparisons require matching
  types on both sides.
