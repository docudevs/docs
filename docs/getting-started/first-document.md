---
title: Your First Document - Detailed Walkthrough
sidebar_position: 3
---

import Tabs from '@theme/Tabs';
import TabItem from '@theme/TabItem';

# Your First Document - Detailed Walkthrough

This comprehensive guide walks you through processing your first document with DocuDevs, explaining each step and exploring the various options available for customizing the extraction process.

## Overview

Document processing with DocuDevs involves four main components:

1. **Document**: The file you want to process (PDF, Word, image, etc.)
2. **Instructions**: Tell the AI what information to extract
3. **Schema**: Define the structure of the output data (optional)
4. **Configuration**: Processing options and settings (optional)

## Step-by-Step Walkthrough

### Step 1: Environment Setup

First, ensure you have the DocuDevs SDK installed and your API key configured:

```python
import os
import asyncio
from docudevs.docudevs_client import DocuDevsClient
import json

# Initialize the client
client = DocuDevsClient(token=os.getenv('API_KEY'))
```

### Step 2: Prepare Your Document

For this walkthrough, we'll use an invoice as an example. You can use any document format:

```python
# Read the document file
document_path = "sample_invoice.pdf"
with open(document_path, "rb") as f:
    document_data = f.read()

print(f"Document size: {len(document_data)} bytes")
print(f"Document type: {document_path.split('.')[-1].upper()}")
```

### Step 3: Basic Processing (Minimal Configuration)

Let's start with the simplest approach - just upload the document and let DocuDevs use its AI to determine what to extract:

<Tabs
  defaultValue="python"
  values={[
    {label: 'Python SDK', value: 'python'},
    {label: 'cURL', value: 'curl'},
    {label: 'CLI', value: 'cli'},
  ]}>
  <TabItem value="python">

```python
async def basic_processing():
    # Submit document for AI extraction (non-OCR)
    job_id = await client.submit_and_process_document(
        document=document_data,
        document_mime_type="application/pdf"
    )
    print(f"Job submitted: {job_id}")
    
    # Wait for completion
    result = await client.wait_until_ready(job_id, result_format="json")
    
    print("Basic Processing Result:")
    print(json.dumps(result, indent=2))
    
    return result

# Run basic processing
result = await basic_processing()
```

  </TabItem>
  <TabItem value="curl">
```bash
# Upload and process in one step
curl -X POST https://api.docudevs.ai/document/upload-files \
  -H "Authorization: Bearer $API_KEY" \
  -F "document=@sample_invoice.pdf"

# Check status (use GUID from upload response)

curl -X GET <https://api.docudevs.ai/job/status/{GUID}> \
  -H "Authorization: Bearer $API_KEY"

# Get results once complete

curl -X GET <https://api.docudevs.ai/job/result/{GUID}> \
  -H "Authorization: Bearer $API_KEY"

```
  </TabItem>
  <TabItem value="cli">
```bash
docudevs process sample_invoice.pdf
```

  </TabItem>
</Tabs>

### Step 4: Processing with Custom Instructions

Now let's add specific instructions to guide the AI extraction:

```python
async def processing_with_instructions():
    instruction = """
    Extract all invoice information including:
    - Invoice number and date
    - Vendor/supplier details
    - Customer/bill-to information  
    - All line items with descriptions, quantities, and prices
    - Tax information
    - Total amounts
    - Payment terms and due date
    """
    
    job_id = await client.submit_and_process_document(
        document=document_data,
        document_mime_type="application/pdf",
        prompt=instruction
    )
    result = await client.wait_until_ready(job_id, result_format="json")
    
    print("Processing with Instructions Result:")
    print(json.dumps(result, indent=2))
    
    return result

# Run processing with instructions
result = await processing_with_instructions()
```

### Step 5: Processing with Custom Schema

For even more control, define exactly how you want the output structured:

```python
async def processing_with_schema():
    instruction = "Extract invoice data according to the provided schema"
    
    schema = {
        "invoice_header": {
            "invoice_number": "The invoice number",
            "invoice_date": "Invoice date in YYYY-MM-DD format",
            "due_date": "Payment due date in YYYY-MM-DD format"
        },
        "vendor": {
            "name": "Vendor/supplier company name",
            "address": "Complete vendor address",
            "contact": "Phone or email contact information"
        },
        "customer": {
            "name": "Customer/bill-to company name",
            "address": "Complete customer address"
        },
        "line_items": [
            {
                "description": "Item or service description",
                "quantity": "Quantity as number",
                "unit_price": "Price per unit as number",
                "total": "Line total as number"
            }
        ],
        "summary": {
            "subtotal": "Subtotal amount as number",
            "tax_rate": "Tax percentage as number",
            "tax_amount": "Tax amount as number",
            "total_amount": "Final total as number",
            "currency": "Currency code (USD, EUR, etc.)"
        },
        "payment_terms": "Payment terms description"
    }
    
    job_id = await client.submit_and_process_document(
        document=document_data,
        document_mime_type="application/pdf",
        prompt=instruction,
        schema=json.dumps(schema)
    )
    result = await client.wait_until_ready(job_id, result_format="json")
    
    print("Processing with Schema Result:")
    print(json.dumps(result, indent=2))
    
    return result

# Run processing with schema
result = await processing_with_schema()
```

