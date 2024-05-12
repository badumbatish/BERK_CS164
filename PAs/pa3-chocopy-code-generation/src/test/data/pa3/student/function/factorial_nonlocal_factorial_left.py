def factorial(n: int) -> int:
    def get_n() -> int:
        return n
    if get_n() <= 1:
        return 1
    else:
        return factorial(n - 1) * get_n()

print(factorial(10))