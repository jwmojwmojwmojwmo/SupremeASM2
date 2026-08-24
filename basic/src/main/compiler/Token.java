package basic.src.main.compiler;

public class Token {
    private final TokenType.Type type;
    private final String value;

    public Token(TokenType.Type type, String word) {
        this.type = type;
        this.value = word;
    }

    public TokenType.Type getType() {
        return type;
    }

    public String getValue() {
        return value;
    }
}