---
title: Job Management
description: Track processing job status, list jobs, retrieve results, and manage job lifecycle with polling and webhooks.
sidebar_position: 4
---
<!-- Generated from docs/advanced/job-management.md. Do not edit by hand. -->

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
| `CREATED` | Job exists and has not been submitted for processing |
| `PENDING` | Submitted, waiting to be processed |
| `IN_PROGRESS` | Document is being processed |
| `OCR_COMPLETED` | Text extraction finished, extraction still running |
| `RUNNING` | A batch job is processing its documents |
| `COMPLETED` | Processing finished successfully |
| `ERROR` | Processing failed with an error |
| `FAILED` | Processing could not be completed |
| `TIMEOUT` | Processing timed out |

Any status that is not terminal means the job is still running: keep polling until `COMPLETED`, `ERROR`, `FAILED` or `TIMEOUT`. Only jobs in these **terminal states** can be deleted. A batch whose documents partly failed ends `COMPLETED`; `GET /job/result/{guid}` for the batch reports `state: "PARTIAL"` with a status for each document, and the results of the documents that succeeded can still be read.

## List and Inspect Jobs

### List jobs

`GET /job/list` returns one page of the jobs of your organization: document jobs, batches and the jobs that operations create (`jobType` tells them apart). Jobs that were deleted or removed by the automatic purge are not in the list. Every query parameter is optional:

| Parameter | Meaning |
| --- | --- |
| `page` | Page number, starting at 0. Default `0`. |
| `size` | Jobs per page. Default `20`. |
| `sort` | `<field>` or `<field>,asc` or `<field>,desc`, where the field is `createdAt`, `updatedAt` or `status`. Any other field sorts by `createdAt`. Default `createdAt`. |
| `status` | Only jobs with exactly this status, for example `COMPLETED` or `ERROR`. |
| `createdAtFrom` | Only jobs created on or after this date, written `YYYY-MM-DD`. |
| `createdAtTo` | Only jobs created on or before this date, written `YYYY-MM-DD`. |

Without `sort` the oldest jobs come first. Use `sort=createdAt,desc` for the newest first. Write `desc` in lower case. Write dates as `YYYY-MM-DD`; other forms do not filter.

The response has `content`, the jobs of the page, and `totalSize` and `totalPages`, the counts over all jobs that match the filters. Each job is the same object that `GET /job/status/{guid}` returns, with fields such as `guid`, `status`, `jobType`, `createdAt`, `updatedAt` and `error`. Timestamps are UTC.

The example walks through all pages of the completed jobs created since a date, newest first. The Python SDK has no method for this route, so it is called over HTTP.

**cURL**

```bash
# Completed jobs created since 2025-01-01, newest first, 50 per page
PAGE=0
while true; do
  RESPONSE=$(curl -s -S -f -G "https://api.docudevs.ai/job/list" \
    -H "Authorization: $API_KEY" \
    --data-urlencode "status=COMPLETED" \
    --data-urlencode "createdAtFrom=2025-01-01" \
    --data-urlencode "sort=createdAt,desc" \
    --data-urlencode "size=50" \
    --data-urlencode "page=$PAGE")

  echo "$RESPONSE" | jq -r '(.content // [])[] | "\(.guid) \(.status) \(.createdAt)"'

  TOTAL_PAGES=$(echo "$RESPONSE" | jq -r '.totalPages // 0')
  PAGE=$((PAGE + 1))
  if [ "$PAGE" -ge "$TOTAL_PAGES" ]; then
    break
  fi
done
```

### Processing parameters of a job

`GET /job/params/{guid}` returns the processing options the job was submitted with, as JSON: for example `mimeType`, `ocr`, `llm`, `barcodes` and `extractionMode`. The prompt and the schema are not part of it. The route answers `404` when the job does not exist or has no stored parameters.

### Dependencies of a job

