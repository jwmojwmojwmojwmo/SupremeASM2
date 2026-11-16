package basic.src.main.compiler;

import java.util.ArrayList;
import java.util.List;

import basic.src.main.exceptions.BadParseException;

// Represents the Parser in the compiler. The job is to turn a list of tokens into an AST, checking grammar rules (to create valid AST), but not semantics.
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

    private void expectKw(String s) {
        if (!isKw(s))
            throw new BadParseException("expected '" + s + "' but found " + peek(), peek().line());
        advance();
    }

    // program contains one or more statements with EOF
    public Node.Program parseProgram() {
        List<Node> statements = new ArrayList<>();
        while (!is(TokenType.Type.EOF))
            statements.add(parseStatement());
        return new Node.Program(statements);
    }

    // a statement is either a DECLARE or an assignment <-
    private Node parseStatement() {
        if (isKw("DECLARE"))
            return parseDeclare();

        if (is(TokenType.Type.IDENTIFIER)) {
            boolean isFieldAssign = false;
            if (pos + 1 < tokens.size()) {
                Token next = tokens.get(pos + 1);
                if (next.type() == TokenType.Type.KEYWORD && next.value().equals("OF")) {
                    isFieldAssign = true;
                }
            }

            if (isFieldAssign)
                return parseFieldAssign();
            return parseAssign();
        }

        throw new BadParseException("expected DECLARE or a variable name, found " + peek(), peek().line());
    }

    private Node parseDeclare() {
        advance();

        if (isKw("VARIABLE"))
            return parseVariableDecl();
        if (isKw("TYPE"))
            return parseTypeDecl();
        if (isKw("STRUCT"))
            return parseStructDecl();

        throw new BadParseException("expected VARIABLE, TYPE, or STRUCT after DECLARE", peek().line());
    }

    private Node parseTypeDecl() {
        advance();
        Token name = expect(TokenType.Type.IDENTIFIER);
        expectKw("SIZE");
        Token n = expect(TokenType.Type.NUMBER);
        expectOp(";");
        return new Node.TypeDeclaration(name.value(), size(n), name.line());
    }

    private Node parseStructDecl() {
        advance();
        Token name = expect(TokenType.Type.IDENTIFIER);

        expectKw("THEN");

        List<Node.StructField> fields = new ArrayList<>();
        while (!isKw("END")) {
            Token fName = expect(TokenType.Type.IDENTIFIER);
            expectKw("AS");
            Token fType = expect(TokenType.Type.IDENTIFIER);
            expectOp(";"); 
            fields.add(new Node.StructField(fName.value(), fType.value(), fName.line()));
        }
        advance();
        expectOp(";"); 

        return new Node.StructDeclaration(name.value(), fields, name.line());
    }

    private Node parseVariableDecl() {
        advance();
        Token name = expect(TokenType.Type.IDENTIFIER);

        expectKw("AS");

        Token typeName = expect(TokenType.Type.IDENTIFIER);
        expectOp(";");

        return new Node.VariableDeclaration(name.value(), typeName.value(), name.line());
    }

    private Node parseAssign() {
        Token name = advance();
        expectOp("<-");
        Node value = parseValue();
        expectOp(";");
        return new Node.ValueAssignment(name.value(), value, name.line());
    }

    private Node parseFieldAssign() {
        Token fieldName = advance();
        expectKw("OF"); // Cleaned up!
        Token varName = expect(TokenType.Type.IDENTIFIER);
        expectOp("<-");
        Node value = parseValue();
        expectOp(";");
        return new Node.FieldValueAssignment(varName.value(), fieldName.value(), value, varName.line());
    }

    private Node parseValue() {
        if (is(TokenType.Type.NUMBER)) {
            Token n = advance();
            long number;
            try {
                number = Long.parseLong(n.value());
            } catch (NumberFormatException e) {
                throw new BadParseException("number out of range: " + n.value(), n.line());
            }
            return new Node.NumberLiteral(number, n.line());
        } else if (is(TokenType.Type.IDENTIFIER)) {
            Token first = advance();
            if (isKw("OF")) {
                advance();
                Token varName = expect(TokenType.Type.IDENTIFIER);
                return new Node.FieldVariableReference(varName.value(), first.value(), first.line());
            } else {
                return new Node.VariableReference(first.value(), first.line());
            }
        } else {
            throw new BadParseException("expected a number or a variable name", peek().line());
        }
    }

    private int size(Token t) {
        try {
            return Integer.parseInt(t.value());
        } catch (NumberFormatException e) {
            throw new BadParseException("size out of range: " + t.value(), t.line());
        }
    }

}