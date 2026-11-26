package basic.src.test;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class CodeGenTest extends CompilerTestBase {

    @Test
    void testCodeGenLiteralAssignment() throws Exception {
        assertBytesEqual(bytes(0, 0, 0, 42), run("""
                DECLARE TYPE int SIZE 4;
                DECLARE VARIABLE x AS int;
                x <- 42;
                PRINT x;
                """));
    }

    @Test
    void testCodeGenVariableCopy() throws Exception {
        assertBytesEqual(bytes(0, 0, 0, 10), run("""
                DECLARE TYPE int SIZE 4;
                DECLARE VARIABLE x AS int;
                DECLARE VARIABLE y AS int;
                x <- 10;
                y <- x;
                PRINT y;
                """));
    }

    @Test
    void testCodeGenStructCopy() throws Exception {
        assertBytesEqual(bytes(0, 0, 0, 200), run("""
                DECLARE TYPE int SIZE 4;
                DECLARE STRUCT Point THEN x AS int; y AS int; END;
                DECLARE VARIABLE p1 AS Point;
                DECLARE VARIABLE p2 AS Point;
                x OF p1 <- 100;
                y OF p1 <- 200;
                p2 <- p1;
                PRINT y OF p2;
                """));
    }

    @Test
    void testCodeGenFieldWrite() throws Exception {
        assertBytesEqual(bytes(0, 0, 0, 100), run("""
                DECLARE TYPE int SIZE 4;
                DECLARE STRUCT Point THEN x AS int; y AS int; END;
                DECLARE VARIABLE p AS Point;
                x OF p <- 100;
                PRINT x OF p;
                """));
    }

    @Test
    void testCodeGenFieldRead() throws Exception {
        assertBytesEqual(bytes(0, 0, 0, 100), run("""
                DECLARE TYPE int SIZE 4;
                DECLARE STRUCT Point THEN x AS int; y AS int; END;
                DECLARE VARIABLE p AS Point;
                DECLARE VARIABLE myX AS int;
                x OF p <- 100;
                myX <- x OF p;
                PRINT myX;
                """));
    }

    @Test
    void testCodeGenByteOperations() throws Exception {
        assertBytesEqual(bytes(50), run("""
                DECLARE TYPE byte SIZE 1;
                DECLARE VARIABLE b1 AS byte;
                DECLARE VARIABLE b2 AS byte;
                b1 <- 50;
                b2 <- b1;
                PRINT b2;
                """));
    }

    @Test
    void testCodeGenStackAllocation() throws Exception {
        run("""
                DECLARE TYPE int SIZE 4;
                DECLARE VARIABLE x AS int;
                DECLARE VARIABLE y AS int;
                """);
    }

    @Test
    void testCodeGenLiteralAssignmentSize2() throws Exception {
        assertBytesEqual(bytes(3, 232), run("""
                DECLARE TYPE short SIZE 2;
                DECLARE VARIABLE x AS short;
                x <- 1000;
                PRINT x;
                """));
    }

    @Test
    void testCodeGenLiteralAssignmentSize8() throws Exception {
        assertBytesEqual(bytes(0, 0, 0, 0, 0, 1, 134, 160), run("""
                DECLARE TYPE long SIZE 8;
                DECLARE VARIABLE x AS long;
                x <- 100000;
                PRINT x;
                """));
    }

    @Test
    void testCodeGenNegativeLiteralSize8() throws Exception {
        assertBytesEqual(bytes(255, 255, 255, 255, 255, 255, 255, 255), run("""
                DECLARE TYPE long SIZE 8;
                DECLARE VARIABLE x AS long;
                x <- -1;
                PRINT x;
                """));
    }

    @Test
    void testFoldPrecedence() throws Exception {
        assertBytesEqual(bytes(0, 0, 0, 14), run("""
                DECLARE TYPE int SIZE 4;
                DECLARE VARIABLE x AS int;
                x <- 2 + 3 * 4;
                PRINT x;
                """));
    }

    @Test
    void testFoldParentheses() throws Exception {
        assertBytesEqual(bytes(0, 0, 0, 20), run("""
                DECLARE TYPE int SIZE 4;
                DECLARE VARIABLE x AS int;
                x <- (2 + 3) * 4;
                PRINT x;
                """));
    }

    @Test
    void testFoldIntegerDivisionAndModulo() throws Exception {
        assertBytesEqual(bytes(0, 0, 0, 3, 0, 0, 0, 1), run("""
                DECLARE TYPE int SIZE 4;
                DECLARE VARIABLE x AS int;
                x <- 10 / 3;
                PRINT x;
                x <- 10 % 3;
                PRINT x;
                """));
    }

    @Test
    void testFoldNegativeResult() throws Exception {
        assertBytesEqual(bytes(255, 255, 255, 253), run("""
                DECLARE TYPE int SIZE 4;
                DECLARE VARIABLE x AS int;
                x <- 2 - 5;
                PRINT x;
                """));
    }

    @Test
    void testCodeGenAddRegisters() throws Exception {
        assertBytesEqual(bytes(0, 0, 0, 30), run("""
                DECLARE TYPE int SIZE 4;
                DECLARE VARIABLE a AS int;
                DECLARE VARIABLE b AS int;
                DECLARE VARIABLE x AS int;
                a <- 10;
                b <- 20;
                x <- a + b;
                PRINT x;
                """));
    }

    @Test
    void testCodeGenShlRegisters() throws Exception {
        assertBytesEqual(bytes(0, 0, 0, 20), run("""
                DECLARE TYPE int SIZE 4;
                DECLARE VARIABLE a AS int;
                DECLARE VARIABLE b AS int;
                DECLARE VARIABLE x AS int;
                a <- 5;
                b <- 2;
                x <- a << b;
                PRINT x;
                """));
    }

    @Test
    void testCodeGenShrRegisters() throws Exception {
        assertBytesEqual(bytes(0, 0, 0, 5), run("""
                DECLARE TYPE int SIZE 4;
                DECLARE VARIABLE a AS int;
                DECLARE VARIABLE b AS int;
                DECLARE VARIABLE x AS int;
                a <- 20;
                b <- 2;
                x <- a >> b;
                PRINT x;
                """));
    }

    @Test
    void testCodeGenAndRegisters() throws Exception {
        assertBytesEqual(bytes(0, 0, 0, 9), run("""
                DECLARE TYPE int SIZE 4;
                DECLARE VARIABLE a AS int;
                DECLARE VARIABLE b AS int;
                DECLARE VARIABLE x AS int;
                a <- 15;
                b <- 9;
                x <- a & b;
                PRINT x;
                """));
    }

    @Test
    void testCodeGenOrRegisters() throws Exception {
        assertBytesEqual(bytes(0, 0, 0, 12), run("""
                DECLARE TYPE int SIZE 4;
                DECLARE VARIABLE a AS int;
                DECLARE VARIABLE b AS int;
                DECLARE VARIABLE x AS int;
                a <- 8;
                b <- 4;
                x <- a | b;
                PRINT x;
                """));
    }

    @Test
    void testCodeGenXorRegisters() throws Exception {
        assertBytesEqual(bytes(0, 0, 0, 12), run("""
                DECLARE TYPE int SIZE 4;
                DECLARE VARIABLE a AS int;
                DECLARE VARIABLE b AS int;
                DECLARE VARIABLE x AS int;
                a <- 10;
                b <- 6;
                x <- a ^ b;
                PRINT x;
                """));
    }

    @Test
    void testCodeGenShiftLiteralCount() throws Exception {
        assertBytesEqual(bytes(0, 0, 0, 20), run("""
                DECLARE TYPE int SIZE 4;
                DECLARE VARIABLE a AS int;
                DECLARE VARIABLE x AS int;
                a <- 5;
                x <- a << 2;
                PRINT x;
                """));
    }

    @Test
    void testCodeGenShiftByte() throws Exception {
        assertBytesEqual(bytes(100), run("""
                DECLARE TYPE byte SIZE 1;
                DECLARE VARIABLE a AS byte;
                DECLARE VARIABLE b AS byte;
                b <- 50;
                a <- b << 1;
                PRINT a;
                """));
    }

    @Test
    void testCodeGenBitwiseByte() throws Exception {
        assertBytesEqual(bytes(15), run("""
                DECLARE TYPE byte SIZE 1;
                DECLARE VARIABLE b1 AS byte;
                DECLARE VARIABLE b2 AS byte;
                b2 <- -1;
                b1 <- b2 & 15;
                PRINT b1;
                """));
    }

    @Test
    void testCodeGenNestedShiftRegisters() throws Exception {
        assertBytesEqual(bytes(0, 0, 0, 20), run("""
                DECLARE TYPE int SIZE 4;
                DECLARE VARIABLE a AS int;
                DECLARE VARIABLE b AS int;
                DECLARE VARIABLE x AS int;
                a <- 5;
                b <- 4;
                x <- a << (b >> 1);
                PRINT x;
                """));
    }

    @Test
    void testFoldShiftRightNegative() throws Exception {
        assertBytesEqual(bytes(255, 255, 255, 252), run("""
                DECLARE TYPE int SIZE 4;
                DECLARE VARIABLE x AS int;
                x <- -8 >> 1;
                PRINT x;
                """));
    }

    @Test
    void testFoldBitwiseOps() throws Exception {
        assertBytesEqual(bytes(0, 0, 0, 8, 0, 0, 0, 15, 0, 0, 0, 6), run("""
                DECLARE TYPE int SIZE 4;
                DECLARE VARIABLE x AS int;
                x <- 12 & 10;
                PRINT x;
                x <- 12 | 3;
                PRINT x;
                x <- 12 ^ 10;
                PRINT x;
                """));
    }

    @Test
    void testCodeGenNot() throws Exception {
        assertBytesEqual(bytes(255, 255, 255, 255), run("""
                DECLARE TYPE int SIZE 4;
                DECLARE VARIABLE a AS int;
                DECLARE VARIABLE x AS int;
                a <- 0;
                x <- ~a;
                PRINT x;
                """));
    }

    @Test
    void testCodeGenNotByte() throws Exception {
        assertBytesEqual(bytes(255), run("""
                DECLARE TYPE byte SIZE 1;
                DECLARE VARIABLE b AS byte;
                b <- 0;
                b <- ~b;
                PRINT b;
                """));
    }

    @Test
    void testCodeGenChainedNot() throws Exception {
        assertBytesEqual(bytes(255, 255, 255, 250), run("""
                DECLARE TYPE int SIZE 4;
                DECLARE VARIABLE a AS int;
                DECLARE VARIABLE x AS int;
                a <- 5;
                x <- ~~~a;
                PRINT x;
                """));
    }

    @Test
    void testCodeGenSethi() throws Exception {
        assertBytesEqual(bytes(0, 0, 0, 0), run("""
                DECLARE TYPE int SIZE 4;
                DECLARE VARIABLE a AS int;
                DECLARE VARIABLE b AS int;
                DECLARE VARIABLE c AS int;
                DECLARE VARIABLE d AS int;
                DECLARE VARIABLE e AS int;
                DECLARE VARIABLE f AS int;
                DECLARE VARIABLE g AS int;
                DECLARE VARIABLE h AS int;
                DECLARE VARIABLE i AS int;
                DECLARE VARIABLE j AS int;
                DECLARE VARIABLE k AS int;
                DECLARE VARIABLE x AS int;
                h <- 1;
                x <- (a+(b+(c+(d+(e+(f^(g/(h*(i-(~j+k))))))))));
                PRINT x;
                """));
    }

    @Test
    void testCodeGenNotEnoughRegs() throws Exception {
        assertBytesEqual(bytes(0, 0, 0, 0), run("""
                DECLARE TYPE int SIZE 4;
                DECLARE VARIABLE a AS int;
                DECLARE VARIABLE x AS int;
                a <- 1;
                x <- ((((((a - a) - (a - a)) - ((a - a) - (a - a)))
                         - (((a - a) - (a - a)) - ((a - a) - (a - a))))
                        - ((((a - a) - (a - a)) - ((a - a) - (a - a)))
                           - (((a - a) - (a - a)) - ((a - a) - (a - a)))))
                       - (((((a - a) - (a - a)) - ((a - a) - (a - a)))
                           - (((a - a) - (a - a)) - ((a - a) - (a - a))))
                          - ((((a - a) - (a - a)) - ((a - a) - (a - a)))
                             - (((a - a) - (a - a)) - ((a - a) - (a - a))))));
                PRINT x;
                """));
    }

    @Test
    void testCodeGenArrayLiteralIndex() throws Exception {
        assertBytesEqual(bytes(0, 0, 0, 42), run("""
                DECLARE TYPE int SIZE 4;
                DECLARE ARRAY grid AS int LENGTH 10;
                grid[0] <- 42;
                PRINT grid[0];
                """));
    }

    @Test
    void testCodeGenArrayLiteralIndexNonZero() throws Exception {
        assertBytesEqual(bytes(0, 0, 0, 42), run("""
                DECLARE TYPE int SIZE 4;
                DECLARE ARRAY grid AS int LENGTH 10;
                grid[3] <- 42;
                PRINT grid[3];
                """));
    }

    @Test
    void testCodeGenArrayVariableIndex() throws Exception {
        assertBytesEqual(bytes(0, 0, 0, 42), run("""
                DECLARE TYPE int SIZE 4;
                DECLARE ARRAY grid AS int LENGTH 10;
                DECLARE VARIABLE i AS int;
                i <- 5;
                grid[i] <- 42;
                PRINT grid[i];
                """));
    }

    @Test
    void testCodeGenArrayRead() throws Exception {
        assertBytesEqual(bytes(0, 0, 0, 42), run("""
                DECLARE TYPE int SIZE 4;
                DECLARE ARRAY grid AS int LENGTH 10;
                DECLARE VARIABLE x AS int;
                grid[5] <- 42;
                x <- grid[5];
                PRINT x;
                """));
    }

    @Test
    void testCodeGenArrayFieldAccess() throws Exception {
        assertBytesEqual(bytes(0, 0, 0, 10), run("""
                DECLARE TYPE int SIZE 4;
                DECLARE STRUCT Point THEN x AS int; y AS int; END;
                DECLARE ARRAY grid AS Point LENGTH 4;
                x OF grid[0] <- 10;
                PRINT x OF grid[0];
                """));
    }

    @Test
    void testCodeGenArrayFieldAccessNonZero() throws Exception {
        assertBytesEqual(bytes(0, 0, 0, 20), run("""
                DECLARE TYPE int SIZE 4;
                DECLARE STRUCT Point THEN x AS int; y AS int; END;
                DECLARE ARRAY grid AS Point LENGTH 4;
                y OF grid[0] <- 20;
                PRINT y OF grid[0];
                """));
    }

    @Test
    void testCodeGenArrayFieldAccessNonZeroElement() throws Exception {
        assertBytesEqual(bytes(0, 0, 0, 30), run("""
                DECLARE TYPE int SIZE 4;
                DECLARE STRUCT Point THEN x AS int; y AS int; END;
                DECLARE ARRAY grid AS Point LENGTH 4;
                x OF grid[2] <- 30;
                PRINT x OF grid[2];
                """));
    }

    @Test
    void testCodeGenArrayFieldAccessVariableIndex() throws Exception {
        assertBytesEqual(bytes(0, 0, 0, 40), run("""
                DECLARE TYPE int SIZE 4;
                DECLARE STRUCT Point THEN x AS int; y AS int; END;
                DECLARE ARRAY grid AS Point LENGTH 4;
                DECLARE VARIABLE i AS int;
                i <- 1;
                x OF grid[i] <- 40;
                PRINT x OF grid[i];
                """));
    }

    @Test
    void testCodeGenArrayFieldRead() throws Exception {
        assertBytesEqual(bytes(0, 0, 0, 10), run("""
                DECLARE TYPE int SIZE 4;
                DECLARE STRUCT Point THEN x AS int; END;
                DECLARE ARRAY grid AS Point LENGTH 4;
                DECLARE VARIABLE myX AS int;
                x OF grid[0] <- 10;
                myX <- x OF grid[0];
                PRINT myX;
                """));
    }

    @Test
    void testCodeGenLenOfArray() throws Exception {
        assertBytesEqual(bytes(0, 0, 0, 42), run("""
                DECLARE TYPE int SIZE 4;
                DECLARE ARRAY grid AS int LENGTH 42;
                DECLARE VARIABLE len AS int;
                len <- LENGTH OF grid;
                PRINT len;
                """));
    }

    @Test
    void testCodeGenLenInExpression() throws Exception {
        assertBytesEqual(bytes(0, 0, 0, 40), run("""
                DECLARE TYPE int SIZE 4;
                DECLARE ARRAY grid AS int LENGTH 10;
                DECLARE VARIABLE total AS int;
                total <- LENGTH OF grid * 4;
                PRINT total;
                """));
    }

    @Test
    void testCodeGenArrayElementCopy() throws Exception {
        assertBytesEqual(bytes(0, 0, 0, 42), run("""
                DECLARE TYPE int SIZE 4;
                DECLARE ARRAY src AS int LENGTH 10;
                DECLARE ARRAY dst AS int LENGTH 10;
                src[5] <- 42;
                dst[3] <- src[5];
                PRINT dst[3];
                """));
    }
}