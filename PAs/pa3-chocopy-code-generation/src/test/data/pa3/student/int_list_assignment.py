x : [int] = None
y : [int] = None
n : int = 0
x = y = [1, 2, 3]

print(x is y)
x = x + [4]


for n in x:
    print(n)


for n in y:
    print(n)

