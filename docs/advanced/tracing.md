---
title: LLM Tracing
description: Enable LLM tracing to inspect prompts, token usage, and model responses for debugging and optimization.
sidebar_position: 3
---

# LLM Tracing

Debug and understand your document extraction by capturing detailed traces of LLM interactions.

## Overview

LLM Tracing provides visibility into how your documents are processed, similar to tools like LangSmith but integrated directly into DocuDevs. When enabled, traces capture:

- **LLM Calls**: Prompts sent to the model and responses received
- **Tool Calls**: Any tool invocations (e.g., knowledge base searches)
- **Token Usage**: Prompt, completion, and total token counts
- **Timing**: Duration of each operation
- **Page Images**: References to document page thumbnails shown to the LLM

## Enabling Tracing

Tracing is opt-in and must be enabled per request by setting `trace` to `true`.

<Example name="tracing/enable-tracing" />

You can also enable tracing in the Workbench:

1. Navigate to the **Processing** panel
2. Check the **"Enable LLM tracing"** checkbox
3. Process your document
4. After completion, click **"Trace"** on the job detail page

## Trace Data Structure

A trace contains high-level summary information and a list of events:

```json
{
  "id": "trace-uuid",
  "name": "Document Extraction",
  "start_time": "2024-01-15T10:30:00Z",
  "end_time": "2024-01-15T10:30:15Z",
  "status": "completed",
  "job_id": "job-guid",
  "extraction_mode": "SIMPLE",
  "llm_level": "DEFAULT",
  "total_tokens": 4521,
  "total_llm_calls": 2,
  "total_tool_calls": 0,
  "events": [...]
}
```

### Event Types

| Type | Description |
|------|-------------|
| `llm_start` | LLM call initiated with prompt/messages |
| `llm_end` | LLM call completed with response and token usage |
| `tool_start` | Tool invocation started |
| `tool_end` | Tool invocation completed |
| `chain_start` | Processing chain started |
| `chain_end` | Processing chain completed |
| `error` | Error occurred during processing |

### LLM Event Details

LLM events include detailed information:

```json
{
  "id": "event-uuid",
  "type": "llm_end",
  "name": "ChatOpenAI",
  "timestamp": "2024-01-15T10:30:05Z",
  "duration_ms": 2340,
  "model": "gpt-4o",
  "messages": [
    {
      "role": "system",
      "content": "You are a document extraction assistant..."
    },
    {
      "role": "user",
      "content": [
        {"type": "text", "text": "Extract the following..."},
        {"type": "image", "image_ref": {"page_index": 0}}
      ]
    }
  ],
  "response": "Based on the document...",
  "prompt_tokens": 1234,
  "completion_tokens": 567,
  "total_tokens": 1801
}
```

## Retrieving Page Images

Messages may contain `image_ref` objects referencing document pages. You can retrieve these images:

<Example name="tracing/get-page-image" />

The Java SDK has no method for page images, so there is no Java example for this call.

## Map-Reduce Tracing

For large documents processed with map-reduce, tracing captures all chunk processing:

<Example name="tracing/trace-map-reduce" />

## Best Practices

### When to Use Tracing

- **Development**: Always enable during prompt development
- **Debugging**: When extraction results are unexpected
- **Optimization**: To analyze token usage and identify improvements
- **Auditing**: When you need a record of LLM interactions

### When NOT to Use Tracing

- **Production at Scale**: Tracing adds storage overhead
- **Sensitive Data**: Traces contain full prompts and responses
- **High-Volume Batch**: Consider sampling instead

### Performance Considerations

- Traces are stored in blob storage alongside job results
- Enabling tracing has minimal impact on processing time
- Storage size depends on document complexity and LLM verbosity

## API Reference

### Get Trace

```http
GET /job/trace/{guid}
```

Returns the trace data for a job, or 404 if tracing was not enabled.

### Get Page Image

```http
GET /job/image/{guid}/{pageIndex}
```

Returns a PNG thumbnail of the specified page (0-indexed).

## SDK Methods

### get_trace

```python
async def get_trace(self, guid: str) -> dict | None:
    """Get the LLM trace for a job.

    Returns:
        The trace data as a dict, or None if no trace is available.
    """
```

### get_image

```python
async def get_image(self, guid: str, page_index: int) -> bytes | None:
    """Get a page thumbnail image for a job.

    Returns:
        The image bytes (PNG), or None if not found.
    """
```
