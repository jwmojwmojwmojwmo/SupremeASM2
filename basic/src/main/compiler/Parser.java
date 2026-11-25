package basic.src.main.compiler;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Supplier;

import basic.src.main.exceptions.BadParseException;

// Represents the Parser in the compiler. The job is to turn a list of tokens into an AST,
// checking grammar rules (to create valid AST), but not semantics.
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

        if (is(TokenType.Type.IDENTIFIER))
            return parseAssign();

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
        if (isKw("ARRAY"))
            return parseArrayDecl();
        throw new BadParseException("expected VARIABLE, TYPE, STRUCT, or ARRAY after DECLARE", peek().line());
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
        List<Node.StructFieldDeclaration> fields = new ArrayList<>();
        while (!isKw("END")) {
            Token fName = expect(TokenType.Type.IDENTIFIER);
            expectKw("AS");
            Token fType = expect(TokenType.Type.IDENTIFIER);
            expectOp(";");
            fields.add(new Node.StructFieldDeclaration(fName.value(), fType.value(), fName.line()));
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

    private Node parseArrayDecl() {
        advance(); // ARRAY
        Token name = expect(TokenType.Type.IDENTIFIER);
        expectKw("AS");
        Token typeName = expect(TokenType.Type.IDENTIFIER);

        Token len = expect(TokenType.Type.IDENTIFIER);
        if (!len.value().equalsIgnoreCase("LENGTH"))
            throw new BadParseException("expected LENGTH but found " + len, len.line());

        Token n = expect(TokenType.Type.NUMBER);
        expectOp(";");

        long length;
        try {
            length = Long.parseLong(n.value());
        } catch (NumberFormatException e) {
            throw new BadParseException("length out of range: " + n.value(), n.line());
        }

        return new Node.ArrayDeclaration(name.value(), typeName.value(), length, name.line());
    }

    private Node parseAssign() {
        int line = peek().line();
        Node target = parseExpression();
        expectOp("<-");
        Node value = parseExpression();
        expectOp(";");
        return new Node.AssignmentStatement(target, value, line);
    }

    // "recursively" parse an expression by calling a parse method for each level of
    // precedence (pre order traversal)
    private Node parseExpression() {
        return parseBitOr();
    }

    // follows Order of operations: () -> * / % -> + - -> << >> -> & -> ^ -> |
    private Node parseBitOr() {
        return parseLeftAssoc(this::parseBitXor, "|");
    }

    private Node parseBitXor() {
        return parseLeftAssoc(this::parseBitAnd, "^");
    }

    private Node parseBitAnd() {
        return parseLeftAssoc(this::parseShift, "&");
    }

    private Node parseShift() {
        return parseLeftAssoc(this::parseAdditive, "<<", ">>");
    }

    private Node parseAdditive() {
        return parseLeftAssoc(this::parseTerm, "+", "-");
    }

    private Node parseTerm() {
        return parseLeftAssoc(this::parseFactor, "*", "/", "%");
    }

    // wait this code is kinda goated
    private Node parseLeftAssoc(Supplier<Node> sub, String... ops) {
        Node left = sub.get();
        while (true) {
            String op = null;
            for (String candidate : ops)
                if (isOp(candidate)) {
                    op = candidate;
                    break;
                }
            if (op == null)
                return left;
            int line = advance().line();
            left = fold(op, left, sub.get(), line);
        }
    }

    // factor = unary | '(' expression ')' | NUMBER | access chain
    private Node parseFactor() {
        if (isOp("~") || isOp("-")) {
            Token op = advance();
            Node operand = parseFactor();
            if (operand instanceof Node.NumberLiteral n) {
                long result;
                try {
                    result = switch (op.value()) {
                        case "~" -> ~n.value();
                        case "-" -> Math.negateExact(n.value());
                        default -> throw new IllegalStateException("unknown unary operator " + op);
                    };
                } catch (ArithmeticException e) {
                    throw new BadParseException("constant expression out of range", op.line());
                }
                return new Node.NumberLiteral(result, op.line());
            }
            return new Node.UnaryExpression(op.value(), operand, op.line());
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

        if (is(TokenType.Type.IDENTIFIER))
            return parseAccess();

        throw new BadParseException("expected a number, variable, or '('", peek().line());
    }

    // an access chain: a variable with optional indexing, or field OF another chain.
    // brackets are postfix and bind tighter than OF. OF recurses right-associatively,
    // so x OF topLeft OF r works and is x OF (topLeft OF r)
    private Node parseAccess() {
        Token first = expect(TokenType.Type.IDENTIFIER);

        if (isKw("OF")) {
            advance();
            Node base = parseAccess();
            return new Node.FieldAccess(base, first.value(), first.line());
        }

        Node node = new Node.VariableAccess(first.value(), first.line());
        while (isOp("[")) {
            advance();
            Node index = parseExpression();
            expectOp("]");
            node = new Node.ArrayAccess(node, index, first.line());
        }
        return node;
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
                    case "&" -> l.value() & r.value();
                    case "|" -> l.value() | r.value();
                    case "^" -> l.value() ^ r.value();
                    case "<<" -> {
                        if (r.value() < 0 || r.value() > 31)
                            throw new BadParseException("shift count " + r.value() + " outside 0..31", line);
                        long res = l.value() << r.value();
                        // larp as Math.exact
                        if ((res >> r.value()) != l.value())
                            throw new BadParseException("constant expression out of range", line);
                        yield res;
                    }
                    case ">>" -> {
                        if (r.value() < 0 || r.value() > 31)
                            throw new BadParseException("shift count " + r.value() + " outside 0..31", line);
                        yield l.value() >> r.value();
                    }
                    default -> throw new IllegalStateException("unknown operator " + op);
                };
            } catch (ArithmeticException e) {
                throw new BadParseException("constant expression out of range", line);
            }
            return new Node.NumberLiteral(result, line);
        }
        return new Node.BinaryExpression(op, left, right, line);
    }

    private int size(Token t) {
        try {
            return Integer.parseInt(t.value());
        } catch (NumberFormatException e) {
            throw new BadParseException("size out of range: " + t.value(), t.line());
        }
    }
}