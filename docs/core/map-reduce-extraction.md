---
title: Map-Reduce Extraction
description: Break long or repetitive documents into manageable windows while preserving header context.
sidebar_position: 4
---

import Tabs from '@theme/Tabs';
import TabItem from '@theme/TabItem';

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
| `splitType` | Chunking mode: `PAGE` or `MARKDOWN_HEADER`. | `PAGE` |
| `splitHeaderLevel` | Header level used when `splitType=MARKDOWN_HEADER` (`1` for `#`, `2` for `##`). | `2` |
| `pagesPerChunk` | Number of pages per extraction window. | `1` |
| `overlapPages` | Number of pages to overlap between chunks to catch data spanning pages. | `0` |
| `dedupKey` | JSON path to a unique field (e.g., `sku`, `date`) used to remove duplicates caused by overlap. | `null` |
| `parallel_processing` | When `true`, chunks are processed in parallel by multiple workers for faster throughput. | `false` |
| `header_options` | Configuration for extracting document-level metadata (header). | `null` |

When `splitType=MARKDOWN_HEADER`, chunking is based on OCR markdown sections instead of pages. In this mode, `overlapPages` and `dedupKey` do not apply.

## Example: Processing a Long Invoice

Imagine a 50-page invoice where the first page has the **Invoice Number** and **Date** (Header), and the remaining 49 pages contain **Line Items** (Rows).

<Tabs
  defaultValue="python"
  values={[
    {label: 'Python SDK', value: 'python'},
    {label: 'Java SDK', value: 'java'},
    {label: 'CLI', value: 'cli'},
    {label: 'cURL', value: 'curl'},
  ]}>
  <TabItem value="python">

```python
from docudevs.docudevs_client import DocuDevsClient
import os
import asyncio

client = DocuDevsClient(token=os.getenv('API_KEY'))

async def process_long_invoice():
    with open("large-invoice.pdf", "rb") as f:
        document_data = f.read()

    # Submit Map-Reduce Job
    job_id = await client.submit_and_process_document_map_reduce(
        document=document_data,
        document_mime_type="application/pdf",
        
        # Instructions for extracting rows (Line Items)
        prompt="Extract invoice line items (sku, description, quantity, unitPrice, total).",
        
        # Chunking Strategy
        pages_per_chunk=4,
        overlap_pages=1,
        dedup_key="lineItems.sku",  # Unique key to deduplicate rows
        parallel_processing=True,  # Process chunks in parallel for faster results
        
        # Header Extraction Strategy
        header_options={
            "page_limit": 1,  # Look for header only on page 1
            "include_in_rows": False, # Don't include page 1 in row processing
            "row_prompt_augmentation": "This is an invoice from Acme Corp." # Context for every chunk
        },
        header_schema='{"invoiceNumber":"string","issueDate":"string","billingAddress":"string"}'
    )

    print(f"Job submitted: {job_id}")
    
    # Wait for results
    result = await client.wait_until_ready(job_id, result_format="json")
    
    print("Header:", result.get("header"))
    print(f"Extracted {len(result.get('records', []))} line items.")

if __name__ == "__main__":
    asyncio.run(process_long_invoice())
```

  </TabItem>
  <TabItem value="cli">

```bash
# Process a long document using Map-Reduce
docudevs process-map-reduce large-invoice.pdf \
  --prompt "Extract line items" \
  --pages-per-chunk 4 \
  --overlap 1 \
  --dedup-key "sku" \
  --header-page-limit 1 \
  --header-schema '{"invoiceNumber": "string"}'
```

  </TabItem>
  <TabItem value="java">

