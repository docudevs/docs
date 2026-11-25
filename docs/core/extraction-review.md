---
title: Extraction review
---

# Extraction review

Extraction review stores a versioned review record beside an immutable extraction result. Evaluators can submit claims against one exact archived result; human reviewers can flag issues, record value resolutions, and adjudicate claims. These actions preserve the extractor's raw result and do not promote data into a golden set.

The implementation includes the authenticated API, Python and Java SDK helpers, and a native job review workspace. Readiness is always `NOT_ASSESSED`; acceptance-policy checks are not part of review. Configurations use the supported stored profile shape, and only configurations that the current capture path can archive produce reviewable runs. Treat unavailable or incomplete evidence as unavailable. Do not substitute a mutable latest result.

## Configure an extraction profile

An active human ADMIN can preview and save an enabled review profile for a named configuration. The configuration must use a supported extraction shape such as `SIMPLE`. The profile stores its schema and optional review overlay with an expected profile revision. Preview uses the same validation and preparation as save, without persisting a revision.

```python
preview = await client.preview_extraction_review_profile(
    "invoice-v1",
    {"schemaJson": '{"type":"object","properties":{"total":{"type":"number"}}}', "overlay": None},
)
saved = await client.save_extraction_review_profile(
    "invoice-v1",
    {
        "expectedRevision": preview.parsed["revision"],
        "enabled": True,
        "schemaJson": preview.parsed["schemaJson"],
        "overlay": None,
    },
)
```

Profile reads allow an active human ADMIN or MEMBER and API keys. Profile preview and save require an active human ADMIN. The API key is not a substitute for human profile administration.

## Hybrid producer descriptor and named profile

`ClientPipeline.review_descriptor(...)` describes a hybrid producer that can be
bound to a named review profile. The Python SDK's opt-in review runner opens a
captured run, records local execution evidence, dispatches supported remote
segments and requests completion. The public API rejects graph operations that
the review runtime cannot verify before it creates the capture or writes input
archives. This workflow is an opt-in integration surface. Do not
upload the descriptor as a server pipeline or save the live Python graph as a
configuration.

The method records the complete remote node definitions and compiled segments,
including the existing synthetic publication segment for a local final. Local
nodes record their inputs, effective dependencies, gates and error policy. Local
callables and Python output types are excluded; supply the final JSON schema and
a stable function/version ID for every local node. Exactly one final is required.

```python
descriptor = pipeline.review_descriptor(
    producer={
        "id": "invoice-producer",
        "releaseId": producer_release_id,
        "codeSha256": producer_bundle_sha256,
        "data": [{"id": "vendor-catalog", "sha256": vendor_catalog_sha256}],
    },
    local_function_ids=local_implementation_ids,
    final_schema=final_result_schema,
)
```

In this example, `pipeline` is the built `ClientPipeline` and
`local_implementation_ids` maps every local node ID to its supplied implementation
ID. Missing or extra entries fail; several nodes may use the same implementation
ID. Release and implementation IDs must be nonblank. Code/data digests must be
64 lowercase hexadecimal characters; data IDs must be unique. Supply `data: []`
when the producer has no versioned data dependencies. Unknown producer fields,
non-string object keys and nonfinite/non-JSON values are rejected.

The returned object is a detached JSON snapshot with format/compiler versions,
the pipeline version, declared producer, original remote nodes, local node
declarations, final node/schema and compiled segments. Mutating it does not change
the graph or caller metadata. Building it neither executes local functions nor
performs network calls. Code/data identities are caller assertions; they do not
attest which code executed. The SDK does not compute a producer fingerprint;
the API computes it when previewing or saving the binding.

Pass the descriptor as JSON text on the existing profile request:

```python
import json

preview = await client.preview_extraction_review_profile(
    "invoice-v1",
    {"clientPipelineJson": json.dumps(descriptor, allow_nan=False)},
)
saved = await client.save_extraction_review_profile(
    "invoice-v1",
    {
        "expectedRevision": preview.parsed["revision"],
        "clientPipelineJson": preview.parsed["clientPipelineJson"],
        "enabled": True,
    },
)
```

The API stores canonical descriptor JSON and returns `producerFingerprint`
(`sha256:` plus 64 lowercase hexadecimal characters). This separate identity
covers the entire descriptor, including graph, compiled segments, implementation
IDs, producer metadata and final schema. Existing configuration and overlay
fingerprints retain their behavior. Descriptor identity uses exact integer and
decimal parsing, and the UTF-8 JSON limit is 2 MiB.

