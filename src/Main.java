import lexer.Lexer;
import lexer.Token;
import parser.Parser;
import parser.ASTPrinter;
import parser.GraphvizExporter;
import ast.ProgramNode;
import semantic.SemanticAnalyzer;
import semantic.SymbolTablePrinter;
import codegen.TACGenerator;
import codegen.ThreeAddressCode;
import codegen.TACInstr;
import codegen.PythonCodeGenerator;
import codegen.WasmCodeGenerator;
import utils.ErrorReporter;
import utils.FileLoader;
import utils.TestRunner;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Scanner;
public class Main {

    public static void main(String[] args) {
        Scanner scanner = new Scanner(System.in);

        System.out.println("===== কথন (Kothon) Bangla Compiler — Review 1 + Review 2 =====");
        System.out.print("Source file path (.bpl) [Enter for default demo]: ");
        String path = scanner.nextLine().trim();
        if (path.isEmpty()) {
            path = "tests/review1_demo/demo_program.bpl";
        }

        String source;
        try {
            source = FileLoader.read(path);
        } catch (Exception e) {
            System.out.println("File পড়া যায়নি: " + path + " (" + e.getMessage() + ")");
            return;
        }

        while (true) {
            System.out.println();
            System.out.println("===== Bangla Compiler =====");
            System.out.println("1. Run Lexer");
            System.out.println("2. Run Parser");
            System.out.println("3. Run Semantic Analyzer");
            System.out.println("4. Export AST as Graphviz (.dot)");
            System.out.println("5. REPL Mode (Interactive)");
            System.out.println("6. Run Automated Test Suite (all tests/ files)");
            System.out.println("7. Generate Three Address Code (TAC)");
            System.out.println("8. Generate Python Target Code");
            System.out.println("9. Generate WebAssembly Target Code (Optional)");
            System.out.println("10. Run Full Pipeline");
            System.out.println("11. Exit");
            System.out.print("Choose: ");
            String choice = scanner.nextLine().trim();

            switch (choice) {
                case "1": runLexer(source); break;
                case "2": runParser(source); break;
                case "3": runSemantic(source); break;
                case "4": exportGraphviz(source); break;
                case "5": runRepl(scanner); break;
                case "6": new TestRunner().runAll("tests"); break;
                case "7": generateTAC(source); break;
                case "8": generatePythonCode(source); break;
                case "9": generateWasmCode(source); break;
                case "10": runFullPipeline(source); break;
                case "11":
                    System.out.println("বিদায়!");
                    return;
                default:
                    System.out.println("Invalid choice. আবার চেষ্টা করো।");
            }
        }
    }

    private static final String BORDER = "============================================================";

    /** Prints a bordered section title, e.g. header("LEXER OUTPUT") */
    private static void header(String title) {
        System.out.println();
        System.out.println(BORDER);
        System.out.println("   " + title);
        System.out.println(BORDER);
    }

    private static void runLexer(String source) {
        ErrorReporter errors = new ErrorReporter();
        Lexer lexer = new Lexer(source, errors);
        List<Token> tokens = lexer.tokenize();

        header("PHASE 1: LEXER OUTPUT (Tokens)");
        for (Token t : tokens) {
            System.out.println("  " + t);
        }
        System.out.println();
        System.out.println("  Total tokens produced: " + utils.BanglaUtil.toBanglaNum(tokens.size()));
        errors.printAll("LEXICAL ANALYSIS RESULT");
    }

    private static void runParser(String source) {
        ErrorReporter errors = new ErrorReporter();
        Lexer lexer = new Lexer(source, errors);
        List<Token> tokens = lexer.tokenize();

        Parser parser = new Parser(tokens, errors);
        ProgramNode ast = parser.parseProgram();

        header("PHASE 2: PARSER OUTPUT (Abstract Syntax Tree)");
        new ASTPrinter().print(ast);
        errors.printAll("SYNTAX ANALYSIS RESULT (includes lexical errors, if any)");
    }

