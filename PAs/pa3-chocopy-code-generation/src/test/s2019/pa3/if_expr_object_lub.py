def test_if_expr_object_lub():
    e1:str = "a"
    e2:int = 3
    print(e1 if False else e2)
    print(e1 if True else e2)

test_if_expr_object_lub()

