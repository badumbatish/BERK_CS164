def exp() -> int:
    a: int = 1
    def f() -> int:
        def geta() -> int:
            return a
        return a
    a = 3
    return a

print(exp())