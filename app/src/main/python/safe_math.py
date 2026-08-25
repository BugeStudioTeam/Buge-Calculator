import ast
import json
import math

MAX_CODE_LENGTH = 6000
MAX_AST_NODES = 320
MAX_OUTPUT_LENGTH = 4000

SAFE_BUILTINS = {
    "abs": abs,
    "min": min,
    "max": max,
    "round": round,
    "sum": sum,
    "pow": pow,
}
SAFE_NAMES = {
    **SAFE_BUILTINS,
    "print": print,
    "pi": math.pi,
    "e": math.e,
    "sin": math.sin,
    "cos": math.cos,
    "tan": math.tan,
    "asin": math.asin,
    "acos": math.acos,
    "atan": math.atan,
    "sqrt": math.sqrt,
    "log": math.log,
    "log10": math.log10,
    "exp": math.exp,
    "floor": math.floor,
    "ceil": math.ceil,
}

ALLOWED_NODES = {
    ast.Module, ast.Expr, ast.Assign, ast.FunctionDef, ast.Return,
    ast.arguments, ast.arg, ast.BinOp, ast.UnaryOp, ast.Call, ast.Name,
    ast.Constant, ast.Load, ast.Store, ast.Add, ast.Sub, ast.Mult,
    ast.Div, ast.FloorDiv, ast.Mod, ast.Pow, ast.USub, ast.UAdd,
    ast.Tuple, ast.List,
}


def _validate(code):
    if not isinstance(code, str) or len(code) > MAX_CODE_LENGTH:
        raise ValueError("Code is empty or exceeds 6000 characters.")
    tree = ast.parse(code, mode="exec")
    nodes = list(ast.walk(tree))
    if len(nodes) > MAX_AST_NODES:
        raise ValueError("Code is too complex for the local math runner.")

    user_functions = {node.name for node in tree.body if isinstance(node, ast.FunctionDef)}
    assigned_names = set()
    for node in ast.walk(tree):
        if isinstance(node, ast.Assign):
            for target in node.targets:
                if not isinstance(target, ast.Name):
                    raise ValueError("Only simple variable assignment is allowed.")
                assigned_names.add(target.id)

    allowed_names = set(SAFE_NAMES) | user_functions | assigned_names
    for function in (node for node in tree.body if isinstance(node, ast.FunctionDef)):
        if function.decorator_list or function.returns is not None or getattr(function, "type_params", []):
            raise ValueError("Decorators and type annotations are not supported.")
        if len(function.args.args) > 6 or function.args.vararg or function.args.kwarg:
            raise ValueError("Functions can have up to six positional arguments.")
        allowed_names.update(argument.arg for argument in function.args.args)

    for node in nodes:
        if type(node) not in ALLOWED_NODES:
            raise ValueError(f"{type(node).__name__} is not allowed in safe math Python.")
        if isinstance(node, ast.Name) and (node.id not in allowed_names or node.id.startswith("__")):
            raise ValueError(f"Name '{node.id}' is not allowed.")
        if isinstance(node, ast.Call):
            if not isinstance(node.func, ast.Name) or node.func.id not in allowed_names:
                raise ValueError("Only approved mathematical function calls are allowed.")
        if isinstance(node, ast.Pow) and isinstance(getattr(node, "right", None), ast.Constant):
            exponent = node.right.value
            if isinstance(exponent, (int, float)) and abs(exponent) > 1000:
                raise ValueError("Exponent magnitude must not exceed 1000.")
    return tree


def execute(code):
    """Execute a small, safe mathematical Python program and return JSON for Kotlin."""
    try:
        tree = _validate(code)
        output = []

        def safe_print(*values):
            text = " ".join(str(value) for value in values)
            output.append(text[:MAX_OUTPUT_LENGTH])

        namespace = {"__builtins__": {**SAFE_BUILTINS, "print": safe_print}, **SAFE_NAMES}
        namespace["print"] = safe_print
        compiled = compile(tree, "<buge-python>", "exec")
        exec(compiled, namespace, namespace)
        if not output:
            visible = {key: value for key, value in namespace.items() if key not in SAFE_NAMES and key != "__builtins__" and not callable(value)}
            if visible:
                output.append("\n".join(f"{key} = {value}" for key, value in visible.items()))
        return json.dumps({"ok": True, "output": "\n".join(output)[:MAX_OUTPUT_LENGTH] or "Completed."})
    except Exception as error:
        return json.dumps({"ok": False, "error": str(error)[:MAX_OUTPUT_LENGTH]})
