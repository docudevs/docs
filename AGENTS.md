# DocuDevs documentation repository instructions

This is the public documentation site for DocuDevs, built with Docusaurus. A push to `main` publishes docs.docudevs.ai through Azure Static Web Apps, so never commit to `main` directly: work on a branch and open a pull request. The site build is Node-only.

## Layout

| Path | Purpose |
| --- | --- |
| `docs/` | Markdown pages |
| `examples/` | Verified code examples embedded in pages |
| `tools/examples/` | Example tooling and the checks that run in CI |
| `static/files/docudevs.yaml` | Vendored OpenAPI spec, pinned to the released API |
| `src/components/Example/` | The `<Example>` component |
| `src/generated/` | Build output (`examples.json`), gitignored |
| `.github/workflows/examples.yml` | CI for the example checks |

## Examples

Every code block that calls the API, SDK or CLI lives in a file under `examples/` and is embedded with `<Example name="<area>/<name>" />`. The component is registered globally; do not import it. Do not write language `<Tabs>` inline. Tabs for other purposes (for example pip/uv installs) are fine. Leave install commands, JSON and response samples and environment setup inline.

### Layout

`examples/<area>/<name>/` holds up to four files. Directory names are lowercase with dashes and describe the user action.

| File | Tab label |
| --- | --- |
| `curl.sh` | cURL |
| `python.py` | Python |
| `Example.java` | Java |
| `cli.sh` | CLI |

Tabs always render in that order, grouped with `groupId="lang"` and `queryString="lang"`. A language without a file gets no tab. Every example needs cURL and Python. Java is required where the published Java facade has the method; otherwise record the gap (see the ratchet). CLI is optional where a command exists. Every file is complete and valid on its own, and all languages of one example share the scenario, sample file names and variable names.

### Markers

A comment line ending in `docs:start` or `docs:end` selects what the page shows. Without markers the whole file is shown, minus a shebang. Several regions are joined with a blank line.

### Visibility rule

Markers may hide only imports, client construction and environment setup. Everything the shown code calls or reads (helpers, variables holding document bytes, ids produced earlier) must be visible. A reader who pastes the shown snippet into a file that already has the client set up must not hit an undefined name. An id from an earlier step is declared visibly at the top of the shown region with a comment saying where it comes from, in the same way in every language. The first example on a page shows imports and client construction in full; later ones may hide them.

### Language conventions

- All languages: the key comes from `DOCUDEVS_API_KEY`; the base URL is `https://api.docudevs.ai`. Status codes and response fields shown must match the spec.
- cURL: quoted `https://api.docudevs.ai/...` URLs, `-H "Authorization: $DOCUDEVS_API_KEY"`, one flag per line.
- Python: `from docudevs.docudevs_client import DocuDevsClient`, async, runnable under `asyncio.run(main())`. Name the client variable `client`; the checker inspects only calls on that name. The client does not consistently raise on HTTP errors, so examples check the status instead of relying on exceptions.
- Java: `ai.docudevs.client.DocuDevsClient` through its builder, synchronous. The file starts with `package examples.<area>.<name>;` (dashes become underscores), declares `public class Example`, and keeps the package line outside the shown region.
- CLI: pass `--token "$DOCUDEVS_API_KEY"` because the published CLI does not read that variable.
- Comments that teach the reader are content and are allowed in example files.

### Sources of truth

Checks run against published packages, never a sibling checkout. The main repository is `docudevs/docudevs`, normally checked out next to this one.

- Features are specified in the main repository's `features/` directory.
- The main repository's `docudevs-sdk/tests` are the reference for Python usage.
- Python and Java signatures: the published packages pinned below. If repository source and published package disagree, the published package wins.
- cURL: `static/files/docudevs.yaml`.
- CLI: the commands of the pinned Python package.

## Pinned versions

| Package | Pinned in |
| --- | --- |
| `docu-devs-api-client` (Python SDK and CLI) | `tools/examples/python/requirements.txt` |
| `ai.docudevs:docudevs-java-sdk` | `tools/examples/java/build.gradle.kts` |

Public docs show only what a released SDK can run. Bump a pin after the SDK is released, then update the examples. To draft ahead of a release, use local overrides and do not merge examples that depend on them:

- Python: run `uv pip install --python tools/examples/python/.venv/bin/python -e <main repo>/docudevs-sdk` into `tools/examples/python/.venv` (manual; the checkers use whatever is installed).
- Java: `./gradlew compileJava -PsdkRoot=<main repo>` from `tools/examples/java` includes the main repository's Java SDK build.
- Spec: `DOCUDEVS_MAIN_ROOT=<main repo> npm run sync-openapi-spec` copies `docudevs.yaml` from the main repository.

## Spec and API reference

Spec sync is always deliberate and never part of `start` or `build`, so the tracked, release-pinned `static/files/docudevs.yaml` is not overwritten by accident. When refreshing the spec for a release, run `npm run sync-openapi-spec` (set `DOCUDEVS_MAIN_ROOT` if the main repository is not at `../docudevs`), then regenerate the API reference with `npm run api-docs` (or `npm run clean-api-docs` and `npm run gen-api-docs` separately) and commit the results.

## Checks

Run these before opening a pull request; CI (`.github/workflows/examples.yml`) runs them on every pull request and push to `main`.

| Command | What it checks |
| --- | --- |
| `npm run examples:test` | Unit tests of the example tooling |
| `npm run examples:manifest` | Example files are recognised, markers are balanced, Java package lines match the directory, every `<Example name>` used in `docs/` exists; writes `src/generated/examples.json` |
| `npm run examples:coverage` | Missing cURL, Python and Java files match `examples/known-gaps.txt` |
| `npm run examples:curl` | `curl.sh` and `cli.sh` pass `bash -n`; each cURL request matches a path and method in the spec |
| `npm run typecheck` | TypeScript in `src/` |
| `npm run build` | The site builds (broken links only warn) |

Python checks need a virtual environment, which `tools/examples/python/.venv/` is for:

```bash
uv venv tools/examples/python/.venv
uv pip install --python tools/examples/python/.venv/bin/python -r tools/examples/python/requirements.txt
tools/examples/python/.venv/bin/python -m pytest tools/examples/python/tests -q
tools/examples/python/.venv/bin/python tools/examples/python/check_python.py examples
tools/examples/python/.venv/bin/python tools/examples/python/check_cli.py examples
```

`check_python.py` verifies that each `client.<method>(...)` call names a method of the pinned `DocuDevsClient` and uses keyword arguments it accepts. `check_cli.py` verifies each `docudevs ...` command and option against the pinned CLI.

Java needs a JDK 17 or newer:

```bash
cd tools/examples/java && ./gradlew compileJava
```

This compiles every `Example.java` against the pinned Java SDK.

The checks do not validate response field names, request body shapes, or that the language variants of an example do the same thing. Reviewers must read each example against the spec and SDK for those.

### Known gaps ratchet

`examples/known-gaps.txt` lists one `<area>/<name>:<lang>` per line, sorted, for guaranteed languages (cURL, Python, Java) an example does not yet have. `examples:coverage` fails both when an unlisted gap appears and when a listed gap has been filled, so the file only ever shrinks. Remove the line when you add the missing file.

## User-facing SDK methods

A user-facing Python client method is a public method of `DocuDevsClient` (no leading underscore) that is not a transport helper. Each one should be covered by a guide or example. Methods that do not need coverage are listed in `examples/sdk-exclusions.txt`, one method name per line.
