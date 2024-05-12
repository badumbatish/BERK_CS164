a: str = "a"
b: str = "b"
c: str = "c"

def cat(x: str, y: str) -> str:
    print("CAT")
    print(x)
    print(y)
    print("RESULT:")
    print(x + y)
    print("ENDCAT")
    return x + y

print(cat(a, b))
print(cat(b, cat(b, c)))