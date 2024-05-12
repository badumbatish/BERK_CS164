next:int = 0

def next_int() -> int:
    global next
    next = next + 1
    return next

def func() -> int:
    next_int()
    next_int()

    return 0

func()
print(next_int())
print(next_int())
print(next_int())
print(next_int()-3)