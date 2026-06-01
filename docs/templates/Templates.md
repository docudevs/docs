---
title: Templates
description: Upload reusable document templates and fill them programmatically with extracted or provided data.
sidebar_position: 3
---

import Tabs from '@theme/Tabs';
import TabItem from '@theme/TabItem';

# Templates

Upload and reuse document templates (PDF, Word, Excel) to automatically fill them with data.

## Overview

Templates allow you to treat documents as reusable forms. You upload a document once, and DocuDevs analyzes it to find fillable fields. You can then fill these templates repeatedly with new data via the API.

For PDFs, there are two distinct workflows:

- **Template workflow**: upload a reusable AcroForm PDF, inspect field names, and fill it repeatedly.
- **Metadata workflow**: inspect AcroForm fields, widgets, and page coordinates for an existing PDF without creating a reusable template. See [AcroForm Metadata for Existing PDFs](#acroform-metadata-for-existing-pdfs).

**Supported Formats:**

- **PDF Forms**: Standard AcroForms.
- **Word Documents**: `.docx` files with placeholders or content controls.
- **Excel Spreadsheets**: `.xlsx` files.
- **PowerPoint Presentations**: `.pptx` files.

## Managing Templates

### Uploading a Template

Upload a document to create a new template. DocuDevs stores the template immediately, then prepares PDF field metadata in the background when the file is an AcroForm PDF.

<Tabs
  defaultValue="python"
  values={[
    {label: 'Python SDK', value: 'python'},
    {label: 'Java SDK', value: 'java'},
    {label: 'CLI', value: 'cli'},
    {label: 'cURL', value: 'curl'},
  ]}>
  <TabItem value="python">

```python
from docudevs import DocuDevsClient
import os
import asyncio

client = DocuDevsClient(token=os.getenv('API_KEY'))

async def upload_invoice_template():
    with open('invoice_template.pdf', 'rb') as f:
        response = await client.upload_template(
            name="invoice",
            document=f.read(),
            file_name="invoice_template.pdf",
            mime_type="application/pdf"
        )

    print(f"Upload status: {response.status_code}")

    fields = await client.wait_for_template_metadata(
        "invoice",
        timeout=60,
        poll_interval=1,
    )
    print(fields)

# asyncio.run(upload_invoice_template())
```

  </TabItem>
  <TabItem value="cli">

```bash
docudevs upload-template invoice invoice_template.pdf
```

  </TabItem>
  <TabItem value="java">

```java
import ai.docudevs.client.DocuDevsClient;
import ai.docudevs.client.UploadRequest;
import java.nio.file.Files;
import java.nio.file.Path;

DocuDevsClient client = DocuDevsClient.builder()
    .apiKey(System.getenv("API_KEY"))
    .build();

byte[] fileBytes = Files.readAllBytes(Path.of("invoice_template.pdf"));
UploadRequest upload = new UploadRequest("invoice_template.pdf", "application/pdf", fileBytes);

client.uploadTemplate("invoice", upload);
System.out.println("Template uploaded.");
```

  </TabItem>

  <TabItem value="curl">

```bash
curl -X POST https://api.docudevs.ai/template/invoice \
  -H "Authorization: Bearer $API_KEY" \
  -F "document=@invoice_template.pdf"
```

  </TabItem>
</Tabs>

### Listing Templates

Get a list of all available templates in your organization.

<Tabs
  defaultValue="python"
  values={[
    {label: 'Python SDK', value: 'python'},
    {label: 'Java SDK', value: 'java'},
    {label: 'CLI', value: 'cli'},
    {label: 'cURL', value: 'curl'},
  ]}>
  <TabItem value="python">

```python
templates = await client.list_templates()
for template in templates:
    print(f"Name: {template.name}, Created: {template.created_at}")
```

  </TabItem>
  <TabItem value="cli">

```bash
docudevs list-templates
```

  </TabItem>
  <TabItem value="java">

```java
import ai.docudevs.client.DocuDevsClient;
import com.fasterxml.jackson.databind.JsonNode;

DocuDevsClient client = DocuDevsClient.builder()
    .apiKey(System.getenv("API_KEY"))
    .build();

JsonNode templates = client.listTemplates();

for (JsonNode template : templates) {
    System.out.println(template.path("name").asText() + " created=" + template.path("createdAt").asText());
}
```

  </TabItem>

  <TabItem value="curl">

```bash
curl -X GET https://api.docudevs.ai/template/list \
  -H "Authorization: Bearer $API_KEY"
```

  </TabItem>
</Tabs>

### Inspecting Template Metadata

Retrieve details about a specific template, including its fillable fields.

For PDF templates, `GET /template/metadata/{name}` returns `202 Accepted` with `Retry-After: 1` while field extraction is still pending. The Python SDK helper `wait_for_template_metadata(...)` handles this polling for you.

<Tabs
  defaultValue="python"
  values={[
    {label: 'Python SDK', value: 'python'},
    {label: 'Java SDK', value: 'java'},
    {label: 'CLI', value: 'cli'},
    {label: 'cURL', value: 'curl'},
  ]}>
  <TabItem value="python">

```python
fields = await client.wait_for_template_metadata(
    "invoice",
    timeout=60,
    poll_interval=1,
)

for field in fields:
    print(f"- {field['name']} ({field['type']})")
```

  </TabItem>
  <TabItem value="cli">

```bash
docudevs template-metadata invoice
```

  </TabItem>
  <TabItem value="java">

```java
import ai.docudevs.client.DocuDevsClient;
import com.fasterxml.jackson.databind.JsonNode;

DocuDevsClient client = DocuDevsClient.builder()
    .apiKey(System.getenv("API_KEY"))
    .build();

JsonNode metadata = client.getTemplateMetadata("invoice");

for (JsonNode field : metadata) {
    System.out.println(field.path("name").asText() + " (" + field.path("type").asText() + ")");
}
```

  </TabItem>

  <TabItem value="curl">

```bash
curl -i -X GET https://api.docudevs.ai/template/metadata/invoice \
  -H "Authorization: Bearer $API_KEY"
```

If the response is `202 Accepted`, wait for the `Retry-After` interval and request the metadata again.

  </TabItem>
</Tabs>

### Deleting a Template

Remove a template when it is no longer needed.

<Tabs
  defaultValue="python"
  values={[
    {label: 'Python SDK', value: 'python'},
    {label: 'Java SDK', value: 'java'},
    {label: 'CLI', value: 'cli'},
    {label: 'cURL', value: 'curl'},
  ]}>
  <TabItem value="python">

```python
await client.delete_template("invoice")
```

  </TabItem>
  <TabItem value="cli">

```bash
docudevs delete-template invoice
```

  </TabItem>
  <TabItem value="java">

```java
import ai.docudevs.client.DocuDevsClient;

DocuDevsClient client = DocuDevsClient.builder()
    .apiKey(System.getenv("API_KEY"))
    .build();

client.deleteTemplate("invoice");
System.out.println("Deleted template invoice");
```

  </TabItem>

  <TabItem value="curl">

```bash
curl -X DELETE https://api.docudevs.ai/template/invoice \
  -H "Authorization: Bearer $API_KEY"
```

  </TabItem>
</Tabs>

## Filling Templates

Once a template is uploaded, you can fill it with data. The data structure depends on the template type.

If you fill a template immediately after upload, prefer `fill_with_retry(...)` in the Python SDK. It retries transient readiness races while background template preparation finishes.

### Filling a PDF Form

PDF forms typically use a flat dictionary of field names and values.

<Tabs
  defaultValue="python"
  values={[
    {label: 'Python SDK', value: 'python'},
    {label: 'Java SDK', value: 'java'},
    {label: 'CLI', value: 'cli'},
    {label: 'cURL', value: 'curl'},
  ]}>
  <TabItem value="python">

```python
from docudevs.models import TemplateFillRequest

async def fill_invoice():
    fill_request = TemplateFillRequest(
        fields={
            "customerName": "Acme Corp",
            "invoiceNumber": "INV-2024-001",
            "totalAmount": "1500.00",
            "paid": True
        }
    )

    response = await client.fill_with_retry(
        name="invoice",
        body=fill_request,
        timeout=30,
        poll_interval=1,
    )
    
    # Save the filled PDF
    with open("filled_invoice.pdf", "wb") as f:
        f.write(response.content)

# asyncio.run(fill_invoice())
```

  </TabItem>
  <TabItem value="cli">

```bash
# Create data.json: {"fields": {"customerName": "Acme Corp", ...}}
docudevs fill invoice data.json --output filled_invoice.pdf
```

  </TabItem>
  <TabItem value="java">

```java
import ai.docudevs.client.DocuDevsClient;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;

DocuDevsClient client = DocuDevsClient.builder()
    .apiKey(System.getenv("API_KEY"))
    .build();

byte[] filled = client.fillTemplate("invoice", Map.of(
    "customerName", "Acme Corp",
    "invoiceNumber", "INV-2024-001",
    "totalAmount", "1500.00",
    "paid", true
));

Files.write(Path.of("filled_invoice.pdf"), filled);
```

  </TabItem>

  <TabItem value="curl">

```bash
curl -X POST https://api.docudevs.ai/template/fill/invoice \
  -H "Authorization: Bearer $API_KEY" \
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
```

  </TabItem>
</Tabs>

### Filling a Word Template with Tables

Word templates support nested data structures, allowing you to fill tables and repeated sections.

<Tabs
  defaultValue="python"
  values={[
    {label: 'Python SDK', value: 'python'},
    {label: 'Java SDK', value: 'java'},
    {label: 'CLI', value: 'cli'},
    {label: 'cURL', value: 'curl'},
  ]}>
  <TabItem value="python">

```python
async def fill_report():
    fill_request = TemplateFillRequest(
        fields={
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
    )
    
    response = await client.fill(name="sales_report", body=fill_request)
    
    with open("filled_report.docx", "wb") as f:
        f.write(response.content)
```

  </TabItem>
  <TabItem value="cli">

```bash
docudevs fill sales_report report_data.json --output filled_report.docx
```

  </TabItem>
  <TabItem value="java">

```java
import ai.docudevs.client.DocuDevsClient;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;

DocuDevsClient client = DocuDevsClient.builder()
    .apiKey(System.getenv("API_KEY"))
    .build();

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
```

  </TabItem>

  <TabItem value="curl">

```bash
curl -X POST https://api.docudevs.ai/template/fill/sales_report \
  -H "Authorization: Bearer $API_KEY" \
  -H "Content-Type: application/json" \
  -d '{
    "fields": {
      "reportTitle": "Quarterly Sales",
      "items": [
        {"product": "Widget A", "sales": 100},
        {"product": "Widget B", "sales": 200}
      ]
    }
  }' \
  --output filled_report.docx
```

  </TabItem>
</Tabs>

## AcroForm Metadata for Existing PDFs

Use AcroForm metadata when you need field IDs, widget IDs, page numbers, or page-anchor coordinates from an existing fillable PDF.

- Use the **direct metadata endpoint** when you only need the AcroForm structure.
- Use the **async job flow** when you also need processed-job images, source locations, extraction, or overlays tied to a job GUID.

The direct endpoint accepts PDF uploads only. Non-PDF uploads return `400 Bad Request`. A normal PDF without an AcroForm still returns `200 OK` with `fields: []`.

<Tabs
  defaultValue="python"
  values={[
    {label: 'Python SDK', value: 'python'},
    {label: 'cURL', value: 'curl'},
  ]}>
  <TabItem value="python">

```python
import json

with open("fillable-form.pdf", "rb") as f:
    pdf_bytes = f.read()

direct_metadata = await client.extract_acroform_metadata(
    document=pdf_bytes,
    file_name="fillable-form.pdf",
    mime_type="application/pdf",
)

print(f"Direct fields: {len(direct_metadata.get('fields', []))}")
print(json.dumps(direct_metadata.get("fields", [])[:3], indent=2))

job_guid = await client.submit_and_process_document(
    document=pdf_bytes,
    document_mime_type="application/pdf",
    acro_form_metadata=True,
    source_locations=True,
    source_location_granularity="block",
)

job_metadata = await client.get_acroform_metadata(job_guid)
source_locations = await client.get_source_locations(job_guid)

print(f"Async metadata fields: {len(job_metadata.get('fields', []))}")
print(source_locations)
```

  </TabItem>
  <TabItem value="curl">

```bash
curl -X POST https://api.docudevs.ai/document/acroform-metadata \
  -H "Authorization: Bearer $API_KEY" \
  -F "document=@fillable-form.pdf"
```

For processed jobs, fetch the stored artifact after a normal async document-processing run:

```bash
curl -X GET https://api.docudevs.ai/job/result/JOB_GUID/acroform-metadata \
  -H "Authorization: Bearer $API_KEY"
```

  </TabItem>
</Tabs>

For OCR-backed evidence overlays on the same job, combine `acro_form_metadata=True` with `source_locations=True` and then read [Source Locations](/docs/core/source-locations).

## Best Practices

- **Field Naming**: Use clear, consistent names for your form fields (e.g., camelCase like `customerName`).
- **Testing**: Upload your template and inspect the metadata to ensure all fields are detected correctly.
- **Data Types**: Ensure the data you send matches the expected type (e.g., booleans for checkboxes).
- **Configurations**: Combine templates with [Named Configurations](../configuration/Configuration.md) to standardize the filling process if you have complex logic.
