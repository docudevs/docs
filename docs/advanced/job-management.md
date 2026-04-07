---
title: Job Management
description: Track processing job status, list jobs, retrieve results, and manage job lifecycle with polling and webhooks.
sidebar_position: 4
---

import Tabs from '@theme/Tabs';
import TabItem from '@theme/TabItem';

# Job Management

Manage the lifecycle of document processing jobs, including cleanup and deletion of completed jobs.

## Overview

Every document processing request in DocuDevs creates a **job**. Jobs track the status, store the uploaded documents, OCR results, and extracted data. Over time, you may want to clean up old jobs to:

- **Free up storage** by removing old documents and results
- **Comply with data retention policies**
- **Maintain a clean workspace** in the UI

## Job Lifecycle

Jobs progress through several states:

| Status | Description |
|--------|-------------|
| `PENDING` | Job created, waiting to be processed |
| `PROCESSING` | Document is being processed |
| `COMPLETED` | Processing finished successfully |
| `ERROR` | Processing failed with an error |
| `TIMEOUT` | Processing timed out |
| `PARTIAL` | Processing partially completed |

Only jobs in **terminal states** (COMPLETED, ERROR, TIMEOUT, PARTIAL) can be deleted.

## Deleting Jobs

Delete a job when you no longer need its data. Jobs older than 14 days are automatically purged, so this API is primarily for cleaning up recent jobs before the scheduled cleanup.

<Tabs
  defaultValue="python"
  values={[
    {label: 'Python SDK', value: 'python'},
    {label: 'Java SDK', value: 'java'},
    {label: 'cURL', value: 'curl'},
  ]}>
  <TabItem value="python">

```python
from docudevs.docudevs_client import DocuDevsClient
import os

client = DocuDevsClient(token=os.getenv('API_KEY'))

# Delete a completed job
result = await client.delete_job("job-guid-here")
if result.status_code == 200:
    print(f"Deleted {result.parsed['jobsDeleted']} job(s)")
```

  </TabItem>
  <TabItem value="java">

```java
import ai.docudevs.client.DocuDevsClient;
import com.fasterxml.jackson.databind.JsonNode;

DocuDevsClient client = DocuDevsClient.builder()
    .apiKey(System.getenv("API_KEY"))
    .build();

JsonNode purgeResult = client.deleteJob("job-guid-here");

System.out.println("Deleted jobs: " + purgeResult.path("jobsDeleted").asInt());
System.out.println("Errors: " + purgeResult.path("errors").asInt());
```

  </TabItem>

  <TabItem value="curl">

```bash
# Delete a job
curl -X DELETE https://api.docudevs.ai/job/{guid} \
  -H "Authorization: Bearer $API_KEY"
```

  </TabItem>
</Tabs>

### What Gets Deleted

When you delete a job, the following are removed:

- **Uploaded document** (the original file)
- **OCR results** (extracted text, markdown, JSONL)
- **Page thumbnails** (PNG images)
- **Extraction results** (JSON/CSV output)
- **Trace data** (if tracing was enabled)
- **Database record** (job metadata)

### What Is Preserved

To support billing and usage analytics, **usage records are preserved** but disassociated from the deleted job. This means:

- Your usage history remains accurate
- Billing calculations are not affected
- The job GUID is stored in usage records for reference

## Automatic Purge

DocuDevs automatically cleans up old jobs on a scheduled basis:

- **Runs daily** at 3:00 AM UTC
- **Deletes all jobs older than 14 days** in terminal states
- **Excludes case documents** (documents attached to cases are not purged)

No action is required on your part—old jobs are automatically removed to manage storage efficiently.

## Reusing Previous Results (`dependsOn`)

DocuDevs can chain jobs together so you **parse a document once** and then run multiple operations on the same result without re-processing. This saves time and cost when you need to extract different schemas or run different prompts against the same document.

Use the `dependsOn` query parameter when processing a document to reference a previous job:

<Tabs
  defaultValue="python"
  values={[
    {label: 'Python SDK', value: 'python'},
    {label: 'cURL', value: 'curl'},
  ]}>
  <TabItem value="python">

