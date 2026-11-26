---
title: Map-Reduce Extraction
description: Break long or repetitive documents into manageable windows while preserving header context.
sidebar_position: 4
---
<!-- Generated from docs/core/map-reduce-extraction.md. Do not edit by hand. -->

# Map-Reduce Extraction

Process long documents (50+ pages) by breaking them into manageable chunks.

Map-Reduce is a powerful processing mode designed for long, repetitive, or multi-section documents. Instead of trying to fit the entire document into a single AI context window, DocuDevs splits the document into overlapping windows ("chunks"), extracts data from each chunk, and then aggregates the results.

## How It Works

1. **Map**: The document is split into chunks (e.g., 5 pages each).
2. **Process**: Each chunk is processed independently to extract data.
3. **Reduce**: The results are combined into a single, clean JSON output.

You always receive a consistent JSON payload:

```json
{
  "header": { ... optional ... },
  "records": [ ... rows ... ]
}
```

## When to Use Map-Reduce

- **Long Documents**: Annual reports, 100+ page contracts, large catalogs.
- **Repetitive Data**: Bank statements, medical records, logs.
- **Header + Rows**: Documents with a summary section (header) and pages of detailed rows.

## Configuration Options

| Option | Description | Default |
| --- | --- | --- |
| `splitType` | Chunking mode: `PAGE`, `MARKDOWN_HEADER`, or `DOCUMENT_OUTLINE`. | `PAGE` |
| `splitHeaderLevel` | Header level used when `splitType=MARKDOWN_HEADER` (`1` for `#`, `2` for `##`). | `2` |
| `pagesPerChunk` | Number of pages per extraction window. | `1` |
| `overlapPages` | Number of pages to overlap between chunks to catch data spanning pages. | `0` |
| `dedupKey` | JSON path to a unique field (e.g., `sku`, `date`) used to remove duplicates caused by overlap. | `null` |
| `parallelProcessing` | When `true`, chunks are processed in parallel by multiple workers for faster throughput. | `false` |
| `header` | Configuration for extracting document-level metadata (header); see [Header Capture Strategy](#header-capture-strategy). In the Python SDK these settings are the `header_options`, `header_schema` and `header_prompt` arguments. | `null` |

When `splitType=MARKDOWN_HEADER`, chunking is based on OCR markdown sections instead of pages. In this mode, `overlapPages` and `dedupKey` do not apply.

When `splitType=DOCUMENT_OUTLINE`, chunking is based on inferred document structure instead of fixed page windows. In this mode, `dedupKey` and `splitHeaderLevel` do not apply; `pagesPerChunk` and `overlapPages` are still accepted, but act as a page-window fallback budget rather than a fixed chunk size — see [Document Outline Split Type](#document-outline-split-type) below.

## Example: Processing a Long Invoice

Imagine a 50-page invoice where the first page has the **Invoice Number** and **Date** (Header), and the remaining 49 pages contain **Line Items** (Rows).

The example submits the document with four-page chunks that overlap by one page, removes rows repeated by the overlap with `dedupKey`, reads the header from page 1 only, and adds a short context line to every chunk prompt. It waits for the job and prints the header and the number of extracted line items. The CLI has no option for parallel processing, so its variant omits it.

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
  -F "document=@large-invoice.pdf" | jq -r '.guid')

curl -s -S -f -o /dev/null -X POST "https://api.docudevs.ai/document/process/$JOB_ID" \
  -H "Authorization: $API_KEY" \
  -H "Content-Type: application/json" \
  --data-binary @- <<'JSON'
{
  "mimeType": "application/pdf",
  "prompt": "Extract invoice line items (sku, description, quantity, unitPrice, total).",
  "mapReduce": {
    "enabled": true,
    "splitType": "PAGE",
    "pagesPerChunk": 4,
    "overlapPages": 1,
    "dedupKey": "sku",
    "parallelProcessing": true,
    "header": {
      "enabled": true,
      "pageLimit": 1,
      "includeInRows": false,
      "rowPromptAugmentation": "This is an invoice from Acme Corp.",
      "schema": "{\"type\": \"object\", \"properties\": {\"invoiceNumber\": {\"type\": \"string\"}, \"issueDate\": {\"type\": \"string\"}, \"billingAddress\": {\"type\": \"string\"}}}"
    }
  }
}
JSON
echo "Job submitted: $JOB_ID"

wait_for_job "$JOB_ID"

RESULT=$(curl -s -S -f "https://api.docudevs.ai/job/result/$JOB_ID/json" \
  -H "Authorization: $API_KEY")
echo "Header: $(jq -c '.header' <<< "$RESULT")"
echo "Extracted $(jq '.records | length' <<< "$RESULT") line items."
```

**Python**

```python
from pydantic import BaseModel

from docudevs import json_schema

class InvoiceHeader(BaseModel):
    invoiceNumber: str | None = None
    issueDate: str | None = None
    billingAddress: str | None = None

with open("large-invoice.pdf", "rb") as f:
    document_data = f.read()

job_id = await client.submit_and_process_document_map_reduce(
    document=document_data,
    document_mime_type="application/pdf",
    # Instructions for extracting the rows (line items)
    prompt="Extract invoice line items (sku, description, quantity, unitPrice, total).",
    # Chunking strategy
    pages_per_chunk=4,
    overlap_pages=1,
    dedup_key="sku",  # unique key used to remove rows repeated by the overlap
    parallel_processing=True,
    # Header extraction strategy
    header_options={
        "page_limit": 1,  # look for the header on page 1 only
        "include_in_rows": False,  # do not process page 1 as rows
        "row_prompt_augmentation": "This is an invoice from Acme Corp.",
    },
    header_schema=json_schema(InvoiceHeader),
)
print(f"Job submitted: {job_id}")

result = await client.wait_until_ready(job_id, result_format="json")
print("Header:", result.get("header"))
print(f"Extracted {len(result.get('records', []))} line items.")
```

**Java**

```java
import ai.docudevs.client.MapReduceOptions;
import ai.docudevs.client.ProcessOptions;
import ai.docudevs.client.UploadRequest;
import ai.docudevs.client.WaitOptions;
import com.fasterxml.jackson.databind.JsonNode;
import java.nio.file.Files;
import java.nio.file.Path;

byte[] documentData = Files.readAllBytes(Path.of("large-invoice.pdf"));
UploadRequest upload = new UploadRequest("large-invoice.pdf", "application/pdf", documentData);

ProcessOptions options = ProcessOptions.builder()
        .prompt("Extract invoice line items (sku, description, quantity, unitPrice, total).")
        .mapReduce(MapReduceOptions.builder()
                .pagesPerChunk(4)
                .overlapPages(1)
                .dedupKey("sku")
                .parallelProcessing(true)
                .headerPageLimit(1)
                .headerIncludeInRows(false)
                .headerRowPromptAugmentation("This is an invoice from Acme Corp.")
                .headerSchema("{\"type\": \"object\", \"properties\": {\"invoiceNumber\": {\"type\": \"string\"}, \"issueDate\": {\"type\": \"string\"}, \"billingAddress\": {\"type\": \"string\"}}}")
                .build())
        .build();

String jobId = client.submitAndProcessDocument(upload, options);
System.out.println("Job submitted: " + jobId);

JsonNode result = client.waitUntilReadyJson(jobId, WaitOptions.defaults());
System.out.println("Header: " + result.path("header"));
System.out.println("Extracted " + result.path("records").size() + " line items.");
```

**CLI**

```bash
# The CLI has no option for parallel processing.
docudevs process-map-reduce large-invoice.pdf \
  --prompt "Extract invoice line items (sku, description, quantity, unitPrice, total)." \
  --pages-per-chunk 4 \
  --overlap 1 \
  --dedup-key "sku" \
  --header-page-limit 1 \
  --header-row-prompt-augmentation "This is an invoice from Acme Corp." \
  --header-schema '{"type": "object", "properties": {"invoiceNumber": {"type": "string"}, "issueDate": {"type": "string"}, "billingAddress": {"type": "string"}}}'
```

## Example: Split by Markdown Headers

Use this when OCR text is markdown and your document is organized by headings.

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
  -F "document=@contract.pdf" | jq -r '.guid')

curl -s -S -f -o /dev/null -X POST "https://api.docudevs.ai/document/process/$JOB_ID" \
  -H "Authorization: $API_KEY" \
  -H "Content-Type: application/json" \
  -d '{
    "mimeType": "application/pdf",
    "prompt": "Extract obligations by section.",
    "mapReduce": {
      "enabled": true,
      "splitType": "MARKDOWN_HEADER",
      "splitHeaderLevel": 2,
      "parallelProcessing": true
    }
  }'

wait_for_job "$JOB_ID"

curl -s -S -f "https://api.docudevs.ai/job/result/$JOB_ID/json" \
  -H "Authorization: $API_KEY" | jq .
```

**Python**

```python
with open("contract.pdf", "rb") as f:
    document_data = f.read()

job_id = await client.submit_and_process_document_map_reduce(
    document=document_data,
    document_mime_type="application/pdf",
    prompt="Extract obligations by section.",
    split_type="markdown_header",
    split_header_level=2,  # split on ## headings
    parallel_processing=True,
)

result = await client.wait_until_ready(job_id, result_format="json")
print(json.dumps(result, indent=2))
```

**Java**

```java
import ai.docudevs.client.MapReduceOptions;
import ai.docudevs.client.ProcessOptions;
import ai.docudevs.client.UploadRequest;
import ai.docudevs.client.WaitOptions;
import com.fasterxml.jackson.databind.JsonNode;
import java.nio.file.Files;
import java.nio.file.Path;

byte[] documentData = Files.readAllBytes(Path.of("contract.pdf"));
UploadRequest upload = new UploadRequest("contract.pdf", "application/pdf", documentData);

ProcessOptions options = ProcessOptions.builder()
        .prompt("Extract obligations by section.")
        .mapReduce(MapReduceOptions.builder()
                .splitType(MapReduceOptions.SplitType.MARKDOWN_HEADER)
                .splitHeaderLevel(2)
                .parallelProcessing(true)
                .build())
        .build();

String jobId = client.submitAndProcessDocument(upload, options);
JsonNode result = client.waitUntilReadyJson(jobId, WaitOptions.defaults());
System.out.println(result.toPrettyString());
```

**CLI**

```bash
# The CLI has no option for parallel processing.
docudevs process-map-reduce contract.pdf \
  --prompt "Extract obligations by section." \
  --split-type markdown-header \
  --split-header-level 2
```

## Document Outline Split Type

Use `DOCUMENT_OUTLINE` for structured business documents — contracts, policies, reports, statements of work, manuals — where a logical section (or a table or figure) often spans multiple pages. Unlike `PAGE`, which slices by a fixed page window, `DOCUMENT_OUTLINE` infers the document's structure from OCR markdown and layout data first, then chunks along confident structural boundaries; it only falls back to page windows with overlap for regions that are oversized or structurally uncertain. The resulting outline is persisted as a reusable artifact and used for reduction, so overlap reconciliation is based on outline-node identity rather than a `dedupKey`.

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
  -F "document=@contract.pdf" | jq -r '.guid')

