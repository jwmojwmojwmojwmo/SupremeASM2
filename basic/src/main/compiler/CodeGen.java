package basic.src.main.compiler;

// Represents the Code Generator in the compiler. The job is to trust the AST and Sema
// and generate the SupremeASM based off them.
public class CodeGen {

    private final StringBuilder finalASM = new StringBuilder();

    // mirrors ASM's addressing model: base register + offset
    private record Addr(int base, int offset) {
    }

    public String generate(Node.Program program, Sema.Result sema) {
        finalASM.setLength(0);
        emit("@version 2");
        emit("");
        // allocate the whole frame at once
        if (sema.frameSize() > 0) {
            emit("ld #" + sema.frameSize() + ", %5;");
            emit("sub %14, %5, %14;");
            emit("");
        }

        for (Node n : program.statements()) {
            switch (n) {
            case Node.AssignmentStatement a -> {
                Sema.Type tgtType = locationType(a.target(), sema);
                int size = tgtType.size();

                // compute the target address first, park it in %13 if dynamic so the
                // RHS evaluation (which may use %10/%11 for its own addresses) can't touch it
                Addr tgt = stabilize(addressOf(a.target(), 4, sema));

                if (size == 1 || size == 4) {
                    // scalar: evaluate the value into %4, then store
                    evalExpr(a.value(), 4, sema, false);
                    emitStore(tgt, size, 4);
                } else if (a.value() instanceof Node.NumberLiteral num) {
                    // literal into a non-register-sized type: sign-extend / byte-wise
                    emitStoreLiteral(num.value(), tgt, size);
                } else {
                    // aggregate copy: address to address, chunked
                    Addr src = addressOf(a.value(), 4, sema);
                    emitCopy(src, tgt, size);
                }
            }
            case Node.VariableDeclaration d -> {
            }
            case Node.ArrayDeclaration d -> {
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


    private Addr addressOf(Node loc, int reg, Sema.Result sema) {
        if (loc instanceof Node.VariableAccess va) {
            Sema.Variable v = sema.vars().get(va.name());
            return new Addr(14, v.offset());
        }
        if (loc instanceof Node.FieldAccess fa) {
            Addr base = addressOf(fa.base(), reg, sema);
            Sema.Type baseType = locationType(fa.base(), sema);
            Sema.FieldLayout field = baseType.getField(fa.fieldName());
            // field offset is a compile-time constant, so it folds into the offset field
            return new Addr(base.base(), base.offset() + field.offset());
        }
        if (loc instanceof Node.ArrayAccess aa) {
            Sema.Variable arr = sema.vars().get(((Node.VariableAccess) aa.base()).name());
            int elemSize = arr.type().size();
            Addr base = addressOf(aa.base(), reg, sema);

            evalExpr(aa.index(), reg, sema, false);
            emit("mov %" + reg + ", %10;");
            emit("ld #" + elemSize + ", %11;");
            emit("mul %10, %11, %10;");
            // + base address (%14 + array offset)
            emit("ld #" + base.offset() + ", %11;");
            emit("add %" + base.base() + ", %11, %11;");
            emit("add %11, %10, %10;");
            return new Addr(10, 0);
        }
        throw new IllegalArgumentException("not a location: " + loc);
    }

    // If the address is dynamic, move it into %13 so it survives RHS evaluation.
    private Addr stabilize(Addr a) {
        if (a.base() == 14)
            return a;
        emit("mov %" + a.base() + ", %13;");
        return new Addr(13, a.offset());
    }

    // Resolve the type of a location expression (Sema has already validated it).
    private Sema.Type locationType(Node loc, Sema.Result sema) {
        if (loc instanceof Node.VariableAccess va)
            return sema.vars().get(va.name()).type();
        if (loc instanceof Node.ArrayAccess aa)
            return sema.vars().get(((Node.VariableAccess) aa.base()).name()).type();
        if (loc instanceof Node.FieldAccess fa)
            return locationType(fa.base(), sema).getField(fa.fieldName()).type();
        throw new IllegalArgumentException("not a location: " + loc);
    }

    private void emitLoad(Addr a, int size, int reg) {
        if (size == 1)
            emit("ldb %" + a.base() + "+#" + a.offset() + ", %" + reg + ";");
        else if (size == 4)
            emit("ldi %" + a.base() + "+#" + a.offset() + ", %" + reg + ";");
        else
            throw new IllegalStateException("cannot load " + size + "-byte type into a register");
    }

    private void emitStore(Addr a, int size, int reg) {
        if (size == 1)
            emit("stb %" + reg + ", %" + a.base() + "+#" + a.offset() + ";");
        else if (size == 4)
            emit("sti %" + reg + ", %" + a.base() + "+#" + a.offset() + ";");
        else
            throw new IllegalStateException("cannot store register into " + size + "-byte type");
    }


    // Every leaf needs one register, an array access is as heavy as its index
    private int need(Node value) {
        if (value instanceof Node.BinaryExpression b) {
            int l = need(b.left());
            int r = need(b.right());
            return l == r ? l + 1 : Math.max(l, r);
        }
        if (value instanceof Node.UnaryExpression u)
            return need(u.operand());
        if (value instanceof Node.ArrayAccess aa)
            return need(aa.index());
        if (value instanceof Node.FieldAccess fa)
            return need(fa.base());
        return 1;
    }

    // Evaluate expression into scratch register `reg`; result lands in `reg`.
    private int evalExpr(Node value, int reg, Sema.Result sema, boolean hasSpilled) {
        if (value instanceof Node.NumberLiteral num) {
            emit("ld #" + num.value() + ", %" + reg + ";");
            return reg;
        }
        if (value instanceof Node.BinaryExpression expr) {
            int needLeft = need(expr.left());
            int needRight = need(expr.right());
            boolean rightFirst = needRight > needLeft;
            Node heavy = rightFirst ? expr.right() : expr.left();
            Node light = rightFirst ? expr.left() : expr.right();

            int available = 8 - reg + 1;
            int heavyReg;
            int lightReg;
            if (Math.min(needLeft, needRight) <= available - 1) {
                // Sethi-Ullman: heavier child first, both results stay in registers
                evalExpr(heavy, reg, sema, hasSpilled);
                evalExpr(light, reg + 1, sema, hasSpilled);
                heavyReg = reg;
                lightReg = reg + 1;
            } else {
                if (!hasSpilled) {
                    emit("mov %14, %9;");
                    hasSpilled = true;
                }
                evalExpr(heavy, reg, sema, hasSpilled);
                emit("dec %9;\ndec %9;\ndec %9;\ndec %9;");
                emit("sti %" + reg + ", %9+#0;");
                evalExpr(light, reg, sema, hasSpilled);
                emit("ldi %9+#0, %" + (reg + 1) + ";");
                emit("inc %9;\ninc %9;\ninc %9;\ninc %9;");
                heavyReg = reg + 1;
                lightReg = reg;
            }

            int leftReg = rightFirst ? lightReg : heavyReg;
            int rightReg = rightFirst ? heavyReg : lightReg;

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
            evalExpr(u.operand(), reg, sema, hasSpilled);
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
        // Anything else is a location: compute its address, then load.
        Addr a = addressOf(value, reg, sema);
        emitLoad(a, locationType(value, sema).size(), reg);
        return reg;
    }


    // store `value` into tgt, filling ALL size bytes, big-endian
    private void emitStoreLiteral(long value, Addr tgt, int size) {
        if (size == 1) {
            emit("ld #" + value + ", %5;");
            emit("stb %5, %" + tgt.base() + "+#" + tgt.offset() + ";");
            return;
        }
        if (size == 4) {
            emit("ld #" + value + ", %5;");
            emit("sti %5, %" + tgt.base() + "+#" + tgt.offset() + ";");
            return;
        }

        int sign_word = (value < 0) ? -1 : 0;
        emit("ld #" + value + ", %5;");
        emit("ld #" + sign_word + ", %6;");

        int cur = tgt.offset();
        if (size > 4) {
            int padding_chunks = (size - 4) / 4;
            for (int i = 0; i < padding_chunks; i++) {
                emit("sti %6, %" + tgt.base() + "+#" + cur + ";");
                cur += 4;
            }
            int rem_padding = (size - 4) % 4;
            for (int i = 0; i < rem_padding; i++) {
                emit("stb %6, %" + tgt.base() + "+#" + cur + ";");
                cur += 1;
            }
            emit("sti %5, %" + tgt.base() + "+#" + cur + ";");
        } else {
            for (int i = 0; i < size; i++) {
                int shift = 8 * (size - 1 - i);
                int b = (int) ((value >> shift) & 0xFF);
                emit("ld #" + b + ", %5;");
                emit("stb %5, %" + tgt.base() + "+#" + cur + ";");
                cur += 1;
            }
        }
    }

    // copy size bytes from src to tgt, 4 bytes at a time then the remainder
    private void emitCopy(Addr src, Addr tgt, int size) {
        int full_chunks = size / 4;
        for (int i = 0; i < full_chunks; i++) {
            int c = i * 4;
            emit("ldi %" + src.base() + "+#" + (src.offset() + c) + ", %5;");
            emit("sti %5, %" + tgt.base() + "+#" + (tgt.offset() + c) + ";");
        }
        int rem_bytes = size % 4;
        int rem_start = full_chunks * 4;
        for (int i = 0; i < rem_bytes; i++) {
            int o = rem_start + i;
            emit("ldb %" + src.base() + "+#" + (src.offset() + o) + ", %5;");
            emit("stb %5, %" + tgt.base() + "+#" + (tgt.offset() + o) + ";");
        }
    }

    private void emit(String line) {
        finalASM.append(line).append('\n');
    }
}