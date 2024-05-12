def exp() -> int:
    a: int = 10
    def f(i: int) -> int:
        if i == 0:
            return a
        else:
            return f(0)
    return f(1)

print(exp())