---
title: Named Configurations
description: Save and reuse processing settings as named configurations for consistent document extraction across multiple jobs.
sidebar_position: 1
---
<!-- Generated from docs/configuration/Configuration.md. Do not edit by hand. -->

# Named Configurations

Save and reuse processing settings, prompts, and schemas across multiple document processing requests.

## Overview

Named configurations allow you to define a set of processing parameters once and reuse them by name. This ensures consistency across your application and simplifies your API calls.

**Key Benefits:**

- **Consistency**: Ensure all documents of a certain type are processed with the exact same settings.
- **Schema Management**: Centralize your JSON schemas to guarantee consistent output formats.
- **Simplicity**: Pass a single configuration name instead of a complex JSON object with every request.

## Managing Configurations

### Saving a Configuration

To create or update a configuration, you define the processing parameters (OCR, LLM, Prompt, Schema) and save them under a unique name. The `schema` is passed as a JSON string.

**cURL**

```bash
curl -s -S -f -X POST "https://api.docudevs.ai/configuration/invoice-config" \
  -H "Authorization: $API_KEY" \
  -H "Content-Type: application/json" \
  -d '{
    "prompt": "Extract invoice data including vendor details, line items, and totals.",
    "schema": "{\"type\": \"object\", \"properties\": {\"vendor_name\": {\"type\": \"string\"}, \"invoice_number\": {\"type\": \"string\"}, \"total_amount\": {\"type\": \"number\"}, \"date\": {\"type\": \"string\", \"format\": \"date\"}}, \"required\": [\"vendor_name\", \"total_amount\"]}",
    "ocr": "PREMIUM",
    "llm": "HIGH",
    "extractionMode": "STEPS",
    "barcodes": true
  }' | jq .
```

**Python**

```python
import asyncio
import datetime
import os

from pydantic import BaseModel

from docudevs import json_schema
from docudevs.docudevs_client import DocuDevsClient
from docudevs.models import UploadCommand


async def main():
    client = DocuDevsClient(token=os.environ["API_KEY"])

    class Invoice(BaseModel):
        vendor_name: str
        invoice_number: str | None = None
        total_amount: float
        date: datetime.date | None = None

    config = UploadCommand(
        prompt="Extract invoice data including vendor details, line items, and totals.",
        schema=json_schema(Invoice),
        ocr="PREMIUM",
        llm="HIGH",
        extraction_mode="STEPS",
        barcodes=True,
    )

    # Save the configuration under the name "invoice-config"
    response = await client.save_configuration("invoice-config", config)
    print(f"Configuration saved (status {response.status_code})")


asyncio.run(main())
```

**Java**

```java
import ai.docudevs.client.DocuDevsClient;
import ai.docudevs.client.ProcessOptions;
import com.fasterxml.jackson.databind.JsonNode;

public class Example {
    public static void main(String[] args) throws Exception {
        DocuDevsClient client = DocuDevsClient.builder()
                .apiKey(System.getenv("API_KEY"))
                .baseUrl("https://api.docudevs.ai")
                .build();

        String schema = """
        {
          "type": "object",
          "properties": {
            "vendor_name": {"type": "string"},
            "invoice_number": {"type": "string"},
            "total_amount": {"type": "number"},
            "date": {"type": "string", "format": "date"}
          },
          "required": ["vendor_name", "total_amount"]
        }
        """;

        ProcessOptions options = ProcessOptions.builder()
            .prompt("Extract invoice data including vendor details, line items, and totals.")
            .schema(schema)
            .ocr("PREMIUM")
            .llm("HIGH")
            .extractionMode("STEPS")
            .barcodes(true)
            .build();

        JsonNode saved = client.saveConfiguration("invoice-config", options);
        System.out.println("Saved configuration: " + saved.path("name").asText());
    }
}
```

**CLI**

```bash
cat > config.json <<'EOF'
{
  "prompt": "Extract invoice data including vendor details, line items, and totals.",
  "schema": "{\"type\": \"object\", \"properties\": {\"vendor_name\": {\"type\": \"string\"}, \"invoice_number\": {\"type\": \"string\"}, \"total_amount\": {\"type\": \"number\"}, \"date\": {\"type\": \"string\", \"format\": \"date\"}}, \"required\": [\"vendor_name\", \"total_amount\"]}",
  "ocr": "PREMIUM",
  "llm": "HIGH",
  "extractionMode": "STEPS",
  "barcodes": true
}
EOF

docudevs save-configuration invoice-config config.json
```

