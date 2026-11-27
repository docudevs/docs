---
title: Your First Document - Detailed Walkthrough
description: Step-by-step guide to processing documents with DocuDevs including instructions, schemas, configurations, and error handling.
sidebar_position: 3
---
<!-- Generated from docs/getting-started/first-document.md. Do not edit by hand. -->

# Your First Document - Detailed Walkthrough

This comprehensive guide walks you through processing your first document with DocuDevs, explaining each step and exploring the various options available for customizing the extraction process.

## Overview

Document processing with DocuDevs involves four main components:

1. **Document**: The file you want to process (PDF, Word, image, etc.)
2. **Instructions**: Tell the AI what information to extract
3. **Schema**: Define the structure of the output data (optional)
4. **Configuration**: Processing options and settings (optional)

## Step-by-Step Walkthrough

### Step 1: Environment Setup

First, ensure you have the DocuDevs SDK installed and your API key set in the `API_KEY` environment variable (see [Installation & Setup](https://docs.docudevs.ai/docs/basics/install)). Every example below reads the key from `API_KEY` and talks to `https://api.docudevs.ai`.

### Step 2: Prepare Your Document

For this walkthrough, we'll use an invoice as an example. You can use any document format. The examples read the file from the current directory: `sample_invoice.pdf` in this guide's own examples, `your-document.pdf` in the shared Quick Start examples, and `contract.pdf` in the case example near the end. Rename the file in the example or put your document under that name.

### Step 3: Basic Processing (Minimal Configuration)

Let's start with the simplest approach - just upload the document and let DocuDevs use its AI to determine what to extract:

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
  -F "document=@sample_invoice.pdf" | jq -r '.guid')

curl -s -S -f -o /dev/null -X POST "https://api.docudevs.ai/document/process/$JOB_ID" \
  -H "Authorization: $API_KEY" \
  -H "Content-Type: application/json" \
  -d '{"mimeType": "application/pdf"}'
echo "Job submitted: $JOB_ID"

wait_for_job "$JOB_ID"

echo "Basic Processing Result:"
curl -s -S -f "https://api.docudevs.ai/job/result/$JOB_ID/json" \
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

    with open("sample_invoice.pdf", "rb") as f:
        document_data = f.read()

    job_id = await client.submit_and_process_document(
        document=document_data,
        document_mime_type="application/pdf",
    )
    print(f"Job submitted: {job_id}")

    result = await client.wait_until_ready(job_id, result_format="json", poll_interval=2)

    print("Basic Processing Result:")
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

public class Example {
    public static void main(String[] args) throws Exception {
        DocuDevsClient client = DocuDevsClient.builder()
                .apiKey(System.getenv("API_KEY"))
                .baseUrl("https://api.docudevs.ai")
                .build();

        byte[] documentBytes = Files.readAllBytes(Path.of("sample_invoice.pdf"));

        String jobGuid = client.submitAndProcessDocument(
                new UploadRequest("sample_invoice.pdf", "application/pdf", documentBytes),
                ProcessOptions.builder()
                        .mimeType("application/pdf")
                        .build());

        JsonNode result = client.waitUntilReadyJson(
                jobGuid,
                WaitOptions.builder()
                        .timeout(Duration.ofSeconds(180))
                        .pollInterval(Duration.ofSeconds(2))
                        .build());

        System.out.println("Job submitted: " + jobGuid);
        System.out.println(result.toPrettyString());
    }
}
```

**CLI**

```bash
docudevs process sample_invoice.pdf
```

The cURL example uses [`jq`](https://jqlang.org/) to read values from responses.

### Step 4: Processing with Custom Instructions

Now let's add specific instructions to guide the AI extraction. The prompt tells the AI which fields matter and how to report them. The example below passes a short prompt as `prompt` (`--prompt` in the CLI); replace it with your own, for example a more detailed one like this:

```text
Extract all invoice information including:
- Invoice number and date
- Vendor/supplier details
- Customer/bill-to information
- All line items with descriptions, quantities, and prices
- Tax information
- Total amounts
- Payment terms and due date
```

The example passes a shorter prompt with the document:

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
  -F "document=@your-document.pdf" | jq -r '.guid')

curl -s -S -f -o /dev/null -X POST "https://api.docudevs.ai/document/process/$JOB_ID" \
  -H "Authorization: $API_KEY" \
  -H "Content-Type: application/json" \
  -d '{
    "mimeType": "application/pdf",
    "prompt": "Extract all key information from this document"
  }'
echo "Document submitted for processing. Job ID: $JOB_ID"

wait_for_job "$JOB_ID"
echo "Processing complete!"

curl -s -S -f "https://api.docudevs.ai/job/result/$JOB_ID/json" \
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

    with open("your-document.pdf", "rb") as f:
        document_data = f.read()

    job_id = await client.submit_and_process_document(
        document=document_data,
        document_mime_type="application/pdf",
        prompt="Extract all key information from this document",
    )
    print(f"Document submitted for processing. Job ID: {job_id}")

    data = await client.wait_until_ready(job_id, result_format="json")
    print("Processing complete!")

    print("\nExtracted Data:")
    print(json.dumps(data, indent=2))


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

public class Example {
    public static void main(String[] args) throws Exception {
        DocuDevsClient client = DocuDevsClient.builder()
                .apiKey(System.getenv("API_KEY"))
                .baseUrl("https://api.docudevs.ai")
                .build();

        byte[] documentData = Files.readAllBytes(Path.of("your-document.pdf"));

        String jobId = client.submitAndProcessDocument(
                new UploadRequest("your-document.pdf", "application/pdf", documentData),
                ProcessOptions.builder()
                        .mimeType("application/pdf")
                        .prompt("Extract all key information from this document")
                        .build());
        System.out.println("Document submitted for processing. Job ID: " + jobId);

        JsonNode data = client.waitUntilReadyJson(
                jobId,
                WaitOptions.builder()
                        .timeout(Duration.ofSeconds(180))
                        .pollInterval(Duration.ofSeconds(5))
                        .build());
        System.out.println("Processing complete!");

        System.out.println("\nExtracted Data:");
        System.out.println(data.toPrettyString());
    }
}
```

**CLI**

```bash
docudevs process your-document.pdf \
  --prompt "Extract all key information from this document"
```

### Step 5: Processing with Custom Schema

For even more control, define exactly how you want the output structured with a JSON Schema. The example below uses a flat schema. Use nested objects for grouped data and arrays of objects for repeating data such as invoice line items, for example:

```json
{
  "type": "object",
  "properties": {
    "vendor": {
      "type": "object",
      "properties": {
        "name": {"type": "string"},
        "address": {"type": "string"}
      }
    },
    "line_items": {
      "type": "array",
      "items": {
        "type": "object",
        "properties": {
          "description": {"type": "string"},
          "amount": {"type": "number"}
        }
      }
    }
  }
}
```

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

SCHEMA='{
  "type": "object",
  "properties": {
    "invoice_number": {"type": "string"},
    "date": {"type": "string", "format": "date"},
    "vendor": {"type": "string"},
    "total": {"type": "number"},
    "currency": {"type": "string"}
  },
  "required": ["invoice_number", "total"]
}'

