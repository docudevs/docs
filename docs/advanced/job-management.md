---
title: Job Management
description: Track processing job status, list jobs, retrieve results, and manage job lifecycle with polling and webhooks.
sidebar_position: 4
---

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

The example walks through all pages of the completed jobs created since a date, newest first. The Python SDK and the Java client have no method for this route, so it is called over HTTP.

<Example name="jobs/list-jobs" />

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

The example reads the parameters and the dependency of one job. The Python SDK and the Java client have no method for either route.

<Example name="jobs/inspect-job" />

## Deleting Jobs

Delete a job when you no longer need its data. Jobs older than 14 days are automatically purged, so this API is primarily for cleaning up recent jobs before the scheduled cleanup.

<Example name="jobs/delete-job" />

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

<Example name="jobs/process-with-depends-on" />

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

<Example name="jobs/check-quality" />

To let DocuDevs pick the OCR level for you, set `ocr` to `AUTO`. The job starts with the default OCR and switches to `PREMIUM` when the quality is low. Check the job's quality score afterwards as shown above.

<Example name="jobs/process-with-auto-ocr" />

:::info Case Documents
Documents uploaded to [Cases](/docs/advanced/cases) are **not automatically purged**. Case documents persist until the case is deleted or documents are manually removed from the case.
:::

## Example: Cleanup Workflow

A complete workflow for processing a document and cleaning up afterward:

<Example name="jobs/process-and-clean-up" />

## Error Handling

Handle common errors when deleting jobs:

<Example name="jobs/handle-delete-errors" />

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

- Learn about [Cases](/docs/advanced/cases) for long-term document storage
- Explore [LLM Tracing](/docs/advanced/tracing) for debugging extractions
- Check [Operations](/docs/advanced/operations) for post-processing workflows

## Webhooks

Instead of polling for job status, you can configure a **webhook URL** to receive HTTP POST notifications when jobs complete or fail (a job that ends `TIMEOUT` sends none). This is ideal for event-driven architectures.

### Configuration

Set your webhook URL in the **Settings → Webhooks** page in the UI, or via the API:

<Example name="webhooks/configure-webhook" />

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
