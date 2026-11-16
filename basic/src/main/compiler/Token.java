package basic.src.main.compiler;

// Represents a token in the compiler. A token is a single unit of meaning in the source code
public record Token(TokenType.Type type, String value, int line) {
    @Override
    public String toString() {
        return type + "('" + value + "')";
    }
}