JOB_ID=$(curl -s -S -f -X POST "https://api.docudevs.ai/document/upload" \
  -H "Authorization: $API_KEY" \
  -F "document=@your-document.pdf" | jq -r '.guid')

jq -n \
  --arg schema "$SCHEMA" \
  '{
    mimeType: "application/pdf",
    prompt: "Extract invoice data according to the schema",
    schema: $schema
  }' |
  curl -s -S -f -o /dev/null -X POST "https://api.docudevs.ai/document/process/$JOB_ID" \
    -H "Authorization: $API_KEY" \
    -H "Content-Type: application/json" \
    --data-binary @-

wait_for_job "$JOB_ID"

curl -s -S -f "https://api.docudevs.ai/job/result/$JOB_ID/json" \
  -H "Authorization: $API_KEY" | jq .
```

**Python**

```python
import datetime

from pydantic import BaseModel

from docudevs import json_schema

with open("your-document.pdf", "rb") as f:
    document_data = f.read()

class Invoice(BaseModel):
    invoice_number: str
    date: datetime.date | None = None
    vendor: str | None = None
    total: float
    currency: str | None = None

job_id = await client.submit_and_process_document(
    document=document_data,
    document_mime_type="application/pdf",
    prompt="Extract invoice data according to the schema",
    schema=json_schema(Invoice),
)

