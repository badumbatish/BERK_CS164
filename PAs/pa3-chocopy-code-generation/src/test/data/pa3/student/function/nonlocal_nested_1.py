def f1() -> int:
    a: int = 0
    def f2(i: int) -> int:
        nonlocal a

        def f3() -> int:
            return a

        a = a + 1

        return f3()

    return f2(0)



print(f1())