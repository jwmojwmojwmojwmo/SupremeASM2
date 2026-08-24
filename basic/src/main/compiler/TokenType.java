package basic.src.main.compiler;

import java.util.Set;

public final class TokenType {
    public enum Type {
        KEYWORD,
        IDENTIFIER,
        LITERAL,
        OPERATOR,
        UNKNOWN;
    }

    public static final Set<String> KEYWORDS = Set.of(
            "PRINT", "ASM");

}
