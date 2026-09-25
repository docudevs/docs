---
title: Contract Analysis
description: Build a reviewed, cited knowledge-base profile from playbook documents and run it against long contracts.
sidebar_position: 11
---

import Tabs from '@theme/Tabs';
import TabItem from '@theme/TabItem';

Contract analysis turns one or more playbook documents into a reusable, human-reviewed knowledge-base profile. The published profile supplies the facts, item types, relationships, criteria, and evidence requirements that a later contract-analysis run applies against a completed contract job.

This capability must be enabled for your organization or deployment before any of the endpoints below are reachable. When it is off, profile and analysis entry points return a `404` with a disabled-capability code.

Contract-analysis results are technical, evidence-linked extraction outcomes, not legal conclusions. This feature is an information-extraction and review aid; it does not provide legal advice, legal opinions, underwriting decisions, or a substitute for qualified counsel. Your organization remains responsible for reviewing source documents and every output before relying on it.

## Result status vocabulary

Contract analysis has two profile and result generations, distinguished by the profile's declared version:

- **v1 profiles** (`profileVersion` `2026-08`) produce a result where each criterion carries a `status` field with one of: `compliant`, `non_compliant`, `missing`, `needs_review`, or `not_applicable`.
- **v2 profiles** (`profileVersion` `2026-09`) produce a result where each criterion carries an `assessment` field with one of: `meets_requirement`, `deviates_from_requirement`, `not_found`, `needs_review`, or `not_applicable`.

In both generations, applicability is tri-state: a missing, conflicting, or unresolved operand resolves to `needs_review`, never an implicit false value, and required unresolved evidence also fails closed to `needs_review`. Every published criterion appears exactly once in a result; a document-scoped criterion has one target, an item-scoped criterion produces one target per matching item, and an applicable criterion with no matching item records `missing` (v1) or `not_found` (v2) rather than being omitted.

v2 also introduces scoped effects: a version-2 relationship can declare a bounded `scopedEffect` naming the source attribute, target attribute, matching scope attributes, an optional effective-date attribute, and a precedence method. The worker applies only that mapping from cited evidence, so freeform relationship text and synthesis guidance never execute as assignments. Results retain each item's original attributes, effective values, scope, derivation edge IDs, evidence, and precedence basis. The effective date is inclusive; a missing date or scope is reported as an uncertainty, and conflicting same-scope amendments require explicit supersession in the source manifest.

## Build and publish a profile

1. Create a case and upload each playbook to `/cases/{caseId}/documents`. Include every source that should participate in the profile; profile generation captures the eligible, completed OCR documents as one source set.
2. Start compilation once uploads finish processing. Generation is asynchronous and always creates a draft — it never publishes automatically.
3. Monitor the returned job with `/job/status/{guid}`. Profile status moves through `GENERATING`, `DRAFT`, `PUBLISHED`, or `FAILED`.
4. Read the draft to review facts, item types, relationship definitions, criteria, citations, source coverage, and conflicts.
5. Edit the draft, passing the last observed `draftRevision`. A stale revision returns `409 Conflict`. Generated and human-edited elements need resolved playbook citations; human-authored uncited elements are warnings only.
6. Resolve structural errors and either fix or explicitly acknowledge permitted warnings, then publish with the observed `draftRevision` and any acknowledged warning codes. Publication validates and atomically cuts over the active revision; a failed publication leaves the previous publication active.
7. Read the active profile once published.

<Tabs groupId="lang">
<TabItem value="curl" label="cURL">

```sh
API_URL="https://api.docudevs.ai"
API_KEY="<your-api-key>"
CASE_ID=42

curl -X POST "$API_URL/knowledge-bases/$CASE_ID/profiles/contract-analysis/generate" \
  -H "Authorization: $API_KEY"

curl "$API_URL/knowledge-bases/$CASE_ID/profiles/contract-analysis?view=draft" \
  -H "Authorization: $API_KEY"

curl -X PUT "$API_URL/knowledge-bases/$CASE_ID/profiles/contract-analysis/draft" \
  -H "Authorization: $API_KEY" \
  -H "Content-Type: application/json" \
  -d '{"draftRevision": 1, "profile": { "...": "edited profile JSON" }}'

curl -X POST "$API_URL/knowledge-bases/$CASE_ID/profiles/contract-analysis/publish" \
  -H "Authorization: $API_KEY" \
  -H "Content-Type: application/json" \
  -d '{"draftRevision": 2, "acknowledgedWarningCodes": ["UNCITED_HUMAN_AUTHORED"]}'

curl "$API_URL/knowledge-bases/$CASE_ID/profiles/contract-analysis?view=published" \
  -H "Authorization: $API_KEY"
```

