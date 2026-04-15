#ifndef CPU_H
#define CPU_H
#include <string.h>
#include <stdint.h>
#include "common.h"

typedef struct {
    uint32_t regs[16];
    uint32_t pc;
    Status status;
} CPU;

// Initialize CPU
void init_cpu(CPU* cpu);

// Runs the CPU based on loaded instructions in memory starting at PC, only stopping once Status != OK
Status cpu_run(CPU *cpu, uint32_t init_pc);

// Fetches next instruction and increments PC
void cpu_fetch(CPU* cpu, uint32_t* first_ins, uint32_t* second_ins);

// Executes current 4-byte instruction
void cpu_execute(CPU *cpu, uint8_t opcode, uint32_t first_ins);

// Executes current 8-byte instruction
void cpu_execute_extended(CPU *cpu, uint8_t opcode, uint32_t first_ins, uint32_t opimm);

// Executes 4 byte load instructions (opcode 0x0-)
void load(CPU *cpu, uint8_t opcode, uint32_t first_ins);

// Executes 4 byte store instructions (opcode 0x1-)
void store(CPU *cpu, uint8_t opcode, uint32_t first_ins);

// Executes 4 byte arithmetic instructions (opcode 0x2-)
void math(CPU *cpu, uint8_t opcode, uint32_t first_ins);

// Executes 4 byte program flow instructions (opcode 0x3-)
void program_flow(CPU *cpu, uint8_t opcode, uint32_t first_ins);

// Executes 4 byte syscall instructions (opcode 0xF-)
void syscall(CPU *cpu, uint8_t opcode, uint32_t first_ins);

#endif