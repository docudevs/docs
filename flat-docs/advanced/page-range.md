---
title: Page Range Processing
description: Process specific pages from multi-page documents for targeted extraction.
sidebar_position: 5
---
<!-- Generated from docs/advanced/page-range.md. Do not edit by hand. -->

# Page Range Processing

Process specific pages from a document instead of the entire document.

The `pageRange` parameter allows you to target specific pages for extraction, which is useful for:

- **Multi-section documents**: Extract data from specific sections (e.g., pages 1-3 for identification, pages 8-10 for physical properties)
- **Large documents**: Focus on relevant pages without processing the entire document
- **Multi-pass processing**: Extract different data from different page ranges of the same document

## How It Works

1. **Upload** your document normally
2. **Specify `pageRange`** with 1-indexed page numbers
3. **Only specified pages** are sent to the AI for extraction

:::info Page Numbering
Pages are **1-indexed**, meaning the first page is page 1 (not 0).
:::

## Basic Usage

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

# Upload the document
JOB_GUID=$(curl -s -S -f -X POST "https://api.docudevs.ai/document/upload" \
  -H "Authorization: $API_KEY" \
  -F "document=@document.pdf" | jq -r '.guid')

# Process only page 1
curl -s -S -f -o /dev/null -X POST "https://api.docudevs.ai/document/process/$JOB_GUID" \
  -H "Authorization: $API_KEY" \
  -H "Content-Type: application/json" \
  -d '{
    "prompt": "Extract the title and summary.",
    "mimeType": "application/pdf",
    "pageRange": [1]
  }'

wait_for_job "$JOB_GUID"

curl -s -S -f "https://api.docudevs.ai/job/result/$JOB_GUID/json" \
  -H "Authorization: $API_KEY" | jq .
```

**Python**

```python
import asyncio
import json
import os

from docudevs.docudevs_client import DocuDevsClient


async def main():
    client = DocuDevsClient(token=os.environ["API_KEY"])

    with open("document.pdf", "rb") as f:
        document_data = f.read()

    # Process only page 1
    job_guid = await client.submit_and_process_document(
        document=document_data,
        document_mime_type="application/pdf",
        prompt="Extract the title and summary.",
        page_range=[1],
    )

    result = await client.wait_until_ready(job_guid, result_format="json")
    print(json.dumps(result, indent=2))


asyncio.run(main())
```

**Java**

```java
import ai.docudevs.client.DocuDevsClient;
import ai.docudevs.client.ProcessOptions;
import ai.docudevs.client.UploadRequest;
import ai.docudevs.client.WaitOptions;
import com.fasterxml.jackson.databind.JsonNode;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.util.List;

public class Example {
    public static void main(String[] args) throws Exception {
        DocuDevsClient client = DocuDevsClient.builder()
                .apiKey(System.getenv("API_KEY"))
                .baseUrl("https://api.docudevs.ai")
                .build();

        byte[] fileBytes = Files.readAllBytes(Path.of("document.pdf"));
        UploadRequest upload = new UploadRequest("document.pdf", "application/pdf", fileBytes);

        // Process only page 1
        String guid = client.submitAndProcessDocument(
                upload,
                ProcessOptions.builder()
                        .mimeType("application/pdf")
                        .prompt("Extract the title and summary.")
                        .pageRange(List.of(1))
                        .build());

        JsonNode result = client.waitUntilReadyJson(guid, WaitOptions.builder()
                .timeout(Duration.ofSeconds(180))
                .pollInterval(Duration.ofSeconds(5))
                .build());
        System.out.println(result.toPrettyString());
    }
}
```

## Processing Multiple Specific Pages

You can specify any combination of pages:

Set `pageRange` to a list of 1-indexed page numbers. Use `[1, 3, 5]` for pages 1, 3 and 5, or `[8, 9, 10]` for pages 8 to 10. The [Safety Data Sheets](#use-case-safety-data-sheets-sds) example below runs several ranges against one document.

## Combined with Map-Reduce

Page range works with map-reduce processing. The specified pages are first filtered, then chunked and processed:

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

JOB_GUID=$(curl -s -S -f -X POST "https://api.docudevs.ai/document/upload" \
  -H "Authorization: $API_KEY" \
  -F "document=@report.pdf" | jq -r '.guid')

# The document has 20 pages, but only pages 1-2, 5-6 and 10-11 are needed.
# These 6 pages are chunked into 3 chunks of 2 pages each.
curl -s -S -f -o /dev/null -X POST "https://api.docudevs.ai/document/process/$JOB_GUID" \
  -H "Authorization: $API_KEY" \
  -H "Content-Type: application/json" \
  -d '{
    "prompt": "Extract all items from these pages.",
    "mimeType": "application/pdf",
    "pageRange": [1, 2, 5, 6, 10, 11],
    "mapReduce": {
      "enabled": true,
      "splitType": "PAGE",
      "pagesPerChunk": 2
    }
  }'

wait_for_job "$JOB_GUID"

curl -s -S -f "https://api.docudevs.ai/job/result/$JOB_GUID/json" \
  -H "Authorization: $API_KEY" | jq .
```

