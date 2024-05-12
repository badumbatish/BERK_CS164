class Printer(object):
    def __init__(self: Printer):
        print("side effect")

i: Printer = None
k: int = 42

i = Printer()
k.__init__()
