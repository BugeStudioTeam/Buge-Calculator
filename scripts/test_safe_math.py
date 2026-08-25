import json
import sys

sys.path.insert(0, "/home/ubuntu/BugeCalculator/app/src/main/python")
import safe_math

success = json.loads(safe_math.execute("def f(x):\n    return sin(x) + x**2\n\nprint(f(2))"))
assert success["ok"], success
assert "4." in success["output"], success

blocked_import = json.loads(safe_math.execute("import os\nprint(1)"))
assert not blocked_import["ok"], blocked_import

blocked_attribute = json.loads(safe_math.execute("print((1).__class__)"))
assert not blocked_attribute["ok"], blocked_attribute

print("SAFE_MATH_TEST_OK")
