package basic.src.test;

import org.junit.jupiter.api.Test;

public class SemaTest extends CompilerTestBase {

    @Test
    void testAll() {
        String code = """
                DECLARE TYPE int SIZE 4;
                DECLARE TYPE byte SIZE 1;
                DECLARE TYPE short SIZE 2;

                DECLARE STRUCT Vec2 THEN
                    x AS int;
                    y AS int;
                END;

                DECLARE STRUCT Color THEN
                    r AS byte;
                    g AS byte;
                    b AS byte;
                    a AS byte;
                END;

                DECLARE VARIABLE playerPos AS Vec2;
                DECLARE VARIABLE enemyPos AS Vec2;
                DECLARE VARIABLE playerCol AS Color;
                DECLARE VARIABLE enemyCol AS Color;
                DECLARE VARIABLE tempX AS int;
                DECLARE VARIABLE tempR AS byte;

                x OF playerPos <- 100;
                y OF playerPos <- 200;
                r OF playerCol <- 127;
                g OF playerCol <- 0;
                b OF playerCol <- 50;
                a OF playerCol <- 100;

                enemyPos <- playerPos;
                enemyCol <- playerCol;

                tempX <- x OF enemyPos;
                tempR <- r OF enemyCol;

                x OF enemyPos <- y OF playerPos;
                r OF enemyCol <- b OF playerCol;
                """;
        assertCompiles(code);
    }

    // ---- valid structs / fields ----

    @Test
    void testValidStructAssignment() {
        String code = """
                DECLARE TYPE int SIZE 4;
                DECLARE STRUCT Point THEN x AS int; y AS int; END;
                DECLARE VARIABLE p1 AS Point;
                DECLARE VARIABLE p2 AS Point;
                p2 <- p1;
                """;
        assertCompiles(code);
    }

    @Test
    void testValidFieldReadWrite() {
        String code = """
                DECLARE TYPE int SIZE 4;
                DECLARE STRUCT Point THEN x AS int; END;
                DECLARE VARIABLE p AS Point;
                DECLARE VARIABLE myX AS int;
                x OF p <- 10;
                myX <- x OF p;
                """;
        assertCompiles(code);
    }

    @Test
    void testFieldToFieldAssignment() {
        String code = """
                DECLARE TYPE int SIZE 4;
                DECLARE STRUCT P THEN x AS int; y AS int; END;
                DECLARE VARIABLE p1 AS P;
                DECLARE VARIABLE p2 AS P;
                x OF p1 <- 10;
                y OF p2 <- x OF p1;
                """;
        assertCompiles(code);
    }

    // ---- literals & bounds ----

    @Test
    void testByteLiteralBounds() {
        String code = """
                DECLARE TYPE byte SIZE 1;
                DECLARE VARIABLE b AS byte;
                b <- 127;
                """;
        assertCompiles(code);
    }

    @Test
    void testLiteralOverflow() {
        String code = """
                DECLARE TYPE byte SIZE 1;
                DECLARE VARIABLE b AS byte;
                b <- 300;
                """;
        assertFails(code, "does not fit in byte");
    }

    @Test
    void testNegativeLiteralAssignment() {
        String code = """
                DECLARE TYPE int SIZE 4;
                DECLARE VARIABLE x AS int;
                x <- -42;
                """;
        assertCompiles(code);
    }

    @Test
    void testNegativeLiteralOverflow() {
        String code = """
                DECLARE TYPE byte SIZE 1;
                DECLARE VARIABLE b AS byte;
                b <- -200;
                """;
        assertFails(code, "does not fit in byte");
    }

    @Test
    void testNegativeLiteralInStruct() {
        String code = """
                DECLARE TYPE int SIZE 4;
                DECLARE STRUCT Point THEN x AS int; y AS int; END;
                DECLARE VARIABLE p AS Point;
                x OF p <- -100;
                """;
        assertCompiles(code);
    }

    @Test
    void testFoldThenBoundsCheck() {
        String code = """
                DECLARE TYPE byte SIZE 1;
                DECLARE VARIABLE b AS byte;
                b <- 100 + 100;
                """;
        assertFails(code, "does not fit in byte");
    }

