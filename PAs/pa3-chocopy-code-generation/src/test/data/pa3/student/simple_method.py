class A(object):
    def foo(self: "A"):
        print("hello world")

a: A = None

a = A()
a.foo()