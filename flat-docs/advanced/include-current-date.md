---
title: Include Current Date
description: Pass the current date to the LLM so date-dependent extractions and calculations are accurate.
sidebar_position: 6
---
<!-- Generated from docs/advanced/include-current-date.md. Do not edit by hand. -->

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

SCHEMA='{
  "type": "object",
  "properties": {
    "registration_number": {"type": "string"},
    "first_registration_date": {"type": "string", "format": "date"},
    "vehicle_age_years": {"type": "number"}
  },
  "required": ["vehicle_age_years"]
}'

JOB_GUID=$(curl -s -S -f -X POST "https://api.docudevs.ai/document/upload" \
  -H "Authorization: $API_KEY" \
  -F "document=@registration.pdf" | jq -r '.guid')

# Process with includeCurrentDate enabled
jq -n --arg schema "$SCHEMA" \
  '{
    mimeType: "application/pdf",
    prompt: "Extract vehicle registration details including vehicle age in years",
    schema: $schema,
    includeCurrentDate: true
  }' |
  curl -s -S -f -o /dev/null -X POST "https://api.docudevs.ai/document/process/$JOB_GUID" \
    -H "Authorization: $API_KEY" \
    -H "Content-Type: application/json" \
    --data-binary @-

wait_for_job "$JOB_GUID"

curl -s -S -f "https://api.docudevs.ai/job/result/$JOB_GUID/json" \
  -H "Authorization: $API_KEY" | jq .
```

**Python**

```python
import asyncio
import datetime
import json
import os

from pydantic import BaseModel

from docudevs import json_schema
from docudevs.docudevs_client import DocuDevsClient


async def main():
    client = DocuDevsClient(token=os.environ["API_KEY"])

    with open("registration.pdf", "rb") as f:
        document_data = f.read()

    class VehicleRegistration(BaseModel):
        registration_number: str | None = None
        first_registration_date: datetime.date | None = None
        vehicle_age_years: float

    job_guid = await client.submit_and_process_document(
        document=document_data,
        document_mime_type="application/pdf",
        prompt="Extract vehicle registration details including vehicle age in years",
        schema=json_schema(VehicleRegistration),
        include_current_date=True,
    )

    result = await client.wait_until_ready(job_guid, result_format="json")
    print(json.dumps(result, indent=2))


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
import java.time.Duration;

public class Example {
    public static void main(String[] args) throws Exception {
        DocuDevsClient client = DocuDevsClient.builder()
                .apiKey(System.getenv("API_KEY"))
                .baseUrl("https://api.docudevs.ai")
                .build();

        String schemaJson = """
                {
                  "type": "object",
                  "properties": {
                    "registration_number": {"type": "string"},
                    "first_registration_date": {"type": "string", "format": "date"},
                    "vehicle_age_years": {"type": "number"}
                  },
                  "required": ["vehicle_age_years"]
                }
                """;

        byte[] fileBytes = Files.readAllBytes(Path.of("registration.pdf"));
        UploadRequest upload = new UploadRequest("registration.pdf", "application/pdf", fileBytes);

        String guid = client.submitAndProcessDocument(
                upload,
                ProcessOptions.builder()
                        .mimeType("application/pdf")
                        .prompt("Extract vehicle registration details including vehicle age in years")
                        .schema(schemaJson)
                        .includeCurrentDate(true)
                        .build());

        JsonNode result = client.waitUntilReadyJson(guid, WaitOptions.builder()
                .timeout(Duration.ofSeconds(180))
                .pollInterval(Duration.ofSeconds(5))
                .build());
        System.out.println(result.toPrettyString());
    }
}
```

The parameter is also available on map-reduce requests:

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

SCHEMA='{
  "type": "object",
  "properties": {
    "line_items": {
      "type": "array",
      "items": {
        "type": "object",
        "properties": {
          "description": {"type": "string"},
          "warranty_end_date": {"type": "string", "format": "date"},
          "remaining_warranty_days": {"type": "number"}
        }
      }
    }
  }
}'

JOB_GUID=$(curl -s -S -f -X POST "https://api.docudevs.ai/document/upload" \
  -H "Authorization: $API_KEY" \
  -F "document=@warranty-list.pdf" | jq -r '.guid')

# Every chunk receives the current date
jq -n --arg schema "$SCHEMA" \
  '{
    mimeType: "application/pdf",
    prompt: "Extract line items with remaining warranty days",
    schema: $schema,
    includeCurrentDate: true,
    mapReduce: {enabled: true, splitType: "PAGE", pagesPerChunk: 2}
  }' |
  curl -s -S -f -o /dev/null -X POST "https://api.docudevs.ai/document/process/$JOB_GUID" \
    -H "Authorization: $API_KEY" \
    -H "Content-Type: application/json" \
    --data-binary @-

wait_for_job "$JOB_GUID"

curl -s -S -f "https://api.docudevs.ai/job/result/$JOB_GUID/json" \
  -H "Authorization: $API_KEY" | jq .
```

**Python**

```python
import datetime

from pydantic import BaseModel

from docudevs import json_schema

class WarrantyLineItem(BaseModel):
    description: str | None = None
    warranty_end_date: datetime.date | None = None
    remaining_warranty_days: float | None = None

class WarrantyList(BaseModel):
    line_items: list[WarrantyLineItem] = []

with open("warranty-list.pdf", "rb") as f:
    document_data = f.read()

# Every chunk receives the current date
job_guid = await client.submit_and_process_document_map_reduce(
    document=document_data,
    document_mime_type="application/pdf",
    prompt="Extract line items with remaining warranty days",
    schema=json_schema(WarrantyList),
    pages_per_chunk=2,
    include_current_date=True,
)

result = await client.wait_until_ready(job_guid, result_format="json")
print(json.dumps(result, indent=2))
```

**Java**

```java
import ai.docudevs.client.MapReduceOptions;
import ai.docudevs.client.ProcessOptions;
import ai.docudevs.client.Schema;
import ai.docudevs.client.UploadRequest;
import ai.docudevs.client.WaitOptions;
import com.fasterxml.jackson.databind.JsonNode;

String schema = Schema.of(Map.of(
        "type", "object",
        "properties", Map.of(
                "line_items", Map.of(
                        "type", "array",
                        "items", Map.of(
                                "type", "object",
                                "properties", Map.of(
                                        "description", Map.of("type", "string"),
                                        "warranty_end_date", Map.of("type", "string", "format", "date"),
                                        "remaining_warranty_days", Map.of("type", "number")))))));

byte[] documentData = Files.readAllBytes(Path.of("warranty-list.pdf"));

// Every chunk receives the current date
String jobGuid = client.submitAndProcessDocumentMapReduce(
        new UploadRequest("warranty-list.pdf", "application/pdf", documentData),
        ProcessOptions.builder()
                .mimeType("application/pdf")
                .prompt("Extract line items with remaining warranty days")
                .schema(schema)
                .includeCurrentDate(true)
                .build(),
        MapReduceOptions.builder()
                .splitType(MapReduceOptions.SplitType.PAGE)
                .pagesPerChunk(2)
                .build());

JsonNode result = client.waitUntilReadyJson(
        jobGuid,
        WaitOptions.builder()
                .timeout(Duration.ofSeconds(180))
                .pollInterval(Duration.ofSeconds(5))
                .build());
System.out.println(result.toPrettyString());
```

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
