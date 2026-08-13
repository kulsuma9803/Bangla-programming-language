package utils;

import java.util.ArrayList;
import java.util.List;

/**
 * Central place to collect and print errors from every compiler phase
 * (lexical, syntax, semantic). Keeping this shared means the compiler
 * never needs to crash on the first error — it can recover and keep
 * going, which satisfies the "no runtime crashes" requirement.
 */
public class ErrorReporter {

    public static class CompileError {
        public final String phase;   // "Lexical" | "Syntax" | "Semantic"
        public final String message;
        public final int line;
        public final int column;

        public CompileError(String phase, String message, int line, int column) {
            this.phase = phase;
            this.message = message;
            this.line = line;
            this.column = column;
        }

        @Override
        public String toString() {
            return String.format("[%s Error] Line %s:%s -> %s", 
                phase, BanglaUtil.toBanglaNum(line), BanglaUtil.toBanglaNum(column), message);
        }
    }

    private final List<CompileError> errors = new ArrayList<>();

    public void report(String phase, String message, int line, int column) {
        errors.add(new CompileError(phase, message, line, column));
    }

    public boolean hasErrors() {
        return !errors.isEmpty();
    }

    public List<CompileError> getErrors() {
        return errors;
    }

    private static final String LINE = "----------------------------------------------------------";

    /**
     * Prints a clean, bordered summary of every error collected so far
     * (across whichever phases have run). Designed to look presentable
     * during a live demo — clear status line, numbered entries, one
     * error per line with phase/line/column called out explicitly.
     */
    public void printAll() {
        System.out.println(LINE);
        if (errors.isEmpty()) {
            System.out.println("Status : OK  ->  কোনো error পাওয়া যায়নি (No errors found)");
            System.out.println(LINE);
            return;
        }

        System.out.println("Status : FAILED  ->  মোট " + BanglaUtil.toBanglaNum(errors.size()) +
                (errors.size() == 1 ? " টি error পাওয়া গেছে" : " টি error পাওয়া গেছে"));
        System.out.println(LINE);

        int i = 1;
        for (CompileError e : errors) {
            System.out.printf("  [%s] %-9s Error  |  Line %-3s Col %-3s%n", 
                BanglaUtil.toBanglaNum(i), e.phase, BanglaUtil.toBanglaNum(e.line), BanglaUtil.toBanglaNum(e.column));
            System.out.println("      -> " + e.message);
            i++;
        }
        System.out.println(LINE);
    }

    /** Same as printAll(), but with a titled header naming the phase/file being reported on. */
    public void printAll(String title) {
        System.out.println();
        System.out.println(LINE);
        System.out.println("  " + title);
        printAll();
    }

    public void clear() {
        errors.clear();
    }
}
