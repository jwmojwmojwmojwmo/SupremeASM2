# Supreme ASM v2 Specification
# Required Header: @version 2

"""
3. Instruction Set Reference (Supreme ASM v2)
@version 2 | Big Endian | 4 or 8 Byte Encoding

[Load Group]
ld  #v, %r        | A0r00000 vvvvvvvv | Load 32-bit immediate 'v' -> r[r]
ldb %r+#o, %s     | 00rsoooo          | Load signed BYTE from m[r[r]+o] -> r[s]
ldi %r+#o, %s     | 01rsoooo          | Load INT from m[r[r]+o] -> r[s]
ldb %r+%o, %s     | 02rso000          | Load signed BYTE from m[r[r]+r[o]] -> r[s]
ldi %r+%o, %s     | 03rso000          | Load INT from m[r[r]+r[o]] -> r[s]

[Store Group]
stb %r, %s+#o     | 10rsoooo          | Store BYTE from r[r] -> m[r[s]+o]
sti %r, %s+#o     | 11rsoooo          | Store INT from r[r] -> m[r[s]+o]
stb %r, %s+%o     | 12rso000          | Store BYTE from r[r] -> m[r[s]+r[o]]
sti %r, %s+%o     | 13rso000          | Store INT from r[r] -> m[r[s]+r[o]]

[Math Group]
mov %r, %s        | 20rs0000          | Copy r[r] -> r[s]
inc %r            | 21r00000          | r[r]++
dec %r            | 22r00000          | r[r]--
shf %r<<#v, %s    | 23rs00vv          | Shift r[r] left by v -> r[s]
shf %r>>#v, %s    | 23rs00vv          | Shift r[r] right by v -> r[s]
add %r, %s, %o    | 24rso000          | r[o] = r[r] + r[s]
sub %r, %s, %o    | 25rso000          | r[o] = r[r] - r[s]
mul %r, %s, %o    | 26rso000          | r[o] = r[r] * r[s]
div %r, %s, %o    | 27rso000          | r[o] = r[r] / r[s]
mod %r, %s, %o    | 28rso000          | r[o] = r[r] % r[s]
and %r, %s, %o    | 29rso000          | r[o] = r[r] & r[s]
or  %r, %s, %o    | 2Arso000          | r[o] = r[r] | r[s]
xor %r, %s, %o    | 2Brso000          | r[o] = r[r] ^ r[s]
not %r, %s        | 2Crs0000          | r[s] = ~r[r]

[Program Flow Group]
br  #o            | 3000oooo          | Rel jump by signed offset 'o'
bre %r, %s, #o    | 31rsoooo          | Rel jump if r[r] == r[s]
brn %r, %s, #o    | 32rsoooo          | Rel jump if r[r] != r[s]
bgs %r, %s, #o    | 33rsoooo          | Rel jump if r[r] > r[s] (Signed)
bgu %r, %s, #o    | 34rsoooo          | Rel jump if r[r] > r[s] (Unsigned)
gpc %r, #o        | 35r0oooo          | r[r] = PC + offset 'o'
jmp %r            | 36r00000          | Direct jump to r[r]

[System Group]
syscall           | f0000000          | System call (r0=ID)
    r0: id (0 = read, 1 = write, 3 = sbrk, 2 = get_time (since program start, in ms)
    r1: fd (0 = stdin, 1 = stdout)
    r2: buffer
    r3: size  
halt              | ffffffff          | Stop execution
"""
import struct
import sys
import time
import subprocess
        
def translate_ld(operands):
    imm = int(operands[0].replace("#", ""), 0)
    reg = int(operands[1].replace("%", ""))
    ins = struct.pack(">I", 0xa00fffff | (reg << 20))
    ins += struct.pack(">i", imm)
    return ins