```python
from docudevs.docudevs_client import DocuDevsClient
import os

client = DocuDevsClient(token=os.getenv("API_KEY"))

# First extraction — parse and extract with schema A
guid_a = await client.submit_and_process_document(
    document=open("invoice.pdf", "rb"),
    document_mime_type="application/pdf",
    schema=schema_a,
    prompt="Extract invoice header fields",
)
result_a = await client.wait_until_ready(guid_a, result_format="json")

# Second extraction — reuse the parsed document with schema B (no re-OCR)
process_resp = await client.process_document(
    guid=guid_a,
    body={"schema": schema_b, "prompt": "Extract line items"},
    depends_on=guid_a,
)
result_b = await client.wait_until_ready(guid_a, result_format="json")
```

  </TabItem>
  <TabItem value="curl">

```bash
# First extraction
GUID=$(curl -s -X POST https://api.docudevs.ai/document/upload \
  -H "Authorization: Bearer $API_KEY" \
  -F "document=@invoice.pdf" | jq -r '.guid')

curl -X POST "https://api.docudevs.ai/document/process/$GUID" \
  -H "Authorization: Bearer $API_KEY" \
  -H "Content-Type: application/json" \
  -d '{"schema": "...", "prompt": "Extract header fields"}'

# Second extraction — reuse parsed document
curl -X POST "https://api.docudevs.ai/document/process/$GUID?dependsOn=$GUID" \
  -H "Authorization: Bearer $API_KEY" \
  -H "Content-Type: application/json" \
  -d '{"schema": "...", "prompt": "Extract line items"}'
```

  </TabItem>
</Tabs>

The dependent job waits for the parent to complete, then reuses its OCR/parsed content. This is especially useful for:

- Extracting **multiple schemas** from the same document
- Running **different prompts** against the same content
- Performing **operations** (generative tasks) on already-processed jobs via `/operation/{jobGuid}/generative-task`

## Quality Score

Every completed job includes a **quality score** (0.0–1.0) and a **quality category** that indicate how confident the OCR engine was about the extracted text. Use this to build automated quality gates.

| Category | Score Range | Description |
|----------|-----------|-------------|
| `Very Confident` | 0.85–1.0 | High quality, reliable extraction |
| `Confident` | 0.70–0.85 | Good quality, minor issues possible |
| `Likely Handwriting Problems` | 0.40–0.70 | May need review or PREMIUM OCR |
| `Many Problems` | 0.0–0.40 | Consider re-processing with PREMIUM OCR |

<Tabs
  defaultValue="python"
  values={[
    {label: 'Python SDK', value: 'python'},
    {label: 'cURL', value: 'curl'},
  ]}>
  <TabItem value="python">

```python
# Check quality after processing
status_resp = await client.status(guid=guid)
job = status_resp.parsed
print(f"Quality: {job.quality_score:.2f} ({job.quality_category})")

# Auto-retry with PREMIUM OCR if quality is low
result = await client.submit_and_process_with_quality_gate(
    document=open("scan.pdf", "rb"),
    document_mime_type="application/pdf",
    schema=my_schema,
    prompt="Extract fields",
    min_quality=0.7,  # auto-retries with PREMIUM if below
)
```

  </TabItem>
  <TabItem value="curl">

```bash
# Check quality after processing
curl -s "https://api.docudevs.ai/job/status/$GUID" \
  -H "Authorization: Bearer $API_KEY" | jq '{qualityScore, qualityCategory}'
```

  </TabItem>
</Tabs>

:::info Case Documents
Documents uploaded to [Cases](/docs/advanced/cases) are **not automatically purged**. Case documents persist until the case is deleted or documents are manually removed from the case.
:::

## Example: Cleanup Workflow

A complete workflow for processing a document and cleaning up afterward:

```python
from docudevs.docudevs_client import DocuDevsClient
import os

async def process_and_cleanup(file_path: str, keep_result: bool = True):
    client = DocuDevsClient(token=os.getenv('API_KEY'))
    
    # Process the document
    with open(file_path, "rb") as f:
        job_guid = await client.submit_and_process_document(
            document=f.read(),
            document_mime_type="application/pdf",
            prompt="Extract invoice data"
        )
    
    # Wait for completion and get result
    result = await client.wait_until_ready(job_guid, result_format="json")
    
    # Save result locally if needed
    if keep_result:
        import json
        with open(f"{job_guid}_result.json", "w") as f:
            json.dump(result, f, indent=2)
    
    # Clean up the job from DocuDevs
    delete_result = await client.delete_job(job_guid)
    if delete_result.status_code == 200:
        print(f"Cleaned up job: {delete_result.parsed['jobsDeleted']} deleted")
    
    return result

# Usage
invoice_data = await process_and_cleanup("invoice.pdf")
```

