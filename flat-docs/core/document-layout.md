---
title: Document Layout
description: Read page geometry, words, paragraphs, tables, and OCR quality for processed documents.
---
<!-- Generated from docs/core/document-layout.md. Do not edit by hand. -->

# Document Layout

Every document processed with DocuDevs OCR has a layout: the recognized text plus where each word, paragraph, table, and figure sits on the page. Use it to draw highlights over the original document, build search-in-document, or resolve the `sourceRefs` returned by source locations.

The layout model is provider-neutral. Field names follow common OCR conventions (Azure Document Intelligence naming) so existing mapping code needs minimal changes.

## Endpoints

| Endpoint | Returns |
| --- | --- |
| `GET /job/result/{guid}/layout` | Document layout |
| `GET /job/result/{guid}/ocr-quality` | OCR quality summary |

Query parameters for `/layout`:

| Parameter | Example | Meaning |
| --- | --- | --- |
| `batchIndex` | `2` | Layout of one batch item (zero or greater) |
| `pages` | `1-3,7` | Only pages 1, 2, 3, and 7 (1-based) |
| `include` | `words,tables` | Only these parts: `content`, `words`, `paragraphs`, `tables`, `figures`, `sections` |

`/ocr-quality` accepts `batchIndex` only.

`pages[]` metadata is always returned. Word ids are assigned before filtering, so they stay the same regardless of `pages` or `include`.

Status codes:

- `400`: malformed `pages` or `include`, or a negative `batchIndex`.
- `404`: the job does not exist, or it has no layout or quality artifact (for example, the job is still processing or was not processed with OCR).
- `500`: the stored artifact exists but cannot be read.

## Reading the layout

The example fetches the words and paragraphs of page 1 and the OCR quality summary of a completed job. In Python, `get_document_layout` and `get_ocr_quality` return `None` when the artifact does not exist; over HTTP that case is the `404` above.

**cURL**

```bash
JOB_GUID="<job-guid>"  # GUID of a completed job that was processed with OCR

# Only page 1, and only its words and paragraphs
curl -s -S -f "https://api.docudevs.ai/job/result/$JOB_GUID/layout?pages=1&include=words,paragraphs" \
  -H "Authorization: $API_KEY" | jq .

curl -s -S -f "https://api.docudevs.ai/job/result/$JOB_GUID/ocr-quality" \
  -H "Authorization: $API_KEY" | jq .
```

**Python**

```python
job_guid = "<job-guid>"  # GUID of a completed job that was processed with OCR

# Only page 1, and only its words and paragraphs
layout = await client.get_document_layout(job_guid, pages="1", include=["words", "paragraphs"])
quality = await client.get_ocr_quality(job_guid)

# Both methods return None when the artifact does not exist
print(json.dumps(layout, indent=2))
print(json.dumps(quality, indent=2))
```

## Layout model

```json
{
  "version": 1,
  "provider": "azure-document-intelligence",
  "contentFormat": "markdown",
  "contentLength": 48213,
  "content": "...",
  "pages": [
    {
      "pageNumber": 1, "width": 8.5, "height": 11, "unit": "inch",
      "spans": [{ "offset": 0, "length": 2310 }],
      "words": [
        { "id": "/pages/0/words/0", "content": "Invoice", "confidence": 0.99,
          "span": { "offset": 0, "length": 7 },
          "polygon": [{ "x": 1.0, "y": 0.5 }, { "x": 1.6, "y": 0.5 }, { "x": 1.6, "y": 0.7 }, { "x": 1.0, "y": 0.7 }] }
      ]
    }
  ],
  "paragraphs": [
    { "id": "/paragraphs/0", "role": "title", "content": "Invoice",
      "spans": [{ "offset": 0, "length": 7 }],
      "boundingRegions": [{ "pageNumber": 1, "polygon": [{ "x": 1.0, "y": 0.5 }] }] }
  ],
  "tables": [
    { "id": "/tables/0", "rowCount": 4, "columnCount": 3,
      "cells": [{ "rowIndex": 0, "columnIndex": 0, "kind": "columnHeader", "content": "Item", "elements": ["/paragraphs/5"] }] }
  ],
  "figures": [{ "id": "/figures/0", "boundingRegions": [], "elements": [], "captionRef": "/paragraphs/9" }],
  "sections": [{ "id": "/sections/0", "elements": ["/paragraphs/0", "/tables/0"] }]
}
```

- `contentFormat` is `markdown` or `text`, depending on the OCR output format used for the job.
- Coordinates are in the page's `unit`, as reported by the OCR provider. Use `width` and `height` to scale when drawing on a rendered page.
- `span.offset` always refers to the full-document `content`, even when you filter pages or parts.
- Empty arrays and fields the provider did not return are omitted; treat a missing array as empty.
- Element ids are stable references. Word ids are DocuDevs identifiers (`/pages/{pageIndex}/words/{wordIndex}`, zero-based) and are not guaranteed to equal Azure Document Intelligence word indices.
- With `pages`, tables spanning several pages are returned whole, and `sections[].elements` may reference elements outside the selection.