    @Test
    void testLiteralToWholeStructRejected() {
        String code = """
                DECLARE TYPE int SIZE 4;
                DECLARE STRUCT P THEN x AS int; END;
                DECLARE VARIABLE p AS P;
                p <- 5;
                """;
        assertFails(code, "cannot assign a literal to the struct");
    }

    // ---- typing ----

    @Test
    void testNominalTypingPreventsMismatch() {
        String code = """
                DECLARE TYPE int SIZE 4;
                DECLARE TYPE str SIZE 4;
                DECLARE VARIABLE x AS int;
                DECLARE VARIABLE y AS str;
                y <- x;
                """;
        assertFails(code, "cannot assign int to str");
    }

    @Test
    void testUndeclaredVariable() {
        String code = """
                DECLARE TYPE int SIZE 4;
                DECLARE VARIABLE x AS int;
                x <- y;
                """;
        assertFails(code, "undeclared variable 'y'");
    }

    @Test
    void testAssigningToNonStruct() {
        String code = """
                DECLARE TYPE int SIZE 4;
                DECLARE VARIABLE x AS int;
                DECLARE VARIABLE y AS int;
                y OF x <- 5;
                """;
        assertFails(code, "'x' is not a struct");
    }

    @Test
    void testReadingFromNonStruct() {
        String code = """
                DECLARE TYPE int SIZE 4;
                DECLARE VARIABLE x AS int;
                DECLARE VARIABLE y AS int;
                y <- z OF x;
                """;
        assertFails(code, "'x' is not a struct");
    }

    // ---- declarations ----

    @Test
    void testDuplicateTypeDeclaration() {
        String code = """
                DECLARE TYPE int SIZE 4;
                DECLARE TYPE int SIZE 8;
                """;
        assertFails(code, "type 'int' already defined");
    }

    @Test
    void testDuplicateVariableDeclaration() {
        String code = """
                DECLARE TYPE int SIZE 4;
                DECLARE VARIABLE x AS int;
                DECLARE VARIABLE x AS int;
                """;
        assertFails(code, "variable 'x' already declared");
    }

    @Test
    void testDuplicateStructField() {
        String code = """
                DECLARE TYPE int SIZE 4;
                DECLARE STRUCT P THEN x AS int; x AS int; END;
                """;
        assertFails(code, "duplicate field 'x'");
    }

    @Test
    void testUnknownTypeInVariable() {
        String code = """
                DECLARE VARIABLE x AS float;
                """;
        assertFails(code, "unknown type 'float'");
    }

    @Test
    void testTypeSizeZero() {
        String code = """
                DECLARE TYPE empty SIZE 0;
                """;
        assertFails(code, "type size must be at least 1");
    }

    @Test
    void testFrameSizeExceeded() {
        String code = """
                DECLARE TYPE huge SIZE 32768;
                DECLARE VARIABLE x AS huge;
                """;
        assertFails(code, "program exceeds the 32767-byte stack frame");
    }

    // ---- arithmetic: valid ----

    @Test
    void testArithmeticAllOperators() {
        String code = """
                DECLARE TYPE int SIZE 4;
                DECLARE VARIABLE a AS int;
                DECLARE VARIABLE b AS int;
                DECLARE VARIABLE x AS int;
                x <- a + b;
                x <- a - b;
                x <- a * b;
                x <- a / b;
                x <- a % b;
                """;
        assertCompiles(code);
    }

    @Test
    void testArithmeticPrecedence() {
        String code = """
                DECLARE TYPE int SIZE 4;
                DECLARE VARIABLE a AS int;
                DECLARE VARIABLE b AS int;
                DECLARE VARIABLE c AS int;
                DECLARE VARIABLE x AS int;
                x <- a + b * c;
                """;
        assertCompiles(code);
    }

    @Test
    void testArithmeticParentheses() {
        String code = """
                DECLARE TYPE int SIZE 4;
                DECLARE VARIABLE a AS int;
                DECLARE VARIABLE b AS int;
                DECLARE VARIABLE c AS int;
                DECLARE VARIABLE x AS int;
                x <- (a + b) * c;
                x <- ((a + b) * (c - a));
                """;
        assertCompiles(code);
    }

