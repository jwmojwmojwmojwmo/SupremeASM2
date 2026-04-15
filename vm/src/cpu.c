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
        uint32_t first_ins;
        uint32_t second_ins = 0;
        cpu_fetch(cpu, &first_ins, &second_ins);
        uint8_t opcode = (first_ins >> 24) & 0xFF;
        uint32_t opimm = second_ins;
        if (cpu->status != STATUS_OK)
            return cpu->status;
        if (((opcode >> 4) & 0xF) == 0xA)
        {
            cpu_execute_extended(cpu, opcode, first_ins, opimm);
        }
        else        
        {
            cpu_execute(cpu, opcode, first_ins);
        }
    }
    return cpu->status;
}

void cpu_fetch(CPU *cpu, uint32_t *first_ins, uint32_t *second_ins)
{
    *first_ins = read_int(cpu->pc, &cpu->status);
    cpu->pc += 4;
    if (cpu->status != STATUS_OK)
        return;
    if (((*first_ins >> 28) & 0xF) == 0xA)
    {
        *second_ins = read_int(cpu->pc, &cpu->status);
        cpu->pc += 4;
    }
}

void cpu_execute(CPU *cpu, uint8_t opcode, uint32_t first_ins)
{
    uint8_t ophexit = (opcode >> 4) & 0xF;
    switch (ophexit)
    {
    case 0x0:
        load(cpu, opcode, first_ins);
        break;
    case 0x1:
        store(cpu, opcode, first_ins);
        break;
    case 0x2:
        math(cpu, opcode, first_ins);
        break;
    case 0x3:
        program_flow(cpu, opcode, first_ins);
        break;
    case 0xF:
        syscall(cpu, opcode, first_ins);
        break;
    default:
        cpu->status = STATUS_ERR_UNKNOWN_OP;
        break;
    }
}

void cpu_execute_extended(CPU *cpu, uint8_t opcode, uint32_t first_ins, uint32_t opimm)
{
    switch (opcode)
    {
    case 0xA0:
        // load INT immediate
        uint8_t op_one = (first_ins >> 20) & 0xF;
        cpu->regs[op_one] = opimm;
        break;
    default:
        cpu->status = STATUS_ERR_UNKNOWN_OP;
        break;
    }
}

void load(CPU *cpu, uint8_t opcode, uint32_t first_ins)
{
    switch (opcode)
    {
    case 0x00:
    {
        // load BYTE+o (signed)
        uint8_t op_one = (first_ins >> 20) & 0xF;
        uint8_t op_two = (first_ins >> 16) & 0xF;
        uint8_t op_three = (first_ins >> 12) & 0xF;
        uint8_t op_four = (first_ins >> 8) & 0xF;
        uint8_t op_five = (first_ins >> 4) & 0xF;
        uint8_t op_six = first_ins & 0xF;
        uint8_t high = (op_three << 4) | op_four;
        uint8_t low = (op_five << 4) | op_six;
        int16_t offset = (int16_t)(((uint16_t)high << 8) | low);
        uint8_t byte = read_byte(cpu->regs[op_one] + offset, &cpu->status);
        cpu->regs[op_two] = (int32_t)(int8_t)byte;
        break;
    }
    case 0x01:
    {
        // load INT+o (signed)
        uint8_t op_one = (first_ins >> 20) & 0xF;
        uint8_t op_two = (first_ins >> 16) & 0xF;
        uint8_t op_three = (first_ins >> 12) & 0xF;
        uint8_t op_four = (first_ins >> 8) & 0xF;
        uint8_t op_five = (first_ins >> 4) & 0xF;
        uint8_t op_six = first_ins & 0xF;
        uint8_t high = (op_three << 4) | op_four;
        uint8_t low = (op_five << 4) | op_six;
        int16_t offset = (int16_t)(((uint16_t)high << 8) | low);
        cpu->regs[op_two] = read_int(cpu->regs[op_one] + offset, &cpu->status);
        break;
    }
    case 0x02:
    {
        // load indexed BYTE (signed)
        uint8_t op_one = (first_ins >> 20) & 0xF;
        uint8_t op_two = (first_ins >> 16) & 0xF;
        uint8_t op_three = (first_ins >> 12) & 0xF;
        uint8_t byte = read_byte(cpu->regs[op_one] + cpu->regs[op_three], &cpu->status);
        cpu->regs[op_two] = (int32_t)(int8_t)byte;
        break;
    }
    case 0x03:
    {
        // load indexed INT (signed)
        uint8_t op_one = (first_ins >> 20) & 0xF;
        uint8_t op_two = (first_ins >> 16) & 0xF;
        uint8_t op_three = (first_ins >> 12) & 0xF;
        cpu->regs[op_two] = read_int(cpu->regs[op_one] + cpu->regs[op_three], &cpu->status);
        break;
    }
    default:
        cpu->status = STATUS_ERR_UNKNOWN_OP;
        break;
    }
}