    private static void runSemantic(String source) {
        ErrorReporter errors = new ErrorReporter();
        Lexer lexer = new Lexer(source, errors);
        List<Token> tokens = lexer.tokenize();

        Parser parser = new Parser(tokens, errors);
        ProgramNode ast = parser.parseProgram();

        SemanticAnalyzer semantic = new SemanticAnalyzer(errors);
        semantic.analyze(ast);

        header("PHASE 3: SEMANTIC ANALYSIS OUTPUT");
        System.out.println("  Global Symbol Table:");
        new SymbolTablePrinter().print(semantic.getGlobalSymbols());
        errors.printAll("SEMANTIC ANALYSIS RESULT (includes lexical + syntax errors, if any)");
    }

    /**
     * Runs ALL FIVE compiler phases back-to-back and shows each
     * phase's output in order: Lexer -> Parser -> Semantic Analyzer
     * -> TAC -> Python target code. TAC and Python generation are
     * skipped (with a clear message) if the earlier phases found any
     * errors, since generating code from a broken AST makes no sense.
     */
    /** Translates a TACInstr class into a short Bangla label for the "ধরন" column. */
    private static String tacKindBangla(TACInstr instr) {
        return instr.getClass().getSimpleName().replaceFirst("^TAC", "");
    }

    private static void runFullPipeline(String source) {
        ErrorReporter errors = new ErrorReporter();

        header("FULL PIPELINE — PHASE 1/5: LEXER (Tokens)");
        Lexer lexer = new Lexer(source, errors);
        List<Token> tokens = lexer.tokenize();
        for (Token t : tokens) {
            System.out.println("  " + t);
        }
        System.out.println();
        System.out.println("  Total tokens produced: " + utils.BanglaUtil.toBanglaNum(tokens.size()));

        header("FULL PIPELINE — PHASE 2/5: PARSER (Abstract Syntax Tree)");
        Parser parser = new Parser(tokens, errors);
        ProgramNode ast = parser.parseProgram();
        new ASTPrinter().print(ast);

        header("FULL PIPELINE — PHASE 3/5: SEMANTIC ANALYZER");
        SemanticAnalyzer semantic = new SemanticAnalyzer(errors);
        semantic.analyze(ast);
        System.out.println("  Global Symbol Table:");
        new SymbolTablePrinter().print(semantic.getGlobalSymbols());

        header("FULL PIPELINE — PHASE 4/5: THREE ADDRESS CODE (TAC)");
        if (errors.hasErrors()) {
            System.out.println("  Source-এ error আছে বলে TAC generate করা হয়নি।");
        } else {
            ThreeAddressCode tac = new TACGenerator().generate(ast);
            if (tac.instructions.isEmpty()) {
                System.out.println("  (প্রোগ্রামে কোনো instruction নেই)");
            } else {
                System.out.println("  নং   ধরন            নির্দেশ");
                System.out.println("  --------------------------------------------------");
                int i = 1;
                for (TACInstr instr : tac.instructions) {
                    String kind = tacKindBangla(instr);
                    System.out.println("  " + utils.BanglaUtil.toBanglaNum(i) + ". [" + kind + "] " + instr.toString());
                    i++;
                }
            }
        }

        header("FULL PIPELINE — PHASE 5/5: PYTHON TARGET CODE GENERATION");
        if (errors.hasErrors()) {
            System.out.println("  Source-এ error আছে বলে target code generate করা হয়নি।");
        } else {
            String pythonCode = new PythonCodeGenerator().generate(ast);
            String outPath = "output/program.py";
            try {
                Files.createDirectories(Path.of("output"));
                Files.write(Path.of(outPath), pythonCode.getBytes(StandardCharsets.UTF_8));
                System.out.println("  Python code লেখা হয়েছে: " + outPath);
                System.out.println("  রান করার জন্য টার্মিনালে লিখো: python " + outPath + "   (Mac/Linux-এ python3 লাগতে পারে)");
                System.out.println();
                System.out.println("  ---- Generated Code Preview ----");
                System.out.println(pythonCode);
            } catch (IOException e) {
                System.out.println("  Could not write " + outPath + ": " + e.getMessage());
            }
        }

        errors.printAll("FULL PIPELINE RESULT (Lexical + Syntax + Semantic)");
    }

