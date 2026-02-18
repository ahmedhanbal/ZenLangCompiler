import java.io.*;
import java.util.*;

/**
 * ManualScanner.java
 * Hand-coded DFA-based lexical analyser for ZenLang.
 *
 * ZenLang token categories (in matching priority order):
 *   1. Block comments   #* ... *#
 *   2. Line comments    ## ...
 *   3. Multi-char ops   ** == != <= >= && || ++ -- += -= *= /= %=
 *   4. Keywords         start finish loop condition declare output input
 *                       function return break continue else
 *   5. Boolean literals true | false
 *   6. Identifiers      [A-Z][a-z0-9_]{0,30}
 *   7. Real literals    [+-]?[0-9]+\.[0-9]{1,6}([eE][+-]?[0-9]+)?
 *   8. Integer literals [+-]?[0-9]+
 *   9. Text literals    "..." with escape sequences
 *  10. Char literals    '.' with escape sequences
 *  11. Single-char ops  + - * / % = < > ! & |
 *  12. Delimiters       ( ) { } [ ] , ; :
 *  13. Whitespace       (skipped, line numbers tracked)
 */
public class ManualScanner {

    // ── Reserved word sets ────────────────────────────────────────────────────

    private static final Set<String> RESERVED_WORDS = new HashSet<>(Arrays.asList(
        "start", "finish", "loop", "condition", "declare",
        "output", "input", "function", "return", "break", "continue", "else"
    ));

    private static final Set<String> BOOL_WORDS = new HashSet<>(Arrays.asList(
        "true", "false"
    ));

    // ── Source state ──────────────────────────────────────────────────────────

    private final String src;       // full source text
    private       int    pos;       // current read position
    private final int    srcLen;    // length of source

    // ── Position tracking ─────────────────────────────────────────────────────

    private int curLine;            // current line (1-based)
    private int curCol;             // current column (1-based)
    private int tokLine;            // line where current token started
    private int tokCol;             // column where current token started

    // ── Output collections ────────────────────────────────────────────────────

    private final List<Token>      tokenStream;
    private final SymbolTable     idTable;
    private final ErrorHandler            errorLog;

    // ── Statistics ────────────────────────────────────────────────────────────

    private final Map<TokenType, Integer> catCounts;
    private int commentCount;

    // ── Constructor ───────────────────────────────────────────────────────────

    /**
     * Initialises the lexer with the given source text.
     *
     * @param source  complete source code to analyse
     */
    public ManualScanner(String source) {
        this.src         = source;
        this.srcLen      = source.length();
        this.pos         = 0;
        this.curLine     = 1;
        this.curCol      = 1;

        this.tokenStream = new ArrayList<>();
        this.idTable     = new SymbolTable();
        this.errorLog    = new ErrorHandler();
        this.catCounts   = new HashMap<>();
        this.commentCount = 0;
    }

    // ── Main entry point ──────────────────────────────────────────────────────

    /**
     * Scans the entire source and populates the token stream.
     * Call this once before using any of the display/get methods.
     */
    public void tokenise() {
        while (pos < srcLen) {
            markTokenStart();
            Token tok = readNextToken();

            if (tok == null) continue;

            TokenType cat = tok.getCategory();

            if (cat == TokenType.LINE_COMMENT ||
                cat == TokenType.BLOCK_COMMENT) {
                commentCount++;                         // count but don't emit
            } else if (cat != TokenType.SPACE) {
                tokenStream.add(tok);
                catCounts.merge(cat, 1, Integer::sum);

                if (cat == TokenType.IDENTIFIER) {
                    idTable.record(tok.getText(), tok.getLine(), tok.getCol());
                }
            }
        }

        // Append sentinel
        tokenStream.add(new Token(TokenType.END_OF_FILE, "", curLine, curCol));
    }

    // ── Token dispatch ────────────────────────────────────────────────────────

