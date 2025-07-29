# Schema Generation

Generate JSON schemas automatically from your documents using AI analysis.

## Overview

The Schema Generation feature analyzes your documents and creates structured JSON schemas that capture the key data fields and their types. This is particularly useful for:

- Creating extraction templates for similar documents
- Understanding document structure before processing
- Building data validation rules
- Standardizing data extraction across document types

## How It Works

1. Upload a representative document
2. Optionally provide instructions for what to focus on
3. AI analyzes the document structure and content
4. Returns a JSON schema with identified fields and data types

## API Usage

### Generate Schema Endpoint

```http
POST /document/generate-schema
Content-Type: multipart/form-data

document: [file]
instructions: [optional text]
```

### Response

```json
{
  "guid": "uuid-string",
  "status": "PENDING"
}
```

## SDK Examples

### Python

```python
from docudevs_client import DocuDevsClient

client = DocuDevsClient(token="your-api-key")

# Basic schema generation
with open("sample-invoice.pdf", "rb") as f:
    response = await client.generate_schema(
        document=f.read(),
        document_mime_type="application/pdf"
    )

# Wait for completion and get schema
guid = response.parsed.guid
result = await client.wait_until_ready(guid)
schema = result.result  # JSON schema string
```

### CLI Usage

```bash
# Basic schema generation
docudevs generate-schema document.pdf

# With custom instructions
docudevs generate-schema document.pdf --instructions "Extract contact information and addresses"

# Save schema to file
docudevs generate-schema document.pdf --output schema.json
```

### cURL Example

```bash
curl -X POST "https://api.docudevs.ai/document/generate-schema" \
  -H "Authorization: Bearer YOUR_API_KEY" \
  -F "document=@sample.pdf" \
  -F "instructions=Focus on financial data"
```

## Schema Output Format

Generated schemas follow JSON Schema Draft 7 specification:

```json
{
  "$schema": "http://json-schema.org/draft-07/schema#",
  "type": "object",
  "title": "Document Schema",
  "properties": {
    "invoice_number": {
      "type": "string",
      "description": "Unique invoice identifier"
    },
    "date": {
      "type": "string",
      "format": "date",
      "description": "Invoice date"
    },
    "total_amount": {
      "type": "number",
      "description": "Total invoice amount"
    },
    "items": {
      "type": "array",
      "items": {
        "type": "object",
        "properties": {
          "description": {"type": "string"},
          "quantity": {"type": "number"},
          "unit_price": {"type": "number"}
        }
      }
    }
  },
  "required": ["invoice_number", "date", "total_amount"]
}
```

## Using Generated Schemas

### For Document Processing

Use the generated schema as extraction instructions:

```python
# Generate schema first
schema_response = await client.generate_schema(
    document=sample_doc,
    document_mime_type="application/pdf"
)
schema = await client.wait_until_ready(schema_response.parsed.guid)

# Use schema for extraction
extraction_response = await client.upload_and_process(
    document=target_doc,
    document_mime_type="application/pdf",
    instructions=f"Extract data according to this schema: {schema.result}"
)
```

### For Template Creation

```python
# Create template from generated schema
await client.upload_template(
    name="invoice-template",
    template_file=template_data,
    instructions=schema.result
)
```

## Best Practices

### Document Selection
- Use representative, well-structured documents
- Choose documents with clear, readable text
- Avoid documents with poor scan quality

### Instructions
- Be specific about the data you want to capture
- Mention any special formatting requirements
- Include examples of edge cases to handle

### Schema Refinement
- Review generated schemas before use
- Test with multiple document samples
- Refine based on processing results

## Common Use Cases

### Invoice Processing
```python
instructions = """
Focus on:
- Invoice number and date
- Vendor information (name, address)
- Line items with descriptions, quantities, prices
- Tax amounts and total
- Payment terms
"""
```

### Contract Analysis
```python
instructions = """
Extract:
- Contract parties and their roles
- Key dates (start, end, renewal)
- Financial terms and amounts
- Obligations and deliverables
- Termination clauses
"""
```

### Form Processing
```python
instructions = """
Capture:
- All form fields and their values
- Checkbox and radio button selections
- Signature information
- Date fields in consistent format
"""
```

## Error Handling

```python
try:
    response = await client.generate_schema(
        document=document_data,
        document_mime_type="application/pdf"
    )
    
    if response.status_code != 200:
        print(f"Error: {response.content}")
        return
        
    result = await client.wait_until_ready(response.parsed.guid)
    schema = result.result
    
except Exception as e:
    print(f"Schema generation failed: {e}")
```

## Limitations

- Maximum document size: 10 MB
- Supported formats: PDF, DOCX, TXT, images
- Processing time varies by document complexity
- Generated schemas may need manual refinement for edge cases

## Next Steps

- [Use schemas for document processing](../basics/SimpleDocuments.md)
- [Create templates from schemas](../templates/Templates.md)
- [Process documents in batches](../advanced/cases.md)