    private static void exportGraphviz(String source) {
        ErrorReporter errors = new ErrorReporter();
        Lexer lexer = new Lexer(source, errors);
        List<Token> tokens = lexer.tokenize();
        Parser parser = new Parser(tokens, errors);
        ProgramNode ast = parser.parseProgram();

        String dot = new GraphvizExporter().export(ast);
        String outPath = "output/ast.dot";
        try {
            Files.createDirectories(Path.of("output"));
            Files.write(Path.of(outPath), dot.getBytes(StandardCharsets.UTF_8));
            header("GRAPHVIZ EXPORT");
            System.out.println("  AST exported to: " + outPath);
            System.out.println("  Render it with Graphviz:");
            System.out.println("    dot -Tpng " + outPath + " -o output/ast.png");
            System.out.println("  Or paste its contents into https://dreampuf.github.io/GraphvizOnline/");
        } catch (IOException e) {
            System.out.println("Could not write " + outPath + ": " + e.getMessage());
        }
        errors.printAll("PARSE RESULT (used for the exported AST)");
    }

    /**
     * Runs the full front-end (lexer -> parser -> semantic analyzer),
     * then — only if the program is error-free — generates and prints
     * Three-Address Code (TAC), the compiler's intermediate
     * representation between the AST and the final target code.
     */
    private static void generateTAC(String source) {
        ErrorReporter errors = new ErrorReporter();
        Lexer lexer = new Lexer(source, errors);
        List<Token> tokens = lexer.tokenize();
        Parser parser = new Parser(tokens, errors);
        ProgramNode ast = parser.parseProgram();
        SemanticAnalyzer semantic = new SemanticAnalyzer(errors);
        semantic.analyze(ast);

        header("THREE ADDRESS CODE (TAC) — Intermediate Representation");
        if (errors.hasErrors()) {
            System.out.println("  Source-এ error আছে বলে TAC generate করা হয়নি।");
            System.out.println("  আগে error গুলো ঠিক করো, তারপর আবার চেষ্টা করো।");
        } else {
            ThreeAddressCode tac = new TACGenerator().generate(ast);
            if (tac.instructions.isEmpty()) {
                System.out.println("  (প্রোগ্রামে কোনো instruction নেই)");
            } else {
                System.out.println("  নং   ধরন            নির্দেশ");
                System.out.println("  --------------------------------------------------");
                int i = 1;
                for (TACInstr instr : tac.instructions) {
                    String kind = tacKindBangla(instr);
                    System.out.println("  " + utils.BanglaUtil.toBanglaNum(i) + ". [" + kind + "] " + instr.toString());
                    i++;
                }
            }
        }
        errors.printAll("TAC GENERATION RESULT");
    }

    /**
     * Runs the full front-end, then — only if the program is
     * error-free — generates valid, executable Python 3 target code
     * and writes it to output/program.py (satisfying the requirement:
     * "Generation of a valid, executable Java or Python target code
     * file from the source").
     */
    private static void generatePythonCode(String source) {
        ErrorReporter errors = new ErrorReporter();
        Lexer lexer = new Lexer(source, errors);
        List<Token> tokens = lexer.tokenize();
        Parser parser = new Parser(tokens, errors);
        ProgramNode ast = parser.parseProgram();
        SemanticAnalyzer semantic = new SemanticAnalyzer(errors);
        semantic.analyze(ast);

        header("PYTHON TARGET CODE GENERATION");
        if (errors.hasErrors()) {
            System.out.println("  Source-এ error আছে বলে target code generate করা হয়নি।");
            System.out.println("  আগে error গুলো ঠিক করো, তারপর আবার চেষ্টা করো।");
            errors.printAll("CODEGEN RESULT");
            return;
        }

        String pythonCode = new PythonCodeGenerator().generate(ast);
        String outPath = "output/program.py";
        try {
            Files.createDirectories(Path.of("output"));
            Files.write(Path.of(outPath), pythonCode.getBytes(StandardCharsets.UTF_8));
            System.out.println("  Python code লেখা হয়েছে: " + outPath);
            System.out.println("  রান করার জন্য টার্মিনালে লিখো: python " + outPath + "   (Mac/Linux-এ python3 লাগতে পারে)");
            System.out.println();
            System.out.println("  ---- Generated Code Preview ----");
            System.out.println(pythonCode);
        } catch (IOException e) {
            System.out.println("Could not write " + outPath + ": " + e.getMessage());
        }
        errors.printAll("CODEGEN RESULT");
    }