`GET /job/dependencies/{guid}` describes the [`dependsOn`](#reusing-previous-results-dependson) link of a job:

```json
{
  "guid": "11111111-2222-3333-4444-555555555555",
  "dependsOnGuid": "66666666-7777-8888-9999-000000000000",
  "dependencyStatus": "PENDING",
  "parentJob": { "guid": "66666666-7777-8888-9999-000000000000", "status": "IN_PROGRESS" }
}
```

- `dependsOnGuid` and `parentJob` are present only for a job that was submitted with `dependsOn`. `parentJob` is the full job object of the parent. Read `dependencyStatus` only when `dependsOnGuid` is present.
- `dependencyStatus` is `PENDING` while the job waits for its parent, `READY` once the job is released to run, and `BLOCKED` when the parent failed, timed out or cannot be found. A blocked job carries the reason in its `error`.
- An unknown GUID answers `404`.

The example reads the parameters and the dependency of one job. The Python SDK has no method for either route.

**cURL**

```bash
JOB_GUID="<job-guid>"  # GUID of a job that was submitted with dependsOn

# The processing options the job was submitted with
curl -s -S -f "https://api.docudevs.ai/job/params/$JOB_GUID" \
  -H "Authorization: $API_KEY" | jq .

# The job it depends on, and whether it may run
curl -s -S -f "https://api.docudevs.ai/job/dependencies/$JOB_GUID" \
  -H "Authorization: $API_KEY" \
  | jq '{dependsOnGuid, dependencyStatus, parentStatus: .parentJob.status}'
```

## Deleting Jobs

Delete a job when you no longer need its data. Jobs older than 14 days are automatically purged, so this API is primarily for cleaning up recent jobs before the scheduled cleanup.

**cURL**

```bash
JOB_GUID="<job-guid>"  # GUID of a completed job

curl -s -S -f -X DELETE "https://api.docudevs.ai/job/$JOB_GUID" \
  -H "Authorization: $API_KEY" | jq -r '"Deleted \(.jobsDeleted) job(s)"'
```

**Python**

```python
import asyncio
import os

from docudevs.docudevs_client import DocuDevsClient


async def main():
    client = DocuDevsClient(token=os.environ["API_KEY"])

    job_guid = "<job-guid>"  # GUID of a completed job

    result = await client.delete_job(job_guid)
    if result.status_code == 200:
        print(f"Deleted {result.parsed['jobsDeleted']} job(s)")


asyncio.run(main())
```

**Java**

```java
import ai.docudevs.client.DocuDevsClient;
import com.fasterxml.jackson.databind.JsonNode;

public class Example {
    public static void main(String[] args) throws Exception {
        DocuDevsClient client = DocuDevsClient.builder()
                .apiKey(System.getenv("API_KEY"))
                .baseUrl("https://api.docudevs.ai")
                .build();

        String jobGuid = "<job-guid>"; // GUID of a completed job

        JsonNode purgeResult = client.deleteJob(jobGuid);

        System.out.println("Deleted " + purgeResult.path("jobsDeleted").asInt() + " job(s)");
    }
}
```

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

Use the `dependsOn` query parameter when processing a document to reference a previous job. In this example the second extraction runs on the job that already holds the parsed document, so the job's own GUID is the `dependsOn` value.

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

HEADER_SCHEMA='{
  "type": "object",
  "properties": {
    "invoice_number": {"type": "string"},
    "vendor": {"type": "string"},
    "total": {"type": "number"}
  },
  "required": ["total"]
}'

LINE_ITEMS_SCHEMA='{
  "type": "object",
  "properties": {
    "line_items": {
      "type": "array",
      "items": {
        "type": "object",
        "properties": {
          "description": {"type": "string"},
          "amount": {"type": "number"}
        }
      }
    }
  }
}'

# First extraction: parse the document and extract the header fields
JOB_GUID=$(curl -s -S -f -X POST "https://api.docudevs.ai/document/upload" \
  -H "Authorization: $API_KEY" \
  -F "document=@invoice.pdf" | jq -r '.guid')

jq -n --arg schema "$HEADER_SCHEMA" \
  '{mimeType: "application/pdf", prompt: "Extract invoice header fields", schema: $schema}' |
  curl -s -S -f -o /dev/null -X POST "https://api.docudevs.ai/document/process/$JOB_GUID" \
    -H "Authorization: $API_KEY" \
    -H "Content-Type: application/json" \
    --data-binary @-

wait_for_job "$JOB_GUID"
curl -s -S -f "https://api.docudevs.ai/job/result/$JOB_GUID/json" \
  -H "Authorization: $API_KEY" | jq .

# Second extraction: reuse the parsed document with another schema (no re-OCR)
jq -n --arg schema "$LINE_ITEMS_SCHEMA" \
  '{mimeType: "application/pdf", prompt: "Extract line items", schema: $schema}' |
  curl -s -S -f -o /dev/null -X POST "https://api.docudevs.ai/document/process/$JOB_GUID?dependsOn=$JOB_GUID" \
    -H "Authorization: $API_KEY" \
    -H "Content-Type: application/json" \
    --data-binary @-

wait_for_job "$JOB_GUID"
curl -s -S -f "https://api.docudevs.ai/job/result/$JOB_GUID/json" \
  -H "Authorization: $API_KEY" | jq .
```

**Python**

```python
from pydantic import BaseModel

from docudevs import json_schema
from docudevs.models import UploadCommand

class InvoiceHeader(BaseModel):
    invoice_number: str | None = None
    vendor: str | None = None
    total: float

