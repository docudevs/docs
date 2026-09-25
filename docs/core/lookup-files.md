---
title: Lookup Files and Excel Templates
description: Attach reference lookup data and Excel export templates to a named configuration, or override the lookup data for a single batch.
---

import Tabs from '@theme/Tabs';
import TabItem from '@theme/TabItem';

A **lookup file** is a reference dataset — a spreadsheet, delimited file, or short document — attached to a named configuration and searched by the `LOOKUP_FILE_SEARCH` tool during extraction, to resolve schema values that are not present in the source document itself. A **batch lookup file** is the same mechanism scoped to a single batch run. An **Excel template** is a separate, unrelated attachment used only when exporting a result to `.xlsx` — it has no effect on extraction.

## Configuration lookup files

### Upload, fetch, and delete

<Tabs
  defaultValue="python"
  values={[
    {label: 'Python SDK', value: 'python'},
    {label: 'CLI', value: 'cli'},
    {label: 'cURL', value: 'curl'},
  ]}>
  <TabItem value="python">

```python
from docudevs.docudevs_client import DocuDevsClient
import os

client = DocuDevsClient(token=os.getenv('API_KEY'))

await client.upload_configuration_lookup_file("employees", open("employees.csv", "rb"))
raw_bytes = await client.get_configuration_lookup_file("employees")
await client.delete_configuration_lookup_file("employees")
```

  </TabItem>
  <TabItem value="cli">

```bash
docudevs configuration lookup-file upload employees employees.csv
docudevs configuration lookup-file delete employees
```

There is no CLI command to download a configuration's lookup file.

  </TabItem>
  <TabItem value="curl">

```bash
curl -X POST "https://api.docudevs.ai/configuration/employees/lookup-file" \
  -H "Authorization: $API_KEY" \
  -F "file=@employees.csv"

curl "https://api.docudevs.ai/configuration/employees/lookup-file" \
  -H "Authorization: $API_KEY" -o employees.csv

curl -X DELETE "https://api.docudevs.ai/configuration/employees/lookup-file" \
  -H "Authorization: $API_KEY"
```

  </TabItem>
</Tabs>

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

<Tabs
  defaultValue="python"
  values={[
    {label: 'Python SDK', value: 'python'},
    {label: 'CLI', value: 'cli'},
    {label: 'cURL', value: 'curl'},
  ]}>
  <TabItem value="python">

```python
guid = await client.submit_and_process_document_with_configuration(
    open("intake.pdf", "rb"),
    "application/pdf",
    "employees",
)
result = await client.wait_until_ready(guid, result_format="json")
```

Equivalently, upload first and process the existing GUID against the configuration with `process_document_with_configuration(guid, configuration="employees")`.

  </TabItem>
  <TabItem value="cli">

```bash
docudevs process intake.pdf --configuration employees
```

  </TabItem>
  <TabItem value="curl">

```bash
GUID=$(curl -s -X POST https://api.docudevs.ai/document/upload \
  -H "Authorization: $API_KEY" \
  -F "document=@intake.pdf" | jq -r .guid)

curl -X POST "https://api.docudevs.ai/document/process/$GUID/with-configuration/employees" \
  -H "Authorization: $API_KEY"
```

  </TabItem>
</Tabs>

The configuration's own saved extraction mode must be `SIMPLE` — lookup files are only supported with `extractionMode=simple`.

## Batch lookup file override

A batch can carry its own lookup file that overrides the configuration's lookup file for that one batch run — or supply a lookup file to a plain (non-configuration) batch. It must be uploaded before the batch is processed.

```python
await client.upload_batch_lookup_file(batch_guid, open("employees-2024.csv", "rb"))
raw_bytes = await client.get_batch_lookup_file(batch_guid)
await client.delete_batch_lookup_file(batch_guid)
```

REST: `GET`/`POST`/`DELETE /document/batch/{guid}/lookup-file`. CLI, as part of `batch process`:

```bash
docudevs batch process doc1.pdf doc2.pdf \
  --configuration employees \
  --lookup-file employees-2024.csv
```

`--lookup-file` works with or without `--configuration`: the batch-scoped file is used whenever present, and otherwise the configuration's own lookup file (if any) applies. The same file-type and `extractionMode=simple` rules as the configuration lookup file apply.

## Excel export templates

An Excel template only affects the `.xlsx` export of a completed job's result (`result_excel`) — it plays no part in extraction. Setting it up takes two independent steps, both required:

1. **Save the write-position config** on the named configuration itself, as part of its `UploadCommand`:

   ```python
   from docudevs.models.upload_command import UploadCommand
   from docudevs.models.excel_template_config import ExcelTemplateConfig

   await client.save_configuration(
       "employees",
       body=UploadCommand(
           prompt="Extract each employee row.",
           schema="...",
           excel_template=ExcelTemplateConfig(
               start_row=0,
               start_col=0,
               include_headers=True,
               sheet_name="Data",
           ),
       ),
   )
   ```

   `start_row` and `start_col` are 0-based cell coordinates, so `start_row=0, start_col=0` targets cell A1. `sheets` (optional) can instead list several `{sheetName, startRow, startCol, includeHeaders}` targets to write the same result into multiple sheets of the template in one export.

2. **Upload the template file itself:**

   ```python
   await client.upload_excel_template("employees", open("template.xlsx", "rb"))
   ```

If the configuration has no `excel_template` config saved (step 1), `result_excel` silently falls back to a plain generated spreadsheet even if a template file was uploaded — both steps are required for the template to take effect.

### Fetching the result

```python
xlsx_bytes = await client.result_excel(job_guid, configuration="employees")
```

REST: `GET /job/result/{uuid}/excel?configuration=employees`. There is currently no working `DocuDevsClient` helper to download the template file you uploaded back out — the generated low-level client's response model for `GET /configuration/{name}/excel-template` does not match the raw binary response the server actually returns; call that endpoint directly if you need to retrieve it. There is no CLI command for uploading or downloading an Excel template.

## Schemas

- `ExcelTemplateConfig`: `startRow` (int, required), `startCol` (int, required), `includeHeaders` (bool, required), `sheetName` (string, optional), `sheets` (array of `SheetWriteTarget`, optional).
- `LookupFileConfig`: `topK` (int, required), `fileName`, `contentType`, `uploadedAt`, `runtimeArtifactType` (all optional, reported back after upload).

Both are nested fields (`excelTemplate`, `lookupFile`) on the named configuration's stored request body.
