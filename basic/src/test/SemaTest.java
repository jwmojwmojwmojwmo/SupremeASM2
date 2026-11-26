package basic.src.test;

import basic.src.main.compiler.SupremeCompiler;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.PrintStream;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.*;

public class SemaTest extends CompilerTestBase {

    private static void assertCompiles(String code) throws Exception {
        run(code);
    }

    private static void assertFails(String code, String expectedFragment) throws IOException {
        Path dir = Files.createTempDirectory("sbas");
        String name = "t" + System.nanoTime();

        PrintStream oldErr = System.err;
        ByteArrayOutputStream errCapture = new ByteArrayOutputStream();
        try {
            System.setErr(new PrintStream(errCapture));
            SupremeCompiler compiler = new SupremeCompiler(1, false, false);
            boolean success = compiler.compile(code, dir.resolve(name).toString());
            assertFalse(success, "expected compilation to fail but it succeeded");
        } finally {
            System.setErr(oldErr);
        }
        String captured = errCapture.toString();
        assertTrue(captured.contains(expectedFragment),
                "expected error containing \"" + expectedFragment + "\" but got:\n" + captured);
    }

    @Test
    void testAll() throws Exception {
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

    @Test
    void testValidStructAssignment() throws Exception {
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
    void testValidFieldReadWrite() throws Exception {
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
    void testFieldToFieldAssignment() throws Exception {
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

    @Test
    void testByteLiteralBounds() throws Exception {
        String code = """
                DECLARE TYPE byte SIZE 1;
                DECLARE VARIABLE b AS byte;
                b <- 127;
                """;
        assertCompiles(code);
    }

    @Test
    void testLiteralOverflow() throws IOException {
        String code = """
                DECLARE TYPE byte SIZE 1;
                DECLARE VARIABLE b AS byte;
                b <- 300;
                """;
        assertFails(code, "does not fit in byte");
    }

    @Test
    void testNegativeLiteralAssignment() throws Exception {
        String code = """
                DECLARE TYPE int SIZE 4;
                DECLARE VARIABLE x AS int;
                x <- -42;
                """;
        assertCompiles(code);
    }

    @Test
    void testNegativeLiteralOverflow() throws IOException {
        String code = """
                DECLARE TYPE byte SIZE 1;
                DECLARE VARIABLE b AS byte;
                b <- -200;
                """;
        assertFails(code, "does not fit in byte");
    }

    @Test
    void testNegativeLiteralInStruct() throws Exception {
        String code = """
                DECLARE TYPE int SIZE 4;
                DECLARE STRUCT Point THEN x AS int; y AS int; END;
                DECLARE VARIABLE p AS Point;
                x OF p <- -100;
                """;
        assertCompiles(code);
    }

    @Test
    void testFoldThenBoundsCheck() throws IOException {
        String code = """
                DECLARE TYPE byte SIZE 1;
                DECLARE VARIABLE b AS byte;
                b <- 100 + 100;
                """;
        assertFails(code, "does not fit in byte");
    }

    @Test
    void testLiteralToWholeStructRejected() throws IOException {
        String code = """
                DECLARE TYPE int SIZE 4;
                DECLARE STRUCT P THEN x AS int; END;
                DECLARE VARIABLE p AS P;
                p <- 5;
                """;
        assertFails(code, "cannot assign a literal to the struct");
    }

    @Test
    void testNominalTypingPreventsMismatch() throws IOException {
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
    void testUndeclaredVariable() throws IOException {
        String code = """
                DECLARE TYPE int SIZE 4;
                DECLARE VARIABLE x AS int;
                x <- y;
                """;
        assertFails(code, "undeclared variable 'y'");
    }

    @Test
    void testAssigningToNonStruct() throws IOException {
        String code = """
                DECLARE TYPE int SIZE 4;
                DECLARE VARIABLE x AS int;
                DECLARE VARIABLE y AS int;
                y OF x <- 5;
                """;
        assertFails(code, "is not a struct");
    }

    @Test
    void testReadingFromNonStruct() throws IOException {
        String code = """
                DECLARE TYPE int SIZE 4;
                DECLARE VARIABLE x AS int;
                DECLARE VARIABLE y AS int;
                y <- z OF x;
                """;
        assertFails(code, "is not a struct");
    }

    @Test
    void testDuplicateTypeDeclaration() throws IOException {
        String code = """
                DECLARE TYPE int SIZE 4;
                DECLARE TYPE int SIZE 8;
                """;
        assertFails(code, "type 'int' already defined");
    }

    @Test
    void testDuplicateVariableDeclaration() throws IOException {
        String code = """
                DECLARE TYPE int SIZE 4;
                DECLARE VARIABLE x AS int;
                DECLARE VARIABLE x AS int;
                """;
        assertFails(code, "variable 'x' already declared");
    }

    @Test
    void testDuplicateStructField() throws IOException {
        String code = """
                DECLARE TYPE int SIZE 4;
                DECLARE STRUCT P THEN x AS int; x AS int; END;
                """;
        assertFails(code, "duplicate field 'x'");
    }

    @Test
    void testUnknownTypeInVariable() throws IOException {
        String code = """
                DECLARE VARIABLE x AS float;
                """;
        assertFails(code, "unknown type 'float'");
    }

    @Test
    void testTypeSizeZero() throws IOException {
        String code = """
                DECLARE TYPE empty SIZE 0;
                """;
        assertFails(code, "type size must be at least 1");
    }

    @Test
    void testFrameSizeExceeded() throws IOException {
        String code = """
                DECLARE TYPE huge SIZE 32768;
                DECLARE VARIABLE x AS huge;
                """;
        assertFails(code, "program exceeds the 32767-byte stack frame");
    }

    @Test
    void testArithmeticAllOperators() throws Exception {
        String code = """
                    DECLARE TYPE int SIZE 4;
                    DECLARE VARIABLE a AS int;
                    DECLARE VARIABLE b AS int;
                    DECLARE VARIABLE x AS int;
                    a <- 10;
                    b <- 3;
                    x <- a + b;
                    x <- a - b;
                    x <- a * b;
                    x <- a / b;
                    x <- a % b;
                    """;
        assertCompiles(code);
    }

    @Test
    void testArithmeticPrecedence() throws Exception {
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
    void testArithmeticParentheses() throws Exception {
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
    void testArithmeticLiteralAdoption() throws Exception {
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
    void testArithmeticOnByte() throws Exception {
        String code = """
                DECLARE TYPE byte SIZE 1;
                DECLARE VARIABLE a AS byte;
                DECLARE VARIABLE b AS byte;
                a <- b + 1;
                """;
        assertCompiles(code);
    }

    @Test
    void testArithmeticFieldOperand() throws Exception {
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
    void testArithmeticFieldTarget() throws Exception {
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
    void testFoldLiteralMathOnWideType() throws Exception {
        String code = """
                DECLARE TYPE long SIZE 8;
                DECLARE VARIABLE x AS long;
                x <- 2 + 3;
                """;
        assertCompiles(code);
    }

    @Test
    void testArithmeticOperandTypeMismatch() throws IOException {
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
    void testArithmeticResultTypeMismatch() throws IOException {
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
    void testArithmeticOn2ByteRejected() throws IOException {
        String code = """
                DECLARE TYPE short SIZE 2;
                DECLARE VARIABLE a AS short;
                a <- a + 1;
                """;
        assertFails(code, "cannot do arithmetic");
    }

    @Test
    void testArithmeticOn8ByteRejected() throws IOException {
        String code = """
                DECLARE TYPE long SIZE 8;
                DECLARE VARIABLE a AS long;
                DECLARE VARIABLE b AS long;
                a <- b + 1;
                """;
        assertFails(code, "cannot do arithmetic");
    }

    @Test
    void testArithmeticUndeclaredOperand() throws IOException {
        String code = """
                DECLARE TYPE int SIZE 4;
                DECLARE VARIABLE x AS int;
                x <- z + 1;
                """;
        assertFails(code, "undeclared variable 'z'");
    }

    @Test
    void testShiftBothDirections() throws Exception {
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
    void testBitwiseAllOperators() throws Exception {
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
    void testShiftOnByte() throws Exception {
        String code = """
                DECLARE TYPE byte SIZE 1;
                DECLARE VARIABLE b AS byte;
                b <- b << 1;
                b <- b >> 1;
                """;
        assertCompiles(code);
    }

    @Test
    void testShiftFoldedValueOverflow() throws IOException {
        String code = """
                DECLARE TYPE int SIZE 4;
                DECLARE VARIABLE x AS int;
                x <- 1 << 31;
                """;
        assertFails(code, "does not fit in int");
    }

    @Test
    void testBitwiseFoldThenBoundsCheck() throws IOException {
        String code = """
                DECLARE TYPE byte SIZE 1;
                DECLARE VARIABLE b AS byte;
                b <- 15 | 240;
                """;
        assertFails(code, "does not fit in byte");
    }

    @Test
    void testNotOnInt() throws Exception {
        String code = """
                DECLARE TYPE int SIZE 4;
                DECLARE VARIABLE a AS int;
                DECLARE VARIABLE x AS int;
                x <- ~a;
                """;
        assertCompiles(code);
    }

    @Test
    void testNotOnByte() throws Exception {
        String code = """
                DECLARE TYPE byte SIZE 1;
                DECLARE VARIABLE b AS byte;
                b <- ~b;
                """;
        assertCompiles(code);
    }

    @Test
    void testNotFoldedOverflowByte() throws IOException {
        String code = """
                DECLARE TYPE byte SIZE 1;
                DECLARE VARIABLE b AS byte;
                b <- ~128;
                """;
        assertFails(code, "does not fit in byte");
    }

    @Test
    void testArrayDeclaration() throws Exception {
        String code = """
                DECLARE TYPE int SIZE 4;
                DECLARE ARRAY grid AS int LENGTH 10;
                """;
        assertCompiles(code);
    }

    @Test
    void testArrayLiteralIndex() throws Exception {
        String code = """
                DECLARE TYPE int SIZE 4;
                DECLARE ARRAY grid AS int LENGTH 10;
                grid[0] <- 42;
                grid[9] <- 99;
                """;
        assertCompiles(code);
    }

    @Test
    void testArrayVariableIndex() throws Exception {
        String code = """
                DECLARE TYPE int SIZE 4;
                DECLARE ARRAY grid AS int LENGTH 10;
                DECLARE VARIABLE i AS int;
                i <- 5;
                grid[i] <- 42;
                """;
        assertCompiles(code);
    }

    @Test
    void testArrayFieldAccess() throws Exception {
        String code = """
                DECLARE TYPE int SIZE 4;
                DECLARE STRUCT Point THEN x AS int; y AS int; END;
                DECLARE ARRAY grid AS Point LENGTH 4;
                x OF grid[0] <- 10;
                y OF grid[1] <- 20;
                """;
        assertCompiles(code);
    }

    @Test
    void testArrayFieldAccessVariableIndex() throws Exception {
        String code = """
                DECLARE TYPE int SIZE 4;
                DECLARE STRUCT Point THEN x AS int; y AS int; END;
                DECLARE ARRAY grid AS Point LENGTH 4;
                DECLARE VARIABLE i AS int;
                i <- 2;
                x OF grid[i] <- 99;
                """;
        assertCompiles(code);
    }

    @Test
    void testArrayOfArrays() throws Exception {
        String code = """
                DECLARE TYPE int SIZE 4;
                DECLARE ARRAY matrix AS int LENGTH 16;
                matrix[0] <- 42;
                matrix[15] <- 99;
                """;
        assertCompiles(code);
    }

    @Test
    void testArrayRead() throws Exception {
        String code = """
                DECLARE TYPE int SIZE 4;
                DECLARE ARRAY grid AS int LENGTH 10;
                DECLARE VARIABLE x AS int;
                grid[5] <- 42;
                x <- grid[5];
                """;
        assertCompiles(code);
    }

    @Test
    void testArrayFieldRead() throws Exception {
        String code = """
                DECLARE TYPE int SIZE 4;
                DECLARE STRUCT Point THEN x AS int; END;
                DECLARE ARRAY grid AS Point LENGTH 4;
                DECLARE VARIABLE myX AS int;
                x OF grid[0] <- 10;
                myX <- x OF grid[0];
                """;
        assertCompiles(code);
    }

    @Test
    void testLengthOfArray() throws Exception {
        String code = """
                DECLARE TYPE int SIZE 4;
                DECLARE ARRAY grid AS int LENGTH 42;
                DECLARE VARIABLE len AS int;
                len <- LENGTH OF grid;
                """;
        assertCompiles(code);
    }

    @Test
    void testLengthInExpression() throws Exception {
        String code = """
                DECLARE TYPE int SIZE 4;
                DECLARE ARRAY grid AS int LENGTH 10;
                DECLARE VARIABLE total AS int;
                total <- LENGTH OF grid * 4;
                """;
        assertCompiles(code);
    }

    @Test
    void testArrayCopyElementToElement() throws Exception {
        String code = """
                DECLARE TYPE int SIZE 4;
                DECLARE ARRAY src AS int LENGTH 10;
                DECLARE ARRAY dst AS int LENGTH 10;
                src[0] <- 42;
                dst[1] <- src[0];
                """;
        assertCompiles(code);
    }

    @Test
    void testArrayFieldToField() throws Exception {
        String code = """
                DECLARE TYPE int SIZE 4;
                DECLARE STRUCT Point THEN x AS int; y AS int; END;
                DECLARE ARRAY grid AS Point LENGTH 4;
                x OF grid[0] <- 10;
                y OF grid[1] <- x OF grid[0];
                """;
        assertCompiles(code);
    }

    @Test
    void testArrayOutOfBounds() throws IOException {
        String code = """
                DECLARE TYPE int SIZE 4;
                DECLARE ARRAY grid AS int LENGTH 10;
                grid[10] <- 42;
                """;
        assertFails(code, "out of bounds");
    }

    @Test
    void testArrayNegativeIndex() throws IOException {
        String code = """
                DECLARE TYPE int SIZE 4;
                DECLARE ARRAY grid AS int LENGTH 10;
                grid[-1] <- 42;
                """;
        assertFails(code, "out of bounds");
    }

    @Test
    void testArrayUnknownType() throws IOException {
        String code = """
                DECLARE ARRAY grid AS unknown LENGTH 10;
                """;
        assertFails(code, "unknown type 'unknown'");
    }

    @Test
    void testArrayZeroLength() throws IOException {
        String code = """
                DECLARE TYPE int SIZE 4;
                DECLARE ARRAY grid AS int LENGTH 0;
                """;
        assertFails(code, "array length must be at least 1");
    }

    @Test
    void testArrayFrameOverflow() throws IOException {
        String code = """
                DECLARE TYPE huge SIZE 8192;
                DECLARE ARRAY grid AS huge LENGTH 5;
                """;
        assertFails(code, "program exceeds the 32767-byte stack frame");
    }

    @Test
    void testArrayIndexNonInteger() throws IOException {
        String code = """
                DECLARE TYPE int SIZE 4;
                DECLARE STRUCT Point THEN x AS int; END;
                DECLARE ARRAY grid AS int LENGTH 10;
                DECLARE VARIABLE p AS Point;
                grid[p] <- 42;
                """;
        assertFails(code, "array index must be a 1- or 4-byte integer type");
    }

    @Test
    void testArrayAccessNonArray() throws IOException {
        String code = """
                DECLARE TYPE int SIZE 4;
                DECLARE VARIABLE x AS int;
                x[0] <- 42;
                """;
        assertFails(code, "'x' is not an array");
    }

    @Test
    void testArrayFieldAccessNonArray() throws IOException {
        String code = """
                DECLARE TYPE int SIZE 4;
                DECLARE VARIABLE x AS int;
                field OF x[0] <- 42;
                """;
        assertFails(code, "'x' is not an array");
    }

    @Test
    void testLengthOfNonArray() throws IOException {
        String code = """
                DECLARE TYPE int SIZE 4;
                DECLARE VARIABLE x AS int;
                DECLARE VARIABLE len AS int;
                len <- LENGTH OF x;
                """;
        assertFails(code, "not a struct");
    }

    @Test
    void testArrayArithmeticForbidden() throws IOException {
        String code = """
                DECLARE TYPE int SIZE 4;
                DECLARE ARRAY grid AS int LENGTH 10;
                DECLARE VARIABLE x AS int;
                x <- grid + 1;
                """;
        assertFails(code, "cannot use array 'grid'");
    }

    @Test
    void testArrayAssignWholeArray() throws IOException {
        String code = """
                DECLARE TYPE int SIZE 4;
                DECLARE ARRAY grid AS int LENGTH 10;
                grid <- 42;
                """;
        assertFails(code, "cannot assign to whole array");
    }
}