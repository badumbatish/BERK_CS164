def outer() -> int:
    x: int = 5
    def inner(y: int) -> int:
        nonlocal x
        x = x + 1
        return x * y
    return inner(2)

print(outer())