data = await client.wait_until_ready(job_id, result_format="json")
print(json.dumps(data, indent=2))
```

**Java**

```java
import ai.docudevs.client.ProcessOptions;
import ai.docudevs.client.Schema;
import ai.docudevs.client.UploadRequest;
import ai.docudevs.client.WaitOptions;
import com.fasterxml.jackson.databind.JsonNode;

byte[] documentData = Files.readAllBytes(Path.of("your-document.pdf"));

String schema = Schema.of(Map.of(
        "type", "object",
        "properties", Map.of(
                "invoice_number", Map.of("type", "string"),
                "date", Map.of("type", "string", "format", "date"),
                "vendor", Map.of("type", "string"),
                "total", Map.of("type", "number"),
                "currency", Map.of("type", "string")),
        "required", List.of("invoice_number", "total")));

String jobId = client.submitAndProcessDocument(
        new UploadRequest("your-document.pdf", "application/pdf", documentData),
        ProcessOptions.builder()
                .mimeType("application/pdf")
                .prompt("Extract invoice data according to the schema")
                .schema(schema)
                .build());

JsonNode data = client.waitUntilReadyJson(
        jobId,
        WaitOptions.builder()
                .timeout(Duration.ofSeconds(180))
                .pollInterval(Duration.ofSeconds(5))
                .build());
System.out.println(data.toPrettyString());
```

**CLI**

```bash
SCHEMA='{
  "type": "object",
  "properties": {
    "invoice_number": {"type": "string"},
    "date": {"type": "string", "format": "date"},
    "vendor": {"type": "string"},
    "total": {"type": "number"},
    "currency": {"type": "string"}
  },
  "required": ["invoice_number", "total"]
}'

docudevs process your-document.pdf \
  --prompt "Extract invoice data according to the schema" \
  --schema "$SCHEMA"
```

### Step 6: Using Named Configurations

For consistent processing across multiple documents, save your settings as a named configuration and process documents with it by name:

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

SCHEMA='{
  "type": "object",
  "properties": {
    "invoice_number": {"type": "string"},
    "date": {"type": "string", "format": "date"},
    "vendor": {"type": "string"},
    "total": {"type": "number"}
  },
  "required": ["total"]
}'

# Save the configuration under a name
jq -n \
  --arg schema "$SCHEMA" \
  '{
    prompt: "Extract comprehensive invoice data",
    schema: $schema,
    llm: "HIGH"
  }' |
  curl -s -S -f -o /dev/null -X POST "https://api.docudevs.ai/configuration/invoice_processing_standard" \
    -H "Authorization: $API_KEY" \
    -H "Content-Type: application/json" \
    --data-binary @-
echo "Configuration saved: invoice_processing_standard"

# Upload a document and process it with the saved configuration
JOB_ID=$(curl -s -S -f -X POST "https://api.docudevs.ai/document/upload" \
  -H "Authorization: $API_KEY" \
  -F "document=@sample_invoice.pdf" | jq -r '.guid')

curl -s -S -f -o /dev/null -X POST "https://api.docudevs.ai/document/process/$JOB_ID/with-configuration/invoice_processing_standard" \
  -H "Authorization: $API_KEY"

wait_for_job "$JOB_ID"

echo "Processing with Named Configuration Result:"
curl -s -S -f "https://api.docudevs.ai/job/result/$JOB_ID/json" \
  -H "Authorization: $API_KEY" | jq .
```

