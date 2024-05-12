def factorial(n: int) -> int:
    a: int = 1230981
    def f(x: int) -> int:
        if x == a:
            return a
        return f(x - 1) * x
    a = 1
    return f(n)

class Foo(object):
    x: int = 0
    y: int = 0
    z: int = 0
    def __init__(self: "Foo"):
        i: str = ""
        j: str = ""
        def factorial_plus_z(n: int) -> int:
            return factorial(n) + self.z
        self.x = 5
        self.z = 3
        for i in "1234":
            for j in "47":
                print(j)
                print(factorial(5))
            print(i)
        self.y = factorial_plus_z(self.x)
    def fib(self: "Foo"): # calc fib on x, put it in y
        res1: int = 0
        res2: int = 0
        orig: int = 0
        if self.x <= 1:
            self.y = 1
        else:
            orig = self.x

            self.x = orig - 1
            self.fib()
            res1 = self.y

            self.x = orig - 2
            self.fib()
            res2 = self.y

            self.y = res1 + res2

foo: Foo = None
foo = Foo()
print(foo.x)
print(foo.y)
print(foo.z)

foo.x = 10
foo.fib()
print(foo.y)