### Step 6: Using Named Configurations

For consistent processing across multiple documents, save your settings as a named configuration:

```python
async def create_and_use_configuration():
    # First, create a named configuration
    config_name = "invoice_processing_standard"
    
    configuration = {
        "prompt": "Extract comprehensive invoice data",
        "schema": {
            "invoice_number": "Invoice number",
            "date": "Invoice date in YYYY-MM-DD format", 
            "vendor": "Vendor name",
            "total": "Total amount as number",
            "line_items": [
                {
                    "description": "Item description",
                    "amount": "Amount as number"
                }
            ]
        },
        "llm": "HIGH"
    }
    
    # Save the configuration
    from docudevs.models import UploadCommand
    # Convert dict to UploadCommand or pass dict if client supports it (client.save_configuration expects UploadCommand)
    # The SDK usually requires the model object, but let's assume we construct it or pass a dict that the client handles (if it does).
    # Actually, looking at sdk-methods.md, we use UploadCommand.
    
    # Let's rewrite this to be correct with the SDK
    cmd = UploadCommand(
        prompt=configuration["prompt"],
        schema=json.dumps(configuration["schema"]), # Schema must be string
        llm=configuration["llm"]
    )
    
    save_response = await client.save_configuration(config_name, cmd)
    print(f"Configuration saved: {config_name}")
    
    # Use the saved configuration
    # Note: process_document_with_configuration takes a GUID of an uploaded doc, 
    # or submit_and_process_document_with_configuration takes the doc.
    # The example in the file uses process_document_with_configuration with a job_id from previous step.
    
    response = await client.process_document_with_configuration(
        guid=job_id,  # From previous upload
        configuration_name=config_name
    )
    
    result = await client.wait_until_ready(response.parsed.guid, result_format="json")
    print("Processing with Named Configuration Result:")
    print(json.dumps(result, indent=2))
    
    return result
```

## Understanding the Results

The processing results typically include:

### Success Response Structure

```json
{
  "invoice_number": "INV-2024-001",
  "invoice_date": "2024-01-15",
  "vendor": {
    "name": "Acme Corporation",
    "address": "123 Business St, City, State 12345"
  },
  "line_items": [
    {
      "description": "Professional Services",
      "quantity": 10,
      "unit_price": 125.00,
      "total": 1250.00
    }
  ],
  "summary": {
    "subtotal": 1250.00,
    "tax_amount": 100.00,
    "total_amount": 1350.00,
    "currency": "USD"
  }
}
```

### Processing Status Flow

1. **Submitted**: Document uploaded and queued for processing
2. **Processing**: AI is analyzing the document
3. **Completed**: Extraction finished successfully
4. **Failed**: Error occurred during processing

## Different Document Types

DocuDevs can process various document types. Here are optimized approaches for common formats:

### PDF Documents

```python
# Standard PDF processing
mime_type = "application/pdf"
# No special configuration needed
```

### Images (JPG, PNG)

```python
# Image processing often benefits from OCR
mime_type = "image/jpeg"  # or "image/png"

# Consider using OCR-specific instructions
instruction = "First perform OCR to extract all text, then analyze the content for key information"
```

### Word Documents

```python
# Word document processing
mime_type = "application/vnd.openxmlformats-officedocument.wordprocessingml.document"

# Word docs often have structured content
instruction = "Extract information from this structured document, preserving any tables or sections"
```

### Scanned Documents

```python
# Scanned documents need OCR
instruction = "This is a scanned document. First extract all text using OCR, then analyze for key information"

# Consider using higher quality settings
configuration = {
    "ocr": "PREMIUM",  # More accurate for scanned docs
    "llm": "HIGH"      # Better understanding of OCR'd text
}
```

## Error Handling Best Practices

Always implement robust error handling:

