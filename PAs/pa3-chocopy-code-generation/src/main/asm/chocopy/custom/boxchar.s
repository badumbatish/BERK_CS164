# boxchar
    addi sp, sp, -8 # push 2 words
    sw ra, 4(sp)    # store ra
    sw a0, 0(sp)    # store a0: the char to box

    la a0, $str$prototype   # load str prototype
    jal alloc               # allocate

    lw t0, 0(sp)            # load a0: char to box
    sw t0, @.__str__(a0)    # store a0 into str
    li t0, 1
    sw t0, @.__len__(a0)    # store len into str

    lw ra, 4(sp)            # load ra
    addi sp, sp, 8          # pop 2 words
    jr ra                   # return