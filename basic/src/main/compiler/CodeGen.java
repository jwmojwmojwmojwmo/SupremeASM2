package basic.src.main.compiler;

// Represents the Code Generator in the compiler. The job is to trust the AST and Sema and generate the SupremeASM based off them
public class CodeGen {

    private final StringBuilder finalASM = new StringBuilder();

    public String generate(Node.Program program, Sema.Result sema) {
        finalASM.setLength(0);
        emit("@version 2");
        emit("");
        // allocate for global vars
        if (sema.frameSize() > 0) {
            emit("ld #" + sema.frameSize() + ", %5;");
            emit("sub %14, %5, %14;");
            emit("");
        }

        for (Node n : program.statements()) {
            switch (n) {
                case Node.ValueAssignment a -> {
                    Sema.Variable v_tgt = sema.vars().get(a.name());
                    emitRHS(a.value(), v_tgt, sema);
                }
                case Node.FieldValueAssignment fa -> {
                    Sema.Variable tgt = fieldVar(fa.structName(), fa.fieldName(), fa.line(), sema);
                    emitRHS(fa.value(), tgt, sema);
                }
                case Node.VariableDeclaration d -> {
                }
                case Node.TypeDeclaration t -> {
                }
                case Node.StructDeclaration s -> {
                }
                default -> System.out.println("Warning: Unhandled statement: " + n);
            }
        }

        emit("");
        emit("halt;");
        return finalASM.toString();
    }

    // Helper method to handle whatever is on the right side of the '<-'
    private void emitRHS(Node value, Sema.Variable v_tgt, Sema.Result sema) {
        if (value instanceof Node.NumberLiteral num) {
            emitStoreLiteral(num.value(), v_tgt);
        } else if (value instanceof Node.BinaryExpression || value instanceof Node.UnaryExpression) {
            int reg = evalExpr(value, 4, sema);
            emitStoreReg(reg, v_tgt);
        } else {
            // VariableReference or FieldVariableReference: copy from that location
            emitStoreVariable(locationOf(value, sema), v_tgt);
        }
    }

    // Every leaf needs one register
    private int need(Node value) {
        if (value instanceof Node.BinaryExpression b) {
            int l = need(b.left());
            int r = need(b.right());
            return l == r ? l + 1 : Math.max(l, r);
        }
        if (value instanceof Node.UnaryExpression u)
            return need(u.operand());
        return 1;
    }

    // Evaluate expression into scratch register `reg`; result lands in `reg`.
    private int evalExpr(Node value, int reg, Sema.Result sema) {
        if (reg > 9)
            throw new IllegalStateException("expression too complex (out of scratch registers)");

        if (value instanceof Node.NumberLiteral num) {
            emit("ld #" + num.value() + ", %" + reg + ";");
            return reg;
        }
        if (value instanceof Node.BinaryExpression expr) {
            // Sethi-Ullman: evaluate the child that needs more registers first
            int regLeft = need(expr.left());
            int regRight = need(expr.right());
            boolean rightFirst = regRight > regLeft;
            evalExpr(rightFirst ? expr.right() : expr.left(), reg, sema);
            evalExpr(rightFirst ? expr.left() : expr.right(), reg + 1, sema);
            int leftReg = rightFirst ? reg + 1 : reg;
            int rightReg = rightFirst ? reg : reg + 1;
            String mnemonic = switch (expr.op()) {
                case "+" -> "add";
                case "-" -> "sub";
                case "*" -> "mul";
                case "/" -> "div";
                case "%" -> "mod";
                case "<<" -> "shl";
                case ">>" -> "shr";
                case "&" -> "and";
                case "|" -> "or";
                case "^" -> "xor";
                default -> throw new IllegalStateException("unknown operator " + expr.op());
            };
            emit(mnemonic + " %" + leftReg + ", %" + rightReg + ", %" + reg + ";");
            return reg;
        }
        if (value instanceof Node.UnaryExpression u) {
            evalExpr(u.operand(), reg, sema);
            switch (u.op()) {
                case "~" -> emit("not %" + reg + ", %" + reg + ";");
                case "-" -> {
                    emit("not %" + reg + ", %" + reg + ";");
                    emit("inc %" + reg + ";");
                }
                default -> throw new IllegalStateException("unknown unary operator " + u.op());
            }
            return reg;
        }
        // Anything else is a location (variable or field)
        Sema.Variable v = locationOf(value, sema);
        emitLoadToReg(v.type().size(), v.offset(), reg);
        return reg;
    }

