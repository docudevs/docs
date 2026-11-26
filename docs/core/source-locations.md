---
title: Source Locations
description: Return page numbers and bounding boxes for extracted structured fields.
sidebar_position: 9
---

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

Send `sourceLocations: true` (Python `source_locations=True`) and, optionally, `sourceLocationGranularity` with the processing request. The example uploads a contract, extracts its clauses, and fetches the evidence artifact next to the extracted JSON. The Java variant uses the `sourceLocations` and `sourceLocationGranularity` options of `ProcessOptions`; the CLI has no flag for it.

<Example name="extraction/extract-with-source-locations" />

The Python client can also wait for the job and fetch both artifacts in one call. `wait_until_ready_with_source_locations(guid)` does that for a job you already submitted, and `submit_and_wait_for_document_with_source_locations(...)` does submit, wait and fetch. It takes the same arguments as `submit_and_process_document(...)` plus `timeout` and `poll_interval`:

<Example name="extraction/extract-with-source-locations-single-call" />

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