</TabItem>
<TabItem value="python" label="Python SDK">

```python
import os
from docudevs.docudevs_client import DocuDevsClient

client = DocuDevsClient(token=os.getenv("API_KEY"))
CASE_ID = 42

generation = await client.generate_contract_analysis_profile(CASE_ID)

draft_response = await client.get_contract_analysis_profile(CASE_ID, view="draft")
draft = draft_response.parsed

updated = await client.update_contract_analysis_draft(
    CASE_ID,
    draft_revision=draft["draftRevision"],
    profile=draft["profile"],
)

published = await client.publish_contract_analysis_profile(
    CASE_ID,
    draft_revision=updated.parsed["draftRevision"],
    acknowledged_warning_codes=["UNCITED_HUMAN_AUTHORED"],
)

active = await client.get_contract_analysis_profile(CASE_ID, view="published")
```

</TabItem>
</Tabs>

The active publication pins its source document IDs and fingerprint. New case uploads are uncompiled until a later regeneration, and a source used by the active publication cannot be deleted. Re-generation creates a new draft while the current publication remains available.

### Regenerating a v2 profile

For a v2 profile, regeneration records the draft revision it started from and stores the generated profile as a proposal — it does not replace the reviewed draft. Read the three-way comparison to see the prior generated value, reviewed value, proposed value, source continuity, and any deletion tombstone. Expert decisions use stable `elementLineageId` values and record an action (`edit`, `add`, `reject`, or `delete`), a reason, and the draft revision. A source change reopens dependent decisions for review; uncertain matches do not merge automatically.

Apply an explicit decision for every added, changed, removed, or conflicting criterion. The request pins `generationJobGuid`, `expectedDraftRevision`, and `expectedDraftFingerprint`, and maps each lineage ID to `accept_proposed`, `keep_current`, or `reject_proposal`. A stale draft or unresolved element decision returns `409`; reload the comparison and reconcile against the latest draft before retrying. Rejected proposals retain lineage tombstones so a later generation cannot silently reintroduce them. Successful application creates a new draft revision and reports the suite, trials, and attestations whose readiness identities are now stale — the suite must be run and reviewed again before publication.

<Tabs groupId="lang">
<TabItem value="curl" label="cURL">

```sh
curl "$API_URL/knowledge-bases/$CASE_ID/profiles/contract-analysis/generations/$GENERATION_JOB_GUID/comparison" \
  -H "Authorization: $API_KEY"

curl -X POST "$API_URL/knowledge-bases/$CASE_ID/profiles/contract-analysis/generations/$GENERATION_JOB_GUID/apply" \
  -H "Authorization: $API_KEY" \
  -H "Content-Type: application/json" \
  -d '{
    "generationJobGuid": "'"$GENERATION_JOB_GUID"'",
    "expectedDraftRevision": 5,
    "expectedDraftFingerprint": "sha256:draft",
    "decisions": {"hours": {"action": "accept_proposed"}}
  }'
```

</TabItem>
<TabItem value="python" label="Python SDK">

```python
comparison = await client.get_contract_analysis_generation_comparison(
    CASE_ID, generation_job_guid
)

applied = await client.apply_contract_analysis_generation(
    CASE_ID,
    generation_job_guid,
    expected_draft_revision=5,
    expected_draft_fingerprint="sha256:draft",
    decisions={"hours": {"action": "accept_proposed"}},
)
```

</TabItem>
</Tabs>

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

<Tabs groupId="lang">
<TabItem value="curl" label="cURL">

```sh
curl "$API_URL/knowledge-bases/$CASE_ID/profiles/contract-analysis/source-authority" \
  -H "Authorization: $API_KEY"

curl -X POST "$API_URL/knowledge-bases/$CASE_ID/profiles/contract-analysis/source-authority/decisions" \
  -H "Authorization: $API_KEY" \
  -H "Content-Type: application/json" \
  -d '{
    "observedRevision": 0,
    "decisionType": "source_authority",
    "content": {
      "documents": [
        {"id": "policy-doc", "sourceRole": "policy", "excluded": false}
      ]
    },
    "reason": "Reviewer confirmed the governing policy document"
  }'
```

