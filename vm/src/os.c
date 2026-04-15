#include "../include/os.h"
#include <stdio.h>
#include <time.h>
#define ONE_MB 1048576
#define STACK_MAX_SIZE 65536

static uint32_t main_mem_size;
static uint32_t heap_start;
static uint32_t heap_end;
static uint32_t heap_os_boundary = ONE_MB * 4;
static uint32_t os_stack_boundary;
static clock_t start_time;

void init_os(CPU *cpu, uint32_t mem_size)
{
    if (mem_size < (ONE_MB * 5))
    {
        printf("Warning! OS's memory size must be at least 5 MB\n");
        exit(1);
    }
    main_mem_size = mem_size;
    init_cpu(cpu);
    init_memory(main_mem_size);
    Status temp;
    write_int(0, heap_os_boundary - 12, &temp);
    write_int(4, 0, &temp);
    write_int(heap_os_boundary - 4, heap_os_boundary - 12, &temp);
    os_stack_boundary = main_mem_size - STACK_MAX_SIZE;
    write_int(heap_os_boundary, os_stack_boundary - heap_os_boundary - 12, &temp);
    write_int(heap_os_boundary + 4, 0, &temp);
    write_int(os_stack_boundary - 4, os_stack_boundary - heap_os_boundary - 12, &temp);
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
    uint32_t address = allocate_memory(0, (uint32_t)size);
    int byte;

    // fgetc reads one byte at a time as an int so it can detect EOF
    while ((byte = fgetc(file)) != EOF)
    {
        write_byte(address, (uint8_t)byte, status);

        if (*status != STATUS_OK)
        {
            free_memory(address);
            fclose(file);
            print_formatted(*status);
            return -1;
        }
        address++;
    }

    fclose(file);
    printf("Program loaded successfully starting at 0x%08X (%d)\n", address, address);
    return STATUS_OK;
}

Status run_program(CPU *cpu, uint32_t pc)
{
    heap_start = allocate_memory(0, ONE_MB); // heap gets 1 MB initially
    heap_end = heap_start + ONE_MB;
    cpu->regs[14] = main_mem_size - 1;
    cpu->regs[0] = heap_start;
    start_time = clock();
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
    case 0:          // read
        if (fd == 0) // stdin
        {
            *r0 = 0; // *r0 returns # of bytes read
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
        }
    case 1:          // write
        if (fd == 1) // stdout
        {
            *r0 = 0; // *r0 returns # of bytes written
            uint8_t c;
            for (uint32_t i = 0; i < size; i++)
            {
                c = read_byte(buffer + i, &status);
                if (status != STATUS_OK)
                {
                    *r0 = -1;
                    return status;
                }
                putchar(c);
                (*r0)++;
            }
            fflush(stdout);
            return STATUS_OK;
        }
    case 2: // get_time
        clock_t current = clock();
        *r0 = (uint32_t)((current - start_time) * 1000 / CLOCKS_PER_SEC);
        return STATUS_OK;
    case 3: // sbrk(size) => extend heap by this much
        status = extend_memory(heap_start, size);
        if (status != STATUS_OK)
        {
            *r0 = -1;
            return status;
        }
        *r0 = heap_end;
        heap_end += size;
        return status;
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

/*
To ensure 4 byte alignment, headers are 8 bytes and footers are 4 bytes.
Headers: contain size of block in first 4 bytes and if block is free in second 4 bytes (1 for used, 0 for free)
Footers: contain size of block in 4 bytes
*/

uint32_t allocate_memory(uint32_t start_address, uint32_t size)
{
    Status temp;
    uint32_t i = 0;
    while (i < os_stack_boundary)
    {
        uint32_t block_size = read_int(i, &temp);
        if (is_memory_block_free(i) && block_size >= size && i >= start_address)
        {
            // split block if large enough
            if (block_size > size + 12)
            {
                write_int(i, size, &temp);
                write_int(i + 4, 1, &temp);
                write_int(i + 8 + size, size, &temp);
                write_int(i + 8 + size + 4, block_size - size - 12, &temp);
                write_int(i + 8 + size + 4 + 4, 0, &temp);
                write_int(i + 8 + block_size, block_size - size - 12, &temp);
            }
            else
            {
                write_int(i + 4, 1, &temp);
            }
            return i + 8;
        }
        i += block_size + 12;
    }
    return -1; // no block large enough
}

Status extend_memory(uint32_t address, uint32_t size)
{
    Status temp;
    uint32_t next_block_header_address = read_int(address - 8, &temp) + address + 4;
    if (next_block_header_address >= heap_os_boundary)
    {
        return STATUS_ERR_SEGFAULT;
    }
    uint32_t next_block_size = read_int(next_block_header_address, &temp);
    if (is_memory_block_free(next_block_header_address) && next_block_size >= size)
    {
        if (next_block_size > size + 12)
        {
            write_int(next_block_header_address, size, &temp);
            write_int(next_block_header_address + 4, 1, &temp);
            write_int(next_block_header_address + 8 + size, size, &temp);
            write_int(next_block_header_address + 8 + size + 4, next_block_size - size - 12, &temp);
            write_int(next_block_header_address + 8 + size + 4 + 4, 0, &temp);
            write_int(next_block_header_address + 8 + next_block_size, next_block_size - size - 12, &temp);
        }
        coalesce(address - 8, next_block_header_address);
        return STATUS_OK;
    }
    return STATUS_ERR_SEGFAULT;
}

void free_memory(uint32_t address)
{
    Status temp;
    bool coalesced_before = false;
    write_int(address - 4, 0, &temp);

    // BACKWARD COALESCE
    uint32_t prev_block_header_address;
    if (address > 8)
    {
        // Calculate it safely inside the check
        prev_block_header_address = address - 12 - read_int(address - 12, &temp) - 8;

        if (read_int(prev_block_header_address + 4, &temp) == 0)
        {
            bool both_in_user = (address - 8 < heap_os_boundary && prev_block_header_address < heap_os_boundary);
            bool both_in_os = (address - 8 >= heap_os_boundary && prev_block_header_address >= heap_os_boundary);

            if (both_in_user || both_in_os)
            {
                coalesce(prev_block_header_address, address - 8);
                coalesced_before = true;
            }
        }
    }
    // FORWARD COALESCE
    uint32_t next_block_header_address = read_int(address - 8, &temp) + address + 4;
    if (next_block_header_address < os_stack_boundary && read_int(next_block_header_address + 4, &temp) == 0)
    {
        bool both_in_user = (address - 8 < heap_os_boundary && next_block_header_address < heap_os_boundary);
        bool both_in_os = (address - 8 >= heap_os_boundary && next_block_header_address >= heap_os_boundary);

        if (both_in_user || both_in_os)
        {
            if (coalesced_before)
            {
                coalesce(prev_block_header_address, next_block_header_address);
            }
            else
            {
                coalesce(address - 8, next_block_header_address);
            }
        }
    }
}

void coalesce(uint32_t first_header_address, uint32_t second_header_address)
{
    Status temp;
    uint32_t new_size = read_int(first_header_address, &temp) + read_int(second_header_address, &temp) + 12;
    write_int(first_header_address, new_size, &temp);
    write_int(first_header_address + 8 + new_size, new_size, &temp);
}

bool is_memory_block_free(uint32_t header)
{
    Status temp;
    return read_int(header + 4, &temp) == 0;
}
