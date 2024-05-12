def is_prime(x:int) -> bool:
    div:int = 2
    while div < x:
        if x % div == 0:
            return False
        div = div + 1
    return True

n:int = 30

# Run [1, n]
i:int = 1

# Crunch
while i <= n:
    if is_prime(i):
        print(i)
    i = i + 1