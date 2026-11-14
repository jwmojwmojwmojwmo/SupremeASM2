package basic.src.main.compiler;

import java.util.Set;

public final class TokenType {
    public enum Type {
        KEYWORD, IDENTIFIER, NUMBER, OPERATOR, EOF
    }

    public static final Set<String> KEYWORDS = Set.of("DECLARE", "SIZE");
}