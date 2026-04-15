#ifndef OS_H
#define OS_H
#include "cpu.h"
#include "memory.h"
#include <stdbool.h>

// Initializes the VM's OS with a CPU and memory
void init_os(CPU* cpu, uint32_t mem_size);

// Loads program from a binary file on disk into memory, with the initial memory address of the program counter
uint32_t load_program(char *filename, Status *status);

// Runs program in memory starting at the program counter
Status run_program(CPU* cpu, uint32_t pc);

// Handles a syscall instruction from the CPU
Status handle_syscall(uint32_t* r0, uint32_t fd, uint32_t buffer, uint32_t size);

// Prints status message cleanly
void print_formatted(Status status);

// Allocates memory of size size, returning starting address. Start_address defines where we should start searching for a free block
uint32_t allocate_memory(uint32_t start_address, uint32_t size);

// Extends an existing allocated memory block by size, returning 1 on success and -1 on failure (if the block can't be extended)
Status extend_memory(uint32_t address, uint32_t size);

// Frees memory at address
void free_memory(uint32_t address);

// Coalesces two adjacent memory blocks given their headers
void coalesce(uint32_t first_header_address, uint32_t second_header_address);

// Checks if a memory block is free
bool is_memory_block_free(uint32_t header);
#endif