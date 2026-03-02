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

**Supported Formats:**

- **PDF Forms**: Standard AcroForms.
- **Word Documents**: `.docx` files with placeholders or content controls.
- **Excel Spreadsheets**: `.xlsx` files.
- **PowerPoint Presentations**: `.pptx` files.

## Managing Templates

### Uploading a Template

Upload a document to create a new template. DocuDevs will automatically detect the fields available for filling.

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
from docudevs.docudevs_client import DocuDevsClient
import os
import asyncio

client = DocuDevsClient(token=os.getenv('API_KEY'))

async def upload_invoice_template():
    with open('invoice_template.pdf', 'rb') as f:
        # Upload template with name "invoice"
        response = await client.upload_template(
            name="invoice",
            document=f,
            mime_type="application/pdf"
        )
        
        # The response contains the detected form fields
        print(f"Template uploaded. Detected fields: {response.parsed.form_fields}")

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
import ai.docudevs.client.generated.api.DocumentApi;
import ai.docudevs.client.generated.internal.ApiClient;
import java.io.File;

ApiClient apiClient = new ApiClient();
apiClient.updateBaseUri("https://api.docudevs.ai");
apiClient.setRequestInterceptor(req ->
    req.header("Authorization", "Bearer " + System.getenv("API_KEY"))
);

DocumentApi documentApi = new DocumentApi(apiClient);
Object response = documentApi.uploadTemplate("invoice", new File("invoice_template.pdf"));

System.out.println(response);
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
import ai.docudevs.client.generated.api.TemplateApi;
import ai.docudevs.client.generated.internal.ApiClient;
import ai.docudevs.client.generated.model.DocumentTemplate;
import java.util.List;

ApiClient apiClient = new ApiClient();
apiClient.updateBaseUri("https://api.docudevs.ai");
apiClient.setRequestInterceptor(req ->
    req.header("Authorization", "Bearer " + System.getenv("API_KEY"))
);

TemplateApi templateApi = new TemplateApi(apiClient);
List<DocumentTemplate> templates = templateApi.listTemplates();

for (DocumentTemplate template : templates) {
    System.out.println(template.getName() + " created=" + template.getCreatedAt());
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
metadata = await client.metadata("invoice")
print(f"Fields for {metadata.name}:")
for field in metadata.form_fields:
    print(f"- {field.name} ({field.type})")
```

  </TabItem>
  <TabItem value="cli">

```bash
docudevs template-metadata invoice
```

  </TabItem>
  <TabItem value="java">

```java
import ai.docudevs.client.generated.api.TemplateApi;
import ai.docudevs.client.generated.internal.ApiClient;
import ai.docudevs.client.generated.model.PDFField;
import java.util.List;

ApiClient apiClient = new ApiClient();
apiClient.updateBaseUri("https://api.docudevs.ai");
apiClient.setRequestInterceptor(req ->
    req.header("Authorization", "Bearer " + System.getenv("API_KEY"))
);

TemplateApi templateApi = new TemplateApi(apiClient);
List<PDFField> fields = templateApi.metadata("invoice");

for (PDFField field : fields) {
    System.out.println(field.getName() + " (" + field.getType() + ")");
}
```

  </TabItem>

  <TabItem value="curl">

```bash
curl -X GET https://api.docudevs.ai/template/metadata/invoice \
  -H "Authorization: Bearer $API_KEY"
```

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
import ai.docudevs.client.generated.api.TemplateApi;
import ai.docudevs.client.generated.internal.ApiClient;
import ai.docudevs.client.generated.model.PDFField;
import java.util.List;

ApiClient apiClient = new ApiClient();
apiClient.updateBaseUri("https://api.docudevs.ai");
apiClient.setRequestInterceptor(req ->
    req.header("Authorization", "Bearer " + System.getenv("API_KEY"))
);

TemplateApi templateApi = new TemplateApi(apiClient);
List<PDFField> deletedFields = templateApi.deleteTemplate("invoice");

System.out.println("Deleted template invoice (fields tracked: " + deletedFields.size() + ")");
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
    
    response = await client.fill(name="invoice", body=fill_request)
    
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
import ai.docudevs.client.generated.api.TemplateApi;
import ai.docudevs.client.generated.internal.ApiClient;
import ai.docudevs.client.generated.model.TemplateFillRequest;
import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.Map;

ApiClient apiClient = new ApiClient();
apiClient.updateBaseUri("https://api.docudevs.ai");
apiClient.setRequestInterceptor(req ->
    req.header("Authorization", "Bearer " + System.getenv("API_KEY"))
);

TemplateApi templateApi = new TemplateApi(apiClient);
TemplateFillRequest fillRequest = new TemplateFillRequest().fields(
    Map.of(
        "customerName", "Acme Corp",
        "invoiceNumber", "INV-2024-001",
        "totalAmount", "1500.00",
        "paid", true
    )
);

File filled = templateApi.fill("invoice", fillRequest);
Files.copy(filled.toPath(), Path.of("filled_invoice.pdf"), StandardCopyOption.REPLACE_EXISTING);
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
import ai.docudevs.client.generated.api.TemplateApi;
import ai.docudevs.client.generated.internal.ApiClient;
import ai.docudevs.client.generated.model.TemplateFillRequest;
import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.List;
import java.util.Map;

ApiClient apiClient = new ApiClient();
apiClient.updateBaseUri("https://api.docudevs.ai");
apiClient.setRequestInterceptor(req ->
    req.header("Authorization", "Bearer " + System.getenv("API_KEY"))
);

TemplateApi templateApi = new TemplateApi(apiClient);

TemplateFillRequest fillRequest = new TemplateFillRequest().fields(
    Map.of(
        "reportTitle", "Quarterly Sales",
        "date", "2024-04-01",
        "items", List.of(
            Map.of("product", "Widget A", "sales", 100, "revenue", 5000),
            Map.of("product", "Widget B", "sales", 200, "revenue", 8000),
            Map.of("product", "Widget C", "sales", 50, "revenue", 2500)
        ),
        "summary", Map.of("totalRevenue", 15500, "growth", "15%")
    )
);

File filled = templateApi.fill("sales_report", fillRequest);
Files.copy(filled.toPath(), Path.of("filled_report.docx"), StandardCopyOption.REPLACE_EXISTING);
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

## Best Practices

- **Field Naming**: Use clear, consistent names for your form fields (e.g., camelCase like `customerName`).
- **Testing**: Upload your template and inspect the metadata to ensure all fields are detected correctly.
- **Data Types**: Ensure the data you send matches the expected type (e.g., booleans for checkboxes).
- **Configurations**: Combine templates with [Named Configurations](../configuration/configuration.md) to standardize the filling process if you have complex logic.