    /**
     * Reads and returns the next token from the source.
     * Returns null to signal that a character was skipped due to an error
     * (the outer tokenise() loop will simply continue to the next position).
     */
    private Token readNextToken() {
        char ch = src.charAt(pos);

        // ── Priority 1: block comment ────────────────────────────────────────
        if (ch == '#' && lookahead(1) == '*') {
            return readBlockComment();
        }

        // ── Priority 2: line comment ─────────────────────────────────────────
        if (ch == '#' && lookahead(1) == '#') {
            return readLineComment();
        }

        // ── Priority 3: multi-character operators ────────────────────────────
        Token multiOp = tryMultiCharOp();
        if (multiOp != null) return multiOp;

        // ── Priority 4 & 5: keywords and booleans (start with lowercase) ─────
        if (isLower(ch)) {
            if (wordAhead("true") || wordAhead("false")) {
                return readBoolLiteral();
            }
            for (String kw : RESERVED_WORDS) {
                if (wordAhead(kw)) return readKeyword();
            }
            // Lowercase that is neither keyword nor boolean → error, skip char
            // Return null so the outer loop retries (avoids deep recursion)
            errorLog.badChar(ch, curLine, curCol);
            step();
            return null;
        }

        // ── Priority 6: identifiers ──────────────────────────────────────────
        if (isUpper(ch)) {
            return readIdentifier();
        }

        // ── Priority 7 & 8: numeric literals ─────────────────────────────────
        if (isDigit(ch) || ((ch == '+' || ch == '-') && isDigit(lookahead(1)))) {
            // Peek past optional sign and digits to see if there is a '.'
            int probe = pos;
            if (src.charAt(probe) == '+' || src.charAt(probe) == '-') probe++;
            while (probe < srcLen && isDigit(src.charAt(probe))) probe++;
            if (probe < srcLen && src.charAt(probe) == '.') {
                return readRealLiteral();
            }
            return readIntLiteral();
        }

        // ── Priority 9: text (string) literals ───────────────────────────────
        if (ch == '"') return readTextLiteral();

        // ── Priority 10: character literals ──────────────────────────────────
        if (ch == '\'') return readCharLiteral();

        // ── Priority 11: single-character operators ───────────────────────────
        if (isOpChar(ch)) return readSingleOp();

        // ── Priority 12: delimiters ───────────────────────────────────────────
        if (isDelimiter(ch)) return readDelimiter();

        // ── Priority 13: whitespace ───────────────────────────────────────────
        if (isSpace(ch)) return readWhitespace();

        // ── Fallback: unrecognised character ──────────────────────────────────
        // Return null so the outer loop retries (avoids deep recursion)
        errorLog.badChar(ch, curLine, curCol);
        step();
        return null; // non-recursive recovery: caller retries
    }

    // ── Token readers ─────────────────────────────────────────────────────────

    /** Reads a block comment: #* ... *# */
    private Token readBlockComment() {
        StringBuilder buf = new StringBuilder();
        int startLine = tokLine;
        int startCol  = tokCol;

        buf.append(eat()); // #
        buf.append(eat()); // *

        boolean closed = false;
        while (pos < srcLen) {
            if (src.charAt(pos) == '*' && lookahead(1) == '#') {
                buf.append(eat()); // *
                buf.append(eat()); // #
                closed = true;
                break;
            }
            buf.append(eat());
        }

        if (!closed) {
            errorLog.unterminatedComment(startLine, startCol);
        }

        return new Token(TokenType.BLOCK_COMMENT, buf.toString(), startLine, startCol);
    }

    /** Reads a line comment: ## ... <newline> */
    private Token readLineComment() {
        StringBuilder buf = new StringBuilder();
        int startLine = tokLine;
        int startCol  = tokCol;

        buf.append(eat()); // #
        buf.append(eat()); // #

        while (pos < srcLen && src.charAt(pos) != '\n') {
            buf.append(eat());
        }

        return new Token(TokenType.LINE_COMMENT, buf.toString(), startLine, startCol);
    }

    /**
     * Tries to match a two-character operator at the current position.
     * Returns null if no two-character operator is found.
     */
    private Token tryMultiCharOp() {
        if (pos + 1 >= srcLen) return null;

        char a = src.charAt(pos);
        char b = src.charAt(pos + 1);
        String pair = "" + a + b;

        switch (pair) {
            case "**":
                return consumeOp(pair, TokenType.ARITH_OP);
            case "==": case "!=": case "<=": case ">=":
                return consumeOp(pair, TokenType.RELATIONAL_OP);
            case "&&": case "||":
                return consumeOp(pair, TokenType.LOGICAL_OP);
            case "++":
                return consumeOp(pair, TokenType.INC_OP);
            case "--":
                return consumeOp(pair, TokenType.DEC_OP);
            case "+=": case "-=": case "*=": case "/=": case "%=":
                return consumeOp(pair, TokenType.ASSIGN_OP);
            default:
                return null;
        }
    }

    /** Consumes `op.length()` characters and returns a token. */
    private Token consumeOp(String op, TokenType cat) {
        int sl = tokLine, sc = tokCol;
        for (int i = 0; i < op.length(); i++) eat();
        return new Token(cat, op, sl, sc);
    }

