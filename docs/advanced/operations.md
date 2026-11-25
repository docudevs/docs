---
title: Operations
description: Post-processing operations on completed jobs including error analysis, generative tasks, image selection, and AcroForm PDF generation.
sidebar_position: 2
---

# Operations

Perform post-processing operations on completed document jobs for error analysis, generative tasks, image selection, and AcroForm PDF generation.

## Overview

Operations allow you to run additional analysis and processing on documents that have already been processed. Available operations include:

- **Error Analysis**: Identify potential issues in extraction results.
- **Generative Tasks**: Generate summaries, translations, or custom AI responses based on processed documents.
- **Image Selection**: Pick relevant figures from extracted images based on a prompt.
- **PDF AcroForm Conversion**: Turn a normal PDF into a generated fillable AcroForm PDF, inspect generated field metadata, and reapply reviewed field definitions.
- **Document Outline**: (Re)build a structural outline artifact for a completed job without rerunning the extraction.

## Available Operations

### Error Analysis

Analyzes completed document processing jobs to identify:

- Potential extraction errors or inconsistencies.
- Missing or incomplete data fields.
- Confidence levels for extracted information.
- Suggestions for improving accuracy.

### Generative Tasks

Generates custom AI responses based on processed document content:

- Document summaries and explanations.
- Content transformation (e.g., format conversion).
- Question answering based on document content.
- Custom analysis with user-defined prompts.

### Image Selection

Selects the most relevant extracted figures based on a prompt. The parent job must have figure artifacts, which are created by running document processing or OCR with `extractFigures` (or `describeFigures`) enabled.

### PDF AcroForm Conversion

Converts a completed PDF job into a generated fillable AcroForm PDF. This is useful when the source document has visible entry areas but no native AcroForm fields.

The conversion workflow has two companion artifacts:

- `GET /job/result/{operationJobGuid}/acroform-metadata` returns the generated PDF's embedded widget metadata in PDF page-point coordinates.
- `GET /job/result/{operationJobGuid}/pdf-acroform-field-definitions` returns normalized editable field definitions with `entryBbox` values in `0..1` page-image coordinates.

Use the field-definition artifact for review UIs or manual edits, then send the reviewed list back through `pdf-acroform/apply` to regenerate the PDF without rerunning visual detection.

### Document Outline

Rebuilds the structural-outline artifact for an already-completed job as a follow-up operation, instead of re-running the whole extraction. This is the same outline-building logic used by the `DOCUMENT_OUTLINE` map-reduce split type — see [Map-Reduce Extraction](/docs/core/map-reduce-extraction#document-outline-split-type) — exposed here as a standalone operation so you can get or regenerate just the outline.

## How Operations Work

1. **Document Processing**: First, process a document normally.
2. **Operation Submission**: Submit an operation request on the completed job.
3. **Analysis**: AI analyzes the original processing results.
4. **Results**: Get detailed analysis, generated content, or selected images.

## API Endpoints

### Submit Operation

```http
POST /operation
```

**Request Body:**

```json
{
  "jobGuid": "uuid-of-completed-job",
  "type": "error-analysis",
  "parameters": {
    "llmType": "HIGH",
    "customParameters": {}
  }
}
```

### Submit Generative Task

```http
POST /operation/{parentJobId}/generative-task
```

**Request Body:**

```json
{
  "prompt": "Summarize this document and explain its main purpose",
  "model": "DEFAULT",
  "temperature": 0.7,
  "maxTokens": 500
}
```

## Examples

The examples below start from a completed job: replace `<job-guid>` with the GUID of a job you processed earlier. The cURL tabs poll the operation job until it completes; the Python SDK helpers and the CLI wait for you.

### Error Analysis

<Example name="operations/error-analysis" />

### Generative Tasks

<Example name="operations/generative-task" />

### Image Selection

The parent job must have figure artifacts, so process the document with figure extraction (`extractFigures` or `describeFigures`) first.

<Example name="operations/select-images" />

### PDF AcroForm Conversion

Run `pdf-acroform` on a completed PDF job to generate a fillable AcroForm PDF, then inspect or edit the generated field definitions.

For the full parameter list and the REST download endpoint, see [PDF AcroForm](./pdf-acroform.md).

The parent job must point to a PDF. The operation also needs rendered page images, so the safest setup is to create the parent job with `ocr="PREMIUM"`, or set `forceOcr` on the operation when thumbnails are missing.

<Example name="operations/convert-pdf-to-acroform" />

The CLI prints the `operationJobGuid`. To fetch `acroform-metadata` and `pdf-acroform-field-definitions`, or to submit reviewed field definitions back through `pdf-acroform/apply`, use the Python SDK or the HTTP API:

<Example name="operations/review-acroform-fields" />

The generated PDF from either the initial conversion or the reviewed apply flow is always downloaded through `GET /job/result/{guid}/pdf-acroform`.

### Document Outline

<Example name="operations/document-outline" />

If the job already has an outline artifact (for example, a job originally processed with `splitType=DOCUMENT_OUTLINE`), fetch it directly without submitting a new operation:

<Example name="operations/get-document-outline" />

In Python, `wait_until_ready_with_document_outline` waits for a still-running job and returns its result and outline in one call.

`pagesPerChunk` must be at least 1, `overlapPages` must be at least 0, and `overlapPages` must be less than `pagesPerChunk`. There is no CLI command and no Java SDK method for this operation today: use the Python SDK or the raw HTTP API directly.

## Advanced Usage

### Document Question Answering

You can use Generative Tasks to ask specific questions about a processed document.

<Example name="operations/ask-document-questions" />

### Advanced Parameters

Set `llmType` to choose the model tier for an operation. Operation-specific settings go in `customParameters`; the parameters each operation accepts are described with that operation above.

<Example name="operations/submit-with-llm-type" />

## Output Formats

### Error Analysis Output

```json
{
  "analysis": {
    "overall_confidence": 0.85,
    "field_confidence": {
      "invoice_number": 0.95,
      "total_amount": 0.90
    },
    "potential_issues": [
      {
        "field": "line_items",
        "issue": "Some quantities may be misread",
        "confidence": 0.75,
        "suggestion": "Review items with quantities > 100"
      }
    ]
  },
  "recommendations": [
    {
      "type": "processing_improvement",
      "description": "Consider using higher quality OCR settings"
    }
  ]
}
```

### Generative Task Output

```json
{
  "generated_text": "This document is a household contents insurance policy...",
  "model_used": "DEFAULT",
  "prompt": "Summarize this document...",
  "processing_time_ms": 3245,
  "token_usage": {
    "prompt_tokens": 2847,
    "completion_tokens": 94,
    "total_tokens": 2941
  }
}
```

## Best Practices

- **Quality Assurance**: Run error analysis on critical documents or when confidence scores are low.
- **Summarization**: Use generative tasks to create quick summaries for document lists.
- **Translation**: Use generative tasks to translate document content while preserving context.
- **Chain Operations**: Process a document, then run error analysis, then summarize it—all via API.
- **Map-Reduce Re-processing**: Use `submit_and_wait_for_map_reduce(parent_job_id=...)` to run map-reduce on a previously processed document without re-uploading or re-running OCR. See [Map-Reduce Extraction](/docs/core/map-reduce-extraction#re-running-map-reduce-on-an-existing-job) for details.
