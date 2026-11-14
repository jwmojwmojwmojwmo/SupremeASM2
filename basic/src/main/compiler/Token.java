package basic.src.main.compiler;

public record Token(TokenType.Type type, String value, int line) {
    @Override
    public String toString() {
        return type + "('" + value + "')";
    }
}