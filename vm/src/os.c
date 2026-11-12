#include "../include/os.h"
#include <stdio.h>
#include <time.h>

#define ONE_MB 1048576
#define PAGE_SIZE 4096

/*
    Layout:
      0x000000 .. PROGRAM_BASE   unused / guard
      PROGRAM_BASE .. break      program code (+ padding to a page)
      break .. USER_LIMIT        heap (grows up via sbrk)
      USER_LIMIT .. stack_base   guard
      stack_base .. stack_top    stack (SP grows down from stack_top)
*/

#define PROGRAM_BASE 0x1000              /* page-aligned load address */
#define USER_LIMIT   (ONE_MB * 4)        /* heap break may not pass this */
#define STACK_SIZE   65536               /* top 64 KB reserved for the stack */

static uint32_t main_mem_size;
static uint32_t heap_break;              /* the program break: current end of heap */
static uint32_t stack_top;
static double start_time_ms;

static double now_ms(void)
{
    struct timespec ts;
    timespec_get(&ts, TIME_UTC);
    return (double)ts.tv_sec * 1000.0 + (double)ts.tv_nsec / 1000000.0;
}

void init_os(CPU *cpu, uint32_t mem_size)
{
    if (mem_size < (ONE_MB * 5))
    {
        printf("Warning! OS's memory size must be at least 5 MB\n");
        exit(1);
    }
    main_mem_size = mem_size;
    stack_top = mem_size - 1;
    init_cpu(cpu);
    init_memory(main_mem_size);
}

uint32_t load_program(char *filename, Status *status)
{
    printf("Loading program from %s...\n", filename);
    FILE *file = fopen(filename, "rb");
    if (!file)
    {
        print_formatted(STATUS_ERR_FILE_NOT_FOUND);
        *status = STATUS_ERR_FILE_NOT_FOUND;
        return -1;
    }
    *status = STATUS_OK;
    fseek(file, 0, SEEK_END);
    long size = ftell(file);
    rewind(file);

    if (size < 0 || (uint32_t)size > USER_LIMIT - PROGRAM_BASE)
    {
        printf("Program too large for user memory!\n");
        fclose(file);
        *status = STATUS_ERR_SEGFAULT;
        return -1;
    }

    uint32_t address = PROGRAM_BASE;
    int byte;
    while ((byte = fgetc(file)) != EOF)
    {
        write_byte(address, (uint8_t)byte, status);
        if (*status != STATUS_OK)
        {
            fclose(file);
            print_formatted(*status);
            return -1;
        }
        address++;
    }
    fclose(file);

    /* heap begins right after the program, rounded up to a page */
    heap_break = (PROGRAM_BASE + (uint32_t)size + PAGE_SIZE - 1) & ~(uint32_t)(PAGE_SIZE - 1);

    printf("Program loaded successfully starting at 0x%08X (%d)\n", (uint32_t)PROGRAM_BASE, (uint32_t)PROGRAM_BASE);
    *status = STATUS_OK;
    return PROGRAM_BASE;
}

Status run_program(CPU *cpu, uint32_t pc)
{
    cpu->regs[14] = stack_top;      /* SP starts at top of stack, grows down */
    cpu->regs[0] = heap_break;      /* r0 = start of heap so programs know where it is */
    start_time_ms = now_ms();
    printf("\n----Running program from 0x%08X (%d)----\n", pc, pc);
    Status status = cpu_run(cpu, pc);
    printf("\n----Program exited at PC: 0x%08X (%d)----\n", cpu->pc, cpu->pc);
    print_formatted(status);
    return status;
}

Status handle_syscall(uint32_t *r0, uint32_t fd, uint32_t buffer, uint32_t size)
{
    Status status = STATUS_OK;
    switch (*r0)
    {
    case 0: // read
        if (fd != 0)
        {
            *r0 = -1;
            return STATUS_OK;
        }
        *r0 = 0;
        int c;
        for (uint32_t i = 0; i < size; i++)
        {
            c = getchar();
            if (c == EOF)
                break;
            write_byte(buffer + i, (uint8_t)c, &status);
            if (status != STATUS_OK)
            {
                *r0 = -1;
                return status;
            }
            (*r0)++;
            if (c == '\n')
                break;
        }
        return STATUS_OK;
    case 1: // write
        if (fd != 1)
        {
            *r0 = -1;
            return STATUS_OK;
        }
        *r0 = 0;
        uint8_t ch;
        for (uint32_t i = 0; i < size; i++)
        {
            ch = read_byte(buffer + i, &status);
            if (status != STATUS_OK)
            {
                *r0 = -1;
                return status;
            }
            putchar(ch);
            (*r0)++;
        }
        fflush(stdout);
        return STATUS_OK;
    case 2: // get_time
        *r0 = (uint32_t)(now_ms() - start_time_ms);
        return STATUS_OK;
    case 3: // sbrk
    {
        uint32_t old = heap_break;
        uint32_t next = old + size;             
        if (next < old || next > USER_LIMIT)
        {
            *r0 = -1;         
            return STATUS_OK;
        }
        heap_break = next;
        *r0 = old;                              // sbrk returns the OLD break
        return STATUS_OK;
    }
    default:
        *r0 = -1;
        return STATUS_OK;
    }
}

void print_formatted(Status status)
{
    switch (status)
    {
    case STATUS_OK:
        printf("Exited with status: OK\n");
        break;
    case STATUS_HALTED:
        printf("Exited with status: OK, Halted\n");
        break;
    case STATUS_ERR_SEGFAULT:
        printf("Exited with status: Segmentation Fault\n");
        break;
    case STATUS_ERR_UNKNOWN_OP:
        printf("Exited with status: Unknown Operation\n");
        break;
    case STATUS_ERR_DIV_ZERO:
        printf("Exited with status: Division by Zero\n");
        break;
    case STATUS_ERR_FILE_NOT_FOUND:
        printf("Exited with status: File Not Found\n");
        break;
    default:
        printf("Exited with status: Unknown Error\n");
        break;
    }
}