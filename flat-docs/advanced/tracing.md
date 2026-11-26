---
title: LLM Tracing
description: Enable LLM tracing to inspect prompts, token usage, and model responses for debugging and optimization.
sidebar_position: 3
---
<!-- Generated from docs/advanced/tracing.md. Do not edit by hand. -->

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

**cURL**

```bash
wait_for_job() {
  local deadline=$((SECONDS + 600))
  while [ "$SECONDS" -lt "$deadline" ]; do
    STATUS=$(curl -s -S -f "https://api.docudevs.ai/job/status/$1" \
      -H "Authorization: $API_KEY" | jq -r '.status')
    case "$STATUS" in
      COMPLETED) return 0 ;;
      PARTIAL) echo "Job $1 ended with status PARTIAL" >&2; return 0 ;;
      ERROR|FAILED|TIMEOUT) echo "Job $1 ended with status $STATUS" >&2; return 1 ;;
    esac
    sleep 5
  done
  echo "Job $1 did not finish within 600 seconds" >&2
  return 1
}

JOB_GUID=$(curl -s -S -f -X POST "https://api.docudevs.ai/document/upload" \
  -H "Authorization: $API_KEY" \
  -F "document=@invoice.pdf" | jq -r '.guid')

# Process with tracing enabled
curl -s -S -f -o /dev/null -X POST "https://api.docudevs.ai/document/process/$JOB_GUID" \
  -H "Authorization: $API_KEY" \
  -H "Content-Type: application/json" \
  -d '{
    "prompt": "Extract invoice data",
    "mimeType": "application/pdf",
    "trace": true
  }'

wait_for_job "$JOB_GUID"

# Retrieve the trace after the job completes
TRACE=$(curl -s -S -f "https://api.docudevs.ai/job/trace/$JOB_GUID" \
  -H "Authorization: $API_KEY")
echo "$TRACE" | jq -r '"Total tokens used: \(.total_tokens)", "LLM calls: \(.total_llm_calls)"'
```

**Python**

```python
import asyncio
import os

from docudevs.docudevs_client import DocuDevsClient


async def main():
    client = DocuDevsClient(token=os.environ["API_KEY"])

    with open("invoice.pdf", "rb") as f:
        document_data = f.read()

    # Enable tracing when processing a document
    job_guid = await client.submit_and_process_document(
        document=document_data,
        document_mime_type="application/pdf",
        prompt="Extract invoice data",
        trace=True,
    )

    # Wait for processing to complete
    await client.wait_until_ready(job_guid)

    # Retrieve the trace
    trace = await client.get_trace(job_guid)
    if trace:
        print(f"Total tokens used: {trace['total_tokens']}")
        print(f"LLM calls: {trace['total_llm_calls']}")


asyncio.run(main())
```

**Java**

```java
import ai.docudevs.client.DocuDevsClient;
import ai.docudevs.client.ProcessOptions;
import ai.docudevs.client.UploadRequest;
import ai.docudevs.client.WaitOptions;
import com.fasterxml.jackson.databind.JsonNode;
import java.nio.file.Files;
import java.nio.file.Path;

public class Example {
    public static void main(String[] args) throws Exception {
        DocuDevsClient client = DocuDevsClient.builder()
                .apiKey(System.getenv("API_KEY"))
                .baseUrl("https://api.docudevs.ai")
                .build();

        byte[] fileBytes = Files.readAllBytes(Path.of("invoice.pdf"));
        UploadRequest upload = new UploadRequest("invoice.pdf", "application/pdf", fileBytes);

        String guid = client.submitAndProcessDocument(
                upload,
                ProcessOptions.builder()
                        .mimeType("application/pdf")
                        .prompt("Extract invoice data")
                        .trace(true)
                        .build());

        client.waitUntilReadyJson(guid, WaitOptions.builder().build());

        JsonNode trace = client.getTrace(guid);
        System.out.println("Total tokens used: " + trace.path("total_tokens").asInt());
        System.out.println("LLM calls: " + trace.path("total_llm_calls").asInt());
    }
}
```

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

**cURL**

