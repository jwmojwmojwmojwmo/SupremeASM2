# SupremeBASIC Language Reference (v0.2)

SupremeBASIC is a work-in-progress compiled language that targets SupremeASM v2.

---

## Overview

SupremeBASIC is a statically-typed, imperative language. Every program compiles to
SupremeASM v2 (`.sasm`), which the Python assembler then turns into Supreme Machine Code
(`.smc`) for the C virtual machine:

```
 .sbas ──► Lexer ──► Parser ──► Sema ──► CodeGen ──► .sasm ──► asm.py ──► .smc ──► VM
           tokens    AST        types     SupremeASM            machine code
```

### Design philosophy

- **`DECLARE` everything.** Types, structs, variables, and arrays are all introduced with `DECLARE`.
  (Functions and blocks will follow the same pattern when they land.)
- **Unified block syntax.** Multi-line constructs use `THEN ... END;`.
- **Untyped literals.** A literal like `5` has no type of its own. It adopts the type of
  whatever it's combined with or assigned to, as long as it fits.
- **Raw data.** The language treats data as raw bytes. `PRINT` writes raw bytes. Ergo, you have to write itoa yourself. Many such cases. This is intentional. SupremeBASIC is less C and more a bridge between a C-like language and assembly.
- **Signed-only math.** All arithmetic and bounds checking assumes signed two's-complement
  integers. There are no unsigned types yet.
- **Every statement ends with `;`.** Comments are `//` to end of line.


Run
```
java basic/src/SupremeBasicCompiler.java
```
to start compiling .sbas files.

**Notes:**
- The input must be a `.sbas` file.
- The generated `<name>.sasm` is written to the **current working directory**, not next to
  the source file.
- Choosing `y`/`ym`/`yr`/`ymr` shells out to `python assembler/asm.py <name>.sasm --autorun`
  (plus dump flags). This assumes `python` is on your PATH and the VM is built.


### A first look

```
// points.sbas — types, structs, fields, expressions
DECLARE TYPE int  SIZE 4;
DECLARE TYPE byte SIZE 1;

DECLARE STRUCT Point THEN
    x AS int;
    y AS int;
END;

DECLARE VARIABLE p      AS Point;
DECLARE VARIABLE q      AS Point;
DECLARE VARIABLE distSq AS int;
DECLARE VARIABLE mask   AS byte;

x OF p <- 3;            // field write  (note: FIELD OF VARIABLE)
y OF p <- 4;

q <- p;                 // whole-struct copy, by value

distSq <- x OF q * x OF q + y OF q * y OF q;   // 25

mask <- 12;
mask <- mask << 2;      // 48
mask <- mask & 31;      // 16

PRINT "Hello World\n";
```

### Whitespace and comments

- Spaces, tabs, and carriage returns are ignored.
- Newlines are ignored. `;` ends lines.
- `//` starts a comment that runs to the end of the line. There are no block comments.

### Case sensitivity

- **Keywords are case-insensitive:** `declare`, `DECLARE`, and `DeClArE` are all the same.
- **Identifiers are case-sensitive:** `myVar` and `myvar` are different variables.

### Keywords

| Keyword | Used in |
| :-- | :-- |
| `DECLARE` | All declarations |
| `TYPE` | Primitive type aliases |
| `SIZE` | Byte size of a type |
| `STRUCT` | Composite types |
| `THEN` / `END` | Block delimiters |
| `VARIABLE` | Variable declarations |
| `ARRAY` | Array declarations |
| `AS` | "has type" |
| `OF` | Field access, array length query |
| `LENGTH` | Query array length (`LENGTH OF arr`) |
| `PRINT` | Output to stdout |

Any other word is an identifier. Identifiers start with a letter or `_`, then letters,
digits, or `_`.

### Number literals

- **Decimal digits only.** There are no hex (`0x`), octal, or binary literals.
- There are no negative literals. `-5` is the unary minus operator applied to `5`
  (it folds to the same thing at compile time, so the distinction never matters in practice).
- Literals are parsed as 64-bit values; anything larger than `9223372036854775807` is a
  compile error. They are range-checked against their *target type* later.

### Character literals

Single-quoted, one character:

```
DECLARE TYPE byte SIZE 1;
DECLARE VARIABLE c AS byte;

c <- 'A';        // 65
c <- 'é';        // 233 (fits in a byte)
c <- '\n';       // 10 (newline escape)
```