```java
import ai.docudevs.client.generated.api.DocumentApi;
import ai.docudevs.client.generated.api.JobApi;
import ai.docudevs.client.generated.internal.ApiClient;
import ai.docudevs.client.generated.model.MapReduceCommand;
import ai.docudevs.client.generated.model.MapReduceHeaderCommand;
import ai.docudevs.client.generated.model.MapReduceSplitType;
import ai.docudevs.client.generated.model.ProcessingJob;
import ai.docudevs.client.generated.model.UploadCommand;
import ai.docudevs.client.generated.model.UploadResponse;
import java.io.File;

ApiClient apiClient = new ApiClient();
apiClient.updateBaseUri("https://api.docudevs.ai");
apiClient.setRequestInterceptor(req ->
    req.header("Authorization", "Bearer " + System.getenv("API_KEY"))
);

DocumentApi documentApi = new DocumentApi(apiClient);
JobApi jobApi = new JobApi(apiClient);

UploadResponse upload = documentApi.uploadDocument(new File("large-invoice.pdf"));

MapReduceHeaderCommand header = new MapReduceHeaderCommand()
    .enabled(true)
    .pageLimit(1)
    .includeInRows(false)
    .rowPromptAugmentation("This is an invoice from Acme Corp.")
    .schema("""
        {"invoiceNumber":"string","issueDate":"string","billingAddress":"string"}
        """);

MapReduceCommand mapReduce = new MapReduceCommand()
    .enabled(true)
    .splitType(MapReduceSplitType.PAGE)
    .pagesPerChunk(4)
    .overlapPages(1)
    .dedupKey("lineItems.sku")
    .parallelProcessing(true)
    .header(header);

UploadCommand command = new UploadCommand()
    .mimeType("application/pdf")
    .prompt("Extract invoice line items (sku, description, quantity, unitPrice, total).")
    .mapReduce(mapReduce);

documentApi.processDocument(upload.getGuid(), null, command);

while (true) {
    ProcessingJob status = jobApi.getJobStatus(upload.getGuid());
    if ("COMPLETED".equals(status.getStatus())) {
        break;
    }
    if ("ERROR".equals(status.getStatus()) || "TIMEOUT".equals(status.getStatus())) {
        throw new IllegalStateException("Map-reduce failed: " + status.getStatus());
    }
    Thread.sleep(3000);
}

Object result = jobApi.resultJson(upload.getGuid());
System.out.println(result);
```

  </TabItem>

  <TabItem value="curl">

```bash
# Map-Reduce via API requires constructing the multipart request carefully.
# We recommend using the SDK for complex Map-Reduce jobs.
# However, you can use the standard process endpoint with specific parameters.

curl -X POST https://api.docudevs.ai/document/upload-files \
  -H "Authorization: Bearer $API_KEY" \
  -F "document=@large-invoice.pdf" \
  -F "mapReduceOptions='{
    \"splitType\": \"PAGE\",
    \"pagesPerChunk\": 4,
    \"overlapPages\": 1,
    \"dedupKey\": \"sku\",
    \"headerOptions\": {\"pageLimit\": 1}
  }'"
```

  </TabItem>
</Tabs>

## Example: Split by Markdown Headers

Use this when OCR text is markdown and your document is organized by headings.

<Tabs
  defaultValue="python"
  values={[
    {label: 'Python SDK', value: 'python'},
    {label: 'Java SDK', value: 'java'},
    {label: 'CLI', value: 'cli'},
    {label: 'cURL', value: 'curl'},
  ]}>
  <TabItem value="python">

```python
job_id = await client.submit_and_process_document_map_reduce(
    document=document_data,
    document_mime_type="application/pdf",
    prompt="Extract obligations by section.",
    split_type="markdown_header",
    split_header_level=2,  # split on ## headings
    parallel_processing=True
)
```

  </TabItem>
  <TabItem value="cli">

```bash
docudevs process-map-reduce contract.pdf \
  --prompt "Extract obligations by section" \
  --split-type markdown-header \
  --split-header-level 2
```

  </TabItem>
  <TabItem value="java">

