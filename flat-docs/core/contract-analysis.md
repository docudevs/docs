---
title: Contract Analysis
description: Review contracts clause by clause against a playbook, with a map of where each step is documented.
sidebar_position: 11
---
<!-- Generated from docs/core/contract-analysis.md. Do not edit by hand. -->

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

**cURL**

```bash
wait_for_job() {
  local deadline=$((SECONDS + 600))
  while [ "$SECONDS" -lt "$deadline" ]; do
    STATUS=$(curl -s -S -f "https://api.docudevs.ai/job/status/$1" \
      -H "Authorization: $API_KEY" | jq -r '.status')
    case "$STATUS" in
      COMPLETED) return 0 ;;
      PARTIAL) echo "Job $1 ended with status PARTIAL" >&2; return 0 ;;
      ERROR|FAILED|TIMEOUT) echo "Job $1 ended with status $STATUS" >&2; return 1 ;;
    esac
    sleep 5
  done
  echo "Job $1 did not finish within 600 seconds" >&2
  return 1
}

JOB_GUID="<completed-contract-job>"  # jobGuid of a completed contract job
KNOWLEDGE_BASE_ID=42  # id of the case whose profile is published

# The response carries the jobGuid of the analysis job, not of the contract job
ANALYSIS_JOB_GUID=$(curl -s -S -f -X POST "https://api.docudevs.ai/operation" \
  -H "Authorization: $API_KEY" \
  -H "Content-Type: application/json" \
  -d '{
    "jobGuid": "'"$JOB_GUID"'",
    "type": "contract-analysis",
    "parameters": {"customParameters": {"knowledgeBaseId": '"$KNOWLEDGE_BASE_ID"'}}
  }' | jq -r '.jobGuid')

wait_for_job "$ANALYSIS_JOB_GUID"

curl -s -S -f "https://api.docudevs.ai/job/result/$ANALYSIS_JOB_GUID" \
  -H "Authorization: $API_KEY" | jq .
```

**Python**

```python
job_guid = "<completed-contract-job>"  # jobGuid of a completed contract job
knowledge_base_id = 42  # id of the case whose profile is published

submission = await client.submit_operation_with_parameters(
    job_guid,
    "contract-analysis",
    custom_parameters={"knowledgeBaseId": knowledge_base_id},
)
if submission is None or not getattr(submission, "job_guid", None):
    raise SystemExit("Contract analysis was not accepted")

# The submission carries the job_guid of the analysis job, not of the contract job
result = await client.wait_until_ready(
    submission.job_guid, timeout=600, poll_interval=5, result_format="json"
)
print(json.dumps(result, indent=2))
```

**Java**

```java
import com.fasterxml.jackson.databind.JsonNode;

String jobGuid = "<completed-contract-job>"; // jobGuid of a completed contract job
long knowledgeBaseId = 42L; // id of the case whose profile is published

// This call polls the operation route of the contract job, which can answer with any
// one of its analyses; cURL and Python wait on the analysis job they just started
JsonNode operation = client.submitAndWaitForContractAnalysis(jobGuid, knowledgeBaseId);

System.out.println(operation.path("result").asText());
```

**CLI**

```bash
JOB_GUID="<completed-contract-job>"  # jobGuid of a completed contract job

docudevs operations submit "$JOB_GUID" \
  --type contract-analysis \
  --parameter knowledgeBaseId=42 \
  --wait
```

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

**cURL**

```bash
curl -s -S -f -X POST "https://api.docudevs.ai/contract-analysis/runs" \
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
  }' | jq .
```

**Python**

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
print(f"Run ID: {created.parsed['runId']}")
```

**Java**

```java
import ai.docudevs.client.generated.model.ContractAnalysisRunRequest;
import ai.docudevs.client.generated.model.ContractPackageMemberRequest;
import com.fasterxml.jackson.databind.JsonNode;

ContractAnalysisRunRequest request = new ContractAnalysisRunRequest()
        .anchorJobGuid("<agreement-job>")
        .knowledgeBaseId(42L)
        .mode("published")
        .idempotencyKey("request-1")
        .addPackageMembersItem(new ContractPackageMemberRequest().jobGuid("<agreement-job>").role("agreement"))
        .addPackageMembersItem(new ContractPackageMemberRequest().jobGuid("<amendment-job>").role("amendment"));