def translate_ldb(operands):
    # ldb %r+%o, %s     | 02rso000
    # ldb %r+#o, %s     | 00rsoooo
    reg_r = int(operands[0].split("+")[0].replace("%", ""))
    reg_s = int(operands[1].replace("%", ""))
    if ("#" in operands[0]):
        offset = int(operands[0].split("+")[1].replace("#", ""), 0)
        offset = offset & 0xFFFF # ensure correct length
        ins = (0x00 << 24) | (reg_r << 20) | (reg_s << 16) | offset
    else:
        reg_o = int(operands[0].split("+")[1].replace("%", ""))
        ins = (0x02 << 24) | (reg_r << 20) | (reg_s << 16) | (reg_o << 12) | 0xFFF
    return struct.pack(">I", ins)

def translate_ldi(operands):
    # ldi %r+#o, %s     | 01rsoooo
    # ldi %r+%o, %s     | 03rso000
    reg_r = int(operands[0].split("+")[0].replace("%", ""))
    reg_s = int(operands[1].replace("%", ""))
    if ("#" in operands[0]):
        offset = int(operands[0].split("+")[1].replace("#", ""), 0)
        offset = offset & 0xFFFF # ensure correct length
        ins = (0x01 << 24) | (reg_r << 20) | (reg_s << 16) | offset
    else:
        reg_o = int(operands[0].split("+")[1].replace("%", ""))
        ins = (0x03 << 24) | (reg_r << 20) | (reg_s << 16) | (reg_o << 12) | 0xFFF
    return struct.pack(">I", ins)

def translate_stb(operands):
    # stb %r, %s+#o     | 10rsoooo
    # stb %r, %s+%o     | 12rso000
    reg_r = int(operands[0].replace("%", ""))
    reg_s = int(operands[1].split("+")[0].replace("%", ""))
    if ("#" in operands[1]):
        offset = int(operands[1].split("+")[1].replace("#", ""), 0)
        offset = offset & 0xFFFF # ensure correct length
        ins = (0x10 << 24) | (reg_r << 20) | (reg_s << 16) | offset
    else:
        reg_o = int(operands[1].split("+")[1].replace("%", ""))
        ins = (0x12 << 24) | (reg_r << 20) | (reg_s << 16) | (reg_o << 12) | 0xFFF
    return struct.pack(">I", ins)


def translate_sti(operands):
    # sti %r, %s+#o     | 11rsoooo
    # sti %r, %s+%o     | 13rso000
    reg_r = int(operands[0].replace("%", ""))
    reg_s = int(operands[1].split("+")[0].replace("%", ""))
    if ("#" in operands[1]):
        offset = int(operands[1].split("+")[1].replace("#", ""), 0)
        offset = offset & 0xFFFF # ensure correct length
        ins = (0x11 << 24) | (reg_r << 20) | (reg_s << 16) | offset
    else:
        reg_o = int(operands[1].split("+")[1].replace("%", ""))
        ins = (0x13 << 24) | (reg_r << 20) | (reg_s << 16) | (reg_o << 12) | 0xFFF
    return struct.pack(">I", ins)

def translate_mov(operands):
    # mov %r, %s        | 20rs0000
    reg_r = int(operands[0].replace("%", ""))
    reg_s = int(operands[1].replace("%", ""))
    ins = (0x20 << 24) | (reg_r << 20) | (reg_s << 16) | 0xFFFF
    return struct.pack(">I", ins)

def translate_inc(operands):
    # inc %r            | 21r00000
    reg_r = int(operands[0].replace("%", ""))
    ins = (0x21 << 24) | (reg_r << 20) | 0xFFFFF
    return struct.pack(">I", ins)

def translate_dec(operands):
    # dec %r            | 22r00000
    reg_r = int(operands[0].replace("%", ""))
    ins = (0x22 << 24) | (reg_r << 20) | 0xFFFFF
    return struct.pack(">I", ins)

def translate_shf(operands):
    # shf %r<<#v, %s    | 23rs00vv
    # shf %r>>#v, %s    | 23rs00vv
    reg_r = int(operands[0].split("<<")[0].split(">>")[0].replace("%", ""))
    reg_s = int(operands[1].replace("%", ""))
    if ("<<" in operands[0]):
        shift = int(operands[0].split("<<")[1].replace("#", ""), 0)
        shift = shift & 0xFF # ensure correct length
        ins = (0x23 << 24) | (reg_r << 20) | (reg_s << 16) | (0xFF << 8) |shift
    else:
        shift = int(operands[0].split(">>")[1].replace("#", ""), 0)
        shift = -shift & 0xFF # convert to 2's complement for negative shift
        ins = (0x23 << 24) | (reg_r << 20) | (reg_s << 16) | (0xFF << 8) | shift
    return struct.pack(">I", ins)

