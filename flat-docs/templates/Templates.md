---
title: Templates
description: Upload reusable document templates and fill them programmatically with extracted or provided data.
sidebar_position: 3
---
<!-- Generated from docs/templates/Templates.md. Do not edit by hand. -->

# Templates

Upload and reuse document templates (PDF, Word, Excel) to automatically fill them with data.

## Overview

Templates allow you to treat documents as reusable forms. You upload a document once, and DocuDevs analyzes it to find fillable fields. You can then fill these templates repeatedly with new data via the API.

For PDFs, there are three distinct workflows:

- **Template workflow**: upload a reusable AcroForm PDF, inspect field names, and fill it repeatedly.
- **Metadata workflow**: inspect AcroForm fields, widgets, and page coordinates for an existing PDF without creating a reusable template. See [AcroForm Metadata for Existing PDFs](#acroform-metadata-for-existing-pdfs).
- **Conversion workflow**: start from a normal PDF, run the `pdf-acroform` operation to generate a fillable AcroForm PDF, inspect editable field definitions, and optionally regenerate the PDF after review. See [PDF AcroForm Conversion](../advanced/operations.md#pdf-acroform-conversion).

**Supported Formats:**

- **PDF Forms**: Standard AcroForms.
- **Word Documents**: `.docx` files with placeholders or content controls.
- **Excel Spreadsheets**: `.xlsx` files.
- **PowerPoint Presentations**: `.pptx` files.

## Managing Templates

### Uploading a Template

Upload a document to create a new template. DocuDevs stores the template immediately, then prepares PDF field metadata in the background when the file is an AcroForm PDF.

**cURL**

```bash
curl -s -S -f -X POST "https://api.docudevs.ai/template/invoice" \
  -H "Authorization: $API_KEY" \
  -F "document=@invoice_template.pdf" | jq .
```

**Python**

```python
import asyncio
import os

from docudevs.docudevs_client import DocuDevsClient


async def main():
    client = DocuDevsClient(token=os.environ["API_KEY"])

    with open("invoice_template.pdf", "rb") as f:
        template_bytes = f.read()

    response = await client.upload_template(
        name="invoice",
        document=template_bytes,
        file_name="invoice_template.pdf",
        mime_type="application/pdf",
    )
    print(f"Upload status: {response.status_code}")


asyncio.run(main())
```

**Java**

```java
import ai.docudevs.client.DocuDevsClient;
import ai.docudevs.client.UploadRequest;
import java.nio.file.Files;
import java.nio.file.Path;

public class Example {
    public static void main(String[] args) throws Exception {
        DocuDevsClient client = DocuDevsClient.builder()
                .apiKey(System.getenv("API_KEY"))
                .baseUrl("https://api.docudevs.ai")
                .build();

        byte[] fileBytes = Files.readAllBytes(Path.of("invoice_template.pdf"));
        UploadRequest upload = new UploadRequest("invoice_template.pdf", "application/pdf", fileBytes);

        client.uploadTemplate("invoice", upload);
        System.out.println("Template uploaded.");
    }
}
```

**CLI**

```bash
docudevs upload-template invoice invoice_template.pdf
```

### Upload and Fill Responses

`POST /template/{name}` uploads or replaces a template. A PDF upload returns a `guid`, a `status` of `completed`, `processing`, or `error`, and the extracted `formFields` when available; an error response can include an `error` message. A non-PDF upload returns the job `guid` and `status` in an `UploadResponse`.

`POST /template/fill/{name}` accepts a JSON `fields` object and returns the filled file as either `application/msword` or `application/pdf`. The success response is binary: keep it as a download, because it is not a JSON response.

Both endpoints can return:

- **409 Conflict**: the request conflicts with an active review run.
- **503 Service Unavailable**: the service is busy. Retry the request after a short delay.

### Listing Templates

Get a list of all available templates in your organization.

**cURL**

```bash
curl -s -S -f -X GET "https://api.docudevs.ai/template/list" \
  -H "Authorization: $API_KEY" | jq -r '.[] | "Name: \(.name), Created: \(.createdAt)"'
```

**Python**

```python
response = await client.list_templates()
if response.status_code != 200:
    raise SystemExit(f"List templates failed: {response.status_code}")

for template in response.parsed:
    print(f"Name: {template.name}, Created: {template.created_at}")
```

**Java**

```java
import com.fasterxml.jackson.databind.JsonNode;

JsonNode templates = client.listTemplates();

for (JsonNode template : templates) {
    System.out.println("Name: " + template.path("name").asText() + ", Created: " + template.path("createdAt").asText());
}
```

**CLI**

```bash
docudevs list-templates
```

### Inspecting Template Metadata

Retrieve details about a specific template, including its fillable fields.

For PDF templates, `GET /template/metadata/{name}` returns `202 Accepted` with `Retry-After: 1` while field extraction is still pending. The cURL example polls until the metadata is ready, and the Python SDK helper `wait_for_template_metadata(...)` does the same for you. The Java SDK and the CLI make a single call, so for a freshly uploaded PDF repeat it until the fields appear.

**cURL**

```bash
# The metadata endpoint answers 202 while PDF field extraction is pending
DEADLINE=$((SECONDS + 60))
STATUS=""
while [ "$SECONDS" -lt "$DEADLINE" ]; do
  STATUS=$(curl -s -S -o metadata.json -w "%{http_code}" "https://api.docudevs.ai/template/metadata/invoice" \
    -H "Authorization: $API_KEY")
  if [ "$STATUS" = "200" ]; then break; fi
  if [ "$STATUS" != "202" ]; then echo "Unexpected status $STATUS" >&2; exit 1; fi
  sleep 1
done
if [ "$STATUS" != "200" ]; then echo "Template metadata was not ready after 60 seconds" >&2; exit 1; fi

jq -r '.[] | "- \(.name) (\(.type))"' metadata.json
```

**Python**

```python
fields = await client.wait_for_template_metadata(
    "invoice",
    timeout=60,
    poll_interval=1,
)

for field in fields:
    print(f"- {field['name']} ({field['type']})")
```

**Java**

```java
import com.fasterxml.jackson.databind.JsonNode;

// Returns immediately: for a freshly uploaded PDF, repeat the call until the fields appear
JsonNode metadata = client.getTemplateMetadata("invoice");

for (JsonNode field : metadata) {
    System.out.println("- " + field.path("name").asText() + " (" + field.path("type").asText() + ")");
}
```

**CLI**

```bash
# Returns immediately: for a freshly uploaded PDF, repeat the command until the fields appear
docudevs template-metadata invoice
```

### Deleting a Template

Remove a template when it is no longer needed.

**cURL**

```bash
curl -s -S -f -o /dev/null -X DELETE "https://api.docudevs.ai/template/invoice" \
  -H "Authorization: $API_KEY"
echo "Deleted template invoice"
```

**Python**

```python
response = await client.delete_template("invoice")
print(f"Delete status: {response.status_code}")
```

**Java**

```java
client.deleteTemplate("invoice");
System.out.println("Deleted template invoice");
```

**CLI**

```bash
docudevs delete-template invoice
```

## Filling Templates

Once a template is uploaded, you can fill it with data. The data structure depends on the template type.

If you fill a template immediately after upload, prefer `fill_with_retry(...)` in the Python SDK. It retries transient readiness races while background template preparation finishes. The Java SDK and the CLI fill once without retrying, so retry the call yourself if it fails right after an upload.

### Filling a PDF Form

PDF forms typically use a flat dictionary of field names and values.

**cURL**

```bash
curl -s -S -f -X POST "https://api.docudevs.ai/template/fill/invoice" \
  -H "Authorization: $API_KEY" \
  -H "Content-Type: application/json" \
  -d '{
    "fields": {
      "customerName": "Acme Corp",
      "invoiceNumber": "INV-2024-001",
      "totalAmount": "1500.00",
      "paid": true
    }
  }' \
  --output filled_invoice.pdf
echo "Saved filled_invoice.pdf"
```

**Python**

```python
from docudevs.models import TemplateFillRequest

fill_request = TemplateFillRequest(
    fields={
        "customerName": "Acme Corp",
        "invoiceNumber": "INV-2024-001",
        "totalAmount": "1500.00",
        "paid": True,
    }
)

# Retries while a freshly uploaded template finishes preparing
response = await client.fill_with_retry(
    name="invoice",
    body=fill_request,
    timeout=30,
    poll_interval=1,
)

if response.status_code != 200:
    raise SystemExit(f"Fill failed: {response.status_code}")

with open("filled_invoice.pdf", "wb") as f:
    f.write(response.content)
print("Saved filled_invoice.pdf")
```

**Java**

```java
byte[] filled = client.fillTemplate("invoice", Map.of(
    "customerName", "Acme Corp",
    "invoiceNumber", "INV-2024-001",
    "totalAmount", "1500.00",
    "paid", true
));

Files.write(Path.of("filled_invoice.pdf"), filled);
System.out.println("Saved filled_invoice.pdf");
```

**CLI**

```bash
cat > data.json <<'EOF'
{
  "fields": {
    "customerName": "Acme Corp",
    "invoiceNumber": "INV-2024-001",
    "totalAmount": "1500.00",
    "paid": true
  }
}
EOF

docudevs fill invoice data.json --output filled_invoice.pdf
```

### Filling a Word Template with Tables

Word templates support nested data structures, allowing you to fill tables and repeated sections.

**cURL**

```bash
curl -s -S -f -X POST "https://api.docudevs.ai/template/fill/sales_report" \
  -H "Authorization: $API_KEY" \
  -H "Content-Type: application/json" \
  -d '{
    "fields": {
      "reportTitle": "Quarterly Sales",
      "date": "2024-04-01",
      "items": [
        {"product": "Widget A", "sales": 100, "revenue": 5000},
        {"product": "Widget B", "sales": 200, "revenue": 8000},
        {"product": "Widget C", "sales": 50, "revenue": 2500}
      ],
      "summary": {
        "totalRevenue": 15500,
        "growth": "15%"
      }
    }
  }' \
  --output filled_report.docx
echo "Saved filled_report.docx"
```

**Python**

```python
from docudevs.models import TemplateFillRequest

fill_request = TemplateFillRequest(
    fields={
        "reportTitle": "Quarterly Sales",
        "date": "2024-04-01",
        "items": [
            {"product": "Widget A", "sales": 100, "revenue": 5000},
            {"product": "Widget B", "sales": 200, "revenue": 8000},
            {"product": "Widget C", "sales": 50, "revenue": 2500},
        ],
        "summary": {
            "totalRevenue": 15500,
            "growth": "15%",
        },
    }
)

response = await client.fill(name="sales_report", body=fill_request)

if response.status_code != 200:
    raise SystemExit(f"Fill failed: {response.status_code}")

with open("filled_report.docx", "wb") as f:
    f.write(response.content)
print("Saved filled_report.docx")
```

**Java**

```java
byte[] filled = client.fillTemplate("sales_report", Map.of(
    "reportTitle", "Quarterly Sales",
    "date", "2024-04-01",
    "items", List.of(
        Map.of("product", "Widget A", "sales", 100, "revenue", 5000),
        Map.of("product", "Widget B", "sales", 200, "revenue", 8000),
        Map.of("product", "Widget C", "sales", 50, "revenue", 2500)
    ),
    "summary", Map.of("totalRevenue", 15500, "growth", "15%")
));

Files.write(Path.of("filled_report.docx"), filled);
System.out.println("Saved filled_report.docx");
```

**CLI**

```bash
cat > report_data.json <<'EOF'
{
  "fields": {
    "reportTitle": "Quarterly Sales",
    "date": "2024-04-01",
    "items": [
      {"product": "Widget A", "sales": 100, "revenue": 5000},
      {"product": "Widget B", "sales": 200, "revenue": 8000},
      {"product": "Widget C", "sales": 50, "revenue": 2500}
    ],
    "summary": {
      "totalRevenue": 15500,
      "growth": "15%"
    }
  }
}
EOF

docudevs fill sales_report report_data.json --output filled_report.docx
```

## AcroForm Metadata for Existing PDFs

Use AcroForm metadata when you need field IDs, widget IDs, page numbers, or page-anchor coordinates from an existing fillable PDF.

- Use the **direct metadata endpoint** when you only need the AcroForm structure.
- Use the **async job flow** when you also need processed-job images, source locations, extraction, or overlays tied to a job GUID.

The direct endpoint accepts PDF uploads only. Non-PDF uploads return `400 Bad Request`. A normal PDF without an AcroForm still returns `200 OK` with `fields: []`.

If the source PDF is visually form-like but `fields` comes back empty, the document does not already contain embedded AcroForm widgets. In that case, use the `pdf-acroform` operation to generate a new fillable PDF and inspect its editable field definitions. The full conversion and review loop is documented in [Operations](../advanced/operations.md#pdf-acroform-conversion).

Direct metadata for an existing PDF:

**cURL**

```bash
METADATA=$(curl -s -S -f -X POST "https://api.docudevs.ai/document/acroform-metadata" \
  -H "Authorization: $API_KEY" \
  -F "document=@fillable-form.pdf")

echo "$METADATA" | jq -r '"Direct fields: \(.fields | length)"'
echo "$METADATA" | jq '.fields[:3]'
```

**Python**

```python
with open("fillable-form.pdf", "rb") as f:
    pdf_bytes = f.read()

metadata = await client.extract_acroform_metadata(
    document=pdf_bytes,
    file_name="fillable-form.pdf",
    mime_type="application/pdf",
)

fields = metadata.get("fields", [])
print(f"Direct fields: {len(fields)}")
print(json.dumps(fields[:3], indent=2))
```

To get AcroForm metadata and source locations tied to a processed job, request them when you submit the document, wait for the job, and then fetch the stored artifacts:

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
  -F "document=@fillable-form.pdf" | jq -r '.guid')

curl -s -S -f -o /dev/null -X POST "https://api.docudevs.ai/document/process/$JOB_ID" \
  -H "Authorization: $API_KEY" \
  -H "Content-Type: application/json" \
  -d '{
    "mimeType": "application/pdf",
    "acroFormMetadata": true,
    "sourceLocations": true,
    "sourceLocationGranularity": "BLOCK"
  }'

wait_for_job "$JOB_ID"

curl -s -S -f -X GET "https://api.docudevs.ai/job/result/$JOB_ID/acroform-metadata" \
  -H "Authorization: $API_KEY" | jq '.fields | length'

curl -s -S -f -X GET "https://api.docudevs.ai/job/result/$JOB_ID/source-locations" \
  -H "Authorization: $API_KEY" | jq .
```

**Python**

```python
with open("fillable-form.pdf", "rb") as f:
    pdf_bytes = f.read()

job_guid = await client.submit_and_process_document(
    document=pdf_bytes,
    document_mime_type="application/pdf",
    acro_form_metadata=True,
    source_locations=True,
    source_location_granularity="block",
)
await client.wait_until_ready(job_guid, result_format="json")

job_metadata = await client.get_acroform_metadata(job_guid)
source_locations = await client.get_source_locations(job_guid)

if job_metadata is None:
    raise SystemExit("No AcroForm metadata found for this job")
if source_locations is None:
    raise SystemExit("No source locations found for this job")

print(f"Async metadata fields: {len(job_metadata.get('fields', []))}")
print(json.dumps(source_locations, indent=2))
```

For OCR-backed evidence overlays on the same job, combine the `acroFormMetadata` and `sourceLocations` request options (`acro_form_metadata=True` and `source_locations=True` in Python) and then read [Source Locations](https://docs.docudevs.ai/docs/core/source-locations).

## Best Practices

- **Field Naming**: Use clear, consistent names for your form fields (e.g., camelCase like `customerName`).
- **Testing**: Upload your template and inspect the metadata to ensure all fields are detected correctly.
- **Data Types**: Ensure the data you send matches the expected type (e.g., booleans for checkboxes).
- **Configurations**: Combine templates with [Named Configurations](../configuration/Configuration.md) to standardize the filling process if you have complex logic.
