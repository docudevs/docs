import ast
import re
import sys
from dataclasses import dataclass
from pathlib import Path

import check_cli
import check_python

PAGE_SUFFIXES = {".md", ".mdx"}
SHELL_LANGUAGES = {"bash", "shell", "sh", "zsh", "console"}
OPENING_FENCE = re.compile(r"^(\s*)(`{3,}|~{3,})\s*([^\s`]*)")
CLIENT_CALL = re.compile(r"\bclient\.(\w+)\(")
PLACEHOLDER = "fence"
PROBLEM = re.compile(rf"^{PLACEHOLDER}:(\d+): (.*)$")


@dataclass(frozen=True)
class Fence:
    lang: str
    line: int
    code: str


def extract_fences(markdown: str) -> list[Fence]:
    fences: list[Fence] = []
    lines = markdown.splitlines()
    index = 0
    while index < len(lines):
        opening = OPENING_FENCE.match(lines[index])
        if not opening:
            index += 1
            continue
        indent, marker, lang = opening.groups()
        start = index + 2
        body: list[str] = []
        index += 1
        while index < len(lines) and not _closes(lines[index], marker):
            line = lines[index]
            body.append(line[len(indent):] if line.startswith(indent) else line.lstrip())
            index += 1
        fences.append(Fence(lang.lower(), start, "\n".join(body)))
        index += 1
    return fences


def _closes(line: str, marker: str) -> bool:
    stripped = line.strip()
    return stripped.startswith(marker) and set(stripped) == {marker[0]}


def check_page(path: Path, root: Path | None = None) -> list[str]:
    if _is_openapi_page(path):
        return []
    from docudevs.cli import cli
    from docudevs.docudevs_client import DocuDevsClient

    page = str(path.relative_to(root) if root else path)
    problems: list[str] = []
    for fence in extract_fences(path.read_text()):
        if fence.lang == "python":
            found = _check_python_fence(fence.code, DocuDevsClient)
        elif fence.lang in SHELL_LANGUAGES:
            found = check_cli.check_source(fence.code, PLACEHOLDER, cli)
        else:
            continue
        for problem in found:
            match = PROBLEM.match(problem)
            line = fence.line + int(match.group(1)) - 1
            problems.append(f"{page}:{line}: {match.group(2)}")
    return problems


def _is_openapi_page(path: Path) -> bool:
    parts = path.parts
    return any(a == "docs" and b == "openapi" for a, b in zip(parts, parts[1:]))


def _check_python_fence(code: str, client_cls: type) -> list[str]:
    try:
        ast.parse(code)
    except SyntaxError:
        return _check_fragment(code, client_cls)
    return check_python.check_source(code, PLACEHOLDER, client_cls)


def _check_fragment(code: str, client_cls: type) -> list[str]:
    problems: list[str] = []
    for number, line in enumerate(code.splitlines(), start=1):
        for name in CLIENT_CALL.findall(line):
            if name.startswith("_") or not callable(getattr(client_cls, name, None)):
                problems.append(f"{PLACEHOLDER}:{number}: DocuDevsClient has no method '{name}'")
    return problems


def _entry(problem: str) -> str:
    page, _, rest = problem.partition(":")
    return f"{page}:{rest.partition(': ')[2]}"


def _read_known(path: Path) -> set[str]:
    return {
        line.strip()
        for line in path.read_text().splitlines()
        if line.strip() and not line.lstrip().startswith("#")
    }


def main(argv: list[str]) -> int:
    if len(argv) != 3 or not Path(argv[1]).is_dir() or not Path(argv[2]).is_file():
        print(f"usage: {Path(argv[0]).name} <docs-directory> <known-failures-file>", file=sys.stderr)
        return 2
    docs_dir = Path(argv[1]).resolve()
    root = docs_dir.parent
    pages = sorted(p for p in docs_dir.rglob("*") if p.suffix in PAGE_SUFFIXES)
    entries: dict[str, str] = {}
    for page in pages:
        for problem in check_page(page, root):
            entries.setdefault(_entry(problem), problem)
    known = _read_known(Path(argv[2]))
    unlisted = sorted(set(entries) - known)
    stale = sorted(known - set(entries))
    for entry in unlisted:
        print(f"unlisted: {entries[entry]}")
    for entry in stale:
        print(f"stale known failure (fixed or removed, delete it): {entry}")
    if unlisted or stale:
        return 1
    print(f"{len(pages)} pages checked, {len(known)} known failures")
    return 0


if __name__ == "__main__":
    sys.exit(main(sys.argv))
