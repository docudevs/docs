---
title: Schema Generation
description: Automatically generate JSON schemas from your documents using AI.
sidebar_position: 5
---
<!-- Generated from docs/core/schema-generation.md. Do not edit by hand. -->

# Schema Generation

Automatically generate JSON schemas from your documents using AI.

Defining a perfect JSON schema for a complex document can be tedious. DocuDevs can analyze your document and generate a schema for you, which you can then refine and use for extraction.

## How It Works

1. **Upload** a representative document (e.g., a sample invoice).
2. **Instruct** the AI on what fields matter (optional).
3. **Receive** a valid JSON Schema ready for use.

## Generating a Schema

The example uploads a sample invoice with instructions, waits for the schema job and prints the generated JSON Schema. Save the printed schema as `invoice-schema.json` to use it in the next step. Schema generation is available through the API, the Python and Java SDKs; the CLI has no schema generation command.

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

INSTRUCTIONS="Focus on extracting the invoice number, date, and all line items with prices."

INSTRUCTIONS_QUERY=$(jq -rn --arg text "$INSTRUCTIONS" '$text | @uri')

JOB_GUID=$(curl -s -S -f -X POST "https://api.docudevs.ai/document/generate-schema?instructionsText=$INSTRUCTIONS_QUERY" \
  -H "Authorization: $API_KEY" \
  -F "document=@sample-invoice.pdf" | jq -r '.guid')

wait_for_job "$JOB_GUID"

echo "Generated schema:"
curl -s -S -f "https://api.docudevs.ai/job/result/$JOB_GUID/json" \
  -H "Authorization: $API_KEY" | jq .
```

**Python**

```python
with open("sample-invoice.pdf", "rb") as f:
    document_data = f.read()

job_guid = await client.generate_schema(
    document=document_data,
    document_mime_type="application/pdf",
    instructions="Focus on extracting the invoice number, date, and all line items with prices.",
)

schema = await client.wait_until_ready(job_guid, result_format="json")
print("Generated schema:")
print(json.dumps(schema, indent=2))
```

**Java**

```java
import ai.docudevs.client.UploadRequest;
import ai.docudevs.client.WaitOptions;
import com.fasterxml.jackson.databind.JsonNode;
import java.nio.file.Files;
import java.nio.file.Path;

byte[] documentData = Files.readAllBytes(Path.of("sample-invoice.pdf"));
UploadRequest upload = new UploadRequest("sample-invoice.pdf", "application/pdf", documentData);

String jobGuid = client.generateSchema(
        upload,
        "Focus on extracting the invoice number, date, and all line items with prices.");

JsonNode schema = client.waitUntilReadyJson(jobGuid, WaitOptions.builder().build());
System.out.println("Generated schema:");
System.out.println(schema.toPrettyString());
```

## Using the Generated Schema

Once you have the schema, you can use it to process documents with strict validation.

### 1. Review and Refine

The generated schema is a starting point. You might want to:

- Rename fields to match your database columns.
- Change data types (e.g., `string` to `number`).
- Add `required` fields.

### 2. Use in Extraction

Pass the reviewed schema as the extraction schema. The example loads `invoice-schema.json` and processes a new invoice with it.

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

# invoice-schema.json holds the schema from the generation step, after review
JOB_GUID=$(curl -s -S -f -X POST "https://api.docudevs.ai/document/upload" \
  -H "Authorization: $API_KEY" \
  -F "document=@new-invoice.pdf" | jq -r '.guid')

jq -n --rawfile schema invoice-schema.json \
  '{mimeType: "application/pdf", schema: $schema}' |
  curl -s -S -f -o /dev/null -X POST "https://api.docudevs.ai/document/process/$JOB_GUID" \
    -H "Authorization: $API_KEY" \
    -H "Content-Type: application/json" \
    --data-binary @-

wait_for_job "$JOB_GUID"

curl -s -S -f "https://api.docudevs.ai/job/result/$JOB_GUID/json" \
  -H "Authorization: $API_KEY" | jq .
```

**Python**

```python
from docudevs import json_schema

# invoice-schema.json holds the schema from the generation step, after review
with open("invoice-schema.json") as f:
    schema = json.load(f)

with open("new-invoice.pdf", "rb") as f:
    document_data = f.read()

job_guid = await client.submit_and_process_document(
    document=document_data,
    document_mime_type="application/pdf",
    schema=json_schema(schema),
)

result = await client.wait_until_ready(job_guid, result_format="json")
print(json.dumps(result, indent=2))
```

**Java**

```java
import ai.docudevs.client.ProcessOptions;
import ai.docudevs.client.Schema;
import ai.docudevs.client.UploadRequest;
import ai.docudevs.client.WaitOptions;
import com.fasterxml.jackson.databind.JsonNode;

// invoice-schema.json holds the schema from the generation step, after review
String schema = Schema.of(Files.readString(Path.of("invoice-schema.json")));

byte[] documentData = Files.readAllBytes(Path.of("new-invoice.pdf"));

String jobGuid = client.submitAndProcessDocument(
        new UploadRequest("new-invoice.pdf", "application/pdf", documentData),
        ProcessOptions.builder()
                .mimeType("application/pdf")
                .schema(schema)
                .build());

JsonNode result = client.waitUntilReadyJson(
        jobGuid,
        WaitOptions.builder()
                .timeout(Duration.ofSeconds(180))
                .pollInterval(Duration.ofSeconds(5))
                .build());
System.out.println(result.toPrettyString());
```

**CLI**

```bash
# invoice-schema.json holds the schema from the generation step, after review
docudevs process new-invoice.pdf --schema-file invoice-schema.json
```

## Best Practices

- **Use Representative Documents**: Upload a document that contains all the fields you expect to see.
- **Be Specific in Instructions**: If you need specific formats (e.g., "Dates as YYYY-MM-DD"), mention it in the instructions.
- **Iterate**: Generate a schema, test it on a few documents, and refine it if necessary.

## Example Output

Here is what a generated schema might look like:

```json
{
  "$schema": "http://json-schema.org/draft-07/schema#",
  "type": "object",
  "properties": {
    "invoice_number": {
      "type": "string",
      "description": "Unique identifier for the invoice"
    },
    "date": {
      "type": "string",
      "format": "date",
      "description": "Date of issue"
    },
    "total_amount": {
      "type": "number",
      "description": "Final total including tax"
    },
    "line_items": {
      "type": "array",
      "items": {
        "type": "object",
        "properties": {
          "description": { "type": "string" },
          "quantity": { "type": "number" },
          "price": { "type": "number" }
        }
      }
    }
  },
  "required": ["invoice_number", "total_amount"]
}
```
