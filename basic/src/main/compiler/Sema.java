package basic.src.main.compiler;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;

import basic.src.main.exceptions.BadParseException;

public class Sema {

    public record FieldLayout(String name, Type type, int offset) {
    }

    public record Type(String name, int size, List<FieldLayout> fields) {
        public Type(String name, int size) {
            this(name, size, List.of());
        }

        public FieldLayout getField(String fieldName) {
            for (FieldLayout f : fields) {
                if (f.name().equals(fieldName))
                    return f;
            }
            return null;
        }
    }

    // arrayLength == 0 means its not an array, otherwise it is in fact an array
    public record Variable(String name, Type type, int offset, int line, long arrayLength) {
        public Variable(String name, Type type, int offset, int line) {
            this(name, type, offset, line, 0);
        }

        public boolean isArray() {
            return arrayLength > 0;
        }
    }

    // program is the desugared tree for CodeGen to consume
    public record Result(Map<String, Variable> vars, int frameSize, Node.Program program) {
    }

    private static final int MAX_FRAME = 32767;

    public Result analyze(Node.Program program) {
        Map<String, Variable> vars = new LinkedHashMap<>();
        Map<String, Type> types = new HashMap<>();
        List<Node> outStatements = new ArrayList<>();
        int offset = 0;

        for (Node n : program.statements()) {
            switch (n) {
                case Node.TypeDeclaration t -> {
                    if (t.size() < 1)
                        throw new BadParseException("type size must be at least 1", t.line());
                    if (types.containsKey(t.name()))
                        throw new BadParseException("type '" + t.name() + "' already defined", t.line());
                    types.put(t.name(), new Type(t.name(), t.size()));
                    outStatements.add(t);
                }

                case Node.StructDeclaration s -> {
                    if (types.containsKey(s.name()))
                        throw new BadParseException("type '" + s.name() + "' already defined", s.line());

                    int currentOffset = 0;
                    List<FieldLayout> layouts = new ArrayList<>();
                    Set<String> fieldNames = new HashSet<>();
                    for (Node.StructFieldDeclaration f : s.fields()) {
                        if (!fieldNames.add(f.name()))
                            throw new BadParseException("duplicate field '" + f.name()
                                    + "' in struct '" + s.name() + "'", f.line());

                        Type fieldType = types.get(f.typeName());
                        if (fieldType == null)
                            throw new BadParseException(
                                    "unknown type '" + f.typeName() + "' in struct '" + s.name() + "'", f.line());

                        layouts.add(new FieldLayout(f.name(), fieldType, currentOffset));
                        currentOffset += fieldType.size();
                    }
                    types.put(s.name(), new Type(s.name(), currentOffset, layouts));
                    outStatements.add(s);
                }

                case Node.VariableDeclaration d -> {
                    if (vars.containsKey(d.name())) {
                        Variable old = vars.get(d.name());
                        throw new BadParseException(
                                "variable '" + d.name() + "' already declared on line " + old.line(), d.line());
                    }
                    Type vType = types.get(d.typeName());
                    if (vType == null)
                        throw new BadParseException("unknown type '" + d.typeName() + "'", d.line());
                    if (vType.size() > MAX_FRAME - offset)
                        throw new BadParseException("program exceeds the " + MAX_FRAME + "-byte stack frame", d.line());

                    vars.put(d.name(), new Variable(d.name(), vType, offset, d.line()));
                    offset += vType.size();
                    outStatements.add(d);
                }

                case Node.ArrayDeclaration d -> {
                    if (vars.containsKey(d.name())) {
                        Variable old = vars.get(d.name());
                        throw new BadParseException(
                                "variable '" + d.name() + "' already declared on line " + old.line(), d.line());
                    }
                    Type elemType = types.get(d.typeName());
                    if (elemType == null)
                        throw new BadParseException("unknown type '" + d.typeName() + "'", d.line());
                    if (d.length() < 1)
                        throw new BadParseException("array length must be at least 1", d.line());

                    long total = d.length() * (long) elemType.size();
                    if (total > MAX_FRAME - offset)
                        throw new BadParseException("program exceeds the " + MAX_FRAME + "-byte stack frame", d.line());

                    vars.put(d.name(), new Variable(d.name(), elemType, offset, d.line(), d.length()));
                    offset += (int) total;
                    outStatements.add(d);
                }

                case Node.AssignmentStatement a -> {
                    // desugar first so LEN becomes a literal before the lvalue check
                    Node target = desugar(a.target(), vars);
                    Node value = desugar(a.value(), vars);

                    if (!(target instanceof Node.VariableAccess
                            || target instanceof Node.FieldAccess
                            || target instanceof Node.ArrayAccess))
                        throw new BadParseException("cannot assign to this expression", a.line());

                    if (target instanceof Node.VariableAccess va) {
                        Variable v = vars.get(va.name());
                        if (v == null)
                            throw new BadParseException("undeclared variable '" + va.name() + "'", a.line());
                        if (v.isArray())
                            throw new BadParseException("cannot assign to whole array '" + va.name() + "'", a.line());
                    }

                    Type targetType = inferExprType(target, vars);
                    checkRHS(value, targetType, a.line(), vars);
                    outStatements.add(new Node.AssignmentStatement(target, value, a.line()));
                }
                case Node.PrintStatement p -> {
                    Node target = p.value();

                    if (target instanceof Node.StringLiteral s) {
                        // just push to stack for now
                        if (s.value().isEmpty())
                            throw new BadParseException("empty string literal", s.line());
                        outStatements.add(new Node.PrintStatement(target, p.line()));
                    } else {
                        target = desugar(target, vars);

                        if (!(target instanceof Node.VariableAccess
                                || target instanceof Node.FieldAccess
                                || target instanceof Node.ArrayAccess))
                            throw new BadParseException(
                                    "PRINT needs a place in memory (variable, field, or element)", p.line());

                        if (target instanceof Node.VariableAccess va) {
                            if (!vars.containsKey(va.name()))
                                throw new BadParseException("undeclared variable '" + va.name() + "'", p.line());
                        } else {
                            inferExprType(target, vars);
                        }

                        outStatements.add(new Node.PrintStatement(target, p.line()));
                    }
                }

                default -> throw new IllegalStateException("not a statement: " + n);
            }
        }

    return new Result(vars,offset,new Node.Program(outStatements));

    }