**Python**

```python
import datetime

from pydantic import BaseModel

from docudevs import json_schema
from docudevs.models import UploadCommand

config_name = "invoice_processing_standard"

class Invoice(BaseModel):
    invoice_number: str | None = None
    date: datetime.date | None = None
    vendor: str | None = None
    total: float

# Save the configuration under a name
save_response = await client.save_configuration(
    config_name,
    UploadCommand(
        prompt="Extract comprehensive invoice data",
        schema=json_schema(Invoice),
        llm="HIGH",
    ),
)
print(f"Configuration saved: {config_name} (status {save_response.status_code})")

# Upload a document and process it with the saved configuration
with open("sample_invoice.pdf", "rb") as f:
    document_data = f.read()

job_id = await client.submit_and_process_document_with_configuration(
    document=document_data,
    document_mime_type="application/pdf",
    configuration_name=config_name,
)

result = await client.wait_until_ready(job_id, result_format="json")
print("Processing with Named Configuration Result:")
print(json.dumps(result, indent=2))
```

**Java**

```java
import ai.docudevs.client.ProcessOptions;
import ai.docudevs.client.Schema;
import ai.docudevs.client.UploadRequest;
import ai.docudevs.client.WaitOptions;
import com.fasterxml.jackson.databind.JsonNode;

String configName = "invoice_processing_standard";

Map<String, Object> properties = new LinkedHashMap<>();
properties.put("invoice_number", Map.of("type", "string"));
properties.put("date", Map.of("type", "string", "format", "date"));
properties.put("vendor", Map.of("type", "string"));
properties.put("total", Map.of("type", "number"));

String schema = Schema.of(Map.of(
        "type", "object",
        "properties", properties,
        "required", List.of("total")));

// Save the configuration under a name
client.saveConfiguration(configName, ProcessOptions.builder()
        .prompt("Extract comprehensive invoice data")
        .schema(schema)
        .llm("HIGH")
        .build());
System.out.println("Configuration saved: " + configName);

// Upload a document and process it with the saved configuration
byte[] documentData = Files.readAllBytes(Path.of("sample_invoice.pdf"));

String jobId = client.submitAndProcessDocumentWithConfiguration(
        new UploadRequest("sample_invoice.pdf", "application/pdf", documentData),
        configName);

JsonNode result = client.waitUntilReadyJson(
        jobId,
        WaitOptions.builder()
                .timeout(Duration.ofSeconds(180))
                .pollInterval(Duration.ofSeconds(5))
                .build());
System.out.println("Processing with Named Configuration Result:");
System.out.println(result.toPrettyString());
```

**CLI**

```bash
SCHEMA='{
  "type": "object",
  "properties": {
    "invoice_number": {"type": "string"},
    "date": {"type": "string", "format": "date"},
    "vendor": {"type": "string"},
    "total": {"type": "number"}
  },
  "required": ["total"]
}'

# The configuration file holds the processing settings
jq -n \
  --arg schema "$SCHEMA" \
  '{prompt: "Extract comprehensive invoice data", schema: $schema, llm: "HIGH"}' > config.json

docudevs save-configuration invoice_processing_standard config.json

docudevs process sample_invoice.pdf --configuration invoice_processing_standard
```

## Understanding the Results

The processing results typically include:

### Success Response Structure

```json
{
  "invoice_number": "INV-2024-001",
  "invoice_date": "2024-01-15",
  "vendor": {
    "name": "Acme Corporation",
    "address": "123 Business St, City, State 12345"
  },
  "line_items": [
    {
      "description": "Professional Services",
      "quantity": 10,
      "unit_price": 125.00,
      "total": 1250.00
    }
  ],
  "summary": {
    "subtotal": 1250.00,
    "tax_amount": 100.00,
    "total_amount": 1350.00,
    "currency": "USD"
  }
}
```

