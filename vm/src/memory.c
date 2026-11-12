#include <string.h>
#include <stdint.h>
#include "../include/memory.h"
#include "../include/common.h"

// the single real definition of the globals (declared extern in the header)
uint8_t *memory = NULL;
uint32_t memory_size = 0;

void init_memory(uint32_t size)
{
    if (memory != NULL)
    {
        free(memory);
    }
    memory_size = size;
    memory = (uint8_t *)calloc(memory_size, sizeof(uint8_t));
}