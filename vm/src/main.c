#include <stdio.h>
#include <inttypes.h>
#include <string.h>
#include <stdbool.h>
#include "../include/memory.h"
#include "../include/common.h"
#include "../include/cpu.h"
#include "../include/os.h"

#define ONE_MB 1048576
#define EIGHT_MB 8388608
/*
    ISA:
    (4 or 8 byte instructions)
    (first hexit = A -> 8 byte instruction)
    given some instruction aabcdefghhhhhhhh
    aa = op code, b = op1, c = op2, d = op3, e = op4, f = op5, g = op6, hhhhhhhh = opimm
    second hexit:
    0 - load
    1 - store
    2 - math
    3 - program flow
    f - sys calls
    INTs are always signed, BYTEs are not unless specified.
    Since registers store ints, regs are always signed
    v -> r[r]    A0r-----vvvvvvvv, load INT
    m[r[r] + o] -> r[s]     00rsoooo, load BYTE+o (signed BYTE, signed o)
    m[r[r] + o] -> r[s]     01rsoooo, load INT+o (signed o)
    m[r[r] + r[o]] -> r[s]     02rso---, load indexed BYTE (signed BYTE)
    m[r[r] + r[o]] -> r[s]     03rso---, load indexed INT
    r[r] -> m[r[s] + o]     10rsoooo, store BYTE+o (signed o)
    r[r] -> m[r[s] + o]     11rsoooo, store INT+o (signed o)
    r[r] -> m[r[s] + r[o]]     12rso---, store indexed BYTE
    r[r] -> m[r[s] + r[o]]     13rso---, store indexed INT
    r[r] -> r[s]    20rs----, mov
    r[r]++      21r-----, inc
    r[r]--      22r-----, dec
    if v < 0: r[r] >> v -> r[s] else: r[r] << v -> r[s]     23rs--vv, bitshift right if v < 0, otherwise bitshift left (signed v duh)
    r[r] + r[s] -> r[o]     24rso---, add
    r[r] - r[s] -> r[o]     25rso---, sub
    r[r] * r[s] -> r[o]     26rso---, mul
    r[r] / r[s] -> r[o]     27rso---, div
    r[r] % r[s] -> r[o]     28rso---, mod
    r[r] & r[s] -> r[o]     29rso---, and
    r[r] | r[s] -> r[o]     2Arso---, or
    r[r] ^ r[s] -> r[o]     2Brso---, xor
    ~r[r] -> r[s]   2Crs----, not
    pc + o -> pc    30--oooo, indirect jump (signed o)
    if r[r] == r[s]: pc+o->pc   31rsoooo, ife jump (signed o)
    if r[r] != r[s]: pc+o->pc   32rsoooo, ifne jump (signed o)
    if r[r] > r[s]: pc+o->pc    33rsoooo, igt jump (signed o) (signed comparison)
    if r[r] > r[s]: pc+o->pc    34rsoooo, igt jump (signed o) (unsigned comparison)
    pc + o -> r[r]      35r-oooo, load PC+o (signed o)
    r[r] -> pc      36r-----, direct jump
    syscall     f0------, see below:
    r0, r1, r2, r3 are arguments
    r0: id (0 = read, 1 = write, 2 = get_time (since program start, in ms), 3 = sbrk)
    r1: fd (0 = stdin, 1 = stdout)
    r2: buffer
    r3: size 

    return value in r0: (id = 0: number of bytes read, id = 1: number of bytes written, id = 2: time in ms, id = 3: end address of old heap (start address of extended section of heap))

generally: r0-r3 are args, r4-r9 general purpose, rA-rD fn params, rE sp, rF ra. r0 return value of syscall


halt    ffffffff


*/

int main(int argc, char *argv[])
{
    if (argc < 2)
    {
        printf("Usage: %s <filename> [--dpm] [--dpr]\n", argv[0]);
        return 0;
    }
    bool mem_dump = false;
    bool reg_dump = false;
    for (int i = 2; i < argc; i++)
    {
        if (strcmp(argv[i], "--dpm") == 0)
        {
            mem_dump = true;
        }
        if (strcmp(argv[i], "--dpr") == 0)
        {
            reg_dump = true;
        }
    }
    CPU cpu;
    init_os(&cpu, EIGHT_MB);
    Status status;
    uint32_t pc = load_program(argv[1], &status);
    if (status != STATUS_OK)
        return 1;
    status = run_program(&cpu, pc);
    if (reg_dump)
    {
        printf("\n--- Register Dump ---\n");
        for (int i = 0; i < 16; i++)
        {
            printf("r%d: 0x%08X (%d)\n", i, cpu.regs[i], cpu.regs[i]);
        }
    }
    if (mem_dump)
    {
        printf("\n--- Memory Dump ---\n");
        for (int i = 0; i < EIGHT_MB; i += 1)
        {
            uint8_t val = read_byte(i, &status);
            if (val != 0)
            {
                printf("0x%08X(%d): 0x%02X(%d)\n", i, i, val, val);
            }
        }
    }
    return 0;
}
