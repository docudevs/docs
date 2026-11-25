---
title: Schema Generation
description: Automatically generate JSON schemas from your documents using AI.
sidebar_position: 5
---

# Schema Generation

Automatically generate JSON schemas from your documents using AI.

Defining a perfect JSON schema for a complex document can be tedious. DocuDevs can analyze your document and generate a schema for you, which you can then refine and use for extraction.

## How It Works

1. **Upload** a representative document (e.g., a sample invoice).
2. **Instruct** the AI on what fields matter (optional).
3. **Receive** a valid JSON Schema ready for use.

## Generating a Schema

The example uploads a sample invoice with instructions, waits for the schema job and prints the generated JSON Schema. Save the printed schema as `invoice-schema.json` to use it in the next step. Schema generation is available through the API, the Python and Java SDKs; the CLI has no schema generation command.

<Example name="extraction/generate-schema" />

## Using the Generated Schema

Once you have the schema, you can use it to process documents with strict validation.

### 1. Review and Refine

The generated schema is a starting point. You might want to:

- Rename fields to match your database columns.
- Change data types (e.g., `string` to `number`).
- Add `required` fields.

### 2. Use in Extraction

Pass the reviewed schema as the extraction schema. The example loads `invoice-schema.json` and processes a new invoice with it.

<Example name="extraction/process-with-generated-schema" />

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
