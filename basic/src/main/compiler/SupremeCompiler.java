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
            print(program, 0);
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
        String asm = codeGen.generate(program, semaResult);
        try {
            Files.writeString(Path.of(fileName + ".sasm"), asm);
            System.out.println("Generated SupremeASM in " + fileName + ".sasm");
        } catch (IOException e) {
            e.printStackTrace();
            return false;
        }
        return true;
    }

    private void print(Node n, int depth) {
        String pad = "     ".repeat(depth);
        switch (n) {
        case Node.Program p -> {
            System.out.println(pad + "PROGRAM");
            for (Node s : p.statements())
                print(s, depth + 1);
        }
        case Node.TypeDeclaration t -> System.out.println(pad + "DECLARE TYPE " + t.name() + " size=" + t.size());
        case Node.VariableDeclaration d -> System.out
                .println(pad + "DECLARE VARIABLE " + d.name() + " type=" + d.typeName());
        case Node.ValueAssignment a -> {
            System.out.println(pad + "ASSIGN " + a.name());
            print(a.value(), depth + 1);
        }
        case Node.NumberLiteral num -> System.out.println(pad + "NUMBER " + num.value());
        case Node.VariableReference v -> System.out.println(pad + "VARIABLE " + v.name());
        case Node.StructField f -> System.out.println(pad + "FIELD " + f.name() + " type=" + f.typeName());
        case Node.StructDeclaration s -> {
            System.out.println(pad + "DECLARE STRUCT " + s.name());
            for (Node.StructField f : s.fields())
                print(f, depth + 1);
        }
        case Node.FieldValueAssignment fa -> {
            System.out.println(pad + "ASSIGN FIELD " + fa.fieldName() + " OF STRUCT " + fa.structName());
            print(fa.value(), depth + 1);
        }
        case Node.FieldVariableReference fvr -> {
            System.out.println(pad + "FIELD VARIABLE " + fvr.fieldName() + " OF STRUCT " + fvr.structName());
        }
        case Node.BinaryExpression ae -> {
            System.out.println(pad + "ARITHMETIC " + ae.op());
            print(ae.left(), depth + 1);
            print(ae.right(), depth + 1);
        }
        case Node.UnaryExpression ue -> {
            System.out.println(pad + "UNARY " + ue.op());
            print(ue.operand(), depth + 1);
        }
        }
    }
}
