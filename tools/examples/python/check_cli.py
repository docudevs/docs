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
            tokens = _tokenise(line)
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


def _tokenise(line: str) -> list[str]:
    lexer = shlex.shlex(line, posix=True, punctuation_chars=True)
    lexer.whitespace_split = True
    lexer.commenters = "#"
    tokens: list[str] = []
    for token in lexer:
        if set(token) <= set(lexer.punctuation_chars):
            break
        tokens.append(token)
    return tokens


def _check_tokens(tokens: list[str], root_command: click.Command) -> str | None:
    command = root_command
    path = [tokens[0]]
    index = 1
    while index < len(tokens):
        token = tokens[index]
        if token.startswith("--"):
            option = token.split("=", 1)[0]
            params = _option_params(command)
            if option not in params:
                return f"'{' '.join(path)}' has no option '{option}'"
            if isinstance(command, click.Group) and "=" not in token and _takes_value(params[option]):
                index += 1
        elif isinstance(command, click.Group) and not token.startswith("-"):
            child = command.commands.get(token)
            path.append(token)
            if child is None:
                return f"unknown command '{' '.join(path)}'"
            command = child
        index += 1
    return None


def _option_params(command: click.Command) -> dict[str, click.Parameter | None]:
    params: dict[str, click.Parameter | None] = {"--help": None}
    for param in command.params:
        for name in [*getattr(param, "opts", []), *getattr(param, "secondary_opts", [])]:
            params[name] = param
    return params


def _takes_value(param: click.Parameter | None) -> bool:
    return isinstance(param, click.Option) and not param.is_flag and not param.count


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
