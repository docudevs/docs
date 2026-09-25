---
title: SDK Changelog
description: User-focused updates for the DocuDevs Python and Java SDKs.
---

# SDK Changelog

<!-- sdk-changelog-fingerprint: d4cc57296e1d6a61 -->
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
