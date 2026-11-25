---
title: Page Range Processing
description: Process specific pages from multi-page documents for targeted extraction.
sidebar_position: 5
---

# Page Range Processing

Process specific pages from a document instead of the entire document.

The `pageRange` parameter allows you to target specific pages for extraction, which is useful for:

- **Multi-section documents**: Extract data from specific sections (e.g., pages 1-3 for identification, pages 8-10 for physical properties)
- **Large documents**: Focus on relevant pages without processing the entire document
- **Multi-pass processing**: Extract different data from different page ranges of the same document

## How It Works

1. **Upload** your document normally
2. **Specify `pageRange`** with 1-indexed page numbers
3. **Only specified pages** are sent to the AI for extraction

:::info Page Numbering
Pages are **1-indexed**, meaning the first page is page 1 (not 0).
:::

## Basic Usage

<Example name="extraction/process-page-range" />

## Processing Multiple Specific Pages

You can specify any combination of pages:

Set `pageRange` to a list of 1-indexed page numbers. Use `[1, 3, 5]` for pages 1, 3 and 5, or `[8, 9, 10]` for pages 8 to 10. The [Safety Data Sheets](#use-case-safety-data-sheets-sds) example below runs several ranges against one document.

## Combined with Map-Reduce

Page range works with map-reduce processing. The specified pages are first filtered, then chunked and processed:

<Example name="extraction/process-page-range-map-reduce" />

:::note Processing Order
With `pageRange` set to `[1, 2, 5, 6, 10, 11]` and `pagesPerChunk` set to `2`:

- **Chunk 1**: Pages 1-2 (original document pages 1-2)
- **Chunk 2**: Pages 5-6 (original document pages 5-6)  
- **Chunk 3**: Pages 10-11 (original document pages 10-11)

Each page is processed exactly once.
:::

## Use Case: Safety Data Sheets (SDS)

Safety Data Sheets have 16 standardized sections spread across many pages. You can extract specific sections using page ranges:

<Example name="extraction/extract-sections-by-page-range" />

## Limitations

### OCR Compatibility

Page range requires page break markers from OCR, which are only available with `DEFAULT` or `PREMIUM` OCR modes.

:::warning LOW OCR Not Supported
`pageRange` cannot be used with `ocr=LOW` because the LOW OCR mode (MarkItDown) does not produce page break markers.
:::

The API rejects a request that sets `pageRange` together with `ocr` set to `LOW`, and the Python SDK raises a `ValueError` before sending it.

### Out-of-Range Pages

Pages that don't exist in the document are silently ignored. For a 10-page document, `pageRange` set to `[8, 9, 10, 11, 12, 13, 14, 15]` processes pages 8 to 10 only.

## API Reference

| Parameter | Type | Description |
|-----------|------|-------------|
| `pageRange` | `list[int]` | List of 1-indexed page numbers to process. If not provided, all pages are processed. |

The parameter is available in:

- `submit_and_process_document()`
- `submit_and_process_document_map_reduce()`
- `process_document()` (via UploadCommand)
- `process_document_map_reduce()`
- Direct API calls to `/document/process/{guid}`