curl -s -S -f -o /dev/null -X POST "https://api.docudevs.ai/document/process/$JOB_ID" \
  -H "Authorization: $API_KEY" \
  -H "Content-Type: application/json" \
  -d '{
    "mimeType": "application/pdf",
    "prompt": "Extract obligations by section.",
    "mapReduce": {
      "enabled": true,
      "splitType": "DOCUMENT_OUTLINE",
      "pagesPerChunk": 3,
      "parallelProcessing": true
    }
  }'

wait_for_job "$JOB_ID"

curl -s -S -f "https://api.docudevs.ai/job/result/$JOB_ID/json" \
  -H "Authorization: $API_KEY" | jq .

curl -s -S -f "https://api.docudevs.ai/job/result/$JOB_ID/document-outline" \
  -H "Authorization: $API_KEY" | jq .
```

**Python**

```python
with open("contract.pdf", "rb") as f:
    document_data = f.read()

job_id = await client.submit_and_process_document_map_reduce(
    document=document_data,
    document_mime_type="application/pdf",
    prompt="Extract obligations by section.",
    split_type="document_outline",
    pages_per_chunk=3,
    parallel_processing=True,
)

result = await client.wait_until_ready(job_id, result_format="json")
print(json.dumps(result, indent=2))

outline = await client.get_document_outline(job_id)
print(json.dumps(outline, indent=2))
```

**Java**

```java
import ai.docudevs.client.MapReduceOptions;
import ai.docudevs.client.ProcessOptions;
import ai.docudevs.client.UploadRequest;
import ai.docudevs.client.WaitOptions;
import com.fasterxml.jackson.databind.JsonNode;

