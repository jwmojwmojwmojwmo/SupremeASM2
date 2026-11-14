package basic.src.main.compiler;

import java.util.ArrayList;
import java.util.List;

public class Lexer {
    private String src;
    private int pos;
    private int line;

    public List<Token> tokenize(String source) {
        src = source;
        pos = 0;
        line = 1;
        List<Token> tokens = new ArrayList<>();

        while (pos < src.length()) {
            char c = src.charAt(pos);

            // 1. whitespace -> skip
            if (c == ' ' || c == '\t' || c == '\r') { pos++; continue; }
            if (c == '\n') { pos++; line++; continue; }

            // 2. // comment -> skip to end of line
            if (c == '/' && pos + 1 < src.length() && src.charAt(pos + 1) == '/') {
                while (pos < src.length() && src.charAt(pos) != '\n') pos++;
                continue;
            }

            // 3. number -> eat all digits
            if (Character.isDigit(c)) {
                int start = pos;
                while (pos < src.length() && Character.isDigit(src.charAt(pos))) pos++;
                tokens.add(new Token(TokenType.Type.NUMBER, src.substring(start, pos), line));
                continue;
            }

            // 4. word -> keyword or identifier
            if (Character.isLetter(c) || c == '_') {
                int start = pos;
                while (pos < src.length()
                        && (Character.isLetterOrDigit(src.charAt(pos)) || src.charAt(pos) == '_')) pos++;
                String word = src.substring(start, pos);
                String upper = word.toUpperCase();
                if (TokenType.KEYWORDS.contains(upper))
                    tokens.add(new Token(TokenType.Type.KEYWORD, upper, line));
                else
                    tokens.add(new Token(TokenType.Type.IDENTIFIER, word, line));
                continue;
            }

            // 5. '<-' is ONE operator, not two
            if (c == '<' && pos + 1 < src.length() && src.charAt(pos + 1) == '-') {
                tokens.add(new Token(TokenType.Type.OPERATOR, "<-", line));
                pos += 2;
                continue;
            }

            // 6. anything else -> one-character operator
            tokens.add(new Token(TokenType.Type.OPERATOR, String.valueOf(c), line));
            pos++;
        }

        tokens.add(new Token(TokenType.Type.EOF, "", line));
        return tokens;
    }

    // temporary test harness
    public static void main(String[] args) {
        String src = "DECLARE x SIZE 4;\nx <- 5;  // hi\n";
        for (Token t : new Lexer().tokenize(src)) System.out.println(t);
    }
}