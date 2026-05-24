---
title: SDK Changelog
description: User-focused updates for the DocuDevs Python and Java SDKs.
---

# SDK Changelog





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
