---
title: SDK Changelog
description: User-focused updates for the DocuDevs Python and Java SDKs.
mdx:
  format: md
---

# SDK Changelog

## 2026-11-26 UTC

### Highlights

- The Java SDK now covers the operations of the Python client: records and datasets, classify, category trees, memos, knowledge bases and search, agent chat and runs, providers and credentials, map-reduce, compare, document layout and figures, and the rest of contract analysis.
- Every blocking method of the Java client has an async version on `DocuDevsAsyncClient`.
- These changes are in Java SDK 0.4.0 and Python SDK 1.16.0.

### Python SDK

- `agent_chat` and `agent_chat_and_wait` accept a plain string as `messages`. A string is sent as one user message; the list form is unchanged. An empty string raises `ValueError`.

### Java SDK

- New areas on `DocuDevsClient` and `DocuDevsAsyncClient`:
  - Records and datasets: `createRecordsCase`, `recordsCase`, `setDataset`, `upsertRecord`, `bulkUpsertRecords`, `syncRecords`, `confirmSyncRecords`, `importRecordsFromUrl`, `listRecordsPage`, `recordStats`, `waitForImport`, and the `RecordsCase` handle.
  - Classify: `classify` for rows or a file, `classifyKeys`, `waitForClassify`, `getClassifyResultsPage`, `forEachClassifyResult`, `reviewClassify`, `commitClassify`, `downloadClassifyCsv`.
  - Category trees, memos and knowledge bases: `uploadTree`, `activateTree`, `waitForTreeActivation`, `getTreeActivationReport`, `listMemo`, `addMemoEntry`, `promoteKnowledgeBase`, `listKnowledgeBases`, `deleteKnowledgeBase`, `search`.
  - Agent: `agentChat` and `agentChatAndWait` with a string or a message list, `startAgentRun`, `waitForAgentRun`, `listAgentActions`, `confirmAgentAction`, `rejectAgentAction`, `startHoldoutEval`, `waitForEval`, `compareHoldoutEvals`.
  - Providers and credentials: LLM, OCR and embedding providers, model credentials, key bindings, embedding overview and reindex.
  - Documents and results: `fetchResult`, `fetchResultJson`, `getSourceLocations`, `getDocumentLayout`, `getDocumentOutline`, `getOcrQuality`, `getFiguresMetadata`, `downloadImage`, AcroForm metadata and fill, `uploadTemplateImages`, `scheduleBatch`.
  - Operations and compare: `submitAndWaitForOperation` and the per-operation variants, `compareDocuments`, `waitUntilCompareReady`, `exportDocumentCompare`.
  - Map-reduce: `processDocumentMapReduce`, `submitAndProcessDocumentMapReduce`, `submitAndWaitForMapReduce`; `MapReduceOptions.SplitType` gains `DOCUMENT_OUTLINE`.
  - Contract analysis: `prepareContractAnalysisTestPackage`, `getContractAnalysisTrialReadiness`, `waitForEvaluation`, `ContractTrialOptions`, and `scope` on profile generation.
- Methods that wait take `WaitOptions`. Methods ending in `AndWait` wait for the job; the same method without the suffix returns as soon as the job is accepted.
- `DocuDevsClient.Builder.requestTimeout(Duration)` sets a timeout for each request.
- `ProcessOptions` gains `dependsOn`, `sourceLocations` and `sourceLocationGranularity`.
- Cancelling a future returned by a waiting async method with `cancel(true)` stops the wait.
- Fixed: `ocrDocument(UploadRequest, String)` called a route that does not exist and always failed. It now uploads the document and starts OCR on it.
- Fixed: names with a space in a URL path, such as a configuration or template name, were sent with `+` instead of `%20`.
- Fixed: `waitForOperation` and `waitForGenerativeTask` kept polling after the job ended with the status `TIMEOUT`. They now fail at once.
- Fixed: map-reduce header settings without a header page limit were not sent.
- An API key with a trailing newline is accepted. A key with control characters or characters outside Latin-1 is rejected when the client is built.
- File names, field names and content types of uploads are rejected when they contain a quote or a line break.

### Migration Notes

