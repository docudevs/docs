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
| `examples/live-skip.txt` | Examples the live runner does not execute, with a reason |
| `tools/examples/live-fixtures/` | Generated sample input files for the live runner |

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

Tabs always render in that order, grouped with `groupId="lang"` and `queryString="lang"`. A language without a file gets no tab. Every example needs cURL and Python, except those listed with a reason in `examples/not-applicable.txt`. Java is required where the published Java facade has the method; otherwise record the gap (see the ratchet). CLI is optional where a command exists. Every file is complete and valid on its own, and all languages of one example share the scenario, sample file names and variable names.

### Markers

A comment line ending in `docs:start` or `docs:end` selects what the page shows. Without markers the whole file is shown, minus a shebang. Several regions are joined with a blank line.

### Visibility rule

Markers may hide only imports, client construction and environment setup. Everything the shown code calls or reads (helpers, variables holding document bytes, ids produced earlier) must be visible. A reader who pastes the shown snippet into a file that already has the client set up must not hit an undefined name. Imports of SDK model classes and third-party types that the shown code names are shown; only the client import and standard-library imports may be hidden. An id from an earlier step is declared visibly at the top of the shown region with a comment saying where it comes from, in the same way in every language. The first example on a page shows imports and client construction in full; later ones may hide them.

### Language conventions

- All languages: the key comes from `API_KEY`; the base URL is `https://api.docudevs.ai`. Status codes and response fields shown must match the spec.
- Python: `from docudevs.docudevs_client import DocuDevsClient`, async, runnable under `asyncio.run(main())`. Name the client variable `client`; the checker inspects only calls on that name. The client does not consistently raise on HTTP errors, so examples check the status instead of relying on exceptions.
- Java: `ai.docudevs.client.DocuDevsClient` through its builder, synchronous. The file starts with `package examples.<area>.<name>;` (dashes become underscores), declares `public class Example`, and keeps the package line outside the shown region.
- CLI: the published CLI reads `API_KEY` from the environment, so commands start with `docudevs <command>` and pass no `--token`.
- cURL: quoted `https://api.docudevs.ai/...` URLs, `-H "Authorization: $API_KEY"`, one flag per line. Use `-f` on every call whose output is used later, unless the example reads the HTTP status on purpose. Bound every polling loop with a deadline. Copy the shared `wait_for_job` helper from `examples/jobs/process-with-auto-ocr/curl.sh` unchanged (success on `COMPLETED`, failure on `ERROR`, `FAILED` and `TIMEOUT`, 600-second deadline; its `PARTIAL` branch is only a safeguard). Python loops are bounded too, and Python checks `status_code` before writing response content to a file and guards values that can be `None`.
- The API leaves empty collections and null fields out of responses. Every `jq` path and Python read of a list or optional field tolerates absence (`(.items // [])[]`, `.get("items", [])`).
- All language variants of one example do the same thing. Where they cannot (for example cURL and Python poll while the Java SDK or the CLI makes one call), say so in a comment in the shown region and in one sentence on the page.
- Sample data is neutral (invoices, vendors, contracts, `productGroup`): no real-looking identifiers, catalogue field names, secrets or brands. Keys come from the environment.
- Comments that teach the reader are content and are allowed in example files.

### Sources of truth

Checks run against published packages, never a sibling checkout. The private main repository is normally checked out at `../docudevs`; only maintainers have access to it, so its `features/` and `docudevs-sdk/tests` are available to maintainers only.

- Features are specified in the private main repository's `features/` directory.
- The private main repository's `docudevs-sdk/tests` are the reference for Python usage.
- Python and Java signatures: the published packages pinned below. If repository source and published package disagree, the published package wins.
- cURL: `static/files/docudevs.yaml`.
- CLI: the commands of the pinned Python package.

## Public content

Pages state what the product does today and how to call it. A plain availability statement is allowed ("private preview", "dedicated deployments only", or the condition under which a feature is available). Do not publish prices or rate figures, internal endpoints, private repository names or file paths, storage paths, infrastructure hosts, operator or installation configuration keys, internal vendor or project names, agent tokens, or rollout, roadmap and changelog wording ("coming soon", "new in", "yet", "no longer", "currently", "will"). Run `node tools/port/scan.mjs --diff main` with `PORT_SCAN_LITERALS_FILE` set before pushing content taken from another source.

## Pinned versions