class LineItem(BaseModel):
    description: str | None = None
    amount: float | None = None

class InvoiceLineItems(BaseModel):
    line_items: list[LineItem] = []

with open("invoice.pdf", "rb") as f:
    document_data = f.read()

# First extraction: parse the document and extract the header fields
job_guid = await client.submit_and_process_document(
    document=document_data,
    document_mime_type="application/pdf",
    prompt="Extract invoice header fields",
    schema=json_schema(InvoiceHeader),
)
header_result = await client.wait_until_ready(job_guid, result_format="json")
print(json.dumps(header_result, indent=2))

# Second extraction: reuse the parsed document with another schema (no re-OCR)
response = await client.process_document(
    guid=job_guid,
    body=UploadCommand(
        mime_type="application/pdf",
        prompt="Extract line items",
        schema=json_schema(InvoiceLineItems),
    ),
    depends_on=job_guid,
)
if response.status_code != 200:
    raise RuntimeError(f"HTTP {response.status_code}")

line_items_result = await client.wait_until_ready(job_guid, result_format="json")
print(json.dumps(line_items_result, indent=2))
```

The dependent job waits for the parent to complete, then reuses its OCR/parsed content. This is especially useful for:

- Extracting **multiple schemas** from the same document
- Running **different prompts** against the same content
- Performing **operations** (generative tasks) on already-processed jobs via `/operation/{jobGuid}/generative-task`

## Quality Score

Jobs whose OCR step computes quality include a **quality score** (0.0–1.0) and a **quality category** that indicate how confident the OCR engine was about the extracted text; both are empty when no quality was computed. Use this to build automated quality gates.

| Category | Score Range | Description |
|----------|-----------|-------------|
| `Very Confident` | 0.85–1.0 | High quality, reliable extraction |
| `Confident` | 0.70–0.85 | Good quality, minor issues possible |
| `Likely Handwriting Problems` | 0.40–0.70 | May need review or PREMIUM OCR |
| `Many Problems` | 0.0–0.40 | Consider re-processing with PREMIUM OCR |

**cURL**

```bash
JOB_GUID="<job-guid>"  # GUID of a completed job

curl -s -S -f "https://api.docudevs.ai/job/status/$JOB_GUID" \
  -H "Authorization: $API_KEY" | jq '{qualityScore, qualityCategory}'
```

**Python**

```python
job_guid = "<job-guid>"  # GUID of a completed job

status_response = await client.status(guid=job_guid)
if status_response.status_code != 200:
    raise RuntimeError(f"HTTP {status_response.status_code}")

job = status_response.parsed
if isinstance(job.quality_score, float):
    print(f"Quality: {job.quality_score:.2f} ({job.quality_category})")
else:
    print("No quality score for this job")
```

**CLI**

```bash
JOB_GUID="<job-guid>"  # GUID of a completed job

# Prints the full job status, which includes the quality score and category
docudevs status "$JOB_GUID"
```

To let DocuDevs pick the OCR level for you, set `ocr` to `AUTO`. The job starts with the default OCR and switches to `PREMIUM` when the quality is low. Check the job's quality score afterwards as shown above.

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
  -F "document=@scan.pdf" | jq -r '.guid')

# AUTO starts with the default OCR and switches to PREMIUM when the quality is low
curl -s -S -f -o /dev/null -X POST "https://api.docudevs.ai/document/process/$JOB_GUID" \
  -H "Authorization: $API_KEY" \
  -H "Content-Type: application/json" \
  -d '{
    "mimeType": "application/pdf",
    "prompt": "Extract the key fields",
    "ocr": "AUTO"
  }'

wait_for_job "$JOB_GUID"

curl -s -S -f "https://api.docudevs.ai/job/status/$JOB_GUID" \
  -H "Authorization: $API_KEY" | jq '{qualityScore, qualityCategory}'
```

**Python**

```python
with open("scan.pdf", "rb") as f:
    document_data = f.read()

# AUTO starts with the default OCR and switches to PREMIUM when the quality is low
job_guid = await client.submit_and_process_document(
    document=document_data,
    document_mime_type="application/pdf",
    prompt="Extract the key fields",
    ocr="AUTO",
)
await client.wait_until_ready(job_guid, result_format="json")

status_response = await client.status(guid=job_guid)
if status_response.status_code != 200:
    raise RuntimeError(f"HTTP {status_response.status_code}")

job = status_response.parsed
if isinstance(job.quality_score, float):
    print(f"Quality: {job.quality_score:.2f} ({job.quality_category})")
else:
    print("No quality score for this job")
```