JsonNode created = client.createContractAnalysisRun(request);
System.out.println("Run ID: " + created.path("runId").asText());
```

**CLI**

```bash
CASE_ID=42  # id of the case that holds the playbook documents

echo '[{"jobGuid": "<agreement-job>", "role": "agreement"}, {"jobGuid": "<amendment-job>", "role": "amendment"}]' > members.json

docudevs knowledge-base contract-analysis run-create "$CASE_ID" \
  --anchor-job-guid "<agreement-job>" \
  --idempotency-key request-1 \
  --mode published \
  --package-members members.json
```

The run is created in `published` mode here. Read its status and result, and review its findings, as described in [Analyze a contract](./contract-analysis-playbooks.md#analyze-a-contract).

## Read a playbook

`GET /knowledge-bases/{caseId}/profiles/contract-analysis` returns the playbook's draft (`view=draft`, the default) or its published revision (`view=published`). The response carries the lifecycle `status` (`GENERATING`, `DRAFT`, `PUBLISHED` or `FAILED`), `draftRevision`, `publishedRevision` and the playbook as `profile`. A case without a playbook returns `404`. For the canonical form that can be edited and imported again, use the export route (see [Import and export a playbook](./contract-analysis-playbooks.md#import-and-export-a-playbook)).

**cURL**

```bash
CASE_ID=42  # id of the case that holds the playbook documents

curl -s -S -f "https://api.docudevs.ai/knowledge-bases/$CASE_ID/profiles/contract-analysis?view=draft" \
  -H "Authorization: $API_KEY" | jq .
```

**Python**

```python
case_id = 42  # id of the case that holds the playbook documents
response = await client.get_contract_analysis_profile(case_id, view="draft")
draft = response.parsed

print(f"Status: {draft['status']}")
print(f"Draft revision: {draft['draftRevision']}")
```

**Java**

```java
import com.fasterxml.jackson.databind.JsonNode;

long caseId = 42L; // id of the case that holds the playbook documents
JsonNode draft = client.getContractAnalysisProfile(caseId, "draft");

System.out.println("Status: " + draft.path("status").asText());
System.out.println("Draft revision: " + draft.path("draftRevision").asLong());
```

**CLI**

```bash
CASE_ID=42  # id of the case that holds the playbook documents

docudevs knowledge-base contract-analysis get "$CASE_ID" --view draft
```

**cURL**

```bash
CASE_ID=42  # id of the case that holds the playbook documents

curl -s -S -f "https://api.docudevs.ai/knowledge-bases/$CASE_ID/profiles/contract-analysis?view=published" \
  -H "Authorization: $API_KEY" | jq .
```

**Python**

```python
case_id = 42  # id of the case that holds the playbook documents
response = await client.get_contract_analysis_profile(case_id, view="published")
active = response.parsed

print(f"Status: {active['status']}")
print(f"Published revision: {active['publishedRevision']}")
```

**Java**

```java
import com.fasterxml.jackson.databind.JsonNode;

long caseId = 42L; // id of the case that holds the playbook documents
JsonNode active = client.getContractAnalysisProfile(caseId, "published");

System.out.println("Status: " + active.path("status").asText());
System.out.println("Published revision: " + active.path("publishedRevision").asLong());
```

**CLI**

```bash
CASE_ID=42  # id of the case that holds the playbook documents

docudevs knowledge-base contract-analysis get "$CASE_ID" --view published
```

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

**cURL**

```bash
CASE_ID=42  # id of the case that holds the playbook documents

curl -s -S -f "https://api.docudevs.ai/knowledge-bases/$CASE_ID/profiles/contract-analysis/source-authority" \
  -H "Authorization: $API_KEY" | jq .
```

**Python**

```python
case_id = 42  # id of the case that holds the playbook documents
response = await client.get_contract_analysis_source_authority(case_id)
authority = response.parsed

print(f"Revision: {authority['revision']}")
print(f"Decisions recorded: {len(authority.get('decisions', []))}")
```

**Java**

```java
import com.fasterxml.jackson.databind.JsonNode;

long caseId = 42L; // id of the case that holds the playbook documents
JsonNode authority = client.getContractAnalysisSourceAuthority(caseId);

System.out.println("Revision: " + authority.path("revision").asLong());
System.out.println("Decisions recorded: " + authority.path("decisions").size());
```

**cURL**

```bash
CASE_ID=42  # id of the case that holds the playbook documents

