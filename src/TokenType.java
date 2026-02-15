/**
 * TokenType.java
 * Defines all possible categories a token can belong to in ZenLang.
 *
 * ZenLang is a custom educational programming language designed for
 * teaching compiler construction concepts.
 */
public enum TokenType {

    // ── Reserved words ──────────────────────────────────────────────────────
    KEYWORD,            // start, finish, loop, condition, declare, output, input,
                        // function, return, break, continue, else

    // ── Names ────────────────────────────────────────────────────────────────
    IDENTIFIER,         // [A-Z][a-z0-9_]{0,30}

    // ── Literal values ───────────────────────────────────────────────────────
    INT_LITERAL,        // [+-]?[0-9]+
    REAL_LITERAL,       // [+-]?[0-9]+\.[0-9]{1,6}([eE][+-]?[0-9]+)?
    TEXT_LITERAL,       // "..." with escape sequences
    CHAR_LITERAL,       // '.' with escape sequences
    BOOL_LITERAL,       // true | false  (case-insensitive)

    // ── Operators ────────────────────────────────────────────────────────────
    ARITH_OP,           // + - * / % **
    RELATIONAL_OP,      // == != < > <= >=
    LOGICAL_OP,         // && || !
    ASSIGN_OP,          // = += -= *= /= %=
    INC_OP,             // ++
    DEC_OP,             // --

    // ── Delimiters ───────────────────────────────────────────────────────────
    DELIMITER,          // ( ) { } [ ] , ; :

    // ── Comments (tracked but not emitted) ───────────────────────────────────
    LINE_COMMENT,       // ## ...
    BLOCK_COMMENT,      // #* ... *#

    // ── Misc ─────────────────────────────────────────────────────────────────
    SPACE,              // whitespace (skipped)
    INVALID,            // unrecognised token
    END_OF_FILE         // sentinel at end of input
}
