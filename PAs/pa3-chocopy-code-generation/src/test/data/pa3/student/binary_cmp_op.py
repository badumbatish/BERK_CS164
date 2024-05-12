def test(a: int, b: int):
    print(a == b)
    print(a != b)
    print(a < b)
    print(a <= b)
    print(a > b)
    print(a >= b)

test(3, 5)
test(4, 4)
test(5, 3)


test(100, 200)
test(150, 150)
test(200, 100)