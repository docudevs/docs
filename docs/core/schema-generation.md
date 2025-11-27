---
title: Schema Generation
description: Automatically generate JSON schemas from your documents using AI.
sidebar_position: 5
---

import Tabs from '@theme/Tabs';
import TabItem from '@theme/TabItem';

# Schema Generation

Automatically generate JSON schemas from your documents using AI.

Defining a perfect JSON schema for a complex document can be tedious. DocuDevs can analyze your document and generate a schema for you, which you can then refine and use for extraction.

## How It Works

1. **Upload** a representative document (e.g., a sample invoice).
2. **Instruct** the AI on what fields matter (optional).
3. **Receive** a valid JSON Schema ready for use.

## Generating a Schema

<Tabs
  defaultValue="python"
  values={[
    {label: 'Python SDK', value: 'python'},
    {label: 'CLI', value: 'cli'},
    {label: 'cURL', value: 'curl'},
  ]}>
  <TabItem value="python">

```python
from docudevs.docudevs_client import DocuDevsClient
import os
import asyncio

client = DocuDevsClient(token=os.getenv('API_KEY'))

async def generate_my_schema():
    with open("sample-invoice.pdf", "rb") as f:
        document_data = f.read()

    # Submit schema generation job
    job_guid = await client.generate_schema(
        document=document_data,
        document_mime_type="application/pdf",
        instructions="Focus on extracting the invoice number, date, and all line items with prices."
    )
    
    # Wait for the result
    result = await client.wait_until_ready(job_guid)
    
    # The result is the JSON Schema string
    print("Generated Schema:")
    print(result.result)

if __name__ == "__main__":
    asyncio.run(generate_my_schema())
```

  </TabItem>
  <TabItem value="cli">

```bash
# Generate schema and save to file
docudevs generate-schema sample-invoice.pdf \
  --instructions "Focus on invoice details" \
  --output invoice-schema.json
```

  </TabItem>
  <TabItem value="curl">

```bash
curl -X POST https://api.docudevs.ai/document/generate-schema \
  -H "Authorization: Bearer $API_KEY" \
  -F "document=@sample-invoice.pdf" \
  -F "instructions=Focus on invoice details"
```

  </TabItem>
</Tabs>

## Using the Generated Schema

Once you have the schema, you can use it to process documents with strict validation.

### 1. Review and Refine

The generated schema is a starting point. You might want to:

- Rename fields to match your database columns.
- Change data types (e.g., `string` to `number`).
- Add `required` fields.

### 2. Use in Extraction

<Tabs
  defaultValue="python"
  values={[
    {label: 'Python SDK', value: 'python'},
    {label: 'CLI', value: 'cli'},
  ]}>
  <TabItem value="python">

```python
# Use the schema string you got from the generation step
my_schema = { ... } 

guid = await client.submit_and_process_document(
    document=new_document_data,
    document_mime_type="application/pdf",
    schema=my_schema
)
```

  </TabItem>
  <TabItem value="cli">

```bash
docudevs process new-invoice.pdf --schema-file invoice-schema.json
```

  </TabItem>
</Tabs>

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
