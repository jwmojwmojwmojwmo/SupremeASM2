# SupremeBASIC Language Specification (v0.2)

## 1. Core Philosophy
* **"DECLARE Everything":** All language constructs (variables, types, structs, and eventually functions/blocks) use the `DECLARE` keyword or consistent block syntax.
* **Block Syntax:** Multi-line blocks are unified using `THEN ... END;` (e.g., structs, eventually IF/FOR/WHILE).
* **Literals:** Literals (like `5`) are untyped mathematical values. They adopt the size and type of the target variable they are assigned to as long as the target variable is big enough to hold it and is a primitive type (not a struct or something else).
* **Nominal Typing:** Types are identified by name, not just size. A 4-byte `int` cannot be assigned to a 4-byte `str`.
* **Signed-Only Math (Currently):** All bounds checking assumes signed two's-complement integers. (No `ubyte`/unsigned types yet).
* **Basic Syntax:** Every statement ends with `;`. Comments start with `//` and run to the end of the line. Keywords are case-insensitive (`declare` = `DECLARE`), but identifiers (variable, type, struct and field names) are case-sensitive.

## 2. Type System
Types must be declared before use. There are two ways to define a type:

### Primitive Aliases
Defines a named type with a specific byte size.
```
DECLARE TYPE int SIZE 4;
DECLARE TYPE byte SIZE 1;
DECLARE TYPE str SIZE 4;
```

### Composite Types (Structs)
Defines a struct containing fields. Fields are laid out sequentially in memory (no padding).
```
DECLARE STRUCT Point THEN
    x AS int;
    y AS int;
END;
```
Types and structs must be declared before use.

## 3. Variables
Variables are allocated on the stack using a bump allocator. They must be explicitly typed.

### Declaration
```
DECLARE VARIABLE myVar AS int;
DECLARE VARIABLE p AS Point;
```

### Assignment
The `<-` operator is used for assignment. 
* **Strict Size/Type Matching:** You can only assign a variable to another variable if their types match exactly.
* **Literal Bounds Checking:** Literals are checked to ensure they fit within the target's signed byte range (e.g., one byte holds -128 to 127). For targets bigger than 4 bytes, the range is still signed 32-bit, and the value is sign-extended to fill the rest.
* **Expressions:** The right-hand side can also be an expression (see Section 5).
```
myVar <- 42;
```

Variables are limited by a 32767-byte stack frame, and every variable name can only be declared once.
## 4. Structs & Field Access
SupremeBASIC uses `OF` keyword for field access.

```
DECLARE STRUCT Point THEN
    x AS int;
    y AS int;
END;
DECLARE VARIABLE p AS Point;
x OF p <- 10;
DECLARE VARIABLE myX AS int;
myX <- x OF p;
// copy by value
x OF p1 <- y OF p2;
// copy by value
p2 <- p1; 
```

## 5. Expressions
The right side of `<-` (and anything inside parentheses) can be an expression built from numbers, variables, and field accesses.
```
DECLARE TYPE int SIZE 4;
DECLARE VARIABLE a AS int;
DECLARE VARIABLE b AS int;
a <- 6;
b <- (a + 2) * 3 - a % 4;
b <- ~a & 255;
b <- -a << 2;
```

### Operators
Listed from highest to lowest precedence. All binary operators are left-associative, and parentheses override precedence.

| Level | Operators | Notes |
| :--- | :--- | :--- |
| 1 | `-` `~` (unary) | negate, bitwise NOT |
| 2 | `*` `/` `%` | multiply, divide, remainder |
| 3 | `+` `-` | add, subtract |
| 4 | `<<` `>>` | shift left, shift right (arithmetic) |
| 5 | `&` | bitwise AND |
| 6 | `^` | bitwise XOR |
| 7 | `\|` | bitwise OR |

This is the same ordering C uses for these operators.

### Typing Rules
* **Operands must be 1-byte or 4-byte primitives.** Structs and other sizes (2, 8 bytes, etc.) can't be used in arithmetic. Struct fields work as long as the field itself is a valid operand type.
* **Both operands must have the same type** (nominal typing still applies): `byte + int` is an error, and so is `int + str` even though both are 4 bytes.
* **Literals adopt the type of the other operand**, so `a + 5` and `5 + a` are both fine. The literal has to fit in that type, so `byteVar + 1000` is an error. (The right side of a shift is a bit count instead, see below.)
* **The result type is the operand type**, and it must match the target of the assignment exactly. `byteVar <- intA + intB` is an error.
* **Shift counts** must be between 0 and 31 when they're literals.

### Constant Folding
If both sides of an operator are literals, the compiler computes the answer at compile time, so `x <- 2 + 3 * 4;` is the same as `x <- 14;`. The folded value is then bounds-checked like any other literal, so `1 << 31` is rejected for an `int` and `100 + 100` is rejected for a `byte`. Division or remainder by a literal zero, and constants that overflow, are compile errors.

## 6. Using the Compiler
Run `basic/src/SupremeBasicCompiler.java` from the repo root and enter the path to a `.sbas` file. The compiler writes the generated SupremeASM to `<name>.sasm` in the current folder, then asks whether to assemble and run it (`y`), or to also dump memory (`ym`), registers (`yr`), or both (`ymr`). Tests live in `basic/src/test` (JUnit).

## 7. Looking Ahead
### Features are planned to be added in the following order:
* Print through stdout
* Smart optimization during compilation (main goal for this project in general)
* Read files (will require updating the VM itself)
* Control flow
* We shall see from here...