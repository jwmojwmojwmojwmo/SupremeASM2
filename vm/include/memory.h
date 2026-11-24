#ifndef MEMORY_H
#define MEMORY_H

#include <stdint.h>
#include <string.h>
#include <stdlib.h>
#include "common.h"

// 0 = allow self-modifying code (writes into the program image). 1 = well no u can't do dat
#define WRITE_PROTECT_CODE 1

extern uint8_t *memory;
extern uint32_t memory_size;

extern uint32_t prog_base;
extern uint32_t code_end;
extern uint32_t heap_limit;
extern uint32_t stack_base;

extern uint32_t fault_addr;
extern uint32_t fault_len;
extern int      fault_kind;

void init_memory(uint32_t size);
void set_mem_regions(uint32_t code_base, uint32_t image_end,
                 uint32_t heap_lim, uint32_t stack_lo);


static inline int mem_check(uint32_t address, uint32_t len, int is_write, Status *status)
{
    uint64_t end = (uint64_t)address + len;
    int ok = 0;

    if (address >= prog_base && end <= code_end)
        ok = !(is_write && WRITE_PROTECT_CODE);
    else if (address >= code_end && end <= heap_limit)
        ok = 1;
    else if (address >= stack_base && end <= memory_size)
        ok = 1;

    if (!ok)
    {
        fault_addr = address;
        fault_len  = len;
        fault_kind = is_write;
        *status = STATUS_ERR_SEGFAULT;
    }
    return ok;
}

static inline void write_byte(uint32_t address, uint8_t value, Status *status)
{
    if (!mem_check(address, 1, 1, status)) return;
    memory[address] = value;
}

static inline uint8_t read_byte(uint32_t address, Status *status)
{
    if (!mem_check(address, 1, 0, status)) return 0;
    return memory[address];
}

static inline void write_int(uint32_t address, uint32_t value, Status *status)
{
    if (!mem_check(address, 4, 1, status)) return;
    memory[address]     = (value >> 24) & 0xFF;
    memory[address + 1] = (value >> 16) & 0xFF;
    memory[address + 2] = (value >> 8) & 0xFF;
    memory[address + 3] = value & 0xFF;
}

static inline uint32_t read_int(uint32_t address, Status *status)
{
    if (!mem_check(address, 4, 0, status)) return 0;
    return ((uint32_t)memory[address] << 24) |
           ((uint32_t)memory[address + 1] << 16) |
           ((uint32_t)memory[address + 2] << 8) |
           (uint32_t)memory[address + 3];
}

#endif