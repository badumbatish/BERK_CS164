def print_int_list(x:[int]):
    y: int = 0
    for y in x:
        print(y)

    return

def list_concat():
    def list_concat(x:[int], y:[int]) -> [int]:
        return x + y

    z:[int] = None
    i:int = 0
    x: int = 25


    while i < x:
        z = list_concat([1,2,3], [4,5,6])
        print_int_list(z)
        i = i + 1

def str_concat():
    s : str = ""

    i : int = 0
    n : int = 20


    while i < n:
        s = s + "a"
        print(s)
        i = i + 1


# Deals with addition and mult
def arithmetic_1():
    def fib(x:int) -> int:
        if x <= 1:
            return 1
        else:
            return fib(x-1) + fib(x-2)

    i: int = 2
    while i < 10:

        print(fib(5) * 2)
        i = i + 1

def arithmetic_2():
    def gcd(a:int, b:int) -> int:
       if b == 0:
           return a
       return gcd(b, a % b)

    i: int = 2
    while i < 100:

        print(gcd(1071, i))
        i = i + 1

x_global :int = 232

def global_func() :
    print(x_global * x_global // x_global + 1)
    return


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

list_concat()
str_concat()
arithmetic_1()
arithmetic_2()
global_func()