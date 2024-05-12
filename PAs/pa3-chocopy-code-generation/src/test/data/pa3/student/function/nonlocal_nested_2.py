def f1() -> int:
    a: int = 0
    def f2(i: int) -> int:
        nonlocal a

        def f3() -> int:
            nonlocal a
            a = a + 1
            return a

        a = a + 2

        return f3() + f3()

    return f2(0) + 22



print(f1())