byte[] documentData = Files.readAllBytes(Path.of("contract.pdf"));
UploadRequest upload = new UploadRequest("contract.pdf", "application/pdf", documentData);

String jobId = client.submitAndProcessDocumentMapReduce(
        upload,
        ProcessOptions.builder()
                .mimeType("application/pdf")
                .prompt("Extract obligations by section.")
                .build(),
        MapReduceOptions.builder()
                .splitType(MapReduceOptions.SplitType.DOCUMENT_OUTLINE)
                .pagesPerChunk(3)
                .parallelProcessing(true)
                .build());

JsonNode result = client.waitUntilReadyJson(
        jobId,
        WaitOptions.builder()
                .timeout(Duration.ofSeconds(180))
                .pollInterval(Duration.ofSeconds(5))
                .build());
System.out.println(result.toPrettyString());

JsonNode outline = client.getDocumentOutline(jobId);
System.out.println(outline.toPrettyString());
```

Notes:

- `pagesPerChunk` is optional; when omitted, chunk sizing is determined automatically. When provided, it acts as a page budget for page-fallback windows and as a soft target for grouping, not a strict chunk size.
- `overlapPages` applies only to page-fallback windows and structural boundary context.
- `dedupKey` and `splitHeaderLevel` are not supported with this split type and are rejected.
- The outline artifact can also be (re)built after the fact as a standalone follow-up operation — see [Document Outline](https://docs.docudevs.ai/docs/advanced/operations#document-outline) — without re-running the whole extraction.
- `DOCUMENT_OUTLINE` is available through the REST API and the Python SDK. It is not exposed by the Java SDK's `MapReduceOptions.SplitType` enum or by the CLI's `--split-type` option.

## Header Capture Strategy

- **`header.pageLimit`** (Python: `page_limit` in `header_options`): How many pages at the start are considered "Header".
- **`header.schema`** (Python: `header_schema`): A specific JSON schema just for the header data.
- **`header.rowPromptAugmentation`** (Python: `row_prompt_augmentation`): Text injected into every subsequent chunk prompt. Useful for passing context (e.g., "This is Invoice #123") to help the AI understand isolated pages.

## Best Practices

1. **Always use a `dedupKey`**: When `overlapPages > 0`, the same row might appear in two chunks. The `dedupKey` tells DocuDevs how to identify and merge them.
2. **Start small**: Try `pagesPerChunk=4`. If the AI misses context, increase it. If it's too slow, decrease it.
3. **Monitor Progress**: Map-Reduce jobs take longer. Use the status endpoint to track progress.
4. **Use markdown-header split for sectioned docs**: If OCR output is markdown with clear headings, set `splitType=MARKDOWN_HEADER` and choose `splitHeaderLevel` (`1` for top-level sections, `2` for subsection chunks).

Read the progress of a running job from the `mapReduceStatus` object of the job status:

**cURL**

```bash
JOB_ID="<job-guid>"  # GUID of a running map-reduce job