The descriptor's final schema drives overlay generation. An explicit `schemaJson`
must match it. Omitted or null `clientPipelineJson` preserves an existing binding;
`removeClientPipeline: true` clears it and restores ordinary schema checks. Do not
combine removal with a new descriptor. Preview validates without saving or
consuming a revision; save still requires `expectedRevision` and active human
ADMIN access. Disabling preserves the binding; a disable-only save preserves the
stored snapshots even if the command has since changed.

Binding does not by itself enable hybrid capture. The ordinary upload capture
path rejects an enabled descriptor-bound profile; a disabled profile proceeds
without capture. The Python SDK adds an opt-in `ReviewCaptureOptions` argument
to `run_client_pipeline` for captured client runs. It binds by named profile and
expected revision, records every local execution using exact
`inputsJson`/`resultJson` strings, and returns review identity only after the
server accepts completion. Producer metadata and local function IDs are caller
provenance claims, not code attestations. Optional `LocalSourceMapping` values
assert copied output provenance; unmapped output remains unresolved. A
transport retry reuses the request/client-run IDs; a whole-run retry creates a
new capture and re-executes local functions. Source-location and OCR
preservation requests remain opt-in and do not guarantee available anchors.

The API exposes `POST /job/{guid}/pipeline/client-runs/review` with
`requestId`, `configurationName`, `expectedProfileRevision`, `clientRunId`,
`descriptor`, and `ttlSeconds`. The response contains the client-run ID, lease
deadline, review-run ID, input-manifest digest, and producer fingerprint. The
opener applies the runtime-supported graph check before capture preparation.
`POST /job/{guid}/pipeline/client-runs/{id}/segments/{index}/skip` accepts
`{"formatVersion":1}`. Bodyless
`POST /job/{guid}/pipeline/client-runs/{id}/complete` returns the selected
completion identity and manifest digest. For a bound run, the existing local
node result PUT accepts the versioned envelope and returns its immutable record
receipt; for an ordinary run, the same PUT continues to store any valid JSON
value and returns HTTP 204.

The captured runner uses the named configuration and expected profile revision
as pins. Producer metadata and local function IDs remain caller provenance
claims; the API does not attest which code executed. Local evidence envelopes
carry `formatVersion: 1`, an explicit `COMPLETED` or `SKIPPED` status, the
function ID, `inputsJson`, `resultJson`, and source mappings. A skipped local
node has null inputs and result, an empty mapping list, and does not invoke its
callable. Completed `resultJson: "null"` remains distinct from a skipped node.
The API returns a completion identity only after it verifies the selected
archive and commits the review binding. A transport retry reuses the same
request and client-run IDs and retries the same operation; a whole-run retry
creates a new capture identity and executes local functions again.

Optional source mappings assert copied values and may provide provenance for
those leaves. Unmapped or unsupported evidence remains unresolved; equal values
alone do not establish a source. Source-location and OCR preservation options
remain opt-in and do not guarantee that usable anchors are present. The captured
runner is not a resumable workflow, and callers must not treat producer IDs as
proof of code identity.

Shared admission now protects upload submission and the OCR endpoint. A bound
review client run cannot be dispatched by the legacy
`process_document`, `process_document_with_configuration`, or `ocr_document`
routes; each returns HTTP 409 when admission finds a review binding, regardless
of lease status or an incoming `clientRunId`. Process routes return HTTP 404
when the job is absent from the caller's organization and HTTP 503 when
admission cannot acquire the job lock. OCR creates a missing job in the caller's
organization and maps its admission conflicts to HTTP 409.

Python detailed SDK calls preserve these status codes and the raw response
body with `parsed=None`, including when `raise_on_unexpected_status=True`;
convenience methods keep their existing response object and expose the same
response metadata. Successful calls retain the existing `UploadResponse`
shape.

## Find and register the exact run

Process a document with the saved configuration using the normal extraction workflow. Then call `list_extraction_review_runs(job_guid)` and select the returned run descriptor for that job. Rerunning the same bytes creates a distinct run; use the descriptor's `runId` and full server-returned `identity` rather than deriving either from a hash or choosing the latest result. Register the selected run, then read it with `get_extraction_review_run(run_id, review_revision=...)` when you need a pinned review snapshot.