- `createContractAnalysisTrial(caseId, anchorJobGuid, idempotencyKey, suiteRevision, trialKind)` and the six-parameter form are deprecated: the API requires an estimate fingerprint, which they do not send. Use `createContractAnalysisTrial(caseId, anchorJobGuid, options, idempotencyKey, trialKind, estimateFingerprint)`.
- `submitAndWaitForOperation` and the other new submit-and-wait methods return the operation's result document. `waitForOperation` still returns the status envelope with the result as a string, and can return an earlier operation of the same type under the same parent.
- A failed wait throws `DocuDevsApiException` with status `409` and the polled status in the body. For record imports and tree activations the body holds both the import and its job, so the job's `error` is available.
- A `switch` over `MapReduceOptions.SplitType` without a default branch needs a case for `DOCUMENT_OUTLINE`.
- Path values that are empty, `.` or `..` are rejected with `IllegalArgumentException`.

## 2026-11-16 UTC

### Highlights

- Playbook evaluations explain each failed clause, count missed problems and false alarms, repeat runs to find unstable expectations, and compare with the published playbook.

### Python SDK

- `import_playbook` takes an optional `reason` of at most 500 characters.
- `list_evaluations`, `get_evaluation` and `wait_for_evaluation` read contract-analysis evaluations. `start_evaluation` takes `repeats` (1 to 3).
- Gold `verdict` may be a list of accepted verdicts. Test-suite expectations take `acceptedVerdicts`.
- Evaluation reports add `explanation`, `redFlagReasons` and `acceptedVerdicts` to each clause assertion, `verdictPairs` to each clause, `missedProblems` and `falseAlarms` to the totals, and `unstable`.
- CLI: `playbook import --reason`, `knowledge-base contract-analysis update --reason`, `eval run --repeats`, `eval run --llm-level` (which saves the draft with a reason), `eval report --against published`, `eval list`, and `eval status` for evaluation ids.

### Java SDK

- The generated models include the new fields. `importPlaybook(caseId, draftRevision, playbook, reason)` takes an optional reason, and `startEvaluation(caseId, set, limit, repeats, idempotencyKey)` takes optional `repeats` (1 to 3). Pass `null` to omit either. The existing overloads are unchanged. Sync and async.

### Migration Notes

- A reason is optional. A reason longer than 500 characters returns `400` with the code `REASON_TOO_LONG`.
- Trial idempotency keys that start with `evaluation:` return `400` with the code `RESERVED_IDEMPOTENCY_KEY`.
- `eval status` reads a holdout evaluation for a numeric id, as before, and a contract-analysis evaluation for any other id.
- A list `verdict` in a gold mapping must not be empty.

## 2026-11-16 06:46 UTC

### Highlights
- Draft saves can carry a reason, and the draft change log shows who changed the playbook and why.

### Python SDK
- `update_contract_analysis_draft` takes an optional `reason` of at most 500 characters.
- `list_draft_revisions(case_id)` lists the change log newest first, with each save's actor and reason.
- `get_draft_revision(case_id, revision)` returns the playbook saved at one revision.

### Java SDK
- `updateContractAnalysisDraft(caseId, draftRevision, profile, reason)` takes an optional reason; pass `null` to save without one. Sync and async.
- `listDraftRevisions(caseId)` and `getDraftRevision(caseId, revision)` read the change log and the saved playbooks. Sync and async.

### Migration Notes
- The reason is optional. A reason longer than 500 characters returns `400` with the code `REASON_TOO_LONG`.
- The change-log routes need an API version that includes the draft change log.

<!-- sdk-changelog-fingerprint: c73090afbcbdf10c -->

## 2026-11-06

### Python SDK

- The SDK now requires Python 3.10 or later, from version 1.15.0. Python 3.9 reached end of life in October 2025, and the patched `anyio` needs 3.10. Projects still on Python 3.9 stay on 1.14.0.

## 2026-11-02 18:17 UTC

### Highlights
- Capture hybrid document runs for human review with stable review and result identities.

### Python SDK
- Open named review configurations, record skipped steps, and complete captured runs using the generated review methods.

### Java SDK
- Use generated methods to open and complete captured runs and record skipped steps.

### Migration Notes
- Review capture is opt-in. Existing ordinary document requests retain their current behavior.

<!-- sdk-changelog-fingerprint: 14078145375d6ef4 -->
## 2026-11-02 05:57 UTC

### Highlights
- Python SDK adds opt-in capture support for hybrid client pipeline runs.

### Python SDK
- Add ReviewCaptureOptions and LocalSourceMapping for detached local-node capture, exact remote result decoding, and completion after server publication.
- Preserve request identities and frozen retry bytes across transport retries without rerunning local functions or source mappers.
- Restore generation compatibility for nullable operation-status fields in generated Python models.