| Package | Pinned in |
| --- | --- |
| `docu-devs-api-client` (Python SDK and CLI) | `tools/examples/python/requirements.txt` |
| `ai.docudevs:docudevs-java-sdk` | `tools/examples/java/build.gradle.kts` |

Public docs show only what a released SDK can run. Bump a pin after the SDK is released, then update the examples. To draft ahead of a release, use local overrides and do not merge examples that depend on them:

- Python: run `uv pip install --python tools/examples/python/.venv/bin/python -e ../docudevs/docudevs-sdk` into `tools/examples/python/.venv` (manual; the checkers use whatever is installed).
- Java: `./gradlew compileJava -PsdkRoot=../docudevs` from `tools/examples/java` includes the private main repository's Java SDK build (configures; not exercised in CI).
- Spec: `DOCUDEVS_MAIN_ROOT=../docudevs npm run sync-openapi-spec` copies `docudevs.yaml`, `pipeline.schema.json` and the playbook evaluation agent skill (published verbatim as `static/files/playbook-evaluation-skill.md`) from the private main repository. The guide page `docs/core/playbook-evaluation.md` is hand-edited and not touched by the sync. `node tools/examples/sync-spec.mjs --check` lists files that differ, by name only, and exits 0 when there is no main checkout.

## Spec and API reference

Spec sync is always deliberate and never part of `start` or `build`, so the tracked, release-pinned `static/files/docudevs.yaml` is not overwritten by accident. When refreshing the spec for a release, run `npm run sync-openapi-spec` (set `DOCUDEVS_MAIN_ROOT` if the private main repository is not at `../docudevs`), then regenerate the API reference with `npm run api-docs` (or `npm run clean-api-docs` and `npm run gen-api-docs` separately) and commit the results.

## Checks

Run these before opening a pull request. CI (`.github/workflows/examples.yml`) runs every check below except `npm run build` on every pull request and push to `main`; the full site build is exercised by the pull-request preview deployment, not by the Examples workflow.

| Command | What it checks |
| --- | --- |
| `npm run examples:test` | Unit tests of the example tooling |
| `npm run examples:manifest` | Example files are recognised, markers are balanced, Java package lines match the directory, every `<Example name>` used in `docs/` exists; writes `src/generated/examples.json` |
| `npm run examples:coverage` | Missing Java files match `examples/known-gaps.txt` |
| `npm run examples:inline` | Inline language tabs and inline product calls per page do not exceed `examples/inline-language-tabs.txt` and `examples/inline-calls.txt`, and listed counts are not stale |
| `npm run examples:curl` | `curl.sh` and `cli.sh` pass `bash -n`; each cURL request matches a path and method in the spec; a `curl` to anything other than the API fails unless its URL is a variable reference |
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

`check_fences.py` applies both checks to code fences inside the Markdown pages under `docs/` (`python` fences for client calls, `bash`/`shell` fences for `docudevs` lines; `docs/openapi/` is skipped). It runs as `check_fences.py docs examples/fence-known-failures.txt`. The known-failures file is a ratchet: entries are `<page>:<message>` without line numbers, and the check fails both for a new problem and for a listed entry that no longer occurs. Fix the page and delete its entry; never add an entry to hide a new problem.

Java needs a JDK 17 installed (the build compiles with a Java 17 toolchain and has no toolchain download; Gradle itself may run on a newer JDK):

```bash
cd tools/examples/java && ./gradlew compileJava
```

This compiles every `Example.java` against the pinned Java SDK.

