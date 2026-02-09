---
title: Quick Start - 5 Minute Tutorial
description: Process your first document with DocuDevs in 5 minutes using the Python SDK, CLI, or cURL.
sidebar_position: 1
---

import Tabs from '@theme/Tabs';
import TabItem from '@theme/TabItem';

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

- Python 3.9+ installed on your system
- A DocuDevs API key ([contact us](mailto:support@docudevs.ai) to get started)
- A sample document (PDF, Word, or image file)

## Step 1: Install the SDK (30 seconds)

<Tabs
  defaultValue="pip"
  values={[
    {label: 'pip', value: 'pip'},
    {label: 'poetry', value: 'poetry'},
    {label: 'uv', value: 'uv'},
  ]}>
  <TabItem value="pip">

```bash
pip install docu-devs-api-client
```

  </TabItem>
  <TabItem value="poetry">
```bash
poetry add docu-devs-api-client
```
  </TabItem>
  <TabItem value="uv">

```bash
# Optional: create a virtual environment
uv venv
source .venv/bin/activate

# Install the SDK
uv pip install docu-devs-api-client
```

  </TabItem>
</Tabs>

## Step 2: Set Your API Key (10 seconds)

```bash
export API_KEY="your-api-key-here"
```

## Step 3: Process Your First Document (2 minutes)

<Tabs
  defaultValue="python"
  values={[
    {label: 'Python SDK', value: 'python'},
    {label: 'CLI', value: 'cli'},
    {label: 'cURL', value: 'curl'},
  ]}>
  <TabItem value="python">

Create a simple Python script to process a document:

```python
import os
import asyncio
from docudevs.docudevs_client import DocuDevsClient

async def process_document():
    # Initialize client
    client = DocuDevsClient(token=os.getenv('API_KEY'))
    
    # Read your document
    with open("your-document.pdf", "rb") as f:
        document_data = f.read()
    
    # Submit for AI extraction (non-OCR)
    job_id = await client.submit_and_process_document(
        document=document_data,
        document_mime_type="application/pdf",
        prompt="Extract all key information from this document"
    )
    print(f"Document submitted for processing. Job ID: {job_id}")
    
    # Wait for processing to complete; request canonical JSON output
    data = await client.wait_until_ready(job_id, result_format="json")
    print("Processing complete!")
    
    # Display results as JSON (extraction returns structured data)
    import json
    print("\nExtracted Data:")
    print(json.dumps(data, indent=2))

# Run the processing
asyncio.run(process_document())
```

  </TabItem>
  <TabItem value="cli">

```bash
# Process the document with a prompt
docudevs process your-document.pdf \
  --prompt "Extract all key information from this document"
```

  </TabItem>
  <TabItem value="curl">

```bash
curl -s -S -X POST https://api.docudevs.ai/document/upload-files \
     -H "Authorization: $API_KEY" \
     -F "document=@your-document.pdf" \
     -F "prompt=Extract all key information from this document"
```

  </TabItem>
</Tabs>

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

```python
async def analyze_document_with_ai():
    client = DocuDevsClient(token=os.getenv('API_KEY'))
    
    # Read your document
    with open("your-document.pdf", "rb") as f:
        document_data = f.read()
    
    # First, process with OCR to extract text
    ocr_job_id = await client.submit_and_ocr_document(
        document=document_data,
        document_mime_type="application/pdf",
        ocr="PREMIUM",
        ocr_format="markdown"
    )
    
    # Wait for OCR to complete
    await client.wait_until_ready(ocr_job_id)
    
    # Now ask AI to analyze the document
    analysis = await client.submit_and_wait_for_generative_task(
        parent_job_id=ocr_job_id,
        prompt="Summarize this document and explain what type of document it is and its main purpose."
    )
    
  # Display the AI response
  print("\nAI Analysis:")
  print(analysis.generated_text)

# Run the analysis
asyncio.run(analyze_document_with_ai())
```

This approach is perfect for:

- **Document Summaries**: Get quick overviews of long documents
- **Document Classification**: Automatically categorize document types
- **Question Answering**: Ask specific questions about document content
- **Content Transformation**: Convert documents to different formats or styles

## Next Steps - Choose Your Path

### 🚀 **For Developers**