    /** Node.js host harness that supplies print_i32/print_str to the compiled .wasm module. */
    private static final String WASM_RUN_HARNESS =
        "// Auto-generated by কথন (Kothon) — host harness for the WebAssembly target.\n" +
        "// Usage:\n" +
        "//   wat2wasm program.wat -o program.wasm   (install WABT: npm install -g wabt, or see\n" +
        "//                                            https://github.com/WebAssembly/wabt)\n" +
        "//   node run_wasm.mjs\n" +
        "import { readFileSync } from 'fs';\n" +
        "import { fileURLToPath } from 'url';\n" +
        "import { dirname, join } from 'path';\n" +
        "\n" +
        "const __dirname = dirname(fileURLToPath(import.meta.url));\n" +
        "const bytes = readFileSync(join(__dirname, 'program.wasm'));\n" +
        "\n" +
        "function readCString(memory, ptr) {\n" +
        "  const bytes = new Uint8Array(memory.buffer);\n" +
        "  let end = ptr;\n" +
        "  while (bytes[end] !== 0) end++;\n" +
        "  return Buffer.from(bytes.slice(ptr, end)).toString('utf8');\n" +
        "}\n" +
        "\n" +
        "const { instance } = await WebAssembly.instantiate(bytes, {\n" +
        "  env: {\n" +
        "    print_i32: (n) => console.log(n),\n" +
        "    print_str: (ptr) => console.log(readCString(instance.exports.memory, ptr)),\n" +
        "  },\n" +
        "});\n" +
        "\n" +
        "instance.exports.main();\n";

    /**
     * OPTIONAL feature from the project requirements: "Generated target
     * code in WebAssembly instead of Java or Python". Writes a .wat
     * (WebAssembly Text) file — the textual, human-readable form of
     * WebAssembly, exactly analogous to how PythonCodeGenerator writes
     * a .py file — plus a small Node.js harness to actually run it,
     * since raw WebAssembly has no built-in I/O of its own.
     */
    private static void generateWasmCode(String source) {
        ErrorReporter errors = new ErrorReporter();
        Lexer lexer = new Lexer(source, errors);
        List<Token> tokens = lexer.tokenize();
        Parser parser = new Parser(tokens, errors);
        ProgramNode ast = parser.parseProgram();
        SemanticAnalyzer semantic = new SemanticAnalyzer(errors);
        semantic.analyze(ast);

        header("WEBASSEMBLY TARGET CODE GENERATION (Optional)");
        if (errors.hasErrors()) {
            System.out.println("  Source-এ error আছে বলে target code generate করা হয়নি।");
            System.out.println("  আগে error গুলো ঠিক করো, তারপর আবার চেষ্টা করো।");
            errors.printAll("CODEGEN RESULT");
            return;
        }

        String watCode = new WasmCodeGenerator().generate(ast);
        String watPath = "output/program.wat";
        String harnessPath = "output/run_wasm.mjs";
        try {
            Files.createDirectories(Path.of("output"));
            Files.write(Path.of(watPath), watCode.getBytes(StandardCharsets.UTF_8));
            Files.write(Path.of(harnessPath), WASM_RUN_HARNESS.getBytes(StandardCharsets.UTF_8));
            System.out.println("  WebAssembly Text code লেখা হয়েছে: " + watPath);
            System.out.println("  রান করার জন্য:");
            System.out.println("    1) wat2wasm " + watPath + " -o output/program.wasm   (WABT toolkit লাগবে)");
            System.out.println("    2) node " + harnessPath);
            System.out.println();
            System.out.println("  ---- Generated Code Preview ----");
            System.out.println(watCode);
        } catch (IOException e) {
            System.out.println("Could not write " + watPath + ": " + e.getMessage());
        }
        errors.printAll("CODEGEN RESULT");
    }