:::info Case Documents
Documents uploaded to [Cases](https://docs.docudevs.ai/docs/advanced/cases) are **not automatically purged**. Case documents persist until the case is deleted or documents are manually removed from the case.
:::

## Example: Cleanup Workflow

A complete workflow for processing a document and cleaning up afterward:

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

curl -s -S -f -o /dev/null -X POST "https://api.docudevs.ai/document/process/$JOB_GUID" \
  -H "Authorization: $API_KEY" \
  -H "Content-Type: application/json" \
  -d '{
    "mimeType": "application/pdf",
    "prompt": "Extract invoice data"
  }'

wait_for_job "$JOB_GUID"

# Save the result locally before deleting the job
curl -s -S -f "https://api.docudevs.ai/job/result/$JOB_GUID/json" \
  -H "Authorization: $API_KEY" | jq . > "${JOB_GUID}_result.json"

# Clean up the job from DocuDevs
curl -s -S -f -X DELETE "https://api.docudevs.ai/job/$JOB_GUID" \
  -H "Authorization: $API_KEY" | jq -r '"Cleaned up job: \(.jobsDeleted) deleted"'
```

**Python**

```python
with open("invoice.pdf", "rb") as f:
    document_data = f.read()

job_guid = await client.submit_and_process_document(
    document=document_data,
    document_mime_type="application/pdf",
    prompt="Extract invoice data",
)

result = await client.wait_until_ready(job_guid, result_format="json")

# Save the result locally before deleting the job
with open(f"{job_guid}_result.json", "w") as f:
    json.dump(result, f, indent=2)

# Clean up the job from DocuDevs
delete_result = await client.delete_job(job_guid)
if delete_result.status_code == 200:
    print(f"Cleaned up job: {(delete_result.parsed or {}).get('jobsDeleted', 0)} deleted")
```

## Error Handling

Handle common errors when deleting jobs:

**cURL**

```bash
JOB_GUID="<job-guid>"  # GUID of the job to delete

HTTP_STATUS=$(curl -s -S -o response.json -w "%{http_code}" -X DELETE "https://api.docudevs.ai/job/$JOB_GUID" \
  -H "Authorization: $API_KEY")

case "$HTTP_STATUS" in
  200)
    echo "Deleted successfully: $(jq '.jobsDeleted' response.json) job(s)"
    ;;
  404)
    echo "Job not found - may already be deleted or purged"
    ;;
  409)
    echo "Cannot delete: $(jq -r '.errors | join("; ")' response.json)"  # e.g., job still processing
    ;;
  *)
    echo "Unexpected status: $HTTP_STATUS"
    ;;
esac
```

**Python**

```python
job_guid = "<job-guid>"  # GUID of the job to delete

result = await client.delete_job(job_guid)

if result.status_code == 200:
    print(f"Deleted successfully: {result.parsed['jobsDeleted']} job(s)")
elif result.status_code == 404:
    print("Job not found - may already be deleted or purged")
elif result.status_code == 409:
    errors = result.parsed.get("errors", []) if result.parsed else []
    print(f"Cannot delete: {'; '.join(errors)}")  # e.g., job still processing
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
| `409` | Job is not in a terminal state (still processing); the body is `{"jobsDeleted": 0, "errors": ["..."]}` |

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

- Learn about [Cases](https://docs.docudevs.ai/docs/advanced/cases) for long-term document storage
- Explore [LLM Tracing](https://docs.docudevs.ai/docs/advanced/tracing) for debugging extractions
- Check [Operations](https://docs.docudevs.ai/docs/advanced/operations) for post-processing workflows

## Webhooks

Instead of polling for job status, you can configure a **webhook URL** to receive HTTP POST notifications when jobs complete or fail (a job that ends `TIMEOUT` sends none). This is ideal for event-driven architectures.

### Configuration

Set your webhook URL in the **Settings → Webhooks** page in the UI, or via the API:

**cURL**

```bash
# Set the organization webhook URL
curl -s -S -f -X PUT "https://api.docudevs.ai/settings/webhook" \
  -H "Authorization: $API_KEY" \
  -H "Content-Type: application/json" \
  -d '{"url": "https://example.com/webhooks/docudevs"}'

# Check the current webhook settings
curl -s -S -f "https://api.docudevs.ai/settings/webhook" \
  -H "Authorization: $API_KEY"

# Disable webhooks
curl -s -S -f -X PUT "https://api.docudevs.ai/settings/webhook" \
  -H "Authorization: $API_KEY" \
  -H "Content-Type: application/json" \
  -d '{"url": null}'
```

The Python SDK has no helper for webhook settings, so use the HTTP API (or the settings page) to change them.

### Payload

When a job ends `COMPLETED`, `ERROR` or `FAILED`, DocuDevs sends a POST request to your webhook URL. No webhook is sent for `TIMEOUT`, so poll the job if you need to catch it:

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