</TabItem>
<TabItem value="python" label="Python SDK">

```python
authority = await client.get_contract_analysis_source_authority(CASE_ID)

decision = await client.record_contract_analysis_source_authority_decision(
    CASE_ID,
    observed_revision=authority.parsed["revision"],
    decision_type="source_authority",
    content={
        "documents": [
            {"id": "policy-doc", "sourceRole": "policy", "excluded": False}
        ]
    },
    reason="Reviewer confirmed the governing policy document",
)
```

</TabItem>
</Tabs>

There is no CLI command for source authority today; use the SDK or the endpoints directly.

## Try and approve a version-2 draft

Version-2 profiles require a scored, expert-attested example trial before publication. The workspace never runs examples automatically when a draft is edited.

Create a revisioned test suite that includes immutable package identities, scenario tags, and stable expectation target keys. An expectation is usable for a scored trial only after it is marked approved; updating approvals creates a new suite revision, so old reports and attestations do not silently carry forward.

Launch either an `exploratory` or a `scored` trial explicitly, supplying an `anchorJobGuid`, the suite revision, and an `idempotencyKey`. Exploratory runs are not correctness scores. Scored reports compare the completed run against the approved expectations deterministically; expectations stay on the evaluator side and are not included in model inputs. When multiple suite cases share the same captured anchor/package, include the exact `suiteCaseId` — legacy callers that omit it retain anchor-based selection. A reviewer can then attest a passing scored report.

Publish a v2 profile by including the returned `trialAttestationId` in the publish request, alongside the draft revision and any warning acknowledgments. The API verifies that the attestation still matches the current draft, suite, package snapshot, and execution configuration inside the publication transaction; a stale attestation is rejected without replacing the active publication. Version-1 publication requests keep their existing behavior and do not need an attestation.

<Tabs groupId="lang">
<TabItem value="curl" label="cURL">

```sh
curl "$API_URL/knowledge-bases/$CASE_ID/profiles/contract-analysis/test-suite" \
  -H "Authorization: $API_KEY"

curl -X PUT "$API_URL/knowledge-bases/$CASE_ID/profiles/contract-analysis/test-suite" \
  -H "Authorization: $API_KEY" \
  -H "Content-Type: application/json" \
  -d '{"revision": 0, "cases": [{"...": "suite case definition"}]}'

curl -X POST "$API_URL/knowledge-bases/$CASE_ID/profiles/contract-analysis/test-suite/proposals" \
  -H "Authorization: $API_KEY" \
  -H "Content-Type: application/json" \
  -d '{
    "runId": "run-1",
    "resultFingerprint": "sha256:result",
    "criterionId": "hours",
    "targetKey": "criterion:hours",
    "name": "Hours-clause omission",
    "expectedAssessment": "deviates_from_requirement",
    "reason": "Reviewer correction",
    "expectedSuiteRevision": 3,
    "idempotencyKey": "proposal-1"
  }'

curl -X POST "$API_URL/knowledge-bases/$CASE_ID/profiles/contract-analysis/trials" \
  -H "Authorization: $API_KEY" \
  -H "Content-Type: application/json" \
  -d '{
    "anchorJobGuid": "<completed-contract-job>",
    "idempotencyKey": "trial-key",
    "trialKind": "scored",
    "suiteRevision": 3
  }'

curl "$API_URL/knowledge-bases/$CASE_ID/profiles/contract-analysis/trials/$TRIAL_ID" \
  -H "Authorization: $API_KEY"

curl -X POST "$API_URL/knowledge-bases/$CASE_ID/profiles/contract-analysis/trials/$TRIAL_ID/attest" \
  -H "Authorization: $API_KEY" \
  -H "Content-Type: application/json" \
  -d '{"reportId": "report-1", "reason": "Reviewed and correct"}'
```

</TabItem>
<TabItem value="python" label="Python SDK">