### Java SDK
- No Java SDK updates in this change.

### Migration Notes
- Captured hybrid execution requires matching server API routes. Existing pipeline calls remain unchanged when review capture is omitted.

<!-- sdk-changelog-fingerprint: 6a53e0f9ac1911de -->

## 2026-10-27

### Highlights

- Record category statistics support paging and category-code filtering, including large category trees.

### Python SDK

- `record_stats(case_id, page=0, size=50, q="4.12")` returns a category page and global record totals. The CLI accepts `--page`, `--size`, and `--q`.

### Java SDK

- Generated `getCaseRecordStats(caseId, page, size, q)` calls expose the optional paging parameters and response metadata.

### Migration Notes

- Deploy the updated API to use paging. Omit options in the Python helper for the complete response; generated Java calls pass `null` for all three optional parameters to request the complete response.



<!-- sdk-changelog-fingerprint: 2cb42a696088fd98 -->
## 2026-10-27 16:18 UTC

### Highlights
- Added interactive agent conversations for cases and runs.
- New AgentConversation and AgentReply types to manage chat-style agent interactions.
- AgentReply exposes the agent message, tools used, and pending agent actions.
- RecordsCase and AgentRun gained conversation() helpers to start or resume chats.

### Python SDK
- New AgentConversation class: create a chat, send a message, and wait for a reply with reply().
- New AgentReply class: access message, tools_used, and pending_actions from the agent reply.
- RecordsCase.conversation(...) starts a conversation scoped to the case and can resume sessions.
- `AgentRun.conversation()` starts a conversation scoped to the run with context `run:{runId}`.

### Java SDK
- No Java SDK user-facing updates in this commit.

### Migration Notes
- If you import exported names from the package root, add AgentConversation and AgentReply where needed.
- Use RecordsCase.conversation(...) or AgentRun.conversation() to start chats instead of calling client chat methods directly.
- Call reply(text, ...) on an AgentConversation to send one message and receive an AgentReply.
- AgentReply.pending_actions contains AgentAction instances you can inspect and act on.

## 2026-10-16 UTC

### Highlights
- SDK 1.13.0: Record imports and classify-from-file upload through the API in 32 MiB blocks instead of a storage SAS URL, so they work when storage is network-restricted. Requires an API with block uploads; older SDKs cannot upload files to it.

<!-- sdk-changelog-fingerprint: d7b294a60234fac0 -->
## 2026-10-16 09:19 UTC

### Highlights
- 1.12.2: ProcessingJob.operation_status_version and Case.embedding_metadata_version are now optional so reads from older servers no longer raise errors.

### Python SDK
- Released version 1.12.2.
- ProcessingJob.operation_status_version is now optional and will be UNSET when the server does not provide it.
- Case.embedding_metadata_version is now optional and will be UNSET when the server does not provide it.
- Parsing jobs or cases returned by older servers no longer raises KeyError; the missing attributes are represented as UNSET.

### Java SDK
- No Java SDK user-facing updates in this commit.

### Migration Notes
- If your code assumes integer values for operation_status_version or embedding_metadata_version, add checks for Unset before using them.
- Treat UNSET as 'field not provided' rather than a numeric value.
- Current servers still send both fields; only servers that predate them omit them.

<!-- sdk-changelog-fingerprint: d4cc57296e1d6a61 -->
## 2026-10-12 10:07 UTC

### Highlights
- Added a new Contract Analysis Workflow API to report a case's contract-analysis setup state.
- New data models expose build state, pending rebuild details, and change counts for contract-analysis workflows.
- Client libraries now include synchronous and asynchronous helpers to fetch the workflow for a knowledge base case.

### Python SDK
- Added get_contract_analysis_workflow(...) to fetch a case's contract-analysis workflow state.
- New API module docudevs.api.knowledge_base_profiles.get_contract_analysis_workflow provides sync and async helpers.
- Added ContractAnalysisWorkflowResponse, ContractAnalysisBuildState, ContractAnalysisPendingRebuild, and RebuildChangeCounts models.
- The workflow endpoint returns either a ContractAnalysisWorkflowResponse or a ProfileCapabilityUnavailableResponse when the profile is missing.

### Java SDK
- Added DocuDevsClient.getContractAnalysisWorkflow(long caseId) to retrieve contract-analysis workflow state synchronously.
- Added DocuDevsAsyncClient.getContractAnalysisWorkflow(long caseId) to retrieve contract-analysis workflow state asynchronously.

