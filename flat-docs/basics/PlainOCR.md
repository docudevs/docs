---
title: Plain OCR Processing
description: Extract plain text or markdown from documents using OCR without structured data extraction.
sidebar_position: 3
---
<!-- Generated from docs/basics/PlainOCR.md. Do not edit by hand. -->

# Plain OCR Processing

DocuDevs provides OCR-only processing for extracting plain text or markdown content from documents without structured data extraction. This feature is perfect when you only need the text content from documents like PDFs, images, or scanned documents.

## Overview

The Plain OCR feature allows you to:

- Extract text content in either plain text or markdown format
- Choose between standard and premium OCR quality
- Process documents without complex data structuring
- Get faster results for simple text extraction needs

## Quick Start

Install the SDK and CLI, and set your API key as an environment variable:

```bash
pip install docu-devs-api-client
export API_KEY="your_api_key"
```

Extract the text of a document as markdown:

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
  -F "document=@document.pdf" | jq -r '.guid')

curl -s -S -f -o /dev/null -X POST "https://api.docudevs.ai/document/ocr/$JOB_ID?format=markdown" \
  -H "Authorization: $API_KEY" \
  -H "Content-Type: application/json" \
  -d '{
    "ocr": "DEFAULT",
    "mimeType": "application/pdf"
  }'

wait_for_job "$JOB_ID"

curl -s -S -f "https://api.docudevs.ai/job/result/$JOB_ID" \
  -H "Authorization: $API_KEY"
```

**Python**

```python
import asyncio
import os

from docudevs.docudevs_client import DocuDevsClient


async def main():
    client = DocuDevsClient(token=os.environ["API_KEY"])

    with open("document.pdf", "rb") as f:
        document_data = f.read()

    job_id = await client.submit_and_ocr_document(
        document=document_data,
        document_mime_type="application/pdf",
        ocr="DEFAULT",
        ocr_format="markdown",
    )

    text = await client.wait_until_ready(job_id)
    print(text)


asyncio.run(main())
```

**CLI**

```bash
docudevs ocr-only document.pdf --format markdown
```

The cURL example uses [`jq`](https://jqlang.org/) to read values from responses. The result of an OCR job is the text itself, not JSON. Image files such as `receipt.jpg` work the same way as PDFs; set the MIME type to match the file (for example `image/jpeg`). The CLI detects the MIME type from the file name.

## OCR Quality Options

### DEFAULT Quality

- Uses Azure Document Intelligence's standard OCR engine
- Fast processing
- Good for clear, well-formatted documents
- No additional AI processing

### PREMIUM Quality  

- Includes AI-powered error correction
- Better accuracy for handwritten or poor-quality documents
- Pagewise processing for optimal results
- Additional processing time for enhanced accuracy

### AUTO Quality (Recommended)

- Automatically analyzes document quality and selects appropriate processing level
- Starts with DEFAULT quality for speed
- Automatically escalates to PREMIUM if quality issues are detected
- Best balance of speed and accuracy
- Ideal for mixed document types or when document quality is unknown

## Output Formats

### Markdown Format

- Preserves document formatting
- Maintains headers, lists, and text structure
- Ideal for documents with rich formatting
- Better readability and structure retention

### Plain Text Format

- Raw text extraction
- No formatting preservation  
- Smaller output size
- Best for simple text analysis

## Choosing Quality and Format

Set the OCR quality (`DEFAULT`, `PREMIUM` or `AUTO`) and the output format (`markdown` or `plain`) per request. If you do not set a format, the SDK produces markdown and the CLI produces plain text, so pass `--format` explicitly in scripts:

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
  -F "document=@handwritten.pdf" | jq -r '.guid')

# ocr: DEFAULT, PREMIUM or AUTO. format: markdown or plain
curl -s -S -f -o /dev/null -X POST "https://api.docudevs.ai/document/ocr/$JOB_ID?format=plain" \
  -H "Authorization: $API_KEY" \
  -H "Content-Type: application/json" \
  -d '{
    "ocr": "PREMIUM",
    "ocrFormat": "plain",
    "mimeType": "application/pdf"
  }'

wait_for_job "$JOB_ID"

curl -s -S -f "https://api.docudevs.ai/job/result/$JOB_ID" \
  -H "Authorization: $API_KEY"
```

