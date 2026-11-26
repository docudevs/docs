---
title: Contract Analysis
description: Review contracts clause by clause against a playbook, with a map of where each step is documented.
sidebar_position: 11
---

Contract analysis reads a contract against a **playbook** and reports one finding per clause: a verdict, the red flags that were triggered, and the passages of the contract the finding rests on. A playbook states what your side wants from a kind of contract. A run is immutable: it captures the contract, the playbook revision and the execution configuration, so later edits never change its result.

Contract analysis is an information-extraction and review aid. It does not provide legal advice, legal opinions, underwriting decisions, or a substitute for qualified counsel. Your organization remains responsible for reviewing the contracts and every finding before relying on them.

## Where to find what

| To do this | Read |
| --- | --- |
| Write or import a playbook, edit it, or build its first clauses from guideline documents | [Import and export a playbook](./contract-analysis-playbooks.md#import-and-export-a-playbook), [Edit a playbook](./contract-analysis-playbooks.md#edit-a-playbook), [Build clauses from guidelines](./contract-analysis-playbooks.md#build-clauses-from-guidelines) |
| Test a draft, attest the result and publish it | [Test and publish a playbook](./contract-analysis-playbooks.md#test-and-publish-a-playbook) |
| Score a playbook against gold answers and improve it | [Playbook evaluation](./playbook-evaluation.md) |
| Look up a field of the playbook JSON | [Playbook format](../reference/playbook-format.md) |
| Create a run, read its findings and review them | [Analyze a contract](./contract-analysis-playbooks.md#analyze-a-contract) |
| Analyze one document with the published playbook in a single call | [Analyze one document](#analyze-one-document) |
| Analyze a package of several documents | [Analyze a package of documents](#analyze-a-package-of-documents) |
| Record which guideline documents are policy, and read that record | [Source authority](#source-authority) |

A run needs a playbook in the current format (`profileVersion` `2026-11`). A run against a playbook in any other format is refused with `400` and the message "unsupported contract analysis profile version".

## Analyze one document

Use the operation endpoint when you want to analyze one completed document against the published playbook and only need the result. Submit a `contract-analysis` operation for the completed contract job, naming the playbook's case as `knowledgeBaseId`. The operation starts a run in `published` mode, the same run that `POST /contract-analysis/runs` creates for a single-document package, so the playbook must have a published revision; otherwise the request is refused with `400` ("published profile is unavailable").

<Example name="contract-analysis/analyze-contract" />

The response carries the `jobGuid` of the analysis job, not of the contract job. The cURL and Python examples wait on that job and read its result from `GET /job/result/{jobGuid}`, so they return the analysis they just started even when the contract has been analyzed before. The CLI example waits with `--wait`. The Java `submitAndWaitForContractAnalysis` submits and then polls `GET /operation/{jobGuid}/contract-analysis` for the contract job, with a default timeout of 180 seconds. That route answers with one `contract-analysis` operation of the contract job, picked from all of them without regard to which was submitted last, so after several analyses of the same contract it may return one other than the one just submitted. To read the analysis you just started, call `submitContractAnalysis` and wait on the returned job yourself.

Things to know about this route:

- The response has no run ID, and the review routes work on a run ID. To review findings, create the run with `POST /contract-analysis/runs` instead (see [Analyze a contract](./contract-analysis-playbooks.md#analyze-a-contract)).
- The only parameters it takes are `knowledgeBaseId` and an optional `idempotencyKey`. Without an `idempotencyKey` every submission starts a new run. Sending any other custom parameter, or an LLM type, returns `400` with the message "Runs do not accept custom model settings"; the model level comes from the playbook (see [Model level](./contract-analysis-playbooks.md#model-level)).
- To analyze several documents together, or to run a draft revision, create the run directly.

## Analyze a package of documents

A run can capture a package: the anchor document plus further completed contract jobs, such as schedules and amendments. Pass `packageMembers` (each with a `jobGuid` and a `role`), and optionally `expectedMissingDocuments` and `excludedDocuments`, when you create the run. Include the anchor in `packageMembers`. A package is limited to:

- 32 members, including the anchor, and 32 MiB of captured source material;
- 32 entries in `expectedMissingDocuments`;
- 256 entries in `excludedDocuments`.

`expectedMissingDocuments` and `excludedDocuments` describe documents that are not in the package; neither list uses up member slots. Duplicate or blank member ids, a missing anchor, a member that has not completed and a member you cannot access are rejected. The order of the members is the order in which passages reach the assessment (see [Clauses, red flags and verdicts](./contract-analysis-playbooks.md#clauses-red-flags-and-verdicts)).

<Example name="contract-analysis/create-run" />

The run is created in `published` mode here. Read its status and result, and review its findings, as described in [Analyze a contract](./contract-analysis-playbooks.md#analyze-a-contract).

## Read a playbook

`GET /knowledge-bases/{caseId}/profiles/contract-analysis` returns the playbook's draft (`view=draft`, the default) or its published revision (`view=published`). The response carries the lifecycle `status` (`GENERATING`, `DRAFT`, `PUBLISHED` or `FAILED`), `draftRevision`, `publishedRevision` and the playbook as `profile`. A case without a playbook returns `404`. For the canonical form that can be edited and imported again, use the export route (see [Import and export a playbook](./contract-analysis-playbooks.md#import-and-export-a-playbook)).

<Example name="contract-analysis/get-draft-profile" />

<Example name="contract-analysis/get-published-profile" />

A guideline document that the active published revision was built from cannot be deleted: `DELETE /cases/{caseId}/documents/{documentId}` returns `409` with the message "Document is referenced by the active published profile and cannot be deleted".

## Source authority

Source authority records the role of each guideline document: `policy`, `guidance`, `example` or `superseded`, or that the document is excluded (see [Suggested roles and sides](./contract-analysis-playbooks.md#suggested-roles-and-sides) for what the roles mean). Building clauses from guidelines needs it confirmed, and so does publishing a playbook whose items cite guideline passages (see [When source authority is required](./contract-analysis-playbooks.md#when-source-authority-is-required)). Decisions are append-only and revisioned: each write must supply the revision you last read, and a stale revision returns `409 Conflict`.

`GET /knowledge-bases/{caseId}/profiles/contract-analysis/source-authority` returns:

- `revision` and `decisions`: the current decision revision and the full decision history;
- `sources`: one entry for each guideline document that has finished processing, with `documentId`, `filename`, `contentFingerprint`, the recorded `sourceRole` and any suggestion;
- `confirmed` and `excludedDocumentIds`.

`POST /knowledge-bases/{caseId}/profiles/contract-analysis/source-authority/decisions` records a decision and returns the same shape. Send:

- `observedRevision`: the `revision` you read.
- `decisionType`: `source_authority`.
- `reason`: required rationale text.
- `content`: an object with a `documents` array that lists every guideline document of the case, one entry each. An entry carries the `id` and the `contentFingerprint` read from `sources`, and a `sourceRole`. It can also carry `supersedesDocumentIds` (ids of documents it replaces), `authorship`, `authorId` and `rationale`. Leave `sourceRole` out only for a document that is listed in `excludedDocumentIds`, an optional array of ids in `content` that takes documents out of scope.

A decision is refused with `422` (the problem is in `validation.errors`, code `SOURCE_SET_UNAVAILABLE`) when `documents` does not match the case's current documents, a document has neither a role nor an exclusion, a `contentFingerprint` is stale because the document changed, or no `policy` document remains that is included and not superseded. Both an organization API key and a signed-in user can record decisions; the decision stores who recorded it.

<Example name="contract-analysis/get-source-authority" />

<Example name="contract-analysis/record-source-authority-decision" />

The example gives every document the role `policy`; set the role of each document to what it is. There is no CLI command for source authority; use the SDK or the endpoints directly.

## Java SDK

`DocuDevsClient` (synchronous, returns `JsonNode`) and `DocuDevsAsyncClient` (the same method names, returns `CompletableFuture<JsonNode>`) have these contract-analysis methods in version 0.3.0:

| Method | Purpose |
| --- | --- |
| `getContractAnalysisProfile(caseId[, view])` | Read the draft (default) or published playbook. |
| `getContractAnalysisWorkflow(caseId)` | Read where the playbook is in its setup. |
| `generateContractAnalysisProfile(caseId, ourParty, counterparty)` | Start building clauses from the guideline documents. |
| `suggestContractAnalysisSourceRoles(caseId)` | Start the background read that suggests source roles and sides. |
| `getContractAnalysisSourceAuthority(caseId)` | Read the source-authority revision, decisions and sources. |
| `recordContractAnalysisSourceAuthorityDecision(caseId, observedRevision, decisionType, content, reason)` | Record a source-authority decision. |
| `updateContractAnalysisDraft(caseId, draftRevision, profile[, reason])` | Replace the draft at the observed revision. |
| `publishContractAnalysisProfile(caseId, draftRevision[, acknowledgedWarningCodes[, trialAttestationId]])` | Publish the draft; pass the attestation of a scored trial. |
| `getContractAnalysisTestSuite(caseId)` | Read the current test-suite revision. |
| `updateContractAnalysisTestSuite(caseId, revision, cases)` | Create the next suite revision. |
| `confirmContractAnalysisFindings(caseId, request)` | Record several run findings as approved expectations. |
| `proposeContractAnalysisFindingTestCase(caseId, proposal)` | Turn a run finding into an unapproved suite expectation. |
| `estimateContractAnalysisTrial(caseId, anchorJobGuid, suiteRevision, trialKind, suiteCaseId, packageId)` | Estimate a trial; the result carries the fingerprints a launch needs. |
| `createContractAnalysisTrial(caseId, anchorJobGuid, idempotencyKey, suiteRevision, trialKind, suiteCaseId, estimateFingerprint, executionFingerprint)` | Launch a scored trial from an estimate. |
| `getContractAnalysisTrial(caseId, trialId)` | Read trial status and its report. |
| `attestContractAnalysisTrial(caseId, trialId, reportId, reason)` | Record the attestation of a passing report. |
| `submitContractAnalysis(jobGuid, knowledgeBaseId)` | Submit the `contract-analysis` operation. |
| `submitAndWaitForContractAnalysis(jobGuid, knowledgeBaseId[, waitOptions])` | Submit and poll the operation to completion. |
| `createContractAnalysisRun(anchorJobGuid, knowledgeBaseId, idempotencyKey)` / `createContractAnalysisRun(anchorJobGuid, knowledgeBaseId, mode, draftRevision, idempotencyKey[, model, configurationFingerprint])` / `createContractAnalysisRun(ContractAnalysisRunRequest request)` | Create a run; the last overload accepts the full generated request model, including package members. |
| `getContractAnalysisRun(runId)` | Read run status. |
| `getContractAnalysisRunResult(runId)` | Read the immutable run result. |
| `getContractAnalysisRunReviews(runId)` | Read the current dispositions for a run. |
| `putContractAnalysisRunReview(runId, clauseId, review)` | Record a disposition for one clause. |

## CLI

The CLI covers the run lifecycle and most of the setup: run `docudevs knowledge-base contract-analysis --help` for every command, and see the [CLI reference](../reference/cli-reference.md#contract-analysis).

- `run-create` accepts optional JSON files through `--package-members`, `--expected-missing` and `--excluded-documents`; include the anchor in the member list.
- `publish` takes `--trial-attestation-id` for the attestation of a scored trial.
- Submit the one-document operation with `docudevs operations submit --type contract-analysis --parameter knowledgeBaseId=<id>`; add `--wait` to wait for completion. The `--llm-type` option is refused for this operation.
- There is no command for source authority.

## In the web app

- **Playbooks** in the sidebar opens `/app/playbooks`: the list of playbooks, a **New playbook** button, and a link to each playbook. A playbook opens at `/app/playbooks/{playbookId}` with the steps **Upload**, **Clauses**, **Test** and **Publish**: guideline documents and roles, the clause editor with **Import playbook (JSON)**, **Export** and **Save playbook**, the test suite with the **Scored trial**, and **Publish playbook**. The steps are described in [What is available](./contract-analysis-playbooks.md#what-is-available).
- On the **Jobs** page, a completed document job has an **Analyze contract** button. The dialog asks for a **Playbook**, and only playbooks with a published revision can be chosen. It also lets you select further documents for the analysis (up to 32 documents and 32 MiB of captured source) and name an **Expected but unavailable document**. **Analyze contract** creates a `published` run and opens its page.
- A run opens at `/app/contract-analysis/runs/{runId}`: the run ID, its status and mode, and the findings by clause. For a `draft_trial` run the page says that the result is for review and does not change the published playbook. Each finding offers **Agree**, **Correct** and **Dismiss**; **Correct** asks for a verdict, and **Correct** and **Dismiss** need a reason. **Save review** records the disposition.