- The character's Unicode code point becomes the literal's numeric value.
- Any character is allowed, but it must fit the target type's signed range.
- Escape sequences: `\n` (newline), `\t` (tab), `\r` (carriage return), `\0` (null),
  `\\` (backslash), `\'` (single quote).

### String literals

Double-quoted sequences of characters:

```
PRINT "Hello World\n";
PRINT "Price: €50";
```

- Encoded as **UTF-8** at compile time.
- String literals are only supported in `PRINT` statements for now.
- Escape sequences: `\n`, `\t`, `\r`, `\0`, `\\`, `\"`.

### Operators

| Operators | Meaning |
| :-- | :-- |
| `<-` | Assignment |
| `+ - * / %` | Arithmetic |
| `<< >>` | Shifts |
| `& ^ \|` | Bitwise AND / XOR / OR |
| `- ~` | Negation / bitwise NOT |
| `( )` | Grouping |

### Declarations

Declarations are processed **in order, in a single pass**. Everything must be declared
before it is used.

### Primitive types (`DECLARE TYPE`)

A named type with a fixed byte size:

```
DECLARE TYPE int   SIZE 4;
DECLARE TYPE byte  SIZE 1;
DECLARE TYPE short SIZE 2;
DECLARE TYPE long  SIZE 8;
```

- Size must be at least 1.
- Type names are unique — you cannot redefine `int` later, even with the same size.
- Sizes above 4 are legal with conditions: such types can be declared, allocated, copied,
  and assigned literals, but they **cannot appear in expressions**.

### Structs (`DECLARE STRUCT`)

```
DECLARE STRUCT Point THEN
    x AS int;
    y AS int;
END;
```

- Fields are laid out **sequentially in memory with no padding**. `Point` is exactly 8 bytes:
  `x` at offset 0, `y` at offset 4.
- Field types must already be declared. Structs can contain other structs:

  ```
  DECLARE STRUCT Rect THEN
      topLeft     AS Point;
      bottomRight AS Point;
  END;
  ```

- Because types must be declared before use, **recursive structs are impossible**.
- Field names must be unique within a struct. The same field name in different structs is fine.
- Struct names and type aliases share one namespace: you can't have a `TYPE Point` and a
  `STRUCT Point`.

### Variables (`DECLARE VARIABLE`)

```
DECLARE VARIABLE counter AS int;
DECLARE VARIABLE origin  AS Point;
```

- Variables live in a single flat stack frame.
- Each variable name may be declared **exactly once**, program-wide. Redeclaring is an error,
  and the error message tells you the line of the original declaration.
- The total frame is capped at **32767 bytes**.

### Arrays (`DECLARE ARRAY`)

```
DECLARE TYPE int SIZE 4;
DECLARE ARRAY grid AS int LENGTH 10;

grid[0] <- 42;
grid[5] <- 99;

DECLARE VARIABLE x AS int;
x <- grid[0];
```

- Arrays are declared with a constant length and live in the stack frame like variables.
- Indices are **0-based**.
- Both constant and variable indices are supported:

  ```
  DECLARE VARIABLE i AS int;
  i <- 5;
  grid[i] <- 100;
  ```

- Constant indices are bounds-checked at compile time:

  ```
  grid[10] <- 42;    // ERROR: index 10 out of bounds for array 'grid' of length 10
  grid[-1] <- 42;    // ERROR: index -1 out of bounds
  ```

- Variable indices are not bounds-checked at runtime (no runtime traps yet).
- Arrays of structs support field access:

  ```
  DECLARE STRUCT Point THEN x AS int; y AS int; END;
  DECLARE ARRAY points AS Point LENGTH 4;

  x OF points[0] <- 10;
  y OF points[0] <- 20;
  ```

- You can query an array's length with `LENGTH OF` (like accessing a field in a struct):

  ```
  DECLARE VARIABLE len AS int;
  len <- LENGTH OF grid;    // len = 10
  ```

  This is a compile-time constant (the literal `10`), so it folds in expressions.

- Whole-array assignment is not allowed:

  ```
  grid <- 42;    // ERROR: cannot assign to whole array 'grid'
  ```

- Arrays cannot be used as values in expressions:

  ```
  x <- grid + 1;    // ERROR: cannot use array 'grid' as a value
  ```

  You must index them.

---

## Memory Model

Understanding this section explains a lot of the language's behavior.

- All variables and arrays are allocated in one **stack frame**, carved out at program start by
  decrementing the stack pointer (`%14`).
- Variables are laid out in **declaration order** at increasing offsets from the frame base,
  with no padding or alignment.
