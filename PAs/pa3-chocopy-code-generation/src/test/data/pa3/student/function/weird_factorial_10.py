def factorial_y(y: int) -> int:
    if y < 1:
        return 1
    else:
        return y * factorial_y(y - 1)

def factorial_x(x: int) -> int:
    if x < 1:
        return 1
    else:
        return x * factorial_x(x - 1)

def factorial(z: int) -> int:
    if z < 1:
        return 1
    else:
        return z * factorial(z - 1)
x:int = 0
z:int = 0
while x < 10:
    # print(factorial(x))
    # print(factorial(x-1))
    print(factorial_y(x) // factorial_y(x-1))
    x = x + 1


x = 0
while x < 10:
    # print(factorial(x))
    # print(factorial(x-1))
    print(factorial_x(x) // factorial_x(x-1))
    x = x + 1

while z < 10:

    print(factorial(z) // factorial(z-1))
    z = z + 1

