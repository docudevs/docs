---
title: Quick Start - 5 Minute Tutorial
description: Process your first document with DocuDevs in 5 minutes using the Python SDK, CLI, or cURL.
sidebar_position: 1
---
<!-- Generated from docs/getting-started/quick-start.md. Do not edit by hand. -->

# Quick Start - 5 Minute Tutorial

Get started with DocuDevs in just 5 minutes! This tutorial will walk you through processing your first document and extracting structured data using AI.

If you are already a seasoned developer, check out these [Python notebooks with examples](https://github.com/docudevs/python-examples)

## What You'll Learn

By the end of this tutorial, you'll know how to:

- Install and configure the DocuDevs SDK
- Upload and process a document
- Extract structured data using AI
- Retrieve and use the results

## Prerequisites

- Python 3.10+ installed on your system
- A DocuDevs API key ([contact us](mailto:support@docudevs.ai) to get started)
- A sample document (PDF, Word, or image file)

## Step 1: Install the SDK (30 seconds)

**pip**

```bash
pip install docu-devs-api-client
```

**poetry**

```bash
poetry add docu-devs-api-client
```

**uv**

```bash
# Optional: create a virtual environment
uv venv
source .venv/bin/activate

# Install the SDK
uv pip install docu-devs-api-client
```

## Step 2: Set Your API Key (10 seconds)

```bash
export API_KEY="your-api-key-here"
```

## Step 3: Process Your First Document (2 minutes)

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

The cURL examples use [`jq`](https://jqlang.org/) to read values from responses.

## Step 4: Run and See Results (2 minutes)

If you used Python, save the script as `quick_start.py` and run it:

```bash
python quick_start.py
```

You'll see output like:

```
Document submitted for processing. Job ID: abc123-def456-ghi789
Processing complete!

Extracted Data:
{
  "invoice_number": "INV-2024-001",
  "date": "2024-01-15",
  "total_amount": "$1,250.00",
  "vendor": "Acme Corp",
  "items": [
    {
      "description": "Professional Services",
      "quantity": 10,
      "unit_price": "$125.00"
    }
  ]
}
```

## What Just Happened?

1. **Document Upload**: Your document was securely uploaded to DocuDevs
2. **AI Processing**: Our AI analyzed the document and extracted structured data
3. **Smart Extraction**: The AI understood the document type and extracted relevant fields
4. **JSON Output**: Results were returned in clean, structured JSON format

## Bonus: AI-Powered Document Analysis (1 minute)

Want to go beyond structured extraction? Try using AI to understand and summarize your documents:

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

OCR_JOB_ID=$(curl -s -S -f -X POST "https://api.docudevs.ai/document/upload" \
  -H "Authorization: $API_KEY" \
  -F "document=@your-document.pdf" | jq -r '.guid')

curl -s -S -f -o /dev/null -X POST "https://api.docudevs.ai/document/ocr/$OCR_JOB_ID?format=markdown" \
  -H "Authorization: $API_KEY" \
  -H "Content-Type: application/json" \
  -d '{
    "ocr": "PREMIUM",
    "ocrFormat": "markdown",
    "mimeType": "application/pdf"
  }'
wait_for_job "$OCR_JOB_ID"

ANALYSIS_JOB_ID=$(curl -s -S -f -X POST "https://api.docudevs.ai/operation/$OCR_JOB_ID/generative-task" \
  -H "Authorization: $API_KEY" \
  -H "Content-Type: application/json" \
  -d '{
    "prompt": "Summarize this document and explain what type of document it is and its main purpose."
  }' | jq -r '.jobGuid')
wait_for_job "$ANALYSIS_JOB_ID"

echo "AI Analysis:"
curl -s -S -f "https://api.docudevs.ai/job/result/$ANALYSIS_JOB_ID" \
  -H "Authorization: $API_KEY" | jq -r '.generated_text'
```

**Python**

```python
with open("your-document.pdf", "rb") as f:
    document_data = f.read()

ocr_job_id = await client.submit_and_ocr_document(
    document=document_data,
    document_mime_type="application/pdf",
    ocr="PREMIUM",
    ocr_format="markdown",
)
await client.wait_until_ready(ocr_job_id)

analysis = await client.submit_and_wait_for_generative_task(
    parent_job_id=ocr_job_id,
    prompt="Summarize this document and explain what type of document it is and its main purpose.",
)

result_data = json.loads(analysis.result)
print("\nAI Analysis:")
print(result_data["generated_text"])
```

**Java**

```java
import ai.docudevs.client.ProcessOptions;
import ai.docudevs.client.UploadRequest;
import ai.docudevs.client.WaitOptions;
import com.fasterxml.jackson.databind.JsonNode;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;

byte[] documentData = Files.readAllBytes(Path.of("your-document.pdf"));

String ocrJobId = client.uploadDocument(
        new UploadRequest("your-document.pdf", "application/pdf", documentData));
client.ocrDocument(
        ocrJobId,
        ProcessOptions.builder().ocr("PREMIUM").mimeType("application/pdf").build(),
        "markdown");
client.waitUntilReadyText(
        ocrJobId,
        WaitOptions.builder()
                .timeout(Duration.ofSeconds(180))
                .pollInterval(Duration.ofSeconds(5))
                .build());

// One call submits the generative task and waits for it; it returns the task's result document
JsonNode analysis = client.submitAndWaitForGenerativeTask(
        ocrJobId,
        "Summarize this document and explain what type of document it is and its main purpose.",
        WaitOptions.builder()
                .timeout(Duration.ofSeconds(120))
                .pollInterval(Duration.ofSeconds(2))
                .build());

System.out.println("\nAI Analysis:");
System.out.println(analysis.path("generated_text").asText());
```

**CLI**

```bash
docudevs ocr-only your-document.pdf \
  --ocr PREMIUM \
  --format markdown \
  --no-wait > ocr-submit.txt
OCR_JOB_ID=$(sed -n 's/.*GUID: //p' ocr-submit.txt)

docudevs wait "$OCR_JOB_ID" --timeout 180 > /dev/null

docudevs operations generative-task "$OCR_JOB_ID" \
  --prompt "Summarize this document and explain what type of document it is and its main purpose."
```

This approach is perfect for:

- **Document Summaries**: Get quick overviews of long documents
- **Document Classification**: Automatically categorize document types
- **Question Answering**: Ask specific questions about document content
- **Content Transformation**: Convert documents to different formats or styles

## Next Steps - Choose Your Path

### 🚀 **For Developers**

- [Complete First Document Guide](https://docs.docudevs.ai/docs/getting-started/first-document) - Learn advanced processing options
- [Simple Documents](https://docs.docudevs.ai/docs/basics/SimpleDocuments) - Understand document processing fundamentals
- [Operations](https://docs.docudevs.ai/docs/advanced/operations) - Use generative tasks and error analysis
- [SDK Methods Reference](https://docs.docudevs.ai/docs/reference/sdk-methods) - Explore all available functions

### 📋 **For Business Users**

- [Use Cases](https://docs.docudevs.ai/docs/integration/use-cases) - See real-world application examples
- [Cases](https://docs.docudevs.ai/docs/advanced/cases) - Organize documents into collections
- [Templates](https://docs.docudevs.ai/docs/templates/) - Create reusable document templates

### ⚙️ **For System Integrators**

- [Best Practices](https://docs.docudevs.ai/docs/integration/best-practices) - Production deployment guidelines
- [Configuration](https://docs.docudevs.ai/docs/configuration/) - Save and reuse processing settings
- [Troubleshooting](https://docs.docudevs.ai/docs/integration/troubleshooting) - Debug common issues

## Try Different Document Types

DocuDevs works with various document formats:

**Invoice**

```python
instruction = "Extract invoice number, date, vendor, total amount, and all line items"
```

**Receipt**

```python
instruction = "Extract merchant name, date, total amount, and purchased items"
```

**Contract**

```python
instruction = "Extract parties involved, contract dates, key terms, and obligations"
```

**Form**

```python
instruction = "Extract all form fields and their values"
```

## Common Customizations

### Add Custom Schema

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

### Process Multiple Similar Documents (Batch)

For processing many similar documents with the same extraction configuration, use **batch processing**:

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

> **Tip**: Use [Cases](https://docs.docudevs.ai/docs/advanced/cases) when you need to organize documents for search and retrieval (RAG), not for batch processing.

## Getting Help

- **Documentation**: Browse our [complete documentation](https://docs.docudevs.ai/docs/basics/install)
- **Examples**: Check out [integration examples](https://docs.docudevs.ai/docs/integration/use-cases)
- **Support**: Email us at [support@docudevs.ai](mailto:support@docudevs.ai)
- **Issues**: Found a bug? Let us know!

## What's Next?

You've successfully processed your first document! Now you're ready to:

1. **Explore Advanced Features**: Learn about [Cases](https://docs.docudevs.ai/docs/advanced/cases) for document organization
2. **Customize Processing**: Dive into [Templates](https://docs.docudevs.ai/docs/templates/) and [Configuration](https://docs.docudevs.ai/docs/configuration/)
3. **Build Integration**: Review [Best Practices](https://docs.docudevs.ai/docs/integration/best-practices) for production use
4. **Scale Your Solution**: Discover performance optimization techniques

Ready to build something amazing with DocuDevs? Let's continue with the [detailed first document guide](https://docs.docudevs.ai/docs/getting-started/first-document)!