curl -s -S -f "https://api.docudevs.ai/job/status/$JOB_ID" \
  -H "Authorization: $API_KEY" \
  | jq -r '"Progress: \(.mapReduceStatus.completedChunks) / \(.mapReduceStatus.totalChunks)"'
```

**Python**

```python
job_id = "<job-guid>"  # GUID of a running map-reduce job

response = await client.status(job_id)
if response.status_code != 200:
    raise RuntimeError(f"HTTP {response.status_code}")

progress = response.parsed.map_reduce_status
if progress:
    print(f"Progress: {progress.completed_chunks} / {progress.total_chunks}")
```

**Java**

```java
import com.fasterxml.jackson.databind.JsonNode;

String jobId = "<job-guid>"; // GUID of a running map-reduce job

// The Java client throws DocuDevsApiException when the status is not 200
JsonNode status = client.getJobStatus(jobId);

JsonNode progress = status.path("mapReduceStatus");
if (!progress.isMissingNode() && !progress.isNull()) {
    System.out.println("Progress: " + progress.path("completedChunks").asInt()
            + " / " + progress.path("totalChunks").asInt());
}
```

## Re-running Map-Reduce on an Existing Job

If you have already processed or OCR'd a document, you can run map-reduce on it again **without re-uploading the file or re-running OCR**. This is useful when you want to:

- Iterate on your prompt/schema without paying for OCR again.
- Run different chunking strategies on the same document.
- Extract different fields from a document that was already OCR'd.

Use `submit_and_wait_for_map_reduce` with the `parent_job_id` of the completed job in Python, or send the process request with `dependsOn` set to the completed job (the Java client has no `dependsOn` option and processes the job directly):

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

EXISTING_JOB_GUID="<job-guid>"  # GUID of a completed job from a previous run

# dependsOn chains the new run to the completed job; the job reuses its own GUID
curl -s -S -f -o /dev/null -X POST "https://api.docudevs.ai/document/process/$EXISTING_JOB_GUID?dependsOn=$EXISTING_JOB_GUID" \
  -H "Authorization: $API_KEY" \
  -H "Content-Type: application/json" \
  -d '{
    "prompt": "Extract all line items (sku, description, qty, total)",
    "schema": "{\"type\": \"array\", \"items\": {\"type\": \"object\"}}",
    "mimeType": "application/pdf",
    "mapReduce": {
      "enabled": true,
      "splitType": "PAGE",
      "pagesPerChunk": 5,
      "overlapPages": 1,
      "dedupKey": "sku",
      "parallelProcessing": true
    }
  }'

wait_for_job "$EXISTING_JOB_GUID"

RESULT=$(curl -s -S -f "https://api.docudevs.ai/job/result/$EXISTING_JOB_GUID/json" \
  -H "Authorization: $API_KEY")
echo "Header: $(jq -c '.header' <<< "$RESULT")"
echo "Extracted $(jq '.records | length' <<< "$RESULT") line items."
```

