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


def test_pipe_truncates_arguments():
    assert check_cli("docudevs cases list | jq --raw-output '.[]'\n", "c.sh", root) == []
    assert check_cli("docudevs cases list|jq --raw-output\n", "c.sh", root) == []


def test_trailing_comment_dropped():
    assert check_cli("docudevs cases list # use --verbose\n", "c.sh", root) == []


def test_and_operator_truncates():
    assert check_cli("docudevs cases list && other --flag\n", "c.sh", root) == []
    assert check_cli("docudevs cases list; other --flag\n", "c.sh", root) == []


def test_redirects_truncate():
    assert check_cli("docudevs cases list > out.json --flag\n", "c.sh", root) == []
    assert check_cli("docudevs cases list >> out.json 2> err.txt\n", "c.sh", root) == []
    assert check_cli("docudevs cases list < in.txt --flag\n", "c.sh", root) == []


def test_options_before_pipe_still_checked():
    assert check_cli("docudevs cases list --limt 5 | jq .\n", "c.sh", root) == [
        "c.sh:1: 'docudevs cases list' has no option '--limt'"
    ]


def test_tokens_with_paths_at_files_and_json_survive():
    line = "docudevs cases list --limit 5 ~/a/b.txt @file.json '{\"a\": [1, 2]}' --verbose\n"
    assert check_cli(line, "c.sh", root) == []


@click.group()
@click.option("--api-url")
@click.option("--debug", is_flag=True)
def globals_root(): ...


@globals_root.command("go")
@click.option("--limit", type=int)
def go(limit): ...


def test_global_value_option_before_subcommand():
    assert check_cli("docudevs --api-url X go --limit 5\n", "c.sh", globals_root) == []


def test_global_equals_option_before_subcommand():
    assert check_cli("docudevs --api-url=X go --limit 5\n", "c.sh", globals_root) == []


def test_global_flag_option_does_not_consume_subcommand():
    assert check_cli("docudevs --debug go --limit 5\n", "c.sh", globals_root) == []


def test_unknown_global_option_reported_against_root():
    assert check_cli("docudevs --nope go\n", "c.sh", globals_root) == [
        "c.sh:1: 'docudevs' has no option '--nope'"
    ]


def test_subcommand_option_after_global_still_checked():
    assert check_cli("docudevs --api-url X go --limt 5\n", "c.sh", globals_root) == [
        "c.sh:1: 'docudevs go' has no option '--limt'"
    ]
