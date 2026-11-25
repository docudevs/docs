import click

from check_cli import check_source as check_cli


@click.group()
def root(): ...


@root.group()
def cases(): ...


@cases.command("list")
@click.option("--limit", type=int)
@click.option("--verbose/--quiet")
def list_cases(limit, verbose): ...


@cases.group()
def labels(): ...


@labels.group()
def rules(): ...


@rules.command()
@click.option("--name")
def add(name): ...


@root.command()
def version(): ...


def test_known_command_and_option_pass():
    assert check_cli("docudevs cases list --limit 5\n", "c.sh", root) == []


def test_unknown_subcommand_reported():
    assert check_cli("docudevs cases lst\n", "c.sh", root) == ["c.sh:1: unknown command 'docudevs cases lst'"]


def test_unknown_option_reported():
    assert check_cli("docudevs cases list --limt 5\n", "c.sh", root) == [
        "c.sh:1: 'docudevs cases list' has no option '--limt'"
    ]


def test_continuation_lines_joined_and_other_commands_ignored():
    assert check_cli("export X=1\ndocudevs cases list \\\n  --limit 5\n", "c.sh", root) == []


def test_equals_form_accepted():
    assert check_cli("docudevs cases list --limit=5\n", "c.sh", root) == []


def test_equals_form_unknown_reported():
    assert check_cli("docudevs cases list --limt=5\n", "c.sh", root) == [
        "c.sh:1: 'docudevs cases list' has no option '--limt'"
    ]


def test_help_accepted():
    assert check_cli("docudevs cases list --help\n", "c.sh", root) == []


def test_secondary_option_accepted():
    assert check_cli("docudevs cases list --quiet\n", "c.sh", root) == []


def test_nested_group_path_three_levels_deep():
    assert check_cli("docudevs cases labels rules add --name x\n", "c.sh", root) == []
    assert check_cli("docudevs cases labels rules ad\n", "c.sh", root) == [
        "c.sh:1: unknown command 'docudevs cases labels rules ad'"
    ]


def test_line_number_is_start_of_logical_line():
    assert check_cli("echo hi\ndocudevs cases nope\n", "c.sh", root) == ["c.sh:2: unknown command 'docudevs cases nope'"]


def test_positional_arguments_after_command_ignored():
    assert check_cli("docudevs version extra\n", "c.sh", root) == []
