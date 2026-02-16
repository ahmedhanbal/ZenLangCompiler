import java.util.*;

/**
 * ErrorHandler.java
 * Collects, categorises, and reports lexical errors found during scanning.
 *
 * Error recovery strategy: every error is logged and scanning continues
 * so that all errors in the file are reported in a single pass.
 */
public class ErrorHandler {

    // ── Error record ──────────────────────────────────────────────────────────

    private static class ErrorRecord {
        final String kind;
        final int    line;
        final int    col;
        final String lexeme;
        final String detail;

        ErrorRecord(String kind, int line, int col, String lexeme, String detail) {
            this.kind   = kind;
            this.line   = line;
            this.col    = col;
            this.lexeme = lexeme;
            this.detail = detail;
        }

        @Override
        public String toString() {
            return String.format("ERROR [%s] Line: %d, Col: %d  lexeme='%s'  -> %s",
                                 kind, line, col, lexeme, detail);
        }
    }

    // ── State ─────────────────────────────────────────────────────────────────

    private final List<ErrorRecord> log = new ArrayList<>();

    // ── Reporting helpers ─────────────────────────────────────────────────────

    /** Logs an unrecognised character. */
    public void badChar(char ch, int line, int col) {
        push("INVALID_CHAR", line, col, String.valueOf(ch),
             "Character '" + ch + "' is not part of the ZenLang alphabet");
    }

    /** Logs a malformed numeric literal. */
    public void badNumber(String lexeme, int line, int col, String reason) {
        push("BAD_NUMBER", line, col, lexeme, reason);
    }

    /** Logs an identifier that violates naming rules. */
    public void badIdentifier(String lexeme, int line, int col, String reason) {
        push("BAD_IDENTIFIER", line, col, lexeme, reason);
    }

    /** Logs a string literal that was never closed. */
    public void unterminatedString(String partial, int line, int col) {
        push("UNTERMINATED_STRING", line, col, partial,
             "String literal opened with '\"' but never closed");
    }

    /** Logs a character literal that was never closed. */
    public void unterminatedChar(String partial, int line, int col) {
        push("UNTERMINATED_CHAR", line, col, partial,
             "Character literal opened with ''' but never closed");
    }

    /** Logs a block comment that was never closed. */
    public void unterminatedComment(int line, int col) {
        push("UNTERMINATED_COMMENT", line, col, "#*",
             "Block comment opened with '#*' but '*#' was never found");
    }

    /** Logs an invalid escape sequence inside a string or char literal. */
    public void badEscape(String seq, int line, int col) {
        push("BAD_ESCAPE", line, col, seq,
             "Unrecognised escape sequence. Valid: \\n \\t \\r \\\" \\' \\\\");
    }

    /** General-purpose error entry point. */
    public void push(String kind, int line, int col, String lexeme, String detail) {
        log.add(new ErrorRecord(kind, line, col, lexeme, detail));
    }

    // ── Query ─────────────────────────────────────────────────────────────────

    public boolean hasErrors()  { return !log.isEmpty(); }
    public int     errorCount() { return log.size();     }

    /** Returns all error messages as strings (for testing). */
    public List<String> allMessages() {
        List<String> out = new ArrayList<>();
        for (ErrorRecord r : log) out.add(r.toString());
        return out;
    }

    /** Clears all recorded errors. */
    public void reset() { log.clear(); }

    // ── Display ───────────────────────────────────────────────────────────────

    /** Prints the full error report to standard output. */
    public void display() {
        if (log.isEmpty()) {
            System.out.println("\n✓ No lexical errors detected.");
            return;
        }

        final int W = 82;
        System.out.println("\n" + "=".repeat(W));
        System.out.println("LEXICAL ERROR REPORT  (" + log.size() + " error(s))");
        System.out.println("=".repeat(W));

        for (int i = 0; i < log.size(); i++) {
            System.out.printf("  %2d. %s%n", i + 1, log.get(i));
        }

        System.out.println("=".repeat(W) + "\n");
    }
}
