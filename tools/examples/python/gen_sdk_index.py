import inspect
import re
import sys
from importlib import metadata
from pathlib import Path

from check_method_coverage import load_client, public_methods

NOTES_FILE = Path(__file__).resolve().parent / "sdk-index-notes.txt"
PACKAGE = "docu-devs-api-client"
EMPTY = "—"
MAX_DEFAULT = 40
ABBREVIATIONS = ("e.g.", "i.e.")


def _cell(text: str) -> str:
    return " ".join(text.split()).replace("|", "\\|")


def _code(text: str) -> str:
    text = _cell(text)
    return f"`` {text} ``" if "`" in text else f"`{text}`"


def _prose(text: str) -> str:
    text = " ".join(text.split())
    for char, entity in (("&", "&amp;"), ("<", "&lt;"), (">", "&gt;"), ("{", "&#123;"), ("}", "&#125;")):
        text = text.replace(char, entity)
    return text.replace("|", "\\|")


def _repr(value: object) -> str:
    text = repr(value)
    return "…" if len(text) > MAX_DEFAULT or re.search(r" at 0x[0-9a-fA-F]+", text) else text


def _annotation(annotation: object) -> str:
    return annotation if isinstance(annotation, str) else inspect.formatannotation(annotation)


def _parameter(parameter: inspect.Parameter) -> str:
    prefix = {inspect.Parameter.VAR_POSITIONAL: "*", inspect.Parameter.VAR_KEYWORD: "**"}.get(parameter.kind, "")
    text = prefix + parameter.name
    if parameter.annotation is not inspect.Parameter.empty:
        text += f": {_annotation(parameter.annotation)}"
    if parameter.default is not inspect.Parameter.empty:
        text += f" = {_repr(parameter.default)}"
    return text


def _first_sentence(doc: str | None) -> str:
    if not doc:
        return ""
    paragraph = " ".join(doc.strip().split("\n\n")[0].split())
    masked = paragraph
    for abbreviation in ABBREVIATIONS:
        masked = masked.replace(abbreviation, abbreviation.replace(".", "\0"))
    match = re.search(r"(?<=[.!?])\s", masked)
    return paragraph[: match.start()] if match else paragraph


def _row(client_cls: type, name: str, notes: dict[str, str]) -> str:
    method = getattr(client_cls, name)
    signature = inspect.signature(method)
    parameters = [p for index, p in enumerate(signature.parameters.values()) if not (index == 0 and p.name == "self")]
    params = "<br />".join(_code(_parameter(p)) for p in parameters) if parameters else EMPTY
    returns = (
        EMPTY
        if signature.return_annotation is inspect.Signature.empty
        else _code(_annotation(signature.return_annotation))
    )
    description = _prose(_first_sentence(inspect.getdoc(method)))
    return f"| `{name}` | {params} | {returns} | {description} | {_prose(notes.get(name, ''))} |"


def render_index(client_cls: type, notes: dict[str, str] | None = None) -> str:
    notes = notes or {}
    version = metadata.version(PACKAGE)
    header = [
        "---",
        "title: SDK Method Index",
        "description: Every public method of the DocuDevs Python client with its parameters, return type and a one-line summary.",
        "---",
        "",
        f"This table is generated from the released Python client (`{PACKAGE}` {version}) and lists every public "
        "method of `DocuDevsClient`. The hand-written [SDK Methods](/docs/reference/sdk-methods) page has "
        "explanations and examples.",
        "",
        "| Method | Parameters | Returns | Description | Notes |",
        "| --- | --- | --- | --- | --- |",
    ]
    return "\n".join(header + [_row(client_cls, name, notes) for name in public_methods(client_cls)]) + "\n"


def load_notes(path: Path, client_cls: type) -> dict[str, str]:
    known = set(public_methods(client_cls))
    notes: dict[str, str] = {}
    for line in path.read_text().splitlines():
        entry, _, note = line.partition("#")
        name = entry.strip()
        if not name:
            continue
        if name not in known:
            raise ValueError(f"{path}: {name} is not a public client method")
        notes[name] = note.strip()
    return notes


def main(argv: list[str]) -> int:
    args = argv[1:]
    check = "--check" in args
    paths = [arg for arg in args if arg != "--check"]
    if len(paths) != 1:
        print(f"usage: {Path(argv[0]).name} [--check] <page-path>", file=sys.stderr)
        return 2
    path = Path(paths[0])
    client = load_client()
    page = render_index(client, load_notes(NOTES_FILE, client))
    if not check:
        path.write_text(page)
        return 0
    if not path.exists() or path.read_text() != page:
        print(f"{path} is out of date; run `npm run sdk:index`")
        return 1
    return 0


if __name__ == "__main__":
    sys.exit(main(sys.argv))