Keep the full private reference set outside the extraction request, profile, and model input. Compare the extracted result against that set separately, then submit the specific disagreement as a post-extraction evaluator claim. That claim may include the relevant proposed value, comparison rationale, and reference/comparator version identifiers; do not upload the full golden corpus or place expected answers in the extraction prompt or profile. If the selected archive is incomplete or unavailable, stop that review path and retain its reported status.

`identity` binds the organization, run, job, artifact, and raw-result fingerprint. Write requests use the `reference` subset (`jobGuid`, `artifactId`, `resultFingerprint`) returned by the server. The fingerprint is the lowercase SHA-256 of raw archived result bytes without a `sha256:` prefix. Download verified evidence with `get_extraction_review_artifact(run_id, path)`; it returns the raw bytes.

## Submit evaluator claims

An API key may submit evaluator claims. The run ID selects the immutable run, while `reference` selects its exact result. Include the current review revision and a unique idempotency key for each logical write.

```python
identity = run.parsed["identity"]
claim = await client.submit_extraction_review_claims(
    identity["runId"],
    reference={
        "jobGuid": identity["jobGuid"],
        "artifactId": identity["artifactId"],
        "resultFingerprint": identity["resultFingerprint"],
    },
    expected_review_revision=run.parsed["reviewRevision"],
    idempotency_key="evaluator-2026-10-06-0001",
    claims=[{
        "claimId": "check-total-0001",
        "claimRevision": 1,
        "identity": {
            "reporterId": "invoice-checker",
            "externalIssueId": "check-0001",
            "referenceVersion": "dataset-2026-10",
            "comparatorVersion": "2",
        },
        "address": {"scope": "BASE", "pointer": "/total"},
        "observed": {"present": True, "valueJson": "0"},
        "proposedValueJson": "125.5",
        "rationaleJson": '"reference differs from extracted value"',
        "priority": "HIGH",
    }],
    audit={"evidenceArtifactPaths": [], "exampleEligibility": False},
)
```

JSON-valued fields such as `valueJson`, `proposedValueJson`, and `rationaleJson` are canonical JSON strings. For example, boolean false is the string `"false"`, numeric zero is `"0"`, and explicit JSON null is `"null"`. A missing value is represented by `present: false` with `valueJson: null`; preserve false, zero, null, and absence as separate states. Audit input contains no actor or timestamp; the service supplies trusted actor and time metadata.

## Link a reviewer to the submitted disagreements

The claim submission response returns the review identifier in
`event.runId` and `state.identity.runId`. Registration returns the same identifier
in `identity.runId`. Use that run ID to construct a link on your DocuDevs web app
origin:

```python
from urllib.parse import quote

review_id = claim.parsed["event"]["runId"]
review_url = f"{docudevs_web_origin.rstrip('/')}/app/extraction-review/{quote(review_id, safe='')}"
```

`docudevs_web_origin` is your configured web app origin. The authenticated link
resolves the authorized run and opens its job review workspace. It does not
create or register a review. The reviewer must sign in and have access to the
same organization. An unavailable or unauthorized run reports an error.

Use the run ID for this workspace link: `claimId` identifies an individual
disagreement, and `eventId` identifies a committed audit event. To link a
historical, read-only snapshot, append `?reviewRevision=N` using the response's
review revision. Omit that parameter to open the current review state.

## Review and adjudicate

An active human ADMIN or MEMBER can add flags, resolve values, and adjudicate a claim. Each mutation supplies an expected review revision and `Idempotency-Key`; adjudication also pins the expected claim, adjudication, and resolution revisions. Read the latest run state before preparing a change, and handle revision conflicts by reading again and making a new decision. Human audit input contains a reason and optional evidence metadata, never an actor or timestamp.

The raw extractor result stays immutable. Resolutions and adjudications are separate versioned review state; they do not update the extraction artifact, dataset labels, training data, or acceptance policy.

## Native review workspace

Open **Review extraction** beside **Trace** on a job's results, or visit
`/app/jobs/{jobGuid}/review`. External evaluators can link directly using
`/app/extraction-review/{runId}`.
An enabled stored review profile must have been captured when the extraction
ran; enabling a profile later does not make an old mutable job result reviewable.
Capture discovery keeps reruns separate and reports incomplete or unavailable
archives. Opening an eligible capture reads its current review state. An
unregistered capture requires an explicit registration action.