### Migration Notes
- Call get_contract_analysis_workflow(case_id) (Python) or getContractAnalysisWorkflow(caseId) (Java) to obtain the setup step, build status, pending rebuild info, and readiness.
- Inspect the new ContractAnalysisWorkflowResponse model to read fields such as current step, build state, pending rebuild, and readiness flags.
- When the contract-analysis profile is not available, the API may return a ProfileCapabilityUnavailableResponse—handle this case when calling the SDK.
- New models (ContractAnalysisBuildState, ContractAnalysisPendingRebuild, RebuildChangeCounts) are included in the Python package exports; update any type annotations or deserialization handling if you rely on previous workflow representations.

<!-- sdk-changelog-fingerprint: ca7d5121ec49631b -->
## 2026-10-06

### Extraction human review

- Python and Java SDK facades expose stored profile preview/save, exact-run discovery and registration, versioned evaluator claims, human flags/resolutions/adjudications, cursor-based decision feeds, and verified artifact downloads.
- Artifact helpers return raw bytes. Review state remains separate from immutable extraction results; readiness remains `NOT_ASSESSED`.
- See the [extraction review guide](/docs/core/extraction-review) for the supported workflow and current limits.

## 2026-09-23 13:31 UTC

### Highlights

- Contract-analysis results show scoped effective values alongside their original values and supporting evidence.

### Python SDK

- Trial expectations can assert effective attributes for a selected contract item.

### Java SDK

- Generated trial expectation models expose optional expected effective attributes.

### Migration Notes

- Existing trial expectations remain valid; add `expectedEffectiveAttributes` when a trial must verify an override or an unaffected value.

<!-- sdk-changelog-fingerprint: d55dffc8699f8c33 -->
## 2026-09-23 13:00 UTC

### Highlights

- Multi-document package limits distinguish captured members from expected-missing and excluded document metadata.

### Python SDK

- Run helpers allow up to 32 captured members plus separately bounded completeness metadata.

### Java SDK

- Typed synchronous and asynchronous run helpers accept generated package-request models while preserving the legacy single-document helpers.

### Migration Notes

- Existing calls remain compatible. Expected-missing criterion IDs must exist in the captured profile; exclusions do not consume captured-member slots.

<!-- sdk-changelog-fingerprint: cadf3978a076cb64 -->
## 2026-09-23 10:30 UTC

### Highlights

- Contract-analysis runs can now use a bounded package of selected documents with explicit unavailable and excluded members.

### Python SDK

- Python run creation and the CLI accept multi-document package members, expected missing documents, and exclusions.

### Java SDK

- Generated Java run requests include package-member and package-completeness fields.

### Migration Notes

- Existing single-document run calls remain supported; package members must include the anchor and packages are limited to 32 documents and 32 MiB of captured sources.

<!-- sdk-changelog-fingerprint: bb5110d69d4a3170 -->
## 2026-09-03 16:36 UTC

### Highlights
- Added an optional per-request reasoning effort override for model processing.
- Both Python and Java SDKs accept a new reasoningEffort / reasoning_effort parameter on document processing calls.
- The new parameter is sent with requests as reasoningEffort when provided.
- This change is fully optional and preserves existing defaults when not supplied.

### Python SDK
- New optional parameter reasoning_effort added to document processing methods.
- Pass reasoning_effort as a string (for example: "low") to override per-request model reasoning effort.
- When provided, the SDK includes reasoningEffort in the outgoing request payload.
- Leaving reasoning_effort unset preserves previous behavior and defaults.

### Java SDK
- ProcessOptions now accepts a reasoningEffort value via Builder.reasoningEffort(...).
- DocuDevsClient includes reasoningEffort from ProcessOptions in processing requests when set.
- Provide the reasoningEffort string (for example: "low") to override per-request model reasoning effort.
- Not setting reasoningEffort keeps existing defaults and behavior unchanged.

### Migration Notes
- You do not need to change existing calls; reasoning effort is optional and defaults remain the same.
- To enable per-request reasoning effort, pass reasoning_effort (Python) or set reasoningEffort on ProcessOptions (Java).
- The request payload key for this setting is reasoningEffort; ensure any custom request consumers expect that field.

<!-- sdk-changelog-fingerprint: 0212c29a19b67d2e -->
## 2026-09-02 15:17 UTC