## Error Handling

Handle common errors when deleting jobs:

```python
result = await client.delete_job(job_guid)

if result.status_code == 200:
    print(f"Deleted successfully: {result.parsed['jobsDeleted']} job(s)")
elif result.status_code == 404:
    print("Job not found - may already be deleted or purged")
elif result.status_code == 400:
    error_msg = result.parsed.get("message", "") if result.parsed else ""
    print(f"Cannot delete: {error_msg}")  # e.g., job still processing
else:
    print(f"Unexpected status: {result.status_code}")
```

## API Reference

### DELETE /job/\{guid\}

Delete a job and its associated storage data.

**Parameters:**

| Parameter | Type | Required | Description |
|-----------|------|----------|-------------|
| `guid` | path | Yes | The job GUID |

**Response:**

```json
{
  "jobsDeleted": 1,
  "errors": []
}
```

**Error Responses:**

| Status | Description |
|--------|-------------|
| `404` | Job not found |
| `400` | Job is not in terminal state (still processing) |

## Best Practices

### Data Retention

- **Define a retention policy** based on your business requirements
- **Export important results** before deleting jobs
- **Use Cases** for documents you need to keep long-term

### Cost Optimization

- **Let automatic purge handle old jobs** - no action needed for jobs older than 14 days
- **Delete jobs immediately after processing** if you don't need them stored
- **Monitor storage usage** in your organization dashboard

### Compliance

- **Document your retention policy** for audit purposes
- **Usage records are preserved** for billing accuracy
- **Job GUIDs remain in usage history** for traceability

## What's Next?

- Learn about [Cases](/docs/advanced/cases) for long-term document storage
- Explore [LLM Tracing](/docs/advanced/tracing) for debugging extractions
- Check [Operations](/docs/advanced/operations) for post-processing workflows

## Webhooks

Instead of polling for job status, you can configure a **webhook URL** to receive HTTP POST notifications when jobs complete or fail. This is ideal for event-driven architectures.

### Configuration

Set your webhook URL in the **Settings → Webhooks** page in the UI, or via the API/SDK:

<Tabs
  defaultValue="python"
  values={[
    {label: 'Python SDK', value: 'python'},
    {label: 'cURL', value: 'curl'},
  ]}>
  <TabItem value="python">

```python
# Configure organization-level webhook
await client.update_webhook_settings(url="https://example.com/webhooks/docudevs")

# Check current webhook settings
settings = await client.get_webhook_settings()
print(settings)  # {"url": "https://example.com/webhooks/docudevs"}

# Disable webhooks
await client.update_webhook_settings(url=None)
```

  </TabItem>
  <TabItem value="curl">

```bash
# Set webhook URL
curl -X PUT "https://api.docudevs.ai/settings/webhook" \
  -H "Authorization: Bearer $API_KEY" \
  -H "Content-Type: application/json" \
  -d '{"url": "https://example.com/webhooks/docudevs"}'

# Get current settings
curl "https://api.docudevs.ai/settings/webhook" \
  -H "Authorization: Bearer $API_KEY"
```

  </TabItem>
</Tabs>

You can also set a **per-request webhook URL** by including `webhookUrl` in your upload command, which overrides the organization default for that specific job.

### Payload

When a job reaches a terminal state (`COMPLETED` or `ERROR`), DocuDevs sends a POST request to your webhook URL:

```json
{
  "jobGuid": "550e8400-e29b-41d4-a716-446655440000",
  "status": "COMPLETED",
  "error": null,
  "qualityScore": 0.92,
  "qualityCategory": "Very Confident"
}
```

### Best Practices

- **Return a 2xx status** quickly — webhook delivery is best-effort with no retries
- **Verify the payload** in your handler (the `jobGuid` can be used to fetch full results)
- **Use HTTPS** endpoints for security
- **Handle duplicates** — in rare cases the same notification may be sent more than once
