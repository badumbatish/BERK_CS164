# a0 = str1, a1 = str2

    lw t0, @.__len__(a0)
    lw t1, @.__len__(a1)

    bne t0, t1, streq_ne

streq_loop:
    beqz t0, streq_eq

    lbu t1, @.__str__(a0)
    lbu t2, @.__str__(a1)

    bne t1, t2, streq_ne

    addi a0, a0, 1
    addi a1, a1, 1
    addi t0, t0, -1

    j streq_loop
streq_eq:
    li a0, 1
    jr ra

streq_ne:
    li a0, 0
    jr ra