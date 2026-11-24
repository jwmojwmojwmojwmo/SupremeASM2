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

static uint32_t checkOverflow(uint32_t base, int64_t off, Status *status)
{
    // checking for 32 bit overflow
    uint64_t a = (uint64_t)base + (uint64_t)off;
    if (a > 0xFFFFFFFFull)
    {
        fault_addr = base; fault_len = 1; fault_kind = 3;
        *status = STATUS_ERR_SEGFAULT;
        return 0;
    }
    return (uint32_t)a;
}

static int checkPC(uint32_t pc, uint32_t len, Status *status)
{
    if (pc < prog_base || (uint64_t)pc + len > code_end)
    {
        fault_addr = pc; fault_len = len; fault_kind = 2;
        *status = STATUS_ERR_SEGFAULT;
        return 0;
    }
    return 1;
}

Status cpu_run(CPU *cpu, uint32_t init_pc)
{
    cpu->pc = init_pc;

    while (cpu->status == STATUS_OK)
    {
        // fetch
        if (!checkPC(cpu->pc, 4, &cpu->status)) break;
        uint32_t ins = read_int(cpu->pc, &cpu->status);
        cpu->pc += 4;
        if (cpu->status != STATUS_OK)
            break;

        // decode & execute
        uint8_t op = (ins >> 24) & 0xFF;
        uint8_t r = (ins >> 20) & 0xF;
        uint8_t s = (ins >> 16) & 0xF;
        uint8_t o = (ins >> 12) & 0xF;
        int16_t off = (int16_t)(ins & 0xFFFF);

        switch (op)
        {
        case 0xA0:
        {
            if (!checkPC(cpu->pc, 4, &cpu->status)) break;
            uint32_t imm = read_int(cpu->pc, &cpu->status);
            cpu->pc += 4;
            if (cpu->status != STATUS_OK) break;
            cpu->regs[r] = imm;
            break;
        }
        case 0x00: {
            uint32_t a = checkOverflow(cpu->regs[r], off, &cpu->status);
            if (cpu->status != STATUS_OK) break;
            cpu->regs[s] = (int32_t)(int8_t)read_byte(a, &cpu->status);
            break;
        }
        case 0x01: {
            uint32_t a = checkOverflow(cpu->regs[r], off, &cpu->status);
            if (cpu->status != STATUS_OK) break;
            cpu->regs[s] = read_int(a, &cpu->status);
            break;
        }
        case 0x02: {
            uint32_t a = checkOverflow(cpu->regs[r], (int64_t)cpu->regs[o], &cpu->status);
            if (cpu->status != STATUS_OK) break;
            cpu->regs[s] = (int32_t)(int8_t)read_byte(a, &cpu->status);
            break;
        }
        case 0x03: {
            uint32_t a = checkOverflow(cpu->regs[r], (int64_t)cpu->regs[o], &cpu->status);
            if (cpu->status != STATUS_OK) break;
            cpu->regs[s] = read_int(a, &cpu->status);
            break;
        }
        case 0x10: {
            uint32_t a = checkOverflow(cpu->regs[s], off, &cpu->status);
            if (cpu->status != STATUS_OK) break;
            write_byte(a, (uint8_t)(cpu->regs[r] & 0xFF), &cpu->status);
            break;
        }
        case 0x11: {
            uint32_t a = checkOverflow(cpu->regs[s], off, &cpu->status);
            if (cpu->status != STATUS_OK) break;
            write_int(a, cpu->regs[r], &cpu->status);
            break;
        }
        case 0x12: {
            uint32_t a = checkOverflow(cpu->regs[s], (int64_t)cpu->regs[o], &cpu->status);
            if (cpu->status != STATUS_OK) break;
            write_byte(a, (uint8_t)(cpu->regs[r] & 0xFF), &cpu->status);
            break;
        }
        case 0x13: {
            uint32_t a = checkOverflow(cpu->regs[s], (int64_t)cpu->regs[o], &cpu->status);
            if (cpu->status != STATUS_OK) break;
            write_int(a, cpu->regs[r], &cpu->status);
            break;
        }
        case 0x20: cpu->regs[s] = cpu->regs[r]; break;
        case 0x21: cpu->regs[r]++; break;
        case 0x22: cpu->regs[r]--; break;
        case 0x23: {
            uint8_t sub_op = ins & 0xF;
            uint32_t shift_amount = cpu->regs[s] & 0x1F;
            if (sub_op == 0)
                cpu->regs[o] = cpu->regs[r] << shift_amount;
            else if (sub_op == 1)
                cpu->regs[o] = (uint32_t)((int32_t)cpu->regs[r] >> shift_amount);
            break;
        }
        case 0x24: cpu->regs[o] = cpu->regs[r] + cpu->regs[s]; break;
        case 0x25: cpu->regs[o] = cpu->regs[r] - cpu->regs[s]; break;
        case 0x26: cpu->regs[o] = cpu->regs[r] * cpu->regs[s]; break;
        case 0x27:
            if (cpu->regs[s] == 0 ||
                (cpu->regs[r] == 0x80000000u && cpu->regs[s] == 0xFFFFFFFFu))
            {
                cpu->status = STATUS_ERR_DIV_ZERO;
                break;
            }
            cpu->regs[o] = (uint32_t)((int32_t)cpu->regs[r] / (int32_t)cpu->regs[s]);
            break;
        case 0x28:
            if (cpu->regs[s] == 0 ||
                (cpu->regs[r] == 0x80000000u && cpu->regs[s] == 0xFFFFFFFFu))
            {
                cpu->status = STATUS_ERR_DIV_ZERO;
                break;
            }
            cpu->regs[o] = (uint32_t)((int32_t)cpu->regs[r] % (int32_t)cpu->regs[s]);
            break;
        case 0x29: cpu->regs[o] = cpu->regs[r] & cpu->regs[s]; break;
        case 0x2A: cpu->regs[o] = cpu->regs[r] | cpu->regs[s]; break;
        case 0x2B: cpu->regs[o] = cpu->regs[r] ^ cpu->regs[s]; break;
        case 0x2C: cpu->regs[s] = ~cpu->regs[r]; break;
        case 0x30: cpu->pc += off; break;
        case 0x31: if (cpu->regs[r] == cpu->regs[s]) cpu->pc += off; break;
        case 0x32: if (cpu->regs[r] != cpu->regs[s]) cpu->pc += off; break;
        case 0x33: if ((int32_t)cpu->regs[r] > (int32_t)cpu->regs[s]) cpu->pc += off; break;
        case 0x34: if (cpu->regs[r] > cpu->regs[s]) cpu->pc += off; break;
        case 0x35: cpu->regs[r] = cpu->pc + off; break;
        case 0x36: cpu->pc = cpu->regs[r]; break;
        case 0xF0:
            cpu->status = handle_syscall(&cpu->regs[0], cpu->regs[1], cpu->regs[2], cpu->regs[3]);
            break;
        case 0xFF: cpu->status = STATUS_HALTED; break;
        default: cpu->status = STATUS_ERR_UNKNOWN_OP; break;
        }
    }
    return cpu->status;
}