### Listing Configurations

Retrieve a list of all saved configurations in your organization.

**cURL**

```bash
curl -s -S -f -X GET "https://api.docudevs.ai/configuration" \
  -H "Authorization: $API_KEY" | jq -r '.[] | "Name: \(.name), Created: \(.createdAt)"'
```

**Python**

```python
response = await client.list_configurations()
if response.status_code != 200:
    raise SystemExit(f"List configurations failed: {response.status_code}")

for config in response.parsed:
    print(f"Name: {config.name}, Created: {config.created_at}")
```

**Java**

```java
import com.fasterxml.jackson.databind.JsonNode;

JsonNode configs = client.listConfigurations();

for (JsonNode config : configs) {
    System.out.println("Name: " + config.path("name").asText() + ", Created: " + config.path("createdAt").asText());
}
```

**CLI**

```bash
docudevs list-configurations
```

### Retrieving a Configuration

Get the details of a specific configuration to inspect its settings.

**cURL**

```bash
curl -s -S -f -X GET "https://api.docudevs.ai/configuration/invoice-config" \
  -H "Authorization: $API_KEY" | jq '{prompt, ocr, llm}'
```

**Python**

```python
response = await client.get_configuration("invoice-config")
if response.status_code != 200:
    raise SystemExit(f"Get configuration failed: {response.status_code}")

config = response.parsed

print(f"Prompt: {config.prompt}")
print(f"OCR: {config.ocr}")
print(f"LLM: {config.llm}")
```

**Java**

```java
import com.fasterxml.jackson.databind.JsonNode;

JsonNode config = client.getConfiguration("invoice-config");

System.out.println("Prompt: " + config.path("prompt").asText());
System.out.println("OCR: " + config.path("ocr").asText());
System.out.println("LLM: " + config.path("llm").asText());
```

**CLI**

```bash
docudevs get-configuration invoice-config
```

### Deleting a Configuration

Remove a configuration when it is no longer needed.

**cURL**

```bash
curl -s -S -f -o /dev/null -X DELETE "https://api.docudevs.ai/configuration/invoice-config" \
  -H "Authorization: $API_KEY"
echo "Deleted configuration invoice-config"
```

**Python**

```python
response = await client.delete_configuration("invoice-config")
print(f"Delete status: {response.status_code}")
```

**Java**

```java
client.deleteConfiguration("invoice-config");
System.out.println("Deleted configuration invoice-config");
```

**CLI**

```bash
docudevs delete-configuration invoice-config
```

## Using Configurations

Once saved, you can use a configuration to process documents. This applies all the settings defined in the configuration to the processing job.

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

JsonNode result = client.waitUntilReadyJson(guid, WaitOptions.defaults());
System.out.println(result);
```

**CLI**

```bash
docudevs process invoice.pdf --configuration invoice-config
```

## Configuration Parameters

When creating a configuration, you can specify the following parameters:

| Parameter | Type | Description |
| --- | --- | --- |
| `prompt` | String | Custom instructions for data extraction. |
| `schema` | String | JSON Schema, serialized as a string, defining the exact output structure. |
| `ocr` | Enum | OCR mode (`DEFAULT`, `PREMIUM`, `LOW`, `NONE`, `AUTO`, `EXCEL`). |
| `llm` | Enum | LLM model quality (`DEFAULT`, `NANO`, `MINI`, `HIGH`). |
| `extractionMode` | Enum | Extraction method (`SIMPLE`, `STEPS`, `OCR`, `GENERATIVE`, `PIPELINE`, `WORKBOOK`). |
| `barcodes` | Boolean | Enable barcode and QR code scanning. |
| `describeFigures` | Boolean | Enable AI description of figures and images. |
| `extractFigures` | Boolean | Store figure images and metadata for later download. |

## Best Practices

- **Version Your Configurations**: If you change your schema significantly, consider creating a new configuration (e.g., `invoice-v2`) to avoid breaking existing integrations.
- **Use Schemas**: Always define a strict JSON schema to ensure predictable API responses.
- **Test Prompts**: Experiment with different prompts in the playground or via temporary configurations before finalizing them.
- **Optimize Costs**: Use `DEFAULT` OCR and LLM for simple documents and `HIGH` only when necessary.
