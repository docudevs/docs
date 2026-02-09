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