def translate_add(operands):
    # add %r, %s, %o    | 24rso000
    reg_r = int(operands[0].replace("%", ""))
    reg_s = int(operands[1].replace("%", ""))
    reg_o = int(operands[2].replace("%", ""))
    ins = (0x24 << 24) | (reg_r << 20) | (reg_s << 16) | (reg_o << 12) | 0xFFF
    return struct.pack(">I", ins)

def translate_sub(operands):
    # sub %r, %s, %o    | 25rso000
    reg_r = int(operands[0].replace("%", ""))
    reg_s = int(operands[1].replace("%", ""))
    reg_o = int(operands[2].replace("%", ""))
    ins = (0x25 << 24) | (reg_r << 20) | (reg_s << 16) | (reg_o << 12) | 0xFFF
    return struct.pack(">I", ins)

def translate_mul(operands):
    # mul %r, %s, %o    | 26rso000
    reg_r = int(operands[0].replace("%", ""))
    reg_s = int(operands[1].replace("%", ""))
    reg_o = int(operands[2].replace("%", ""))
    ins = (0x26 << 24) | (reg_r << 20) | (reg_s << 16) | (reg_o << 12) | 0xFFF
    return struct.pack(">I", ins)

def translate_div(operands):
    # div %r, %s, %o    | 27rso000
    reg_r = int(operands[0].replace("%", ""))
    reg_s = int(operands[1].replace("%", ""))
    reg_o = int(operands[2].replace("%", ""))
    ins = (0x27 << 24) | (reg_r << 20) | (reg_s << 16) | (reg_o << 12) | 0xFFF
    return struct.pack(">I", ins)

def translate_mod(operands):
    # mod %r, %s, %o    | 28rso000
    reg_r = int(operands[0].replace("%", ""))
    reg_s = int(operands[1].replace("%", ""))
    reg_o = int(operands[2].replace("%", ""))
    ins = (0x28 << 24) | (reg_r << 20) | (reg_s << 16) | (reg_o << 12) | 0xFFF
    return struct.pack(">I", ins)

def translate_and(operands):
    # and %r, %s, %o    | 29rso000
    reg_r = int(operands[0].replace("%", ""))
    reg_s = int(operands[1].replace("%", ""))
    reg_o = int(operands[2].replace("%", ""))
    ins = (0x29 << 24) | (reg_r << 20) | (reg_s << 16) | (reg_o << 12) | 0xFFF
    return struct.pack(">I", ins)

def translate_or(operands):
    # or %r, %s, %o     | 2arso000
    reg_r = int(operands[0].replace("%", ""))
    reg_s = int(operands[1].replace("%", ""))
    reg_o = int(operands[2].replace("%", ""))
    ins = (0x2a << 24) | (reg_r << 20) | (reg_s << 16) | (reg_o << 12) | 0xFFF
    return struct.pack(">I", ins)

def translate_xor(operands):
    # xor %r, %s, %o    | 2brso000
    reg_r = int(operands[0].replace("%", ""))
    reg_s = int(operands[1].replace("%", ""))
    reg_o = int(operands[2].replace("%", ""))
    ins = (0x2b << 24) | (reg_r << 20) | (reg_s << 16) | (reg_o << 12) | 0xFFF
    return struct.pack(">I", ins)

def translate_not(operands):
    # not %r, %s       | 2crs0000
    reg_r = int(operands[0].replace("%", ""))
    reg_s = int(operands[1].replace("%", ""))
    ins = (0x2c << 24) | (reg_r << 20) | (reg_s << 16) | 0xFFFF
    return struct.pack(">I", ins)

def translate_br(operands):
    # br  #o            | 3000oooo
    offset = int(operands[0].replace("#", ""), 0)
    offset = offset & 0xFFFF # ensure correct length
    ins = (0x30 << 24) | (0xFF << 16) |offset
    return struct.pack(">I", ins)


