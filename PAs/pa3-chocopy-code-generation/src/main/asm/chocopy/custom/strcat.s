# strcat. a0 = address of string 1, a1 = address of string 2
    addi sp, sp, -28 # push 4 words
    sw ra, 24(sp)
    sw s5, 20(sp)
    sw s4, 16(sp)
    sw s3, 12(sp)
    sw s2, 8(sp)
    sw s1, 4(sp)
    sw s0, 0(sp)


    mv s0, a0       # s0 = address of first string
    mv s1, a1       # s1 = address of second string

    lw s3, @.__len__(s0)    # s3 = len(first string)
    lw s4, @.__len__(s1)    # s4 = len(second string)
    add s5, s3, s4          # s5 = len(first) + len(second)


    la a0, $str$prototype   # a0 = string prototype address

    addi a1, s5, 4          # a1 = s5 + 1
    li t0, 4                # t0 = 4
    div a1, a1, t0          # a1 = (s5 + 1) / 4
    addi a1, a1, 4          # a1 = 4 + (s5 + 1) / 4

    jal alloc2              # a0 = new string allocation
    mv s2, a0               # s2 = new string

    sw s5, @.__len__(s2)    # store s5 new string length into allocation

    addi t0, s0, @.__str__  # t0 = start of first data
    addi t1, s1, @.__str__  # t1 = start of second data
    addi t2, s2, @.__str__  # t2 = start of result data

l1s:
    beqz s3, l1e        # nothing left in first => end
    lbu t3, 0(t0)       # load from first pointer into t3
    sb t3, 0(t2)        # store t3 into result pointer

    addi s3, s3, -1     # subtract 1 from number left in first
    addi t0, t0, 1      # increment first pointer by 1
    addi t2, t2, 1      # increment result pointer by 1
    j l1s               # repeat
l1e:

l2s:
    beqz s4, l2e        # nothing left in second => end
    lbu t3, 0(t1)       # load from second pointer into t3
    sb t3, 0(t2)        # store t3 into result pointer

    addi s4, s4, -1     # subtract 1 from number left in second
    addi t1, t1, 1      # increment second pointer by 1
    addi t2, t2, 1      # increment result pointer by 1
    j l2s               # repeat
l2e:

    sb x0, 0(t2)        # store null pointer at the end

    mv a0, s2           # set return value in a0 to result pointer

    # epilogue
    lw ra, 24(sp)
    lw s5, 20(sp)
    lw s4, 16(sp)
    lw s3, 12(sp)
    lw s2, 8(sp)
    lw s1, 4(sp)
    lw s0, 0(sp)
    addi sp, sp, 28
    jr ra