import ast
import inspect
import sys
from pathlib import Path


def check_source(source: str, filename: str, client_cls: type) -> list[str]:
    try:
        tree = ast.parse(source, filename)
    except SyntaxError as error:
        return [f"{filename}:{error.lineno or 1}: {error.msg}"]
    problems: list[str] = []
    for node in ast.walk(tree):
        if _is_client_call(node):
            problems.extend(_check_call(node, filename, client_cls))
    return problems


def _is_client_call(node: ast.AST) -> bool:
    return (
        isinstance(node, ast.Call)
        and isinstance(node.func, ast.Attribute)
        and isinstance(node.func.value, ast.Name)
        and node.func.value.id == "client"
    )


def _check_call(node: ast.Call, filename: str, client_cls: type) -> list[str]:
    name = node.func.attr
    method = None if name.startswith("_") else getattr(client_cls, name, None)
    if not callable(method):
        return [f"{filename}:{node.lineno}: DocuDevsClient has no method '{name}'"]
    if any(keyword.arg is None for keyword in node.keywords):
        return []
    parameters = inspect.signature(method).parameters
    if any(p.kind is inspect.Parameter.VAR_KEYWORD for p in parameters.values()):
        return []
    return [
        f"{filename}:{node.lineno}: {name}() has no parameter '{keyword.arg}'"
        for keyword in node.keywords
        if keyword.arg not in parameters
    ]


def main(argv: list[str]) -> int:
    from docudevs.docudevs_client import DocuDevsClient

    root = Path(argv[1])
    files = sorted(root.rglob("python.py"))
    problems: list[str] = []
    for file in files:
        problems.extend(check_source(file.read_text(), str(file), DocuDevsClient))
    if problems:
        print("\n".join(problems))
        return 1
    print(f"{len(files)} files checked")
    return 0


if __name__ == "__main__":
    sys.exit(main(sys.argv))
