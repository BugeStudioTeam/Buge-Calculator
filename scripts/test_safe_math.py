import json
import math
import pathlib
import sys

PROJECT_ROOT = pathlib.Path(__file__).resolve().parents[1]
sys.path.insert(0, str(PROJECT_ROOT / "app/src/main/python"))
import safe_math

source = """import math
import statistics

class Calculator:
    def __init__(self, values):
        self.values = values

    def mean(self):
        return statistics.mean(self.values)

values = [3, 4]
calculator = Calculator(values)
for value in values:
    print(value)
print(math.sqrt(81))
print(calculator.mean())
"""
success = json.loads(safe_math.execute(source))
assert success["ok"], success
assert "9.0" in success["output"], success
assert "3.5" in success["output"], success

syntax_error = json.loads(safe_math.execute("def broken(:\n    pass"))
assert not syntax_error["ok"], syntax_error
assert "SyntaxError" in syntax_error["error"], syntax_error

print("FULL_PYTHON_3_12_TEST_OK")
print(math.pi)
