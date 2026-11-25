import json
import re
import sys
from pathlib import Path


def load_client() -> type:
    from docudevs.docudevs_client import DocuDevsClient

    return DocuDevsClient


def public_methods(client_cls: type) -> list[str]:
    names = {
        name
        for klass in client_cls.__mro__
        if klass is not object
        for name, value in vars(klass).items()
        if not name.startswith("_") and callable(value)
    }
    return sorted(names)


def write_methods(path: Path) -> None:
    path.write_text(json.dumps(public_methods(load_client()), indent=2) + "\n")


def _entries(path: Path) -> set[str]:
    names: set[str] = set()
    for line in path.read_text().splitlines():
        entry = line.partition("#")[0].strip()
        if entry:
            names.add(entry)
    return names


def _called_in_examples(examples_dir: Path) -> set[str]:
    call = re.compile(r"\bclient\.(\w+)\(")
    return {name for file in examples_dir.rglob("python.py") for name in call.findall(file.read_text())}


def _covered_by_reference(reference: Path, methods: list[str]) -> set[str]:
    text = reference.read_text()
    return {name for name in methods if re.search(rf"\b{re.escape(name)}\(", text)}


def main(argv: list[str]) -> int:
    if len(argv) == 3 and argv[1] == "--write-methods":
        write_methods(Path(argv[2]))
        return 0
    if len(argv) != 5:
        print(
            f"usage: {Path(argv[0]).name} <examples-directory> <sdk-methods-page> <exclusions-file> <uncovered-file>",
            file=sys.stderr,
        )
        return 2
    examples_dir, reference, exclusions, uncovered_file = (Path(arg) for arg in argv[1:])
    methods = public_methods(load_client())
    covered = _called_in_examples(examples_dir) | _covered_by_reference(reference, methods) | _entries(exclusions)
    uncovered = {name for name in methods if name not in covered}
    listed = _entries(uncovered_file)
    unlisted = sorted(uncovered - listed)
    stale = sorted(listed - uncovered)
    for name in unlisted:
        print(f"{name}: not called in an example or the SDK reference; add coverage or list it in {uncovered_file}")
    for name in stale:
        print(f"{name}: now covered or unknown; remove it from {uncovered_file}")
    if unlisted or stale:
        return 1
    print(f"{len(methods)} public methods, {len(uncovered)} uncovered")
    return 0


if __name__ == "__main__":
    sys.exit(main(sys.argv))
