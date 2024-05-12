x : [int] = None

n : int = 0
m : int = 0
x = [1, 2, 3]

# Modifying n should not modify the content of the list
for n in x:
    m = n
    n = n + 1
    print(m == n)

for n in x:
    print(n)