```python
async def robust_document_processing(document_path):
    try:
        # Read document
        with open(document_path, "rb") as f:
            document_data = f.read()
        
        # Submit for processing using convenience method
        job_id = await client.submit_and_process_document(
            document=document_data,
            document_mime_type="application/pdf",
            prompt="Extract key information from this document"
        )
        print(f"Processing job {job_id}...")
        
        # Wait for completion with timeout
        try:
            result = await client.wait_until_ready(job_id, timeout=300, result_format="json")  # 5 minute timeout
            return result
        
        except TimeoutError:
            print(f"Processing timed out for job {job_id}")
            # Check status to see current state
            status_response = await client.status(job_id)
            print(f"Current status: {status_response.parsed}")
            return None
            
    except FileNotFoundError:
        print(f"Document file not found: {document_path}")
        return None
        
    except Exception as e:
        print(f"Error processing document: {e}")
        return None

# Usage
result = await robust_document_processing("sample_invoice.pdf")
if result:
    print("Processing successful!")
else:
    print("Processing failed - check errors above")
```

## Advanced Processing Options

### Batch Processing Multiple Documents

```python
async def process_multiple_documents(file_paths):
    results = []
    
    for file_path in file_paths:
        try:
            with open(file_path, "rb") as f:
                document_data = f.read()
            
            job_id = await client.submit_and_process_document(
                document=document_data,
                document_mime_type="application/pdf",
                prompt="Extract invoice data"
            )
            results.append({
                "file": file_path,
                "job_id": job_id,
                "status": "submitted"
            })
            
        except Exception as e:
            results.append({
                "file": file_path,
                "job_id": None,
                "status": f"error: {e}"
            })
    
    # Wait for all jobs to complete
    for item in results:
        if item["job_id"]:
            try:
                result = await client.wait_until_ready(item["job_id"], result_format="json")
                item["result"] = result
                item["status"] = "completed"
            except Exception as e:
                item["status"] = f"processing_error: {e}"
    
    return results

# Process multiple invoices
file_list = ["invoice1.pdf", "invoice2.pdf", "invoice3.pdf"]
batch_results = await process_multiple_documents(file_list)

for item in batch_results:
    print(f"{item['file']}: {item['status']}")
```

### Using Cases for Organization

```python
async def process_with_case_organization():
    # Create a case for related documents
    case_data = {
        "name": "January 2024 Vendor Invoices",
        "description": "All vendor invoices received in January 2024"
    }
    
    case_response = await client.create_case(case_data)
    case_id = case_response.parsed.id
    print(f"Created case: {case_id}")
    
    # Upload documents to the case
    invoice_files = ["vendor1_invoice.pdf", "vendor2_invoice.pdf"]
    
    for filename in invoice_files:
        with open(filename, "rb") as f:
            document_data = f.read()
        
        doc_response = await client.upload_case_document(
            case_id=case_id,
            document=document_data,
            filename=filename
        )
        
        print(f"Uploaded {filename} to case {case_id}")
        
        # Process the document
        document_id = doc_response.parsed.document_id
        process_response = await client.process_document(
            guid=document_id,
            instruction="Extract invoice data"
        )
        
    result = await client.wait_until_ready(process_response.parsed.guid, result_format="json")
        print(f"Processed {filename}")
    
    # List all documents in the case
    docs_response = await client.list_case_documents(case_id)
    print(f"Case contains {len(docs_response.parsed.content)} documents")

# Run case-based processing
await process_with_case_organization()
```

## Performance Tips

### Optimizing Processing Speed

1. **Use appropriate LLM models**:
   - `gpt-3.5-turbo`: Faster, good for simple extraction
   - `gpt-4`: Slower, better for complex documents

2. **Provide clear, specific instructions**:
   - Good: "Extract invoice number, date, vendor name, and total amount"
   - Poor: "Extract all important information"

3. **Use structured schemas** for consistent output format

4. **Process documents in parallel** when handling multiple files

### Quality Optimization

1. **Include document context** in instructions
2. **Specify output format** requirements (dates, numbers, etc.)
3. **Use examples** in instructions when needed
4. **Test with sample documents** before processing large batches

## What's Next?

Now that you understand document processing fundamentals:

1. **Explore Advanced Features**:
   - [Cases](/docs/advanced/cases) - Organize related documents
   - [Operations](/docs/advanced/operations) - Error analysis and post-processing
   - [Templates](/docs/templates/Templates) - Reusable document templates

2. **Learn Integration Patterns**:
   - [Use Cases](/docs/integration/use-cases) - Real-world implementation examples  
   - [Best Practices](/docs/integration/best-practices) - Production deployment guidelines
   - [Troubleshooting](/docs/integration/troubleshooting) - Debug common issues

3. **Reference Materials**:
   - [SDK Methods](/docs/reference/sdk-methods) - Complete method documentation
   - [Error Codes](/docs/reference/error-codes) - Error handling reference
   - [Configuration](/docs/configuration/Configuration) - Settings management

Ready to build more sophisticated document processing workflows? Continue with [Cases](/docs/advanced/cases) to learn about organizing and managing multiple documents!
