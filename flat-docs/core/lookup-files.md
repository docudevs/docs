---
title: Lookup Files and Excel Templates
description: Attach reference lookup data and Excel export templates to a named configuration, or override the lookup data for a single batch.
---
<!-- Generated from docs/core/lookup-files.md. Do not edit by hand. -->

A **lookup file** is a reference dataset — a spreadsheet, delimited file, or short document — attached to a named configuration and searched by the `LOOKUP_FILE_SEARCH` tool during extraction, to resolve schema values that are not present in the source document itself. A **batch lookup file** is the same mechanism scoped to a single batch run. An **Excel template** is a separate, unrelated attachment used only when exporting a result to `.xlsx` — it has no effect on extraction.

## Configuration lookup files

### Upload, fetch, and delete

**cURL**

```bash
curl -s -S -f -o /dev/null -w "Upload status: %{http_code}\n" -X POST "https://api.docudevs.ai/configuration/invoice-config/lookup-file" \
  -H "Authorization: $API_KEY" \
  -F "file=@vendors.csv"

curl -s -S -f -o vendors-downloaded.csv "https://api.docudevs.ai/configuration/invoice-config/lookup-file" \
  -H "Authorization: $API_KEY"

curl -s -S -f -o /dev/null -w "Delete status: %{http_code}\n" -X DELETE "https://api.docudevs.ai/configuration/invoice-config/lookup-file" \
  -H "Authorization: $API_KEY"
```

**Python**

```python
with open("vendors.csv", "rb") as f:
    lookup_data = f.read()

upload = await client.upload_configuration_lookup_file(
    "invoice-config", lookup_data, file_name="vendors.csv"
)
print(f"Upload status: {upload.status_code}")

downloaded = await client.get_configuration_lookup_file("invoice-config")
Path("vendors-downloaded.csv").write_bytes(downloaded)

delete = await client.delete_configuration_lookup_file("invoice-config")
print(f"Delete status: {delete.status_code}")
```

**Java**

```java
import ai.docudevs.client.UploadRequest;
import java.nio.file.Files;
import java.nio.file.Path;

byte[] lookupData = Files.readAllBytes(Path.of("vendors.csv"));

// The Java client returns no status code: each call throws DocuDevsApiException unless the API answers 2xx
client.uploadConfigurationLookupFile(
        "invoice-config", new UploadRequest("vendors.csv", "text/csv", lookupData));
System.out.println("Uploaded vendors.csv");

byte[] downloaded = client.downloadConfigurationLookupFile("invoice-config");
Files.write(Path.of("vendors-downloaded.csv"), downloaded);

client.deleteConfigurationLookupFile("invoice-config");
System.out.println("Deleted the configuration lookup file");
```

**CLI**

```bash
docudevs configuration lookup-file upload invoice-config vendors.csv

# The CLI has no command to download a lookup file.

docudevs configuration lookup-file delete invoice-config
```

Deleting the file at the end removes the configuration's lookup data. The CLI has no command to download a configuration's lookup file.

### Accepted file types

The uploaded file is preprocessed into a runtime search artifact at upload time:

| Extension | Runtime form | Tool result shape |
| --- | --- | --- |
| `.csv`, `.tsv`, `.xlsx`, `.xls` | Rows converted to JSONL, one JSON object per row | `{"score": ..., "row": {...}}` |
| `.txt`, `.md` | Left as text, split into passages on blank lines | `{"score": ..., "passage": "..."}` |
| `.docx` | Text extracted from the document, split into passages | `{"score": ..., "passage": "..."}` |

Any other extension is rejected with a 400. There is no separate endpoint or parameter to set a search result limit: it defaults to 8 and is not exposed as a caller-adjustable option on the upload call.

### Using the tool during extraction

`LOOKUP_FILE_SEARCH` cannot be attached by hand in a request's `tools` array — the API rejects it with a 400 (`"LOOKUP_FILE_SEARCH is only supported via named configurations"`) unless the job is processed through a named configuration. When you process a document **through the configuration**, the server automatically copies the configuration's lookup file into the job and appends a fully-resolved `LOOKUP_FILE_SEARCH` tool descriptor for you — you never construct that descriptor yourself.

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

JOB_ID=$(curl -s -S -f -X POST "https://api.docudevs.ai/document/upload" \
  -H "Authorization: $API_KEY" \
  -F "document=@invoice.pdf" | jq -r '.guid')

# Process the uploaded document with the saved configuration
curl -s -S -f -o /dev/null -X POST "https://api.docudevs.ai/document/process/$JOB_ID/with-configuration/invoice-config" \
  -H "Authorization: $API_KEY"

