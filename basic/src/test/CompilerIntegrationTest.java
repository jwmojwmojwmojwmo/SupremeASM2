package basic.src.test;

import basic.src.main.compiler.SupremeCompiler;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import static org.junit.jupiter.api.Assertions.*;

public class CompilerIntegrationTest extends CompilerTestBase {

    @Test
    void testSupremeCompilerSuccess(@TempDir Path tempDir) {
        SupremeCompiler compiler = new SupremeCompiler(1, false);
        String code = """
                DECLARE TYPE int SIZE 4;
                DECLARE VARIABLE x AS int;
                x <- 42;
                """;
        String filePath = tempDir.resolve("test_success").toString();
        boolean result = compiler.compile(code, filePath);

        assertTrue(result, "Compilation should succeed");
        Path generatedFile = Path.of(filePath + ".sasm");
        assertTrue(Files.exists(generatedFile), "Output .sasm file should exist");

        try {
            String content = Files.readString(generatedFile);
            assertTrue(content.contains("ld #42, %5;"), "Generated ASM should contain correct instruction");
            assertTrue(content.contains("halt;"), "Generated ASM should contain halt");
        } catch (IOException e) {
            fail("Failed to read generated .sasm file: " + e.getMessage());
        }
    }

    @Test
    void testSupremeCompilerParseError(@TempDir Path tempDir) {
        SupremeCompiler compiler = new SupremeCompiler(1, false);
        String code = "DECLARE VARIABLE x AS;";
        String filePath = tempDir.resolve("test_parse_error").toString();

        boolean result = compiler.compile(code, filePath);

        assertFalse(result, "Compilation should fail on parse error");
        assertFalse(Files.exists(Path.of(filePath + ".sasm")), "Output file should not be created on failure");
    }

    @Test
    void testSupremeCompilerSemaError(@TempDir Path tempDir) {
        SupremeCompiler compiler = new SupremeCompiler(1, false);
        String code = """
                DECLARE TYPE int SIZE 4;
                DECLARE VARIABLE x AS int;
                x <- y;
                """;
        String filePath = tempDir.resolve("test_sema_error").toString();

        boolean result = compiler.compile(code, filePath);

        assertFalse(result, "Compilation should fail on sema error");
        assertFalse(Files.exists(Path.of(filePath + ".sasm")), "Output file should not be created on failure");
    }

    @Test
    void testSupremeCompilerStructs(@TempDir Path tempDir) {
        SupremeCompiler compiler = new SupremeCompiler(1, false);
        String code = """
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
                """;
        String filePath = tempDir.resolve("test_structs").toString();
        boolean result = compiler.compile(code, filePath);

        assertTrue(result, "Compilation should succeed");
        assertTrue(Files.exists(Path.of(filePath + ".sasm")), "Output file should be created");
    }

    @Test
    void testSupremeCompilerArithmetic(@TempDir Path tempDir) {
        SupremeCompiler compiler = new SupremeCompiler(1, false);
        String code = """
                DECLARE TYPE int SIZE 4;
                DECLARE VARIABLE a AS int;
                DECLARE VARIABLE b AS int;
                DECLARE VARIABLE x AS int;
                a <- 6;
                b <- 7;
                x <- a * b;
                """;
        String filePath = tempDir.resolve("test_arithmetic").toString();
        boolean result = compiler.compile(code, filePath);

        assertTrue(result, "Compilation should succeed");
        try {
            String content = Files.readString(Path.of(filePath + ".sasm"));
            assertTrue(content.contains("mul"), "Generated ASM should contain a mul instruction");
        } catch (IOException e) {
            fail("Failed to read generated .sasm file: " + e.getMessage());
        }
    }