### Highlights

- Operation status responses now preserve an explicit `null` runtime status instead of failing during Python SDK deserialization.

### Python SDK

- `OperationInfo`, `InternalOperationStatusResponse`, and `ProcessingJob` safely round-trip nullable operation status values.

### Java SDK

- No Java SDK changes are required for this compatibility fix.

### Migration Notes

- No application changes are required.

<!-- sdk-changelog-fingerprint: 99878f9cd04f305f -->
## 2026-09-02 12:02 UTC

### Highlights

- Contract-analysis status responses now include the current stage, document and chunk progress, and warning count.

### Python SDK

- Operation status models now expose contract-analysis runtime progress and status-version data.

### Java SDK

- Generated operation status models expose the same contract-analysis runtime progress fields.

### Migration Notes

- No breaking SDK changes are required; all new status fields are optional.

## 2026-08-30

### Knowledge-base profile summaries

- Knowledge-base list and lookup responses now include optional, bounded
  `profileType`, `profileStatus`, and positive `publishedRevision` summary
  fields when an organization-scoped active profile is available.
- The agent uses these summaries to route document evaluation to the
  contract-analysis operation only for an active published profile. The API
  selects the revision and source set; profile artifacts, draft content, and
  source identifiers are not returned, and agent tools do not publish or edit
  profiles.

<!-- sdk-changelog-fingerprint: 651a3928389603bc -->
## 2026-08-26 17:46 UTC

### Highlights
- New verify endpoint for LLM providers added to the Python SDK.
- Model credential create and delete endpoints now return richer, typed error/response objects.
- Model credential resource IDs are accepted as strings for delete operations in the Python SDK.
- New and updated model and error DTOs added to represent credential status, validation, and delete conflicts.

### Python SDK
- Added `/llm/providers/{id}/verify` endpoint functions for synchronous and async verification calls.
- Create model credential responses may now return either a ModelCredentialDto or a ModelCredentialErrorResponse.
- Delete model credential operation now treats successful delete with 204 No Content and may return ModelCredentialDeleteConflict on conflict.
- Delete model credential ID parameter is now accepted as a string.

### Java SDK
- Java DTOs are validated to keep secret inputs write-only on create/update requests.

### Migration Notes
- If you handle create_model_credential responses, update your code to accept either a successful ModelCredentialDto or a ModelCredentialErrorResponse and branch accordingly.
- Update delete_model_credential handlers to accept 204 No Content as success and to handle ModelCredentialDeleteConflict when a 409 conflict is returned.
- When calling the delete model credential function in Python, pass the credential ID as a string.
- If you relied on any previously undocumented status codes or response shapes, review the new typed results for create and delete operations and adjust error handling.

<!-- sdk-changelog-fingerprint: e935869c57fdd86e -->
## 2026-08-21 06:59 UTC

### Highlights
- Clarifies where to edit manual client files and how they are synchronized with generated packages.

### Python SDK
- Generate API code using the repository-root docudevs.yaml file.
- Use the provided Makefile to copy manual sources into the generated docudevs/ package and to synchronize generated and manual files.

### Java SDK
- No Java SDK user-facing updates in this commit.

### Migration Notes
- If you maintain manual client files, ensure you edit the root-level sources and then run the Makefile to propagate them into the packaged/generated module.

<!-- sdk-changelog-fingerprint: bff3298225eba84b -->
## 2026-06-11 12:35 UTC

### Highlights
- SDK usability improvements for common document processing flows.

### Python SDK
- submit_pdf_acroform_operation no longer accepts CommonForms-related parameters (use_commonforms, commonforms_model, commonforms_fast, commonforms_confidence).
- submit_and_wait_for_pdf_acroform_operation no longer accepts CommonForms-related parameters.

### Java SDK
- No Java SDK user-facing updates in this commit.

### Migration Notes
- If your code passed CommonForms-related keyword arguments to submit_pdf_acroform_operation or submit_and_wait_for_pdf_acroform_operation, remove those arguments and rely on the remaining public parameters.
- Expectation: PDF acroform submissions should continue to work using the remaining public parameters (min_confidence, max_fields_per_page, force_ocr, etc.).
- If you relied on toggling CommonForms behavior via the public Python SDK methods, please contact support for recommended alternatives or configuration options.

<!-- sdk-changelog-fingerprint: a3cdaf7df4d2f76f -->
## 2026-06-04 05:24 UTC