void store(CPU *cpu, uint8_t opcode, uint32_t first_ins)
{
    switch (opcode)
    {
    case 0x10:
    {
        // store BYTE+o
        uint8_t op_one = (first_ins >> 20) & 0xF;
        uint8_t op_two = (first_ins >> 16) & 0xF;
        uint8_t op_three = (first_ins >> 12) & 0xF;
        uint8_t op_four = (first_ins >> 8) & 0xF;
        uint8_t op_five = (first_ins >> 4) & 0xF;
        uint8_t op_six = first_ins & 0xF;
        uint8_t high = (op_three << 4) | op_four;
        uint8_t low = (op_five << 4) | op_six;
        int16_t offset = (int16_t)(((uint16_t)high << 8) | low);
        write_byte(cpu->regs[op_two] + offset, (uint8_t)(cpu->regs[op_one] & 0xFF), &cpu->status);
        break;
    }
    case 0x11:
    {
        // store INT+o
        uint8_t op_one = (first_ins >> 20) & 0xF;
        uint8_t op_two = (first_ins >> 16) & 0xF;
        uint8_t op_three = (first_ins >> 12) & 0xF;
        uint8_t op_four = (first_ins >> 8) & 0xF;
        uint8_t op_five = (first_ins >> 4) & 0xF;
        uint8_t op_six = first_ins & 0xF;
        uint8_t high = (op_three << 4) | op_four;
        uint8_t low = (op_five << 4) | op_six;
        int16_t offset = (int16_t)(((uint16_t)high << 8) | low);
        write_int(cpu->regs[op_two] + offset, cpu->regs[op_one], &cpu->status);
        break;
    }
    case 0x12:
    {
        // store indexed BYTE
        uint8_t op_one = (first_ins >> 20) & 0xF;
        uint8_t op_two = (first_ins >> 16) & 0xF;
        uint8_t op_three = (first_ins >> 12) & 0xF;
        write_byte(cpu->regs[op_two] + cpu->regs[op_three], (uint8_t)(cpu->regs[op_one] & 0xFF), &cpu->status);
        break;
    }
    case 0x13:
    {
        // store indexed INT
        uint8_t op_one = (first_ins >> 20) & 0xF;
        uint8_t op_two = (first_ins >> 16) & 0xF;
        uint8_t op_three = (first_ins >> 12) & 0xF;
        write_int(cpu->regs[op_two] + cpu->regs[op_three], cpu->regs[op_one], &cpu->status);
        break;
    }
    default:
        cpu->status = STATUS_ERR_UNKNOWN_OP;
        break;
    }
}