    @Test
    void testArithmeticLiteralAdoption() {
        String code = """
                DECLARE TYPE int SIZE 4;
                DECLARE VARIABLE a AS int;
                DECLARE VARIABLE x AS int;
                x <- a + 5;
                x <- 5 + a;
                x <- a * 2 - 1;
                """;
        assertCompiles(code);
    }

    @Test
    void testArithmeticOnByte() {
        String code = """
                DECLARE TYPE byte SIZE 1;
                DECLARE VARIABLE a AS byte;
                DECLARE VARIABLE b AS byte;
                a <- b + 1;
                """;
        assertCompiles(code);
    }

    @Test
    void testArithmeticFieldOperand() {
        String code = """
                DECLARE TYPE int SIZE 4;
                DECLARE STRUCT P THEN x AS int; END;
                DECLARE VARIABLE p AS P;
                DECLARE VARIABLE result AS int;
                result <- x OF p + 5;
                """;
        assertCompiles(code);
    }

    @Test
    void testArithmeticFieldTarget() {
        String code = """
                DECLARE TYPE int SIZE 4;
                DECLARE STRUCT P THEN x AS int; END;
                DECLARE VARIABLE p AS P;
                DECLARE VARIABLE a AS int;
                DECLARE VARIABLE b AS int;
                x OF p <- a + b;
                """;
        assertCompiles(code);
    }

    @Test
    void testFoldLiteralMathOnWideType() {
        String code = """
                DECLARE TYPE long SIZE 8;
                DECLARE VARIABLE x AS long;
                x <- 2 + 3;
                """;
        assertCompiles(code);
    }

    // ---- arithmetic: failures ----

    @Test
    void testArithmeticOperandTypeMismatch() {
        String code = """
                DECLARE TYPE int SIZE 4;
                DECLARE TYPE byte SIZE 1;
                DECLARE VARIABLE a AS int;
                DECLARE VARIABLE b AS byte;
                DECLARE VARIABLE x AS int;
                x <- a + b;
                """;
        assertFails(code, "cannot assign int to byte");
    }

    @Test
    void testArithmeticResultTypeMismatch() {
        String code = """
                DECLARE TYPE int SIZE 4;
                DECLARE TYPE byte SIZE 1;
                DECLARE VARIABLE i AS int;
                DECLARE VARIABLE b AS byte;
                b <- i + 1;
                """;
        assertFails(code, "cannot assign int to byte");
    }

    @Test
    void testArithmeticOn2ByteRejected() {
        String code = """
                DECLARE TYPE short SIZE 2;
                DECLARE VARIABLE a AS short;
                a <- a + 1;
                """;
        assertFails(code, "cannot do arithmetic");
    }

    @Test
    void testArithmeticOn8ByteRejected() {
        String code = """
                DECLARE TYPE long SIZE 8;
                DECLARE VARIABLE a AS long;
                DECLARE VARIABLE b AS long;
                a <- b + 1;
                """;
        assertFails(code, "cannot do arithmetic");
    }

    @Test
    void testArithmeticUndeclaredOperand() {
        String code = """
                DECLARE TYPE int SIZE 4;
                DECLARE VARIABLE x AS int;
                x <- z + 1;
                """;
        assertFails(code, "undeclared variable 'z'");
    }

    // ---- bitwise: valid ----

    @Test
    void testShiftBothDirections() {
        String code = """
                DECLARE TYPE int SIZE 4;
                DECLARE VARIABLE a AS int;
                DECLARE VARIABLE b AS int;
                DECLARE VARIABLE x AS int;
                x <- a << b;
                x <- a >> b;
                """;
        assertCompiles(code);
    }

    @Test
    void testBitwiseAllOperators() {
        String code = """
                DECLARE TYPE int SIZE 4;
                DECLARE VARIABLE a AS int;
                DECLARE VARIABLE b AS int;
                DECLARE VARIABLE x AS int;
                x <- a & b;
                x <- a | b;
                x <- a ^ b;
                """;
        assertCompiles(code);
    }

    @Test
    void testShiftOnByte() {
        String code = """
                DECLARE TYPE byte SIZE 1;
                DECLARE VARIABLE b AS byte;
                b <- b << 1;
                b <- b >> 1;
                """;
        assertCompiles(code);
    }

