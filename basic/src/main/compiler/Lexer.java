package basic.src.main.compiler;

import java.util.ArrayList;
import java.util.List;

import basic.src.main.exceptions.BadParseException;

// Represents the Lexer in the compiler. The job is to turn a string of source code into a list of tokens
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
            if (c == ' ' || c == '\t' || c == '\r') {
                pos++;
                continue;
            }
            if (c == '\n') {
                pos++;
                line++;
                continue;
            }

            // 2. // comment -> skip to end of line
            if (c == '/' && pos + 1 < src.length() && src.charAt(pos + 1) == '/') {
                while (pos < src.length() && src.charAt(pos) != '\n')
                    pos++;
                continue;
            }

            // 3. number -> eat all digits
            if (Character.isDigit(c)) {
                int start = pos;
                while (pos < src.length() && Character.isDigit(src.charAt(pos)))
                    pos++;
                tokens.add(new Token(TokenType.Type.NUMBER, src.substring(start, pos), line));
                continue;
            }

            // 4. word -> keyword or identifier
            if (Character.isLetter(c) || c == '_') {
                int start = pos;
                while (pos < src.length()
                        && (Character.isLetterOrDigit(src.charAt(pos)) || src.charAt(pos) == '_'))
                    pos++;
                String word = src.substring(start, pos);
                String upper = word.toUpperCase();
                if (TokenType.KEYWORDS.contains(upper))
                    tokens.add(new Token(TokenType.Type.KEYWORD, upper, line));
                else
                    tokens.add(new Token(TokenType.Type.IDENTIFIER, word, line));
                continue;
            }

            // 5. two-character operators -> <-, <<, etc.
            if (c == '<' && pos + 1 < src.length()) {
                if (src.charAt(pos + 1) == '-') {
                    tokens.add(new Token(TokenType.Type.OPERATOR, "<-", line));
                    pos += 2;
                    continue;
                } else if (src.charAt(pos + 1) == '<') {
                    tokens.add(new Token(TokenType.Type.OPERATOR, "<<", line));
                    pos += 2;
                    continue;
                }
            } else if (c == '>' && pos + 1 < src.length() && src.charAt(pos + 1) == '>') {
                tokens.add(new Token(TokenType.Type.OPERATOR, ">>", line));
                pos += 2;
                continue;
            }

            // 6. character -> one ascii character surrounded by single quotes
            if (c == '\'') {
                pos++; // opening quote

                if (pos >= src.length() || src.charAt(pos) == '\n')
                    throw new BadParseException("unterminated character literal", line);

                char ch = src.charAt(pos);
                char value;
                if (ch == '\\') {
                    pos++;
                    if (pos >= src.length() || src.charAt(pos) == '\n')
                        throw new BadParseException("unterminated character literal", line);
                    value = switch (src.charAt(pos)) {
                        case 'n' -> '\n';
                        case 't' -> '\t';
                        case 'r' -> '\r';
                        case '0' -> '\0';
                        case '\\' -> '\\';
                        case '\'' -> '\'';
                        default -> throw new BadParseException(
                                "unknown escape sequence '" + src.charAt(pos) + "'", line);
                    };
                } else {
                    value = ch;
                }
                pos++; // consume the character or the escape target

                if (pos >= src.length())
                    throw new BadParseException("unterminated character literal", line);
                if (src.charAt(pos) != '\'')
                    throw new BadParseException("character literal must contain exactly one character", line);
                pos++; // closing quote

                tokens.add(new Token(TokenType.Type.CHAR, String.valueOf(value), line));
                continue;
            }

            // 7. string literal -> scan escape-aware, preserve raw contents
            if (c == '"') {
                pos++; 
                StringBuilder raw = new StringBuilder();
                while (true) {
                    if (pos >= src.length() || src.charAt(pos) == '\n')
                        throw new BadParseException("unterminated string literal", line);
                    char ch = src.charAt(pos);
                    if (ch == '"') { pos++; break; }
                    if (ch == '\\') {
                        raw.append(ch); pos++;
                        if (pos >= src.length() || src.charAt(pos) == '\n')
                            throw new BadParseException("unterminated string literal", line);
                    }
                    raw.append(src.charAt(pos)); pos++;
                }
                tokens.add(new Token(TokenType.Type.STRING, raw.toString(), line));
                continue;
            }

            // 8. anything else -> one-character operator
            tokens.add(new Token(TokenType.Type.OPERATOR, String.valueOf(c), line));
            pos++;
        }

        tokens.add(new Token(TokenType.Type.EOF, "", line));
        return tokens;
    }
}