## Resolving source locations

`GET /job/result/{guid}/source-locations` returns a JSON document with a `locations` map. Each key is a pointer into the extracted result, and each value has a `sourceRefs` list such as `/paragraphs/12` or `/tables/0`. The `resolution` and `pageAnchors` fields describe how the location was matched.

To look up paragraph text for a reference, fetch the paragraphs from the layout and match them by `id`:

**cURL**

```bash
JOB_GUID="<job-guid>"  # GUID of a completed job that was processed with sourceLocations enabled

curl -s -S -f "https://api.docudevs.ai/job/result/$JOB_GUID/source-locations" \
  -H "Authorization: $API_KEY" -o source-locations.json

curl -s -S -f "https://api.docudevs.ai/job/result/$JOB_GUID/layout?include=paragraphs" \
  -H "Authorization: $API_KEY" -o layout.json

# Print each pointer, its source reference and the paragraph text for that reference.
# References to tables or figures print an empty string.
jq -r --slurpfile layout layout.json '
  (($layout[0].paragraphs // []) | map({(.id): .content}) | add // {}) as $text
  | (.locations // {}) | to_entries[]
  | .key as $pointer
  | (.value.sourceRefs // [])[]
  | "\($pointer) \(.) \($text[.] // "")"
' source-locations.json
```

**Python**

```python
job_guid = "<job-guid>"  # GUID of a completed job that was processed with source_locations enabled

sources = await client.get_source_locations(job_guid)
layout = await client.get_document_layout(job_guid, include=["paragraphs"])

# Print each pointer, its source reference and the paragraph text for that reference.
# References to tables or figures print an empty string.
if sources and layout:
    paragraph_text = {p["id"]: p["content"] for p in layout.get("paragraphs", [])}
    for pointer, location in sources.get("locations", {}).items():
        for ref in location.get("sourceRefs", []):
            print(pointer, ref, paragraph_text.get(ref, ""))
```

References to tables or figures are not among the paragraphs, so they print an empty string. Fetch `tables` or `figures` from the layout to resolve those.

## Figures and page images

The layout lists where each figure sits on a page. To get the figures themselves as images, switch figure extraction on in the processing request:

- `extractFigures: true` keeps every figure of the document as a PNG image, together with a list that describes them.
- `describeFigures: true` does the same and also asks a model to describe each figure. The description is added to the markdown text as a `FigureDescription` comment inside the figure's `<figure>` element, so the extraction that reads the text sees it. `describeFigures` implies `extractFigures`. Use markdown output with it; the Python client rejects `describe_figures` together with plain output.

Both options are off unless you set them. They are fields of the process request and of the OCR request, and the Python client takes them as `extract_figures` and `describe_figures`. In the CLI, `docudevs process --describe-figures` turns them on; there is no CLI flag for extraction alone. Figures are read by the document OCR, so use `ocr` set to `DEFAULT`, `PREMIUM` or `AUTO`; `LOW` and `EXCEL` do not extract figures.

| Route | Returns |
| --- | --- |
| `GET /job/figures/{guid}` | The list of extracted figures |
| `GET /job/figure/{guid}/{imageId}` | One figure as a PNG image |
| `GET /job/image/{guid}/{pageIndex}` | One page as a PNG image (page indexes start at 0) |

The two figure routes accept `batchIndex` to read the figures of one document of a batch. `{imageId}` is the `id` from the list; a trailing `.png` is optional.

The list looks like this:

```json
{
  "version": 1,
  "images": [
    {
      "id": "figure_0",
      "figureId": "1.1",
      "pageIndex": 0,
      "boundingPolygon": [{ "x": 1.0, "y": 2.0 }, { "x": 4.0, "y": 2.0 }, { "x": 4.0, "y": 5.0 }, { "x": 1.0, "y": 5.0 }],
      "caption": "Revenue by quarter",
      "mimeType": "image/png",
      "width": 640,
      "height": 480
    }
  ]
}
```

`id` names the image, in the order of the figures. `figureId` is the identifier of the figure in the OCR output. `pageIndex` is the page that holds the figure, counted from 0 like the page image route. `caption`, `pageIndex` and `boundingPolygon` can be empty when the OCR did not return them, so read them as optional.

A job that was processed with figure extraction on answers the list with `200`, and an empty `images` list when no figures were extracted, for example because the document has none. `404` means that the job does not exist or was processed without figure extraction. A missing image also answers `404`.

