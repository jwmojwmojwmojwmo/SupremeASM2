#ifndef MEMORY_H
#define MEMORY_H

// use uint for universal int (idk how much that matters lowk)
#include <stdint.h>
#include <string.h>
#include <stdlib.h>
#include "common.h"

// Initialize 1mb of main memory, setting every value inside to 0
void init_memory(uint32_t size);

// Write a single byte into memory 
void write_byte(uint32_t address, uint8_t value, Status* status);

// Read a single byte from memory
uint8_t read_byte(uint32_t address, Status* status);

// Write an int (4 byte value) from memory, starting at address
void write_int(uint32_t address, uint32_t value, Status* status);

// Read an int (4 byte value) from memory, starting at address
uint32_t read_int(uint32_t address, Status* status);

#endif
