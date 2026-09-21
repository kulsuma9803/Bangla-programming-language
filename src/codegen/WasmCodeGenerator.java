package codegen;

import ast.*;
import semantic.SemanticAnalyzer;

import java.nio.charset.StandardCharsets;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Generates textbook WebAssembly Text format (.wat) from a কথন AST.
 * This is the OPTIONAL target-code feature from the project
 * requirements ("Generated target code in WebAssembly instead of
 * Java or Python").
 *
 * Design, in short:
 *   - সংখ্যা (Integer) and boolean values are plain i32 values.
 *   - বাক্য (String) values are i32 POINTERS into linear memory, to
 *     null-terminated UTF-8 byte sequences (C-string style) — either
 *     a fixed offset for a string LITERAL (baked in as a `data`
 *     segment) or a heap address for a value built at runtime
 *     (string concatenation, or an Integer coerced to a String).
 *   - A tiny bump allocator ($alloc / global $heap_ptr) and four
 *     helper functions ($strlen, $concat, $itoa, $streq) provide the
 *     runtime support real "+"/coercion/equality semantics need —
 *     WebAssembly itself has no string type or garbage collector.
 *   - if/while map directly onto WASM's own structured `if` and
 *     `block`+`loop`+`br_if` — no labels/gotos needed here, unlike
 *     the TAC form (WASM's control flow is structured by design).
 *   - কথন blocks (যদি/নাহলে/যতক্ষণ bodies) are lexically scoped,
 *     exactly like SemanticAnalyzer's SymbolTable — so this class
 *     keeps its own parallel scope stack mapping কথন variable names
 *     to *uniquely generated* WASM globals (one per declaration,
 *     never reused across sibling/nested blocks), which is what
 *     makes shadowing behave correctly.
 *
 * Every AST node already carries a resolvedType (set by
 * SemanticAnalyzer) — this class trusts it completely instead of
 * re-deriving types, exactly like PythonCodeGenerator does.
 */
public class WasmCodeGenerator {

    private static final String T_INT = SemanticAnalyzer.TYPE_INT;
    private static final String T_STR = SemanticAnalyzer.TYPE_STRING;
    private static final String T_BOOL = SemanticAnalyzer.TYPE_BOOL;

    /** কথন variable name -> its unique WASM global, per lexical scope (innermost first). */
    private final ArrayDeque<Map<String, String>> scopes = new ArrayDeque<>();
    private int varCounter = 0;
    private int labelCounter = 0;

    /** String literal (Java String) -> its fixed byte offset in linear memory. */
    private final Map<String, Integer> literalOffsets = new LinkedHashMap<>();
    private int nextLiteralOffset = SCRATCH_SIZE;

    private final List<String> globalDecls = new ArrayList<>();
    private final StringBuilder mainBody = new StringBuilder();

    private static final int SCRATCH_SIZE = 16; // reserved for $itoa's digit scratch buffer

    public String generate(ProgramNode program) {
        scopes.push(new LinkedHashMap<>());

        // Always reserve these two — used for printing দেখাও(boolean) results,
        // and "" as the safe default value of a declared-but-unset বাক্য variable.
        literalOffset("");
        int trueOffset = literalOffset("সত্য");
        int falseOffset = literalOffset("মিথ্যা");

        for (ASTNode stmt : program.statements) {
            genStmt(stmt, mainBody, trueOffset, falseOffset);
        }

        int heapStart = align4(nextLiteralOffset);
        return assembleModule(heapStart, trueOffset, falseOffset);
    }

    // ---------------- statements ----------------

    private void genStmt(ASTNode node, StringBuilder out, int trueOff, int falseOff) {
        if (node == null) return;

        if (node instanceof DeclarationNode) {
            DeclarationNode d = (DeclarationNode) node;
            String unique = declare(d.varName);
            int defaultVal = d.typeName.equals(T_STR) ? literalOffsets.get("") : 0;
            globalDecls.add("  (global " + unique + " (mut i32) (i32.const " + defaultVal + "))");
            if (d.initializer != null) {
                genExpr(d.initializer, out, trueOff, falseOff);
                if (d.typeName.equals(T_STR) && d.initializer.resolvedType.equals(T_INT)) {
                    out.append("    call $itoa\n");
                }
                out.append("    global.set ").append(unique).append("\n");
            }

        } else if (node instanceof AssignmentNode) {
            AssignmentNode a = (AssignmentNode) node;
            String unique = resolve(a.varName);
            genExpr(a.value, out, trueOff, falseOff);
            if (a.resolvedType.equals(T_STR) && a.value.resolvedType.equals(T_INT)) {
                out.append("    call $itoa\n");
            }
            out.append("    global.set ").append(unique).append("\n");

        } else if (node instanceof PrintNode) {
            ASTNode expr = ((PrintNode) node).expression;
            genExpr(expr, out, trueOff, falseOff);
            String t = expr.resolvedType;
            if (t.equals(T_INT)) {
                out.append("    call $print_i32\n");
            } else if (t.equals(T_BOOL)) {
                out.append("    if (result i32)\n");
                out.append("      i32.const ").append(trueOff).append("\n");
                out.append("    else\n");
                out.append("      i32.const ").append(falseOff).append("\n");
                out.append("    end\n");
                out.append("    call $print_str\n");
            } else {
                out.append("    call $print_str\n");
            }

        } else if (node instanceof IfNode) {
            IfNode i = (IfNode) node;
            genExpr(i.condition, out, trueOff, falseOff);
            out.append("    if\n");
            scopes.push(new LinkedHashMap<>());
            for (ASTNode s : i.thenBranch.statements) genStmt(s, out, trueOff, falseOff);
            scopes.pop();
            if (i.elseBranch != null) {
                out.append("    else\n");
                scopes.push(new LinkedHashMap<>());
                for (ASTNode s : i.elseBranch.statements) genStmt(s, out, trueOff, falseOff);
                scopes.pop();
            }
            out.append("    end\n");

        } else if (node instanceof WhileNode) {
            WhileNode w = (WhileNode) node;
            String breakLabel = "$Lbreak" + (labelCounter);
            String contLabel = "$Lcont" + (labelCounter);
            labelCounter++;
            out.append("    block ").append(breakLabel).append("\n");
            out.append("    loop ").append(contLabel).append("\n");
            genExpr(w.condition, out, trueOff, falseOff);
            out.append("    i32.eqz\n");
            out.append("    br_if ").append(breakLabel).append("\n");
            scopes.push(new LinkedHashMap<>());
            for (ASTNode s : w.body.statements) genStmt(s, out, trueOff, falseOff);
            scopes.pop();
            out.append("    br ").append(contLabel).append("\n");
            out.append("    end\n");
            out.append("    end\n");

        } else if (node instanceof BlockNode) {
            scopes.push(new LinkedHashMap<>());
            for (ASTNode s : ((BlockNode) node).statements) genStmt(s, out, trueOff, falseOff);
            scopes.pop();
        }
    }

    // ---------------- expressions ----------------

    private void genExpr(ASTNode node, StringBuilder out, int trueOff, int falseOff) {
        if (node instanceof NumberNode) {
            out.append("    i32.const ").append(((NumberNode) node).value).append("\n");

        } else if (node instanceof StringNode) {
            out.append("    i32.const ").append(literalOffset(((StringNode) node).value)).append("\n");

        } else if (node instanceof BooleanNode) {
            out.append("    i32.const ").append(((BooleanNode) node).value ? 1 : 0).append("\n");

        } else if (node instanceof VariableNode) {
            out.append("    global.get ").append(resolve(((VariableNode) node).name)).append("\n");

        } else if (node instanceof UnaryNode) {
            UnaryNode u = (UnaryNode) node;
            genExpr(u.operand, out, trueOff, falseOff);
            if (u.operator.equals("-")) {
                out.append("    i32.const -1\n    i32.mul\n");
            } else { // "না"
                out.append("    i32.eqz\n");
            }

        } else if (node instanceof BinaryNode) {
            genBinary((BinaryNode) node, out, trueOff, falseOff);
        }
    }

    private void genBinary(BinaryNode b, StringBuilder out, int trueOff, int falseOff) {
        String op = b.operator;
        String lt = b.left.resolvedType;
        String rt = b.right.resolvedType;

        if (op.equals("এবং") || op.equals("অথবা")) {
            genExpr(b.left, out, trueOff, falseOff);
            genExpr(b.right, out, trueOff, falseOff);
            out.append(op.equals("এবং") ? "    i32.and\n" : "    i32.or\n");
            return;
        }

        if (op.equals("==") || op.equals("!=")) {
            if (lt.equals(T_STR)) {
                genExpr(b.left, out, trueOff, falseOff);
                genExpr(b.right, out, trueOff, falseOff);
                out.append("    call $streq\n");
                if (op.equals("!=")) out.append("    i32.eqz\n");
            } else {
                genExpr(b.left, out, trueOff, falseOff);
                genExpr(b.right, out, trueOff, falseOff);
                out.append(op.equals("==") ? "    i32.eq\n" : "    i32.ne\n");
            }
            return;
        }
        if (op.equals("<") || op.equals(">") || op.equals("<=") || op.equals(">=")) {
            genExpr(b.left, out, trueOff, falseOff);
            genExpr(b.right, out, trueOff, falseOff);
            switch (op) {
                case "<":  out.append("    i32.lt_s\n"); break;
                case ">":  out.append("    i32.gt_s\n"); break;
                case "<=": out.append("    i32.le_s\n"); break;
                default:   out.append("    i32.ge_s\n"); break;
            }
            return;
        }

        if (op.equals("+")) {
            if (lt.equals(T_INT) && rt.equals(T_INT)) {
                genExpr(b.left, out, trueOff, falseOff);
                genExpr(b.right, out, trueOff, falseOff);
                out.append("    i32.add\n");
            } else {
                // String concatenation, with Integer -> String coercion via $itoa
                // on whichever side needs it — same polymorphic '+' PythonCodeGenerator implements.
                genExpr(b.left, out, trueOff, falseOff);
                if (lt.equals(T_INT)) out.append("    call $itoa\n");
                genExpr(b.right, out, trueOff, falseOff);
                if (rt.equals(T_INT)) out.append("    call $itoa\n");
                out.append("    call $concat\n");
            }
            return;
        }

        // - * / %
        genExpr(b.left, out, trueOff, falseOff);
        genExpr(b.right, out, trueOff, falseOff);
        switch (op) {
            case "-": out.append("    i32.sub\n"); break;
            case "*": out.append("    i32.mul\n"); break;
            case "/": out.append("    i32.div_s\n"); break;
            default:  out.append("    i32.rem_s\n"); break;
        }
    }

    // ---------------- scope helpers ----------------

    private String declare(String name) {
        String unique = "$v" + (varCounter++);
        scopes.peek().put(name, unique);
        return unique;
    }

    private String resolve(String name) {
        for (Map<String, String> scope : scopes) {
            String u = scope.get(name);
            if (u != null) return u;
        }
        // Should never happen — SemanticAnalyzer already rejected undefined variables.
        throw new IllegalStateException("WASM codegen: unresolved variable '" + name + "'");
    }

    // ---------------- string literal / memory layout helpers ----------------

    private int literalOffset(String value) {
        Integer existing = literalOffsets.get(value);
        if (existing != null) return existing;
        int offset = nextLiteralOffset;
        literalOffsets.put(value, offset);
        int byteLen = value.getBytes(StandardCharsets.UTF_8).length + 1; // +1 for null terminator
        nextLiteralOffset = offset + byteLen;
        return offset;
    }

    private static int align4(int n) {
        return (n + 3) & ~3;
    }

    /** Encodes a Java String's UTF-8 bytes as WAT `\xx` hex escapes — encoding-independent. */
    private static String encodeDataBytes(String value) {
        byte[] bytes = value.getBytes(StandardCharsets.UTF_8);
        StringBuilder sb = new StringBuilder();
        for (byte bt : bytes) {
            sb.append('\\').append(String.format("%02x", bt & 0xff));
        }
        sb.append("\\00"); // null terminator
        return sb.toString();
    }

    // ---------------- module assembly ----------------

    private String assembleModule(int heapStart, int trueOff, int falseOff) {
        StringBuilder m = new StringBuilder();
        m.append(";; Auto-generated by কথন (Kothon) WasmCodeGenerator — optional WebAssembly target.\n");
        m.append(";; Run via: node output/run_wasm.mjs  (see that file for the print_i32/print_str host imports)\n");
        m.append("(module\n");
        m.append("  (import \"env\" \"print_i32\" (func $print_i32 (param i32)))\n");
        m.append("  (import \"env\" \"print_str\" (func $print_str (param i32)))\n");
        m.append("  (memory (export \"memory\") 1)\n");
        m.append("  (global $heap_ptr (mut i32) (i32.const ").append(heapStart).append("))\n");

        m.append("\n  ;; ---- string literal data (offset -> bytes) ----\n");
        for (Map.Entry<String, Integer> e : literalOffsets.entrySet()) {
            m.append("  (data (i32.const ").append(e.getValue()).append(") \"")
             .append(encodeDataBytes(e.getKey())).append("\")\n");
        }

        m.append("\n  ;; ---- programma variables (one global per declaration, scope-unique) ----\n");
        for (String decl : globalDecls) m.append(decl).append("\n");

        m.append("\n  ;; ---- runtime helpers (strings have no native support in WASM) ----\n");
        m.append(HELPER_FUNCTIONS);

        m.append("\n  (func $main (export \"main\")\n");
        m.append(mainBody);
        m.append("  )\n");
        m.append(")\n");
        return m.toString();
    }

    private static final String HELPER_FUNCTIONS =
        "  (func $strlen (param $p i32) (result i32)\n" +
        "    (local $i i32)\n" +
        "    (local.set $i (i32.const 0))\n" +
        "    (block $done\n" +
        "      (loop $loop\n" +
        "        (br_if $done (i32.eqz (i32.load8_u (i32.add (local.get $p) (local.get $i)))))\n" +
        "        (local.set $i (i32.add (local.get $i) (i32.const 1)))\n" +
        "        (br $loop)\n" +
        "      )\n" +
        "    )\n" +
        "    (local.get $i)\n" +
        "  )\n" +
        "\n" +
        "  (func $alloc (param $size i32) (result i32)\n" +
        "    (local $ptr i32)\n" +
        "    (local.set $ptr (global.get $heap_ptr))\n" +
        "    (global.set $heap_ptr (i32.add (global.get $heap_ptr) (local.get $size)))\n" +
        "    (local.get $ptr)\n" +
        "  )\n" +
        "\n" +
        "  (func $concat (param $a i32) (param $b i32) (result i32)\n" +
        "    (local $la i32) (local $lb i32) (local $ptr i32) (local $i i32)\n" +
        "    (local.set $la (call $strlen (local.get $a)))\n" +
        "    (local.set $lb (call $strlen (local.get $b)))\n" +
        "    (local.set $ptr (call $alloc (i32.add (i32.add (local.get $la) (local.get $lb)) (i32.const 1))))\n" +
        "    (local.set $i (i32.const 0))\n" +
        "    (block $done_a\n" +
        "      (loop $loop_a\n" +
        "        (br_if $done_a (i32.ge_u (local.get $i) (local.get $la)))\n" +
        "        (i32.store8 (i32.add (local.get $ptr) (local.get $i))\n" +
        "                    (i32.load8_u (i32.add (local.get $a) (local.get $i))))\n" +
        "        (local.set $i (i32.add (local.get $i) (i32.const 1)))\n" +
        "        (br $loop_a)\n" +
        "      )\n" +
        "    )\n" +
        "    (local.set $i (i32.const 0))\n" +
        "    (block $done_b\n" +
        "      (loop $loop_b\n" +
        "        (br_if $done_b (i32.ge_u (local.get $i) (local.get $lb)))\n" +
        "        (i32.store8 (i32.add (i32.add (local.get $ptr) (local.get $la)) (local.get $i))\n" +
        "                    (i32.load8_u (i32.add (local.get $b) (local.get $i))))\n" +
        "        (local.set $i (i32.add (local.get $i) (i32.const 1)))\n" +
        "        (br $loop_b)\n" +
        "      )\n" +
        "    )\n" +
        "    (i32.store8 (i32.add (i32.add (local.get $ptr) (local.get $la)) (local.get $lb)) (i32.const 0))\n" +
        "    (local.get $ptr)\n" +
        "  )\n" +
        "\n" +
        "  (func $itoa (param $n i32) (result i32)\n" +
        "    (local $neg i32) (local $x i32) (local $i i32) (local $len i32)\n" +
        "    (local $ptr i32) (local $j i32) (local $digit i32)\n" +
        "    (local.set $x (local.get $n))\n" +
        "    (if (i32.eq (local.get $x) (i32.const 0))\n" +
        "      (then\n" +
        "        (i32.store8 (i32.const 0) (i32.const 48))\n" +
        "        (local.set $i (i32.const 1))\n" +
        "      )\n" +
        "      (else\n" +
        "        (if (i32.lt_s (local.get $x) (i32.const 0))\n" +
        "          (then\n" +
        "            (local.set $neg (i32.const 1))\n" +
        "            (local.set $x (i32.sub (i32.const 0) (local.get $x)))\n" +
        "          )\n" +
        "        )\n" +
        "        (block $done_digits\n" +
        "          (loop $loop_digits\n" +
        "            (br_if $done_digits (i32.eqz (local.get $x)))\n" +
        "            (local.set $digit (i32.rem_s (local.get $x) (i32.const 10)))\n" +
        "            (i32.store8 (i32.add (i32.const 0) (local.get $i)) (i32.add (local.get $digit) (i32.const 48)))\n" +
        "            (local.set $i (i32.add (local.get $i) (i32.const 1)))\n" +
        "            (local.set $x (i32.div_s (local.get $x) (i32.const 10)))\n" +
        "            (br $loop_digits)\n" +
        "          )\n" +
        "        )\n" +
        "      )\n" +
        "    )\n" +
        "    (local.set $len (local.get $i))\n" +
        "    (local.set $ptr (call $alloc (i32.add (i32.add (local.get $len) (local.get $neg)) (i32.const 1))))\n" +
        "    (local.set $j (i32.const 0))\n" +
        "    (if (local.get $neg)\n" +
        "      (then\n" +
        "        (i32.store8 (local.get $ptr) (i32.const 45))\n" +
        "        (local.set $j (i32.const 1))\n" +
        "      )\n" +
        "    )\n" +
        "    (local.set $i (i32.sub (local.get $len) (i32.const 1)))\n" +
        "    (block $done_rev\n" +
        "      (loop $loop_rev\n" +
        "        (br_if $done_rev (i32.lt_s (local.get $i) (i32.const 0)))\n" +
        "        (i32.store8 (i32.add (local.get $ptr) (local.get $j)) (i32.load8_u (i32.add (i32.const 0) (local.get $i))))\n" +
        "        (local.set $j (i32.add (local.get $j) (i32.const 1)))\n" +
        "        (local.set $i (i32.sub (local.get $i) (i32.const 1)))\n" +
        "        (br $loop_rev)\n" +
        "      )\n" +
        "    )\n" +
        "    (i32.store8 (i32.add (local.get $ptr) (local.get $j)) (i32.const 0))\n" +
        "    (local.get $ptr)\n" +
        "  )\n" +
        "\n" +
        "  (func $streq (param $a i32) (param $b i32) (result i32)\n" +
        "    (local $i i32) (local $ca i32) (local $cb i32)\n" +
        "    (block $result (result i32)\n" +
        "      (loop $loop\n" +
        "        (local.set $ca (i32.load8_u (i32.add (local.get $a) (local.get $i))))\n" +
        "        (local.set $cb (i32.load8_u (i32.add (local.get $b) (local.get $i))))\n" +
        "        (if (i32.ne (local.get $ca) (local.get $cb))\n" +
        "          (then (br $result (i32.const 0)))\n" +
        "        )\n" +
        "        (if (i32.eqz (local.get $ca))\n" +
        "          (then (br $result (i32.const 1)))\n" +
        "        )\n" +
        "        (local.set $i (i32.add (local.get $i) (i32.const 1)))\n" +
        "        (br $loop)\n" +
        "      )\n" +
        "      (unreachable)\n" +
        "    )\n" +
        "  )\n";
}
