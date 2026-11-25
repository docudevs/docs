import json
import subprocess
import sys
from pathlib import Path

from check_method_coverage import main, public_methods

ROOT = Path(__file__).resolve().parents[4]


class FakeClient:
    async def covered_by_example(self): ...

    async def covered_by_reference(self): ...

    async def excluded(self): ...

    async def uncovered(self): ...

    _private = None
    attribute = 3


def run(tmp_path, uncovered_lines, monkeypatch, capsys):
    examples = tmp_path / "examples" / "area" / "thing"
    examples.mkdir(parents=True)
    (examples / "python.py").write_text("await client.covered_by_example()\n")
    reference = tmp_path / "sdk-methods.md"
    reference.write_text("Use `covered_by_reference(` here.\nAlso get_case_summary(\n")
    exclusions = tmp_path / "exclusions.txt"
    exclusions.write_text("# header\nexcluded  # reason\n")
    uncovered = tmp_path / "uncovered.txt"
    uncovered.write_text("\n".join(uncovered_lines))
    monkeypatch.setattr("check_method_coverage.load_client", lambda: FakeClient)
    code = main(["check", str(tmp_path / "examples"), str(reference), str(exclusions), str(uncovered)])
    return code, capsys.readouterr().out


def test_passes_when_uncovered_file_matches(tmp_path, monkeypatch, capsys):
    code, _ = run(tmp_path, ["uncovered"], monkeypatch, capsys)
    assert code == 0


def test_fails_when_uncovered_method_is_not_listed(tmp_path, monkeypatch, capsys):
    code, out = run(tmp_path, [], monkeypatch, capsys)
    assert code == 1
    assert "uncovered" in out


def test_fails_when_listed_method_is_now_covered(tmp_path, monkeypatch, capsys):
    code, out = run(tmp_path, ["uncovered", "covered_by_example"], monkeypatch, capsys)
    assert code == 1
    assert "covered_by_example" in out


def test_private_and_non_callable_attributes_are_never_reported(tmp_path, monkeypatch, capsys):
    code, out = run(tmp_path, ["uncovered"], monkeypatch, capsys)
    assert code == 0
    assert "_private" not in out and "attribute" not in out


def test_public_methods_are_sorted_callables_defined_on_the_class():
    assert public_methods(FakeClient) == ["covered_by_example", "covered_by_reference", "excluded", "uncovered"]


def test_reference_match_is_a_whole_word(tmp_path, monkeypatch, capsys):
    class Client:
        async def get_case(self): ...

    monkeypatch.setattr("check_method_coverage.load_client", lambda: Client)
    (tmp_path / "examples").mkdir()
    reference = tmp_path / "ref.md"
    reference.write_text("get_case_summary(\n")
    exclusions = tmp_path / "e.txt"
    exclusions.write_text("")
    uncovered = tmp_path / "u.txt"
    uncovered.write_text("get_case\n")
    assert main(["c", str(tmp_path / "examples"), str(reference), str(exclusions), str(uncovered)]) == 0


def test_committed_method_list_matches_installed_client():
    from check_method_coverage import load_client

    committed = json.loads((ROOT / "tools/examples/client-methods.json").read_text())
    assert committed == public_methods(load_client())


def test_write_methods_flag_writes_sorted_json(tmp_path, monkeypatch):
    monkeypatch.setattr("check_method_coverage.load_client", lambda: FakeClient)
    target = tmp_path / "methods.json"
    from check_method_coverage import write_methods

    write_methods(target)
    assert json.loads(target.read_text()) == public_methods(FakeClient)
