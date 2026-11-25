---
title: Your First Document - Detailed Walkthrough
description: Step-by-step guide to processing documents with DocuDevs including instructions, schemas, configurations, and error handling.
sidebar_position: 3
---

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

First, ensure you have the DocuDevs SDK installed and your API key set in the `API_KEY` environment variable (see [Installation & Setup](/docs/basics/install)). Every example below reads the key from `API_KEY` and talks to `https://api.docudevs.ai`.

### Step 2: Prepare Your Document

For this walkthrough, we'll use an invoice named `sample_invoice.pdf` as an example. You can use any document format. The examples read the file from the current directory.

### Step 3: Basic Processing (Minimal Configuration)

Let's start with the simplest approach - just upload the document and let DocuDevs use its AI to determine what to extract:

<Example name="first-document/process-minimal" />

The cURL example uses [`jq`](https://jqlang.org/) to read values from responses.

### Step 4: Processing with Custom Instructions

Now let's add specific instructions to guide the AI extraction. The prompt tells the AI which fields matter and how to report them:

```text
Extract all invoice information including:
- Invoice number and date
- Vendor/supplier details
- Customer/bill-to information
- All line items with descriptions, quantities, and prices
- Tax information
- Total amounts
- Payment terms and due date
```

Pass the prompt with the document:

<Example name="quick-start/process-document" />

### Step 5: Processing with Custom Schema

For even more control, define exactly how you want the output structured with a JSON Schema. Use nested objects for grouped data (a `vendor` with a `name` and an `address`) and arrays of objects for repeating data such as invoice line items:

<Example name="quick-start/custom-schema" />

### Step 6: Using Named Configurations

For consistent processing across multiple documents, save your settings as a named configuration and process documents with it by name:

<Example name="first-document/save-and-use-configuration" />

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

Always implement robust error handling. Check that the file exists, set a timeout when waiting for the result, and read the current job status when the wait runs out:

<Example name="first-document/handle-timeout" />

## Advanced Processing Options

### Batch Processing Multiple Documents

To process many documents with the same settings, create a batch, upload the documents to it and process the batch once:

<Example name="quick-start/batch-process" />

See [Batch Processing](/docs/core/batch-processing) for scheduling, results and error handling.

### Using Cases for Organization

Cases group related documents. Create a case, then upload documents to it:

<Example name="cases/create-and-upload" />

List the documents in the case to see their processing status:

<Example name="cases/list-documents" />

See [Cases](/docs/advanced/cases) for processing, search and summaries over case documents.

## Performance Tips

### Optimizing Processing Speed

1. **Use an appropriate LLM level**:
   - `MINI`: Faster, good for simple extraction
   - `HIGH`: Slower, better for complex documents

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
   - [Templates](/docs/templates/) - Reusable document templates

2. **Learn Integration Patterns**:
   - [Use Cases](/docs/integration/use-cases) - Real-world implementation examples  
   - [Best Practices](/docs/integration/best-practices) - Production deployment guidelines
   - [Troubleshooting](/docs/integration/troubleshooting) - Debug common issues

3. **Reference Materials**:
   - [SDK Methods](/docs/reference/sdk-methods) - Complete method documentation
   - [Error Codes](/docs/reference/error-codes) - Error handling reference
   - [Configuration](/docs/configuration/) - Settings management

Ready to build more sophisticated document processing workflows? Continue with [Cases](/docs/advanced/cases) to learn about organizing and managing multiple documents!
