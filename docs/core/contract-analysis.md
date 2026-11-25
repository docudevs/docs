---
title: Contract Analysis
description: Build a reviewed, cited knowledge-base profile from playbook documents and run it against long contracts.
sidebar_position: 11
---

:::note
The clause-based playbook format described in [Contract-analysis playbooks](./contract-analysis-playbooks.md) is the current format: run results and reviews are organized by clause, each with a verdict. Parts of this page, including the result status vocabulary and the v1 and v2 result formats, describe earlier profile formats. See the [get contract analysis run result](/docs/openapi/get-contract-analysis-run-result) API reference for the current response schema.
:::

Contract analysis turns one or more playbook documents into a reusable, human-reviewed knowledge-base profile. The published profile supplies the facts, item types, relationships, criteria, and evidence requirements that a later contract-analysis run applies against a completed contract job.

This capability must be enabled for your organization before any of the endpoints below are reachable. When it is off, profile and analysis entry points return a `404` with a disabled-capability code.

Contract-analysis results are technical, evidence-linked extraction outcomes, not legal conclusions. This feature is an information-extraction and review aid; it does not provide legal advice, legal opinions, underwriting decisions, or a substitute for qualified counsel. Your organization remains responsible for reviewing source documents and every output before relying on it.

## Result status vocabulary

Contract analysis has two profile and result generations, distinguished by the profile's declared version:

- **v1 profiles** (`profileVersion` `2026-08`) produce a result where each criterion carries a `status` field with one of: `compliant`, `non_compliant`, `missing`, `needs_review`, or `not_applicable`.
- **v2 profiles** (`profileVersion` `2026-09`) produce a result where each criterion carries an `assessment` field with one of: `meets_requirement`, `deviates_from_requirement`, `not_found`, `needs_review`, or `not_applicable`.

In both generations, applicability is tri-state: a missing, conflicting, or unresolved operand resolves to `needs_review`, never an implicit false value, and required unresolved evidence also fails closed to `needs_review`. Every published criterion appears exactly once in a result; a document-scoped criterion has one target, an item-scoped criterion produces one target per matching item, and an applicable criterion with no matching item records `missing` (v1) or `not_found` (v2) rather than being omitted.

v2 also introduces scoped effects: a version-2 relationship can declare a bounded `scopedEffect` naming the source attribute, target attribute, matching scope attributes, an optional effective-date attribute, and a precedence method. The worker applies only that mapping from cited evidence, so freeform relationship text and synthesis guidance never execute as assignments. Results retain each item's original attributes, effective values, scope, derivation edge IDs, evidence, and precedence basis. The effective date is inclusive; a missing date or scope is reported as an uncertainty, and conflicting same-scope amendments require explicit supersession in the source manifest.

## Build and publish a profile

