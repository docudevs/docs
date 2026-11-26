---
title: Map-Reduce Extraction
description: Break long or repetitive documents into manageable windows while preserving header context.
sidebar_position: 4
---

# Map-Reduce Extraction

Process long documents (50+ pages) by breaking them into manageable chunks.

Map-Reduce is a powerful processing mode designed for long, repetitive, or multi-section documents. Instead of trying to fit the entire document into a single AI context window, DocuDevs splits the document into overlapping windows ("chunks"), extracts data from each chunk, and then aggregates the results.

## How It Works

1. **Map**: The document is split into chunks (e.g., 5 pages each).
2. **Process**: Each chunk is processed independently to extract data.
3. **Reduce**: The results are combined into a single, clean JSON output.

You always receive a consistent JSON payload:

```json
{
  "header": { ... optional ... },
  "records": [ ... rows ... ]
}
```

## When to Use Map-Reduce

- **Long Documents**: Annual reports, 100+ page contracts, large catalogs.
- **Repetitive Data**: Bank statements, medical records, logs.
- **Header + Rows**: Documents with a summary section (header) and pages of detailed rows.

## Configuration Options

| Option | Description | Default |
| --- | --- | --- |
| `splitType` | Chunking mode: `PAGE`, `MARKDOWN_HEADER`, or `DOCUMENT_OUTLINE`. | `PAGE` |
| `splitHeaderLevel` | Header level used when `splitType=MARKDOWN_HEADER` (`1` for `#`, `2` for `##`). | `2` |
| `pagesPerChunk` | Number of pages per extraction window. | `1` |
| `overlapPages` | Number of pages to overlap between chunks to catch data spanning pages. | `0` |
| `dedupKey` | JSON path to a unique field (e.g., `sku`, `date`) used to remove duplicates caused by overlap. | `null` |
| `parallelProcessing` | When `true`, chunks are processed in parallel by multiple workers for faster throughput. | `false` |
| `header` | Configuration for extracting document-level metadata (header); see [Header Capture Strategy](#header-capture-strategy). In the Python SDK these settings are the `header_options`, `header_schema` and `header_prompt` arguments. | `null` |

When `splitType=MARKDOWN_HEADER`, chunking is based on OCR markdown sections instead of pages. In this mode, `overlapPages` and `dedupKey` do not apply.

When `splitType=DOCUMENT_OUTLINE`, chunking is based on inferred document structure instead of fixed page windows. In this mode, `dedupKey` and `splitHeaderLevel` do not apply; `pagesPerChunk` and `overlapPages` are still accepted, but act as a page-window fallback budget rather than a fixed chunk size — see [Document Outline Split Type](#document-outline-split-type) below.

## Example: Processing a Long Invoice

Imagine a 50-page invoice where the first page has the **Invoice Number** and **Date** (Header), and the remaining 49 pages contain **Line Items** (Rows).

The example submits the document with four-page chunks that overlap by one page, removes rows repeated by the overlap with `dedupKey`, reads the header from page 1 only, and adds a short context line to every chunk prompt. It waits for the job and prints the header and the number of extracted line items. The CLI has no option for parallel processing, so its variant omits it.

<Example name="extraction/process-long-invoice-map-reduce" />

## Example: Split by Markdown Headers

Use this when OCR text is markdown and your document is organized by headings.

<Example name="extraction/split-map-reduce-by-markdown-header" />

## Document Outline Split Type

Use `DOCUMENT_OUTLINE` for structured business documents — contracts, policies, reports, statements of work, manuals — where a logical section (or a table or figure) often spans multiple pages. Unlike `PAGE`, which slices by a fixed page window, `DOCUMENT_OUTLINE` infers the document's structure from OCR markdown and layout data first, then chunks along confident structural boundaries; it only falls back to page windows with overlap for regions that are oversized or structurally uncertain. The resulting outline is persisted as a reusable artifact and used for reduction, so overlap reconciliation is based on outline-node identity rather than a `dedupKey`.

<Example name="extraction/split-map-reduce-by-document-outline" />

Notes:

- `pagesPerChunk` is optional; when omitted, chunk sizing is determined automatically. When provided, it acts as a page budget for page-fallback windows and as a soft target for grouping, not a strict chunk size.
- `overlapPages` applies only to page-fallback windows and structural boundary context.
- `dedupKey` and `splitHeaderLevel` are not supported with this split type and are rejected.
- The outline artifact can also be (re)built after the fact as a standalone follow-up operation — see [Document Outline](/docs/advanced/operations#document-outline) — without re-running the whole extraction.
- `DOCUMENT_OUTLINE` is available through the REST API and the Python SDK. It is not exposed by the Java SDK's `MapReduceOptions.SplitType` enum or by the CLI's `--split-type` option.

## Header Capture Strategy

- **`header.pageLimit`** (Python: `page_limit` in `header_options`): How many pages at the start are considered "Header".
- **`header.schema`** (Python: `header_schema`): A specific JSON schema just for the header data.
- **`header.rowPromptAugmentation`** (Python: `row_prompt_augmentation`): Text injected into every subsequent chunk prompt. Useful for passing context (e.g., "This is Invoice #123") to help the AI understand isolated pages.

## Best Practices

1. **Always use a `dedupKey`**: When `overlapPages > 0`, the same row might appear in two chunks. The `dedupKey` tells DocuDevs how to identify and merge them.
2. **Start small**: Try `pagesPerChunk=4`. If the AI misses context, increase it. If it's too slow, decrease it.
3. **Monitor Progress**: Map-Reduce jobs take longer. Use the status endpoint to track progress.
4. **Use markdown-header split for sectioned docs**: If OCR output is markdown with clear headings, set `splitType=MARKDOWN_HEADER` and choose `splitHeaderLevel` (`1` for top-level sections, `2` for subsection chunks).

Read the progress of a running job from the `mapReduceStatus` object of the job status:

<Example name="extraction/check-map-reduce-progress" />

## Re-running Map-Reduce on an Existing Job

If you have already processed or OCR'd a document, you can run map-reduce on it again **without re-uploading the file or re-running OCR**. This is useful when you want to:

- Iterate on your prompt/schema without paying for OCR again.
- Run different chunking strategies on the same document.
- Extract different fields from a document that was already OCR'd.

Use `submit_and_wait_for_map_reduce` with the `parent_job_id` of the completed job in Python, `submitAndWaitForMapReduce` with the guid of the completed job in Java, or send the process request with `dependsOn` set to the completed job:

<Example name="extraction/reprocess-with-map-reduce" />

:::tip
Since OCR is the most expensive part of processing, re-running extraction with different prompts or schemas on an already-processed document is very cost-effective.
:::

## Troubleshooting

- **Missing Rows**: Check if rows are split across pages. Increase `overlapPages`.
- **Duplicate Rows**: Ensure your `dedupKey` is truly unique for each row.
- **Slow Processing**: Reduce `pagesPerChunk` to speed up individual chunk processing.
- **Wrong Markdown Sections**: For markdown-header chunking, switch between `splitHeaderLevel=1` and `splitHeaderLevel=2` depending on your document structure.