    // Resolve a field access to a "fake variable" (its size + offset in the frame).
    private Sema.Variable fieldVar(String structName, String fieldName, int line, Sema.Result sema) {
        Sema.Variable v = sema.vars().get(structName);
        Sema.FieldLayout field = v.type().getField(fieldName);
        return new Sema.Variable(v.name() + "." + field.name(), field.type(),
                v.offset() + field.offset(), line);
    }

    // Resolve a value node that names a memory location (variable or field).
    private Sema.Variable locationOf(Node value, Sema.Result sema) {
        if (value instanceof Node.VariableReference ref)
            return sema.vars().get(ref.name());
        if (value instanceof Node.FieldVariableReference ref)
            return fieldVar(ref.structName(), ref.fieldName(), ref.line(), sema);
        throw new IllegalArgumentException("not a location: " + value);
    }

    // Load a stack value into a register (1 or 4-byte types only).
    private void emitLoadToReg(int size, int offset, int reg) {
        if (size == 1)
            emit("ldb %14+#" + offset + ", %" + reg + ";");
        else if (size == 4)
            emit("ldi %14+#" + offset + ", %" + reg + ";");
        else
            throw new IllegalStateException("cannot load " + size + "-byte type into a register");
    }

    // Store a register into the target (1 or 4-byte types only).
    private void emitStoreReg(int reg, Sema.Variable v) {
        int size = v.type().size();
        int off = v.offset();
        if (size == 1)
            emit("stb %" + reg + ", %14+#" + off + ";");
        else if (size == 4)
            emit("sti %" + reg + ", %14+#" + off + ";");
        else
            throw new IllegalStateException("cannot store register into " + size + "-byte type");
    }

    // store `value` into v, filling ALL v.size() bytes, big-endian
    private void emitStoreLiteral(long value, Sema.Variable v) {
        int size = v.type().size();
        int off = v.offset();

        // use stb and sti natively when possible
        if (size == 1) {
            emit("ld #" + value + ", %5;");
            emit("stb %5, %14+#" + off + ";");
            return;
        }
        if (size == 4) {
            emit("ld #" + value + ", %5;");
            emit("sti %5, %14+#" + off + ";");
            return;
        }

        // the value and the padding word once
        int sign_word = (value < 0) ? -1 : 0;
        emit("ld #" + value + ", %5;");
        emit("ld #" + sign_word + ", %6;");

        int current_off = off;

        if (size > 4) {
            int padding_chunks = (size - 4) / 4;
            for (int i = 0; i < padding_chunks; i++) {
                emit("sti %6, %14+#" + current_off + ";");
                current_off += 4;
            }

            int rem_padding = (size - 4) % 4;
            for (int i = 0; i < rem_padding; i++) {
                emit("stb %6, %14+#" + current_off + ";");
                current_off += 1;
            }
            emit("sti %5, %14+#" + current_off + ";");
        } else {
            for (int i = 0; i < size; i++) {
                int shift = 8 * (size - 1 - i);
                int b = (int) ((value >> shift) & 0xFF);
                emit("ld #" + b + ", %5;");
                emit("stb %5, %14+#" + current_off + ";");
                current_off += 1;
            }
        }
    }

    private void emitStoreVariable(Sema.Variable v_src, Sema.Variable v_tgt) {
        int size = v_src.type().size();
        if (size == 1 || size == 4) {
            emitLoadToReg(size, v_src.offset(), 5);
            emitStoreReg(5, v_tgt);
            return;
        }

        // Optimized chunk copy: 4 bytes at a time, then the remainder
        int src_off = v_src.offset();
        int tgt_off = v_tgt.offset();
        int full_chunks = size / 4;
        for (int i = 0; i < full_chunks; i++) {
            int chunk_off = i * 4;
            emit("ldi %14+#" + (src_off + chunk_off) + ", %5;");
            emit("sti %5, %14+#" + (tgt_off + chunk_off) + ";");
        }

        int rem_bytes = size % 4;
        int rem_start = full_chunks * 4;
        for (int i = 0; i < rem_bytes; i++) {
            int rem_off = rem_start + i;
            emit("ldb %14+#" + (src_off + rem_off) + ", %5;");
            emit("stb %5, %14+#" + (tgt_off + rem_off) + ";");
        }
    }

    private void emit(String line) {
        finalASM.append(line).append('\n');
    }
}