    /** Reads a boolean literal: true | false */
    private Token readBoolLiteral() {
        StringBuilder buf = new StringBuilder();
        int sl = tokLine, sc = tokCol;
        while (pos < srcLen && isLetter(src.charAt(pos))) {
            buf.append(eat());
        }
        return new Token(TokenType.BOOL_LITERAL, buf.toString(), sl, sc);
    }

    /** Reads a keyword. */
    private Token readKeyword() {
        StringBuilder buf = new StringBuilder();
        int sl = tokLine, sc = tokCol;
        while (pos < srcLen && isLetter(src.charAt(pos))) {
            buf.append(eat());
        }
        return new Token(TokenType.KEYWORD, buf.toString(), sl, sc);
    }

    /**
     * Reads an identifier: [A-Z][a-z0-9_]{0,30}
     * Also catches identifiers that are too long.
     */
    private Token readIdentifier() {
        StringBuilder buf = new StringBuilder();
        int sl = tokLine, sc = tokCol;

        buf.append(eat()); // first char: uppercase letter

        while (pos < srcLen) {
            char ch = src.charAt(pos);
            if (isLower(ch) || isDigit(ch) || ch == '_') {
                buf.append(eat());
            } else {
                break;
            }
        }

        String name = buf.toString();
        if (name.length() > 31) {
            errorLog.badIdentifier(name, sl, sc,
                "Identifier length " + name.length() + " exceeds the 31-character limit");
        }

        // Identifiers that happen to spell a keyword are still keywords
        if (RESERVED_WORDS.contains(name)) {
            return new Token(TokenType.KEYWORD, name, sl, sc);
        }

        return new Token(TokenType.IDENTIFIER, name, sl, sc);
    }

    /**
     * Reads an integer literal: [+-]?[0-9]+
     */
    private Token readIntLiteral() {
        StringBuilder buf = new StringBuilder();
        int sl = tokLine, sc = tokCol;

        // Optional sign
        char ch = src.charAt(pos);
        if (ch == '+' || ch == '-') buf.append(eat());

        // Must have at least one digit
        if (pos >= srcLen || !isDigit(src.charAt(pos))) {
            errorLog.badNumber(buf.toString(), sl, sc, "Digit expected after sign");
            return new Token(TokenType.INVALID, buf.toString(), sl, sc);
        }

        while (pos < srcLen && isDigit(src.charAt(pos))) {
            buf.append(eat());
        }

        return new Token(TokenType.INT_LITERAL, buf.toString(), sl, sc);
    }

    /**
     * Reads a real (floating-point) literal:
     *   [+-]?[0-9]+\.[0-9]{1,6}([eE][+-]?[0-9]+)?
     */
    private Token readRealLiteral() {
        StringBuilder buf = new StringBuilder();
        int sl = tokLine, sc = tokCol;

        // Optional sign
        char ch = src.charAt(pos);
        if (ch == '+' || ch == '-') buf.append(eat());

        // Integer part
        while (pos < srcLen && isDigit(src.charAt(pos))) {
            buf.append(eat());
        }

        // Decimal point (mandatory for a real literal)
        if (pos < srcLen && src.charAt(pos) == '.') {
            buf.append(eat());
        } else {
            errorLog.badNumber(buf.toString(), sl, sc, "Decimal point expected");
            return new Token(TokenType.INVALID, buf.toString(), sl, sc);
        }

        // Fractional part: 1–6 digits required
        int fracDigits = 0;
        while (pos < srcLen && isDigit(src.charAt(pos))) {
            buf.append(eat());
            fracDigits++;
        }

        if (fracDigits == 0) {
            errorLog.badNumber(buf.toString(), sl, sc,
                "At least one digit required after the decimal point");
        } else if (fracDigits > 6) {
            errorLog.badNumber(buf.toString(), sl, sc,
                "Too many fractional digits (max 6, found " + fracDigits + ")");
        }

        // Optional exponent: [eE][+-]?[0-9]+
        if (pos < srcLen && (src.charAt(pos) == 'e' || src.charAt(pos) == 'E')) {
            buf.append(eat()); // e or E

            if (pos < srcLen && (src.charAt(pos) == '+' || src.charAt(pos) == '-')) {
                buf.append(eat());
            }

            int expDigits = 0;
            while (pos < srcLen && isDigit(src.charAt(pos))) {
                buf.append(eat());
                expDigits++;
            }

            if (expDigits == 0) {
                errorLog.badNumber(buf.toString(), sl, sc,
                    "Digit(s) required after exponent marker");
            }
        }

        return new Token(TokenType.REAL_LITERAL, buf.toString(), sl, sc);
    }

