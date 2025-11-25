# Plain OCR Processing

DocuDevs provides OCR-only processing for extracting plain text or markdown content from documents without structured data extraction. This feature is perfect when you only need the text content from documents like PDFs, images, or scanned documents.

## Overview

The Plain OCR feature allows you to:

- Extract text content in either plain text or markdown format
- Choose between standard and premium OCR quality
- Process documents without complex data structuring
- Get faster results for simple text extraction needs

## Quick Start with CLI

The simplest way to extract text from a document is using the DocuDevs CLI:

```bash
# Extract text as markdown (default)
docudevs ocr-only document.pdf

# Extract as plain text
docudevs ocr-only document.pdf --format plain

# Use premium OCR for better accuracy
docudevs ocr-only document.pdf --ocr premium

# Use auto quality selection (recommended)
docudevs ocr-only document.pdf --ocr auto
```

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

### Markdown Format (Default)

- Preserves document formatting
- Maintains headers, lists, and text structure
- Ideal for documents with rich formatting
- Better readability and structure retention

### Plain Text Format

- Raw text extraction
- No formatting preservation  
- Smaller output size
- Best for simple text analysis

## CLI Usage

### Installation

First, install the DocuDevs CLI:

```bash
pip install docu-devs-api-client
```

Set your API key as an environment variable:

```bash
export DOCUDEVS_API_KEY="your_api_key"
```

### Basic OCR Commands

```bash
# Extract text from a PDF (markdown format by default)
docudevs ocr-only document.pdf

# Extract as plain text
docudevs ocr-only document.pdf --format plain

# Use premium OCR for handwritten documents
docudevs ocr-only handwritten.pdf --ocr premium

# Process an image file
docudevs ocr-only receipt.jpg

# Use auto quality (recommended for mixed quality documents)
docudevs ocr-only scanned-doc.pdf --ocr auto
```

### Advanced CLI Usage

```bash
# Process and save output to a file
docudevs ocr-only document.pdf > extracted_text.md

# Process multiple files
for file in *.pdf; do
    docudevs ocr-only "$file" > "${file%.pdf}.txt" --format plain
done

# Use with specific API endpoint
docudevs --api-url https://api.docudevs.ai ocr-only document.pdf

# Override API key for this command
docudevs --token different_api_key ocr-only document.pdf
```

### Checking Results

```bash
# If you need to check status manually (usually automatic)
docudevs status {guid}

# Get results manually
docudevs result {guid}
```

## API Usage

### Basic OCR Processing

1. **Upload your document:**

```bash
curl -X POST "https://api.docudevs.ai/document/upload" \
  -H "Authorization: Bearer YOUR_API_KEY" \
  -F "document=@document.pdf"
```

2. **Process with OCR:**

```bash
curl -X POST "https://api.docudevs.ai/document/ocr/{guid}?format=markdown" \
  -H "Authorization: Bearer YOUR_API_KEY" \
  -H "Content-Type: application/json" \
  -d '{
    "ocr": "DEFAULT"
  }'
```

3. **Check status and get results:**

```bash
# Check processing status
curl -X GET "https://api.docudevs.ai/job/status/{guid}" \
  -H "Authorization: Bearer YOUR_API_KEY"

# Get the results when ready
curl -X GET "https://api.docudevs.ai/job/result/{guid}" \
  -H "Authorization: Bearer YOUR_API_KEY"
```

### One-Step Synchronous OCR

For simpler usage, use the synchronous endpoint:

```bash
curl -X POST "https://api.docudevs.ai/document/ocr/sync?format=markdown&ocr=auto" \
  -H "Authorization: Bearer YOUR_API_KEY" \
  -F "document=@document.pdf"
```

## Python SDK Usage

### Basic OCR Processing

```python
from docudevs.docudevs_client import DocuDevsClient
from io import BytesIO

# Initialize client
client = DocuDevsClient(token="your_api_key")

# Upload and process document
with open("document.pdf", "rb") as f:
    document = BytesIO(f.read())

# Submit for OCR processing
guid = await client.submit_and_ocr_document(
    document=document,
    document_mime_type="application/pdf",
    ocr="DEFAULT",
    ocr_format="markdown"
)

# Wait for results
result = await client.wait_until_ready(guid)

# OCR returns text content
print(result.result)
```
