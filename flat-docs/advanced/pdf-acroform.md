---
title: PDF AcroForm Operation
description: Convert normal PDFs into fillable AcroForm PDFs with blank data-entry fields.
---
<!-- Generated from docs/advanced/pdf-acroform.md. Do not edit by hand. -->

# PDF AcroForm Operation

The `pdf-acroform` operation converts a completed PDF job into a fillable AcroForm PDF. It uses page images to detect blank data-entry areas, writes blank PDF widgets onto a copy of the original PDF, and stores the generated PDF with the operation job.

Field names use the source document language and ASCII `snake_case`. Labels are not translated.

## Convert a PDF

Run the operation on a completed PDF job and wait for the generated PDF. The parent job needs rendered page images, so process it with `ocr` set to `PREMIUM` or set `forceOcr` on the operation.

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

PARENT_JOB_GUID="<job-guid>"  # GUID of a completed PDF job whose page images exist (processed with ocr PREMIUM)

OPERATION_JOB_GUID=$(curl -s -S -f -X POST "https://api.docudevs.ai/operation" \
  -H "Authorization: $API_KEY" \
  -H "Content-Type: application/json" \
  -d "{
    \"jobGuid\": \"$PARENT_JOB_GUID\",
    \"type\": \"pdf-acroform\",
    \"parameters\": {
      \"llmType\": \"DEFAULT\",
      \"customParameters\": {
        \"ocr\": \"PREMIUM\",
        \"fieldNameStyle\": \"source_snake_case\",
        \"pageRange\": [1],
        \"minConfidence\": 0.6,
        \"maxFieldsPerPage\": 300
      }
    }
  }" | jq -r '.jobGuid')

wait_for_job "$OPERATION_JOB_GUID"

curl -s -S -f "https://api.docudevs.ai/job/result/$OPERATION_JOB_GUID/pdf-acroform" \
  -H "Authorization: $API_KEY" \
  --output fillable.pdf

echo "Operation job GUID: $OPERATION_JOB_GUID"
```

**Python**

```python
import asyncio
import os

from docudevs.docudevs_client import DocuDevsClient


async def main():
    client = DocuDevsClient(token=os.environ["API_KEY"])

    parent_job_guid = "<job-guid>"  # GUID of a completed PDF job whose page images exist (processed with ocr PREMIUM)

    result = await client.submit_and_wait_for_pdf_acroform_operation(
        parent_job_guid,
        ocr="PREMIUM",
        page_range=[1],
        min_confidence=0.6,
        max_fields_per_page=300,
        timeout=600,
        save_to="fillable.pdf",
    )

    print(f"Operation job GUID: {result.operation_job_guid}")


asyncio.run(main())
```

**CLI**

```bash
PARENT_JOB_GUID="<job-guid>"  # GUID of a completed PDF job whose page images exist (processed with ocr PREMIUM)

docudevs operations pdf-acroform "$PARENT_JOB_GUID" \
  --ocr PREMIUM \
  --page 1 \
  --min-confidence 0.6 \
  --max-fields-per-page 300 \
  --timeout 600 \
  --output fillable.pdf
```

To submit without waiting:

**cURL**

```bash
PARENT_JOB_GUID="<job-guid>"  # GUID of a completed PDF job whose page images exist (processed with ocr PREMIUM)

curl -s -S -f -X POST "https://api.docudevs.ai/operation" \
  -H "Authorization: $API_KEY" \
  -H "Content-Type: application/json" \
  -d "{
    \"jobGuid\": \"$PARENT_JOB_GUID\",
    \"type\": \"pdf-acroform\",
    \"parameters\": {
      \"llmType\": \"DEFAULT\",
      \"customParameters\": {
        \"ocr\": \"PREMIUM\",
        \"fieldNameStyle\": \"source_snake_case\"
      }
    }
  }" | jq '{jobGuid, status}'
```

**Python**

```python
parent_job_guid = "<job-guid>"  # GUID of a completed PDF job whose page images exist (processed with ocr PREMIUM)

response = await client.submit_pdf_acroform_operation(parent_job_guid)

print(f"Operation job GUID: {response.job_guid}")
print(f"Status: {response.status}")
```

**CLI**

```bash
PARENT_JOB_GUID="<job-guid>"  # GUID of a completed PDF job whose page images exist (processed with ocr PREMIUM)

docudevs operations pdf-acroform "$PARENT_JOB_GUID" --no-wait
```

To download a generated PDF later:

**cURL**

```bash
OPERATION_JOB_GUID="<operation-job-guid>"  # operation job GUID returned when the conversion was submitted

curl -s -S -f "https://api.docudevs.ai/job/result/$OPERATION_JOB_GUID/pdf-acroform" \
  -H "Authorization: $API_KEY" \
  --output fillable.pdf
```

**Python**

```python
operation_job_guid = "<operation-job-guid>"  # operation job GUID returned when the conversion was submitted

pdf_bytes = await client.get_pdf_acroform(operation_job_guid, save_to="fillable.pdf")
print(f"Saved fillable.pdf ({len(pdf_bytes)} bytes)")
```

Useful CLI options:

- `--page 1 --page 2`: process selected 1-based page numbers.
- `--min-confidence 0.65`: skip lower-confidence detections.
- `--force-ocr`: regenerate page images even if page images already exist.
- `--no-wait`: only submit the operation.

## API

Submit the operation with `POST /operation`:

```json
{
  "jobGuid": "parent-job-guid",
  "type": "pdf-acroform",
  "parameters": {
    "llmType": "DEFAULT",
    "customParameters": {
      "ocr": "PREMIUM",
      "fieldNameStyle": "source_snake_case",
      "minConfidence": 0.6
    }
  }
}
```

Download the generated PDF:

```text
GET /job/result/{operationJobGuid}/pdf-acroform
```

To apply edited field definitions, use `POST
/job/{parentGuid}/pdf-acroform/apply`. It returns a
`SubmitOperationResponse`. HTTP 409 means the operation job is bound to a
review run. HTTP 503 means the service is busy; retry after a short delay.

The response content type is `application/pdf`.

## Parameters

- `ocr`: OCR mode used when page images are missing. Default: `PREMIUM`.
- `forceOcr`: regenerate page images. Default: `false`.
- `pageRange`: selected 1-based page numbers.
- `fieldNameStyle`: currently `source_snake_case`.
- `minConfidence`: minimum accepted detection confidence. Default: `0.6`.
- `maxFieldsPerPage`: maximum number of fields generated per page. Default: `200`.