**Python**

```python
with open("report.pdf", "rb") as f:
    document_data = f.read()

# The document has 20 pages, but only pages 1-2, 5-6 and 10-11 are needed.
# These 6 pages are chunked into 3 chunks of 2 pages each.
job_guid = await client.submit_and_process_document_map_reduce(
    document=document_data,
    document_mime_type="application/pdf",
    prompt="Extract all items from these pages.",
    pages_per_chunk=2,
    page_range=[1, 2, 5, 6, 10, 11],
)

result = await client.wait_until_ready(job_guid, result_format="json")
print(json.dumps(result, indent=2))
```

**Java**

```java
import ai.docudevs.client.MapReduceOptions;
import ai.docudevs.client.ProcessOptions;
import ai.docudevs.client.UploadRequest;
import ai.docudevs.client.WaitOptions;
import com.fasterxml.jackson.databind.JsonNode;

byte[] documentData = Files.readAllBytes(Path.of("report.pdf"));
UploadRequest upload = new UploadRequest("report.pdf", "application/pdf", documentData);

// The document has 20 pages, but only pages 1-2, 5-6 and 10-11 are needed.
// These 6 pages are chunked into 3 chunks of 2 pages each.
String jobGuid = client.submitAndProcessDocumentMapReduce(
        upload,
        ProcessOptions.builder()
                .mimeType("application/pdf")
                .prompt("Extract all items from these pages.")
                .pageRange(List.of(1, 2, 5, 6, 10, 11))
                .build(),
        MapReduceOptions.builder()
                .splitType(MapReduceOptions.SplitType.PAGE)
                .pagesPerChunk(2)
                .build());

JsonNode result = client.waitUntilReadyJson(
        jobGuid,
        WaitOptions.builder()
                .timeout(Duration.ofSeconds(180))
                .pollInterval(Duration.ofSeconds(5))
                .build());
System.out.println(result.toPrettyString());
```

:::note Processing Order
With `pageRange` set to `[1, 2, 5, 6, 10, 11]` and `pagesPerChunk` set to `2`:

- **Chunk 1**: Pages 1-2 (original document pages 1-2)
- **Chunk 2**: Pages 5-6 (original document pages 5-6)  
- **Chunk 3**: Pages 10-11 (original document pages 10-11)

Each page is processed exactly once.
:::

## Use Case: Safety Data Sheets (SDS)

Safety Data Sheets have 16 standardized sections spread across many pages. You can extract specific sections using page ranges:

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

# Upload one document per extraction and process only the pages that hold the section
extract_pages() {
  local pages="$1" prompt="$2" job_guid
  job_guid=$(curl -s -S -f -X POST "https://api.docudevs.ai/document/upload" \
    -H "Authorization: $API_KEY" \
    -F "document=@safety-data-sheet.pdf" | jq -r '.guid')

  jq -n --argjson pages "$pages" --arg prompt "$prompt" \
    '{mimeType: "application/pdf", prompt: $prompt, pageRange: $pages}' |
    curl -s -S -f -o /dev/null -X POST "https://api.docudevs.ai/document/process/$job_guid" \
      -H "Authorization: $API_KEY" \
      -H "Content-Type: application/json" \
      --data-binary @-

  wait_for_job "$job_guid"
  curl -s -S -f "https://api.docudevs.ai/job/result/$job_guid/json" \
    -H "Authorization: $API_KEY" | jq .
}

# Section 1: Identification (page 1)
extract_pages '[1]' "Extract product name, manufacturer, and emergency contact."

