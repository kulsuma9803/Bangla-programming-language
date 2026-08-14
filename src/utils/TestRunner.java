package utils;

import ast.ProgramNode;
import lexer.Lexer;
import lexer.Token;
import parser.Parser;
import semantic.SemanticAnalyzer;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Stream;

public class TestRunner {

    private static final String LINE = "------------------------------------------------------------";

    public static class Result {
        public final String path;
        public final boolean expectedNoErrors;
        public final boolean actualNoErrors;
        public final int errorCount;

        public Result(String path, boolean expectedNoErrors, boolean actualNoErrors, int errorCount) {
            this.path = path;
            this.expectedNoErrors = expectedNoErrors;
            this.actualNoErrors = actualNoErrors;
            this.errorCount = errorCount;
        }

        public boolean passed() {
            return expectedNoErrors == actualNoErrors;
        }
    }

    public void runAll(String testsRoot) {
        List<Path> files = discover(testsRoot);

        System.out.println(LINE);
        System.out.println("  স্বয়ংক্রিয় টেস্ট স্যুট — " + testsRoot + " ফোল্ডারে " + BanglaUtil.toBanglaNum(files.size()) + " টি ফাইল পাওয়া গেছে");
        System.out.println(LINE);

        List<Result> results = new ArrayList<>();
        for (Path file : files) {
            results.add(runOne(file));
        }

        System.out.println();
        for (Result r : results) {
            String status = r.passed() ? "[উত্তীর্ণ]" : "[অকৃতকার্য]";
            String expected = r.expectedNoErrors ? "০টি error আশা করা হচ্ছে" : "কমপক্ষে ১টি error আশা করা হচ্ছে";
            String actual = BanglaUtil.toBanglaNum(r.errorCount) + " টি error পাওয়া গেছে";
            System.out.printf("  %-12s %-55s (%s, %s)%n", status, r.path, expected, actual);
        }

        long passed = results.stream().filter(Result::passed).count();
        long failed = results.size() - passed;

        System.out.println();
        System.out.println(LINE);
        System.out.printf("  সারাংশ: মোট = %s | উত্তীর্ণ = %s | অকৃতকার্য = %s%n", 
                BanglaUtil.toBanglaNum(results.size()), BanglaUtil.toBanglaNum(passed), BanglaUtil.toBanglaNum(failed));
        System.out.println(LINE);
        if (failed == 0) {
            System.out.println("  সব টেস্ট উত্তীর্ণ হয়েছে ✔ — প্রতিটা টেস্ট ফাইল তার নাম অনুযায়ী ঠিক কাজ করছে।");
        } else {
            System.out.println("  " + BanglaUtil.toBanglaNum(failed) + " টি টেস্ট প্রত্যাশা অনুযায়ী কাজ করেনি — উপরে [অকৃতকার্য] লাইনগুলো দেখো।");
        }
        System.out.println(LINE);
    }

    private List<Path> discover(String testsRoot) {
        List<Path> files = new ArrayList<>();
        try (Stream<Path> walk = Files.walk(Path.of(testsRoot))) {
            walk.filter(p -> p.toString().endsWith(".bpl"))
                .sorted()
                .forEach(files::add);
        } catch (IOException e) {
            System.out.println("Could not scan " + testsRoot + ": " + e.getMessage());
        }
        return files;
    }

    
    private static final String[] ERROR_INDICATING_WORDS = {
            "invalid", "missing", "undefined", "duplicate", "mismatch", "uninitialized"
    };

    private Result runOne(Path file) {
        String pathStr = file.toString().replace('\\', '/');
        boolean expectedNoErrors = true;
        for (String word : ERROR_INDICATING_WORDS) {
            if (pathStr.contains(word)) {
                expectedNoErrors = false;
                break;
            }
        }

        ErrorReporter errors = new ErrorReporter();
        try {
            String source = FileLoader.read(pathStr);
            Lexer lexer = new Lexer(source, errors);
            List<Token> tokens = lexer.tokenize();
            Parser parser = new Parser(tokens, errors);
            ProgramNode ast = parser.parseProgram();
            new SemanticAnalyzer(errors).analyze(ast);
        } catch (IOException e) {
            System.out.println("Could not read " + pathStr + ": " + e.getMessage());
        }

        boolean actualNoErrors = !errors.hasErrors();
        return new Result(pathStr, expectedNoErrors, actualNoErrors, errors.getErrors().size());
    }
}