def translate_bre(operands):
    # bre %r, %s, #o    | 31rsoooo
    reg_r = int(operands[0].replace("%", ""))
    reg_s = int(operands[1].replace("%", ""))
    offset = int(operands[2].replace("#", ""), 0)
    offset = offset & 0xFFFF # ensure correct length
    ins = (0x31 << 24) | (reg_r << 20) | (reg_s << 16) | offset
    return struct.pack(">I", ins)

def translate_brn(operands):
    # brn %r, %s, #o    | 32rsoooo
    reg_r = int(operands[0].replace("%", ""))
    reg_s = int(operands[1].replace("%", ""))
    offset = int(operands[2].replace("#", ""), 0)
    offset = offset & 0xFFFF # ensure correct length
    ins = (0x32 << 24) | (reg_r << 20) | (reg_s << 16) | offset
    return struct.pack(">I", ins)

def translate_bgs(operands):
    # bgs %r, %s, #o    | 33rsoooo
    reg_r = int(operands[0].replace("%", ""))
    reg_s = int(operands[1].replace("%", ""))
    offset = int(operands[2].replace("#", ""), 0)
    offset = offset & 0xFFFF # ensure correct length
    ins = (0x33 << 24) | (reg_r << 20) | (reg_s << 16) | offset
    return struct.pack(">I", ins)

def translate_bgu(operands):
    # bgu %r, %s, #o    | 34rsoooo
    reg_r = int(operands[0].replace("%", ""))
    reg_s = int(operands[1].replace("%", ""))
    offset = int(operands[2].replace("#", ""), 0)
    offset = offset & 0xFFFF # ensure correct length
    ins = (0x34 << 24) | (reg_r << 20) | (reg_s << 16) | offset
    return struct.pack(">I", ins)

def translate_gpc(operands):
    # gpc %r, #o        | 35r0oooo
    reg_r = int(operands[0].replace("%", ""))
    offset = int(operands[1].replace("#", ""), 0)
    offset = offset & 0xFFFF # ensure correct length
    ins = (0x35 << 24) | (reg_r << 20) | (0xF << 16) | offset
    return struct.pack(">I", ins)

def translate_jmp(operands):
    # jmp %r            | 36r00000
    reg_r = int(operands[0].replace("%", ""))
    ins = (0x36 << 24) | (reg_r << 20) | 0xFFFFF
    return struct.pack(">I", ins)

def translate_syscall(operands):
    return struct.pack(">I", 0xf0ffffff)

def translate_halt(operands):
    return struct.pack(">I", 0xffffffff)
    
translate_dict = {
    "ld": translate_ld,
    "ldb": translate_ldb,
    "ldi": translate_ldi,
    "stb": translate_stb,
    "sti": translate_sti,
    "mov": translate_mov,
    "inc": translate_inc,
    "dec": translate_dec,
    "shf": translate_shf,
    "add": translate_add,
    "sub": translate_sub,
    "mul": translate_mul,
    "div": translate_div,
    "mod": translate_mod,
    "and": translate_and,
    "or": translate_or,
    "xor": translate_xor,
    "not": translate_not,
    "br": translate_br,
    "bre": translate_bre,
    "brn": translate_brn,
    "bgs": translate_bgs,
    "bgu": translate_bgu,
    "gpc": translate_gpc,
    "jmp": translate_jmp,
    "syscall": translate_syscall,
    "halt": translate_halt
}

