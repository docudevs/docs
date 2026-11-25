from check_python import check_source


class FakeClient:
    async def create_case(self, name: str, description: str | None = None): ...

    async def flexible(self, **kwargs): ...

    _private = None
    attribute = 3


def test_valid_call_passes():
    assert check_source("async def main():\n    await client.create_case(name='a')\n", "p.py", FakeClient) == []


def test_unknown_method_reported_with_line():
    problems = check_source("client.create_cases(name='a')\n", "p.py", FakeClient)
    assert problems == ["p.py:1: DocuDevsClient has no method 'create_cases'"]


def test_unknown_keyword_reported():
    problems = check_source("client.create_case(title='a')\n", "p.py", FakeClient)
    assert problems == ["p.py:1: create_case() has no parameter 'title'"]


def test_kwargs_method_accepts_any_keyword():
    assert check_source("client.flexible(anything=1)\n", "p.py", FakeClient) == []


def test_other_receivers_ignored():
    assert check_source("import json\nprint(json.dumps(result.parsed, indent=2))\n", "p.py", FakeClient) == []


def test_syntax_error_reported():
    problems = check_source("def broken(:\n", "p.py", FakeClient)
    assert len(problems) == 1
    assert problems[0].startswith("p.py:1:")


def test_awaited_call_is_checked():
    problems = check_source("async def main():\n    await client.nope()\n", "p.py", FakeClient)
    assert problems == ["p.py:2: DocuDevsClient has no method 'nope'"]


def test_star_kwargs_at_call_site_skipped():
    assert check_source("client.create_case(**opts)\n", "p.py", FakeClient) == []


def test_private_attribute_reported():
    problems = check_source("client._private()\n", "p.py", FakeClient)
    assert problems == ["p.py:1: DocuDevsClient has no method '_private'"]


def test_non_callable_attribute_reported():
    problems = check_source("client.attribute()\n", "p.py", FakeClient)
    assert problems == ["p.py:1: DocuDevsClient has no method 'attribute'"]


def test_attribute_chains_not_checked():
    assert check_source("client.foo.bar(x=1)\n", "p.py", FakeClient) == []


def test_each_bad_keyword_reported_once():
    problems = check_source("client.create_case(a=1, b=2)\n", "p.py", FakeClient)
    assert problems == [
        "p.py:1: create_case() has no parameter 'a'",
        "p.py:1: create_case() has no parameter 'b'",
    ]