To download the result as CSV or Excel, or to read the schema a job was processed with, see [Result Formats](https://docs.docudevs.ai/docs/core/result-formats).

### Processing Status Flow

1. `CREATED` or `PENDING`: Document uploaded and queued for processing
2. `IN_PROGRESS`, then `OCR_COMPLETED`: The document is being read and analyzed
3. `COMPLETED`: Extraction finished successfully
4. `ERROR`, `FAILED` or `TIMEOUT`: Processing did not finish successfully

Any other status means the job is still running; keep polling until one of the final statuses in steps 3 and 4.

## Different Document Types

DocuDevs can process various document types. Here are optimized approaches for common formats:

### PDF Documents

```python
# Standard PDF processing
mime_type = "application/pdf"
# No special configuration needed
```

### Images (JPG, PNG)

```python
# Image processing often benefits from OCR
mime_type = "image/jpeg"  # or "image/png"

# Consider using OCR-specific instructions
prompt = "First perform OCR to extract all text, then analyze the content for key information"
```

### Word Documents

```python
# Word document processing
mime_type = "application/vnd.openxmlformats-officedocument.wordprocessingml.document"

# Word docs often have structured content
prompt = "Extract information from this structured document, preserving any tables or sections"
```

### Scanned Documents

```python
# Scanned documents need OCR
prompt = "This is a scanned document. First extract all text using OCR, then analyze for key information"

```

For scanned documents, also consider higher quality settings: `ocr` set to `PREMIUM` is more accurate for scans, and `llm` set to `HIGH` understands the OCR text better. Set both in the process request (`ocr` and `llm` fields, `--ocr` and `--llm` in the CLI) or in a saved [configuration](https://docs.docudevs.ai/docs/configuration/).

## Error Handling Best Practices

Always implement robust error handling. Check that the file exists, set a timeout when waiting for the result, and read the current job status when the wait runs out:

**cURL**

```bash
if [ ! -f sample_invoice.pdf ]; then
  echo "Document file not found: sample_invoice.pdf" >&2
  exit 1
fi

JOB_ID=$(curl -s -S -f -X POST "https://api.docudevs.ai/document/upload" \
  -H "Authorization: $API_KEY" \
  -F "document=@sample_invoice.pdf" | jq -r '.guid')

curl -s -S -f -o /dev/null -X POST "https://api.docudevs.ai/document/process/$JOB_ID" \
  -H "Authorization: $API_KEY" \
  -H "Content-Type: application/json" \
  -d '{
    "mimeType": "application/pdf",
    "prompt": "Extract key information from this document"
  }'
echo "Processing job $JOB_ID..."

# Wait up to 5 minutes for the result
DEADLINE=$((SECONDS + 300))
while true; do
  STATUS=$(curl -s -S -f "https://api.docudevs.ai/job/status/$JOB_ID" \
    -H "Authorization: $API_KEY" | jq -r '.status')
  if [ "$STATUS" = "COMPLETED" ]; then
    echo "Processing successful!"
    curl -s -S -f "https://api.docudevs.ai/job/result/$JOB_ID/json" \
      -H "Authorization: $API_KEY" | jq .
    break
  fi
  if [ "$STATUS" = "ERROR" ] || [ "$STATUS" = "FAILED" ] || [ "$STATUS" = "TIMEOUT" ]; then
    echo "Processing failed for job $JOB_ID with status $STATUS" >&2
    exit 1
  fi
  if [ "$SECONDS" -ge "$DEADLINE" ]; then
    echo "Processing timed out for job $JOB_ID" >&2
    echo "Current status: $STATUS" >&2
    exit 1
  fi
  sleep 5
done
```

**Python**

```python
try:
    with open("sample_invoice.pdf", "rb") as f:
        document_data = f.read()
except FileNotFoundError:
    print("Document file not found: sample_invoice.pdf")
    return

job_id = await client.submit_and_process_document(
    document=document_data,
    document_mime_type="application/pdf",
    prompt="Extract key information from this document",
)
print(f"Processing job {job_id}...")

try:
    # Wait up to 5 minutes for the result
    result = await client.wait_until_ready(job_id, timeout=300, result_format="json")
    print("Processing successful!")
    print(result)
except TimeoutError:
    status_response = await client.status(job_id)
    print(f"Processing timed out for job {job_id}")
    print(f"Current status: {status_response.parsed.status}")
except Exception as error:
    print(f"Processing failed: {error}")
```

**Java**

```java
import ai.docudevs.client.DocuDevsApiException;
import ai.docudevs.client.DocuDevsTimeoutException;
import ai.docudevs.client.ProcessOptions;
import ai.docudevs.client.UploadRequest;
import ai.docudevs.client.WaitOptions;
import com.fasterxml.jackson.databind.JsonNode;

Path documentPath = Path.of("sample_invoice.pdf");
if (!Files.exists(documentPath)) {
    System.out.println("Document file not found: sample_invoice.pdf");
    return;
}

byte[] documentData = Files.readAllBytes(documentPath);

String jobId = client.submitAndProcessDocument(
        new UploadRequest("sample_invoice.pdf", "application/pdf", documentData),
        ProcessOptions.builder()
                .mimeType("application/pdf")
                .prompt("Extract key information from this document")
                .build());
System.out.println("Processing job " + jobId + "...");

try {
    // Wait up to 5 minutes for the result
    JsonNode result = client.waitUntilReadyJson(
            jobId,
            WaitOptions.builder()
                    .timeout(Duration.ofSeconds(300))
                    .pollInterval(Duration.ofSeconds(5))
                    .build());
    System.out.println("Processing successful!");
    System.out.println(result.toPrettyString());
} catch (DocuDevsTimeoutException error) {
    JsonNode status = client.getJobStatus(jobId);
    System.out.println("Processing timed out for job " + jobId);
    System.out.println("Current status: " + status.path("status").asText());
} catch (DocuDevsApiException error) {
    System.out.println("Processing failed: " + error.getMessage());
}
```

## Advanced Processing Options

### Batch Processing Multiple Documents

To process many documents with the same settings, create a batch, upload the documents to it and process the batch once:

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

for FILENAME in invoice1.pdf invoice2.pdf invoice3.pdf; do
  curl -s -S -f -o /dev/null -X POST "https://api.docudevs.ai/document/batch/$BATCH_GUID/upload" \
    -H "Authorization: $API_KEY" \
    -F "document=@$FILENAME"
done

SCHEMA='{
  "type": "object",
  "properties": {
    "invoice_number": {"type": "string"},
    "total": {"type": "number"}
  },
  "required": ["total"]
}'

jq -n \
  --arg schema "$SCHEMA" \
  '{
    mimeType: "application/pdf",
    prompt: "Extract invoice data",
    schema: $schema
  }' |
  curl -s -S -f -o /dev/null -X POST "https://api.docudevs.ai/document/batch/$BATCH_GUID/process" \
    -H "Authorization: $API_KEY" \
    -H "Content-Type: application/json" \
    --data-binary @-