    /**
     * Reads a text (string) literal: "([^"\\\n]|(\\["\ntr]))*"
     */
    private Token readTextLiteral() {
        StringBuilder buf = new StringBuilder();
        int sl = tokLine, sc = tokCol;

        buf.append(eat()); // opening "
        boolean closed = false;

        while (pos < srcLen) {
            char ch = src.charAt(pos);

            if (ch == '\n') {
                errorLog.unterminatedString(buf.toString(), sl, sc);
                break;
            }

            if (ch == '"') {
                buf.append(eat());
                closed = true;
                break;
            }

            if (ch == '\\') {
                buf.append(eat()); // backslash
                if (pos < srcLen) {
                    char esc = src.charAt(pos);
                    if (esc == '"' || esc == '\\' || esc == 'n' || esc == 't' || esc == 'r') {
                        buf.append(eat());
                    } else {
                        errorLog.badEscape("\\" + esc, curLine, curCol);
                        eat(); // skip the bad escape char
                    }
                }
            } else {
                buf.append(eat());
            }
        }

        if (!closed) {
            errorLog.unterminatedString(buf.toString(), sl, sc);
        }

        return new Token(TokenType.TEXT_LITERAL, buf.toString(), sl, sc);
    }

    /**
     * Reads a character literal: '([^'\\\n]|(\\['ntr]))'
     */
    private Token readCharLiteral() {
        StringBuilder buf = new StringBuilder();
        int sl = tokLine, sc = tokCol;

        buf.append(eat()); // opening '
        boolean closed = false;
        int charsSeen = 0;

        while (pos < srcLen && charsSeen < 3) {
            char ch = src.charAt(pos);

            if (ch == '\n') {
                errorLog.unterminatedChar(buf.toString(), sl, sc);
                break;
            }

            if (ch == '\'') {
                buf.append(eat());
                closed = true;
                break;
            }

            if (ch == '\\') {
                buf.append(eat());
                charsSeen++;
                if (pos < srcLen) {
                    char esc = src.charAt(pos);
                    if (esc == '\'' || esc == '\\' || esc == 'n' || esc == 't' || esc == 'r') {
                        buf.append(eat());
                    } else {
                        errorLog.badEscape("\\" + esc, curLine, curCol);
                        eat();
                    }
                }
            } else {
                buf.append(eat());
                charsSeen++;
            }
        }

        if (!closed) {
            errorLog.unterminatedChar(buf.toString(), sl, sc);
        }

        return new Token(TokenType.CHAR_LITERAL, buf.toString(), sl, sc);
    }

    /** Reads a single-character operator. */
    private Token readSingleOp() {
        int sl = tokLine, sc = tokCol;
        char ch = eat();
        TokenType cat;

        switch (ch) {
            case '+': case '-': case '*': case '/': case '%':
                cat = TokenType.ARITH_OP;      break;
            case '<': case '>':
                cat = TokenType.RELATIONAL_OP; break;
            case '!':
                cat = TokenType.LOGICAL_OP;    break;
            case '=':
                cat = TokenType.ASSIGN_OP;     break;
            default:
                cat = TokenType.INVALID;
        }

        return new Token(cat, String.valueOf(ch), sl, sc);
    }

    /** Reads a single delimiter character. */
    private Token readDelimiter() {
        int sl = tokLine, sc = tokCol;
        char ch = eat();
        return new Token(TokenType.DELIMITER, String.valueOf(ch), sl, sc);
    }

    /** Consumes a run of whitespace characters. */
    private Token readWhitespace() {
        StringBuilder buf = new StringBuilder();
        int sl = tokLine, sc = tokCol;
        while (pos < srcLen && isSpace(src.charAt(pos))) {
            buf.append(eat());
        }
        return new Token(TokenType.SPACE, buf.toString(), sl, sc);
    }

    // ── Low-level helpers ─────────────────────────────────────────────────────

    /** Saves the current position as the start of the next token. */
    private void markTokenStart() {
        tokLine = curLine;
        tokCol  = curCol;
    }

    /**
     * Consumes and returns the current character, advancing position
     * and updating line/column counters.
     */
    private char eat() {
        char ch = src.charAt(pos++);
        if (ch == '\n') { curLine++; curCol = 1; }
        else            { curCol++;              }
        return ch;
    }

    /** Advances one character without returning it. */
    private void step() {
        if (pos < srcLen) eat();
    }

