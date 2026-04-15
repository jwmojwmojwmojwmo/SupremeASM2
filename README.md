# SUPREME ASM v2 IS HERE BETTER THAN EVER!!!

So yeah SupremeASM v1 was really cool and all but it's time for an upgrade. SupremeASM v2 features a better VM with a custom "OS" built with C, and a much better assembler built in Python. We have better instructions, more in line with how real CPUs work, and we have labels now!! no more stupid offset calculators 

Again, built for learning purposes.

I have plans to make this into an actual language!! SupremeBASIC boutta be the most goated language ever (no it wont)

# SupremeASM v2 Documentation

**Architecture Overview**
* **Word Size:** 32-bit (Big Endian).
* **Instruction Width:** Dual-width (4-byte or 8-byte).
* **Encoding Logic:** If the first hexit is `A`, the instruction is **8 bytes** (includes a 32-bit immediate). Otherwise, it is **4 bytes**.
* **Instruction Format:** `aabcdefghhhhhhhh`
    * `aa`: Opcode
    * `b-g`: Register/Operand nibbles
    * `hhhhhhhh`: 32-bit immediate (for 8-byte instructions)
* **Registers:** 16 Registers (`%r0` - `%rF`). All registers store signed integers.

---

## Running SupremeASM


To assemble SupremeASM code into machine code, run the Python script asm.py in the "assembler" folder, with args: python asm.py <filename.sasm> [--autorun] [--dpr] [--dpm]. 


To run machine code, run main.exe in the "vm" folder, with args: ./main <filename> [--dpr] [--dpm]


"filename" is the file path to the desired file. "--autorun" specifies if the Python script should immediately run the compiled machine code. Not including this flag means the code will only compile. "--dpr" and "--dpm" tells the VM to dump registers and dump memory after execution into the console, respectively.
Including these flags when running the Python script without "--autorun" does nothing.


**IMPORTANT**: All Supreme Machine Code files have file extension .smc, and all SupremeASM v2 files have file extension .sasm. To differentiate from SupremeASM v1, SupremeASM v2 files must have header "@version 2". See examples for uh...examples.


**ALSO IMPORTANT**: The makefile probably won't work if you're on a different OS or something or other tbh I don't really get makefiles, you might have to modify it so the C code compiles. Anyways it works on my machine so YMMV.

## Register Conventions
While all registers are technically general-purpose, the general conventions are as follows:

| Register | Usage |
| :--- | :--- |
| `%r0 - %r3` | Syscall Arguments / Return Values |
| `%r4 - %r9` | General Purpose (Compiler Scratch) |
| `%rA - %rD` | Function Parameters |
| `%rE` | **SP** (Stack Pointer) |
| `%rF` | **RA** (Return Address) |

---

## PC
The program counter points to the next instruction, not the current instruction. Useful to know for jumps and stuff. BUT WE HAVE LABELS SO IT DOESN'T MATTER WHOOOOO