    @Test
    void testShiftFoldedValueOverflow() {
        // constants must fit at compile time, same rule as 2147483647 + 1
        String code = """
                DECLARE TYPE int SIZE 4;
                DECLARE VARIABLE x AS int;
                x <- 1 << 31;
                """;
        assertFails(code, "does not fit in int");
    }

    @Test
    void testBitwiseFoldThenBoundsCheck() {
        String code = """
                DECLARE TYPE byte SIZE 1;
                DECLARE VARIABLE b AS byte;
                b <- 15 | 240;
                """;
        assertFails(code, "does not fit in byte");
    }

    // ---- unary: valid ----
    @Test
    void testNotOnInt() {
        String code = """
                DECLARE TYPE int SIZE 4;
                DECLARE VARIABLE a AS int;
                DECLARE VARIABLE x AS int;
                x <- ~a;
                """;
        assertCompiles(code);
    }

    @Test
    void testNotOnByte() {
        String code = """
                DECLARE TYPE byte SIZE 1;
                DECLARE VARIABLE b AS byte;
                b <- ~b;
                """;
        assertCompiles(code);
    }

    // ---- unary: failures ----
    @Test
    void testNotFoldedOverflowByte() {
        String code = """
                DECLARE TYPE byte SIZE 1;
                DECLARE VARIABLE b AS byte;
                b <- ~128;
                """;
        assertFails(code, "does not fit in byte"); // -129
    }

    // ---- arrays: valid ----

    @Test
    void testArrayDeclaration() {
        String code = """
                DECLARE TYPE int SIZE 4;
                DECLARE ARRAY grid AS int LEN 10;
                """;
        assertCompiles(code);
    }

    @Test
    void testArrayLiteralIndex() {
        String code = """
                DECLARE TYPE int SIZE 4;
                DECLARE ARRAY grid AS int LEN 10;
                grid[0] <- 42;
                grid[9] <- 99;
                """;
        assertCompiles(code);
    }

    @Test
    void testArrayVariableIndex() {
        String code = """
                DECLARE TYPE int SIZE 4;
                DECLARE ARRAY grid AS int LEN 10;
                DECLARE VARIABLE i AS int;
                i <- 5;
                grid[i] <- 42;
                """;
        assertCompiles(code);
    }

    @Test
    void testArrayFieldAccess() {
        String code = """
                DECLARE TYPE int SIZE 4;
                DECLARE STRUCT Point THEN x AS int; y AS int; END;
                DECLARE ARRAY grid AS Point LEN 4;
                x OF grid[0] <- 10;
                y OF grid[1] <- 20;
                """;
        assertCompiles(code);
    }

    @Test
    void testArrayFieldAccessVariableIndex() {
        String code = """
                DECLARE TYPE int SIZE 4;
                DECLARE STRUCT Point THEN x AS int; y AS int; END;
                DECLARE ARRAY grid AS Point LEN 4;
                DECLARE VARIABLE i AS int;
                i <- 2;
                x OF grid[i] <- 99;
                """;
        assertCompiles(code);
    }

    @Test
    void testArrayOfArrays() {
        String code = """
                DECLARE TYPE int SIZE 4;
                DECLARE ARRAY matrix AS int LEN 16;
                matrix[0] <- 42;
                matrix[15] <- 99;
                """;
        assertCompiles(code);
    }

    @Test
    void testArrayRead() {
        String code = """
                DECLARE TYPE int SIZE 4;
                DECLARE ARRAY grid AS int LEN 10;
                DECLARE VARIABLE x AS int;
                grid[5] <- 42;
                x <- grid[5];
                """;
        assertCompiles(code);
    }

    @Test
    void testArrayFieldRead() {
        String code = """
                DECLARE TYPE int SIZE 4;
                DECLARE STRUCT Point THEN x AS int; END;
                DECLARE ARRAY grid AS Point LEN 4;
                DECLARE VARIABLE myX AS int;
                x OF grid[0] <- 10;
                myX <- x OF grid[0];
                """;
        assertCompiles(code);
    }

    @Test
    void testLenOfArray() {
        String code = """
                DECLARE TYPE int SIZE 4;
                DECLARE ARRAY grid AS int LEN 42;
                DECLARE VARIABLE len AS int;
                len <- LEN OF grid;
                """;
        assertCompiles(code);
    }