    @Test
    void testSupremeCompilerBitwise(@TempDir Path tempDir) {
        SupremeCompiler compiler = new SupremeCompiler(1, false);
        String code = """
                DECLARE TYPE int SIZE 4;
                DECLARE VARIABLE a AS int;
                DECLARE VARIABLE x AS int;
                a <- 3;
                x <- (a << 2) | 1;
                x <- x >> 1;
                x <- x & 3;
                """;
        String filePath = tempDir.resolve("test_bitwise").toString();
        assertTrue(compiler.compile(code, filePath), "Compilation should succeed");
        try {
            String content = Files.readString(Path.of(filePath + ".sasm"));
            assertTrue(content.contains("shl"), "should contain shl");
            assertTrue(content.contains("or"), "should contain or");
            assertTrue(content.contains("shr"), "should contain shr");
            assertTrue(content.contains("and"), "should contain and");
        } catch (IOException e) {
            fail("Failed to read generated .sasm file: " + e.getMessage());
        }
    }

    @Test
    void testSupremeCompilerArrays(@TempDir Path tempDir) {
        SupremeCompiler compiler = new SupremeCompiler(1, false);
        String code = """
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
                """;

        String filePath = tempDir.resolve("test_arrays").toString();
        boolean result = compiler.compile(code, filePath);
        assertTrue(result, "Compilation should succeed");

        Path generatedFile = Path.of(filePath + ".sasm");
        assertTrue(Files.exists(generatedFile), "Output .sasm file should exist");

        try {
            String content = Files.readString(generatedFile);
            // Should contain mul for variable indexing
            assertTrue(content.contains("mul"), "Generated ASM should contain mul for array indexing");
            // Should contain literal stores
            assertTrue(content.contains("ld #10, %5;"), "Should store x=10");
            assertTrue(content.contains("ld #20, %5;"), "Should store y=20");
            assertTrue(content.contains("halt;"), "Generated ASM should contain halt");
        } catch (IOException e) {
            fail("Failed to read generated .sasm file: " + e.getMessage());
        }
    }

    @Test
    void testSupremeCompilerLENGTHOfArray(@TempDir Path tempDir) {
        SupremeCompiler compiler = new SupremeCompiler(1, false);
        String code = """
                DECLARE TYPE int SIZE 4;
                DECLARE ARRAY grid AS int LENGTH 10;
                DECLARE VARIABLE LENGTH AS int;
                DECLARE VARIABLE total AS int;

                LENGTH <- LENGTH OF grid;
                total <- LENGTH * 4;
                """;

        String filePath = tempDir.resolve("test_LENGTH").toString();
        boolean result = compiler.compile(code, filePath);
        assertTrue(result, "Compilation should succeed");

        try {
            String content = Files.readString(Path.of(filePath + ".sasm"));
            // LENGTH should be replaced with literal 10
            assertTrue(content.contains("ld #10, %5;"), "LENGTH should be replaced with literal 10");
            // total should be 40
            assertTrue(content.contains("ld #40, %5;"), "total should be 40");
        } catch (IOException e) {
            fail("Failed to read generated .sasm file: " + e.getMessage());
        }
    }

    @Test
    void testSupremeCompilerArrayRead(@TempDir Path tempDir) {
        SupremeCompiler compiler = new SupremeCompiler(1, false);
        String code = """
                DECLARE TYPE int SIZE 4;
                DECLARE ARRAY grid AS int LENGTH 10;
                DECLARE VARIABLE x AS int;

                grid[5] <- 42;
                x <- grid[5];
                """;

        String filePath = tempDir.resolve("test_array_read").toString();
        boolean result = compiler.compile(code, filePath);
        assertTrue(result, "Compilation should succeed");

        try {
            String content = Files.readString(Path.of(filePath + ".sasm"));
            assertTrue(content.contains("ldi %14+#20, %5;"), "Should load from grid[5]");
            assertTrue(content.contains("sti %5, %14+#40;"), "Should store to x");
        } catch (IOException e) {
            fail("Failed to read generated .sasm file: " + e.getMessage());
        }
    }
}