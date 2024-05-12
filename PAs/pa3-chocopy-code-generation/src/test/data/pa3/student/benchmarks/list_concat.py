def concat(x:[int], y:[int]) -> [int]:
    return x + y

z:[int] = None
i:int = 0
x: int = 25


while i < x:
    z = concat([1,2,3], [4,5,6])
    print(z)
    i = i + 1