- Arrays occupy `length × elemSize` consecutive bytes, just like a variable of a large type.
- The machine is **big-endian**: for multi-byte values, the most significant byte lives at
  the lowest address.
- Literals assigned into types larger than 4 bytes are **sign-extended** from 32 bits to fill
  the whole value.
- Memory starts **zero-initialized**, so reading a variable before assigning it yields 0.
  Uninitialized reads are legal. Again, SupremeBASIC exposes raw data.

---

## Assignment

### The `<-` operator

Every assignment is a statement ending in `;`:

```
counter <- 42;
counter <- other;            // variable to variable
counter <- a + b * 2;        // expression
grid[3] <- 100;              // array element
x OF p <- 10;                // struct field
```

The right-hand side may be:

1. a literal,
2. a variable reference,
3. a field access,
4. an array element,
5. an expression.

### Literal rules

Literals are untyped and adopt the target's type, subject to a **signed range check**:

| Target size | Allowed literal range |
| :-- | :-- |
| 1 byte | −128 .. 127 |
| 2 bytes | −32768 .. 32767 |
| 3 bytes | −8388608 .. 8388607 |
| 4 bytes | −2147483648 .. 2147483647 |
| > 4 bytes | same as 4 bytes, then sign-extended to fill |

```
DECLARE TYPE byte SIZE 1;
DECLARE VARIABLE b AS byte;
b <- 127;      // ok
b <- 128;      // ERROR: value 128 does not fit in byte (1 byte(s), range -128..127)
b <- -128;     // ok
```

A literal **cannot** be assigned to a struct-typed variable:

```
p <- 5;        // ERROR: cannot assign a literal to the struct 'Point'
```

### Variable-to-variable assignment

Types must match **by name** (nominal typing), not by size:

```
DECLARE TYPE int SIZE 4;
DECLARE TYPE str SIZE 4;
DECLARE VARIABLE x AS int;
DECLARE VARIABLE y AS str;
y <- x;        // ERROR: cannot assign int to str   (both are 4 bytes — doesn't matter)
```

Struct variables copy **by value**.

### Field access: `field OF variable`

Fields are read and written with the `OF` keyword. The field name comes first, then the
variable:

```
x OF p <- 10;              // write field x of variable p
DECLARE VARIABLE myX AS int;
myX <- x OF p;             // read it back
x OF p1 <- y OF p2;        // field-to-field
```

Fields behave exactly like variables in expressions:

```
distSq <- x OF q * x OF q + y OF q * y OF q;
```

Fields can be accessed on array elements:

```
x OF grid[3] <- 42;
```

---

## Output

### The `PRINT` statement

`PRINT` writes raw bytes to stdout. No formatting, no conversion, no newline unless you
include one.

```
PRINT place;
PRINT "literal";
```

**For variables, arrays, and fields:** writes exactly `SIZE` bytes (or `length × elemSize`
for arrays) from memory.

```
DECLARE TYPE int SIZE 4;
DECLARE VARIABLE x AS int;
x <- 42;
PRINT x;    // writes 4 bytes: 00 00 00 2A (big-endian)
```

```
DECLARE TYPE byte SIZE 1;
DECLARE ARRAY msg AS byte LENGTH 5;
msg[0] <- 'H';
msg[1] <- 'i';
msg[2] <- 10;    // newline
PRINT msg;       // writes 5 bytes: H i \n
```

**For string literals:** pushes the UTF-8 bytes onto the stack, writes them, pops.

```
PRINT "Hello\n";    // writes 6 bytes: H e l l o \n
PRINT "€";          // writes 3 bytes (UTF-8 encoding of €)
```

**`PRINT` does not format numbers.** `PRINT x;` where `x` is an `int` writes 4 raw bytes,
not decimal digits. If you want to print `42` as the characters `4` and `2`, you must build
those bytes yourself. Then again since there's no control flow uhh good luck with that :/

---

## Expressions

Expressions appear on the right-hand side of `<-` and nowhere else.

### Operators and precedence

| Level | Operators | Notes |
| :-- | :-- | :-- |
| 1 | `-` `~` | negation, bitwise NOT |
| 2 | `*` `/` `%` | multiply, divide, remainder |
| 3 | `+` `-` | add, subtract |
| 4 | `<<` `>>` | shift left, shift right (**arithmetic**) |
| 5 | `&` | bitwise AND |
| 6 | `^` | bitwise XOR |
| 7 | `\|` | bitwise OR |

This matches C's precedence for these operators.

### Typing rules