def translate(split_asm, line_number):
    if (len(split_asm) > 4):
        print(f"Compilation failed! unknown instruction: {str(split_asm)[1:-1]} @ line {line_number + 1}")
        sys.exit(1)
    if (len(split_asm) == 0):
        print(f"Compilation failed! empty instruction @ line {line_number + 1}")
        sys.exit(1)
    if (split_asm[0] not in translate_dict):
        print(f"Compliation failed! unknown operand: {split_asm[0]} @ line {line_number + 1}")
        sys.exit(1)
    for operand in split_asm[1:]:
        try:
            if (not operand.startswith("%") and not operand.startswith("#")):
                print(f"Compilation failed! unknown operand: {operand} @ line {line_number + 1}")
                sys.exit(1)
            if (operand.startswith("%")):
                operand = operand.replace("%", "")
                if (not "+" in operand and not ">>" in operand and not "<<" in operand):
                    if (int(operand) < 0 or int(operand) > 15):
                        print(f"Compilation failed! invalid register: %{operand} @ line {line_number + 1}")
                        sys.exit(1)
                elif ("+" in operand and not ">>" in operand and not "<<" in operand):
                    reg = operand.split("+")[0].replace("%", "")
                    if (int(reg) < 0 or int(reg) > 15):
                        print(f"Compilation failed! invalid register: %{reg} @ line {line_number + 1}")
                        sys.exit(1)
                elif (">>" in operand or "<<" in operand):
                    reg = operand.split(">>")[0].split("<<")[0].replace("%", "")
                    if (int(reg) < 0 or int(reg) > 15):
                        print(f"Compilation failed! invalid register: %{reg} @ line {line_number + 1}")
                        sys.exit(1)
            if (operand.startswith("#")):
                operand = operand.replace("#", "")
                try:
                    int(operand, 0)
                except ValueError:
                    print(f"Compilation failed! invalid immediate: #{operand} @ line {line_number + 1}")
                    sys.exit(1)
        except ValueError:
            print(f"Compilation failed! invalid operand: {operand} @ line {line_number + 1}")
            sys.exit(1)
    return translate_dict[split_asm[0]](split_asm[1:])


def main():
    if (len(sys.argv) < 2):
        print("Usage: python asm.py <filename.sasm> [--autorun] [--dpr] [--dpm]")
        sys.exit(1)
        
    file = open(sys.argv[1])
    code = file.read()
    file.close()    
    
    if (not code.startswith("@version 2") or not sys.argv[1].endswith(".sasm")):
        print("file must be a Supreme ASM v2 file!")
        sys.exit(1)
        
    print(f"Compiling {sys.argv[1]}...")
    start = time.perf_counter()
    clean_text = ""
    for line in code.split("\n"):
        clean_text += line.split("//")[0] + "\n"
    raw_commands = clean_text.replace("@version 2", "").lower().replace("\n", "").split(";")
    code = [cmd.strip() for cmd in raw_commands if cmd.strip() != ""]
    labelled_code = []
    labels = {}
    pc = 0
    for line in code:
        if (line.startswith("lbl:")):
            label_name = line.split(":")[1].strip()
            if (label_name in labels):
                print(f"Compilation failed! duplicate label: {label_name}")
                sys.exit(1)
            labels[label_name] = pc
            continue   
        else:
            labelled_code.append(line)
            parts = line.replace(",", " ").split()
            if (parts[0] == "ld"):
                pc += 8
            else:
                pc += 4
    
    smc = bytearray()
    pc = 0
    for index, line in enumerate(labelled_code):
        line = line.replace(",", " ").split()
        for i in range(len(line)):
            if (i == 0):
                continue
            if (line[i] in labels):
                if (line[0] in ["br", "bre", "brn", "bgs", "bgu", "gpc"]):
                    offset = labels[line[i]] - pc - 4
                    line[i] = "#" + str(offset)
        smc.extend(translate(line, index + 2))
        if (line[0] == "ld"):
            pc += 8
        else:
            pc += 4
    output_file_name = sys.argv[1].replace(".sasm", "") + ".smc"
    with open("../" + output_file_name, "wb") as output_file:
        output_file.write(smc)
    end = time.perf_counter()
    print(f"Compiled {sys.argv[1]} into {output_file_name} in {(end - start) * 1000:.2f} ms!\n")
    
    if ("--autorun" not in sys.argv):
        return
    
    print("-- Loading into VM --\n")

    run_args = ["../vm/main", "../" + output_file_name]
    if ("--dpr" in sys.argv):
        run_args.append("--dpr")
    if ("--dpm" in sys.argv):
        run_args.append("--dpm")
    result = subprocess.run(run_args, text = True)
    
if __name__ == "__main__":
    main()