**Python**

```python
with open("handwritten.pdf", "rb") as f:
    document_data = f.read()

# ocr: "DEFAULT", "PREMIUM" or "AUTO". ocr_format: "markdown" or "plain"
job_id = await client.submit_and_ocr_document(
    document=document_data,
    document_mime_type="application/pdf",
    ocr="PREMIUM",
    ocr_format="plain",
)

text = await client.wait_until_ready(job_id)
print(text)
```

**CLI**

```bash
# --ocr: DEFAULT, PREMIUM or AUTO. --format: markdown or plain
docudevs ocr-only handwritten.pdf --ocr PREMIUM --format plain
```

### Checking Results

The examples wait for the job to finish and print the text. To check a job yourself, use the job status and result endpoints, or `docudevs status <guid>` and `docudevs result <guid>` in the CLI. Add `--no-wait` to `docudevs ocr-only` to return as soon as the document is queued.

### CLI Options

- `--api-url` points the CLI at a different API endpoint. Put it before the command name.
- `--token` overrides the key from the environment for one command. Put it before the command name.
- `--timeout` sets how long the CLI waits for the result, in seconds.

## Barcodes and QR codes

OCR can also read barcodes and QR codes and write their values into the text. Set `barcodes` to `true` in the request:

- In a process request, the `barcodes` field; in the Python client, `barcodes=True`; in the CLI, `docudevs process --barcodes`.
- In a saved [configuration](https://docs.docudevs.ai/docs/configuration/), the `barcodes` field.

The plain OCR request has no `barcodes` option: `POST /document/ocr/{guid}`, `POST /document/ocr/sync`, `docudevs ocr-only` and `submit_and_ocr_document` do not read barcodes. To get the OCR text with barcodes, send a process request with `extractionMode` set to `OCR`, which returns the text without structured extraction. The example does that.

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

# extractionMode OCR returns the text only; barcodes turns on the scan
curl -s -S -f -o /dev/null -X POST "https://api.docudevs.ai/document/process/$JOB_ID" \
  -H "Authorization: $API_KEY" \
  -H "Content-Type: application/json" \
  -d '{
    "mimeType": "application/pdf",
    "ocr": "DEFAULT",
    "extractionMode": "OCR",
    "barcodes": true
  }'

wait_for_job "$JOB_ID"

curl -s -S -f "https://api.docudevs.ai/job/result/$JOB_ID" \
  -H "Authorization: $API_KEY"
```

**Python**

```python
with open("invoice.pdf", "rb") as f:
    document_data = f.read()

# extraction_mode="OCR" returns the text only; barcodes=True turns on the scan
job_id = await client.submit_and_process_document(
    document=document_data,
    document_mime_type="application/pdf",
    ocr="DEFAULT",
    extraction_mode="OCR",
    barcodes=True,
)

text = await client.wait_until_ready(job_id, timeout=600)
print(text)
```

**CLI**

```bash
docudevs process invoice.pdf --ocr DEFAULT --extraction-mode OCR --barcodes --timeout 600
```

Each value is written into the text at the place where the barcode appears, as `<kind>=<value>`, where `<kind>` is the barcode type that the scanner reports. If you use a prompt and a schema instead of `extractionMode` `OCR`, the extraction reads the same text and can return the values as fields; see [Extracting QR Codes](https://docs.docudevs.ai/docs/basics/SimpleDocuments#extracting-qr-codes).

The scan is part of the document OCR, so it has no effect when `ocr` is `NONE`, `LOW` or `EXCEL`. Workbook normalization rejects requests that set `barcodes`. The CLI waits 60 seconds by default, so the example raises `--timeout`; cURL and Python stop waiting after 600 seconds.

## One-Step Synchronous OCR

For simpler usage, upload the document and receive the text in the same request. The response is a JSON string containing the OCR output. This endpoint is called over HTTP; the Python SDK has no helper for it.

**cURL**

```bash
curl -s -S -f -X POST "https://api.docudevs.ai/document/ocr/sync?format=markdown&ocr=AUTO" \
  -H "Authorization: $API_KEY" \
  -F "document=@document.pdf"
```
