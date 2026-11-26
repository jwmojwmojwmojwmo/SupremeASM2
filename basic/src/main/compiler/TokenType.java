package basic.src.main.compiler;

import java.util.Set;

// Represents the types of tokens in the compiler. A token is a single unit of meaning in the source code
public final class TokenType {
    public enum Type {
        KEYWORD, IDENTIFIER, NUMBER, CHAR, STRING, OPERATOR, EOF
    }

    public static final Set<String> KEYWORDS = Set.of(
            "DECLARE", "SIZE", "VARIABLE", "TYPE", "AS", "STRUCT", "THEN", "END", "OF", "ARRAY", "PRINT", "CONST");
}