**Python**

```python
from docudevs import array_schema

existing_job_id = "<job-guid>"  # GUID of a completed job from a previous run

# Re-run with map-reduce on the same document: no upload, no new OCR
result = await client.submit_and_wait_for_map_reduce(
    parent_job_id=existing_job_id,
    prompt="Extract all line items (sku, description, qty, total)",
    schema=array_schema({"type": "object"}),
    pages_per_chunk=5,
    overlap_pages=1,
    dedup_key="sku",
    parallel_processing=True,
    timeout=300,
)

print("Header:", result.get("header"))
print(f"Extracted {len(result.get('records', []))} line items.")
```

**Java**

```java
import ai.docudevs.client.MapReduceOptions;
import ai.docudevs.client.ProcessOptions;
import ai.docudevs.client.Schema;
import ai.docudevs.client.WaitOptions;
import com.fasterxml.jackson.databind.JsonNode;
import java.util.Map;

String existingJobGuid = "<job-guid>"; // GUID of a completed job from a previous run

ProcessOptions options = ProcessOptions.builder()
        .prompt("Extract all line items (sku, description, qty, total)")
        .schema(Schema.arrayOf(Map.of("type", "object")))
        .mapReduce(MapReduceOptions.builder()
                .pagesPerChunk(5)
                .overlapPages(1)
                .dedupKey("sku")
                .parallelProcessing(true)
                .build())
        .build();

// The Java client has no dependsOn option, which the Python and cURL variants send
client.processDocument(existingJobGuid, options);

JsonNode result = client.waitUntilReadyJson(existingJobGuid, WaitOptions.defaults());
System.out.println("Header: " + result.path("header"));
System.out.println("Extracted " + result.path("records").size() + " line items.");
```

:::tip
Since OCR is the most expensive part of processing, re-running extraction with different prompts or schemas on an already-processed document is very cost-effective.
:::

## Troubleshooting

- **Missing Rows**: Check if rows are split across pages. Increase `overlapPages`.
- **Duplicate Rows**: Ensure your `dedupKey` is truly unique for each row.
- **Slow Processing**: Reduce `pagesPerChunk` to speed up individual chunk processing.
- **Wrong Markdown Sections**: For markdown-header chunking, switch between `splitHeaderLevel=1` and `splitHeaderLevel=2` depending on your document structure.
