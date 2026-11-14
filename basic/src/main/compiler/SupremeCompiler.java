package basic.src.main.compiler;

import java.util.List;

import basic.src.main.exceptions.BadParseException;

// Represents the compiler that turns SupremeBASIC into SupremeASM
public class SupremeCompiler {
    int compilerVersion;
    Lexer lexer;
    Parser parser;


    public SupremeCompiler(int version) {
        compilerVersion = version;
        lexer = new Lexer();
        parser = new Parser();
    }

    public void compile(String sourceCode) {
        System.out.println("Compiling SupremeBASIC version " + compilerVersion + " source code:");
        List<Token> tokens = lexer.tokenize(sourceCode);
        System.out.println("Successfully tokenized " + tokens.size() + " tokens:");
        for (Token token : tokens) {
            System.out.println(token);
        }
        System.out.println("Parsing tokens into AST...");
        parser.setTokens(tokens);
        Node program;
        try {
            program = parser.parseProgram();
        } catch (BadParseException e) {
            System.err.println("Error parsing program: " + e.getMessage());
            return;
        }
        System.out.println("Successfully parsed program:");
        print(program, 0);
    }

        // ---- temporary test ----

    public void print(Node n, int depth) {
        String pad = "  ".repeat(depth);
        switch (n) {
            case Node.Program p -> {
                System.out.println(pad + "PROGRAM");
                for (Node s : p.statements())
                    print(s, depth + 1);
            }
            case Node.DeclareStatement d ->
                System.out.println(pad + "DECLARE " + d.name() + " size=" + d.size());
            case Node.AssignStatement a -> {
                System.out.println(pad + "ASSIGN " + a.name());
                print(a.value(), depth + 1);
            }
            case Node.NumberLiteral num ->
                System.out.println(pad + "NUMBER " + num.value());
        }
    }
}
