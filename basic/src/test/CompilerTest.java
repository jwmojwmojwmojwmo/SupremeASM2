package basic.src.test;

import basic.src.main.compiler.Lexer;
import basic.src.main.compiler.Node;
import basic.src.main.compiler.Parser;
import basic.src.main.compiler.Sema;
import basic.src.main.compiler.CodeGen;
import basic.src.main.compiler.SupremeCompiler;
import basic.src.main.compiler.Token;
import basic.src.main.exceptions.BadParseException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import static org.junit.jupiter.api.Assertions.*;

public class CompilerTest {

    private Sema.Result analyze(String code) {
        Lexer lexer = new Lexer();
        Parser parser = new Parser();
        Sema sema = new Sema();

        List<Token> tokens = lexer.tokenize(code);
        parser.setTokens(tokens);
        Node.Program program = parser.parseProgram();
        return sema.analyze(program);
    }

    private String generateAsm(String code) {
        Lexer lexer = new Lexer();
        Parser parser = new Parser();
        Sema sema = new Sema();
        CodeGen codegen = new CodeGen();

        List<Token> tokens = lexer.tokenize(code);
        parser.setTokens(tokens);
        Node.Program program = parser.parseProgram();
        Sema.Result result = sema.analyze(program);
        return codegen.generate(program, result);
    }

    private void assertCompiles(String code) {
        assertDoesNotThrow(() -> analyze(code), "Code should compile but threw an exception");
    }

    private void assertFails(String code, String expectedError) {
        Exception ex = assertThrows(Exception.class, () -> analyze(code));
        assertTrue(ex.getMessage().contains(expectedError),
                "Expected error containing '" + expectedError + "', but got: " + ex.getMessage());
    }

    private void assertAsmContains(String code, String expectedInstruction) {
        String asm = generateAsm(code);
        assertTrue(asm.contains(expectedInstruction),
                "Expected ASM to contain '" + expectedInstruction + "', but got:\n" + asm);
    }

    @Test
    void testAll() {
        String code = """
                // 1. Define Primitives
                DECLARE TYPE int SIZE 4;
                DECLARE TYPE byte SIZE 1;
                DECLARE TYPE short SIZE 2;

                // 2. Define Structs
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

                // 3. Instantiate Variables
                DECLARE VARIABLE playerPos AS Vec2;
                DECLARE VARIABLE enemyPos AS Vec2;
                DECLARE VARIABLE playerCol AS Color;
                DECLARE VARIABLE enemyCol AS Color;
                DECLARE VARIABLE tempX AS int;
                DECLARE VARIABLE tempR AS byte;

                // 4. Write to fields (Using safe signed byte ranges!)
                x OF playerPos <- 100;
                y OF playerPos <- 200;
                r OF playerCol <- 127;   // Max signed byte
                g OF playerCol <- 0;
                b OF playerCol <- 50;    // Safe positive number
                a OF playerCol <- 100;

                // 5. Copy entire structs (Triggers chunk-copy optimization!)
                enemyPos <- playerPos;
                enemyCol <- playerCol;

                // 6. Read fields into variables
                tempX <- x OF enemyPos;
                tempR <- r OF enemyCol;

                // 7. Field to Field copy
                x OF enemyPos <- y OF playerPos;
                r OF enemyCol <- b OF playerCol;
                """;
        assertCompiles(code);
    }

    @Test
    void testValidStructAssignment() {
        String code = """
                DECLARE TYPE int SIZE 4;
                DECLARE STRUCT Point THEN
                    x AS int;
                    y AS int;
                END;
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

    @Test
    void testByteLiteralBounds() {
        String codePass = """
                DECLARE TYPE byte SIZE 1;
                DECLARE VARIABLE b AS byte;
                b <- 127;
                """;
        assertCompiles(codePass);
    }

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
    void testLiteralOverflow() {
        String code = """
                DECLARE TYPE byte SIZE 1;
                DECLARE VARIABLE b AS byte;
                b <- 300;
                """;
        assertFails(code, "does not fit in byte");
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
    void testUnknownTypeInVariable() {
        String code = """
                DECLARE VARIABLE x AS float;
                """;
        assertFails(code, "unknown type 'float'");
    }

    @Test
    void testUnknownTypeInStructField() {
        String code = """
                DECLARE STRUCT Point THEN
                    x AS float;
                END;
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

    @Test
    void testUnknownFieldWrite() {
        String code = """
                DECLARE TYPE int SIZE 4;
                DECLARE STRUCT P THEN x AS int; END;
                DECLARE VARIABLE p AS P;
                y OF p <- 5;
                """;
        assertFails(code, "struct 'P' has no field 'y'");
    }

    @Test
    void testUnknownFieldRead() {
        String code = """
                DECLARE TYPE int SIZE 4;
                DECLARE STRUCT P THEN x AS int; END;
                DECLARE VARIABLE p AS P;
                DECLARE VARIABLE z AS int;
                z <- y OF p;
                """;
        assertFails(code, "struct 'P' has no field 'y'");
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
    void testSupremeCompilerSuccess(@TempDir Path tempDir) {
        SupremeCompiler compiler = new SupremeCompiler(1);
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
        SupremeCompiler compiler = new SupremeCompiler(1);
        String code = "DECLARE VARIABLE x AS;"; // Missing type identifier
        String filePath = tempDir.resolve("test_parse_error").toString();

        boolean result = compiler.compile(code, filePath);

        assertFalse(result, "Compilation should fail on parse error");
        assertFalse(Files.exists(Path.of(filePath + ".sasm")), "Output file should not be created on failure");
    }

    @Test
    void testSupremeCompilerSemaError(@TempDir Path tempDir) {
        SupremeCompiler compiler = new SupremeCompiler(1);
        String code = """
                DECLARE TYPE int SIZE 4;
                DECLARE VARIABLE x AS int;
                x <- y; // Undeclared variable y
                """;
        String filePath = tempDir.resolve("test_sema_error").toString();

        boolean result = compiler.compile(code, filePath);

        assertFalse(result, "Compilation should fail on sema error");
        assertFalse(Files.exists(Path.of(filePath + ".sasm")), "Output file should not be created on failure");
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
        // Tests the sign_word = -1 logic
        assertAsmContains(code, "ld #-1, %5;");
        assertAsmContains(code, "ld #-1, %6;");
    }

    @Test
    void testSupremeCompilerStructs(@TempDir Path tempDir) {
        SupremeCompiler compiler = new SupremeCompiler(1);
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
}