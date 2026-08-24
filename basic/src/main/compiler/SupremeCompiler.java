package basic.src.main.compiler;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

// Represents the compiler that turns SupremeBASIC into SupremeMC
public class SupremeCompiler {
    int compilerVersion;
    List<Token> tokens;
    Set<String> identifiers;
    Lexer lexer;

    public SupremeCompiler(int version) {
        compilerVersion = version;
        tokens = new ArrayList<>();
        identifiers = new HashSet<>();
        lexer = new Lexer();
    }

    public void compile(String sourceCode) {
        tokens = lexer.tokenize(sourceCode);
    }
}