wait_for_job "$BATCH_GUID"

# A document that failed or has no stored result is null in this list
curl -s -S -f "https://api.docudevs.ai/job/result/$BATCH_GUID/json" \
  -H "Authorization: $API_KEY" | jq .
```

**Python**

```python
from pydantic import BaseModel

from docudevs import json_schema

batch_guid = await client.create_batch(max_concurrency=3)

for filename in ["invoice1.pdf", "invoice2.pdf", "invoice3.pdf"]:
    with open(filename, "rb") as f:
        await client.upload_batch_document(
            batch_guid=batch_guid,
            document=f.read(),
            mime_type="application/pdf",
            file_name=filename,
        )

class Invoice(BaseModel):
    invoice_number: str | None = None
    total: float

await client.process_batch(
    batch_guid=batch_guid,
    mime_type="application/pdf",
    prompt="Extract invoice data",
    schema=json_schema(Invoice),
)

results = await client.wait_until_ready(batch_guid, timeout=600, result_format="json")
for i, result in enumerate(results):
    if result is None:
        print(f"Document {i}: no result (it failed or has no stored result)")
    else:
        print(f"Document {i}: {result}")
```

**Java**

```java
import ai.docudevs.client.ProcessOptions;
import ai.docudevs.client.Schema;
import ai.docudevs.client.UploadRequest;
import ai.docudevs.client.WaitOptions;
import com.fasterxml.jackson.databind.JsonNode;