### Highlights
- Added first-class support for a new pdf-acroform operation to generate fillable PDFs from completed jobs.
- CLI gained a new operations command `pdf-acroform` to submit and retrieve AcroForm PDFs.
- Client now exposes methods to submit pdf-acroform operations and to download generated AcroForm PDFs.

### Python SDK
- New async method submit_pdf_acroform_operation(...) to submit a pdf-acroform operation for an existing job.
- New async method get_pdf_acroform(operation_job_guid, save_to=None) to download the generated AcroForm PDF and optionally save it to disk.
- New PdfAcroFormOperationResult dataclass representing operation metadata and PDF bytes.
- Client method validates inputs: page_range must use 1-based positive integers, confidences must be between 0 and 1, and max_fields_per_page must be >= 1.

### Java SDK
- No Java SDK user-facing updates in this commit.

### Migration Notes
- Adjust any code that constructs page ranges to use 1-based page numbers and ensure values are positive integers.
- Pass valid confidence values in the 0..1 range for min_confidence and commonforms_confidence to avoid validation errors.
- When using custom parameters for pdf-acroform operations, fieldNameStyle defaults to source_snake_case and OCR mode defaults to PREMIUM unless overridden.

<!-- sdk-changelog-fingerprint: 05f941329cd11805 -->
## 2026-06-01 15:00 UTC

### Highlights
- Added Python SDK and CLI helpers for the `pdf-acroform` operation.

### Python SDK
- New `submit_pdf_acroform_operation`, `submit_and_wait_for_pdf_acroform_operation`, and `get_pdf_acroform` helpers.
- Operation custom parameters now preserve booleans, numbers, and lists instead of coercing every value to a string.

### CLI
- New `docudevs operations pdf-acroform` command for submitting the operation and saving the generated fillable PDF.

## 2026-06-01 13:00 UTC

### Highlights
- New submission portals and related APIs added to the Python SDK.
- Added APIs for extracting and retrieving AcroForm metadata and usage events in the Python SDK.

### Python SDK
- New submission portals endpoints: create, update, delete, get by slug, get portal, list portals, get default for configuration, download, submit, and get record.
- New models for submission portals and submission records are included (create/update requests, response types, download and submit types).
- Added APIs to extract and retrieve AcroForm metadata for documents and jobs.
- Added billing usage event retrieval API.

### Java SDK
- Java SDK improvements focused on easier day-to-day usage.

### Migration Notes
- Python users: new submission portal APIs introduce new request/response models—update any direct payloads to use the provided models where possible.

<!-- sdk-changelog-fingerprint: b7de32ab8c2b1bde -->
## 2026-06-01 14:43 UTC

### Highlights
- New pdf-acroform operation: convert completed PDF jobs into fillable AcroForm PDFs.
- CLI command added to run pdf-acroform operations and optionally save the resulting PDF.
- Client APIs to submit, poll, and download pdf-acroform results are available.
- Input validation for page ranges, confidence, and field limits is performed client-side.

### Python SDK
- Added DocuDevsClient.submit_pdf_acroform_operation to submit a pdf-acroform operation for an existing PDF job.
- Added DocuDevsClient.submit_and_wait_for_pdf_acroform_operation to submit, wait for completion, and download the AcroForm PDF.
- Added DocuDevsClient.get_pdf_acroform to download the generated AcroForm PDF as bytes or save it to a file.
- submit_pdf_acroform_operation accepts llm_type, ocr, page_range (1-based), min_confidence, max_fields_per_page, and force_ocr parameters.

### Java SDK
- No Java SDK user-facing updates in this commit.

### Migration Notes
- If you need a fillable PDF from a completed job, use submit_pdf_acroform_operation or submit_and_wait_for_pdf_acroform_operation.
- min_confidence must be within [0, 1] and max_fields_per_page must be >= 1; adjust callers accordingly.
- CLI: the new operations pdf-acroform command supports --page, --min-confidence, --max-fields-per-page, --force-ocr, --ocr, --llm-type, and --output.
- When using the CLI --output option, the command must run in wait mode (default) so the file is saved after completion.

<!-- sdk-changelog-fingerprint: 84d924bdf9d28b00 -->
## 2026-05-18 10:53 UTC

### Highlights
- New endpoints for uploading, downloading, and deleting batch lookup files.

