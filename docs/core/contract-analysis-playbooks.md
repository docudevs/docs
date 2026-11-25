---
sidebar_position: 6
title: Contract-analysis playbooks
description: Review long contracts clause by clause against a playbook you import, with findings that cite the contract.
---

# Contract-analysis playbooks

A **playbook** states, clause by clause, what your side wants from a kind of
contract: the wording you prefer, the wording you accept instead, the red flags
that make a clause unacceptable, and how to recognise the clause in a contract.
A contract-analysis run reads a contract against the playbook and reports one
finding per clause. The playbook is a case whose knowledge base has a
`contract-analysis` profile, so the API, SDKs and CLI keep the names case,
knowledge base and profile. To create a playbook through the API, call
`POST /knowledge-bases` with `"profileType": "contract-analysis"`; it starts
with an empty draft. In the app, playbooks are under **Playbooks**.

Contract analysis is an information-extraction and review aid. It does not
provide legal advice, legal opinions, underwriting decisions, or a substitute
for qualified counsel. Your organization remains responsible for reviewing the
contracts and every finding before relying on them.

## What is available

Playbooks come from import: you write or edit the playbook as JSON and import
it. The format is described in [Playbook format](../reference/playbook-format.md),
which also carries the JSON Schema. You can also build the first set of clauses
from uploaded guideline documents (see [Build clauses from guidelines](#build-clauses-from-guidelines)).
The app does not edit clauses; edit them through the API or CLI (see
[Edit a playbook](#edit-a-playbook)). You can test a draft against contracts with known answers and publish it through the API
(see [Test and publish a playbook](#test-and-publish-a-playbook)). Runs use the
draft playbook (`mode` set to `draft_trial`) until a revision is published. The
app shows the clauses read-only, imports and exports the playbook, and lists the
findings of a run.

Runs use the clause format (`profileVersion` `2026-11`). A run started against a
playbook stored in the earlier rule-based format (`profileVersion` `2026-09`) is
refused with `400` and the message "unsupported contract analysis profile
version", and the run page for an old result says "This run uses a format that is
no longer supported." To use such a playbook, create it again in the clause format.

## Import and export a playbook

1. Read the current draft revision with
   `GET /knowledge-bases/{caseId}/profiles/contract-analysis?view=draft`
   (`draftRevision`; a new playbook is at revision 0).
2. Import the playbook with
   `POST /knowledge-bases/{caseId}/profiles/contract-analysis/import`:

   ```json
   {"draftRevision": 0, "playbook": { "...": "see Playbook format" }}
   ```

   Import replaces the whole draft and saves it as the next draft revision.
   A stale `draftRevision` returns `409 Conflict`. The optional `reason` is
   recorded in the draft's change log (see [Say why you changed the
   draft](#say-why-you-changed-the-draft)).
3. A file with mistakes returns `422` with every problem and its JSON path, not
   only the first: a duplicate id, a `relatedClauses` entry that names no
   clause, a condition on a fact the playbook does not declare, an unknown
   enum value. The problems are in `validation.errors`, each with a `code`, a
   `path` and a `message`. Fix the file and import again.
4. Export the draft or the published revision with
   `GET /knowledge-bases/{caseId}/profiles/contract-analysis/export?view=draft`
   (or `published`). The export is the canonical form and can be edited and
   imported again.

<Example name="contract-analysis/import-playbook" />

The Python and cURL variants read the current draft revision first and print the
`422` problem list. The Java variant passes the revision you supply and does not
read the problem list. The CLI import without `--draft-revision` replaces the
current draft.

<Example name="contract-analysis/export-playbook" />

The Java client returns JSON trees; `DocuDevsAsyncClient` returns
`CompletableFuture<JsonNode>` with the same method names.

## Edit a playbook

An organization API key can read and change every playbook in its organization,
including playbooks that a user created. Editing is a loop of read, change,
save:

1. Read the draft with `GET .../profiles/contract-analysis?view=draft`: the
   response carries `draftRevision` and the playbook as `profile`. The export
   (`GET .../export?view=draft`) returns the same playbook in canonical form.
2. Change the JSON: add, edit or remove clauses, red flags, risk factors,
   alternatives and identification hints.
3. Save it with `PUT /knowledge-bases/{caseId}/profiles/contract-analysis/draft`,
   sending the `draftRevision` you read and the changed playbook as `profile`.
   The response carries the next `draftRevision`.

```json
{"draftRevision": 3, "profile": { "...": "the edited playbook" }}
```

<Example name="contract-analysis/update-draft" />

The example saves with a `reason`, which is optional (see [Say why you changed
the draft](#say-why-you-changed-the-draft)).

Two editors can save from the same `draftRevision`. The first save wins; the
second gets `409 Conflict` and nothing is overwritten. Read the draft again, apply your
change to the new draft and save with the new revision. A playbook with mistakes
returns `422` with every problem and its JSON path.

Authorship is set by the server, so any `authorship` value you send is ignored.
A new fact, clause or line item (alternative, red flag, risk factor,
identification hint) is stored as `human_authored`. An item that was `generated`
and that you change becomes `human_edited`; other items keep their stored value.
An item counts as changed when anything but its `authorship` differs, so editing
a red flag never changes the authorship of its clause.

Deleting a clause or red flag that test expectations name is allowed. The save
succeeds, and the readiness check then reports `STALE_EXPECTATION` until you
update the expectations.

### Say why you changed the draft

The draft save takes an optional `reason`: a note of why the draft changed, at
most 500 characters (emoji count as one character each). Every successful save
is recorded in the draft's change log with the reason and who saved it: a user's
id, or `api-key:<id>` for an organization API key. Imports, generated builds and
applied generation proposals are recorded too; an import records the reason you
send with it, if any, and a build records `Built from guidelines`.

```json
{"draftRevision": 3, "profile": { "...": "the edited playbook" }, "reason": "Tightened the notice period"}
```

A reason longer than 500 characters returns `400` with the code
`REASON_TOO_LONG`, and nothing is saved. A `409` writes no change-log row.

Read the change log, newest first, with `GET .../profiles/contract-analysis/draft/revisions`.
Each entry has `draftRevision`, `actorId`, `reason` and `createdAt`. To read the
playbook saved at one revision, call `GET .../profiles/contract-analysis/draft/revisions/{revision}`;
its `profile` field holds that playbook, and an unknown revision returns `404`.
A case from another organization returns `404` on both routes.

<Example name="contract-analysis/list-draft-revisions" />

## Clauses, red flags and verdicts

Each clause carries a requirement level, optional conditions on facts, wording
and alternatives, red flags and identification hints. See
[Playbook format](../reference/playbook-format.md) for every field. A clause
in the example below is a generic "Limitation of liability" clause with the
perspective "the customer" and counterparty "the supplier":

```json
{
  "id": "limitation_of_liability",
  "name": "Limitation of liability",
  "requirementLevel": "mandatory",
  "redFlags": [
    {"id": "cap_one_sided", "flag": "Only the supplier's liability is capped", "severity": "high", "triggeredBy": "wording"}
  ]
}
```

A run reads the contract from your side's point of view. Facts that clause
conditions depend on are read from the contract. A clause whose condition is
false is **Does not apply**. If a fact cannot be read, the clause is **AI
unsure** with the reason `applicability_unknown`, never **Does not apply**.
Each clause is judged as a whole across every passage found for it, in package
order when the contract is a package (main document, schedules, amendments),
with later amendments changing earlier wording. The judge reads the whole
outline section around each quote, and it also receives the contract facts.

The verdict is computed from the assessment, not chosen by the model:

| Verdict | When |
| --- | --- |
| `satisfactory` | No red flag is triggered and the wording matches the preferred wording, the market standard, an acceptable alternative, or no red flag applies. |
| `mixed` | A `medium` or `low` red flag is triggered, or the wording matches an alternative that is acceptable with modifications. |
| `flagged` | A `critical` or `high` red flag is triggered, the wording matches a problematic alternative, or a `mandatory` or `expected` clause is missing (reason `required_clause_missing`; such a clause is never `not_found`). |
| `not_found` | The clause is absent, the absence was checked, and its requirement level is `preferred`, `optional` or `none` (reason `clause_absent`). |
| `ai_unsure` | Applicability cannot be decided (`applicability_unknown`), an absence check was inconclusive (`absence_unresolved`), the assessment of a found clause failed (`assessment_unavailable`), a red flag is unsure and nothing makes the clause `flagged` (`red_flag_unsure`), or the analysis stopped for budget before finishing the clause (`budget_exhausted`). |
| `does_not_apply` | The clause's condition is false. |

When several rows apply, the first of these wins: the analysis stopped for
budget (`ai_unsure`), the condition is false (`does_not_apply`), applicability is
unknown (`ai_unsure`), the clause was not found (`ai_unsure`, `flagged` or
`not_found` as above), the assessment failed (`ai_unsure`), a blocking red flag or
problematic wording (`flagged`), an unsure red flag (`ai_unsure`), a medium or low
red flag or wording that needs changes (`mixed`), and otherwise `satisfactory`.

Risk factors that hold raise or lower a red flag's severity by one level, and
the finding says why. A red flag the model reports as triggered but whose
quoted text is not in the contract becomes `unsure`, and the rejection is
recorded in the run diagnostics.

## Analyze a contract

Create an immutable run for a completed OCR/document-processing job and the
playbook's knowledge base:

`POST /contract-analysis/runs` takes the `anchorJobGuid`, the `knowledgeBaseId`,
a `mode` and an `idempotencyKey`. The mode is `draft_trial`, which runs the draft
at the `draftRevision` you send, or `published`.

<Example name="contract-analysis/create-draft-run" />

To analyze a package of several documents against the published playbook, see
[Analyze a contract](./contract-analysis.md#contract-analysis-runs-v2).

The response includes a `runId`. Use `GET /contract-analysis/runs/{runId}` for
status and `GET /contract-analysis/runs/{runId}/result` for the result.

<Example name="contract-analysis/get-run" />

<Example name="contract-analysis/get-run-result" />

The run captures the selected package, the playbook revision and the execution
configuration, so later edits cannot change it. Keep the run ID with its result
and any review record.

Run creation returns HTTP 409 for a run identity or execution-configuration
conflict and HTTP 503 when admitting the run timed out. A `draft_trial` run whose
`draftRevision` is not the current draft revision returns `400` ("draft profile is
unavailable"). Retry a 503 with the same
idempotency key to recover the recorded run
identity and status; replay does not schedule another attempt. To start a new
attempt after an `ERROR`, use a new idempotency key.

A run can capture a package of up to 32 explicitly selected documents
(including the anchor) and 32 MiB of source material. Pass `packageMembers`,
`expectedMissingDocuments` and `excludedDocuments` to describe it; member order
is the order passages reach the assessment. Duplicate ids, inaccessible members
and a missing anchor are rejected.

### Findings

The result has one entry per clause:

- `clauseId`, `verdict`, a short `reason` code (for example `meets_playbook`,
  `red_flag`, `required_clause_missing` or `clause_absent`), and an
  `explanation` in plain language from your side's point of view;
- `redFlags`: each with `redFlagId`, `outcome` (`triggered`, `not_triggered`
  or `unsure`), `effectiveSeverity` and source references;
- `matchedWording`: `preferred`, `marketStandard`, an alternative id, or `none`;
- `satisfactoryAspects`: one line per aspect that is fine, whatever the verdict;
- `passages`: every contract passage the finding rests on, each resolved to a
  document, page, character span and outline node (clause number and heading),
  with the filename of the document it comes from;

The result also carries `facts`, the facts used with their values and sources.
Coverage and diagnostics are reported alongside the clauses.

### Review a finding

Finding dispositions are stored separately from the machine result.
`GET /contract-analysis/runs/{runId}/reviews` returns the current review for
each reviewed clause.
`PUT /contract-analysis/runs/{runId}/reviews/{clauseId}` records one. Send:

- `resultFingerprint`, `disposition` (`accepted`, `corrected`, `dismissed` or
  `unresolved`), `expectedReviewRevision` and `idempotencyKey`;
- for `corrected`: `correctedVerdict` (`satisfactory`, `mixed`, `flagged`,
  `ai_unsure`, `not_found` or `does_not_apply`), required, and optionally
  `correctedRedFlags`, a map of red flag id to `triggered` or `not_triggered`;
- optional `evidence` and `reason`. Corrected and dismissed decisions require a
  reason.

`correctedVerdict` and `correctedRedFlags` are rejected with any other
disposition (`400 INVALID_CORRECTION`), and a red flag the clause does not have
in the run's playbook returns `400 UNKNOWN_RED_FLAG`. A red flag outcome other
than `triggered` or `not_triggered` returns `400 INVALID_RED_FLAG_OUTCOME`. A
stale revision returns
`409`. The response carries `clauseId`, `correctedVerdict`,
`correctedRedFlags`, `actor` and the new `reviewRevision`.

The actor is the authenticated user id, or `api-key:<keyId>` for an
organization API key. Reviewing never rewrites the captured result, and a rerun
starts without a disposition.

<Example name="contract-analysis/review-finding" />

The example corrects the verdict of one clause. A correction can also carry
`correctedRedFlags`, a map of red flag id to `triggered` or `not_triggered`.

<Example name="contract-analysis/list-run-reviews" />

## Review findings into test cases

A reviewed run can feed the test suite directly. Both routes work from a run
and its `resultFingerprint`, and both are keyed by clause: a test case holds one
expectation per clause, and a new one for the same clause replaces the old one.
Both fail with a clause error when the clause or red flag is gone from the run's
playbook.

**Confirm** records several expectations at once as approved. Send to
`POST /knowledge-bases/{caseId}/profiles/contract-analysis/test-suite/confirmations`:

<Example name="contract-analysis/confirm-findings" />

`expectedRedFlags` defaults to empty and `importance` to `mandatory`. A clause
the run does not have returns `400 UNKNOWN_CLAUSE`; the same clause twice in one
request returns `400 DUPLICATE_EXPECTATION`. The suite is unchanged on error.

**Propose** adds one unapproved expectation to a new suite revision, for
someone to approve later. Send to `.../test-suite/proposals` the `runId`,
`resultFingerprint`, `clauseId`, `name`, `expectedVerdict`, optional
`expectedRedFlags` and `notes`, a `reason`, `expectedSuiteRevision` and an
`idempotencyKey`.

<Example name="contract-analysis/propose-test-case" />

A stale `expectedSuiteRevision` returns `409`; read the suite again and retry. Every
expectation stores `recordedBy`, set by the server: the user id, or
`api-key:<keyId>` for an organization API key. It changes only when the
expectation is added or edited.

## Test contracts

A test contract is a contract you use to see what a draft's clauses do to a real
document, without making it a guideline source. Upload it with
`datasetType=test_contract` (and optionally `testContractRetention`):

<Example name="contract-analysis/upload-test-contract" />

Or mark a document that is already in the case. The document must have finished
processing:

<Example name="contract-analysis/mark-test-contract" />

- `datasetType` accepts only `test_contract`. Marking is one-way: no request
  removes the marker. To use a test contract as a guideline source, upload it
  again without the marker.
- `testContractRetention` is `kept` or `temporary` (default `temporary`). It
  records whether the contract should stay in the case for future tests. It
  is accepted only for a test contract, and changing it on an existing test
  contract does not re-index or otherwise change the document.
- Marking a document that is an active guideline source of the current draft
  fails with `409 Conflict`. A document is active when the draft's source set
  lists it and neither the current source authority nor the draft excludes it.
  Exclude the document from the guideline sources first, then mark it.
- A document can be marked only after its processing completes; until then
  the request fails with `409 Conflict`. Calendar documents cannot be marked
  (`400 Bad Request`).
- Marking removes the document's existing search-index chunks. The document,
  its stored file and its processing results are kept.
- A test contract is never a guideline source and is never indexed for case
  search. It does not take part in building clauses, so a test contract that is
  still processing does not block a build.
- A temporary test contract is deleted 24 hours after it was uploaded, unless
  a test-suite example still uses it. The cleanup runs about hourly. A kept
  test contract is never deleted by the cleanup; delete it with
  `DELETE /cases/{caseId}/documents/{documentId}` when you no longer need it.
- The app hides temporary test contracts from the case document list and the
  Test step picker. `GET /cases/{caseId}/documents` still returns them, and
  they count toward the case's document limit.

## Test and publish a playbook

Publishing is gated by a test. You describe what the draft should conclude for
contracts you already know, a scored trial runs the draft over them, a user or an API
key attests the result, and the draft can then be published. Every step works with
an organization API key alone. All paths are under
`/knowledge-bases/{caseId}/profiles/contract-analysis`.

To build these expectations from a spreadsheet of gold answers, and to tune the
playbook against them, see [Playbook evaluation](./playbook-evaluation.md).

### 1. Prepare a test package

Pick a completed contract job and capture it with `POST /test-packages`. Send
`anchorJobGuid`, an `idempotencyKey`, optional `packageMembers` (`jobGuid` and
`role`) and an optional `context` with `asOfDate`. The answer carries
`packageId` and `fingerprint`. Preparing a package analyzes nothing and is not
charged.

<Example name="contract-analysis/prepare-test-package" />

### 2. Write the expectations

Replace the test suite with `PUT /test-suite`, sending the `revision` you last
read and the `cases`. Each case names the package (`anchorJobGuid`, `packageId`,
`packageFingerprint`, `packageMembers`, `context`) and lists `expectations`:

```json
{
  "id": "renewal-deviation",
  "clauseId": "term",
  "expectedVerdict": "flagged",
  "expectedRedFlags": {"auto-renewal": "triggered"},
  "importance": "mandatory",
  "approved": false,
  "notes": "The contract renews without notice."
}
```

`expectedVerdict` is one of `satisfactory`, `mixed`, `flagged`, `not_found`,
`does_not_apply` or `ai_unsure`. `expectedRedFlags` maps red flag ids of that
clause to `triggered` or `not_triggered`; it may be empty and names only the
red flags you care about. `importance` is `mandatory` (the default) or
`advisory`. The request is refused with `UNKNOWN_CLAUSE`, `UNKNOWN_RED_FLAG`
or `INVALID_VERDICT` when an id or verdict does not exist.

Read the suite and its revision with `GET /test-suite`, edit the `cases`, and
save them again:

<Example name="contract-analysis/get-test-suite" />

<Example name="contract-analysis/update-test-suite" />

### 3. Approve

A person who has checked an expectation saves the suite again with `approved`
set to `true`. Only approved expectations count toward coverage.

### 4. Estimate and run a scored trial

`POST /trials/estimate` with `anchorJobGuid`, `suiteRevision` and `trialKind`
set to `scored` returns an `estimateFingerprint` and an `executionFingerprint`.
The estimate counts clauses. An optional `suiteCaseId` narrows the trial to one
case of the suite.

`POST /trials` with the same fields, an `idempotencyKey`, the
`estimateFingerprint` and the `configurationFingerprint` (the
`executionFingerprint` from the estimate) starts the trial.

<Example name="contract-analysis/create-trial" />

Read it with `GET /trials/{trialId}` and repeat until its status is final. An expectation
passes when the finding's verdict equals the expected verdict and every named
red flag has the expected outcome; otherwise the assertion carries one reason:
`clause_not_found`, `verdict_mismatch`, `red_flag_not_found` or
`red_flag_mismatch`. A trial compares verdicts exactly, unless the expectation
lists `acceptedVerdicts` (the expected verdict first). If the run for any one
contract fails, the whole trial fails with a reason and cannot be attested.

<Example name="contract-analysis/get-trial" />

### 5. Attest

`POST /trials/{trialId}/attest` with the report's `reportId` and an optional
`reason` records that a user or an API key accepts the report. The attestation
stores who attested: the user id, or `api-key:<keyId>` for an API key.

<Example name="contract-analysis/attest-trial" />

### 6. Check readiness

`GET /readiness` lists everything that stops publishing as blocker codes, such
as `ATTESTATION_STALE`, `NO_CONFIRMED_EXAMPLES` and `SCENARIO_COVERAGE_MISSING`.
Publishing is possible only when the list is empty. A blocker appears when the
draft changed after the trial, when an expectation names a clause or red flag
that was removed from the draft (a stale expectation), when a scenario is not
covered, or when source authority is required and not confirmed.

<Example name="contract-analysis/check-readiness" />

### 7. Publish

`POST /publish` with the observed `draftRevision` and the `trialAttestationId`.
The published revision records who published it.

<Example name="contract-analysis/publish-with-attestation" />

### The three required scenarios

Scenarios are worked out from your expectations, not typed in. A playbook needs
all three, and each needs at least one expectation that is approved, mandatory
and passed in the attested trial. Otherwise readiness reports
`SCENARIO_COVERAGE_MISSING` with the names of the missing scenarios.

- `meets_playbook`: the expected verdict is `satisfactory`.
- `deviation`: the expected verdict is `mixed` or `flagged`, and at least one
  named red flag is expected `triggered`.
- `missing_required_clause`: the expected verdict is `flagged` or `not_found`
  for a clause whose requirement level is `mandatory` or `expected`, with no
  expected red flags. The analysis returns `flagged` for such a missing clause,
  so expect `flagged`; an expectation of `not_found` passes only when it lists
  `acceptedVerdicts` with `flagged` as well.

### When source authority is required

Source authority is needed only when at least one item of the playbook cites a
source passage, as in a playbook built from guidelines. A playbook you wrote or
imported without source references publishes without it. When it is needed, a
policy section counts as covered if any item cites it, and only the sections
that no item cites need an explicit coverage disposition. Confirm authority
before running trials, because changing it changes the draft and makes earlier
attestations stale.

## Build clauses from guidelines

A playbook with no clauses can be built from the guideline documents in its
knowledge base. The build needs to know which side the playbook is written for,
so every build request names two sides:

- `ourParty`: the side the playbook protects and is written for.
- `counterparty`: the other side of the agreements.

Both are required, must not be empty and are at most 8192 characters. Send them as
the `perspective` of `POST /knowledge-bases/{caseId}/profiles/contract-analysis/generate`:

<Example name="contract-analysis/generate-profile" />

A build needs the role of each guideline document confirmed first: policy,
guidance, example or superseded, or excluded, with at least one policy document that is not excluded.
A signed-in user confirms the roles on the playbook page. An organization API
key can confirm them too, with `POST .../source-authority/decisions` (see [Source authority](./contract-analysis.md#source-authority)), and can then build;
the decision is recorded as that key. A build without a confirmation fails with
`400` and the message "Source authority is incomplete: SOURCE_AUTHORITY_UNCONFIRMED".
Changing the documents makes the confirmation stale.

The request starts a background job and returns its job ID. When the job
finishes, the draft holds the generated clauses.

The build is available only for the first build. If the playbook already has
clauses, the request fails with `400` and the message "Rebuilding a playbook
that already has clauses is not available yet." Change an existing playbook by
exporting it, editing the JSON and importing it again.

### Suggested roles and sides

Choosing a role for each document is easier with a suggestion. The role describes the
weight of the whole document:

| Role | Meaning |
| --- | --- |
| `policy` | Our rules: what we accept and what needs approval |
| `guidance` | Advice or background, not binding |
| `example` | A sample contract or clause wording |
| `superseded` | An older version we no longer follow |

`POST /knowledge-bases/{caseId}/profiles/contract-analysis/source-authority/suggestions`
starts a background read of the first pages of each guideline document and returns
`202` with a job ID. An organization API key can call it. When the job finishes, `GET .../source-authority` lists for each unclassified source a
`suggestedRole` and a one-sentence `suggestionReason`, and one `suggestedPerspective`
with `ourParty`, `counterparty` and a `reason` read from the policy and guidance
documents. A document that is clearly two documents, such as rules with a sample
contract appended, gets a reason that says so; splitting it stays manual. Each source
also carries `suggestedFacts`, described under [Suggested versions and scope](#suggested-versions-and-scope).

A suggestion never counts as a role. Confirmation, readiness, authority, the build and
analysis use only the roles you record and the sides you send with the build request.
Record a suggestion by accepting it on the playbook page, or with a source-authority
decision.

<Example name="contract-analysis/suggest-source-roles" />

<Example name="contract-analysis/get-source-authority" />

### Suggested versions and scope

For each guideline document, `suggestedFacts` holds what the document says about itself,
in its own words and with absent fields omitted: `title`, `version`, `effectiveFrom`,
`replaces` (the `title` and `version` of an older document it names) and `scope` (the
`contractTypes`, `businessLines` and `jurisdictions` it applies to, and `from`, the date
from which it applies). They are read from the document and never used until you accept
them.

**Version and lineage.** When two guideline documents share a title, or one says it
replaces the other, the older one (lower version, else earlier `effectiveFrom`) is
suggested as `superseded` with the reason "Replaced by \<newer title> \<version>", but only
while it is unclassified. The source-authority response also lists
`suggestedPrecedence` entries with `winnerDocumentId`, `loserDocumentId`, `scope` and a
`summary` such as "4.2 wins over 4.1". Accept one by sending it in the `precedence` list
of an ordinary source-authority decision as `{"winner": ..., "loser": ..., "scope": ...}`,
together with the rules already recorded; the playbook page does this when you press
Accept and then confirm the roles.

**Scope.** The build request accepts an optional `scope` with `contractTypes`,
`businessLines`, `jurisdictions` and `from`, which is stored on the playbook as `scope`.
It can also be set on a draft through the draft save. When a playbook has a scope, each
analysis first checks once whether the contract is within it, reading the contract's
opening pages. Only the contract type can put a contract outside the scope: a contract
that clearly matches none of the `contractTypes` phrases gets "does not apply" for every
clause, with the explanation "outside the playbook's scope: \<phrase>". A contract type
phrase such as "all X agreements" matches any X agreement, and a phrase that names an
organisation, department or team is ignored. `businessLines`, `jurisdictions` and `from`
never exclude a contract: when one clearly does not match, the analysis runs normally and
the run result carries a warning naming the phrase, shown as "This contract may be outside
the playbook's scope: \<phrase>. The clauses were still checked." An older contract than
`from` is still reviewed. The run result carries `scopeCheck` with `inScope`, the `phrase`
and `warnings`; an unclear answer leaves the analysis unchanged. The Test step shows
"This contract is outside the playbook's scope: \<phrase>" next to a test contract judged
outside the scope, and the scope warning next to one that is only flagged.

<Example name="contract-analysis/generate-profile-with-scope" />

The CLI `generate` command takes the two sides only; send a scope through the API or a client that takes one.

### Suggested test expectations from sample contracts

The same suggestion call reads sample contracts and test contracts. A source whose role is
`example` (confirmed, or suggested in the same run) gets `suggestedFacts` describing the
contract: `contractType`, `parties` (names as written), `governingLaw` and `date`. Every
test contract of the playbook, meaning a document marked as a test contract, gets the
same facts in `suggestedContractFacts` of the source-authority response, as entries with
`documentId`, `filename` and `suggestedFacts`.

When a draft playbook exists the facts also list `clausesPresent` and `clausesMissing`, as
clause ids of the draft. The whole contract is read in page-aligned windows (fixed-size
windows when the document has no page layout), each window by one model call that names
the draft clauses it contains. `clausesMissing` is the draft minus everything found, and
only when every window was read; each candidate is then re-checked against the whole
document with the absence check used by analysis runs, and only confirmed absences are
kept. Ids that are not in the draft are dropped. A change to the draft clauses or to a
contract starts a new suggestion run on the next request, and clause facts from an earlier
draft are not returned. The suggestion request is also accepted for a playbook with no
guideline documents (for example an imported one) as long as it has test contracts.

On the Test step, a test case whose contract lists a clause in `clausesMissing` with
requirement level `mandatory` or `expected` shows the suggestion "This contract seems to
lack \<clause name>". Add puts the expectation `{clauseId, expectedVerdict: "not_found",
importance: "mandatory"}` into the suite form, unapproved. The analysis returns
`flagged` for a missing mandatory or expected clause, so change the expected verdict
to `flagged` before you approve it; Dismiss hides the suggestion. Nothing is
written until you save the suite, which sets `recordedBy` as for any expectation, and a
clause that already has an expectation in that test case gets no suggestion. The Sources
step shows the facts of an example source under its suggestion as "Sample contract:
\<type>, \<parties>, governed by \<law>, \<date>".

## Model level

Contract analysis runs on the DocuDevs model level that the playbook chooses:
`mini`, `default` or `high`. A new playbook uses `default`. Organizations that
bring their own model (BYOM) get the model they configured for each level. Each
run result records the model that answered.
