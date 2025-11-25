---
title: Structured Data Extraction
sidebar_position: 1
---

import Tabs from '@theme/Tabs';
import TabItem from '@theme/TabItem';

# Structured Data Extraction

Turn any document into clean, structured JSON data.

DocuDevs allows you to extract specific fields from documents using natural language prompts or strict JSON schemas. This is the core feature of the platform, enabling you to process invoices, contracts, forms, and more with high precision.

## How It Works

1. **Upload** a document (PDF, Image, Word, etc.)
2. **Provide Instructions** (Prompt) or a **Schema**
3. **Receive JSON** data extracted from the document

## Basic Extraction (Prompt-based)

The simplest way to extract data is to describe what you want in plain English.

**Example:** Extracting data from an invoice.

<Tabs
  defaultValue="curl"
  values={[
    {label: 'cURL', value: 'curl'},
    {label: 'Python SDK', value: 'python'},
    {label: 'CLI', value: 'cli'},
  ]}>
  <TabItem value="curl">

```bash
curl -X POST https://api.docudevs.ai/document/upload-files \
  -H "Authorization: Bearer $API_KEY" \
  -F "document=@invoice.pdf" \
  -F "instructions=Extract the invoice number, date, total amount, and vendor name."
```

  </TabItem>
  <TabItem value="python">

```python
from docudevs.docudevs_client import DocuDevsClient
import os

client = DocuDevsClient(token=os.getenv('API_KEY'))

with open("invoice.pdf", "rb") as f:
    guid = await client.submit_and_process_document(
        document=f.read(),
        document_mime_type="application/pdf",
        prompt="Extract the invoice number, date, total amount, and vendor name."
    )

result = await client.wait_until_ready(guid, result_format="json")
print(result)
```

  </TabItem>
  <TabItem value="cli">

```bash
docudevs process invoice.pdf --prompt "Extract the invoice number, date, total amount, and vendor name."
```

  </TabItem>
</Tabs>

**Response:**

```json
{
  "invoice_number": "INV-2024-001",
  "date": "2024-01-15",
  "total_amount": "1500.00 USD",
  "vendor_name": "Acme Corp"
}
```

## Structured Extraction (Schema-based)

For production use cases, you should use a **JSON Schema**. This ensures the API returns data in the exact format you need, with correct data types (numbers, dates, arrays).

**Why use a Schema?**

- **Consistency:** Always get the same JSON structure.
- **Type Safety:** Numbers are numbers, dates are standard strings.
- **Validation:** The AI knows exactly what fields are required.

### Example: Strict Invoice Schema

Let's define a schema for an invoice with line items.

```json
{
  "type": "object",
  "properties": {
    "invoice_number": {"type": "string"},
    "date": {"type": "string", "format": "date"},
    "total": {"type": "number"},
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
  },
  "required": ["invoice_number", "total"]
}
```

<Tabs
  defaultValue="curl"
  values={[
    {label: 'cURL', value: 'curl'},
    {label: 'Python SDK', value: 'python'},
    {label: 'CLI', value: 'cli'},
  ]}>
  <TabItem value="curl">

```bash
# Save schema to schema.json first
curl -X POST https://api.docudevs.ai/document/upload-files/sync \
  -H "Authorization: Bearer $API_KEY" \
  -F "document=@invoice.pdf" \
  -F "schema=@schema.json"
```

  </TabItem>
  <TabItem value="python">

```python
schema = {
    "type": "object",
    "properties": {
        "invoice_number": {"type": "string"},
        "total": {"type": "number"}
    }
}

guid = await client.submit_and_process_document(
    document=doc_bytes,
    document_mime_type="application/pdf",
    schema=schema
)
```

  </TabItem>
  <TabItem value="cli">

```bash
docudevs process invoice.pdf --schema-file schema.json
```

  </TabItem>
</Tabs>

## Type-Safe Extraction with Pydantic (Python)

The Python SDK integrates seamlessly with **Pydantic**, allowing you to define your schema using Python classes. This gives you full type safety and IDE autocompletion.

<Tabs
  defaultValue="python"
  values={[
    {label: 'Python SDK', value: 'python'},
  ]}>
  <TabItem value="python">

```python
from pydantic import BaseModel, Field
from typing import List
import json

# 1. Define your data model
class LineItem(BaseModel):
    description: str
    quantity: int
    price: float

class Invoice(BaseModel):
    number: str = Field(description="The invoice number")
    date: str
    items: List[LineItem]
    total: float

# 2. Generate schema from model
schema = json.dumps(Invoice.model_json_schema())

# 3. Process document
guid = await client.submit_and_process_document(
    document=doc_bytes,
    document_mime_type="application/pdf",
    schema=schema
)

# 4. Validate result back into Pydantic model
result_dict = await client.wait_until_ready(guid, result_format="json")
invoice = Invoice(**result_dict)

print(f"Invoice {invoice.number} total: {invoice.total}")
```

  </TabItem>
</Tabs>

## Advanced Configuration

You can combine extraction with other features like Barcode/QR scanning or specific OCR settings.

### Extracting QR Codes

<Tabs
  defaultValue="curl"
  values={[
    {label: 'cURL', value: 'curl'},
    {label: 'Python SDK', value: 'python'},
    {label: 'CLI', value: 'cli'},
  ]}>
  <TabItem value="curl">

```bash
# config.json: {"barcodes": true}
curl -X POST https://api.docudevs.ai/document/upload-files \
  -H "Authorization: Bearer $API_KEY" \
  -F "document=@invoice.pdf" \
  -F "metadata=@config.json" \
  -F "instructions=Extract QR codes"
```

  </TabItem>
  <TabItem value="python">

```python
guid = await client.submit_and_process_document(
    document=doc_bytes,
    document_mime_type="application/pdf",
    prompt="Extract QR codes",
    barcodes=True  # Enable barcode scanning
)
```

  </TabItem>
  <TabItem value="cli">

```bash
docudevs process invoice.pdf --barcodes --prompt "Extract QR codes"
```

  </TabItem>
</Tabs>

## Next Steps

- **[Map-Reduce Extraction](../core/map-reduce-extraction.md)**: Handle very large documents (50+ pages).
- **[Batch Processing](../core/batch-processing.md)**: Process thousands of documents efficiently.
- **[Named Configurations](../configuration/Configuration.md)**: Save your schemas and prompts for reuse.