wait_for_job "$JOB_ID"

curl -s -S -f "https://api.docudevs.ai/job/result/$JOB_ID/json" \
  -H "Authorization: $API_KEY" | jq .
```

**Python**

```python
with open("invoice.pdf", "rb") as f:
    document_data = f.read()

# Process the document using the saved configuration
job_guid = await client.submit_and_process_document_with_configuration(
    document=document_data,
    document_mime_type="application/pdf",
    configuration_name="invoice-config",
)

result = await client.wait_until_ready(job_guid, result_format="json")
print(json.dumps(result, indent=2))
```

**Java**

```java
import ai.docudevs.client.UploadRequest;
import ai.docudevs.client.WaitOptions;
import com.fasterxml.jackson.databind.JsonNode;

byte[] fileBytes = Files.readAllBytes(Path.of("invoice.pdf"));
UploadRequest upload = new UploadRequest("invoice.pdf", "application/pdf", fileBytes);

String guid = client.uploadDocument(upload);
client.processDocumentWithConfiguration(guid, "invoice-config");

JsonNode result = client.waitUntilReadyJson(guid, WaitOptions.builder()
        .timeout(Duration.ofSeconds(180))
        .pollInterval(Duration.ofSeconds(5))
        .build());
System.out.println(result);
```

**CLI**

```bash
docudevs process invoice.pdf --configuration invoice-config
```

The configuration's own saved extraction mode must be `SIMPLE` — lookup files are only supported with `extractionMode=simple`.

## Batch lookup file override

A batch can carry its own lookup file that overrides the configuration's lookup file for that one batch run — or supply a lookup file to a plain (non-configuration) batch. It must be uploaded before the batch is processed. The batch endpoints are `GET`, `POST` and `DELETE` on `/document/batch/{guid}/lookup-file`:

**cURL**

```bash
BATCH_GUID="<batch-guid>"  # GUID of a batch whose documents are uploaded and not yet processed

curl -s -S -f -o /dev/null -w "Upload status: %{http_code}\n" -X POST "https://api.docudevs.ai/document/batch/$BATCH_GUID/lookup-file" \
  -H "Authorization: $API_KEY" \
  -F "file=@vendors-2024.csv"

curl -s -S -f -o vendors-downloaded.csv "https://api.docudevs.ai/document/batch/$BATCH_GUID/lookup-file" \
  -H "Authorization: $API_KEY"

curl -s -S -f -o /dev/null -w "Delete status: %{http_code}\n" -X DELETE "https://api.docudevs.ai/document/batch/$BATCH_GUID/lookup-file" \
  -H "Authorization: $API_KEY"
```

**Python**

```python
batch_guid = "<batch-guid>"  # GUID of a batch whose documents are uploaded and not yet processed

with open("vendors-2024.csv", "rb") as f:
    lookup_data = f.read()

upload = await client.upload_batch_lookup_file(
    batch_guid, lookup_data, file_name="vendors-2024.csv"
)
print(f"Upload status: {upload.status_code}")

downloaded = await client.get_batch_lookup_file(batch_guid)
Path("vendors-downloaded.csv").write_bytes(downloaded)

delete = await client.delete_batch_lookup_file(batch_guid)
print(f"Delete status: {delete.status_code}")
```

**Java**

```java
import ai.docudevs.client.UploadRequest;
import java.nio.file.Files;
import java.nio.file.Path;

String batchGuid = "<batch-guid>"; // GUID of a batch whose documents are uploaded and not yet processed

byte[] lookupData = Files.readAllBytes(Path.of("vendors-2024.csv"));

// The Java client returns no status code: each call throws DocuDevsApiException unless the API answers 2xx
client.uploadBatchLookupFile(batchGuid, new UploadRequest("vendors-2024.csv", "text/csv", lookupData));
System.out.println("Uploaded vendors-2024.csv");

byte[] downloaded = client.downloadBatchLookupFile(batchGuid);
Files.write(Path.of("vendors-downloaded.csv"), downloaded);

