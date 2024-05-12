def is_even(x:int) -> bool:
    if x % 2 == 0:
        return True
    return False

n:int = 3

# Run [1, n]
i:int = 1

# Crunch
while i <= n:
    if is_even(i):
        print(i)
    i = i + 1