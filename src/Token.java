/**
 * Token.java
 * Represents a single lexical unit (token) produced by the ZenLang scanner.
 *
 * Each token carries:
 *   - its category  (what kind of token it is)
 *   - its text      (the exact characters from the source)
 *   - its position  (1-based line and column numbers)
 */
public class Token {

    private final TokenType category;
    private final String        text;
    private final int           line;
    private final int           col;

    /**
     * Constructs a new LexToken.
     *
     * @param category  the token category
     * @param text      the raw source text (lexeme)
     * @param line      1-based line number where the token starts
     * @param col       1-based column number where the token starts
     */
    public Token(TokenType category, String text, int line, int col) {
        this.category = category;
        this.text     = text;
        this.line     = line;
        this.col      = col;
    }

    // ── Accessors ────────────────────────────────────────────────────────────

    public TokenType getCategory() { return category; }
    public String        getText()     { return text;     }
    public int           getLine()     { return line;     }
    public int           getCol()      { return col;      }

    // ── Formatting ───────────────────────────────────────────────────────────

    /**
     * Standard output format required by the assignment:
     *   <CATEGORY, "text", Line: X, Col: Y>
     */
    @Override
    public String toString() {
        return String.format("<%s, \"%s\", Line: %d, Col: %d>",
                             category, text, line, col);
    }

    /**
     * Verbose format useful during debugging.
     */
    public String toDebugString() {
        return String.format("Token{cat=%s, text='%s', line=%d, col=%d}",
                             category, text, line, col);
    }
}
