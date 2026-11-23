# SUPREME ASM v2 IS HERE BETTER THAN EVER!!!

So yeah SupremeASM v1 was really cool and all but it's time for an upgrade. SupremeASM v2 features a better VM with a custom "OS" built with C, and a much better assembler built in Python. We have better instructions, more in line with how real CPUs work, and we have labels now!! no more stupid offset calculators 

Again, built for learning purposes.

I have plans to make this into an actual language!! SupremeBASIC boutta be the most goated language ever (no it wont)

Want to see the development of SupremeBASIC? See BASIC.md, and run basic/src/SupremeBasicCompiler.java to begin compiling SupremeBASIC! (Don't ask why I have so many different languages ig I'm just #whimsical)

# SupremeASM v2 Documentation

**Architecture Overview**
* **Word Size:** 32-bit (Big Endian).
* **Instruction Width:** Dual-width (4-byte or 8-byte).
* **Encoding Logic:** If the first hexit is `A`, the instruction is **8 bytes** (includes a 32-bit immediate). Otherwise, it is **4 bytes**.
* **Instruction Format:** `aabcdefghhhhhhhh`
    * `aa`: Opcode
    * `b-g`: Register/Operand nibbles
    * `hhhhhhhh`: 32-bit immediate (for 8-byte instructions)
* **Registers:** 16 registers, all holding 32-bit integers. They're stored as raw bits, each instruction decides whether to read them as signed or unsigned.

## Register Notation
Registers can be written three ways, all equivalent:
* **Decimal** (what the examples use): `%0` - `%15`. Multi-digit numbers are ALWAYS decimal: `%10` is register ten, not sixteen.
* **Hex letters:** `%A` - `%F` for registers 10-15 (case-insensitive).
* **Optional `r` prefix:** `%r0`, `%rE`, `%rF`.

So `%14` = `%E` = `%rE` = SP, and `%15` = `%F` = `%rF` = RA.

---

## Running SupremeASM

To assemble SupremeASM code into machine code, run the Python script asm.py in the "assembler" folder, with args: python asm.py <filename.sasm> [--autorun] [--dpr] [--dpm]. 

To run machine code, run main.exe in the "vm" folder, with args: ./main <filename> [--dpr] [--dpm]

"filename" is the file path to the desired file. "--autorun" specifies if the Python script should immediately run the compiled machine code (works from any directory — it finds the VM on its own). Not including this flag means the code will only compile. "--dpr" and "--dpm" tells the VM to dump registers and dump memory after execution into the console, respectively.
Including these flags when running the Python script without "--autorun" does nothing.

Compiled .smc files are written to the **executable** folder at the repo root (created automatically).

**IMPORTANT**: All Supreme Machine Code files have file extension .smc, and all SupremeASM v2 files have file extension .sasm. To differentiate from SupremeASM v1, SupremeASM v2 files must have header "@version 2". See examples for uh...examples.

**ALSO IMPORTANT**: The makefile is MSVC/Windows (nmake). The current compiled C code found in vm/main.exe probably won't work if you're on a different OS or hardware or something or other tbh I don't really get makefiles. On Linux/macOS you'll probably have to recompile main.c with like gcc -std=c11 or whatever. Anyways it works on my machine but YMMV.

## Register Conventions
While all registers are technically general-purpose, the general conventions are as follows:

| Register | Usage |
| :--- | :--- |
| `%r0 - %r3` | Syscall Arguments / Return Values |
| `%r4 - %r9` | General Purpose (Compiler Scratch) |
| `%rA - %rD` | Function Parameters |
| `%rE` (`%14`) | **SP** (Stack Pointer) |
| `%rF` (`%15`) | **RA** (Return Address) |

---

## PC
The program counter points to the next instruction, not the current instruction. Useful to know for jumps and stuff. BUT WE HAVE LABELS SO IT DOESN'T MATTER WHOOOOO

## Assembler Rules & Errors
The assembler fails with a line number if you:
* use an unknown instruction, an invalid register (`%G`, `%16`), or a malformed immediate
* reference an undefined label, or define the same label twice
* use a branch/load/store offset outside **-32768..32767** (16-bit signed)
* use a shift amount outside **-128..127** (8-bit signed)
* use an `ld` immediate outside **-2147483648..4294967295** (so `#0xFFFFFFFF` is legal and loads -1)

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
| `shl %r, %s, %o` | `23rso--0` | `%o = %r << %s`|
| `shr %r, %s, %o` | `23rso--1` | `%o = %r >> %s`|
| `add %r, %s, %o` | `24rso---` | `%o = %r + %s` |
| `sub %r, %s, %o` | `25rso---` | `%o = %r - %s` |
| `mul %r, %s, %o` | `26rso---` | `%o = %r * %s` |
| `div %r, %s, %o` | `27rso---` | `%o = %r / %s` (signed, traps on divide-by-zero) |
| `mod %r, %s, %o` | `28rso---` | `%o = %r % %s` (signed, traps on divide-by-zero) |
| `and %r, %s, %o` | `29rso---` | `%o = %r & %s` |
| `or %r, %s, %o` | `2Arso---` | `%o = %r \| %s` |
| `xor %r, %s, %o` | `2Brso---` | `%o = %r ^ %s` |
| `not %r, %s` | `2Crs----` | `%s = ~%r` |
| `br #o` | `30--oooo` | Relative jump: `PC = PC + o` (signed `o`) |
| `bre %r, %s, #o` | `31rsoooo` | Relative jump if `%r == %s` |
| `brn %r, %s, #o` | `32rsoooo` | Relative jump if `%r != %s` |
| `bgs %r, %s, #o` | `33rsoooo` | Relative jump if `%r > %s` (**signed** compare) |
| `bgu %r, %s, #o` | `34rsoooo` | Relative jump if `%r > %s` (**unsigned** compare) |
| `gpc %r, #o` | `35r-oooo` | Load `PC + o` into `%r` (signed `o`) |
| `jmp %r` | `36r-----` | Absolute jump: `PC = %r` |
| `halt` | `ffffffff` | Terminate VM execution |

Notes:
* Shift counts are taken mod 32 by the VM (only the low 5 bits matter).
* All offsets (`#o`) are signed 16-bit. Immediates can be decimal or hex (`#42`, `#0x2A`).

---

## System Operation

### Memory Map (8 MB)
| Region | Range | Purpose |
| :--- | :--- | :--- |
| Guard | `0x000000 - 0x000FFF` | Unused guard page |
| Program | `0x001000 - heap start` | Loaded at `0x1000` (page-aligned) |
| Heap | `heap start - 0x3FFFFF` | Begins right after the program, rounded up to a 4 KB page; grows up via `sbrk` |
| Guard | `0x400000 - 0x7EFFFF` | Unmapped gap between user memory and the stack |
| Stack | `0x7F0000 - 0x7FFFFF` | SP (`%r14`) starts at `0x7FFFFF` and grows down. |

On program entry: `%r0` = heap start, `%r14` = top of stack. Every memory access is bounds-checked, so bad addresses exit with "Segmentation Fault" instead of crashing the VM.

### System Calls (`syscall`)
**Encoding:** `f0------`
Arguments are passed via `%r0-%r3`. Results are returned in `%r0`. On failure (bad FD, unknown ID), `%r0` = -1.

| ID (`%r0`) | Function | Inputs | Return (`%r0`) |
| :--- | :--- | :--- | :--- |
| **0** | `read` | `%r1`: FD, `%r2`: Buffer, `%r3`: Size | Bytes read, or -1 |
| **1** | `write` | `%r1`: FD, `%r2`: Buffer, `%r3`: Size | Bytes written, or -1 |
| **2** | `get_time`| None | MS since program start (wall clock) |
| **3** | `sbrk` | `%r3`: Increment size | Old heap end address |

Currently, FD = 0 (stdin) and FD = 1 (stdout) are the only supported file descriptors — anything else returns -1 (it does NOT silently perform a different syscall anymore lol). Reading and writing from other files is planned. Reading from stdin returns each character as its ascii representation and stops at a newline. A failed `sbrk` returns -1 in `%r0`.
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

Factorial Calculator. Takes input from user and calculates factorial (only works up to around 12! due to integer overflow).
```
@version 2
// gets user input n and finds n! recursively

// Load text and output
ld #0x2000, %2;
ld #0x456E7465, %4;
sti %4, %2+#0;
ld #0x72206E3A, %4;
sti %4, %2+#4;
ld #0x20, %4;
stb %4, %2+#8;
ld #1, %0;
ld #1, %1;
ld #9, %3;
syscall;
// wait for user input
ld #0, %0;
ld #0, %1;
ld #0x3000, %2;
ld #16, %3;
syscall;
// convert ascii to int
ld #0, %0;
ld #10, %4;
ld #48, %5;
ld #10, %7;
lbl: atoi_loop;
ldb %2+#0, %6;
bre %6, %7, atoi_done;
bre %6, %0, atoi_done;
mul %0, %4, %0;
sub %6, %5, %6; 
add %0, %6, %0;
inc %2;
br atoi_loop;
lbl: atoi_done;
// int in r0, start factorial calculation
ld #4, %4;
sub %14, %4, %14;
sti %0, %14+#0;
gpc %15, factorial_calculated;
br calculate_factorial;
lbl: factorial_calculated;
// result in r0, need to convert int to ascii
ld #10, %4;
ld #48, %5;
ld #0, %7; 
ld #0, %3;
lbl: itoa_loop;
mod %0, %4, %6;
div %0, %4, %0; 
add %6, %5, %6;
dec %2;
stb %6, %2+#0;
inc %3;
bgs %0, %7, itoa_loop; 
ld #1, %0;
ld #1, %1;
syscall;
halt;
lbl: calculate_factorial;
sub %14, %4, %14;
sti %15, %14+#0;
ldi %14+#4, %0;
ld #2, %5;
bgs %5, %0, base_case;
mov %0, %6;
ld #1, %7;
sub %6, %7, %6;
sub %14, %4, %14;
sti %6, %14+#0;
gpc %15, ret;
br calculate_factorial;
lbl: ret;
add %14, %4, %14;
ldi %14+#4, %6;
mul %0, %6, %0;
br end_loop;
lbl: base_case;
ld #1, %0;
lbl: end_loop;
ldi %14+#0, %15;
add %14, %4, %14;
jmp %15;
```