String batchGuid = client.createBatch(3);

for (String filename : List.of("invoice1.pdf", "invoice2.pdf", "invoice3.pdf")) {
    byte[] documentData = Files.readAllBytes(Path.of(filename));
    client.uploadBatchDocument(
            batchGuid,
            new UploadRequest(filename, "application/pdf", documentData));
}

String schema = Schema.of(Map.of(
        "type", "object",
        "properties", Map.of(
                "invoice_number", Map.of("type", "string"),
                "total", Map.of("type", "number")),
        "required", List.of("total")));

client.processBatch(
        batchGuid,
        ProcessOptions.builder()
                .mimeType("application/pdf")
                .prompt("Extract invoice data")
                .schema(schema)
                .build());

JsonNode results = client.waitUntilReadyJson(
        batchGuid,
        WaitOptions.builder()
                .timeout(Duration.ofSeconds(600))
                .pollInterval(Duration.ofSeconds(5))
                .build());
for (int i = 0; i < results.size(); i++) {
    if (results.get(i).isNull()) {
        System.out.println("Document " + i + ": no result (it failed or has no stored result)");
    } else {
        System.out.println("Document " + i + ": " + results.get(i));
    }
}
```

**CLI**

```bash
SCHEMA='{
  "type": "object",
  "properties": {
    "invoice_number": {"type": "string"},
    "total": {"type": "number"}
  },
  "required": ["total"]
}'

docudevs batch process invoice1.pdf invoice2.pdf invoice3.pdf \
  --prompt "Extract invoice data" \
  --schema "$SCHEMA" \
  --mime-type application/pdf \
  --max-concurrency 3 \
  --format json \
  --output results.json
```

See [Batch Processing](https://docs.docudevs.ai/docs/core/batch-processing) for scheduling, results and error handling.

### Using Cases for Organization

Cases group related documents. Create a case, then upload documents to it:

**cURL**

```bash
# Step 1: Create a case first
CASE_ID=$(curl -s -S -f -X POST "https://api.docudevs.ai/cases" \
  -H "Authorization: $API_KEY" \
  -H "Content-Type: application/json" \
  -d '{
    "name": "Contract Review",
    "description": "Contract documents for review"
  }' | jq -r '.id')

# Step 2: Upload document to case
curl -s -S -f -o /dev/null -X POST "https://api.docudevs.ai/cases/$CASE_ID/documents" \
  -H "Authorization: $API_KEY" \
  -F "document=@contract.pdf"

echo "Uploaded document to case $CASE_ID"
```

**Python**

```python
from docudevs.models import UploadCaseDocumentBody
from docudevs.types import File

# Step 1: Create a case first
case_response = await client.create_case(
    name="Contract Review",
    description="Contract documents for review",
)
if case_response.status_code != 201:
    raise SystemExit(f"Creating the case failed: HTTP {case_response.status_code}")
case_id = case_response.parsed.id

# Step 2: Upload document to case
with open("contract.pdf", "rb") as f:
    document_data = f.read()

upload_body = UploadCaseDocumentBody(
    document=File(payload=document_data, file_name="contract.pdf", mime_type="application/pdf")
)
upload_response = await client.upload_case_document(case_id=case_id, body=upload_body)

print(f"Uploaded document to case {case_id}, status {upload_response.status_code}")
```

**Java**

```java
import ai.docudevs.client.UploadRequest;
import com.fasterxml.jackson.databind.JsonNode;

// Step 1: Create a case first
JsonNode created = client.createCase("Contract Review", "Contract documents for review");
long caseId = created.path("id").asLong();