    private Node desugar(Node n, Map<String, Variable> vars) {
        if (n instanceof Node.FieldAccess fa) {
            Node base = desugar(fa.base(), vars);
            Variable arr = arrayVar(base, vars);
            if (arr != null && fa.fieldName().equalsIgnoreCase("LENGTH"))
                return new Node.NumberLiteral(arr.arrayLength(), fa.line());
            return new Node.FieldAccess(base, fa.fieldName(), fa.line());
        }
        if (n instanceof Node.ArrayAccess aa)
            return new Node.ArrayAccess(desugar(aa.base(), vars), desugar(aa.index(), vars), aa.line());
        if (n instanceof Node.BinaryExpression be)
            return new Node.BinaryExpression(be.op(), desugar(be.left(), vars), desugar(be.right(), vars), be.line());
        if (n instanceof Node.UnaryExpression ue)
            return new Node.UnaryExpression(ue.op(), desugar(ue.operand(), vars), ue.line());
        return n;
    }

    // return the variable if the base is a VariableAccess to an array, otherwise
    // null
    private Variable arrayVar(Node base, Map<String, Variable> vars) {
        if (base instanceof Node.VariableAccess va) {
            Variable v = vars.get(va.name());
            if (v != null && v.isArray())
                return v;
        }
        return null;
    }

    // Validates the RHS against the target type
    private void checkRHS(Node value, Type targetType, int line, Map<String, Variable> vars) {
        if (value instanceof Node.NumberLiteral num) {
            if (!targetType.fields().isEmpty())
                throw new BadParseException("cannot assign a literal to the struct '"
                        + targetType.name() + "'", line);
            checkLiteralFits(num.value(), targetType, line);
        } else {
            Type srcType = inferExprType(value, vars);
            checkAssignable(srcType, targetType, line);
        }
    }

    // Literals are untyped, so they must fit in whatever type they end up adopting
    private void checkLiteralFits(long value, Type type, int line) {
        long bits = Math.min(type.size(), 4) * 8L;
        long half = 1L << (bits - 1);
        if (value < -half || value >= half)
            throw new BadParseException("value " + value + " does not fit in "
                    + type.name() + " (" + type.size() + " byte(s), range " + (-half) + ".."
                    + (half - 1) + ")", line);
    }

    // arithmetic/bitwise only on 1- and 4-byte primitives natively
    private void checkOperandType(Type t, int line) {
        if (t == null)
            return; // untyped literal
        if (!t.fields().isEmpty())
            throw new BadParseException("cannot do arithmetic on struct '" + t.name() + "'", line);
        if (t.size() != 1 && t.size() != 4)
            throw new BadParseException("cannot do arithmetic on " + t.size() + "-byte type '"
                    + t.name() + "'", line);
    }

