---
title: Limits & Quotas
sidebar_position: 7
---

# Platform Limits and Quotas

Reference for all limits, quotas, and constraints in the DocuDevs platform.

## Document Limits

### File Size Limits

| Limit Type | Maximum Size | Notes |
| :--- | :--- | :--- |
| **Upload Size** | **50 MB** | Maximum file size for a single document upload. |
| **Request Body** | **15 MB** | Maximum size for JSON payloads (e.g. base64 encoded files). |

**Error Response for Exceeded Limits:**

```json
{
  "message": "File size exceeds maximum limit of 50MB",
  "status": 413
}
```

### Page Limits

| Document Type | Maximum Pages | Notes |
| :--- | :--- | :--- |
| **PDF Documents** | **2,000 pages** | For standard processing. |
| **TIFF Documents** | **2,000 pages** | Multi-page TIFFs. |

### Supported File Formats

**Supported file types:**

- PDF documents (`.pdf`)
- Word documents (`.docx`)
- Plain text files (`.txt`)
- Images (`.jpg`, `.jpeg`, `.png`, `.tiff`, `.bmp`, `.heif`)
- Excel Spreadsheets (`.xlsx`, `.xls`) - *Requires `ocr="EXCEL"` or `ocr_format="jsonl"`*

**Unsupported formats return:**

```json
{
  "message": "Unsupported file format",
  "status": 415
}
```

### Image Requirements

For optimal OCR and AI processing:

| Constraint | Value |
| :--- | :--- |
| **Minimum Image Size** | 50 x 50 pixels |
| **Maximum Image Size** | 10,000 x 10,000 pixels |
| **Minimum Text Height** | 12 pixels |

## Processing Constraints

### AI Vision Limits

When using AI models with vision capabilities (e.g. for image-heavy PDFs):

- **Total Image Payload**: Max **20 MB** of image data sent to the AI model per request.
- **Behavior**: If a document exceeds this, the system may downsample images or process fewer pages per chunk to fit within the limit.

### Timeouts

- **Standard Job Timeout**: **300 seconds** (5 minutes).
- **Map-Reduce Job Timeout**: Configurable, typically higher for large batches.

## Case Limits

### Documents per Case

| Limit | Value |
| :--- | :--- |
| **Maximum Documents** | **200** | Per single case. |

**Error when limit exceeded:**

```json
{
  "message": "Case document limit reached: 200",
  "status": 409
}
```

## Rate Limits

API usage is subject to rate limiting to ensure fair usage.

| Tier | Requests per Minute | Burst |
| :--- | :--- | :--- |
| **Standard** | 60 | 100 |
| **Enterprise** | Custom | Custom |

**Headers:**

- `X-RateLimit-Limit`
- `X-RateLimit-Remaining`
- `X-RateLimit-Reset`

## Best Practices

### File Size Optimization

For best performance:

- Keep files under 10 MB when possible.
- Compress large PDFs before uploading.
- Use appropriate image formats (JPEG for photos, PNG for documents).

### Handling Large Documents

For documents exceeding page limits or requiring complex extraction:

- Use **Map-Reduce** processing (`submit_and_process_document_map_reduce`) to process in chunks.
- Split very large PDFs into smaller files before uploading.

### Case Management

- Monitor document count as you approach the 200 document limit.
- Create new cases (e.g. "Invoices Q1", "Invoices Q2") rather than one giant case.
- Use descriptive case names to organize your documents effectively.