### Python SDK
- Added batch.upload_batch_lookup_file for posting multipart lookup files to a batch.
- Added batch.download_batch_lookup_file to retrieve batch lookup file contents as a list of strings.
- Added batch.delete_batch_lookup_file to remove a batch lookup file.
- Updated batch.process_batch_with_configuration to accept an optional trace parameter.

### Java SDK
- No Java SDK user-facing updates in this commit.

### Migration Notes
- When uploading batch lookup files, use the new UploadBatchLookupFileBody helper to supply multipart file content.
- Download of batch lookup files now returns a list of strings representing the file content lines; adjust parsing accordingly.
- Existing code calling process endpoints without the trace parameter will continue to work unchanged.

<!-- sdk-changelog-fingerprint: a0744c1178175726 -->
## 2026-05-08 06:48 UTC

### Highlights
- Added a new tool type value: REFERENCE_DICTIONARY_LOOKUP.

### Python SDK
- ToolType now includes 'REFERENCE_DICTIONARY_LOOKUP' as a valid value.

### Java SDK
- No Java SDK user-facing updates in this commit.

### Migration Notes
- If you validate tool types, allow 'REFERENCE_DICTIONARY_LOOKUP' as an accepted value.

<!-- sdk-changelog-fingerprint: cf7d832091a9fe8c -->
## 2026-05-04 04:51 UTC

### Highlights
- Added APIs to manage batch and configuration lookup files.
- Added ability to process batches using named configurations.
- Async client now supports the new batch/configuration and lookup file operations.
- Build task adjusted to ensure staging directory is cleared before publishing.

### Python SDK
- No changes in the Python SDK surface in this release.

### Java SDK
- New methods to upload, download, and delete batch lookup files.
- New methods to upload, download, and delete configuration lookup files.
- New method to process a batch using a named configuration.
- Async client has matching asynchronous methods for the new operations.

### Migration Notes
- If you use the Java async client, you can call the new async methods to perform lookup file operations and configuration-based batch processing.
- When uploading lookup files, provide filename, content type, and bytes in an UploadRequest; the client sends a multipart/form-data request.
- Processing a batch with a configuration requires both the batch GUID and the configuration name.
- Existing synchronous code can call the new synchronous methods; async equivalents are available via the async client.

<!-- sdk-changelog-fingerprint: 9d0e483e7f917fb1 -->
## 2026-04-22 08:08 UTC

### Highlights
- Add support for configuration-backed batch processing via a new --configuration option for batch commands.
- Allow attaching a per-batch lookup file via a new --lookup-file option for batch processing.
- Add CLI commands to manage configuration lookup files: upload and delete.
- Expose new API methods for uploading, downloading, and deleting configuration lookup files.

### Python SDK
- New functions to upload, download, and delete configuration lookup files are available.
- New API to process a batch using a named configuration is available.
- Batch processing supports an optional batch-scoped lookup file that overrides configuration lookup data.
- CLI now includes configuration lookup-file commands: `upload <name> <file>` and `delete <name>`.

### Java SDK
- No Java SDK user-facing updates in this commit.

### Migration Notes
- If you switch to --configuration for batch processing, do not provide prompt, schema, processing flags, OCR, or LLM options in the same request.
- To override configuration lookup data only for a single batch, use the new --lookup-file option when starting the batch.
- When using the CLI upload command for configuration lookup files, provide the configuration name and the file path.
- Existing batch workflows that supplied prompt/schema or processing flags should continue using the previous behavior (omit --configuration).

<!-- sdk-changelog-fingerprint: 39d3f9856fb2e68e -->
## 2026-04-13 18:00 UTC

### Highlights
- Added Excel template download endpoint for configurations.
- Added integration (secrets) endpoints to create, list, and delete secrets.
- Added document outline retrieval for job results with optional batch index.

### Python SDK
- New function to download Excel templates for a configuration by name.
- New integration API: create_or_update secret endpoint returning a SecretSummary.
- New integration API: list secrets endpoint returning a list of SecretSummary objects.
- New integration API: delete secret endpoint by name.

### Java SDK
- No Java SDK user-facing updates in this commit.

### Migration Notes
- If you rely on configuration Excel templates, use the new download endpoint with the configuration name to retrieve template columns.
- Use the new integration secrets endpoints to create, list, and remove secrets; creating a secret returns a SecretSummary object.
- Managed execution functionality is available via new endpoints for dispatch, cancellation, retrieval, listing, and logs—update any client usage to call these new operations.
- Review the new models added to ensure any typed code or data mappings include the new types and fields.
