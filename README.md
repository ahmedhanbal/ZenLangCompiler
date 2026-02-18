# ZenLang Lexical Analyser

**Members:**
- **Ahmed Ali Zahid** (22i-1271)
- **Asad Mehdi** (22i-1120)

---

## 1. Language Overview
**ZenLang** uses the extension `.lang`.

### 1.1 Keywords & Meanings
All keywords are reserved and case-sensitive.

| Keyword | Meaning |
| :--- | :--- |
| `start` | Entry point of program / function definition block start |
| `finish` | End of program / block |
| `declare` | Variable declaration |
| `condition` | Conditional statement (if) |
| `else` | Alternative conditional branch |
| `loop` | Loop construct (while) |
| `output` | Print statement |
| `input` | Read statement |
| `function` | Function definition marker |
| `return` | Return value from function |
| `break` | Exit loop immediately |
| `continue` | Skip to next loop iteration |

### 1.2 Identifiers
- **Rule:** Must start with an **uppercase letter** (`A-Z`).
- **Followed by:** Any combination of lowercase (`a-z`), digits (`0-9`), or underscore (`_`).
- **Maximum Length:** 30 characters.
- **Examples:** `Sum`, `My_Counter_1`, `Result`. (Invalid: `count`, `1var`, `_temp`).

### 1.3 Literals
| Type | Format | Examples |
| :--- | :--- | :--- |
| **Integer** | Optional sign + digits | `0`, `123`, `-45`, `+99` |
| **Float** | Optional sign + digits + dot + digits + opt exponent | `3.14`, `-0.001`, `1.2e-5`, `+6.0E+9` |
| **Boolean** | `true` or `false` | `true`, `false` |
| **String** | Double quotes, supports escapes `\n`, `\t` | `"Hello World"`, `"Val: \n"` |
| **Char** | Single quotes, single char or escape | `'A'`, `'\n'`, `'\''` |

### 1.4 Comments
- **Block Comment:** Starts with `#*` and ends with `*#`. Can span multiple lines.
- **Line Comment:** Starts with `##` and extends to the end of the line.

---

## 2. Operators & Precedence
Operators are listed from highest to lowest precedence (conceptually, though lexer emits tokens linearly).

| Precedence | Operators | Description |
| :--- | :--- | :--- |
| 1 | `( ) [ ]` | Grouping, Array Indexing |
| 2 | `++ --` | Increment, Decrement |
| 3 | `**` | Exponentiation |
| 4 | `* / %` | Multiplication, Division, Modulo |
| 5 | `+ -` | Addition, Subtraction |
| 6 | `< > <= >=` | Relational Comparisons |
| 7 | `== !=` | Equality Tests |
| 8 | `&&` | Logical AND |
| 9 | `||` | Logical OR |
| 10 | `! ` | Logical NOT |
| 11 | `= += -= *= /= %=` | Assignment |

---

## 3. Sample Programs

### Sample 1: Hello World
```lang
start
    #* This is a simple
       hello world program *#
    output "Hello, ZenLang!"
finish
```

### Sample 2: Fibonacci Sequence
```lang
start
    declare Count = 10
    declare T1 = 0
    declare T2 = 1
    declare Next = 0

    output "Fibonacci Series:"

    ## Loop to generate series
    loop (Count > 0)
        output T1
        Next = T1 + T2
        T1 = T2
        T2 = Next
        Count--
    finish
finish
```

### Sample 3: Factorial Calculation
```lang
start function Factorial(N)
    condition (N <= 1)
        return 1
    finish
    return N * Factorial(N - 1)
finish

start
    declare Num = 5
    declare Result = 0
    Result = Factorial(Num)
    output "Factorial of 5 is: "
    output Result
finish
```

---

## 4. Compilation & Execution

### Prerequisites
- Java JDK 8+
- JFlex (tested with 1.8.2)

### Build Instructions

**1. Manual Scanner (Hand-coded DFA)**
```bash
# Compile
cd src
javac ManualScanner.java

# Run
java ManualScanner ../tests/test1.lang
```

**2. JFlex Scanner**
```bash
# Generate Lexer
cd src
jflex Scanner.flex

# Compile
javac JFlexScanner.java Yylex.java

# Run
java -cp . JFlexScanner ../tests/test1.lang
```

### File Structure
- `src/ManualScanner.java`: Handwritten DFA implementation.
- `src/Scanner.flex`: JFlex specification file.
- `src/Yylex.java`: Generated scanner code.
- `docs/Automata_Design.pdf`: DFA diagrams and design report.
- `docs/Comparison.pdf`: Comparison of manual vs generated scanner outputs.