1. Create a case and upload each playbook to `/cases/{caseId}/documents`. Include every source that should participate in the profile; profile generation captures the eligible, completed OCR documents as one source set.
2. Confirm the role of each source document (see [Source authority](#source-authority)), then start compilation once uploads finish processing, naming the two sides the playbook is written for with `ourParty` and `counterparty`. Generation is asynchronous and always creates a draft — it never publishes automatically.
3. Monitor the returned job with `/job/status/{guid}`. Profile status moves through `GENERATING`, `DRAFT`, `PUBLISHED`, or `FAILED`.
4. Read the draft to review facts, item types, relationship definitions, criteria, citations, source coverage, and conflicts.
5. Edit the draft, passing the last observed `draftRevision`. A stale revision returns `409 Conflict`. Generated and human-edited elements need resolved playbook citations; human-authored uncited elements are warnings only.
6. Resolve structural errors and either fix or explicitly acknowledge permitted warnings, then publish with the observed `draftRevision` and any acknowledged warning codes. Publication validates and atomically cuts over the active revision; a failed publication leaves the previous publication active.
7. Read the active profile once published.

<Example name="contract-analysis/generate-profile" />

<Example name="contract-analysis/get-draft-profile" />

<Example name="contract-analysis/update-draft" />

<Example name="contract-analysis/publish-profile" />

<Example name="contract-analysis/get-published-profile" />

The active publication pins its source document IDs and fingerprint. New case uploads are uncompiled until a later regeneration, and a source used by the active publication cannot be deleted. Re-generation creates a new draft while the current publication remains available.

### Regenerating a v2 profile

For a v2 profile, regeneration records the draft revision it started from and stores the generated profile as a proposal — it does not replace the reviewed draft. Read the three-way comparison to see the prior generated value, reviewed value, proposed value, source continuity, and any deletion tombstone. Expert decisions use stable `elementLineageId` values and record an action (`edit`, `add`, `reject`, or `delete`), a reason, and the draft revision. A source change reopens dependent decisions for review; uncertain matches do not merge automatically.

Apply an explicit decision for every added, changed, removed, or conflicting criterion. The request pins `generationJobGuid`, `expectedDraftRevision`, and `expectedDraftFingerprint`, and maps each lineage ID to `accept_proposed`, `keep_current`, or `reject_proposal`. A stale draft or unresolved element decision returns `409`; reload the comparison and reconcile against the latest draft before retrying. Rejected proposals retain lineage tombstones so a later generation cannot silently reintroduce them. Successful application creates a new draft revision and reports the suite, trials, and attestations whose readiness identities are now stale — the suite must be run and reviewed again before publication.

<Example name="contract-analysis/compare-generation" />

<Example name="contract-analysis/apply-generation" />

## Source authority

Before a v2 profile can publish, every source document needs a confirmed role or an explicit exclusion. Source-authority decisions are append-only and revisioned: each write must supply the last observed revision, and a stale revision returns `409 Conflict`.

`GET /knowledge-bases/{caseId}/profiles/contract-analysis/source-authority` returns `{revision, decisions}`, the current decision revision and the full decision history.

`POST /knowledge-bases/{caseId}/profiles/contract-analysis/source-authority/decisions` records one decision. The request body is:

- `observedRevision` — the last revision this decision was based on.
- `decisionType` — a caller-defined string describing the kind of decision. The reference web app records document-role classifications under `source_authority`.
- `content` — a JSON object; its shape depends on `decisionType`. For document-role classification, `content` carries a `documents` array, one entry per document: `{id, sourceRole, excluded, authorship, authorId, rationale, supersedesDocumentIds}`, where `sourceRole` is one of `policy`, `guidance`, `example`, or `superseded`, and `excluded` marks a document as out of scope for the profile.
- `reason` — required rationale text.
- `elementLineageId` (optional) — required alongside `elementAction` when the decision also resolves a generation-comparison lineage element.
- `elementAction` (optional) — one of `edit`, `add`, `reject`, `delete`.
- `tombstone` (optional, default `false`) — only valid when `elementAction` is `reject` or `delete`.
- `generationJobGuid` / `draftRevision` (optional) — pin the decision to a specific regeneration and draft snapshot.

Only `policy` and `guidance` sources can establish requirements; `example` sources cannot, and `superseded` sources are historical context only.

<Example name="contract-analysis/get-source-authority" />

<Example name="contract-analysis/record-source-authority-decision" />

There is no CLI command for source authority today; use the SDK or the endpoints directly.

## Try and approve a version-2 draft

Version-2 profiles require a scored, expert-attested example trial before publication. The workspace never runs examples automatically when a draft is edited.

Create a revisioned test suite that includes immutable package identities, scenario tags, and stable expectation target keys. An expectation is usable for a scored trial only after it is marked approved; updating approvals creates a new suite revision, so old reports and attestations do not silently carry forward.

Launch either an `exploratory` or a `scored` trial explicitly, supplying an `anchorJobGuid`, the suite revision, and an `idempotencyKey`. Estimate the trial first with the same inputs: the launch request carries the `estimateFingerprint` and the `executionFingerprint` (sent as `configurationFingerprint`) that the estimate returned. Exploratory runs are not correctness scores. Scored reports compare the completed run against the approved expectations deterministically; expectations stay on the evaluator side and are not included in model inputs. When multiple suite cases share the same captured anchor/package, include the exact `suiteCaseId` — legacy callers that omit it retain anchor-based selection. A reviewer can then attest a passing scored report.

Publish a v2 profile by including the returned attestation `attestationId` as `trialAttestationId` in the publish request, alongside the draft revision and any warning acknowledgments. The API verifies that the attestation still matches the current draft, suite, package snapshot, and execution configuration inside the publication transaction; a stale attestation is rejected without replacing the active publication. Version-1 publication requests keep their existing behavior and do not need an attestation.

<Example name="contract-analysis/get-test-suite" />

<Example name="contract-analysis/update-test-suite" />

<Example name="contract-analysis/propose-test-case" />

<Example name="contract-analysis/create-trial" />

<Example name="contract-analysis/get-trial" />

<Example name="contract-analysis/attest-trial" />

<Example name="contract-analysis/publish-with-attestation" />

When a draft declares scoped effects, publication also requires passing, attested examples tagged `scoped_effect_override` and `scoped_effect_unaffected_scope`. Each example expectation includes an `expectedEffectiveAttributes` object on an `item:<id>` target, and trial comparison checks those values against the returned effective variant in addition to the criterion assessment.

Proposing a test case is a draft suite edit only: it copies the captured package members and fingerprint into a new case in the next suite revision with `approved: false`. It does not directly update the profile, approve an assertion, start a trial, or publish a revision — an expert must explicitly approve the expectation, launch and inspect a new scored trial, attest that report, then publish using its attestation. If another reviewer changes the suite first, the proposal returns `409` with the suite revision conflict.

## Analyze a completed contract (v1)

Submit a first-class operation for a completed OCR/document-processing job and a published profile. The operation performs outline-aware map/reduce for long contracts, structural reduction, semantic relationship synthesis, deterministic applicability, and criterion evaluation before exposing one complete result — it never exposes a partial final result.

<Example name="contract-analysis/analyze-contract" />

The cURL and Python examples submit the operation and poll `/operation/{jobGuid}/contract-analysis` until `resultAvailable` is `true`; the CLI example waits with `--wait`. The Java `submitAndWaitForContractAnalysis` does this for you with a default timeout of 120 seconds; call `submitContractAnalysis` and poll yourself when you want to control retries.

## Contract-analysis runs (v2)

Runs are immutable, addressable records: a run captures its own package of source documents, mode, and configuration, and its result never changes on rerun. `mode` is `published` (use the active publication) or `draft_trial` (use a specific draft revision). Include the anchor document in `packageMembers`; captures are limited to 32 members and 32 MiB of captured sources. `expectedMissingDocuments` is separately limited to 32 entries and `excludedDocuments` to 256 entries — neither list consumes captured-member slots. Duplicate IDs, inaccessible members, and a missing anchor are rejected, and member order does not imply precedence.

<Example name="contract-analysis/create-run" />

<Example name="contract-analysis/get-run" />

<Example name="contract-analysis/get-run-result" />

<Example name="contract-analysis/list-run-reviews" />

<Example name="contract-analysis/review-finding" />

The review path names the clause being reviewed. `disposition` is one of `accepted`, `corrected`, `dismissed`, or `unresolved`; a corrected decision requires a `correctedVerdict` (`satisfactory`, `mixed`, `flagged`, `ai_unsure`, `not_found`, or `does_not_apply`), and corrected and dismissed decisions require a `reason`. A stale `expectedReviewRevision` returns `409`. The reviewer is recorded as the authenticated user id, or `api-key:<keyId>` for an organization API key. Accepting or correcting a finding never rewrites the captured machine result — each rerun has a new run ID and starts without a disposition. CSV exports and the run workspace keep the machine assessment, disposition, reviewer, and rationale in separate fields, and original evidence links open the captured source page and source element.

## Result format: v1 vs v2

Both generations expose `contractFacts`/model data, `analysisItems`, `relationships` (v1) or `effectiveVariants`/`effectUncertainties` (v2), `criteria`, and evidence references. The profile identity — knowledge-base ID, published or draft revision, and source fingerprint — is embedded in every result so a client can confirm which profile snapshot produced it. Citations use a stable source-reference shape: an ID, document ID, source-map path, source element IDs, page numbers, and an exact quote (`evidenceStatus` is `resolved` or `unresolved`); quotes are exact source text, not generated paraphrases.

v1 results are keyed by the criterion's `status` value described above. v2 results add `runId`, `mode`, `profileFingerprint`, `packageFingerprint`, and structured `coverage` reporting on included documents, expected/processed sections, gaps, and selection uncertainty. A trimmed, illustrative v2 result looks like:

```json
{
  "resultVersion": "2026-09",
  "type": "contract_analysis",
  "runId": "run-8f21",
  "mode": "published",
  "publishedRevision": 4,
  "profileFingerprint": "sha256:profile",
  "packageFingerprint": "sha256:package",
  "execution": {
    "engineVersion": "2026-09.1",
    "promptVersion": "criteria-v3",
    "model": "gpt-5"
  },
  "criteria": [
    {
      "criterionId": "hours-clause",
      "assessment": "deviates_from_requirement",
      "reasonCode": "explicit_deviation",
      "applicability": "applicable",
      "selectedItemIds": ["item-42"],
      "sourceRefs": [
        {
          "id": "ref-1",
          "documentId": "agreement.pdf",
          "quote": "Coverage hours shall not exceed 8 consecutive hours.",
          "pageNumbers": [4],
          "evidenceStatus": "resolved"
        }
      ]
    }
  ],
  "coverage": {
    "includedDocuments": ["agreement.pdf", "amendment.pdf"],
    "expectedSections": ["hours", "territory", "exclusions"],
    "processedSections": ["hours", "territory", "exclusions"],
    "gaps": [],
    "selectionUncertainty": []
  }
}
```

## Citations, evidence, conflicts, and revisions

Compilation preserves conflicts and ambiguous facts for review rather than silently choosing between contradictory playbook clauses — edit the draft to make precedence explicit, or acknowledge the warning when the product permits it. Draft writes use optimistic concurrency: v1 keeps one editable draft, one active publication, and a monotonic published revision, and each analysis job snapshots the active revision so later edits cannot change an in-flight result.

Finding dispositions (see [Contract-analysis runs](#contract-analysis-runs-v2)) are stored separately from the machine result, so a new run never inherits a disposition, even against the same package.

## Java SDK

`DocuDevsClient` (synchronous, returns `JsonNode`) and `DocuDevsAsyncClient` (same method names, returns `CompletableFuture<JsonNode>`) expose the full lifecycle. Every example above has a Java tab; the methods are:

| Method | Purpose |
| --- | --- |
| `getContractAnalysisProfile(caseId[, view])` | Read the draft (default) or published profile. |
| `generateContractAnalysisProfile(caseId, ourParty, counterparty)` | Start asynchronous profile compilation. |
| `getContractAnalysisSourceAuthority(caseId)` | Read source-authority decision history. |
| `recordContractAnalysisSourceAuthorityDecision(caseId, observedRevision, decisionType, content, reason)` | Record a source-authority decision. |
| `recordContractAnalysisElementDecision(caseId, observedRevision, decisionType, content, reason, elementLineageId, elementAction, tombstone, generationJobGuid, draftRevision)` | Record a decision that also resolves a generation-comparison lineage element. |
| `getContractAnalysisGenerationComparison(caseId, generationJobGuid)` | Read the three-way regeneration comparison. |
| `applyContractAnalysisGeneration(caseId, generationJobGuid, expectedDraftRevision, expectedDraftFingerprint, decisions)` | Apply per-lineage regeneration decisions. |
| `updateContractAnalysisDraft(caseId, draftRevision, profile)` | Replace the draft at the observed revision. |
| `publishContractAnalysisProfile(caseId, draftRevision[, acknowledgedWarningCodes[, trialAttestationId]])` | Publish the draft; pass a trial attestation for v2. |
| `getContractAnalysisTestSuite(caseId)` | Read the current test-suite revision. |
| `updateContractAnalysisTestSuite(caseId, revision, cases)` | Create the next suite revision. |
| `proposeContractAnalysisFindingTestCase(caseId, proposal)` | Turn a run finding into a draft suite case. |
| `estimateContractAnalysisTrial(caseId, anchorJobGuid, suiteRevision, trialKind, suiteCaseId, packageId)` | Estimate a trial; the result carries the fingerprints a launch needs. |
| `createContractAnalysisTrial(caseId, anchorJobGuid, idempotencyKey, suiteRevision, trialKind, suiteCaseId, estimateFingerprint, executionFingerprint)` | Launch an exploratory or scored trial from an estimate. |
| `getContractAnalysisTrial(caseId, trialId)` | Read trial status and its immutable report. |
| `attestContractAnalysisTrial(caseId, trialId, reportId, reason)` | Record expert attestation of a passing report. |
| `submitContractAnalysis(jobGuid, knowledgeBaseId)` | Submit the v1 analysis operation. |
| `submitAndWaitForContractAnalysis(jobGuid, knowledgeBaseId[, waitOptions])` | Submit and poll the v1 operation to completion. |
| `createContractAnalysisRun(anchorJobGuid, knowledgeBaseId, idempotencyKey)` / `createContractAnalysisRun(anchorJobGuid, knowledgeBaseId, mode, draftRevision, idempotencyKey[, model, configurationFingerprint])` / `createContractAnalysisRun(ContractAnalysisRunRequest request)` | Create a run; the last overload accepts the full generated request model, including package members. |
| `getContractAnalysisRun(runId)` | Read run status. |
| `getContractAnalysisRunResult(runId)` | Read the immutable run result. |
| `getContractAnalysisRunReviews(runId)` | Read current human dispositions for a run. |
| `putContractAnalysisRunReview(runId, clauseId, review)` | Append an expert disposition. |

## In the web app

- The profile workspace lives on the case detail page, under Contract analysis, at `/app/cases/{caseId}/contract-analysis`. It drives profile generation, draft editing, source-authority classification, test-suite management, and trial review for that case's knowledge base.
- Run review lives at `/app/contract-analysis/runs/{runId}`, showing the machine assessment and reviewer disposition side by side, with links to the original evidence.
- A completed job's own page shows a per-job contract-analysis results panel alongside its other outputs.

## CLI

Every example above has a CLI tab except the source-authority ones. The CLI mirrors most of the SDK lifecycle; run `docudevs knowledge-base contract-analysis --help` for every command.

`run-create` accepts optional JSON files through `--package-members`, `--expected-missing`, and `--excluded-documents`; include the anchor in the member list. Use `--trial-attestation-id` on `publish` when publishing a reviewed v2 profile. There is no CLI command for source-authority decisions — use the SDK or the endpoints directly. Submit the v1 operation with `docudevs operations submit --type contract-analysis`; add `--wait` to wait for completion, or read the result later with `docudevs operations result`.

## Availability

Contract-analysis generation and analysis must be enabled for your organization; when disabled, public profile and analysis entry points return a typed `404`. Knowledge-base search and inline evaluation (see [Knowledge Search](/docs/core/knowledge-search)) work when contract analysis is disabled.

## Related

- [Contract-analysis playbooks](./contract-analysis-playbooks.md): import a clause-based playbook and review contracts against it.
- [Playbook format](../reference/playbook-format.md): the playbook JSON format and its JSON Schema.
- [Playbook evaluation](./playbook-evaluation.md): test a playbook against gold answers and improve it.