// Step 2: Upload document to case
byte[] documentData = Files.readAllBytes(Path.of("contract.pdf"));
client.uploadCaseDocument(caseId, new UploadRequest("contract.pdf", "application/pdf", documentData));

System.out.println("Uploaded document to case " + caseId);
```

List the documents in the case to see their processing status:

**cURL**

```bash
CASE_ID=123  # id returned when the case was created

curl -s -S -f -X GET "https://api.docudevs.ai/cases/$CASE_ID/documents?page=0&size=20" \
  -H "Authorization: $API_KEY" | jq .
```

**Python**

```python
case_id = 123  # id returned by create_case

response = await client.list_case_documents(case_id=case_id, page=0, size=20)
if response.status_code != 200:
    raise SystemExit(f"List documents failed: {response.status_code}")

documents_page = response.parsed

print(f"Total documents: {documents_page.total_size}")
print(f"Page: {documents_page.page_number}/{documents_page.total_pages}")

for document in documents_page.content:
    print(f"{document.filename} [{document.document_id}]")
    print(f"  status={document.processing_status}")
    print(f"  size={document.size_bytes} bytes, uploaded={document.created_at}")
```

**Java**

```java
import com.fasterxml.jackson.databind.JsonNode;

long caseId = 123L; // id returned by createCase

JsonNode documentsPage = client.listCaseDocuments(caseId, 0, 20);

System.out.println("Total documents: " + documentsPage.path("totalSize").asInt());
System.out.println("Page: " + documentsPage.path("pageNumber").asInt() + "/" + documentsPage.path("totalPages").asInt());

for (JsonNode document : documentsPage.path("content")) {
    System.out.println(document.path("filename").asText() + " [" + document.path("documentId").asText() + "]");
    System.out.println("  status=" + document.path("processingStatus").asText());
}
```

**CLI**

```bash
docudevs cases list-documents 123 --page 0 --size 20
```

See [Cases](https://docs.docudevs.ai/docs/advanced/cases) for processing, search and summaries over case documents.

## Performance Tips

### Optimizing Processing Speed

1. **Use an appropriate LLM level**:
   - `MINI`: Faster, good for simple extraction
   - `HIGH`: Slower, better for complex documents

2. **Provide clear, specific instructions**:
   - Good: "Extract invoice number, date, vendor name, and total amount"
   - Poor: "Extract all important information"

3. **Use structured schemas** for consistent output format

4. **Process documents in parallel** when handling multiple files

### Quality Optimization

1. **Include document context** in instructions
2. **Specify output format** requirements (dates, numbers, etc.)
3. **Use examples** in instructions when needed
4. **Test with sample documents** before processing large batches

## What's Next?

Now that you understand document processing fundamentals:

1. **Explore Advanced Features**:
   - [Cases](https://docs.docudevs.ai/docs/advanced/cases) - Organize related documents
   - [Operations](https://docs.docudevs.ai/docs/advanced/operations) - Error analysis and post-processing
   - [Templates](https://docs.docudevs.ai/docs/templates/) - Reusable document templates

2. **Learn Integration Patterns**:
   - [Use Cases](https://docs.docudevs.ai/docs/integration/use-cases) - Real-world implementation examples  
   - [Best Practices](https://docs.docudevs.ai/docs/integration/best-practices) - Production deployment guidelines
   - [Troubleshooting](https://docs.docudevs.ai/docs/integration/troubleshooting) - Debug common issues

3. **Reference Materials**:
   - [SDK Methods](https://docs.docudevs.ai/docs/reference/sdk-methods) - Complete method documentation
   - [Error Codes](https://docs.docudevs.ai/docs/reference/error-codes) - Error handling reference
   - [Configuration](https://docs.docudevs.ai/docs/configuration/) - Settings management

Ready to build more sophisticated document processing workflows? Continue with [Cases](https://docs.docudevs.ai/docs/advanced/cases) to learn about organizing and managing multiple documents!
