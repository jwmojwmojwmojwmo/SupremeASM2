package basic.src.main.compiler;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import basic.src.main.exceptions.BadParseException;

// Represents the compiler that turns SupremeBASIC into SupremeASM
public class SupremeCompiler {
    int compilerVersion;
    Lexer lexer;
    Parser parser;
    Sema sema;
    CodeGen codeGen;
    boolean verbose;

    public SupremeCompiler(int version, boolean verbose) {
        compilerVersion = version;
        this.verbose = verbose;
        lexer = new Lexer();
        parser = new Parser();
        sema = new Sema();
        codeGen = new CodeGen();
    }

    public boolean compile(String sourceCode, String fileName) {
        System.out.println("Compiling SupremeBASIC version " + compilerVersion + " source code:");
        List<Token> tokens = lexer.tokenize(sourceCode);
        if (verbose) {
            System.out.println("Successfully tokenized " + tokens.size() + " tokens:");
            for (Token token : tokens) {
                System.out.println(token);
            }
            System.out.println("Parsing tokens into AST...");
        }
        parser.setTokens(tokens);
        Node.Program program;
        try {
            program = parser.parseProgram();
        } catch (BadParseException e) {
            System.err.println("Error parsing program: " + e.getMessage());
            return false;
        }
        if (verbose) {
            System.out.println("Successfully parsed program:");
            print(program, "");
            System.out.println("Performing semantic analysis...");
        }
        Sema.Result semaResult;
        try {
            semaResult = sema.analyze(program);
        } catch (Exception e) {
            System.err.println("Error in semantic analysis: " + e.getMessage());
            return false;
        }
        if (verbose) {
            System.out.println("Semantic analysis complete.");
        }
        System.out.println("Generating SupremeASM...");
        String asm = codeGen.generate(semaResult.program(), semaResult);
        try {
            Files.writeString(Path.of(fileName + ".sasm"), asm);
            System.out.println("Generated SupremeASM in " + fileName + ".sasm");
        } catch (IOException e) {
            e.printStackTrace();
            return false;
        }
        return true;
    }

    private void print(Node n, String prefix) {
        switch (n) {
        case Node.Program p -> {
            System.out.println(prefix + "PROGRAM");
            for (int i = 0; i < p.statements().size(); i++)
                printChild(p.statements().get(i), prefix, i == p.statements().size() - 1);
        }
        case Node.TypeDeclaration t -> System.out.println(prefix + "DECLARE TYPE " + t.name() + " SIZE " + t.size());
        case Node.StructDeclaration s -> {
            System.out.println(prefix + "DECLARE STRUCT " + s.name());
            for (int i = 0; i < s.fields().size(); i++)
                printChild(s.fields().get(i), prefix, i == s.fields().size() - 1);
        }
        case Node.StructFieldDeclaration f -> System.out.println(prefix + "FIELD " + f.name() + " AS " + f.typeName());
        case Node.VariableDeclaration d -> System.out
                .println(prefix + "DECLARE VARIABLE " + d.name() + " AS " + d.typeName());
        case Node.ArrayDeclaration d -> System.out
                .println(prefix + "DECLARE ARRAY " + d.name() + " AS " + d.typeName() + " LENGTH " + d.length());
        case Node.AssignmentStatement a -> {
            System.out.println(prefix + "ASSIGN");
            printLabeledChild("target:", a.target(), prefix, false);
            printLabeledChild("value:", a.value(), prefix, true);
        }
        case Node.NumberLiteral num -> System.out.println(prefix + "NUMBER " + num.value());
        case Node.VariableAccess v -> System.out.println(prefix + "VARIABLE " + v.name());
        case Node.FieldAccess fa -> {
            System.out.println(prefix + "FIELD ACCESS of field " + fa.fieldName());
            printLabeledChild("base:", fa.base(), prefix, true);
        }
        case Node.ArrayAccess aa -> {
            System.out.println(prefix + "ARRAY ACCESS []");
            printLabeledChild("base:", aa.base(), prefix, false);
            printLabeledChild("index:", aa.index(), prefix, true);
        }
        case Node.BinaryExpression be -> {
            System.out.println(prefix + "BINARY of operand " + be.op());
            printLabeledChild("left:", be.left(), prefix, false);
            printLabeledChild("right:", be.right(), prefix, true);
        }
        case Node.UnaryExpression ue -> {
            System.out.println(prefix + "UNARY of operand " + ue.op());
            printLabeledChild("operand:", ue.operand(), prefix, true);
        }
        }
    }

    private void printLabeledChild(String label, Node child, String prefix, boolean isLast) {
        String branch = isLast ? "└── " : "├── ";
        // Print the branch and label, then recurse for the child with proper
        // indentation
        System.out.println(prefix + branch + label);
        String nextPrefix = prefix + (isLast ? "    " : "│   ");
        print(child, nextPrefix + "    ");
    }

    private void printChild(Node child, String prefix, boolean isLast) {
        String branch = isLast ? "└── " : "├── ";
        System.out.println(prefix + branch);
        String nextPrefix = prefix + (isLast ? "    " : "│   ");
        print(child, nextPrefix + "    ");
    }

}