    // Returns the overall type of an expression. Access chains resolve recursively;
    // a bare array used as a value is an error.
    private Type inferExprType(Node value, Map<String, Variable> vars) {
        if (value instanceof Node.NumberLiteral)
            return null; // untyped literal
        if (value instanceof Node.StringLiteral s)
            throw new BadParseException("string literals are only supported in PRINT for now", s.line());
        if (value instanceof Node.VariableAccess ref) {
            Variable v = vars.get(ref.name());
            if (v == null)
                throw new BadParseException("undeclared variable '" + ref.name() + "'", ref.line());
            if (v.isArray())
                throw new BadParseException("cannot use array '" + ref.name() + "' as a value", ref.line());
            return v.type();
        }

        if (value instanceof Node.ArrayAccess aa) {
            Variable arr = resolveArrayBase(aa.base(), vars, aa.line());
            Type idxType = inferExprType(aa.index(), vars);
            if (idxType != null && (!idxType.fields().isEmpty()
                    || (idxType.size() != 1 && idxType.size() != 4)))
                throw new BadParseException("array index must be a 1- or 4-byte integer type", aa.line());
            if (aa.index() instanceof Node.NumberLiteral n
                    && (n.value() < 0 || n.value() >= arr.arrayLength()))
                throw new BadParseException("index " + n.value() + " out of bounds for array '"
                        + arr.name() + "' of length " + arr.arrayLength(), aa.line());
            return arr.type(); // element type
        }

        if (value instanceof Node.FieldAccess fa) {
            Variable arr = arrayVar(fa.base(), vars);
            if (arr != null)
                throw new BadParseException("array '" + arr.name() + "' has no field '"
                        + fa.fieldName() + "' — index it first", fa.line());

            Type baseType = inferExprType(fa.base(), vars);
            if (baseType == null)
                throw new BadParseException("cannot access field '" + fa.fieldName()
                        + "' of a literal", fa.line());
            if (baseType.fields().isEmpty())
                throw new BadParseException("'" + baseType.name() + "' is not a struct", fa.line());
            FieldLayout field = baseType.getField(fa.fieldName());
            if (field == null)
                throw new BadParseException("struct '" + baseType.name() + "' has no field '"
                        + fa.fieldName() + "'", fa.line());
            return field.type();
        }

        if (value instanceof Node.BinaryExpression expr) {
            Type leftType = inferExprType(expr.left(), vars);
            Type rightType = inferExprType(expr.right(), vars);

            Type operandType;
            if (leftType == null)
                operandType = rightType;
            else if (rightType == null)
                operandType = leftType;
            else {
                checkAssignable(leftType, rightType, expr.line());
                operandType = leftType;
            }
            checkOperandType(operandType, expr.line());

            boolean isShift = expr.op().equals("<<") || expr.op().equals(">>");
            if (expr.left() instanceof Node.NumberLiteral n)
                checkLiteralFits(n.value(), operandType, expr.line());
            if (!isShift && expr.right() instanceof Node.NumberLiteral n)
                checkLiteralFits(n.value(), operandType, expr.line());
            if (isShift && expr.right() instanceof Node.NumberLiteral n
                    && (n.value() < 0 || n.value() > 31))
                throw new BadParseException("shift count " + n.value() + " outside 0..31", expr.line());
            return operandType;
        }

        if (value instanceof Node.UnaryExpression u) {
            Type t = inferExprType(u.operand(), vars);
            checkOperandType(t, u.line());
            return t;
        }

        throw new IllegalStateException("unexpected value node: " + value);
    }

    private Variable resolveArrayBase(Node base, Map<String, Variable> vars, int line) {
        if (base instanceof Node.VariableAccess va) {
            Variable v = vars.get(va.name());
            if (v == null)
                throw new BadParseException("undeclared variable '" + va.name() + "'", line);
            if (!v.isArray())
                throw new BadParseException("'" + va.name() + "' is not an array", line);
            return v;
        }
        throw new BadParseException("cannot index this expression", line);
    }

    // nominal type checking
    private void checkAssignable(Type src, Type tgt, int line) {
        if (!src.name().equals(tgt.name()))
            throw new BadParseException("cannot assign " + src.name() + " to " + tgt.name(), line);
    }
}