1. **Only 1-byte and 4-byte primitives may appear in arithmetic.** Structs and other sizes
   (2, 3, 8, … bytes) are rejected. Struct *fields* are fine as long as the field itself is
   1 or 4 bytes.

   ```
   DECLARE TYPE short SIZE 2;
   DECLARE VARIABLE s AS short;
   s <- s + 1;      // ERROR: cannot do arithmetic on 2-byte type 'short'
   ```

2. **Both typed operands must have the same nominal type.** `int + byte` is an error, and so
   is `int + str` (even though both are 4 bytes).

3. **Literals adopt the other operand's type.** `a + 5` and `5 + a` are both fine. The
   literal must fit that type: `byteVar + 1000` is an error.

4. **The result type equals the operand type**, and it must match the assignment target
   exactly:

   ```
   byteVar <- intA + intB;    // ERROR: cannot assign int to byte
   ```

5. **Unary operators** keep their operand's type: `~byteVar` is a `byte`, `-intVar` is an `int`.

### Constant folding

If both operands of an operator are literals, the compiler **computes the result at compile
time**, during parsing:

```
x <- 2 + 3 * 4;      // identical to:  x <- 14;
x <- ~0 & 255;       // identical to:  x <- 255;
x <- -8 >> 1;        // identical to:  x <- -4;   (arithmetic shift)
```

---

## Quirks

- **Literals are untyped chameleons.** `5` is not an `int`; it becomes whatever it's
  combined with or assigned to.

- **Nominal typing is absolute.** Same size, same layout are still incompatible if the names
  differ. Duck typing is overrated.

- **Bounds checks are compile-time only.** The compiler checks *literals*, but
  runtime arithmetic silently wraps. Ex:

  ```
  DECLARE TYPE byte SIZE 1;
  DECLARE VARIABLE b AS byte;
  b <- 100 << 1;    // stores 200… which reads back as -56
  ```

  compiles and runs without complaint.

- **Arithmetic only exists for 1-byte and 4-byte types.** Any other size can be declared,
  copied, and assigned literals, but cannot do arithmetic. If you need to compute, compute
  in `int` and copy the result.


- **`LENGTH OF arr` is a compile-time constant.** It folds to the literal length, so
  `LENGTH OF grid * 4` becomes `40` if `grid` has length 10.

---

## Under the Hood: Generated SupremeASM

For a program with variables, the generated `.sasm` file has this shape:

```
@version 2

ld #<frameSize>, %5;      // allocate the whole frame at once
sub %14, %5, %14;

// … one block per assignment …

halt;
```

- **Declarations emit no code.** Types, structs, variables, and arrays are resolved entirely
  at compile time into (offset, size) pairs.
- **Every variable access is `%14 + #offset`** — a 16-bit signed offset from the frame base.
  `ldb`/`stb` for 1-byte types (with sign-extension on load), `ldi`/`sti` for 4-byte types,
  and chunked copies for larger values.
- **Array indexing** with a constant index folds into the offset. Variable indices emit
  `mul` to compute the byte offset at runtime.
- **Expressions use Sethi–Ullman register allocation** over `%4–%8`, evaluating the heavier
  subtree first to minimize register pressure.
- **When registers run out, the compiler spills to the stack**: `%9` becomes a spill pointer
  (`mov %14, %9`), and intermediate values are temporarily stored just below the frame with
  balanced `dec`/`inc` pairs.
- **String literals in `PRINT`** are staged on the stack: `sub %14, %5, %14` to allocate,
  `stb` for each byte, `syscall`, then `add %14, %5, %14` to deallocate. The compiler
  tracks stack depth so variable offsets stay correct.
