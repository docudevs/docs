---
title: PDF AcroForm Operation
description: Convert normal PDFs into fillable AcroForm PDFs with blank data-entry fields.
---

# PDF AcroForm Operation

The `pdf-acroform` operation converts a completed PDF job into a fillable AcroForm PDF. It uses page images to detect blank data-entry areas, writes blank PDF widgets onto a copy of the original PDF, and stores the generated PDF with the operation job.

Field names use the source document language and ASCII `snake_case`. Labels are not translated.

## Convert a PDF

Run the operation on a completed PDF job and wait for the generated PDF. The parent job needs rendered page images, so process it with `ocr` set to `PREMIUM` or set `forceOcr` on the operation.

<Example name="operations/convert-pdf-to-acroform" />

To submit without waiting:

<Example name="operations/submit-pdf-acroform" />

To download a generated PDF later:

<Example name="operations/download-acroform-pdf" />

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
