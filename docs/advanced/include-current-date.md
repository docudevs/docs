---
title: Include Current Date
description: Pass the current date to the LLM so date-dependent extractions and calculations are accurate.
sidebar_position: 6
---

import Tabs from '@theme/Tabs';
import TabItem from '@theme/TabItem';

# Include Current Date

Provide the LLM with today's date so that date-relative calculations in your extractions produce correct results.

## Overview

LLMs do not inherently know the current date. If your extraction prompt or schema relies on date-dependent logic — for example computing the age of a vehicle, the remaining term of a contract, or how many days until an expiry date — the model may hallucinate or return inaccurate values.

Setting `includeCurrentDate: true` injects a short message containing today's date (ISO 8601 format) into the LLM conversation, giving the model the context it needs.

## When to Use

- **Age / duration calculations**: "How old is this vehicle?" or "Years since manufacture"
- **Relative date references**: "Is this contract still valid?" or "Days until expiry"
- **Depreciation or tax formulas**: Anything that compares a document date to "today"

If your extraction does not involve date arithmetic, you can leave the flag off (the default).

## Enabling Include Current Date

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

client = DocuDevsClient(token=os.getenv("API_KEY"))

job_guid = await client.submit_and_process_document(
    document=document_bytes,
    document_mime_type="application/pdf",
    prompt="Extract vehicle registration details including vehicle age in years",
    schema=schema_json,
    include_current_date=True,
)

result = await client.wait_until_ready(job_guid)
```

The parameter is also available on map-reduce methods:

```python
job_guid = await client.submit_and_process_document_map_reduce(
    document=document_bytes,
    document_mime_type="application/pdf",
    prompt="Extract line items with remaining warranty days",
    schema=schema_json,
    pages_per_chunk=2,
    include_current_date=True,
)
```

  </TabItem>
  <TabItem value="java">

```java
import ai.docudevs.client.DocuDevsClient;
import ai.docudevs.client.ProcessOptions;
import ai.docudevs.client.UploadRequest;
import ai.docudevs.client.WaitOptions;
import java.nio.file.Files;
import java.nio.file.Path;

DocuDevsClient client = DocuDevsClient.builder()
    .apiKey(System.getenv("API_KEY"))
    .build();

byte[] fileBytes = Files.readAllBytes(Path.of("registration.pdf"));
UploadRequest upload = new UploadRequest("registration.pdf", "application/pdf", fileBytes);

String guid = client.submitAndProcessDocument(
    upload,
    ProcessOptions.builder()
        .mimeType("application/pdf")
        .prompt("Extract vehicle registration details including vehicle age in years")
        .schema(schemaJson)
        .includeCurrentDate(true)
        .build()
);

var result = client.waitUntilReadyJson(guid, WaitOptions.builder().build());
```

  </TabItem>
  <TabItem value="curl">

```bash
# Process with includeCurrentDate enabled
curl -X POST https://api.docudevs.ai/document/process/{guid} \
  -H "Authorization: Bearer $API_KEY" \
  -H "Content-Type: application/json" \
  -d '{
    "prompt": "Extract vehicle registration details including vehicle age in years",
    "schema": "...",
    "mimeType": "application/pdf",
    "includeCurrentDate": true
  }'
```

  </TabItem>
</Tabs>

## How It Works

When `includeCurrentDate` is `true`, the system adds a message to the LLM conversation before the document content:

```
Current date: 2025-07-17
```

The date is always in **ISO 8601** format (`YYYY-MM-DD`) and represents the server's current date at the time the extraction runs.

## Notes

- The flag defaults to `false` — existing requests are unaffected.
- Works with all extraction modes: `SIMPLE`, `STEPS`, and map-reduce.
- When used with map-reduce, every chunk receives the current date.
- The date is injected once per LLM call; it does not add significant token overhead.
