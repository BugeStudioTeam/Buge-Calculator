import contextlib
import io
import json
import traceback

# Chaquopy embeds a real CPython 3.12 runtime in the APK. These limits only
# protect the Compose result panel from accidental log floods; they do not
# restrict Python syntax, imports, statements, functions, classes, or modules.
MAX_CODE_LENGTH = 20000
MAX_OUTPUT_LENGTH = 12000


def execute(code):
    """Run a complete Python 3 program in the embedded Chaquopy interpreter.

    The Kotlin bridge expects a JSON string. stdout and stderr are captured so
    print(), warnings, and tracebacks can be shown in the app's output panel.
    Normal CPython builtins and import machinery are preserved.
    """
    if not isinstance(code, str):
        return json.dumps({"ok": False, "error": "Python source must be text."})
    if not code.strip():
        return json.dumps({"ok": False, "error": "Python source is empty."})
    if len(code) > MAX_CODE_LENGTH:
        return json.dumps({"ok": False, "error": f"Python source exceeds {MAX_CODE_LENGTH} characters."})

    stdout = io.StringIO()
    stderr = io.StringIO()
    namespace = {
        "__name__": "__main__",
        "__file__": "<buge-python>",
        "__package__": None,
    }

    try:
        compiled = compile(code, "<buge-python>", "exec")
        with contextlib.redirect_stdout(stdout), contextlib.redirect_stderr(stderr):
            exec(compiled, namespace, namespace)

        output = stdout.getvalue()
        error_output = stderr.getvalue()
        combined = output + ("\n" if output and error_output else "") + error_output
        if not combined.strip():
            # A normal Python script has no implicit REPL echo. Show simple
            # top-level values when the user did not call print().
            hidden = {"__name__", "__file__", "__package__", "__builtins__"}
            visible = [
                f"{key} = {value!r}"
                for key, value in namespace.items()
                if key not in hidden and not key.startswith("_")
            ]
            combined = "\n".join(visible)
        return json.dumps({"ok": True, "output": combined[:MAX_OUTPUT_LENGTH] or "Completed."})
    except BaseException:
        # Preserve the standard Python traceback for syntax, import, runtime,
        # and user-raised exceptions.
        traceback.print_exc(file=stderr)
        return json.dumps({"ok": False, "error": stderr.getvalue()[-MAX_OUTPUT_LENGTH:]})


DEFAULT_EXAMPLE = (
    "import math\n"
    "import statistics\n\n"
    "values = [1, 2, 3, 4, 5]\n"
    "mean = statistics.mean(values)\n"
    "print(f'√81 = {math.sqrt(81)}')\n"
    "print(f'mean = {mean}')"
)

__version__ = "1.2.8"
__all__ = ["execute", "DEFAULT_EXAMPLE", "__version__"]