```bash
JOB_GUID="<job-guid>"  # GUID of a completed job

# Get the thumbnail of page 0
curl -s -S -f "https://api.docudevs.ai/job/image/$JOB_GUID/0" \
  -H "Authorization: $API_KEY" \
  --output page_0.png
```

**Python**

```python
job_guid = "<job-guid>"  # GUID of a completed job

# Get the thumbnail of page 0
image_bytes = await client.get_image(job_guid, page_index=0)
if image_bytes:
    with open("page_0.png", "wb") as f:
        f.write(image_bytes)
```

**Java**

```java
String jobGuid = "<job-guid>"; // GUID of a completed job

// Get the thumbnail of page 0
byte[] imageBytes = client.downloadImage(jobGuid, 0);
Files.write(Path.of("page_0.png"), imageBytes);
```

The Java SDK has no method for page images, so there is no Java example for this call.

## Map-Reduce Tracing

For large documents processed with map-reduce, tracing captures all chunk processing:

**cURL**

```bash
wait_for_job() {
  local deadline=$((SECONDS + 600))
  while [ "$SECONDS" -lt "$deadline" ]; do
    STATUS=$(curl -s -S -f "https://api.docudevs.ai/job/status/$1" \
      -H "Authorization: $API_KEY" | jq -r '.status')
    case "$STATUS" in
      COMPLETED) return 0 ;;
      PARTIAL) echo "Job $1 ended with status PARTIAL" >&2; return 0 ;;
      ERROR|FAILED|TIMEOUT) echo "Job $1 ended with status $STATUS" >&2; return 1 ;;
    esac
    sleep 5
  done
  echo "Job $1 did not finish within 600 seconds" >&2
  return 1
}

JOB_GUID=$(curl -s -S -f -X POST "https://api.docudevs.ai/document/upload" \
  -H "Authorization: $API_KEY" \
  -F "document=@large-report.pdf" | jq -r '.guid')

# Tracing covers every chunk
curl -s -S -f -o /dev/null -X POST "https://api.docudevs.ai/document/process/$JOB_GUID" \
  -H "Authorization: $API_KEY" \
  -H "Content-Type: application/json" \
  -d '{
    "prompt": "Extract line items",
    "mimeType": "application/pdf",
    "trace": true,
    "mapReduce": {
      "enabled": true,
      "splitType": "PAGE",
      "pagesPerChunk": 5
    }
  }'

wait_for_job "$JOB_GUID"

curl -s -S -f "https://api.docudevs.ai/job/trace/$JOB_GUID" \
  -H "Authorization: $API_KEY" | jq -r '"Processed in \(.events | length) events"'
```

**Python**

```python
with open("large-report.pdf", "rb") as f:
    document_data = f.read()

job_guid = await client.submit_and_process_document_map_reduce(
    document=document_data,
    document_mime_type="application/pdf",
    prompt="Extract line items",
    pages_per_chunk=5,
    trace=True,  # Traces every chunk
)

await client.wait_until_ready(job_guid)

trace = await client.get_trace(job_guid)
if trace:
    print(f"Processed in {len(trace.get('events', []))} events")
```

**Java**

```java
import ai.docudevs.client.MapReduceOptions;
import ai.docudevs.client.ProcessOptions;
import ai.docudevs.client.UploadRequest;
import ai.docudevs.client.WaitOptions;
import com.fasterxml.jackson.databind.JsonNode;

byte[] documentData = Files.readAllBytes(Path.of("large-report.pdf"));

// Tracing covers every chunk
String jobGuid = client.submitAndProcessDocumentMapReduce(
        new UploadRequest("large-report.pdf", "application/pdf", documentData),
        ProcessOptions.builder()
                .prompt("Extract line items")
                .trace(true)
                .build(),
        MapReduceOptions.builder()
                .pagesPerChunk(5)
                .build());

client.waitUntilReadyJson(
        jobGuid,
        WaitOptions.builder()
                .timeout(Duration.ofSeconds(600))
                .pollInterval(Duration.ofSeconds(5))
                .build());

JsonNode trace = client.getTrace(jobGuid);
System.out.println("Processed in " + trace.path("events").size() + " events");
```

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