```java
import ai.docudevs.client.generated.api.DocumentApi;
import ai.docudevs.client.generated.api.JobApi;
import ai.docudevs.client.generated.internal.ApiClient;
import ai.docudevs.client.generated.model.MapReduceCommand;
import ai.docudevs.client.generated.model.MapReduceSplitType;
import ai.docudevs.client.generated.model.ProcessingJob;
import ai.docudevs.client.generated.model.UploadCommand;
import ai.docudevs.client.generated.model.UploadResponse;
import java.io.File;

ApiClient apiClient = new ApiClient();
apiClient.updateBaseUri("https://api.docudevs.ai");
apiClient.setRequestInterceptor(req ->
    req.header("Authorization", "Bearer " + System.getenv("API_KEY"))
);

DocumentApi documentApi = new DocumentApi(apiClient);
JobApi jobApi = new JobApi(apiClient);

UploadResponse upload = documentApi.uploadDocument(new File("contract.pdf"));

UploadCommand command = new UploadCommand()
    .mimeType("application/pdf")
    .prompt("Extract obligations by section.")
    .mapReduce(
        new MapReduceCommand()
            .enabled(true)
            .splitType(MapReduceSplitType.MARKDOWN_HEADER)
            .splitHeaderLevel(2)
            .parallelProcessing(true)
    );

documentApi.processDocument(upload.getGuid(), null, command);

while (true) {
    ProcessingJob status = jobApi.getJobStatus(upload.getGuid());
    if ("COMPLETED".equals(status.getStatus())) {
        break;
    }
    if ("ERROR".equals(status.getStatus()) || "TIMEOUT".equals(status.getStatus())) {
        throw new IllegalStateException("Map-reduce failed: " + status.getStatus());
    }
    Thread.sleep(3000);
}

Object result = jobApi.resultJson(upload.getGuid());
System.out.println(result);
```

  </TabItem>

  <TabItem value="curl">

```bash
curl -X POST "https://api.docudevs.ai/document/process/JOB_GUID" \
  -H "Authorization: Bearer $API_KEY" \
  -H "Content-Type: application/json" \
  -d '{
    "mimeType": "application/pdf",
    "prompt": "Extract obligations by section",
    "mapReduce": {
      "enabled": true,
      "splitType": "MARKDOWN_HEADER",
      "splitHeaderLevel": 2,
      "parallelProcessing": true
    }
  }'
```

  </TabItem>
</Tabs>

## Header Capture Strategy

Often, the first few pages contain "Header" information (Summary, Dates, Parties) that applies to the whole document.

- **`page_limit`**: How many pages at the start are considered "Header".
- **`header_schema`**: A specific JSON schema just for the header data.
- **`row_prompt_augmentation`**: Text injected into every subsequent chunk prompt. Useful for passing context (e.g., "This is Invoice #123") to help the AI understand isolated pages.

## Best Practices

1. **Always use a `dedupKey`**: When `overlapPages > 0`, the same row might appear in two chunks. The `dedupKey` tells DocuDevs how to identify and merge them.
2. **Start small**: Try `pagesPerChunk=4`. If the AI misses context, increase it. If it's too slow, decrease it.
3. **Monitor Progress**: Map-Reduce jobs take longer. Use the status endpoint to track progress.
4. **Use markdown-header split for sectioned docs**: If OCR output is markdown with clear headings, set `splitType=MARKDOWN_HEADER` and choose `splitHeaderLevel` (`1` for top-level sections, `2` for subsection chunks).

```python
status = await client.status(job_id)
print(f"Progress: {status.parsed.mapReduceStatus.completedChunks} / {status.parsed.mapReduceStatus.totalChunks}")
```

## Re-running Map-Reduce on an Existing Job

If you have already processed or OCR'd a document, you can run map-reduce on it again **without re-uploading the file or re-running OCR**. This is useful when you want to:

- Iterate on your prompt/schema without paying for OCR again.
- Run different chunking strategies on the same document.
- Extract different fields from a document that was already OCR'd.

Use `submit_and_wait_for_map_reduce` with the `parent_job_id` of the completed job:

<Tabs
  defaultValue="python"
  values={[
    {label: 'Python SDK', value: 'python'},
    {label: 'Java SDK', value: 'java'},
    {label: 'cURL', value: 'curl'},
  ]}>
  <TabItem value="python">