The workspace opens with a compact **Needs attention** queue: unresolved base
values and unresolved claims appear once per address. Choose **Browse all
fields** to inspect other captured values. The archived original opens beside the
selected field and has the largest pane on wide screens. On a narrow screen,
use **Queue** and **Original document** beside the field name to open those
panels while keeping the selected field and draft.
Captured overlay labels, descriptions and array identity fields are used
throughout the queue and viewer. The default review flow presents
the extracted value, evaluator suggestion, and human decision. For a list
disagreement, start with the proposed additions, omissions, and moves. Select
an observed occurrence to inspect its source while keeping the parent claim
and reason in place. The full extracted list remains available on demand.
Choose **Keep extracted value**, **Correct**, or **Leave unresolved**, enter a
reason, and select **Save decision**. Extracted values, evaluator proposals and
saved human decisions remain separate. Missing values, explicit null, blank
strings, zero and false remain distinguishable. Raw addresses are hidden until
**Address** or **Show address** is opened; search also matches addresses. The
full reviewed projection is available in a collapsed disclosure.

When several claims refer to the same field, select the claim by its reporter
and version identifiers. Scalar values use typed string, number, boolean or
null controls. Scalar arrays have typed add, remove and replace controls, and
each member change retains its exact stable address. Objects and explicit
structural operations are in **Advanced correction controls**. **Use
suggestion as draft** copies a selected evaluator proposal into the correction
controls after confirmation when a draft is already open. This only prepares a
correction; the reviewer must enter a reason and explicitly save. **Advanced
decision details** contains optional diagnosis, example eligibility, and
archived evidence attachments. Use **Flag issue** to record a manual issue or,
for a missing field outside the captured overlay, enter its JSON Pointer; the
parent container must exist in the immutable base result.

After saving, the workspace confirms the saved decision and offers **Next
needing attention** when another queued target remains. The selected target is
kept in place until the reviewer chooses that action.

The initial editor supports integers up to 100 digits. Noninteger corrections
support up to 12 significant digits with absolute values at least `1e-100` and
less than `1e100`, and must retain their exact decimal meaning through the API
encoding.
Numbers outside those bounds, or numbers that would round or underflow, show a
validation error before submission. JSON objects and arrays have narrower
numeric support: `[0.1]`, `[1.25]`, `[1e3]` and `[1.0]` are accepted, while
`[1.25e-4]` is rejected even though the scalar number control supports `1.25e-4`.
Correct the individual numeric field through the number control where possible.
These editing limits do not change how existing archived values are displayed.
Correction objects must have unique property names; duplicate keys are rejected
instead of silently keeping the last value.

The original can be previewed inline in supported PDF and image formats and
downloaded when available. Recorded locations use the archived source map and
page images to highlight the relevant region. When a value has no location,
consult the original directly or leave the value unresolved. The lack of a
recorded location does not establish that a value is absent. A correction does
not inherit the machine's source rectangle.
Reviewer evidence attachments are under **Advanced decision details** and
select archived artifact paths explicitly; precise reviewer-drawn regions are
not supported yet.

Assess an evaluator claim separately through **Assess suggestion separately**.
Keeping or correcting a value does not decide whether the evaluator's reference
was wrong or semantically equivalent. The assessment has its own reason and
persists only after **Save assessment**. A later correction can make an earlier
linked assessment stale, while retaining its history. After a member-level
list edit, the selected list remains in view with the saved member reason and
current reviewed list; a claim at a different member remains pending until you
assess it.

After an uncertain transport failure, retry the same action with its original
idempotency key and payload. After a revision conflict, read the updated state
and explicitly resubmit the intended decision. A pinned
`?reviewRevision=N` view is read-only. Readiness remains `NOT_ASSESSED`; saved
decisions do not establish acceptance. Reviewer leases, a global inbox and acceptance policies are not available.

## Consume the decision feed

Call `pull_extraction_review_decisions(after_ordinal=cursor, limit=100, case_id=...)` and persist the returned `nextCursor` exactly as provided, even when `events` is empty. A filtered page can advance the authoritative cursor. Use the event ordinal and event identity to make downstream handling idempotent; do not calculate the cursor from the last visible event.

## Keep evaluation outputs separate

Store the raw extraction artifact and its fingerprint as the immutable machine output. Store adjudicated values, reviewer decisions, and their revision history in a separate result stream. Score both against the private reference set outside the extraction request. This preserves the original output for audit and prevents adjudicated outcomes from silently becoming future golden labels.
