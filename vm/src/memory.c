#include <string.h>
#include <stdint.h>
#include "../include/memory.h"
#include "../include/common.h"

// the single real definition of the globals (declared extern in the header)
uint8_t *memory = NULL;
uint32_t memory_size = 0;

// the three mapped regions; anything outside them is unmapped and faults
//   code  : [prog_base, code_end)      read + execute, no writes
//   heap  : [code_end, heap_limit)     read + write
//   stack : [stack_base, memory_size)  read + write
uint32_t prog_base = 0;
uint32_t code_end = 0;
uint32_t heap_limit = 0;
uint32_t stack_base = 0;

uint32_t fault_addr = 0;
uint32_t fault_len = 0;
int fault_kind = 0; // 0 read, 1 write, 2 execute, 3 address overflow

void init_memory(uint32_t size)
{
    if (memory != NULL)
        free(memory);
    memory_size = size;
    memory = (uint8_t *)calloc(memory_size, sizeof(uint8_t));

    // nothing mapped until the loader says what goes where. stack_base == size
    // makes the stack region empty, so every access faults in the window
    // between here and set_regions().
    prog_base = 0;
    code_end = 0;
    heap_limit = 0;
    stack_base = size;
}

void set_mem_regions(uint32_t code_base, uint32_t code_stop, uint32_t heap_lim, uint32_t stack_start)
{
    prog_base = code_base;
    code_end = code_stop;
    heap_limit = heap_lim;
    stack_base = stack_start;
}