- [Complete First Document Guide](/docs/getting-started/first-document) - Learn advanced processing options
- [Simple Documents](/docs/basics/SimpleDocuments) - Understand document processing fundamentals
- [Operations](/docs/advanced/operations) - Use generative tasks and error analysis
- [SDK Methods Reference](/docs/reference/sdk-methods) - Explore all available functions

### 📋 **For Business Users**

- [Use Cases](/docs/integration/use-cases) - See real-world application examples
- [Cases](/docs/advanced/cases) - Organize documents into collections
- [Templates](/docs/templates/Templates) - Create reusable document templates

### ⚙️ **For System Integrators**

- [Best Practices](/docs/integration/best-practices) - Production deployment guidelines
- [Configuration](/docs/configuration/Configuration) - Save and reuse processing settings
- [Troubleshooting](/docs/integration/troubleshooting) - Debug common issues

## Try Different Document Types

DocuDevs works with various document formats:

<Tabs
  defaultValue="invoice"
  values={[
    {label: 'Invoice', value: 'invoice'},
    {label: 'Receipt', value: 'receipt'},
    {label: 'Contract', value: 'contract'},
    {label: 'Form', value: 'form'},
  ]}>
  <TabItem value="invoice">

```python
instruction = "Extract invoice number, date, vendor, total amount, and all line items"
```

  </TabItem>
  <TabItem value="receipt">
```python
instruction = "Extract merchant name, date, total amount, and purchased items"
```
  </TabItem>
  <TabItem value="contract">
```python
instruction = "Extract parties involved, contract dates, key terms, and obligations"
```
  </TabItem>
  <TabItem value="form">
```python
instruction = "Extract all form fields and their values"
```
  </TabItem>
</Tabs>

## Common Customizations

### Add Custom Schema

```python
import json

# Define a proper JSON Schema
schema = {
    "type": "object",
    "properties": {
        "invoice_number": {"type": "string"},
        "date": {"type": "string", "format": "date"},
        "vendor": {"type": "string"},
        "total": {"type": "number"},
        "currency": {"type": "string"}
    },
    "required": ["invoice_number", "total"]
}

job_id = await client.submit_and_process_document(
    document=document_data,
    document_mime_type="application/pdf",
    prompt="Extract invoice data according to the schema",
    schema=json.dumps(schema)  # Schema must be a JSON string
)
```

### Process Multiple Similar Documents (Batch)

For processing many similar documents with the same extraction configuration, use **batch processing**:

```python
import json

# 1. Create a batch
batch_guid = await client.create_batch(max_concurrency=3)

# 2. Upload documents to the batch
for filename in ["invoice1.pdf", "invoice2.pdf", "invoice3.pdf"]:
    with open(filename, "rb") as f:
        await client.upload_batch_document(
            batch_guid=batch_guid,
            document=f.read(),
            mime_type="application/pdf",
            file_name=filename,
        )

# 3. Process all documents with the same configuration
schema = {
    "type": "object",
    "properties": {
        "invoice_number": {"type": "string"},
        "total": {"type": "number"}
    }
}

await client.process_batch(
    batch_guid=batch_guid,
    mime_type="application/pdf",
    prompt="Extract invoice data",
    schema=json.dumps(schema),
)

# 4. Wait for all results
results = await client.wait_until_ready(batch_guid, result_format="json")
for i, result in enumerate(results):
    print(f"Document {i}: {result}")
```

> **Tip**: Use [Cases](/docs/advanced/cases) when you need to organize documents for search and retrieval (RAG), not for batch processing.

## Getting Help

- **Documentation**: Browse our [complete documentation](/docs/basics/install)
- **Examples**: Check out [integration examples](/docs/integration/use-cases)
- **Support**: Email us at [support@docudevs.ai](mailto:support@docudevs.ai)
- **Issues**: Found a bug? Let us know!

## What's Next?

You've successfully processed your first document! Now you're ready to:

1. **Explore Advanced Features**: Learn about [Cases](/docs/advanced/cases) for document organization
2. **Customize Processing**: Dive into [Templates](/docs/templates/Templates) and [Configuration](/docs/configuration/Configuration)
3. **Build Integration**: Review [Best Practices](/docs/integration/best-practices) for production use
4. **Scale Your Solution**: Discover performance optimization techniques

Ready to build something amazing with DocuDevs? Let's continue with the [detailed first document guide](/docs/getting-started/first-document)!
