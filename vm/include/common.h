#ifndef COMMON_H
#define COMMON_H

typedef enum {
    STATUS_OK = 0,
    STATUS_HALTED = 1,
    STATUS_ERR_SEGFAULT = 2,
    STATUS_ERR_UNKNOWN_OP = 3,
    STATUS_ERR_DIV_ZERO = 4,
    STATUS_ERR_FILE_NOT_FOUND = 5
} Status;

#endif