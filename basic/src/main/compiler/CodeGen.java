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
                    Sema.Variable v = sema.vars().get(fa.structName());
                    Sema.FieldLayout field = v.type().getField(fa.fieldName());
                    int finalOffset = v.offset() + field.offset();

                    // Create a "fake" variable record for the target field
                    Sema.Variable fakeTgt = new Sema.Variable(
                            fa.structName() + "." + fa.fieldName(),
                            field.type(),
                            finalOffset,
                            fa.line());

                    emitRHS(fa.value(), fakeTgt, sema);
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
            emitAssignLiteral(num.value(), v_tgt);
        } else if (value instanceof Node.VariableReference ref) {
            Sema.Variable v_src = sema.vars().get(ref.name());
            emitAssignVariable(v_src, v_tgt);
        } else if (value instanceof Node.FieldVariableReference ref) {
            Sema.Variable v_src = sema.vars().get(ref.structName());
            Sema.FieldLayout srcField = v_src.type().getField(ref.fieldName());
            
            // Create a "fake" variable for the source field
            Sema.Variable fakeSrc = new Sema.Variable(
                    v_src.name() + "." + srcField.name(),
                    srcField.type(),
                    v_src.offset() + srcField.offset(),
                    ref.line()
            );
            emitAssignVariable(fakeSrc, v_tgt);
        }
    }

    // store `value` into v, filling ALL v.size() bytes, big-endian
    private void emitAssignLiteral(long value, Sema.Variable v) {
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

    private void emitAssignVariable(Sema.Variable v_src, Sema.Variable v_tgt) {
        int size = v_src.type().size();
        int src_off = v_src.offset();
        int tgt_off = v_tgt.offset();

        if (size == 1) {
            emit("ldb %14+#" + src_off + ", %5;");
            emit("stb %5, %14+#" + tgt_off + ";");
        } else if (size == 4) {
            emit("ldi %14+#" + src_off + ", %5;");
            emit("sti %5, %14+#" + tgt_off + ";");
        } else {
            // Optimized chunk copy: 4 bytes at a time, then the remainder
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
    }

    private void emit(String line) {
        finalASM.append(line).append('\n');
    }
}