package basic.src.test;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

public class CodeGenTest extends CompilerTestBase {

    @Test
    void testCodeGenLiteralAssignment() {
        String code = """
                DECLARE TYPE int SIZE 4;
                DECLARE VARIABLE x AS int;
                x <- 42;
                """;
        assertAsmContains(code, "ld #42, %5;");
        assertAsmContains(code, "sti %5, %14+#0;");
    }

    @Test
    void testCodeGenVariableCopy() {
        String code = """
                DECLARE TYPE int SIZE 4;
                DECLARE VARIABLE x AS int;
                DECLARE VARIABLE y AS int;
                x <- 10;
                y <- x;
                """;
        assertAsmContains(code, "ldi %14+#0, %5;");
        assertAsmContains(code, "sti %5, %14+#4;");
    }

    @Test
    void testCodeGenStructCopy() {
        String code = """
                DECLARE TYPE int SIZE 4;
                DECLARE STRUCT Point THEN x AS int; y AS int; END;
                DECLARE VARIABLE p1 AS Point;
                DECLARE VARIABLE p2 AS Point;
                p2 <- p1;
                """;
        assertAsmContains(code, "ldi %14+#0, %5;");
        assertAsmContains(code, "sti %5, %14+#8;");
        assertAsmContains(code, "ldi %14+#4, %5;");
        assertAsmContains(code, "sti %5, %14+#12;");
    }

    @Test
    void testCodeGenFieldWrite() {
        String code = """
                DECLARE TYPE int SIZE 4;
                DECLARE STRUCT Point THEN x AS int; y AS int; END;
                DECLARE VARIABLE p AS Point;
                x OF p <- 100;
                """;
        assertAsmContains(code, "ld #100, %5;");
        assertAsmContains(code, "sti %5, %14+#0;");
    }

    @Test
    void testCodeGenFieldRead() {
        String code = """
                DECLARE TYPE int SIZE 4;
                DECLARE STRUCT Point THEN x AS int; y AS int; END;
                DECLARE VARIABLE p AS Point;
                DECLARE VARIABLE myX AS int;
                myX <- x OF p;
                """;
        assertAsmContains(code, "ldi %14+#0, %5;");
        assertAsmContains(code, "sti %5, %14+#8;");
    }

    @Test
    void testCodeGenByteOperations() {
        String code = """
                DECLARE TYPE byte SIZE 1;
                DECLARE VARIABLE b1 AS byte;
                DECLARE VARIABLE b2 AS byte;
                b1 <- 50;
                b2 <- b1;
                """;
        assertAsmContains(code, "ld #50, %5;");
        assertAsmContains(code, "stb %5, %14+#0;");
        assertAsmContains(code, "ldb %14+#0, %5;");
        assertAsmContains(code, "stb %5, %14+#1;");
    }

    @Test
    void testCodeGenStackAllocation() {
        String code = """
                DECLARE TYPE int SIZE 4;
                DECLARE VARIABLE x AS int;
                DECLARE VARIABLE y AS int;
                """;
        assertAsmContains(code, "ld #8, %5;");
        assertAsmContains(code, "sub %14, %5, %14;");
    }

    @Test
    void testCodeGenLiteralAssignmentSize2() {
        String code = """
                DECLARE TYPE short SIZE 2;
                DECLARE VARIABLE x AS short;
                x <- 1000;
                """;
        assertAsmContains(code, "ld #3, %5;");
        assertAsmContains(code, "stb %5, %14+#0;");
        assertAsmContains(code, "ld #232, %5;");
        assertAsmContains(code, "stb %5, %14+#1;");
    }

    @Test
    void testCodeGenLiteralAssignmentSize8() {
        String code = """
                DECLARE TYPE long SIZE 8;
                DECLARE VARIABLE x AS long;
                x <- 100000;
                """;
        assertAsmContains(code, "ld #100000, %5;");
        assertAsmContains(code, "ld #0, %6;");
        assertAsmContains(code, "sti %6, %14+#0;");
        assertAsmContains(code, "sti %5, %14+#4;");
    }

    @Test
    void testCodeGenNegativeLiteralSize8() {
        String code = """
                DECLARE TYPE long SIZE 8;
                DECLARE VARIABLE x AS long;
                x <- -1;
                """;
        assertAsmContains(code, "ld #-1, %5;");
        assertAsmContains(code, "ld #-1, %6;");
    }

    // ---- constant folding ----

