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
        Node value = parseExpression();
        expectOp(";");
        return new Node.ValueAssignment(name.value(), value, name.line());
    }

    private Node parseFieldAssign() {
        Token fieldName = advance();
        expectKw("OF");
        Token varName = expect(TokenType.Type.IDENTIFIER);
        expectOp("<-");
        Node value = parseExpression();
        expectOp(";");
        return new Node.FieldValueAssignment(varName.value(), fieldName.value(), value, varName.line());
    }

    // follows BEDMAS
    // expression = term (('+' | '-') term)*
    private Node parseExpression() {
        Node left = parseTerm();
        while (isOp("+") || isOp("-")) {
            Token op = advance();
            Node right = parseTerm();
            left = fold(op.value(), left, right, op.line());
        }
        return left;
    }

    // term = factor (('*' | '/' | '%') factor)*
    private Node parseTerm() {
        Node left = parseFactor();
        while (isOp("*") || isOp("/") || isOp("%")) {
            Token op = advance();
            Node right = parseFactor();
            left = fold(op.value(), left, right, op.line());
        }
        return left;
    }

    // factor = NUMBER | IDENTIFIER [OF IDENTIFIER] | '(' expression ')' | '-'
    // NUMBER
    private Node parseFactor() {
        if (isOp("-")) {
            advance();
            Token n = expect(TokenType.Type.NUMBER);
            long number;
            try {
                number = -Long.parseLong(n.value());
            } catch (NumberFormatException e) {
                throw new BadParseException("number out of range: -" + n.value(), n.line());
            }
            return new Node.NumberLiteral(number, n.line());
        }
        if (isOp("(")) {
            advance();
            Node inner = parseExpression();
            expectOp(")");
            return inner;
        }
        if (is(TokenType.Type.NUMBER)) {
            Token n = advance();
            long number;
            try {
                number = Long.parseLong(n.value());
            } catch (NumberFormatException e) {
                throw new BadParseException("number out of range: " + n.value(), n.line());
            }
            return new Node.NumberLiteral(number, n.line());
        }
        if (is(TokenType.Type.IDENTIFIER)) {
            Token first = advance();
            if (isKw("OF")) {
                advance();
                Token varName = expect(TokenType.Type.IDENTIFIER);
                return new Node.FieldVariableReference(varName.value(), first.value(), first.line());
            }
            return new Node.VariableReference(first.value(), first.line());
        }
        throw new BadParseException("expected a number, variable, or '('", peek().line());
    }

    // If both sides are literals, compute now; otherwise build a BinaryExpression.
    private Node fold(String op, Node left, Node right, int line) {
        if (left instanceof Node.NumberLiteral l && right instanceof Node.NumberLiteral r) {
            long result;
            try {
                result = switch (op) {
                    case "+" -> Math.addExact(l.value(), r.value());
                    case "-" -> Math.subtractExact(l.value(), r.value());
                    case "*" -> Math.multiplyExact(l.value(), r.value());
                    case "/" -> {
                        if (r.value() == 0)
                            throw new BadParseException("division by zero in constant expression", line);
                        yield l.value() / r.value();
                    }
                    case "%" -> {
                        if (r.value() == 0)
                            throw new BadParseException("division by zero in constant expression", line);
                        yield l.value() % r.value();
                    }
                    default -> throw new IllegalStateException("unknown operator " + op);
                };
            } catch (ArithmeticException e) {
                throw new BadParseException("constant expression out of range", line);
            }
            return new Node.NumberLiteral(result, line);
        }
        return new Node.ArithmeticExpression(op, left, right, line);
    }

    private int size(Token t) {
        try {
            return Integer.parseInt(t.value());
        } catch (NumberFormatException e) {
            throw new BadParseException("size out of range: " + t.value(), t.line());
        }
    }

}