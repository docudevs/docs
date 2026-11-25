import re
import shlex
import sys
from pathlib import Path

import click


def check_source(source: str, filename: str, root_command: click.Command) -> list[str]:
    problems: list[str] = []
    for line_number, line in _logical_lines(source):
        if not line.lstrip().startswith("docudevs "):
            continue
        try:
            tokens = shlex.split(line, comments=False, posix=True)
        except ValueError as error:
            problems.append(f"{filename}:{line_number}: {error}")
            continue
        problem = _check_tokens(tokens, root_command)
        if problem:
            problems.append(f"{filename}:{line_number}: {problem}")
    return problems


def _logical_lines(source: str) -> list[tuple[int, str]]:
    lines: list[tuple[int, str]] = []
    pending: list[str] = []
    start = 1
    for number, physical in enumerate(source.splitlines(), start=1):
        if not pending:
            start = number
        if re.search(r"\\\s*$", physical):
            pending.append(re.sub(r"\\\s*$", "", physical))
            continue
        pending.append(physical)
        lines.append((start, " ".join(pending)))
        pending = []
    if pending:
        lines.append((start, " ".join(pending)))
    return lines


def _check_tokens(tokens: list[str], root_command: click.Command) -> str | None:
    command = root_command
    path = [tokens[0]]
    for token in tokens[1:]:
        if token.startswith("-") or not isinstance(command, click.Group):
            break
        child = command.commands.get(token)
        path.append(token)
        if child is None:
            return f"unknown command '{' '.join(path)}'"
        command = child
    known = _option_names(command)
    for token in tokens[1:]:
        if token.startswith("--"):
            option = token.split("=", 1)[0]
            if option not in known:
                return f"'{' '.join(path)}' has no option '{option}'"
    return None


def _option_names(command: click.Command) -> set[str]:
    names = {"--help"}
    for param in command.params:
        names.update(getattr(param, "opts", []))
        names.update(getattr(param, "secondary_opts", []))
    return names


def main(argv: list[str]) -> int:
    from docudevs.cli import cli

    root = Path(argv[1])
    files = sorted(root.rglob("cli.sh"))
    problems: list[str] = []
    for file in files:
        problems.extend(check_source(file.read_text(), str(file), cli))
    if problems:
        print("\n".join(problems))
        return 1
    print(f"{len(files)} files checked")
    return 0


if __name__ == "__main__":
    sys.exit(main(sys.argv))
