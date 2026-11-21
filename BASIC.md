# SupremeBASIC Language Specification (v0.1)

## 1. Core Philosophy
* **"DECLARE Everything":** All language constructs (variables, types, structs, and eventually functions/blocks) use the `DECLARE` keyword or consistent block syntax.
* **Block Syntax:** Multi-line blocks are unified using `THEN ... END;` (e.g., structs, eventually IF/FOR/WHILE).
* **Literals:** Literals (like `5`) are untyped mathematical values. They adopt the size and type of the target variable they are assigned to as long as the target variable is big enough to hold it and is a primitive type (not a struct or something else).
* **Nominal Typing:** Types are identified by name, not just size. A 4-byte `int` cannot be assigned to a 4-byte `str`.
* **Signed-Only Math (Currently):** All bounds checking assumes signed two's-complement integers. (No `ubyte`/unsigned types yet).

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
Struct fields must use types declared earlier in the file.

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
* **Literal Bounds Checking:** Literals are checked to ensure they fit within the target's signed byte range (e.g., one byte holds -128 to 127).
```
myVar <- 42;
```
## 4. Structs & Field Access
SupremeBASIC uses `OF` keyword for field access.

```
x OF p <- 10;
DECLARE VARIABLE myX AS int;
myX <- x OF p;
// copy by value
x OF p1 <- y OF p2;
// copy by value
p2 <- p1; 
```

## 5. Looking Ahead
### Features are planned to be added in the following order:
* Unsigned Types
* Math/Expressions
* Print through stdout
* Read files (will require updating the VM itself)
* Control flow
* We shall see from here...
