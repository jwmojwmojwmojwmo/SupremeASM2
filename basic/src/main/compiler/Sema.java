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

    public record Variable(String name, Type type, int offset, int line) {
    }

    public record Result(Map<String, Variable> vars, int frameSize) {
    }

    private static final int MAX_FRAME = 32767;

    public Result analyze(Node.Program program) {
        Map<String, Variable> vars = new LinkedHashMap<>();
        Map<String, Type> types = new HashMap<>();
        int offset = 0;

        // PASS 1: Collect all primitive TYPE definitions
        for (Node n : program.statements()) {
            if (n instanceof Node.TypeDeclaration t) {
                if (t.size() < 1)
                    throw new BadParseException("type size must be at least 1", t.line());
                if (types.containsKey(t.name()))
                    throw new BadParseException("type '" + t.name() + "' already defined", t.line());
                types.put(t.name(), new Type(t.name(), t.size()));
            }
        }

        // PASS 2: Collect all STRUCT definitions
        for (Node n : program.statements()) {
            if (n instanceof Node.StructDeclaration s) {
                if (types.containsKey(s.name()))
                    throw new BadParseException("type '" + s.name() + "' already defined", s.line());

                int currentOffset = 0;
                List<FieldLayout> layouts = new ArrayList<>();
                Set<String> fieldNames = new HashSet<>();
                for (Node.StructField f : s.fields()) {
                    if (!fieldNames.add(f.name()))
                        throw new BadParseException("duplicate field '" + f.name()
                                + "' in struct '" + s.name() + "'", f.line());

                    Type fieldType = types.get(f.typeName());
                    if (fieldType == null)
                        throw new BadParseException("unknown type '" + f.typeName() + "' in struct '" + s.name() + "'",
                                f.line());

                    layouts.add(new FieldLayout(f.name(), fieldType, currentOffset));
                    currentOffset += fieldType.size();
                }
                types.put(s.name(), new Type(s.name(), currentOffset, layouts));
            }
        }

        // PASS 3: process variables and assignments
        for (Node n : program.statements()) {
            switch (n) {
                case Node.TypeDeclaration t -> {
                }
                case Node.StructDeclaration s -> {
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
                }

                case Node.ValueAssignment a -> {
                    // Resolve the type of the variable on the left
                    Type targetType = resolveTargetType(a.name(), null, a.line(), vars);
                    // Validate the value on the right
                    checkRHS(a.value(), targetType, a.line(), vars);
                }

                case Node.FieldValueAssignment fa -> {
                    // Resolve the type of the field on the left
                    Type targetType = resolveTargetType(fa.structName(), fa.fieldName(), fa.line(), vars);
                    // Validate the value on the right
                    checkRHS(fa.value(), targetType, fa.line(), vars);
                }   

                default -> throw new IllegalStateException("not a statement: " + n);

            }
        }

        return new Result(vars, offset);
    }

    // Resolves the target type for an assignment (handles both vars and fields)
    private Type resolveTargetType(String varName, String fieldName, int line, Map<String, Variable> vars) {
        Variable v = vars.get(varName);
        if (v == null)
            throw new BadParseException("undeclared variable '" + varName + "'", line);

        if (fieldName == null) {
            // It's a regular variable assignment
            return v.type();
        } else {
            // It's a field assignment
            if (v.type().fields().isEmpty())
                throw new BadParseException("'" + varName + "' is not a struct", line);
            FieldLayout field = v.type().getField(fieldName);
            if (field == null)
                throw new BadParseException("struct '" + v.type().name() + "' has no field '" + fieldName + "'", line);
            return field.type();
        }
    }

    // Validates the RHS against the target type
    private void checkRHS(Node value, Type targetType, int line, Map<String, Variable> vars) {
        if (value instanceof Node.NumberLiteral num) {
            if (!targetType.fields().isEmpty())
                throw new BadParseException("cannot assign a literal to the struct '"
                        + targetType.name() + "'", line);
            long bits = Math.min(targetType.size(), 4) * 8L;
            long half = 1L << (bits - 1);
            if (num.value() < -half || num.value() >= half)
                throw new BadParseException("value " + num.value() + " does not fit in "
                        + targetType.name() + " (" + targetType.size() + " byte(s), range " + (-half) + ".."
                        + (half - 1) + ")", line);

        } else if (value instanceof Node.VariableReference ref) {
            Variable v_src = vars.get(ref.name());
            if (v_src == null)
                throw new BadParseException("undeclared variable '" + ref.name() + "'", ref.line());
            checkAssignable(v_src.type(), targetType, line);

        } else if (value instanceof Node.FieldVariableReference ref) {
            Variable v_src = vars.get(ref.structName());
            if (v_src == null)
                throw new BadParseException("undeclared variable '" + ref.structName() + "'", ref.line());
            if (v_src.type().fields().isEmpty())
                throw new BadParseException("'" + ref.structName() + "' is not a struct", ref.line());

            FieldLayout srcField = v_src.type().getField(ref.fieldName());
            if (srcField == null)
                throw new BadParseException(
                        "struct '" + v_src.type().name() + "' has no field '" + ref.fieldName() + "'", ref.line());
            checkAssignable(srcField.type(), targetType, line);
        } else if (value instanceof Node.BinaryExpression || value instanceof Node.UnaryExpression) {
            Type exprType = inferExprType(value, vars);
            checkAssignable(exprType, targetType, line);
        } else {
            throw new IllegalStateException("unexpected value node: " + value);
        }
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

    // returns the overal type of the expression
    private Type inferExprType(Node value, Map<String, Variable> vars) {
        if (value instanceof Node.NumberLiteral) {
            return null; // untyped literal
        }
        if (value instanceof Node.VariableReference ref) {
            Variable v = vars.get(ref.name());
            if (v == null)
                throw new BadParseException("undeclared variable '" + ref.name() + "'", ref.line());
            return v.type();
        }
        if (value instanceof Node.FieldVariableReference ref) {
            Variable v = vars.get(ref.structName());
            if (v == null)
                throw new BadParseException("undeclared variable '" + ref.structName() + "'", ref.line());
            if (v.type().fields().isEmpty())
                throw new BadParseException("'" + ref.structName() + "' is not a struct", ref.line());
            FieldLayout field = v.type().getField(ref.fieldName());
            if (field == null)
                throw new BadParseException("struct '" + v.type().name() + "' has no field '"
                        + ref.fieldName() + "'", ref.line());
            return field.type();
        }
        if (value instanceof Node.BinaryExpression expr) {
            Type leftType = inferExprType(expr.left(), vars);
            Type rightType = inferExprType(expr.right(), vars);

            // Resolve operand type: literals adopt the typed side
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
            if (expr.op().equals("<<") || expr.op().equals(">>")) {
                if (expr.right() instanceof Node.NumberLiteral n && (n.value() < 0 || n.value() > 31))
                    throw new BadParseException("shift count " + n.value() + " outside 0..31", expr.line());
            }
            return operandType;
        }
        if (value instanceof Node.UnaryExpression u) {
            Type t = inferExprType(u.operand(), vars);
            checkOperandType(t, u.line());
            return t;
        }
        throw new IllegalStateException("unexpected value node: " + value);
    }

    // type checking
    private void checkAssignable(Type src, Type tgt, int line) {
        if (!src.name().equals(tgt.name()))
            throw new BadParseException("cannot assign " + src.name() + " to " + tgt.name(), line);
    }
}