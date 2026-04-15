#include <stdio.h>
#include <stdint.h>
// cl test\build_test.c /Fe:build_test.exe
int main()
{
    uint32_t program[] = {
        0xa00fffff, 0x00000003,
        0xf0ffffff,
        0x2003ffff,
        0xa02fffff, 0x00000000,
        0xa01fffff, 0x11000000,
        0x31120008,
        0x221fffff,
        0x30ffFFF4,
        0xa00fffff, 0x00000003, 
        0xf0ffffff,
        0x25030fff,
        0xffffffff};

    FILE *f = fopen("test.smc", "wb");
    if (!f)
    {
        printf("Failed to create file!\n");
        return 1;
    }

    for (int i = 0; i < sizeof(program) / sizeof(uint32_t); i++)
    {
        uint32_t val = program[i];
        fputc((val >> 24) & 0xFF, f);
        fputc((val >> 16) & 0xFF, f);
        fputc((val >> 8) & 0xFF, f);
        fputc((val) & 0xFF, f);
    }

    fclose(f);
    printf("Created test.smc successfully!\n");
    return 0;
}