client.deleteBatchLookupFile(batchGuid);
System.out.println("Deleted the batch lookup file");
```

The batch-scoped file is used whenever present, and otherwise the configuration's own lookup file (if any) applies. The same file-type and `extractionMode=simple` rules as the configuration lookup file apply. The CLI uploads the batch lookup file as part of `batch process` with `--lookup-file`, with or without `--configuration`. The full flow, from creating the batch to reading the results, looks like this:

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

BATCH_GUID=$(curl -s -S -f -X POST "https://api.docudevs.ai/document/batch" \
  -H "Authorization: $API_KEY" \
  -H "Content-Type: application/json" \
  -d '{"maxConcurrency": 3}' | jq -r '.jobGuid')

for FILENAME in invoice1.pdf invoice2.pdf; do
  curl -s -S -f -o /dev/null -X POST "https://api.docudevs.ai/document/batch/$BATCH_GUID/upload" \
    -H "Authorization: $API_KEY" \
    -F "document=@$FILENAME"
done

# The batch lookup file overrides the lookup file of invoice-config for this batch
curl -s -S -f -o /dev/null -X POST "https://api.docudevs.ai/document/batch/$BATCH_GUID/lookup-file" \
  -H "Authorization: $API_KEY" \
  -F "file=@vendors-2024.csv"

curl -s -S -f -o /dev/null -X POST "https://api.docudevs.ai/document/batch/$BATCH_GUID/process/with-configuration/invoice-config" \
  -H "Authorization: $API_KEY"

wait_for_job "$BATCH_GUID"

curl -s -S -f "https://api.docudevs.ai/job/result/$BATCH_GUID/json" \
  -H "Authorization: $API_KEY" | jq .
```

**Python**

```python
batch_guid = await client.create_batch(max_concurrency=3)

for filename in ["invoice1.pdf", "invoice2.pdf"]:
    with open(filename, "rb") as f:
        await client.upload_batch_document(
            batch_guid=batch_guid,
            document=f.read(),
            mime_type="application/pdf",
            file_name=filename,
        )

# The batch lookup file overrides the lookup file of invoice-config for this batch
with open("vendors-2024.csv", "rb") as f:
    await client.upload_batch_lookup_file(batch_guid, f.read(), file_name="vendors-2024.csv")

await client.process_batch_with_configuration(batch_guid, "invoice-config")

results = await client.wait_until_ready(batch_guid, timeout=600, result_format="json")
print(json.dumps(results, indent=2))
```

**Java**

```java
import ai.docudevs.client.UploadRequest;
import ai.docudevs.client.WaitOptions;
import com.fasterxml.jackson.databind.JsonNode;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.util.List;

String batchGuid = client.createBatch(3);

for (String filename : List.of("invoice1.pdf", "invoice2.pdf")) {
    byte[] documentData = Files.readAllBytes(Path.of(filename));
    client.uploadBatchDocument(
            batchGuid,
            new UploadRequest(filename, "application/pdf", documentData));
}

// The batch lookup file overrides the lookup file of invoice-config for this batch
byte[] lookupData = Files.readAllBytes(Path.of("vendors-2024.csv"));
client.uploadBatchLookupFile(batchGuid, new UploadRequest("vendors-2024.csv", "text/csv", lookupData));

client.processBatchWithConfiguration(batchGuid, "invoice-config");

JsonNode results = client.waitUntilReadyJson(
        batchGuid,
        WaitOptions.builder()
                .timeout(Duration.ofSeconds(600))
                .pollInterval(Duration.ofSeconds(5))
                .build());
System.out.println(results.toPrettyString());
```

**CLI**

```bash
docudevs batch process invoice1.pdf invoice2.pdf \
  --configuration invoice-config \
  --lookup-file vendors-2024.csv
```

## Excel export templates

An Excel template only affects the `.xlsx` export of a completed job's result — it plays no part in extraction. Setting it up takes two independent steps, both required:

1. **Save the write-position config** on the named configuration itself, as part of its `UploadCommand` request body (`excelTemplate`):

**cURL**

```bash
SCHEMA='{
  "type": "object",
  "properties": {
    "vendor_name": {"type": "string"},
    "invoice_number": {"type": "string"},
    "total_amount": {"type": "number"}
  },
  "required": ["total_amount"]
}'

# startRow and startCol are 0-based cell coordinates: 0 and 0 target cell A1
jq -n \
  --arg schema "$SCHEMA" \
  '{
    prompt: "Extract the vendor, invoice number and total of each invoice.",
    schema: $schema,
    excelTemplate: {startRow: 0, startCol: 0, includeHeaders: true, sheetName: "Data"}
  }' |
  curl -s -S -f -o /dev/null -w "Configuration saved (status %{http_code})\n" -X POST "https://api.docudevs.ai/configuration/invoice-config" \
    -H "Authorization: $API_KEY" \
    -H "Content-Type: application/json" \
    --data-binary @-
```

**Python**

