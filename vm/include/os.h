#ifndef OS_H
#define OS_H
#include "cpu.h"
#include "memory.h"
#include <stdbool.h>

// Initializes the VM's OS with a CPU and memory
void init_os(CPU* cpu, uint32_t mem_size);

// Loads a program into memory at the fixed load base; returns the initial PC
uint32_t load_program(char* filename, Status* status);

// Runs the program in memory starting at the program counter
Status run_program(CPU* cpu, uint32_t pc);

// Handles a syscall instruction from the CPU
Status handle_syscall(uint32_t* r0, uint32_t fd, uint32_t buffer, uint32_t size);

// Prints a status message cleanly
void print_formatted(Status status);

#endif