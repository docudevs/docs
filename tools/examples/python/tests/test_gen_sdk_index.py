import re
import subprocess
import sys
from importlib import metadata
from pathlib import Path

import pytest

from gen_sdk_index import load_notes, main, render_index

HERE = Path(__file__).resolve().parents[1]


class Fake:
    async def b_plain(self):
        pass

    async def a(self, x: int, *, y: str = "k") -> dict:
        """Does a. More text."""

    async def pipe(self, mode: str | None = "a|b") -> str:
        """Has {braces} and <tags> | bars."""

    async def addr(self, thing=object()):
        pass

    async def three(self, a: int, b: str = "x", c=None):
        pass

    async def long_default(self, v="x" * 60):
        pass

    async def tick(self, v="a`b"):
        """Sends data, e.g. files. Second sentence."""

    def _private(self):
        pass


def rows(page):
    return [line for line in page.splitlines() if line.startswith("| `")]


def test_row_content():
    page = render_index(Fake)
    row = next(line for line in rows(page) if line.startswith("| `a`"))
    assert "x: int" in row
    assert "y: str = 'k'" in row
    assert "`dict`" in row
    assert "Does a." in row
    assert "More text" not in row
    assert "self" not in row


def test_private_absent_and_sorted():
    page = render_index(Fake)
    assert "_private" not in page
    names = [line.split("`")[1] for line in rows(page)]
    assert names == sorted(names) == ["a", "addr", "b_plain", "long_default", "pipe", "three", "tick"]


def test_missing_docstring_and_annotation():
    row = next(line for line in rows(render_index(Fake)) if line.startswith("| `b_plain`"))
    assert "—" in row
    assert row.split("|")[-3].strip() == ""


def test_pipes_escaped_in_cells():
    row = next(line for line in rows(render_index(Fake)) if line.startswith("| `pipe`"))
    assert "a\\|b" in row
    assert "str \\| None" in row
    assert len(re.split(r"(?<!\\)\|", row)) == 7


def test_mdx_unsafe_characters_cannot_break_the_page():
    row = next(line for line in rows(render_index(Fake)) if line.startswith("| `pipe`"))
    description = re.split(r"(?<!\\)\|", row)[-3]
    assert "{" not in description and "}" not in description and "<" not in description
    assert "&lt;tags&gt;" in description


def test_memory_address_default_is_stable():
    page = render_index(Fake)
    assert "0x" not in page
    assert "thing = …" in page
    assert render_index(Fake) == page


def test_front_matter_and_intro_name_installed_version():
    page = render_index(Fake)
    assert page.startswith("---\ntitle: SDK Method Index\n")
    assert metadata.version("docu-devs-api-client") in page
    assert "[SDK Methods](/docs/reference/sdk-methods)" in page


def test_notes_column():
    page = render_index(Fake, {"a": "Careful."})
    row = next(line for line in rows(page) if line.startswith("| `a`"))
    assert row.rstrip().endswith("Careful. |")


def test_notes_file_parsing_and_unknown_method(tmp_path):
    notes = tmp_path / "notes.txt"
    notes.write_text("a  # Careful.\n\n# comment\nb_plain  # Also.\n")
    assert load_notes(notes, Fake) == {"a": "Careful.", "b_plain": "Also."}
    notes.write_text("missing  # Nope.\n")
    with pytest.raises(ValueError, match="missing"):
        load_notes(notes, Fake)


def test_committed_notes_name_real_methods():
    from check_method_coverage import load_client

    assert load_notes(HERE / "sdk-index-notes.txt", load_client())


def run_cli(*args):
    return subprocess.run(
        [sys.executable, "-B", str(HERE / "gen_sdk_index.py"), *args], capture_output=True, text=True, cwd=HERE
    )


def test_cli_write_then_check(tmp_path):
    out = tmp_path / "page.md"
    assert run_cli(str(out)).returncode == 0
    first = out.read_bytes()
    assert run_cli(str(out)).returncode == 0
    assert out.read_bytes() == first
    assert run_cli("--check", str(out)).returncode == 0
    out.write_text(out.read_text()[:-2] + "X\n")
    result = run_cli("--check", str(out))
    assert result.returncode == 1
    assert len(result.stdout.strip().splitlines()) == 1


def test_main_usage(capsys):
    assert main(["gen_sdk_index.py"]) == 2


def test_check_flag_may_follow_the_path(tmp_path):
    out = tmp_path / "page.md"
    run_cli(str(out))
    assert run_cli(str(out), "--check").returncode == 0


def row_of(name):
    return next(line for line in rows(render_index(Fake)) if line.startswith(f"| `{name}`"))


def test_each_parameter_is_its_own_code_span():
    cell = re.split(r"(?<!\\)\|", row_of("three"))[2].strip()
    assert cell == "`a: int`<br />`b: str = 'x'`<br />`c = None`"


def test_long_default_is_elided():
    row = row_of("long_default")
    assert "v = …" in row
    assert "xxxx" not in row


def test_backtick_value_renders_in_a_wider_code_span():
    assert "`` v = 'a`b' ``" in row_of("tick")


def test_abbreviations_do_not_end_the_summary():
    row = row_of("tick")
    assert "Sends data, e.g. files. |" in row
    assert "Second" not in row
