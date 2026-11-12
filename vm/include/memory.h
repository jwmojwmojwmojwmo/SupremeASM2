#ifndef MEMORY_H
#define MEMORY_H

#include <stdint.h>
#include <string.h>
#include <stdlib.h>
#include "common.h"

// the ONE real copy of these lives in memory.c
extern uint8_t *memory;
extern uint32_t memory_size;

// called once at startup — stays a normal function
void init_memory(uint32_t size);

// inline
static inline void write_byte(uint32_t address, uint8_t value, Status *status)
{
    if (address >= memory_size)
    {
        *status = STATUS_ERR_SEGFAULT;
        return;
    }
    memory[address] = value;
}

static inline uint8_t read_byte(uint32_t address, Status *status)
{
    if (address >= memory_size)
    {
        *status = STATUS_ERR_SEGFAULT;
        return 0;
    }
    return memory[address];
}

static inline void write_int(uint32_t address, uint32_t value, Status *status)
{
    if (address > (memory_size - 4))
    {
        *status = STATUS_ERR_SEGFAULT;
        return;
    }
    memory[address]     = (value >> 24) & 0xFF;
    memory[address + 1] = (value >> 16) & 0xFF;
    memory[address + 2] = (value >> 8) & 0xFF;
    memory[address + 3] = value & 0xFF;
}

static inline uint32_t read_int(uint32_t address, Status *status)
{
    if (address > (memory_size - 4))
    {
        *status = STATUS_ERR_SEGFAULT;
        return 0;
    }
    return ((uint32_t)memory[address] << 24) |
           ((uint32_t)memory[address + 1] << 16) |
           ((uint32_t)memory[address + 2] << 8) |
           (uint32_t)memory[address + 3];
}

#endif