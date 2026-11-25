---
title: Lookup Files and Excel Templates
description: Attach reference lookup data and Excel export templates to a named configuration, or override the lookup data for a single batch.
---

A **lookup file** is a reference dataset — a spreadsheet, delimited file, or short document — attached to a named configuration and searched by the `LOOKUP_FILE_SEARCH` tool during extraction, to resolve schema values that are not present in the source document itself. A **batch lookup file** is the same mechanism scoped to a single batch run. An **Excel template** is a separate, unrelated attachment used only when exporting a result to `.xlsx` — it has no effect on extraction.

## Configuration lookup files

### Upload, fetch, and delete

<Example name="lookup-files/manage-configuration-lookup-file" />

Deleting the file at the end removes the configuration's lookup data. The CLI has no command to download a configuration's lookup file.

### Accepted file types

The uploaded file is preprocessed into a runtime search artifact at upload time:

| Extension | Runtime form | Tool result shape |
| --- | --- | --- |
| `.csv`, `.tsv`, `.xlsx`, `.xls` | Rows converted to JSONL, one JSON object per row | `{"score": ..., "row": {...}}` |
| `.txt`, `.md` | Left as text, split into passages on blank lines | `{"score": ..., "passage": "..."}` |
| `.docx` | Text extracted from the document, split into passages | `{"score": ..., "passage": "..."}` |

Any other extension is rejected with a 400. There is no separate endpoint or parameter to set a search result limit: it defaults to 8 and is not currently exposed as a caller-adjustable option on the upload call.

### Using the tool during extraction

`LOOKUP_FILE_SEARCH` cannot be attached by hand in a request's `tools` array — the API rejects it with a 400 (`"LOOKUP_FILE_SEARCH is only supported via named configurations"`) unless the job is processed through a named configuration. When you process a document **through the configuration**, the server automatically copies the configuration's lookup file into the job and appends a fully-resolved `LOOKUP_FILE_SEARCH` tool descriptor for you — you never construct that descriptor yourself.

<Example name="configuration/process-with-configuration" />

The configuration's own saved extraction mode must be `SIMPLE` — lookup files are only supported with `extractionMode=simple`.

## Batch lookup file override

A batch can carry its own lookup file that overrides the configuration's lookup file for that one batch run — or supply a lookup file to a plain (non-configuration) batch. It must be uploaded before the batch is processed. The batch endpoints are `GET`, `POST` and `DELETE` on `/document/batch/{guid}/lookup-file`:

<Example name="lookup-files/manage-batch-lookup-file" />

The batch-scoped file is used whenever present, and otherwise the configuration's own lookup file (if any) applies. The same file-type and `extractionMode=simple` rules as the configuration lookup file apply. The CLI uploads the batch lookup file as part of `batch process` with `--lookup-file`, with or without `--configuration`. The full flow, from creating the batch to reading the results, looks like this:

<Example name="lookup-files/process-batch-with-lookup-file" />

## Excel export templates

An Excel template only affects the `.xlsx` export of a completed job's result — it plays no part in extraction. Setting it up takes two independent steps, both required:

1. **Save the write-position config** on the named configuration itself, as part of its `UploadCommand` request body (`excelTemplate`):

   <Example name="lookup-files/save-excel-template-config" />

   `startRow` and `startCol` are 0-based cell coordinates, so `0` and `0` target cell A1. `sheets` (optional) can instead list several `{sheetName, startRow, startCol, includeHeaders}` targets to write the same result into multiple sheets of the template in one export.

2. **Upload the template file itself:**

   <Example name="lookup-files/upload-excel-template" />

If the configuration has no `excelTemplate` config saved (step 1), the Excel result silently falls back to a plain generated spreadsheet even if a template file was uploaded — both steps are required for the template to take effect.

### Fetching the result

Pass the configuration name when you fetch the Excel result of a completed job:

<Example name="lookup-files/download-excel-result" />

There is currently no working `DocuDevsClient` helper to download the template file you uploaded back out — the generated low-level client's response model for `GET /configuration/{name}/excel-template` does not match the raw binary response the server actually returns; call that endpoint directly if you need to retrieve it. There is no CLI command for uploading or downloading an Excel template.

## Schemas

- `ExcelTemplateConfig`: `startRow` (int, required), `startCol` (int, required), `includeHeaders` (bool, required), `sheetName` (string, optional), `sheets` (array of `SheetWriteTarget`, optional).
- `LookupFileConfig`: `topK` (int, required), `fileName`, `contentType`, `uploadedAt`, `runtimeArtifactType` (all optional, reported back after upload).

Both are nested fields (`excelTemplate`, `lookupFile`) on the named configuration's stored request body.
