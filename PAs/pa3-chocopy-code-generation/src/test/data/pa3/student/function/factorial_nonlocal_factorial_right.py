def factorial(n: int) -> int:
    def get_n() -> int:
        return n
    if get_n() <= 1:
        return 1
    else:
        return get_n() * factorial(n - 1)

print(factorial(10))