```python
import asyncio, os
from docudevs.docudevs_client import DocuDevsClient

client = DocuDevsClient(token=os.getenv('API_KEY'))

async def reprocess_with_map_reduce():
    # Assume we already have a completed job from a previous run
    existing_job_id = "your-completed-job-guid"

    # Re-run with map-reduce — no upload, no OCR cost
    result = await client.submit_and_wait_for_map_reduce(
        parent_job_id=existing_job_id,
        prompt="Extract all line items (sku, description, qty, total)",
        schema='{"type":"array","items":{"type":"object"}}',
        pages_per_chunk=5,
        overlap_pages=1,
        dedup_key="sku",
        parallel_processing=True,
        timeout=300
    )

    print("Header:", result.get("header"))
    print(f"Extracted {len(result.get('records', []))} line items.")

asyncio.run(reprocess_with_map_reduce())
```

  </TabItem>
  <TabItem value="java">

```java
import ai.docudevs.client.generated.api.DocumentApi;
import ai.docudevs.client.generated.api.JobApi;
import ai.docudevs.client.generated.internal.ApiClient;
import ai.docudevs.client.generated.model.MapReduceCommand;
import ai.docudevs.client.generated.model.MapReduceSplitType;
import ai.docudevs.client.generated.model.ProcessingJob;
import ai.docudevs.client.generated.model.UploadCommand;

ApiClient apiClient = new ApiClient();
apiClient.updateBaseUri("https://api.docudevs.ai");
apiClient.setRequestInterceptor(req ->
    req.header("Authorization", "Bearer " + System.getenv("API_KEY"))
);

DocumentApi documentApi = new DocumentApi(apiClient);
JobApi jobApi = new JobApi(apiClient);

String existingJobGuid = "your-completed-job-guid";

UploadCommand command = new UploadCommand()
    .mimeType("application/pdf")
    .prompt("Extract all line items (sku, description, qty, total)")
    .schema("{\"type\":\"array\",\"items\":{\"type\":\"object\"}}")
    .mapReduce(
        new MapReduceCommand()
            .enabled(true)
            .splitType(MapReduceSplitType.PAGE)
            .pagesPerChunk(5)
            .overlapPages(1)
            .dedupKey("sku")
            .parallelProcessing(true)
    );

documentApi.processDocument(existingJobGuid, existingJobGuid, command);

while (true) {
    ProcessingJob status = jobApi.getJobStatus(existingJobGuid);
    if ("COMPLETED".equals(status.getStatus())) {
        break;
    }
    if ("ERROR".equals(status.getStatus()) || "TIMEOUT".equals(status.getStatus())) {
        throw new IllegalStateException("Re-processing failed: " + status.getStatus());
    }
    Thread.sleep(3000);
}

Object result = jobApi.resultJson(existingJobGuid);
System.out.println(result);
```

  </TabItem>

  <TabItem value="curl">

```bash
# Process an existing document with map-reduce using the depends_on query param.
# The GUID is reused from a previous upload; dependsOn chains to the parent job.
curl -X POST "https://api.docudevs.ai/document/process/EXISTING_JOB_GUID?dependsOn=EXISTING_JOB_GUID" \
  -H "Authorization: Bearer $API_KEY" \
  -H "Content-Type: application/json" \
  -d '{
    "prompt": "Extract line items",
    "schema": "{\"type\":\"array\"}",
    "mimeType": "application/pdf",
    "mapReduce": {
      "enabled": true,
      "splitType": "PAGE",
      "pagesPerChunk": 5,
      "overlapPages": 1,
      "dedupKey": "sku",
      "parallelProcessing": true
    }
  }'
```

  </TabItem>
</Tabs>

:::tip
Since OCR is the most expensive part of processing, re-running extraction with different prompts or schemas on an already-processed document is very cost-effective.
:::

## Troubleshooting

- **Missing Rows**: Check if rows are split across pages. Increase `overlapPages`.
- **Duplicate Rows**: Ensure your `dedupKey` is truly unique for each row.
- **Slow Processing**: Reduce `pagesPerChunk` to speed up individual chunk processing.
- **Wrong Markdown Sections**: For markdown-header chunking, switch between `splitHeaderLevel=1` and `splitHeaderLevel=2` depending on your document structure.