    @Test
    void testFoldPrecedence() {
        String code = """
                DECLARE TYPE int SIZE 4;
                DECLARE VARIABLE x AS int;
                x <- 2 + 3 * 4;
                """;
        assertAsmContains(code, "ld #14, %5;");
        assertAsmNotContains(code, "add");
        assertAsmNotContains(code, "mul");
    }

    @Test
    void testFoldParentheses() {
        String code = """
                DECLARE TYPE int SIZE 4;
                DECLARE VARIABLE x AS int;
                x <- (2 + 3) * 4;
                """;
        assertAsmContains(code, "ld #20, %5;");
    }

    @Test
    void testFoldIntegerDivisionAndModulo() {
        String code = """
                DECLARE TYPE int SIZE 4;
                DECLARE VARIABLE x AS int;
                x <- 10 / 3;
                x <- 10 % 3;
                """;
        assertAsmContains(code, "ld #3, %5;");
        assertAsmContains(code, "ld #1, %5;");
    }

    @Test
    void testFoldNegativeResult() {
        String code = """
                DECLARE TYPE int SIZE 4;
                DECLARE VARIABLE x AS int;
                x <- 2 - 5;
                """;
        assertAsmContains(code, "ld #-3, %5;");
    }

    // ---- arithmetic with variables (register emission) ----

    @Test
    void testCodeGenAddRegisters() {
        String code = """
                DECLARE TYPE int SIZE 4;
                DECLARE VARIABLE a AS int;
                DECLARE VARIABLE b AS int;
                DECLARE VARIABLE x AS int;
                x <- a + b;
                """;
        assertAsmContains(code, "ldi %14+#0, %4;");
        assertAsmContains(code, "ldi %14+#4, %5;");
        assertAsmContains(code, "add %4, %5, %4;");
        assertAsmContains(code, "sti %4, %14+#8;");
    }

    // ---- bitwise: register emission ----

    @Test
    void testCodeGenShlRegisters() {
        String code = """
                DECLARE TYPE int SIZE 4;
                DECLARE VARIABLE a AS int;
                DECLARE VARIABLE b AS int;
                DECLARE VARIABLE x AS int;
                x <- a << b;
                """;
        assertAsmContains(code, "ldi %14+#0, %4;");
        assertAsmContains(code, "ldi %14+#4, %5;");
        assertAsmContains(code, "shl %4, %5, %4;");
        assertAsmContains(code, "sti %4, %14+#8;");
    }

    @Test
    void testCodeGenShrRegisters() {
        String code = """
                DECLARE TYPE int SIZE 4;
                DECLARE VARIABLE a AS int;
                DECLARE VARIABLE b AS int;
                DECLARE VARIABLE x AS int;
                x <- a >> b;
                """;
        assertAsmContains(code, "shr %4, %5, %4;");
    }

    @Test
    void testCodeGenAndRegisters() {
        String code = """
                DECLARE TYPE int SIZE 4;
                DECLARE VARIABLE a AS int;
                DECLARE VARIABLE b AS int;
                DECLARE VARIABLE x AS int;
                x <- a & b;
                """;
        assertAsmContains(code, "and %4, %5, %4;");
    }

    @Test
    void testCodeGenOrRegisters() {
        String code = """
                DECLARE TYPE int SIZE 4;
                DECLARE VARIABLE a AS int;
                DECLARE VARIABLE b AS int;
                DECLARE VARIABLE x AS int;
                x <- a | b;
                """;
        assertAsmContains(code, "or %4, %5, %4;");
    }

    @Test
    void testCodeGenXorRegisters() {
        String code = """
                DECLARE TYPE int SIZE 4;
                DECLARE VARIABLE a AS int;
                DECLARE VARIABLE b AS int;
                DECLARE VARIABLE x AS int;
                x <- a ^ b;
                """;
        assertAsmContains(code, "xor %4, %5, %4;");
    }

    @Test
    void testCodeGenShiftLiteralCount() {
        String code = """
                DECLARE TYPE int SIZE 4;
                DECLARE VARIABLE a AS int;
                DECLARE VARIABLE x AS int;
                x <- a << 2;
                """;
        assertAsmContains(code, "ldi %14+#0, %4;");
        assertAsmContains(code, "ld #2, %5;");
        assertAsmContains(code, "shl %4, %5, %4;");
        assertAsmContains(code, "sti %4, %14+#4;");
    }