```python
suite = await client.get_contract_analysis_test_suite(CASE_ID)

updated_suite = await client.update_contract_analysis_test_suite(
    CASE_ID, revision=suite.parsed["revision"], cases=[{"...": "suite case definition"}]
)

proposal = await client.propose_contract_analysis_finding_test_case(
    CASE_ID,
    run_id="run-1",
    result_fingerprint="sha256:result",
    criterion_id="hours",
    target_key="criterion:hours",
    name="Hours-clause omission",
    expected_assessment="deviates_from_requirement",
    reason="Reviewer correction",
    expected_suite_revision=updated_suite.parsed["revision"],
    idempotency_key="proposal-1",
)

trial = await client.create_contract_analysis_trial(
    CASE_ID,
    anchor_job_guid="<completed-contract-job>",
    idempotency_key="trial-key",
    trial_kind="scored",
    suite_revision=updated_suite.parsed["revision"],
)

trial_status = await client.get_contract_analysis_trial(CASE_ID, trial.parsed["trialId"])

attested = await client.attest_contract_analysis_trial(
    CASE_ID, trial.parsed["trialId"], report_id="report-1", reason="Reviewed and correct"
)

published_v2 = await client.publish_contract_analysis_profile(
    CASE_ID,
    draft_revision=updated_suite.parsed["revision"],
    trial_attestation_id=attested.parsed["trialAttestationId"],
)
```

</TabItem>
</Tabs>

When a draft declares scoped effects, publication also requires passing, attested examples tagged `scoped_effect_override` and `scoped_effect_unaffected_scope`. Each example expectation includes an `expectedEffectiveAttributes` object on an `item:<id>` target, and trial comparison checks those values against the returned effective variant in addition to the criterion assessment.

Proposing a test case is a draft suite edit only: it copies the captured package members and fingerprint into a new case in the next suite revision with `approved: false`. It does not directly update the profile, approve an assertion, start a trial, or publish a revision — an expert must explicitly approve the expectation, launch and inspect a new scored trial, attest that report, then publish using its attestation. If another reviewer changes the suite first, the proposal returns `409` with the suite revision conflict.

## Analyze a completed contract (v1)

Submit a first-class operation for a completed OCR/document-processing job and a published profile. The operation performs outline-aware map/reduce for long contracts, structural reduction, semantic relationship synthesis, deterministic applicability, and criterion evaluation before exposing one complete result — it never exposes a partial final result.

<Tabs groupId="lang">
<TabItem value="curl" label="cURL">

```sh
curl -X POST "$API_URL/operation" \
  -H "Authorization: $API_KEY" \
  -H "Content-Type: application/json" \
  -d '{
    "jobGuid": "<completed-contract-job>",
    "type": "contract-analysis",
    "parameters": {"customParameters": {"knowledgeBaseId": 42}}
  }'

curl "$API_URL/operation/<completed-contract-job>/contract-analysis" \
  -H "Authorization: $API_KEY"
```

</TabItem>
<TabItem value="python" label="Python SDK">

```python
result = await client.submit_and_wait_for_contract_analysis(
    job_guid="<completed-contract-job>", knowledge_base_id=42
)
```

</TabItem>
</Tabs>

`submit_and_wait_for_contract_analysis` submits the operation and polls `/operation/{jobGuid}/contract-analysis` until `resultAvailable` is `true` or `timeout` (default 120 seconds) elapses. Call `submit_contract_analysis(...)` and poll manually when you want to control retries yourself.

## Contract-analysis runs (v2)

Runs are immutable, addressable records: a run captures its own package of source documents, mode, and configuration, and its result never changes on rerun. `mode` is `published` (use the active publication) or `draft_trial` (use a specific draft revision). Include the anchor document in `package_members`; captures are limited to 32 members and 32 MiB of captured sources. `expected_missing_documents` is separately limited to 32 entries and `excluded_documents` to 256 entries — neither list consumes captured-member slots. Duplicate IDs, inaccessible members, and a missing anchor are rejected, and member order does not imply precedence.

<Tabs groupId="lang">
<TabItem value="curl" label="cURL">

