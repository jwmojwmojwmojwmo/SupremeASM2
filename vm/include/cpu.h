#ifndef CPU_H
#define CPU_H

#include <string.h>
#include <stdint.h>
#include "common.h"

typedef struct {
    // stored as raw bits; read as signed by div/mod/bgs/shf-right, unsigned by bgu
    uint32_t regs[16];
    uint32_t pc;
    Status status;
} CPU;

void init_cpu(CPU* cpu);
Status cpu_run(CPU* cpu, uint32_t init_pc);

#endif