- **Unary minus compiles to `not` + `inc`** (two's-complement negation).
- **Programs always end with `halt`.**

---

## Roadmap

Planned features, roughly in order:

1. **Smart optimization during compilation** — the main long-term goal of the project.
2. **Read files** — will require extending the VM's syscall surface.
3. **Control flow** — `IF`/`WHILE`/`FOR` with the `THEN ... END;` block syntax.
4. **Functions** — `DECLARE FUNCTION` with parameters and return values.
5. **Pointers** — typed pointers, `ADDRESS OF`, dereference.
6. Whatever comes after that.

---

## Appendix A: Full Grammar

```
program         := statement* EOF

statement       := typeDecl | structDecl | varDecl | arrayDecl | assign | printStmt

typeDecl        := DECLARE TYPE IDENTIFIER SIZE NUMBER ;
structDecl      := DECLARE STRUCT IDENTIFIER THEN fieldDecl* END ;
fieldDecl       := IDENTIFIER AS IDENTIFIER ;
varDecl         := DECLARE VARIABLE IDENTIFIER AS IDENTIFIER ;
arrayDecl       := DECLARE ARRAY IDENTIFIER AS IDENTIFIER LENGTH NUMBER ;

assign          := access <- expression ;
printStmt       := PRINT (access | STRING) ;

access          := IDENTIFIER (OF access)? ( [ expression ] )*

expression      := bitOr
bitOr           := bitXor  ( |  bitXor )*
bitXor          := bitAnd  ( ^  bitAnd )*
bitAnd          := shift   ( &  shift )*
shift           := additive( ( << | >> ) additive )*
additive        := term    ( ( + | - ) term )*
term            := unary   ( ( * | / | % ) unary )*
unary           := ( - | ~ ) unary | atom
atom            := NUMBER
                 | CHAR
                 | STRING
                 | access
                 | LENGTH OF IDENTIFIER
                 | ( expression )
```

The `access` rule handles variables, field access (`x OF p`), array indexing (`arr[i]`), and
combinations (`x OF arr[i]`). `OF` is right-associative.

---

## Appendix B: Compiler Error Index

**Parse errors** (reported as `Error parsing program: …`):

| Message | Cause |
| :-- | :-- |
| `expected <TOKEN-TYPE> but found …` | Wrong token where a specific kind was required |
| `expected ';' but found …` | Missing semicolon |
| `expected ')' but found …` | Unclosed parenthesis |
| `expected DECLARE, PRINT, or a variable name, found …` | Line doesn't start a statement |
| `expected VARIABLE, TYPE, STRUCT, or ARRAY after DECLARE` | Unknown declaration kind |
| `expected a number, variable, or '('` | Malformed expression (e.g. `x <- a + ;`) |
| `number out of range: N` | Literal exceeds 64-bit |
| `size out of range: N` | `DECLARE TYPE … SIZE` isn't an integer |
| `length out of range: N` | `DECLARE ARRAY … LENGTH` isn't an integer |
| `division by zero in constant expression` | `5 / 0` or `5 % 0` folded at compile time |
| `constant expression out of range` | Literal arithmetic overflowed during folding |
| `shift count N outside 0..31` | Literal shift count out of range |
| `unterminated character literal` | `'a` with no closing quote |
| `unterminated string literal` | `"hello` with no closing quote |
| `unterminated escape sequence` | `\` at end of string |
| `unknown escape sequence '\X'` | Invalid escape like `\q` |

**Semantic errors** (reported as `Error in semantic analysis: …`):

| Message | Cause |
| :-- | :-- |
| `type size must be at least 1` | `DECLARE TYPE t SIZE 0;` |
| `type 'X' already defined` | Duplicate type or struct name |
| `unknown type 'X'` | Variable declared with an undeclared type |
| `unknown type 'X' in struct 'S'` | Struct field uses an undeclared type |
| `duplicate field 'f' in struct 'S'` | Same field name twice in one struct |
| `variable 'v' already declared on line N` | Redeclaration (points at the first one) |
| `program exceeds the 32767-byte stack frame` | Total variable size too large |
| `undeclared variable 'v'` | Use before declaration |
| `'v' is not a struct` | `OF` used on a non-struct variable |
| `struct 'S' has no field 'f'` | Unknown field name |
| `cannot assign a literal to the struct 'S'` | Literal RHS into a struct target |
| `value V does not fit in T (N byte(s), range A..B)` | Literal out of target range |
| `cannot assign A to B` | Nominal type mismatch |
| `cannot do arithmetic on struct 'S'` | Struct used in an expression |
| `cannot do arithmetic on N-byte type 'T'` | Non-1/4-byte type in an expression |
| `shift count N outside 0..31` | Literal shift count out of range (sema pass) |
| `array length must be at least 1` | `DECLARE ARRAY … LENGTH 0;` |
| `'v' is not an array` | Array indexing on a non-array |
| `array index N out of bounds for array 'A' of length L` | Constant index out of range |
| `array index must be a 1- or 4-byte integer type` | Index has wrong type |
| `cannot assign to whole array 'A'` | `arr <- 42;` instead of `arr[i] <- 42;` |
| `cannot use array 'A' as a value` | Array in expression without indexing |
| `LEN can only be applied to arrays` | `LENGTH OF x` where `x` is not an array |
| `empty string literal` | `PRINT "";` |
| `string literals are only supported in PRINT for now` | `"hello"` outside PRINT |