    @Test
    void testCodeGenShiftByte() {
        String code = """
                DECLARE TYPE byte SIZE 1;
                DECLARE VARIABLE a AS byte;
                DECLARE VARIABLE b AS byte;
                a <- b << 1;
                """;
        assertAsmContains(code, "ldb %14+#1, %4;");
        assertAsmContains(code, "shl %4, %5, %4;");
        assertAsmContains(code, "stb %4, %14+#0;");
    }

    @Test
    void testCodeGenBitwiseByte() {
        String code = """
                DECLARE TYPE byte SIZE 1;
                DECLARE VARIABLE b1 AS byte;
                DECLARE VARIABLE b2 AS byte;
                b1 <- b2 & 15;
                """;
        assertAsmContains(code, "ldb %14+#1, %4;");
        assertAsmContains(code, "and %4, %5, %4;");
        assertAsmContains(code, "stb %4, %14+#0;");
    }

    @Test
    void testCodeGenNestedShiftRegisters() {
        String code = """
                DECLARE TYPE int SIZE 4;
                DECLARE VARIABLE a AS int;
                DECLARE VARIABLE b AS int;
                DECLARE VARIABLE x AS int;
                x <- a << (b >> 1);
                """;
        assertAsmContains(code, "ld #12, %5;");
    }

    @Test
    void testFoldShiftRightNegative() {
        String code = """
                DECLARE TYPE int SIZE 4;
                DECLARE VARIABLE x AS int;
                x <- -8 >> 1;
                """;
        assertAsmContains(code, "ld #-4, %5;");
    }

    @Test
    void testFoldBitwiseOps() {
        String code = """
                DECLARE TYPE int SIZE 4;
                DECLARE VARIABLE x AS int;
                x <- 12 & 10;
                x <- 12 | 3;
                x <- 12 ^ 10;
                """;
        assertAsmContains(code, "ld #8, %5;");
        assertAsmContains(code, "ld #15, %5;");
        assertAsmContains(code, "ld #6, %5;");
        assertAsmNotContains(code, "and");
        assertAsmNotContains(code, "or");
        assertAsmNotContains(code, "xor");
    }

    @Test
    void testCodeGenNot() {
        String code = """
                DECLARE TYPE int SIZE 4;
                DECLARE VARIABLE a AS int;
                DECLARE VARIABLE x AS int;
                x <- ~a;
                """;
        assertAsmContains(code, "ldi %14+#0, %4;");
        assertAsmContains(code, "not %4, %4;");
        assertAsmContains(code, "sti %4, %14+#4;");
    }

    @Test
    void testCodeGenNotByte() {
        String code = """
                DECLARE TYPE byte SIZE 1;
                DECLARE VARIABLE b AS byte;
                b <- ~b;
                """;
        assertAsmContains(code, "ldb %14+#0, %4;");
        assertAsmContains(code, "not %4, %4;");
        assertAsmContains(code, "stb %4, %14+#0;");
    }

    @Test
    void testCodeGenChainedNot() {
        String code = """
                DECLARE TYPE int SIZE 4;
                DECLARE VARIABLE a AS int;
                DECLARE VARIABLE x AS int;
                x <- ~~~a;
                """;
        // all in place in %4
        assertAsmContains(code, "not %4, %4;");
    }

    @Test
    void testCodeGenSethi() {
        String code = """
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
                x <- (a+(b+(c+(d+(e+(f^(g/(h*(i-(~j+k))))))))));
                """;
        assertAsmContains(code, "ld #48, %5;");

    }

    @Test
    void testCodeGenNotEnoughRegs() {
        // It uses '-' because subtraction is not associative, so the tree can't be
        // regrouped to need fewer registers.
        String code = """
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
                """;
        assertAsmContains(code, "sti %4, %14+#4");
    }

    // ---- array code generation ----

    @Test
    void testCodeGenArrayLiteralIndex() {
        String code = """
                DECLARE TYPE int SIZE 4;
                DECLARE ARRAY grid AS int LENGTH 10;
                grid[0] <- 42;
                """;
        assertAsmContains(code, "ld #42, %5;");
        assertAsmContains(code, "sti %5, %14+#0;");
    }

    @Test
    void testCodeGenArrayLiteralIndexNonZero() {
        String code = """
                DECLARE TYPE int SIZE 4;
                DECLARE ARRAY grid AS int LENGTH 10;
                grid[3] <- 42;
                """;
        assertAsmContains(code, "ld #42, %5;");
        assertAsmContains(code, "sti %5, %14+#12;");
    }