AUTHORITY=$(curl -s -S -f "https://api.docudevs.ai/knowledge-bases/$CASE_ID/profiles/contract-analysis/source-authority" \
  -H "Authorization: $API_KEY")

# Every guideline document of the case must be listed, with the fingerprint read above.
# This example confirms each document as policy; set the role of each document.
jq -n \
  --argjson authority "$AUTHORITY" \
  '{
    observedRevision: $authority.revision,
    decisionType: "source_authority",
    content: {
      documents: [($authority.sources // [])[]
        | {id: .documentId, contentFingerprint: .contentFingerprint, sourceRole: "policy"}]
    },
    reason: "Reviewer confirmed the governing policy documents"
  }' \
  | curl -s -S -f -X POST "https://api.docudevs.ai/knowledge-bases/$CASE_ID/profiles/contract-analysis/source-authority/decisions" \
      -H "Authorization: $API_KEY" \
      -H "Content-Type: application/json" \
      -d @- | jq .
```

**Python**

```python
case_id = 42  # id of the case that holds the playbook documents
response = await client.get_contract_analysis_source_authority(case_id)
authority = response.parsed

# Every guideline document of the case must be listed, with the fingerprint read above.
# This example confirms each document as policy; set the role of each document.
documents = [
    {
        "id": source["documentId"],
        "contentFingerprint": source["contentFingerprint"],
        "sourceRole": "policy",
    }
    for source in authority.get("sources", [])
]
if not documents:
    raise SystemExit("The case has no guideline documents that finished processing")

decision = await client.record_contract_analysis_source_authority_decision(
    case_id,
    observed_revision=authority["revision"],
    decision_type="source_authority",
    content={"documents": documents},
    reason="Reviewer confirmed the governing policy documents",
)
if decision.status_code != 200:
    raise SystemExit(f"Source authority was not recorded: {decision.status_code}")
print(f"Revision: {decision.parsed['revision']}")
```

**Java**

```java
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;

long caseId = 42L; // id of the case that holds the playbook documents
JsonNode authority = client.getContractAnalysisSourceAuthority(caseId);

// Every guideline document of the case must be listed, with the fingerprint read above.
// This example confirms each document as policy; set the role of each document.
ObjectNode content = new ObjectMapper().createObjectNode();
ArrayNode documents = content.putArray("documents");
for (JsonNode source : authority.path("sources")) {
    documents.addObject()
            .put("id", source.path("documentId").asText())
            .put("contentFingerprint", source.path("contentFingerprint").asText())
            .put("sourceRole", "policy");
}
JsonNode decision = client.recordContractAnalysisSourceAuthorityDecision(
        caseId,
        authority.path("revision").asLong(),
        "source_authority",
        content,
        "Reviewer confirmed the governing policy documents");
System.out.println("Revision: " + decision.path("revision").asLong());
```

The example gives every document the role `policy`; set the role of each document to what it is. There is no CLI command for source authority; use the SDK or the endpoints directly.

## Java SDK

`DocuDevsClient` (synchronous, returns `JsonNode`) and `DocuDevsAsyncClient` (the same method names, returns `CompletableFuture<JsonNode>`) have these contract-analysis methods:

| Method | Purpose |
| --- | --- |
| `getContractAnalysisProfile(caseId[, view])` | Read the draft (default) or published playbook. |
| `getContractAnalysisWorkflow(caseId)` | Read where the playbook is in its setup. |
| `generateContractAnalysisProfile(caseId, ourParty, counterparty[, scope])` | Start building clauses from the guideline documents. |
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

- **Playbooks** in the sidebar; the steps are in [What is available](./contract-analysis-playbooks.md#what-is-available).
- On the **Jobs** page, a completed document job has an **Analyze contract** button. The dialog asks for a **Playbook**, and only playbooks with a published revision can be chosen. It also lets you select further documents for the analysis (up to 32 documents and 32 MiB of captured source) and name an **Expected but unavailable document**. **Analyze contract** creates a `published` run and opens its page.
- A run opens at `/app/contract-analysis/runs/{runId}`: the run ID, its status and mode, and the findings by clause. For a `draft_trial` run the page says that the result is for review and does not change the published playbook. Each finding offers **Agree**, **Correct** and **Dismiss**; **Correct** asks for a verdict, and **Correct** and **Dismiss** need a reason. **Save review** records the disposition.
