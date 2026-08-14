import lexer.Lexer;
import lexer.Token;
import parser.Parser;
import parser.ASTPrinter;
import parser.GraphvizExporter;
import ast.ProgramNode;
import semantic.SemanticAnalyzer;
import semantic.SymbolTablePrinter;
import utils.ErrorReporter;
import utils.FileLoader;
import utils.TestRunner;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Scanner;

/**
 * Entry point for the কথন (Kothon) compiler — Review 1 build.
 * Presents a simple console menu so any team member can demo any
 * phase of the pipeline independently, or run it end-to-end.
 */
public class Main {

    public static void main(String[] args) {
        Scanner scanner = new Scanner(System.in);

        System.out.println("===== কথন (Kothon) Bangla Compiler — Review 1 =====");
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
            System.out.println("4. Run Full Pipeline");
            System.out.println("5. Export AST as Graphviz (.dot)");
            System.out.println("6. REPL Mode (Interactive)");
            System.out.println("7. Run Automated Test Suite (all tests/ files)");
            System.out.println("8. Exit");
            System.out.print("Choose: ");
            String choice = scanner.nextLine().trim();

            switch (choice) {
                case "1": runLexer(source); break;
                case "2": runParser(source); break;
                case "3": runSemantic(source); break;
                case "4": runFullPipeline(source); break;
                case "5": exportGraphviz(source); break;
                case "6": runRepl(scanner); break;
                case "7": new TestRunner().runAll("tests"); break;
                case "8":
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

    private static void runFullPipeline(String source) {
        ErrorReporter errors = new ErrorReporter();

        header("FULL PIPELINE — STEP 1/3: LEXER");
        Lexer lexer = new Lexer(source, errors);
        List<Token> tokens = lexer.tokenize();
        System.out.println("  -> " + utils.BanglaUtil.toBanglaNum(tokens.size()) + " tokens produced.");

        header("FULL PIPELINE — STEP 2/3: PARSER");
        Parser parser = new Parser(tokens, errors);
        ProgramNode ast = parser.parseProgram();
        System.out.println("  -> AST built with " + utils.BanglaUtil.toBanglaNum(ast.statements.size()) + " top-level statement(s).");

        header("FULL PIPELINE — STEP 3/3: SEMANTIC ANALYZER");
        SemanticAnalyzer semantic = new SemanticAnalyzer(errors);
        semantic.analyze(ast);
        System.out.println("  -> Symbol table checked.");

        header("GLOBAL SYMBOL TABLE");
        new SymbolTablePrinter().print(semantic.getGlobalSymbols());

        header("FINAL AST");
        new ASTPrinter().print(ast);

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
     * Interactive REPL: accumulate কথন source line by line, run it
     * through the full pipeline on demand, and keep the session going.
     * Great for live Q&A during a review — "what if I write X?" can be
     * answered on the spot instead of editing a file.
     */
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
                runFullPipeline(buffer.toString());
            } else {
                buffer.append(line).append("\n");
            }
        }
    }
}