    @Test
    void testCodeGenArrayVariableIndex() {
        String code = """
                DECLARE TYPE int SIZE 4;
                DECLARE ARRAY grid AS int LENGTH 10;
                DECLARE VARIABLE i AS int;
                i <- 5;
                grid[i] <- 42;
                """;
        // Should compute address: %14 + (i * 4)
        assertAsmContains(code, "mul");
        assertAsmContains(code, "sti %5, %10+#0;");
    }

    @Test
    void testCodeGenArrayRead() {
        String code = """
                DECLARE TYPE int SIZE 4;
                DECLARE ARRAY grid AS int LENGTH 10;
                DECLARE VARIABLE x AS int;
                grid[5] <- 42;
                x <- grid[5];
                """;
        assertAsmContains(code, "ldi %14+#20, %5;");
        assertAsmContains(code, "sti %5, %14+#40;");
    }

    @Test
    void testCodeGenArrayFieldAccess() {
        String code = """
                DECLARE TYPE int SIZE 4;
                DECLARE STRUCT Point THEN x AS int; y AS int; END;
                DECLARE ARRAY grid AS Point LENGTH 4;
                x OF grid[0] <- 10;
                """;
        assertAsmContains(code, "ld #10, %5;");
        assertAsmContains(code, "sti %5, %14+#0;");
    }

    @Test
    void testCodeGenArrayFieldAccessNonZero() {
        String code = """
                DECLARE TYPE int SIZE 4;
                DECLARE STRUCT Point THEN x AS int; y AS int; END;
                DECLARE ARRAY grid AS Point LENGTH 4;
                y OF grid[0] <- 20;
                """;
        assertAsmContains(code, "ld #20, %5;");
        assertAsmContains(code, "sti %5, %14+#4;");
    }

    @Test
    void testCodeGenArrayFieldAccessNonZeroElement() {
        String code = """
                DECLARE TYPE int SIZE 4;
                DECLARE STRUCT Point THEN x AS int; y AS int; END;
                DECLARE ARRAY grid AS Point LENGTH 4;
                x OF grid[2] <- 30;
                """;
        assertAsmContains(code, "ld #30, %5;");
        assertAsmContains(code, "sti %5, %14+#16;");
    }

    @Test
    void testCodeGenArrayFieldAccessVariableIndex() {
        String code = """
                DECLARE TYPE int SIZE 4;
                DECLARE STRUCT Point THEN x AS int; y AS int; END;
                DECLARE ARRAY grid AS Point LENGTH 4;
                DECLARE VARIABLE i AS int;
                i <- 1;
                x OF grid[i] <- 40;
                """;
        // Should compute address: %14 + (i * 8) + 0
        assertAsmContains(code, "ld #8, %11;");
        assertAsmContains(code, "mul");
    }

    @Test
    void testCodeGenArrayFieldRead() {
        String code = """
                DECLARE TYPE int SIZE 4;
                DECLARE STRUCT Point THEN x AS int; END;
                DECLARE ARRAY grid AS Point LENGTH 4;
                DECLARE VARIABLE myX AS int;
                x OF grid[0] <- 10;
                myX <- x OF grid[0];
                """;
        assertAsmContains(code, "ldi %14+#0, %5;");
        assertAsmContains(code, "sti %5, %14+#16;");
    }

    @Test
    void testCodeGenLenOfArray() {
        String code = """
                DECLARE TYPE int SIZE 4;
                DECLARE ARRAY grid AS int LENGTH 42;
                DECLARE VARIABLE len AS int;
                len <- LENGTH OF grid;
                """;
        assertAsmContains(code, "ld #42, %5;");
        assertAsmContains(code, "sti %5, %14+#168;");
    }

    @Test
    void testCodeGenLenInExpression() {
        String code = """
                DECLARE TYPE int SIZE 4;
                DECLARE ARRAY grid AS int LENGTH 10;
                DECLARE VARIABLE total AS int;
                total <- LENGTH OF grid * 4;
                """;
        assertAsmContains(code, "ld #40, %5;");
    }

    @Test
    void testCodeGenArrayElementCopy() {
        String code = """
                DECLARE TYPE int SIZE 4;
                DECLARE ARRAY src AS int LENGTH 10;
                DECLARE ARRAY dst AS int LENGTH 10;
                src[5] <- 42;
                dst[3] <- src[5];
                """;
        assertAsmContains(code, "ldi %14+#20, %5;");
        assertAsmContains(code, "sti %5, %14+#52;");
    }
}