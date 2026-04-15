#include <string.h>
#include <stdint.h>
#include "../include/memory.h"
#include "../include/common.h"

// all read writes have bounds checking so we can exit in a friendlier way
// nothing should change status unless something is wrong, so that we can keep track of errors

uint8_t *memory;
uint32_t memory_size;

void init_memory(uint32_t size)
{
    if (memory != NULL)
    {
        free(memory);
    }
    memory_size = size;
    memory = (uint8_t*)calloc(memory_size, sizeof(uint8_t));
}

void write_byte(uint32_t address, uint8_t value, Status *status)
{
    if (address >= memory_size)
    {
        *status = STATUS_ERR_SEGFAULT;
        return;
    }
    memory[address] = value;
}

uint8_t read_byte(uint32_t address, Status *status)
{
    if (address >= memory_size)
    {
        *status = STATUS_ERR_SEGFAULT;
        return 0;
    }
    return memory[address];
}

void write_int(uint32_t address, uint32_t value, Status *status)
{
    if (address > (memory_size - 4))
    {
        *status = STATUS_ERR_SEGFAULT;
        return;
    }
    memory[address] = (value >> 24) & 0xFF;
    memory[address + 1] = (value >> 16) & 0xFF;
    memory[address + 2] = (value >> 8) & 0xFF;
    memory[address + 3] = value & 0xFF;
}

uint32_t read_int(uint32_t address, Status *status)
{
    if (address > (memory_size - 4))
    {
        *status = STATUS_ERR_SEGFAULT;
        return 0;
    }
    uint32_t value = (memory[address] << 24) |
                     (memory[address + 1] << 16) |
                     (memory[address + 2] << 8) |
                     (memory[address + 3]);
    return value;
}