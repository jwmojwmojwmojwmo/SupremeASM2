package basic.src.main.compiler;

import java.util.ArrayList;
import java.util.List;

import basic.src.main.exceptions.BadParseException;

public class Parser {
    private final List<Token> tokens;
    private int pos;

    public Parser() {
        this.tokens = new ArrayList<>();
        pos = 0;
    }

    public void setTokens(List<Token> tokens) {
        this.tokens.clear();
        this.tokens.addAll(tokens);
        pos = 0;
    }

    private Token peek() {
        return tokens.get(pos);
    }

    private Token advance() {
        return tokens.get(pos++);
    }

    private boolean is(TokenType.Type t) {
        return peek().type() == t;
    }

    private boolean isOp(String s) {
        return is(TokenType.Type.OPERATOR) && peek().value().equals(s);
    }

    private boolean isKw(String s) {
        return is(TokenType.Type.KEYWORD) && peek().value().equals(s);
    }

    private Token expect(TokenType.Type t) {
        if (!is(t))
            throw new BadParseException("expected " + t + " but found " + peek(), peek().line());
        return advance();
    }

    private void expectOp(String s) {
        if (!isOp(s))
            throw new BadParseException("expected '" + s + "' but found " + peek(), peek().line());
        advance();
    }


    // program contains one or more statements with EOF
    public Node parseProgram() {
        List<Node> statements = new ArrayList<>();
        while (!is(TokenType.Type.EOF))
            statements.add(parseStatement());
        return new Node.Program(statements);
    }

    // a statement is either a DECLARE or an assignment <-
    private Node parseStatement() {
        if (isKw("DECLARE"))
            return parseDeclare();
        if (is(TokenType.Type.IDENTIFIER))
            return parseAssign();
        throw new BadParseException("expected DECLARE or a variable name, found " + peek(), peek().line());
    }

    // a DECLARE statement is "DECLARE" IDENTIFIER "SIZE" NUMBER ";"
    private Node parseDeclare() {
        advance();
        Token name = expect(TokenType.Type.IDENTIFIER);
        if (!isKw("SIZE"))
            throw new BadParseException("expected SIZE but found " + peek(), peek().line());
        advance();
        Token n = expect(TokenType.Type.NUMBER);
        expectOp(";");
        return new Node.DeclareStatement(name.value(), size(n), name.line());
    }

    // an assignment statement is "IDENT" "<-" "NUMBER" ";"
    private Node parseAssign() {
        Token name = advance();
        expectOp("<-");
        Token n = expect(TokenType.Type.NUMBER);
        expectOp(";");
        return new Node.AssignStatement(name.value(), new Node.NumberLiteral(Long.parseLong(n.value()), n.line()), name.line());
    }

    private int size(Token t) {
        try {
            return Integer.parseInt(t.value());
        } catch (NumberFormatException e) {
            throw new BadParseException("size out of range: " + t.value(), t.line());
        }
    }


}