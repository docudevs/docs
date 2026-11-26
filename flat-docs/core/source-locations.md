---
title: Source Locations
description: Return page numbers and bounding boxes for extracted structured fields.
sidebar_position: 9
---
<!-- Generated from docs/core/source-locations.md. Do not edit by hand. -->

# Source Locations

DocuDevs can return page-aware evidence for structured extraction results.

Use this when you want the extracted value and the location on the document page where it came from.

If you want the model itself to return visible-object bounding boxes inside an image, see [Object Detection](./object-detection.md). Source locations are different: they are a separate evidence artifact keyed to extracted fields.

Typical examples:

- contract clauses with page numbers and bounding boxes
- insurance deductible options linked to the pricing table
- extracted fields that need human review in the jobs UI

## What It Returns

Source locations are returned as a separate artifact so the normal extracted result stays backward compatible.

- `GET /job/result/{guid}` returns the extracted JSON
- `GET /job/result/{guid}/source-locations` returns the evidence manifest

Each entry is keyed by a JSON Pointer into the extracted result.

Example:

```json
{
  "version": 1,
  "granularity": "block",
  "pages": [
    {
      "pageNumber": 1,
      "width": 8.5,
      "height": 11.0,
      "unit": "inch"
    }
  ],
  "locations": {
    "/basic_deductible_options": {
      "resolution": "block",
      "sourceRefs": ["/paragraphs/9", "/paragraphs/11"],
      "pageAnchors": [
        {
          "pageNumber": 1,
          "unit": "inch",
          "bbox": {
            "left": 1.58,
            "top": 3.91,
            "right": 5.39,
            "bottom": 5.06
          },
          "polygons": [
            [
              { "x": 1.58, "y": 3.91 },
              { "x": 5.39, "y": 3.91 },
              { "x": 5.39, "y": 5.06 },
              { "x": 1.58, "y": 5.06 }
            ]
          ]
        }
      ]
    }
  },
  "unresolved": []
}
```

## Supported Scope

Version 1 supports:

- structured extraction jobs
- `simple` extraction mode
- documents with stable page-aware text layout
- PDF and image inputs
- AcroForm-backed field refs when async PDF jobs request both `sourceLocations=true` and `acroFormMetadata=true`

Version 1 does not support:

- map-reduce extraction
- every OCR and spreadsheet workflow
- exact citation for free-form generative summaries

For synthesized outputs, treat source locations as supporting evidence, not as a single exact box for the final conclusion.

## Request Parameters

Enable the feature with:

- `sourceLocations=true`
- optional `sourceLocationGranularity`

Allowed `sourceLocationGranularity` values:

- `auto`
- `block`
- `word`

Behavior:

- `block`: returns layout-block geometry
- `word`: narrows to matched words when possible
- `auto`: prefers word matches and falls back to block geometry

For existing fillable PDFs, source locations remain an async-job feature. The direct `/document/acroform-metadata` endpoint returns form metadata only; it does not create OCR/layout artifacts or a job GUID.

When an async PDF job requests both `sourceLocations=true` and `acroFormMetadata=true`, the manifest can include AcroForm-backed evidence entries such as the one below. A reference of this form is the `id` of a field in the job's AcroForm metadata, which `GET /job/result/{guid}/acroform-metadata` returns; it is an identifier inside the artifact, not a route you call. Its page anchors are in PDF points:

```json
{
  "sourceRefs": ["/acroform/fields/0"],
  "acroFormRefs": ["/acroform/fields/0"],
  "resolution": "acroform-field",
  "pageAnchors": [
    {
      "pageNumber": 1,
      "unit": "point",
      "bbox": {
        "left": 94.7,
        "top": 652.4,
        "right": 215.0,
        "bottom": 667.9
      }
    }
  ]
}
```

## Requesting source locations

