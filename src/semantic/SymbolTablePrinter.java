package semantic;

import java.util.Collection;

public class SymbolTablePrinter {

    public void print(Collection<Symbol> symbols) {
        if (symbols.isEmpty()) {
            System.out.println("  (কোনো variable declare করা হয়নি — global scope-এ কিছু নেই)");
            return;
        }

        int nameWidth = "Name".length();
        int typeWidth = "Type".length();
        for (Symbol s : symbols) {
            nameWidth = Math.max(nameWidth, displayWidth(s.name));
            typeWidth = Math.max(typeWidth, displayWidth(s.type));
        }

        printRow("Name", "Type", "Initialized", nameWidth, typeWidth);
        printSeparator(nameWidth, typeWidth);
        for (Symbol s : symbols) {
            printRow(s.name, s.type, s.initialized ? "Yes" : "No", nameWidth, typeWidth);
        }
    }

    private void printRow(String name, String type, String init, int nameWidth, int typeWidth) {
        StringBuilder sb = new StringBuilder("  ");
        sb.append(pad(name, nameWidth)).append("  |  ");
        sb.append(pad(type, typeWidth)).append("  |  ");
        sb.append(init);
        System.out.println(sb);
    }

    private void printSeparator(int nameWidth, int typeWidth) {
        StringBuilder sb = new StringBuilder("  ");
        sb.append("-".repeat(nameWidth)).append("--+--");
        sb.append("-".repeat(typeWidth)).append("--+--");
        sb.append("-".repeat("Initialized".length()));
        System.out.println(sb);
    }

    private int displayWidth(String s) {
        return s.length();
    }

    private String pad(String s, int width) {
        StringBuilder sb = new StringBuilder(s);
        while (sb.length() < width) sb.append(' ');
        return sb.toString();
    }
}
