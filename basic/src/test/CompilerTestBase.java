package basic.src.test;

import basic.src.main.compiler.Lexer;
import basic.src.main.compiler.Node;
import basic.src.main.compiler.Parser;
import basic.src.main.compiler.Sema;
import basic.src.main.compiler.CodeGen;
import basic.src.main.compiler.Token;
import java.util.List;
import static org.junit.jupiter.api.Assertions.*;

// Shared helpers for all compiler test classes. No @Test methods here.
public abstract class CompilerTestBase {

    protected Sema.Result analyze(String code) {
        Lexer lexer = new Lexer();
        Parser parser = new Parser();
        Sema sema = new Sema();
        List<Token> tokens = lexer.tokenize(code);
        parser.setTokens(tokens);
        Node.Program program = parser.parseProgram();
        return sema.analyze(program);
    }

    protected void assertParses(String code) {
        Lexer lexer = new Lexer();
        Parser parser = new Parser();
        parser.setTokens(lexer.tokenize(code));
        assertDoesNotThrow(() -> parser.parseProgram(), "Code should parse but threw an exception");
    }

    protected String generateAsm(String code) {
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

    protected void assertCompiles(String code) {
        assertDoesNotThrow(() -> analyze(code), "Code should compile but threw an exception");
    }

    protected void assertFails(String code, String expectedError) {
        Exception ex = assertThrows(Exception.class, () -> analyze(code));
        assertTrue(ex.getMessage().contains(expectedError),
                "Expected error containing '" + expectedError + "', but got: " + ex.getMessage());
    }

    protected void assertParseFails(String code, String expectedError) {
        Lexer lexer = new Lexer();
        Parser parser = new Parser();
        List<Token> tokens = lexer.tokenize(code);
        parser.setTokens(tokens);
        Exception ex = assertThrows(Exception.class, () -> parser.parseProgram());
        assertTrue(ex.getMessage().contains(expectedError),
                "Expected parse error containing '" + expectedError + "', but got: " + ex.getMessage());
    }

    protected void assertAsmContains(String code, String expectedInstruction) {
        String asm = generateAsm(code);
        System.out.println(asm);
        assertTrue(asm.contains(expectedInstruction),
                "Expected ASM to contain '" + expectedInstruction + "', but got:\n" + asm);
    }

    protected void assertAsmNotContains(String code, String unexpected) {
        String asm = generateAsm(code);
        assertFalse(asm.contains(unexpected),
                "Expected ASM to NOT contain '" + unexpected + "', but got:\n" + asm);
    }
}