The checks do not validate response field names, request body shapes, or that the language variants of an example do the same thing. The Python check validates method and keyword names only (not `await`, positional arity or required arguments), and the CLI check only inspects lines that begin with `docudevs ` (not `VAR=$(docudevs ...)` or `if docudevs ...`). Reviewers must read each example against the spec and SDK for those. Confirm every response shape an example reads, and every authorization statement a page makes (per route, including the product's tests), in the vendored spec and the product source; do not document request options the product ignores.

### Known gaps ratchet

`examples/known-gaps.txt` lists one `<area>/<name>:<lang>` per line, sorted, for examples that do not yet have a Java file. `examples:coverage` fails both when an unlisted gap appears and when a listed gap has been filled, so the file only ever shrinks. Remove the line when you add the missing file.

### Ratchets

`examples/inline-language-tabs.txt` and `examples/inline-calls.txt` list no pages and stay that way: a new inline language tab or inline product call fails CI, so put the code in an example. Never use `node tools/examples/check-inline-code.mjs --write` to add entries.

`examples/known-gaps.txt` only shrinks. A check fails when something grows and also when a listed entry is no longer needed, so remove the line when you add the missing file. `examples/sdk-uncovered.txt` (public client methods not called in an example or shown in the SDK reference page) stays empty: a new public client method needs an example, a reference entry, or a line in `examples/sdk-exclusions.txt` with a reason. `docs/reference/sdk-methods.md`, `docs/reference/cli-reference.md` and `docs/core/playbook-evaluation.md` keep inline fences and are exempt from the inline checks. `examples/not-applicable.txt` lists `<example>:curl` or `<example>:python` with a reason where a language cannot do the step. `tools/examples/client-methods.json` is generated by `check_method_coverage.py --write-methods tools/examples/client-methods.json` and committed; a pytest fails when it differs from the installed client.

## User-facing SDK methods

A user-facing Python client method is a public method of `DocuDevsClient` (no leading underscore) that is not a transport helper. Each one should be covered by a guide or example. Methods that do not need coverage are listed in `examples/sdk-exclusions.txt`, one `<method>  # reason` per line; never add a method to `examples/sdk-uncovered.txt`. `python tools/examples/python/check_method_coverage.py examples docs/reference/sdk-methods.md examples/sdk-exclusions.txt examples/sdk-uncovered.txt` runs the check.

## SDK method index

`docs/reference/sdk-method-index.md` is generated from the installed Python client: run `npm run sdk:index` (and commit the result) whenever the pinned `docu-devs-api-client` version changes; `npm run sdk:index -- --check` and CI fail when the page is stale. Notes for methods with known limits live in `tools/examples/python/sdk-index-notes.txt` (`<method>  # note`); a note for a method that does not exist fails the generator. The index never counts as coverage: the method coverage check reads only `docs/reference/sdk-methods.md`.

## Running examples against a real organization

`npm run examples:live` runs the cURL, Python and CLI files of the examples against a real API. It is a manual check and never runs in CI; only its unit tests do. Use a test organization and its API key, never a production organization.

```bash
API_KEY=<test organization key> npm run examples:live
npm run examples:live -- --list
npm run examples:live -- --only cases --variant curl
```

- Flags: `--only <area>[/<name>]`, `--variant curl|python|cli` (default all three), `--list` (print the selection and run nothing). `API_KEY` is required; the script exits 2 without it.
- The Python and CLI variants need `tools/examples/python/.venv` (see Checks).
- Each file runs with `bash` (Python with the venv interpreter, the CLI with the venv `bin` first on `PATH`) in its own temporary directory holding the files from `tools/examples/live-fixtures/`, under a 10 minute timeout, with only `API_KEY`, `PATH`, `HOME` and `DOCUDEVS_API_URL` in its environment. Most examples hardcode `https://api.docudevs.ai`, so `DOCUDEVS_API_URL` only applies where an example reads it.
- What a run creates in the organization: jobs and batches, cases (documents and records datasets), saved configurations, uploaded templates and Excel templates, and a review profile on a configuration. Run order is alphabetical; an example that uses a configuration or template another example saves (`invoice-config`, `invoice`) fails on a first run when it comes first, so run again before treating it as a defect.
- Output goes to `live-run/<area>/<name>/<variant>.log` and the table to `live-run/report.md` (`pass`, `fail` with the exit code and last output line, `skipped` with the reason). The directory is gitignored. The key value is replaced with `***` in every log and on the console. The exit code is 1 when any row failed.
- `examples/live-skip.txt` lists `<area>/<name><TAB><reason>`; the runner never executes those. The reason is one of: needs a signed-in user; needs a dedicated deployment; needs a public callback URL; needs provider credentials; deletes or changes organization-wide settings; needs an existing resource id. A test fails for an unknown example, a missing reason or any other reason. A new example that deletes or changes organization-wide settings, or that starts from an id the example cannot create, gets a line there.
- A selected example that opens or uploads a file needs a file of that name in `tools/examples/live-fixtures/`; a test fails otherwise. Regenerate the fixtures with `node tools/examples/make-live-fixtures.mjs` and commit them. Names an example writes are detected automatically; list any other output name in `OUTPUT_NAMES` in `tools/examples/lib/live-run.mjs`.