    /** Peeks at the character `offset` positions ahead (0 = current). */
    private char lookahead(int offset) {
        int idx = pos + offset;
        return (idx < srcLen) ? src.charAt(idx) : '\0';
    }

    /**
     * Returns true if the text starting at `pos` exactly matches `word`
     * AND the character immediately after is not a letter, digit, or underscore
     * (i.e., the word is not a prefix of a longer identifier).
     */
    private boolean wordAhead(String word) {
        int end = pos + word.length();
        if (end > srcLen) return false;
        if (!src.substring(pos, end).equals(word)) return false;
        if (end < srcLen) {
            char after = src.charAt(end);
            if (isLetter(after) || isDigit(after) || after == '_') return false;
        }
        return true;
    }

    // ── Character classification ───────────────────────────────────────────────

    private boolean isUpper(char c)     { return c >= 'A' && c <= 'Z'; }
    private boolean isLower(char c)     { return c >= 'a' && c <= 'z'; }
    private boolean isLetter(char c)    { return isUpper(c) || isLower(c); }
    private boolean isDigit(char c)     { return c >= '0' && c <= '9'; }
    private boolean isSpace(char c)     { return c == ' ' || c == '\t' || c == '\r' || c == '\n'; }

    private boolean isOpChar(char c) {
        return c == '+' || c == '-' || c == '*' || c == '/' || c == '%'
            || c == '=' || c == '<' || c == '>' || c == '!' || c == '&' || c == '|';
    }

    private boolean isDelimiter(char c) {
        return c == '(' || c == ')' || c == '{' || c == '}' ||
               c == '[' || c == ']' || c == ',' || c == ';' || c == ':';
    }

    // ── Output / display ──────────────────────────────────────────────────────

    /** Prints all non-EOF tokens in the required format. */
    public void printTokens() {
        final int W = 82;
        System.out.println("\n" + "=".repeat(W));
        System.out.println("TOKEN STREAM");
        System.out.println("=".repeat(W));

        for (Token t : tokenStream) {
            if (t.getCategory() != TokenType.END_OF_FILE) {
                System.out.println(t);
            }
        }

        System.out.println("=".repeat(W) + "\n");
    }

    /** Prints scanning statistics. */
    public void printStats() {
        final int W = 82;
        System.out.println("\n" + "=".repeat(W));
        System.out.println("SCAN STATISTICS");
        System.out.println("=".repeat(W));

        int emitted = tokenStream.size() - 1; // exclude EOF
        System.out.println("  Total tokens emitted : " + emitted);
        System.out.println("  Lines processed      : " + curLine);
        System.out.println("  Comments removed     : " + commentCount);
        System.out.println("  Lexical errors       : " + errorLog.errorCount());

        System.out.println("\n  Breakdown by category:");
        System.out.println("  " + "-".repeat(40));

        List<Map.Entry<TokenType, Integer>> sorted = new ArrayList<>(catCounts.entrySet());
        sorted.sort(Comparator.comparing(e -> e.getKey().toString()));
        for (Map.Entry<TokenType, Integer> e : sorted) {
            System.out.printf("    %-22s : %d%n", e.getKey(), e.getValue());
        }

        System.out.println("=".repeat(W) + "\n");
    }

    // ── Accessors ─────────────────────────────────────────────────────────────

    public List<Token>    getTokens()      { return tokenStream; }
    public SymbolTable   getIdTable()     { return idTable;     }
    public ErrorHandler          getErrorLog()    { return errorLog;    }

    // ── Main ──────────────────────────────────────────────────────────────────

    public static void main(String[] args) {
        if (args.length != 1) {
            System.out.println("Usage: java Lexer <source-file.zl>");
            return;
        }

        String filename = args[0];

        try {
            String source = readFile(filename);

            System.out.println("ZenLang Lexer  —  scanning: " + filename);
            System.out.println("=".repeat(82));

            ManualScanner lexer = new ManualScanner(source);
            lexer.tokenise();

            lexer.printTokens();
            lexer.printStats();
            lexer.getIdTable().display();
            lexer.getErrorLog().display();

        } catch (IOException ex) {
            System.err.println("Cannot read file: " + ex.getMessage());
        }
    }

    /** Reads an entire file into a String. */
    private static String readFile(String path) throws IOException {
        StringBuilder sb = new StringBuilder();
        try (BufferedReader br = new BufferedReader(new FileReader(path))) {
            String line;
            while ((line = br.readLine()) != null) {
                sb.append(line).append('\n');
            }
        }
        return sb.toString();
    }
}

