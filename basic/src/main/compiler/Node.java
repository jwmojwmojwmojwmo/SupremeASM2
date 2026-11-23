package basic.src.main.compiler;

import java.util.List;

// Represents all possible nodes in the AST
public sealed interface Node {
    // Represents a program consisting of multiple statements
    record Program(List<Node> statements) implements Node {
    }

    // Represents the variable declaration statement, declaring a variable with a
    // name and type (ex: "DECLARE VARIABLE x AS int")
    record VariableDeclaration(String name, String typeName, int line) implements Node {
    }

    // Represents the type declaration statement, declaring a type with a name and
    // size (ex: DECLARE TYPE int SIZE 4)
    record TypeDeclaration(String name, int size, int line) implements Node {
    }

    // Represents an assignment statement, assigning a value to a variable (ex: x <-
    // 42)
    record ValueAssignment(String name, Node value, int line) implements Node {
    }

    // Represents as assignment statement of a value to a field of a struct (ex: x
    // OF Point <- 42)
    record FieldValueAssignment(String structName, String fieldName, Node value, int line) implements Node {
    }

    // Represents a literal number (ex: 42)
    record NumberLiteral(long value, int line) implements Node {
    }

    // Represents a reference of an existing variable (ex: the "x" in y <- x)
    record VariableReference(String name, int line) implements Node {
    }

    // Represents a reference of an existing field of a struct (ex: the "x" in y <-
    // x OF Point)
    record FieldVariableReference(String structName, String fieldName, int line) implements Node {
    }

    // Represents a field inside a struct (ex: "x AS int;")
    record StructField(String name, String typeName, int line) implements Node {
    }

    // Represents the struct declaration, declaring a struct with a name and fields
    // (ex: DECLARE STRUCT Point THEN fields END)
    record StructDeclaration(String name, List<StructField> fields, int line) implements Node {
    }

    // Represents a binary operation (ex: a + b, a << 2, a & b)
    record BinaryExpression(String op, Node left, Node right, int line) implements Node {
    }

    // Represents a unary operation (ex: ~x)
    record UnaryExpression(String op, Node operand, int line) implements Node {
    }
}