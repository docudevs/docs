---
title: Redline (document comparison)
description: Compare two PDF versions of a document, review the changes, and export a redline to PDF, Word or Excel.
---
<!-- Generated from docs/core/document-compare.md. Do not edit by hand. -->

# Redline (document comparison)

A redline compares two versions of a document, for example the original contract and a new draft, and returns every change between them. It compares the documents themselves, so it works on two clean PDFs, a scan, or a redline someone else made. It does not read Word tracked changes; to read those, see [Track changes](https://docs.docudevs.ai/docs/core/track-changes).

A comparison runs as one job. It OCRs both documents, builds the structure (outline) of both, and then compares them. The result is a document-compare artifact that you can render, filter and export.

Only PDF is accepted. Any other file is rejected.

## Billing

A comparison is billed as its parts: OCR of both documents plus the document outline of both, the same as running those operations separately. The comparison step and exports add no charge.

## Stages

Each comparison reports these stages, per document where it applies:

| Stage | Side | Meaning |
| --- | --- | --- |
| `ocr` | `original`, `revised` | Reads each document. |
| `outline` | `original`, `revised` | Builds the clause structure of each document. |
| `compare` | none | Compares the two structures. |

The overall status is `PENDING`, `PROCESSING`, `COMPLETED` or `ERROR`. When a stage fails, the overall status is `ERROR` and that stage carries an `error` message.

## Compare two documents and export a redline

The example submits two PDFs, waits for the comparison, and exports a PDF redline that shows only the substantive changes. The cURL variant polls `GET /compare/{compareGuid}` and the export status route, then downloads the export file.

**cURL**

```bash
wait_for_status() {
  local status_url="$1"
  local deadline=$((SECONDS + 1200))  # same limit as the Python timeout
  while [ "$SECONDS" -lt "$deadline" ]; do
    STATUS=$(curl "$status_url" -s -S -f -H "Authorization: $API_KEY" | jq -r '.status')
    case "$STATUS" in
      COMPLETED) return 0 ;;
      ERROR) echo "$status_url ended with status ERROR" >&2; return 1 ;;
    esac
    sleep 5
  done
  echo "$status_url did not complete in time" >&2
  return 1
}

COMPARE_GUID=$(curl -s -S -f -X POST "https://api.docudevs.ai/compare" \
  -H "Authorization: $API_KEY" \
  -F "original=@slip-2025.pdf" \
  -F "revised=@slip-2026-draft.pdf" \
  -F 'metadata={"originalLabel": "Slip 2025", "revisedLabel": "Draft 2026", "ocr": "PREMIUM"}' | jq -r '.compareGuid')

wait_for_status "https://api.docudevs.ai/compare/$COMPARE_GUID"

CHANGES=$(curl -s -S -f "https://api.docudevs.ai/job/result/$COMPARE_GUID/document-compare" \
  -H "Authorization: $API_KEY" | jq '.changes | length')
echo "Changes found: $CHANGES"

# Export a redline as PDF with only the substantive changes
EXPORT_GUID=$(curl -s -S -f -X POST "https://api.docudevs.ai/compare/$COMPARE_GUID/exports" \
  -H "Authorization: $API_KEY" \
  -H "Content-Type: application/json" \
  -d '{"format": "pdf", "filters": {"substantiveOnly": true}}' | jq -r '.exportGuid')

wait_for_status "https://api.docudevs.ai/compare/$COMPARE_GUID/exports/$EXPORT_GUID"

curl -s -S -f -o redline.pdf "https://api.docudevs.ai/compare/$COMPARE_GUID/exports/$EXPORT_GUID/file" \
  -H "Authorization: $API_KEY"
```

**Python**

```python
compare_guid = await client.compare_documents(
    "slip-2025.pdf",
    "slip-2026-draft.pdf",
    original_label="Slip 2025",
    revised_label="Draft 2026",
    ocr="PREMIUM",
)

# Raises an error that names the stage if a stage fails, and TimeoutError after 1200 seconds
artifact = await client.wait_until_compare_ready(compare_guid, timeout=1200)
print(f"Changes found: {len(artifact.get('changes', []))}")

# Export a redline as PDF with only the substantive changes
pdf_bytes = await client.export_document_compare(
    compare_guid,
    "pdf",
    filters={"substantiveOnly": True},
    save_to="redline.pdf",
)
print(f"Saved {len(pdf_bytes)} bytes to redline.pdf")
```

**Java**

```java
import ai.docudevs.client.UploadRequest;
import ai.docudevs.client.WaitOptions;
import com.fasterxml.jackson.databind.JsonNode;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.util.Map;

String compareGuid = client.compareDocuments(
        new UploadRequest("slip-2025.pdf", "application/pdf", Files.readAllBytes(Path.of("slip-2025.pdf"))),
        new UploadRequest("slip-2026-draft.pdf", "application/pdf",
                Files.readAllBytes(Path.of("slip-2026-draft.pdf"))),
        "PREMIUM",
        "Slip 2025",
        "Draft 2026");

// Throws DocuDevsApiException with the failed stage as its body if a stage fails,
// and DocuDevsTimeoutException after 1200 seconds
JsonNode artifact = client.waitUntilCompareReady(
        compareGuid,
        WaitOptions.builder()
                .timeout(Duration.ofSeconds(1200))
                .pollInterval(Duration.ofSeconds(5))
                .build());
System.out.println("Changes found: " + artifact.path("changes").size());

// Export a redline as PDF with only the substantive changes
byte[] pdfBytes = client.exportDocumentCompare(
        compareGuid,
        "pdf",
        Map.of("substantiveOnly", true),
        WaitOptions.builder()
                .timeout(Duration.ofSeconds(300))
                .pollInterval(Duration.ofSeconds(1))
                .build());
Files.write(Path.of("redline.pdf"), pdfBytes);
System.out.println("Saved " + pdfBytes.length + " bytes to redline.pdf");
```

Python client helpers:

- `compare_documents` accepts a path, bytes or a file-like object for each document. It returns the compare GUID.
- `ocr` is `DEFAULT` or `PREMIUM` and applies to both documents. The default is `PREMIUM`.
- `wait_until_compare_ready` polls until the comparison completes and returns the artifact. If a stage fails, it raises an error that names the stage, the document side and the error. It raises `TimeoutError` if the comparison does not finish in time.
- `get_document_compare(compare_guid)` returns the artifact of a completed comparison without polling.
- `export_document_compare(compare_guid, format, ...)` starts an export, waits for it, and returns the file bytes. `format` is `pdf`, `docx` or `xlsx`. Pass `save_to` to also write the file.

## REST API

The routes behind the example are listed here. Upload the two documents with a multipart request:

```text
POST /compare
Content-Type: multipart/form-data

original = old.pdf        (PDF)
revised  = new.pdf        (PDF)
metadata = {"originalLabel": "Slip 2025", "revisedLabel": "Draft 2026", "ocr": "PREMIUM"}   (optional)
```

`metadata` is optional. It can be sent as a text part or as a JSON file part. All of its fields are optional: `originalLabel`, `revisedLabel`, and `ocr` (`DEFAULT` or `PREMIUM`). The response is `202 Accepted`:

```json
{ "compareGuid": "11111111-2222-3333-4444-555555555555" }
```

Check progress:

```text
GET /compare/{compareGuid}
```

The response has `status`, `documents` (file name and label per side), `stages` and `createdAt`.

List recent comparisons, newest first:

```text
GET /compare?limit=20
```

`limit` defaults to 20 and is capped at 100.

Read the artifact once the status is `COMPLETED`:

```text
GET /job/result/{compareGuid}/document-compare
```

The artifact is JSON, version 1. It holds the settings used, a summary, the clauses, the list of changes, the redline, the alignment between the two documents, coverage counts, ownership and any warnings. Each change has a kind, a severity, attributes, a `hiddenByDefault` flag, and its text and page boxes on each side.

## What is hidden by default

Noise kinds are hidden by default, but no change disappears silently. Each hidden change carries `hiddenByDefault: true` in the artifact, so a viewer can count it and show it. Exports hide noise unless you ask for it in `shownNoise`.

Noise kinds, hidden by default:

- `caseOnly`: only the letter case differs.
- `punctuationOnly`: only punctuation differs.
- `furniture`: headers, footers and page furniture.
- `possibleOcrDifference`: a difference that is likely an OCR artifact.
- `renumbered`: a clause or list number changed without a change in text.
- `layout`: a difference in layout only.

Substantive changes have a high severity. They are changes to amounts, dates, numbers and defined terms (`amountChanged`, `dateChanged`, `numberChanged`, `definedTermChanged`).

## Filters and exports

Export a redline in one of three formats:

```text
POST /compare/{compareGuid}/exports
```

```json
{
  "format": "docx",
  "filters": {
    "insertions": true,
    "deletions": true,
    "moves": true,
    "tables": true,
    "substantiveOnly": false,
    "shownNoise": ["renumbered"]
  }
}
```

- `format`: `pdf`, `docx` or `xlsx`.
- `filters` is optional. Its fields are `insertions`, `deletions`, `moves` and `tables` (all default to `true`), `substantiveOnly` (default `false`), and `shownNoise`, a list of the noise kinds above to show (default empty).

The response is `202 Accepted` with `{ "exportGuid": "..." }`. Poll the export, then download the file:

```text
GET /compare/{compareGuid}/exports/{exportGuid}        status: PENDING, PROCESSING, COMPLETED or ERROR
GET /compare/{compareGuid}/exports/{exportGuid}/file  the file, as an attachment
```

The file is named `redline-<first 8 characters of the compare GUID>.<format>`.

The exports use the same colors as the on-screen redline: deletions red and struck through, insertions blue and underlined, moves green.

- **PDF:** the redline as shown, with the changes in the filter.
- **Word (`docx`):** the new document rebuilt as text, with each change as a Word tracked change, so you can accept or reject in Word. The layout and fonts of the original are not reproduced.
- **Excel (`xlsx`):** one row per change with the clause path, kind, severity, attributes, original and new text, and the page on each side.

Exports reflect the filters in the request. Each export is a separate job, so a long comparison does not hit a request timeout.

## Errors

- `400 Bad Request`: a document is not a PDF, `metadata` is not a JSON object, `format` is not `pdf`, `docx` or `xlsx`, or `shownNoise` names an unknown kind.
- `404 Not Found`: the comparison or export does not exist in your organization.
- `409 Conflict`: an export was requested before the comparison completed.
- A failed stage sets the comparison to `ERROR`. The `stages` entry names the failing stage, side and error.
