# a0 = argument
    addi sp, sp, -8
    sw ra, 4(sp)
    sw a0, 0(sp)

    la a0, $bool$prototype
    jal alloc
    lw t0, 0(sp) # load literal from stack
    sw t0, @.__int__(a0)

    lw ra, 4(sp)
    addi sp, sp, 8
    jr ra