Send `sourceLocations: true` (Python `source_locations=True`) and, optionally, `sourceLocationGranularity` with the processing request. The example uploads a contract, extracts its clauses, and fetches the evidence artifact next to the extracted JSON. The Java SDK has no source-location option, and the CLI has no flag for it.

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
    "clauses": {"type": "array", "items": {"type": "string"}}
  },
  "required": ["clauses"]
}'

JOB_GUID=$(curl -s -S -f -X POST "https://api.docudevs.ai/document/upload" \
  -H "Authorization: $API_KEY" \
  -F "document=@contract.pdf" | jq -r '.guid')

jq -n \
  --arg schema "$SCHEMA" \
  '{
    mimeType: "application/pdf",
    prompt: "Extract the main clauses from this contract.",
    schema: $schema,
    sourceLocations: true,
    sourceLocationGranularity: "block"
  }' |
  curl -s -S -f -o /dev/null -X POST "https://api.docudevs.ai/document/process/$JOB_GUID" \
    -H "Authorization: $API_KEY" \
    -H "Content-Type: application/json" \
    --data-binary @-

wait_for_job "$JOB_GUID"

curl -s -S -f "https://api.docudevs.ai/job/result/$JOB_GUID/json" \
  -H "Authorization: $API_KEY" | jq .

# The evidence manifest is a separate artifact
curl -s -S -f "https://api.docudevs.ai/job/result/$JOB_GUID/source-locations" \
  -H "Authorization: $API_KEY" | jq .
```

**Python**

```python
from pydantic import BaseModel

from docudevs import json_schema

class ContractClauses(BaseModel):
    clauses: list[str]

with open("contract.pdf", "rb") as f:
    document_data = f.read()

job_id = await client.submit_and_process_document(
    document=document_data,
    document_mime_type="application/pdf",
    prompt="Extract the main clauses from this contract.",
    schema=json_schema(ContractClauses),
    source_locations=True,
    source_location_granularity="block",
)

result = await client.wait_until_ready(job_id, result_format="json")
print(json.dumps(result, indent=2))

# The evidence manifest is a separate artifact
source_locations = await client.get_source_locations(job_id)
print(json.dumps(source_locations, indent=2))
```

The Python client can also wait for the job and fetch both artifacts in one call. `wait_until_ready_with_source_locations(guid)` does that for a job you already submitted, and `submit_and_wait_for_document_with_source_locations(...)` does submit, wait and fetch. It takes the same arguments as `submit_and_process_document(...)` plus `timeout` and `poll_interval`:

**Python**

```python
from pydantic import BaseModel

from docudevs import json_schema

class ContractClauses(BaseModel):
    clauses: list[str]

with open("contract.pdf", "rb") as f:
    document_data = f.read()

combined = await client.submit_and_wait_for_document_with_source_locations(
    document=document_data,
    document_mime_type="application/pdf",
    prompt="Extract the main clauses from this contract.",
    schema=json_schema(ContractClauses),
    source_location_granularity="block",
    timeout=180,
)

print(json.dumps(combined.result, indent=2))
print(json.dumps(combined.source_locations, indent=2))
```

Both return an object with `result` and `source_locations`. The single-call helpers exist only in the Python client; every other language makes the same calls one at a time, as the first example shows.

## Jobs UI

When a completed structured job includes source locations, the jobs result inspector can show:

- evidence cards per extracted field
- page selectors
- page thumbnails
- bounding-box overlays on the selected page
- raw coordinate values for auditing

## Document Support Notes

Source locations work best when DocuDevs can determine reliable page geometry for the input document.

Important caveat:

- PDF and image inputs are best supported
- some editable formats may not provide consistent page-aware evidence boxes for every extracted value

## Recommended Usage

Prefer `block` granularity for longer fields such as clauses and summaries of a row or paragraph.

Prefer `auto` when you want better word-level precision but still want a successful fallback when exact word matching is not possible.

To resolve `sourceRefs` into page geometry and text, see [Document layout](./document-layout.md).
