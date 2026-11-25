---
title: Plain OCR Processing
description: Extract plain text or markdown from documents using OCR without structured data extraction.
sidebar_position: 3
---

# Plain OCR Processing

DocuDevs provides OCR-only processing for extracting plain text or markdown content from documents without structured data extraction. This feature is perfect when you only need the text content from documents like PDFs, images, or scanned documents.

## Overview

The Plain OCR feature allows you to:

- Extract text content in either plain text or markdown format
- Choose between standard and premium OCR quality
- Process documents without complex data structuring
- Get faster results for simple text extraction needs

## Quick Start

Install the SDK and CLI, and set your API key as an environment variable:

```bash
pip install docu-devs-api-client
export API_KEY="your_api_key"
```

Extract the text of a document as markdown:

<Example name="ocr/extract-text" />

The cURL example uses [`jq`](https://jqlang.org/) to read values from responses. The result of an OCR job is the text itself, not JSON. Image files such as `receipt.jpg` work the same way as PDFs; set the MIME type to match the file (for example `image/jpeg`). The CLI detects the MIME type from the file name.

## OCR Quality Options

### DEFAULT Quality

- Uses Azure Document Intelligence's standard OCR engine
- Fast processing
- Good for clear, well-formatted documents
- No additional AI processing

### PREMIUM Quality  

- Includes AI-powered error correction
- Better accuracy for handwritten or poor-quality documents
- Pagewise processing for optimal results
- Additional processing time for enhanced accuracy

### AUTO Quality (Recommended)

- Automatically analyzes document quality and selects appropriate processing level
- Starts with DEFAULT quality for speed
- Automatically escalates to PREMIUM if quality issues are detected
- Best balance of speed and accuracy
- Ideal for mixed document types or when document quality is unknown

## Output Formats

### Markdown Format

- Preserves document formatting
- Maintains headers, lists, and text structure
- Ideal for documents with rich formatting
- Better readability and structure retention

### Plain Text Format

- Raw text extraction
- No formatting preservation  
- Smaller output size
- Best for simple text analysis

## Choosing Quality and Format

Set the OCR quality (`DEFAULT`, `PREMIUM` or `AUTO`) and the output format (`markdown` or `plain`) per request. If you do not set a format, the SDK produces markdown and the CLI produces plain text, so pass `--format` explicitly in scripts:

<Example name="ocr/choose-quality-and-format" />

### Checking Results

The examples wait for the job to finish and print the text. To check a job yourself, use the job status and result endpoints, or `docudevs status <guid>` and `docudevs result <guid>` in the CLI. Add `--no-wait` to `docudevs ocr-only` to return as soon as the document is queued.

### CLI Options

- `--api-url` points the CLI at a different API endpoint. Put it before the command name.
- `--token` overrides the key from the environment for one command. Put it before the command name.
- `--timeout` sets how long the CLI waits for the result, in seconds.

## One-Step Synchronous OCR

For simpler usage, upload the document and receive the text in the same request. The response is a JSON string containing the OCR output:

<Example name="ocr/ocr-sync" />
