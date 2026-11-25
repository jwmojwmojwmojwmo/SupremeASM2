package basic.src.main.compiler;

import java.util.List;

// Represents all possible nodes in the AST
public sealed interface Node {

    record Program(List<Node> statements) implements Node {}

    // Declarations are resolved at compile time and emit no code
    record TypeDeclaration(String name, int size, int line) implements Node {}

    record StructFieldDeclaration(String name, String typeName, int line) implements Node {}

    record StructDeclaration(String name, List<StructFieldDeclaration> fields, int line) implements Node {}

    record VariableDeclaration(String name, String typeName, int line) implements Node {}

    record ArrayDeclaration(String name, String typeName, long length, int line) implements Node {}

    // Statements do not return values
    record AssignmentStatement(Node target, Node value, int line) implements Node {}

    // Expressions return values and can be used in assignments or other expressions
    record BinaryExpression(String op, Node left, Node right, int line) implements Node {}

    record UnaryExpression(String op, Node operand, int line) implements Node {}

    // Nodes that represent values or will be resolved to values
    record NumberLiteral(long value, int line) implements Node {}

    record VariableAccess(String name, int line) implements Node {}

    record FieldAccess(Node base, String fieldName, int line) implements Node {}

    record ArrayAccess(Node base, Node index, int line) implements Node {}
}