Page images are rendered for PDFs processed with `PREMIUM` OCR, including `AUTO` when it moves up to `PREMIUM`. Without page images the route answers `404`. [LLM Tracing](https://docs.docudevs.ai/docs/advanced/tracing#retrieving-page-images) shows the page image call on its own.

The example processes a PDF with figure extraction on, waits for the job, lists the figures, and downloads every figure and the first page image. cURL and Python poll the job; both stop after 600 seconds.

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

JOB_ID=$(curl -s -S -f -X POST "https://api.docudevs.ai/document/upload" \
  -H "Authorization: $API_KEY" \
  -F "document=@report.pdf" | jq -r '.guid')

# extractFigures keeps the figures as images; PREMIUM OCR also renders page images
curl -s -S -f -o /dev/null -X POST "https://api.docudevs.ai/document/ocr/$JOB_ID?format=markdown" \
  -H "Authorization: $API_KEY" \
  -H "Content-Type: application/json" \
  -d '{
    "ocr": "PREMIUM",
    "mimeType": "application/pdf",
    "extractFigures": true
  }'

wait_for_job "$JOB_ID"

# List the figures, then download each one
curl -s -S -f "https://api.docudevs.ai/job/figures/$JOB_ID" \
  -H "Authorization: $API_KEY" \
  | jq -r '(.images // [])[] | "\(.id) page \(.pageIndex // "unknown")"'

curl -s -S -f "https://api.docudevs.ai/job/figures/$JOB_ID" \
  -H "Authorization: $API_KEY" \
  | jq -r '(.images // [])[].id' \
  | while read -r FIGURE_ID; do
      curl -s -S -f "https://api.docudevs.ai/job/figure/$JOB_ID/$FIGURE_ID" \
        -H "Authorization: $API_KEY" \
        --output "$FIGURE_ID.png"
    done

# Download the image of the first page (page indexes start at 0)
curl -s -S -f "https://api.docudevs.ai/job/image/$JOB_ID/0" \
  -H "Authorization: $API_KEY" \
  --output page_0.png
```

**Python**

```python
with open("report.pdf", "rb") as f:
    document_data = f.read()

# extract_figures keeps the figures as images; PREMIUM OCR also renders page images
job_id = await client.submit_and_ocr_document(
    document=document_data,
    document_mime_type="application/pdf",
    ocr="PREMIUM",
    ocr_format="markdown",
    extract_figures=True,
)
await client.wait_until_ready(job_id, timeout=600)

# List the figures, then download each one
metadata = await client.get_figures_metadata(job_id)
images = (metadata or {}).get("images", [])
for image in images:
    page_index = image.get("pageIndex")
    print(f"{image['id']} page {page_index if page_index is not None else 'unknown'}")

for image in images:
    figure_bytes = await client.get_figure_image(job_id, image["id"])
    if figure_bytes:
        with open(f"{image['id']}.png", "wb") as f:
            f.write(figure_bytes)

# Download the image of the first page (page indexes start at 0)
page_bytes = await client.get_image(job_id, 0)
if page_bytes:
    with open("page_0.png", "wb") as f:
        f.write(page_bytes)
```

## Migrating from Azure Document Intelligence `AnalyzeResult`

| Azure Document Intelligence | DocuDevs layout |
| --- | --- |
| `content` | `content` |
| `pages[].pageNumber/width/height/unit/spans` | same |
| `pages[].words[].content/span/confidence` | same, plus `id` |
| `pages[].words[].polygon` (`[x1, y1, x2, y2, ...]`) | `polygon` (`[{x, y}, ...]`) |
| `paragraphs[].role/content/spans/boundingRegions` | same, plus `id` |
| `tables[].rowCount/columnCount/cells/spans/boundingRegions` | same, plus `id` |
| `figures[]`, `sections[]` | same element-reference style, plus `id` |
| `pages[].lines`, `selectionMarks`, `styles`, `keyValuePairs`, `languages` | not provided |

To convert a DocuDevs polygon back to the Azure flat form:

```python
flat = [coordinate for point in polygon for coordinate in (point["x"], point["y"])]
```

## OCR quality

```json
{
  "version": 1,
  "provider": "azure-document-intelligence",
  "requestedOcrMode": "AUTO",
  "effectiveOcrMode": "PREMIUM",
  "escalatedToPremium": true,
  "qualityScore": 0.71,
  "qualityCategory": "Likely Handwriting Problems",
  "wordCount": 1843,
  "confidenceDetails": { "meanTrimmed": 0.71, "highConfidenceRatio": 0.62, "midConfidenceRatio": 0.30, "lowConfidenceRatio": 0.08 },
  "rationale": "Potential handwriting detected (low confidence: 8.0%)",
  "warnings": []
}
```

`qualityCategory` is one of `Very Confident`, `Confident`, `Likely Handwriting Problems`, `Many Problems`, `No Confidence Data`.