```sh
curl -X POST "$API_URL/contract-analysis/runs" \
  -H "Authorization: $API_KEY" \
  -H "Content-Type: application/json" \
  -d '{
    "anchorJobGuid": "<agreement-job>",
    "knowledgeBaseId": 42,
    "mode": "published",
    "idempotencyKey": "request-1",
    "packageMembers": [
      {"jobGuid": "<agreement-job>", "role": "agreement"},
      {"jobGuid": "<amendment-job>", "role": "amendment"}
    ]
  }'

curl "$API_URL/contract-analysis/runs/$RUN_ID" \
  -H "Authorization: $API_KEY"

curl "$API_URL/contract-analysis/runs/$RUN_ID/result" \
  -H "Authorization: $API_KEY"

curl "$API_URL/contract-analysis/runs/$RUN_ID/reviews" \
  -H "Authorization: $API_KEY"

curl -X PUT "$API_URL/contract-analysis/runs/$RUN_ID/reviews/hours/criterion:hours" \
  -H "Authorization: $API_KEY" \
  -H "Content-Type: application/json" \
  -d '{
    "resultFingerprint": "sha256:result",
    "disposition": "corrected",
    "correctedAssessment": "deviates_from_requirement",
    "expectedReviewRevision": 0,
    "idempotencyKey": "review-1",
    "reason": "Confirmed the omission against the source clause"
  }'
```

</TabItem>
<TabItem value="python" label="Python SDK">

```python
created = await client.create_contract_analysis_run(
    "<agreement-job>",
    knowledge_base_id=42,
    idempotency_key="request-1",
    mode="published",
    package_members=[
        {"jobGuid": "<agreement-job>", "role": "agreement"},
        {"jobGuid": "<amendment-job>", "role": "amendment"},
    ],
)
run_id = created.parsed["runId"]

status = await client.get_contract_analysis_run(run_id)
result = await client.get_contract_analysis_run_result(run_id)
reviews = await client.get_contract_analysis_run_reviews(run_id)

review = await client.put_contract_analysis_run_review(
    run_id,
    "hours",
    "criterion:hours",
    result_fingerprint="sha256:result",
    disposition="corrected",
    corrected_assessment="deviates_from_requirement",
    expected_review_revision=0,
    idempotency_key="review-1",
    reason="Confirmed the omission against the source clause",
)
```

</TabItem>
</Tabs>

Targets in the review path use `criterion:<criterionId>` or `item:<itemId>`. `disposition` is one of `accepted`, `corrected`, `dismissed`, or `unresolved`; corrected and dismissed decisions require a `reason`. A stale `expectedReviewRevision` returns `409`. The API derives reviewer identity from the authenticated user, so organization-only API keys cannot submit human dispositions. Accepting or correcting a finding never rewrites the captured machine result — each rerun has a new run ID and starts without a disposition. CSV exports and the run workspace keep the machine assessment, disposition, reviewer, and rationale in separate fields, and original evidence links open the captured source page and source element.

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
    "model": "gpt-5",
    "deployment": "prod-eastus"
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

`DocuDevsClient` (synchronous, returns `JsonNode`) and `DocuDevsAsyncClient` (same method names, returns `CompletableFuture<JsonNode>`) expose the full lifecycle:

```java
DocuDevsClient client = DocuDevsClient.builder()
    .baseUrl("https://api.docudevs.ai")
    .apiKey(System.getenv("API_KEY"))
    .build();

JsonNode generation = client.generateContractAnalysisProfile(42L);
JsonNode draft = client.getContractAnalysisProfile(42L, "draft");
JsonNode published = client.publishContractAnalysisProfile(42L, 1L);
JsonNode result = client.submitAndWaitForContractAnalysis("<completed-contract-job>", 42L);
```

| Method | Purpose |
| --- | --- |
| `getContractAnalysisProfile(caseId[, view])` | Read the draft (default) or published profile. |
| `generateContractAnalysisProfile(caseId)` | Start asynchronous profile compilation. |
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
| `createContractAnalysisTrial(caseId, anchorJobGuid, idempotencyKey, suiteRevision, trialKind[, suiteCaseId])` | Launch an exploratory or scored trial. |
| `getContractAnalysisTrial(caseId, trialId)` | Read trial status and its immutable report. |
| `attestContractAnalysisTrial(caseId, trialId, reportId, reason)` | Record expert attestation of a passing report. |
| `submitContractAnalysis(jobGuid, knowledgeBaseId)` | Submit the v1 analysis operation. |
| `submitAndWaitForContractAnalysis(jobGuid, knowledgeBaseId[, waitOptions])` | Submit and poll the v1 operation to completion. |
| `createContractAnalysisRun(anchorJobGuid, knowledgeBaseId, idempotencyKey)` / `createContractAnalysisRun(anchorJobGuid, knowledgeBaseId, mode, draftRevision, idempotencyKey[, model, configurationFingerprint])` / `createContractAnalysisRun(ContractAnalysisRunRequest request)` | Create a run; the last overload accepts the full generated request model, including package members. |
| `getContractAnalysisRun(runId)` | Read run status. |
| `getContractAnalysisRunResult(runId)` | Read the immutable run result. |
| `getContractAnalysisRunReviews(runId)` | Read current human dispositions for a run. |
| `putContractAnalysisRunReview(runId, criterionId, targetKey, review)` | Append an expert disposition. |

