import subprocess
import sys
from pathlib import Path

import pytest

from check_fences import Fence, check_page, extract_fences

SCRIPT = Path(__file__).resolve().parent.parent / "check_fences.py"


def write_page(directory: Path, name: str, body: str) -> Path:
    page = directory / name
    page.parent.mkdir(parents=True, exist_ok=True)
    page.write_text(body)
    return page


def fenced(lang: str, code: str) -> str:
    return f"# Title\n\n```{lang}\n{code}\n```\n"


def test_extract_fences_records_language_line_and_code():
    fences = extract_fences("intro\n\n```python title=\"x\"\nclient.a()\n```\n\n~~~bash\nls\n~~~\n")
    assert fences == [Fence("python", 4, "client.a()"), Fence("bash", 8, "ls")]


def test_extract_fences_dedents_indented_fences():
    fences = extract_fences("  ```python\n  client.a()\n    x = 1\n  ```\n")
    assert fences == [Fence("python", 2, "client.a()\n  x = 1")]


def test_unknown_python_method_reported(tmp_path):
    page = write_page(tmp_path, "a.md", fenced("python", "client.no_such_method()"))
    assert check_page(page) == [f"{page}:4: DocuDevsClient has no method 'no_such_method'"]


def test_known_python_method_passes(tmp_path):
    page = write_page(tmp_path, "a.md", fenced("python", "client.list_cases()"))
    assert check_page(page) == []


def test_top_level_await_is_parsed(tmp_path):
    page = write_page(tmp_path, "a.md", fenced("python", "x = await client.no_such_method()"))
    assert len(check_page(page)) == 1


def test_unparsable_fragment_with_bad_method_reported(tmp_path):
    page = write_page(tmp_path, "a.md", fenced("python", "client.no_such_method(\n  ..."))
    assert check_page(page) == [f"{page}:4: DocuDevsClient has no method 'no_such_method'"]


def test_unparsable_fragment_with_real_method_passes(tmp_path):
    page = write_page(tmp_path, "a.md", fenced("python", "client.list_cases(\n  ..."))
    assert check_page(page) == []


def test_bash_docudevs_command_checked(tmp_path):
    page = write_page(tmp_path, "a.md", fenced("bash", "docudevs no-such-command"))
    assert check_page(page) == [f"{page}:4: unknown command 'docudevs no-such-command'"]


def test_bash_other_commands_ignored(tmp_path):
    page = write_page(tmp_path, "a.md", fenced("shell", "curl https://x\npip install docu-devs-api-client"))
    assert check_page(page) == []


def test_other_languages_ignored(tmp_path):
    page = write_page(tmp_path, "a.md", fenced("json", "client.no_such_method()"))
    assert check_page(page) == []


def test_openapi_pages_skipped(tmp_path):
    page = write_page(tmp_path, "docs/openapi/a.md", fenced("python", "client.no_such_method()"))
    assert check_page(page) == []


def run_cli(*args) -> subprocess.CompletedProcess:
    return subprocess.run([sys.executable, str(SCRIPT), *map(str, args)], capture_output=True, text=True)


@pytest.fixture
def docs(tmp_path):
    root = tmp_path / "docs"
    write_page(root, "a.md", fenced("python", "client.no_such_method()"))
    return root


def test_cli_exits_zero_when_problems_match_known(docs, tmp_path):
    known = tmp_path / "known.txt"
    known.write_text("# comment\n\ndocs/a.md:DocuDevsClient has no method 'no_such_method'\n")
    result = run_cli(docs, known)
    assert result.returncode == 0, result.stdout + result.stderr


def test_cli_exits_one_for_unlisted_problem(docs, tmp_path):
    known = tmp_path / "known.txt"
    known.write_text("")
    result = run_cli(docs, known)
    assert result.returncode == 1
    assert "no_such_method" in result.stdout


def test_cli_exits_one_for_stale_listed_entry(docs, tmp_path):
    known = tmp_path / "known.txt"
    known.write_text(
        "docs/a.md:DocuDevsClient has no method 'no_such_method'\n"
        "docs/a.md:DocuDevsClient has no method 'gone'\n"
    )
    result = run_cli(docs, known)
    assert result.returncode == 1
    assert "gone" in result.stdout


def test_cli_collapses_duplicate_problems(tmp_path):
    root = tmp_path / "docs"
    write_page(root, "a.md", fenced("python", "client.no_such_method()\nclient.no_such_method()"))
    known = tmp_path / "known.txt"
    known.write_text("docs/a.md:DocuDevsClient has no method 'no_such_method'\n")
    assert run_cli(root, known).returncode == 0


def test_cli_exits_two_for_missing_directory(tmp_path):
    known = tmp_path / "known.txt"
    known.write_text("")
    assert run_cli(tmp_path / "missing", known).returncode == 2


def test_cli_exits_two_for_missing_known_file(docs, tmp_path):
    assert run_cli(docs, tmp_path / "missing.txt").returncode == 2