void math(CPU *cpu, uint8_t opcode, uint32_t first_ins)
{
    switch (opcode)
    {
    case 0x20:
    {
        // mov
        uint8_t op_one = (first_ins >> 20) & 0xF;
        uint8_t op_two = (first_ins >> 16) & 0xF;
        cpu->regs[op_two] = cpu->regs[op_one];
        break;
    }
    case 0x21:
    {
        // inc
        uint8_t op_one = (first_ins >> 20) & 0xF;
        cpu->regs[op_one]++;
        break;
    }
    case 0x22:
    {
        // dec
        uint8_t op_one = (first_ins >> 20) & 0xF;
        cpu->regs[op_one]--;
        break;
    }
    case 0x23:
    {
        // bitshift
        uint8_t op_one = (first_ins >> 20) & 0xF;
        uint8_t op_two = (first_ins >> 16) & 0xF;
        uint8_t op_five = (first_ins >> 4) & 0xF;
        uint8_t op_six = first_ins & 0xF;
        int8_t val = (int8_t)((op_five << 4) | op_six);
        if (val < 0)
        {
            cpu->regs[op_two] = cpu->regs[op_one] >> (-val & 0x1F);
        }
        else
        {
            cpu->regs[op_two] = cpu->regs[op_one] << (val & 0x1F);
        }
        break;
    }
    case 0x24:
    {
        // add
        uint8_t op_one = (first_ins >> 20) & 0xF;
        uint8_t op_two = (first_ins >> 16) & 0xF;
        uint8_t op_three = (first_ins >> 12) & 0xF;
        cpu->regs[op_three] = cpu->regs[op_one] + cpu->regs[op_two];
        break;
    }
    case 0x25:
    {
        // sub
        uint8_t op_one = (first_ins >> 20) & 0xF;
        uint8_t op_two = (first_ins >> 16) & 0xF;
        uint8_t op_three = (first_ins >> 12) & 0xF;
        cpu->regs[op_three] = cpu->regs[op_one] - cpu->regs[op_two];
        break;
    }
    case 0x26:
    {
        // mul
        uint8_t op_one = (first_ins >> 20) & 0xF;
        uint8_t op_two = (first_ins >> 16) & 0xF;
        uint8_t op_three = (first_ins >> 12) & 0xF;
        cpu->regs[op_three] = cpu->regs[op_one] * cpu->regs[op_two];
        break;
    }
    case 0x27:
    {
        // div
        uint8_t op_one = (first_ins >> 20) & 0xF;
        uint8_t op_two = (first_ins >> 16) & 0xF;
        uint8_t op_three = (first_ins >> 12) & 0xF;
        if (cpu->regs[op_two] == 0)
        {
            cpu->status = STATUS_ERR_DIV_ZERO;
            break;
        }
        cpu->regs[op_three] = (uint32_t)((int32_t)cpu->regs[op_one] / (int32_t)cpu->regs[op_two]);
        break;
    }
    case 0x28:
    {
        // mod
        uint8_t op_one = (first_ins >> 20) & 0xF;
        uint8_t op_two = (first_ins >> 16) & 0xF;
        uint8_t op_three = (first_ins >> 12) & 0xF;
        if (cpu->regs[op_two] == 0)
        {
            cpu->status = STATUS_ERR_DIV_ZERO;
            break;
        }
        cpu->regs[op_three] = (uint32_t)((int32_t)cpu->regs[op_one] % (int32_t)cpu->regs[op_two]);
        break;
    }
    case 0x29:
    {
        // and
        uint8_t op_one = (first_ins >> 20) & 0xF;
        uint8_t op_two = (first_ins >> 16) & 0xF;
        uint8_t op_three = (first_ins >> 12) & 0xF;
        cpu->regs[op_three] = cpu->regs[op_one] & cpu->regs[op_two];
        break;
    }
    case 0x2A:
    {
        // or
        uint8_t op_one = (first_ins >> 20) & 0xF;
        uint8_t op_two = (first_ins >> 16) & 0xF;
        uint8_t op_three = (first_ins >> 12) & 0xF;
        cpu->regs[op_three] = cpu->regs[op_one] | cpu->regs[op_two];
        break;
    }
    case 0x2B:
    {
        // xor
        uint8_t op_one = (first_ins >> 20) & 0xF;
        uint8_t op_two = (first_ins >> 16) & 0xF;
        uint8_t op_three = (first_ins >> 12) & 0xF;
        cpu->regs[op_three] = cpu->regs[op_one] ^ cpu->regs[op_two];
        break;
    }
    case 0x2C:
    {
        // not
        uint8_t op_one = (first_ins >> 20) & 0xF;
        uint8_t op_two = (first_ins >> 16) & 0xF;
        uint8_t op_three = (first_ins >> 12) & 0xF;
        cpu->regs[op_two] = ~cpu->regs[op_one];
        break;
    }
    default:
        cpu->status = STATUS_ERR_UNKNOWN_OP;
        break;
    }
}

void program_flow(CPU *cpu, uint8_t opcode, uint32_t first_ins)
{
    uint8_t op_one = (first_ins >> 20) & 0xF;
    uint8_t op_two = (first_ins >> 16) & 0xF;
    uint8_t op_three = (first_ins >> 12) & 0xF;
    uint8_t op_four = (first_ins >> 8) & 0xF;
    uint8_t op_five = (first_ins >> 4) & 0xF;
    uint8_t op_six = first_ins & 0xF;
    uint8_t high = (op_three << 4) | op_four;
    uint8_t low = (op_five << 4) | op_six;
    int16_t offset = (int16_t)(((uint16_t)high << 8) | low);
    switch (opcode)
    {
    case 0x30:
        // indirect jump
        cpu->pc += offset;
        break;
    case 0x31:
        // if equal
        if (cpu->regs[op_one] == cpu->regs[op_two])
            cpu->pc += offset;
        break;
    case 0x32:
        // if not equal
        if (cpu->regs[op_one] != cpu->regs[op_two])
            cpu->pc += offset;
        break;
    case 0x33:
        // if greater
        if ((int32_t)cpu->regs[op_one] > (int32_t)cpu->regs[op_two])
            cpu->pc += offset;
        break;
    case 0x34:
        // if greater (unsigned)
        if ((uint32_t)cpu->regs[op_one] > (uint32_t)cpu->regs[op_two])
            cpu->pc += offset;
        break;
    case 0x35:
        // load PC+o
        cpu->regs[op_one] = cpu->pc + offset;
        break;
    case 0x36:
        // direct jump
        cpu->pc = cpu->regs[op_one];
        break;
    default:
        cpu->status = STATUS_ERR_UNKNOWN_OP;
        break;
    }
}

void syscall(CPU *cpu, uint8_t opcode, uint32_t first_ins)
{
    switch (opcode)
    {
    case 0xF0:
        // syscall, give perms to os
        cpu->status = handle_syscall(&cpu->regs[0], cpu->regs[1], cpu->regs[2], cpu->regs[3]);
        break;
    case 0xFF:
        // halt
        cpu->status = STATUS_HALTED;
        break;
    default:
        cpu->status = STATUS_ERR_UNKNOWN_OP;
        break;
    }
}