    @Test
    void testLenInExpression() {
        String code = """
                DECLARE TYPE int SIZE 4;
                DECLARE ARRAY grid AS int LEN 10;
                DECLARE VARIABLE total AS int;
                total <- LEN OF grid * 4;
                """;
        assertCompiles(code);
    }

    @Test
    void testArrayCopyElementToElement() {
        String code = """
                DECLARE TYPE int SIZE 4;
                DECLARE ARRAY src AS int LEN 10;
                DECLARE ARRAY dst AS int LEN 10;
                src[0] <- 42;
                dst[1] <- src[0];
                """;
        assertCompiles(code);
    }

    @Test
    void testArrayFieldToField() {
        String code = """
                DECLARE TYPE int SIZE 4;
                DECLARE STRUCT Point THEN x AS int; y AS int; END;
                DECLARE ARRAY grid AS Point LEN 4;
                x OF grid[0] <- 10;
                y OF grid[1] <- x OF grid[0];
                """;
        assertCompiles(code);
    }

    // ---- arrays: failures ----

    @Test
    void testArrayOutOfBounds() {
        String code = """
                DECLARE TYPE int SIZE 4;
                DECLARE ARRAY grid AS int LEN 10;
                grid[10] <- 42;
                """;
        assertFails(code, "array index 10 out of bounds");
    }

    @Test
    void testArrayNegativeIndex() {
        String code = """
                DECLARE TYPE int SIZE 4;
                DECLARE ARRAY grid AS int LEN 10;
                grid[-1] <- 42;
                """;
        assertFails(code, "array index -1 out of bounds");
    }

    @Test
    void testArrayUnknownType() {
        String code = """
                DECLARE ARRAY grid AS unknown LEN 10;
                """;
        assertFails(code, "unknown type 'unknown'");
    }

    @Test
    void testArrayZeroLength() {
        String code = """
                DECLARE TYPE int SIZE 4;
                DECLARE ARRAY grid AS int LEN 0;
                """;
        assertFails(code, "array length must be at least 1");
    }

    @Test
    void testArrayFrameOverflow() {
        String code = """
                DECLARE TYPE huge SIZE 8192;
                DECLARE ARRAY grid AS huge LEN 5;
                """;
        assertFails(code, "program exceeds the 32767-byte stack frame");
    }

    @Test
    void testArrayIndexNonInteger() {
        String code = """
                DECLARE TYPE int SIZE 4;
                DECLARE STRUCT Point THEN x AS int; END;
                DECLARE ARRAY grid AS int LEN 10;
                DECLARE VARIABLE p AS Point;
                grid[p] <- 42;
                """;
        assertFails(code, "array index must be a 1- or 4-byte integer type");
    }

    @Test
    void testArrayAccessNonArray() {
        String code = """
                DECLARE TYPE int SIZE 4;
                DECLARE VARIABLE x AS int;
                x[0] <- 42;
                """;
        assertFails(code, "'x' is not an array");
    }

    @Test
    void testArrayFieldAccessNonArray() {
        String code = """
                DECLARE TYPE int SIZE 4;
                DECLARE VARIABLE x AS int;
                field OF x[0] <- 42;
                """;
        assertFails(code, "'x' is not an array");
    }

    @Test
    void testLenOfNonArray() {
        String code = """
                DECLARE TYPE int SIZE 4;
                DECLARE VARIABLE x AS int;
                DECLARE VARIABLE len AS int;
                len <- LEN OF x;
                """;
        assertFails(code, "LEN can only be applied to arrays");
    }

    @Test
    void testArrayArithmeticForbidden() {
        String code = """
                DECLARE TYPE int SIZE 4;
                DECLARE ARRAY grid AS int LEN 10;
                DECLARE VARIABLE x AS int;
                x <- grid + 1;
                """;
        assertFails(code, "cannot use array 'grid' directly in expression");
    }

    @Test
    void testArrayAssignWholeArray() {
        String code = """
                DECLARE TYPE int SIZE 4;
                DECLARE ARRAY grid AS int LEN 10;
                grid <- 42;
                """;
        assertFails(code, "cannot assign to whole array");
    }
}