    /**
     * REPL support: compiles the accumulated buffer of কথন source all
     * the way to Python and actually EXECUTES it, so a দেখাও(...) the
     * user just typed shows its real output immediately — not just
     * tokens/AST/symbol-table info. This is what "চালাও" runs.
     */
    private static void replRunAndShowOutput(String source) {
        ErrorReporter errors = new ErrorReporter();
        Lexer lexer = new Lexer(source, errors);
        List<Token> tokens = lexer.tokenize();
        Parser parser = new Parser(tokens, errors);
        ProgramNode ast = parser.parseProgram();
        SemanticAnalyzer semantic = new SemanticAnalyzer(errors);
        semantic.analyze(ast);

        header("চালাও — প্রোগ্রামের আউটপুট");
        if (errors.hasErrors()) {
            errors.printAll("REPL RESULT");
            return;
        }

        String pythonCode = new PythonCodeGenerator().generate(ast);
        Path tempFile = null;
        try {
            tempFile = Files.createTempFile("kothon_repl_", ".py");
            Files.write(tempFile, pythonCode.getBytes(StandardCharsets.UTF_8));

            // "python3" is the right name on Mac/Linux; Windows installs only
            // register "python". Try python3 first, fall back to python so this
            // works out of the box on either OS without the user configuring anything.
            Process process;
            try {
                process = new ProcessBuilder("python3", tempFile.toString()).redirectErrorStream(true).start();
            } catch (IOException notFound) {
                process = new ProcessBuilder("python", tempFile.toString()).redirectErrorStream(true).start();
            }
            String output = new String(process.getInputStream().readAllBytes(), StandardCharsets.UTF_8);
            process.waitFor();

            if (output.isBlank()) {
                System.out.println("  (কোনো output নেই — দেখাও() ব্যবহার করেছ তো?)");
            } else {
                System.out.print(output);
            }
        } catch (IOException | InterruptedException e) {
            System.out.println("  Python দিয়ে রান করা যায়নি: " + e.getMessage());
            System.out.println("  (নিশ্চিত করো এই মেশিনে 'python' অথবা 'python3' PATH-এ আছে)");
        } finally {
            if (tempFile != null) {
                try { Files.deleteIfExists(tempFile); } catch (IOException ignored) { }
            }
        }
    }

    private static void runRepl(Scanner scanner) {
        header("REPL MODE (Interactive)");
        System.out.println("  একটার পর একটা লাইন লিখো। বিশেষ কমান্ড:");
        System.out.println("    চালাও   -> এখন পর্যন্ত লেখা code পুরো pipeline দিয়ে run করো");
        System.out.println("    মুছি    -> এখন পর্যন্ত লেখা code মুছে ফেলো, নতুন করে শুরু করো");
        System.out.println("    বাহির   -> REPL থেকে বের হয়ে main menu-তে ফিরে যাও");
        System.out.println();

        StringBuilder buffer = new StringBuilder();
        while (true) {
            System.out.print("kothon> ");
            String line = scanner.nextLine();

            if (line.equals("বাহির")) {
                System.out.println("REPL থেকে বের হওয়া হলো।");
                return;
            } else if (line.equals("মুছি")) {
                buffer.setLength(0);
                System.out.println("(buffer cleared)");
            } else if (line.equals("চালাও")) {
                if (buffer.length() == 0) {
                    System.out.println("(কিছু লেখা হয়নি এখনো)");
                    continue;
                }
                replRunAndShowOutput(buffer.toString());
            } else {
                buffer.append(line).append("\n");
            }
        }
    }
}