```python
from pydantic import BaseModel

from docudevs import json_schema
from docudevs.models import ExcelTemplateConfig, UploadCommand

class Invoice(BaseModel):
    vendor_name: str | None = None
    invoice_number: str | None = None
    total_amount: float

config = UploadCommand(
    prompt="Extract the vendor, invoice number and total of each invoice.",
    schema=json_schema(Invoice),
    # start_row and start_col are 0-based cell coordinates: 0 and 0 target cell A1
    excel_template=ExcelTemplateConfig(
        start_row=0,
        start_col=0,
        include_headers=True,
        sheet_name="Data",
    ),
)

response = await client.save_configuration("invoice-config", config)
print(f"Configuration saved (status {response.status_code})")
```

**Java**

```java
import ai.docudevs.client.ExcelTemplateOptions;
import ai.docudevs.client.ProcessOptions;
import ai.docudevs.client.Schema;
import java.util.List;
import java.util.Map;

String schema = Schema.of(Map.of(
        "type", "object",
        "properties", Map.of(
                "vendor_name", Map.of("type", "string"),
                "invoice_number", Map.of("type", "string"),
                "total_amount", Map.of("type", "number")),
        "required", List.of("total_amount")));

// The Java client returns no status code: the call throws DocuDevsApiException unless the API answers 2xx
client.saveConfiguration(
        "invoice-config",
        ProcessOptions.builder()
                .prompt("Extract the vendor, invoice number and total of each invoice.")
                .schema(schema)
                // startRow and startCol are 0-based cell coordinates: 0 and 0 target cell A1
                .excelTemplate(ExcelTemplateOptions.builder()
                        .startRow(0)
                        .startCol(0)
                        .includeHeaders(true)
                        .sheetName("Data")
                        .build())
                .build());
System.out.println("Configuration saved");
```

   `startRow` and `startCol` are 0-based cell coordinates, so `0` and `0` target cell A1. `sheets` (optional) can instead list several `{sheetName, startRow, startCol, includeHeaders}` targets to write the same result into multiple sheets of the template in one export.

2. **Upload the template file itself:**

**cURL**

```bash
curl -s -S -f -o /dev/null -w "Upload status: %{http_code}\n" -X POST "https://api.docudevs.ai/configuration/invoice-config/excel-template" \
  -H "Authorization: $API_KEY" \
  -F "file=@template.xlsx"
```

**Python**

```python
response = await client.upload_excel_template("invoice-config", "template.xlsx")
print(f"Upload status: {response.status_code}")
```

**Java**

```java
import ai.docudevs.client.UploadRequest;
import java.nio.file.Files;
import java.nio.file.Path;

byte[] templateData = Files.readAllBytes(Path.of("template.xlsx"));

// The Java client returns no status code: the call throws DocuDevsApiException unless the API answers 2xx
client.uploadExcelTemplate(
        "invoice-config",
        new UploadRequest(
                "template.xlsx",
                "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet",
                templateData));
System.out.println("Uploaded template.xlsx");
```

If the configuration has no `excelTemplate` config saved (step 1), the Excel result silently falls back to a plain generated spreadsheet even if a template file was uploaded — both steps are required for the template to take effect.

### Fetching the result

Pass the configuration name when you fetch the Excel result of a completed job:

**cURL**

```bash
JOB_GUID="<job-guid>"  # GUID of a completed extraction job

curl -s -S -f -o invoices.xlsx "https://api.docudevs.ai/job/result/$JOB_GUID/excel?configuration=invoice-config" \
  -H "Authorization: $API_KEY"
```

**Python**

```python
job_guid = "<job-guid>"  # GUID of a completed extraction job

xlsx_bytes = await client.result_excel(
    job_guid, save_to="invoices.xlsx", configuration="invoice-config"
)
print(f"Saved {len(xlsx_bytes)} bytes to invoices.xlsx")
```

**Java**

```java
String jobGuid = "<job-guid>"; // GUID of a completed extraction job

byte[] xlsxBytes = client.fetchResultExcelBytes(jobGuid, "invoice-config");
Files.write(Path.of("invoices.xlsx"), xlsxBytes);
System.out.println("Saved " + xlsxBytes.length + " bytes to invoices.xlsx");
```

There is no working `DocuDevsClient` helper to download the template file you uploaded back out — the generated low-level client's response model for `GET /configuration/{name}/excel-template` does not match the raw binary response the server actually returns; call that endpoint directly if you need to retrieve it. There is no CLI command for uploading or downloading an Excel template.

## Schemas

- `ExcelTemplateConfig`: `startRow` (int, required), `startCol` (int, required), `includeHeaders` (bool, required), `sheetName` (string, optional), `sheets` (array of `SheetWriteTarget`, optional).
- `LookupFileConfig`: `topK` (int, required), `fileName`, `contentType`, `uploadedAt`, `runtimeArtifactType` (all optional, reported back after upload).

Both are nested fields (`excelTemplate`, `lookupFile`) on the named configuration's stored request body.