## Instruction Set Reference
| Mnemonic | Hex Pattern | Description |
| :--- | :--- | :--- |
| `ld #v, %r` | `A0r----- vvvvvvvv` | Load 32-bit immediate `v` into `%r` (**8-byte**) |
| `ldb %r+#o, %s` | `00rsoooo` | Load signed BYTE from `m[%r + o]` into `%s` |
| `ldi %r+#o, %s` | `01rsoooo` | Load INT from `m[%r + o]` into `%s` |
| `ldb %r+%o, %s` | `02rso---` | Load indexed BYTE from `m[%r + %o]` into `%s` |
| `ldi %r+%o, %s` | `03rso---` | Load indexed INT from `m[%r + %o]` into `%s` |
| `stb %r, %s+#o` | `10rsoooo` | Store BYTE from `%r` into `m[%s + o]` |
| `sti %r, %s+#o` | `11rsoooo` | Store INT from `%r` into `m[%s + o]` |
| `stb %r, %s+%o` | `12rso---` | Store indexed BYTE from `%r` into `m[%s + %o]` |
| `sti %r, %s+%o` | `13rso---` | Store indexed INT from `%r` into `m[%s + %o]` |
| `mov %r, %s` | `20rs----` | Copy `%r` -> `%s` |
| `inc %r` | `21r-----` | `%r = %r + 1` |
| `dec %r` | `22r-----` | `%r = %r - 1` |
| `shf %r, #v, %s` | `23rs--vv` | Bitshift `%r` (Right if `v < 0`, Left if `v >= 0`) -> `%s` |
| `add %r, %s, %o` | `24rso---` | `%o = %r + %s` |
| `sub %r, %s, %o` | `25rso---` | `%o = %r - %s` |
| `mul %r, %s, %o` | `26rso---` | `%o = %r * %s` |
| `div %r, %s, %o` | `27rso---` | `%o = %r / %s` |
| `mod %r, %s, %o` | `28rso---` | `%o = %r % %s` |
| `and %r, %s, %o` | `29rso---` | `%o = %r & %s` |
| `or %r, %s, %o` | `2Arso---` | `%o = %r \| %s` |
| `xor %r, %s, %o` | `2Brso---` | `%o = %r ^ %s` |
| `not %r, %s` | `2Crs----` | `%s = ~%r` |
| `br #o` | `30--oooo` | Indirect jump: `PC = PC + o` (signed `o`) |
| `bre %r, %s, #o` | `31rsoooo` | Indirect jump if `%r == %s` |
| `brn %r, %s, #o` | `32rsoooo` | Indirect jump if `%r != %s` |
| `bgs %r, %s, #o` | `33rsoooo` | Indirect jump if `%r > %s`|
| `bgu %r, %s, #o` | `34rsoooo` | Indirect jump if `%r > %s` |
| `gpc %r, #o` | `35r-oooo` | Load `PC + o` into `%r` (signed `o`) |
| `jmp %r` | `36r-----` | Direct jump: `PC = %r` |
| `halt` | `ffffffff` | Terminate VM execution |

---

## System Operation

### System Calls (`syscall`)
**Encoding:** `f0------`
Arguments are passed via `%r0-%r3`. Results are returned in `%r0`.

| ID (`%r0`) | Function | Inputs | Return (`%r0`) |
| :--- | :--- | :--- | :--- |
| **0** | `read` | `%r1`: FD, `%r2`: Buffer, `%r3`: Size | Bytes read |
| **1** | `write` | `%r1`: FD, `%r2`: Buffer, `%r3`: Size | Bytes written |
| **2** | `get_time`| None | MS since program start |
| **3** | `sbrk` | `%r3`: Increment size | Old heap end address |

Currently, FD = 0 (stdin) and FD = 1 (stdout) are the only supported file descriptors. Reading and writing from other files is planned.

---

## Labels

Jump instructions (ie. "br", "bre", "brn", "bgs", "bgu", "gpc") are compatible with labels. To create a label, use lbl: lbl_name; To jump to a label, replace the immediate jump value (ie. the #o in the ISA) with the label name. For example:

```
lbl: start_loop;
bre %1, %2, break;
dec %1;               
br start_loop;
lbl: break;
halt;
```

## Example Code

Yeah I'm WAY too lazy to code in Assembly and also since there's no easy way to get input from stdout anymore, idk what really to show. Stuff like Fibonacci sequence, etc. is not very cool when the immediate is hard coded in. Maybe I'll get on it one day, but for now there are not many examples. 


Timing Test (for debugging and comparison purposes). Runs a simple loop subtracting 1 from 0x11000000 until it hits 0, and outputs the time in ms it took to run into r0. Use --dpr to see the result:
```
@version 2

ld #2, %0;
syscall;
mov %0, %3;
ld #0, %2;
ld #0x11000000, %1;
lbl: start_loop;
bre %1, %2, break;
dec %1;               
br start_loop;
lbl: break;
ld #2, %0;
syscall;
sub %0, %3, %0;
halt;
```
