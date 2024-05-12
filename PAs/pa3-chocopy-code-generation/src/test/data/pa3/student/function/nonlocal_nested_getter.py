def exp() -> int:
    a: int = 1
    def f() -> int:
        def geta() -> int:
            return a
        return geta() + 1
    a = 3
    return f()

print(exp())