%%

%public
%class Yylex
%unicode
%line
%column
%type Token

%{
    /* No extra imports needed — Token and TokenType are in the same package */
%}

/* ─── Macro definitions ─────────────────────────────────────────────────── */

DIGIT      = [0-9]
UPPER      = [A-Z]
LOWER      = [a-z]
IDENT      = {UPPER}({LOWER}|{DIGIT}|_){0,30}
INT_PAT    = [+-]?{DIGIT}+
REAL_PAT   = [+-]?{DIGIT}+\.{DIGIT}{1,6}([eE][+-]?{DIGIT}+)?

%%

/* ─── Rules (in priority order) ─────────────────────────────────────────── */

/* 1. Block comments */
"#*"([^*]|\*+[^*#])*\*+"#"   { /* discard */ }

/* 2. Line comments */
"##".*                        { /* discard */ }

/* 3. Multi-character operators */
"**"  { return new Token(TokenType.ARITH_OP,      yytext(), yyline+1, yycolumn+1); }
"=="  { return new Token(TokenType.RELATIONAL_OP, yytext(), yyline+1, yycolumn+1); }
"!="  { return new Token(TokenType.RELATIONAL_OP, yytext(), yyline+1, yycolumn+1); }
"<="  { return new Token(TokenType.RELATIONAL_OP, yytext(), yyline+1, yycolumn+1); }
">="  { return new Token(TokenType.RELATIONAL_OP, yytext(), yyline+1, yycolumn+1); }
"&&"  { return new Token(TokenType.LOGICAL_OP,    yytext(), yyline+1, yycolumn+1); }
"||"  { return new Token(TokenType.LOGICAL_OP,    yytext(), yyline+1, yycolumn+1); }
"++"  { return new Token(TokenType.INC_OP,        yytext(), yyline+1, yycolumn+1); }
"--"  { return new Token(TokenType.DEC_OP,        yytext(), yyline+1, yycolumn+1); }
"+="  { return new Token(TokenType.ASSIGN_OP,     yytext(), yyline+1, yycolumn+1); }
"-="  { return new Token(TokenType.ASSIGN_OP,     yytext(), yyline+1, yycolumn+1); }
"*="  { return new Token(TokenType.ASSIGN_OP,     yytext(), yyline+1, yycolumn+1); }
"/="  { return new Token(TokenType.ASSIGN_OP,     yytext(), yyline+1, yycolumn+1); }
"%="  { return new Token(TokenType.ASSIGN_OP,     yytext(), yyline+1, yycolumn+1); }

/* 4. Keywords */
"start"     { return new Token(TokenType.KEYWORD, yytext(), yyline+1, yycolumn+1); }
"finish"    { return new Token(TokenType.KEYWORD, yytext(), yyline+1, yycolumn+1); }
"loop"      { return new Token(TokenType.KEYWORD, yytext(), yyline+1, yycolumn+1); }
"condition" { return new Token(TokenType.KEYWORD, yytext(), yyline+1, yycolumn+1); }
"declare"   { return new Token(TokenType.KEYWORD, yytext(), yyline+1, yycolumn+1); }
"output"    { return new Token(TokenType.KEYWORD, yytext(), yyline+1, yycolumn+1); }
"input"     { return new Token(TokenType.KEYWORD, yytext(), yyline+1, yycolumn+1); }
"function"  { return new Token(TokenType.KEYWORD, yytext(), yyline+1, yycolumn+1); }
"return"    { return new Token(TokenType.KEYWORD, yytext(), yyline+1, yycolumn+1); }
"break"     { return new Token(TokenType.KEYWORD, yytext(), yyline+1, yycolumn+1); }
"continue"  { return new Token(TokenType.KEYWORD, yytext(), yyline+1, yycolumn+1); }
"else"      { return new Token(TokenType.KEYWORD, yytext(), yyline+1, yycolumn+1); }

/* 5. Boolean literals */
"true"   { return new Token(TokenType.BOOL_LITERAL, yytext(), yyline+1, yycolumn+1); }
"false"  { return new Token(TokenType.BOOL_LITERAL, yytext(), yyline+1, yycolumn+1); }

/* 6. Identifiers */
{IDENT}  { return new Token(TokenType.IDENTIFIER, yytext(), yyline+1, yycolumn+1); }

/* 7. Real literals (must precede integer rule) */
{REAL_PAT}  { return new Token(TokenType.REAL_LITERAL, yytext(), yyline+1, yycolumn+1); }

/* 8. Integer literals */
{INT_PAT}   { return new Token(TokenType.INT_LITERAL, yytext(), yyline+1, yycolumn+1); }

/* 9. Text (string) literals */
\"([^\"\\]|\\[\"\\ntr])*\"  {
    return new Token(TokenType.TEXT_LITERAL, yytext(), yyline+1, yycolumn+1);
}

/* 10. Character literals */
\'([^\'\\]|\\[\'\\ntr])\'  {
    return new Token(TokenType.CHAR_LITERAL, yytext(), yyline+1, yycolumn+1);
}

/* 11. Single-character operators */
"="  { return new Token(TokenType.ASSIGN_OP,     yytext(), yyline+1, yycolumn+1); }
"<"  { return new Token(TokenType.RELATIONAL_OP, yytext(), yyline+1, yycolumn+1); }
">"  { return new Token(TokenType.RELATIONAL_OP, yytext(), yyline+1, yycolumn+1); }
"+"  { return new Token(TokenType.ARITH_OP,      yytext(), yyline+1, yycolumn+1); }
"-"  { return new Token(TokenType.ARITH_OP,      yytext(), yyline+1, yycolumn+1); }
"*"  { return new Token(TokenType.ARITH_OP,      yytext(), yyline+1, yycolumn+1); }
"/"  { return new Token(TokenType.ARITH_OP,      yytext(), yyline+1, yycolumn+1); }
"%"  { return new Token(TokenType.ARITH_OP,      yytext(), yyline+1, yycolumn+1); }
"!"  { return new Token(TokenType.LOGICAL_OP,    yytext(), yyline+1, yycolumn+1); }

/* 12. Delimiters */
[(){}\[\],;:]  {
    return new Token(TokenType.DELIMITER, yytext(), yyline+1, yycolumn+1);
}

/* 13. Whitespace – skip */
[ \t\r\n]+  { /* discard */ }

/* Catch-all: unrecognised character */
.  {
    System.err.println("Lexical error at Line " + (yyline+1) +
                       ", Col " + (yycolumn+1) +
                       ": unrecognised character '" + yytext() + "'");
}