## In the web app

- The profile workspace lives on the case detail page, under Contract analysis, at `/app/cases/{caseId}/contract-analysis`. It drives profile generation, draft editing, source-authority classification, test-suite management, and trial review for that case's knowledge base.
- Run review lives at `/app/contract-analysis/runs/{runId}`, showing the machine assessment and reviewer disposition side by side, with links to the original evidence.
- A completed job's own page shows a per-job contract-analysis results panel alongside its other outputs.

## CLI

The CLI mirrors most of the SDK lifecycle:

```bash
docudevs knowledge-base contract-analysis generate 42
docudevs knowledge-base contract-analysis get 42 --view published
docudevs knowledge-base contract-analysis update 42 --draft-revision 1 --profile-file profile.json
docudevs knowledge-base contract-analysis publish 42 --draft-revision 1 --acknowledge-warning UNCITED_HUMAN_AUTHORED
docudevs knowledge-base contract-analysis generation-compare 42 <generation-job-guid>
docudevs knowledge-base contract-analysis generation-apply 42 <generation-job-guid> --decisions decisions.json
docudevs knowledge-base contract-analysis suite-get 42
docudevs knowledge-base contract-analysis suite-update 42 --revision 3 --cases cases.json
docudevs knowledge-base contract-analysis propose-test-case 42 --run-id run-1 --result-fingerprint sha256:result \
  --criterion-id hours --target-key criterion:hours --name "Hours-clause omission" \
  --expected-assessment deviates_from_requirement --reason "Reviewer correction" \
  --expected-suite-revision 3 --idempotency-key proposal-1
docudevs knowledge-base contract-analysis trial 42 --anchor-job-guid <job> --idempotency-key <key> --kind scored
docudevs knowledge-base contract-analysis trial-get 42 <trial-id>
docudevs knowledge-base contract-analysis trial-attest 42 <trial-id> --report-id <report-id>
docudevs knowledge-base contract-analysis run-create 42 --anchor-job-guid <job> --idempotency-key <key>
docudevs knowledge-base contract-analysis run-get <run-id>
docudevs knowledge-base contract-analysis run-result <run-id>
docudevs knowledge-base contract-analysis run-reviews <run-id>
docudevs knowledge-base contract-analysis review <run-id> <criterion-id> <target-key> \
  --result-fingerprint sha256:result --disposition accepted \
  --expected-review-revision 0 --idempotency-key review-1
docudevs operations contract-analysis <completed-contract-job> --knowledge-base-id 42
docudevs operations status <operation-job-guid>
docudevs operations result <operation-job-guid> --type contract-analysis
```

`run-create` accepts optional JSON files through `--package-members`, `--expected-missing`, and `--excluded-documents`; include the anchor in the member list. Use `--trial-attestation-id` on `publish` when publishing a reviewed v2 profile. There is no CLI command for source-authority decisions — use the SDK or the endpoints directly. The CLI waits for the v1 operation by default; pass `--no-wait` to receive the operation job reference instead.

## Availability

Contract-analysis generation and analysis must be enabled for your organization or deployment; when disabled, public profile and analysis entry points return a typed `404`. The reviewed version-2 workflow (source authority, trials, scored review, `/contract-analysis/runs`) is a separate, nested capability that must also be enabled for your deployment before it accepts writes — v1 profile publication is unaffected either way. Legacy knowledge-base search and inline evaluation (see [Knowledge Search](/docs/core/knowledge-search)) continue to work when contract analysis is disabled.
