package basic.src.main.compiler;

import java.util.List;

// Represents all possible nodes in the AST
public sealed interface Node {
    // Represents a program consisting of multiple statements
    record Program(List<Node> statements) implements Node {
    }

    // Represents the declaration statement, declaring a variable with a name and
    // size
    record DeclareStatement(String name, int size, int line) implements Node {
    }

    // Represents an assignment statement, assigning a value to a variable
    record AssignStatement(String name, Node value, int line) implements Node {
    }

    // Represents a literal number
    record NumberLiteral(long value, int line) implements Node {
    }
}