# Section 9: Physical properties (pages 8-9)
extract_pages '[8, 9]' "Extract appearance, odor, boiling point, and flash point."

# Sections 14-16: Transport and regulatory (pages 12-14)
extract_pages '[12, 13, 14]' "Extract transport classification and regulatory information."
```

**Python**

```python
with open("safety-data-sheet.pdf", "rb") as f:
    document_data = f.read()

# Upload one document per extraction and process only the pages that hold the section
async def extract_pages(page_range, prompt):
    job_guid = await client.submit_and_process_document(
        document=document_data,
        document_mime_type="application/pdf",
        prompt=prompt,
        page_range=page_range,
    )
    return await client.wait_until_ready(job_guid, result_format="json")

# Section 1: Identification (page 1)
identification = await extract_pages(
    [1], "Extract product name, manufacturer, and emergency contact."
)

# Section 9: Physical properties (pages 8-9)
properties = await extract_pages(
    [8, 9], "Extract appearance, odor, boiling point, and flash point."
)

# Sections 14-16: Transport and regulatory (pages 12-14)
regulatory = await extract_pages(
    [12, 13, 14], "Extract transport classification and regulatory information."
)

print(json.dumps([identification, properties, regulatory], indent=2))
```

**Java**

```java
import ai.docudevs.client.ProcessOptions;
import ai.docudevs.client.UploadRequest;
import ai.docudevs.client.WaitOptions;
import com.fasterxml.jackson.databind.JsonNode;

byte[] documentData = Files.readAllBytes(Path.of("safety-data-sheet.pdf"));
UploadRequest upload = new UploadRequest("safety-data-sheet.pdf", "application/pdf", documentData);
WaitOptions wait = WaitOptions.builder()
        .timeout(Duration.ofSeconds(180))
        .pollInterval(Duration.ofSeconds(5))
        .build();

// Upload one document per extraction and process only the pages that hold the section
// Section 1: Identification (page 1)
String identificationJob = client.submitAndProcessDocument(upload, ProcessOptions.builder()
        .mimeType("application/pdf")
        .prompt("Extract product name, manufacturer, and emergency contact.")
        .pageRange(List.of(1))
        .build());
JsonNode identification = client.waitUntilReadyJson(identificationJob, wait);

// Section 9: Physical properties (pages 8-9)
String propertiesJob = client.submitAndProcessDocument(upload, ProcessOptions.builder()
        .mimeType("application/pdf")
        .prompt("Extract appearance, odor, boiling point, and flash point.")
        .pageRange(List.of(8, 9))
        .build());
JsonNode properties = client.waitUntilReadyJson(propertiesJob, wait);

// Sections 14-16: Transport and regulatory (pages 12-14)
String regulatoryJob = client.submitAndProcessDocument(upload, ProcessOptions.builder()
        .mimeType("application/pdf")
        .prompt("Extract transport classification and regulatory information.")
        .pageRange(List.of(12, 13, 14))
        .build());
JsonNode regulatory = client.waitUntilReadyJson(regulatoryJob, wait);

System.out.println(identification.toPrettyString());
System.out.println(properties.toPrettyString());
System.out.println(regulatory.toPrettyString());
```

## Limitations

### OCR Compatibility

Page range requires page break markers from OCR, which are only available with `DEFAULT` or `PREMIUM` OCR modes.

:::warning LOW OCR Not Supported
`pageRange` cannot be used with `ocr=LOW` because the LOW OCR mode (MarkItDown) does not produce page break markers.
:::

The API rejects a request that sets `pageRange` together with `ocr` set to `LOW`, and the Python SDK raises a `ValueError` before sending it.

### Out-of-Range Pages

Pages that don't exist in the document are silently ignored. For a 10-page document, `pageRange` set to `[8, 9, 10, 11, 12, 13, 14, 15]` processes pages 8 to 10 only.

## API Reference

| Parameter | Type | Description |
|-----------|------|-------------|
| `pageRange` | `list[int]` | List of 1-indexed page numbers to process. If not provided, all pages are processed. |

The parameter is available in:

- Single-document processing (Python: `submit_and_process_document`, option `page_range`)
- Map-reduce processing (Python: `submit_and_process_document_map_reduce`, option `page_range`)
- Processing an uploaded document with an upload command (Python: `process_document`)
- Direct API calls to `/document/process/{guid}`
