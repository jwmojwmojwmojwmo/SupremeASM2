package basic.src.test;

import basic.src.main.compiler.SupremeCompiler;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.PrintStream;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class CompilerIntegrationTest extends CompilerTestBase {

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
    void testSupremeCompilerSuccess() throws Exception {
        assertBytesEqual(bytes(0, 0, 0, 42), run("""
                DECLARE TYPE int SIZE 4;
                DECLARE VARIABLE x AS int;
                x <- 42;
                PRINT x;
                """));
    }

    @Test
    void testSupremeCompilerParseError() throws IOException {
        assertFails("DECLARE VARIABLE x AS;", "expected IDENTIFIER");
    }

    @Test
    void testSupremeCompilerSemaError() throws IOException {
        assertFails("""
                DECLARE TYPE int SIZE 4;
                DECLARE VARIABLE x AS int;
                x <- y;
                """, "undeclared variable 'y'");
    }

    @Test
    void testSupremeCompilerStructs() throws Exception {
        assertBytesEqual(bytes(0, 0, 0, 10), run("""
                DECLARE TYPE int SIZE 4;
                DECLARE STRUCT Point THEN
                    x AS int;
                    y AS int;
                END;
                DECLARE VARIABLE p1 AS Point;
                DECLARE VARIABLE p2 AS Point;
                x OF p1 <- 10;
                p2 <- p1;
                DECLARE VARIABLE myX AS int;
                myX <- x OF p2;
                PRINT myX;
                """));
    }

    @Test
    void testSupremeCompilerArithmetic() throws Exception {
        assertBytesEqual(bytes(0, 0, 0, 42), run("""
                DECLARE TYPE int SIZE 4;
                DECLARE VARIABLE a AS int;
                DECLARE VARIABLE b AS int;
                DECLARE VARIABLE x AS int;
                a <- 6;
                b <- 7;
                x <- a * b;
                PRINT x;
                """));
    }

    @Test
    void testSupremeCompilerBitwise() throws Exception {
        assertBytesEqual(bytes(0, 0, 0, 2), run("""
                DECLARE TYPE int SIZE 4;
                DECLARE VARIABLE a AS int;
                DECLARE VARIABLE x AS int;
                a <- 3;
                x <- (a << 2) | 1;
                x <- x >> 1;
                x <- x & 3;
                PRINT x;
                """));
    }

    @Test
    void testSupremeCompilerArrays() throws Exception {
        assertBytesEqual(bytes(0, 0, 0, 30, 0, 0, 0, 40), run("""
                DECLARE TYPE int SIZE 4;
                DECLARE STRUCT Point THEN
                    x AS int;
                    y AS int;
                END;

                DECLARE ARRAY grid AS Point LENGTH 4;
                DECLARE VARIABLE i AS int;

                x OF grid[0] <- 10;
                y OF grid[0] <- 20;

                i <- 2;
                x OF grid[i] <- 30;
                y OF grid[i] <- 40;
                
                PRINT x OF grid[i];
                PRINT y OF grid[i];
                """));
    }

    @Test
    void testSupremeCompilerLENGTHOfArray() throws Exception {
        assertBytesEqual(bytes(0, 0, 0, 10, 0, 0, 0, 40), run("""
                DECLARE TYPE int SIZE 4;
                DECLARE ARRAY grid AS int LENGTH 10;
                DECLARE VARIABLE LENGTH AS int;
                DECLARE VARIABLE total AS int;

                LENGTH <- LENGTH OF grid;
                total <- LENGTH * 4;
                PRINT LENGTH;
                PRINT total;
                """));
    }

    @Test
    void testSupremeCompilerArrayRead() throws Exception {
        assertBytesEqual(bytes(0, 0, 0, 42), run("""
                DECLARE TYPE int SIZE 4;
                DECLARE ARRAY grid AS int LENGTH 10;
                DECLARE VARIABLE x AS int;

                grid[5] <- 42;
                x <- grid[5];
                PRINT x;
                """));
    }
}