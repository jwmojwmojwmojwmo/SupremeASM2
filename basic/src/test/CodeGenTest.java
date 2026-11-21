package basic.src.test;

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
}