#include <stdint.h>
#include <string.h>
#include "../include/memory.h"
#include "../include/common.h"
#include "../include/cpu.h"
#include "../include/os.h"

void init_cpu(CPU *cpu)
{
    cpu->pc = 0;
    memset(cpu->regs, 0, sizeof(cpu->regs));
    cpu->status = STATUS_OK;
}

Status cpu_run(CPU *cpu, uint32_t init_pc)
{
    cpu->pc = init_pc;

    while (cpu->status == STATUS_OK)
    {
        /* fetch */
        uint32_t ins = read_int(cpu->pc, &cpu->status);
        cpu->pc += 4;
        if (cpu->status != STATUS_OK)
            break;

        /* decode */
        uint8_t op  = (ins >> 24) & 0xFF;
        uint8_t r   = (ins >> 20) & 0xF;
        uint8_t s   = (ins >> 16) & 0xF;
        uint8_t o   = (ins >> 12) & 0xF;
        int16_t off = (int16_t)(ins & 0xFFFF);

        /* execute with one switch statement */
        switch (op)
        {
        case 0xA0: /* ld #imm, %r  (8-byte) */
        {
            uint32_t imm = read_int(cpu->pc, &cpu->status);
            cpu->pc += 4;
            if (cpu->status != STATUS_OK)
                break;
            cpu->regs[r] = imm;
            break;
        }
        case 0x00: /* ldb %r+#o, %s */
        {
            uint8_t b = read_byte(cpu->regs[r] + off, &cpu->status);
            cpu->regs[s] = (int32_t)(int8_t)b;
            break;
        }
        case 0x01: /* ldi %r+#o, %s */
            cpu->regs[s] = read_int(cpu->regs[r] + off, &cpu->status);
            break;
        case 0x02: /* ldb %r+%o, %s */
        {
            uint8_t b = read_byte(cpu->regs[r] + cpu->regs[o], &cpu->status);
            cpu->regs[s] = (int32_t)(int8_t)b;
            break;
        }
        case 0x03: /* ldi %r+%o, %s */
            cpu->regs[s] = read_int(cpu->regs[r] + cpu->regs[o], &cpu->status);
            break;
        case 0x10: /* stb %r, %s+#o */
            write_byte(cpu->regs[s] + off, (uint8_t)(cpu->regs[r] & 0xFF), &cpu->status);
            break;
        case 0x11: /* sti %r, %s+#o */
            write_int(cpu->regs[s] + off, cpu->regs[r], &cpu->status);
            break;
        case 0x12: /* stb %r, %s+%o */
            write_byte(cpu->regs[s] + cpu->regs[o], (uint8_t)(cpu->regs[r] & 0xFF), &cpu->status);
            break;
        case 0x13: /* sti %r, %s+%o */
            write_int(cpu->regs[s] + cpu->regs[o], cpu->regs[r], &cpu->status);
            break;
        case 0x20: /* mov %r, %s */
            cpu->regs[s] = cpu->regs[r];
            break;
        case 0x21: /* inc %r */
            cpu->regs[r]++;
            break;
        case 0x22: /* dec %r */
            cpu->regs[r]--;
            break;
        case 0x23: /* shf %r<<#v / %r>>#v, %s */
        {
            int8_t val = (int8_t)(ins & 0xFF);
            if (val < 0)
                cpu->regs[s] = (uint32_t)((int32_t)cpu->regs[r] >> (-val & 0x1F));
            else
                cpu->regs[s] = cpu->regs[r] << (val & 0x1F);
            break;
        }
        case 0x24: /* add */
            cpu->regs[o] = cpu->regs[r] + cpu->regs[s];
            break;
        case 0x25: /* sub */
            cpu->regs[o] = cpu->regs[r] - cpu->regs[s];
            break;
        case 0x26: /* mul */
            cpu->regs[o] = cpu->regs[r] * cpu->regs[s];
            break;
        case 0x27: /* div (signed) */
            if (cpu->regs[s] == 0) { cpu->status = STATUS_ERR_DIV_ZERO; break; }
            cpu->regs[o] = (uint32_t)((int32_t)cpu->regs[r] / (int32_t)cpu->regs[s]);
            break;
        case 0x28: /* mod (signed) */
            if (cpu->regs[s] == 0) { cpu->status = STATUS_ERR_DIV_ZERO; break; }
            cpu->regs[o] = (uint32_t)((int32_t)cpu->regs[r] % (int32_t)cpu->regs[s]);
            break;
        case 0x29: /* and */
            cpu->regs[o] = cpu->regs[r] & cpu->regs[s];
            break;
        case 0x2A: /* or */
            cpu->regs[o] = cpu->regs[r] | cpu->regs[s];
            break;
        case 0x2B: /* xor */
            cpu->regs[o] = cpu->regs[r] ^ cpu->regs[s];
            break;
        case 0x2C: /* not */
            cpu->regs[s] = ~cpu->regs[r];
            break;
        case 0x30: /* br #o */
            cpu->pc += off;
            break;
        case 0x31: /* bre */
            if (cpu->regs[r] == cpu->regs[s]) cpu->pc += off;
            break;
        case 0x32: /* brn */
            if (cpu->regs[r] != cpu->regs[s]) cpu->pc += off;
            break;
        case 0x33: /* bgs (signed) */
            if ((int32_t)cpu->regs[r] > (int32_t)cpu->regs[s]) cpu->pc += off;
            break;
        case 0x34: /* bgu (unsigned) */
            if (cpu->regs[r] > cpu->regs[s]) cpu->pc += off;
            break;
        case 0x35: /* gpc %r, #o */
            cpu->regs[r] = cpu->pc + off;
            break;
        case 0x36: /* jmp %r */
            cpu->pc = cpu->regs[r];
            break;
        case 0xF0: /* syscall */
            cpu->status = handle_syscall(&cpu->regs[0], cpu->regs[1], cpu->regs[2], cpu->regs[3]);
            break;
        case 0xFF: /* halt */
            cpu->status = STATUS_HALTED;
            break;
        default:
            cpu->status = STATUS_ERR_UNKNOWN_OP;
            break;
        }
    }
    return cpu->status;
}