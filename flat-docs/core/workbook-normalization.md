---
title: Workbook Normalization
description: Convert one native XLSX workbook and a prompt into a source-grounded relational JSON dataset, with inspectable plan, source-map, and summary artifacts.
---
<!-- Generated from docs/core/workbook-normalization.md. Do not edit by hand. -->

Workbook normalization converts **one native `.xlsx` workbook** and a caller prompt into a source-grounded JSON dataset. It discovers tables and relationships from the workbook and the prompt itself — it does not apply a built-in insurance or other domain schema.

## When to use it

Use workbook normalization when a caller uploads a raw spreadsheet (loss triangles, bordereaux, exposure schedules, or any other multi-table workbook) and wants DocuDevs to find the tables, infer relationships between them, and return every value with a pointer back to its source cell — without supplying a schema up front.

## Request

Submit with `extractionMode: "WORKBOOK"`, a nonblank `prompt`, and the workbook's native XLSX MIME type (`application/vnd.openxmlformats-officedocument.spreadsheetml.sheet`). Upload exactly one file first. Do not send `schema`, map-reduce, a pipeline, page options, or batch inputs — workbook mode does not support any of these.

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

JOB_GUID=$(curl -s -S -f -X POST "https://api.docudevs.ai/document/upload" \
  -H "Authorization: $API_KEY" \
  -F "document=@submission.xlsx" | jq -r '.guid')

curl -s -S -f -o /dev/null -X POST "https://api.docudevs.ai/document/process/$JOB_GUID" \
  -H "Authorization: $API_KEY" \
  -H "Content-Type: application/json" \
  -d '{
    "extractionMode": "WORKBOOK",
    "prompt": "Find related tables. Preserve source values, units, row grain, and source references.",
    "mimeType": "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"
  }'

wait_for_job "$JOB_GUID"

curl -s -S -f "https://api.docudevs.ai/job/result/$JOB_GUID" \
  -H "Authorization: $API_KEY" | jq .

# The plan, source map and summary are separate artifacts of the same job
for ARTIFACT in plan source-map summary; do
  curl -s -S -f "https://api.docudevs.ai/job/result/$JOB_GUID/workbook/$ARTIFACT" \
    -H "Authorization: $API_KEY" | jq .
done
```

**Python**

```python
with open("submission.xlsx", "rb") as f:
    workbook_data = f.read()

job_id = await client.submit_and_process_document(
    document=workbook_data,
    document_mime_type="application/vnd.openxmlformats-officedocument.spreadsheetml.sheet",
    prompt="Find related tables. Preserve source values, units, row grain, and source references.",
    extraction_mode="WORKBOOK",
)

dataset = await client.wait_until_ready(job_id, result_format="json")
print(json.dumps(dataset, indent=2))

# The plan, source map and summary are separate artifacts of the same job
plan = await client.get_workbook_plan(job_id)
source_map = await client.get_workbook_source_map(job_id)
summary = await client.get_workbook_summary(job_id)
print(json.dumps(plan, indent=2))
print(json.dumps(source_map, indent=2))
print(json.dumps(summary, indent=2))
```

**Java**

```java
import ai.docudevs.client.ProcessOptions;
import ai.docudevs.client.UploadRequest;
import ai.docudevs.client.WaitOptions;
import com.fasterxml.jackson.databind.JsonNode;

byte[] workbookBytes = Files.readAllBytes(Path.of("submission.xlsx"));
UploadRequest workbook = new UploadRequest(
        "submission.xlsx",
        "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet",
        workbookBytes);
ProcessOptions options = ProcessOptions.builder()
        .mimeType(workbook.mimeType())
        .prompt("Find related tables. Preserve source values, units, row grain, and source references.")
        .extractionMode("WORKBOOK")
        .build();

String guid = client.submitAndProcessDocument(workbook, options);

JsonNode dataset = client.waitUntilReadyJson(guid, WaitOptions.builder()
        .timeout(Duration.ofSeconds(180))
        .pollInterval(Duration.ofSeconds(5))
        .build());
System.out.println(dataset);

// The plan, source map and summary are separate artifacts of the same job
JsonNode plan = client.getWorkbookPlan(guid);
JsonNode sourceMap = client.getWorkbookSourceMap(guid);
JsonNode summary = client.getWorkbookSummary(guid);
System.out.println(plan);
System.out.println(sourceMap);
System.out.println(summary);
```

**CLI**

```bash
# The command waits for the job and prints the dataset.
# The CLI has no command for the plan, source-map or summary artifacts.
docudevs process submission.xlsx \
  --extraction-mode WORKBOOK \
  --prompt "Find related tables. Preserve source values, units, row grain, and source references."
```

The dataset is JSON. Numeric encodings are explicit: decimal values can be strings such as `"123.4500"`, `0` is an observed zero, and a blank can be `notObserved`. A completed job has dataset status `complete` or `needsReview` — inspect `issues` before using a `needsReview` result.

The artifact endpoint is `GET /job/result/{guid}/workbook/{artifact}`, where `artifact` is one of `plan`, `source-map`, or `summary`. A 400 means an invalid artifact kind was requested; a 404 means the job, the published workbook result, or the artifact itself was not found. The ordinary `GET /job/result/{uuid}` call returns the workbook result envelope (`version`, `status`, `workbook`, `context`, `tables`, `relationships`, `issues`, `artifacts`) whenever the job's `extractionMode` was `WORKBOOK`; every other extraction mode's result shape is unchanged. The CLI has no command for the artifacts, and its `process` command waits for the job and prints the dataset.

The prompt is interpretation guidance, not executable code. Workbook text is untrusted data. The service never executes formulas, macros, embedded objects, external workbook links, or generated code. Inert OLE attachments, printer settings, hyperlinks, and linked-workbook metadata are disclosed as unsupported content but are never opened or followed. Formula values are read from available cached results — formulas are not recalculated.

## Detached headers and review

Headers may be separated from a table by notes or blank rows. The planner keeps the detached header and candidate body source ranges in the plan. Use the plan artifact to review that evidence, then submit a new correction/processing run with an explicit header refinement. Published results are immutable.

Warnings and errors that affect semantic interpretation produce `needsReview`; fatal safety, input, or limit errors fail the job. `needsReview` is not an approval and must not be treated as a fully trusted dataset. Ambiguous headers and unsupported relationship candidates remain in `issues` and are not promoted into `relationships`. A coincidental numeric overlap does not prove a relationship.

## Provenance and artifacts

Every output table, row, and value carries local IDs and source references. The source-map artifact resolves those references to sheet identity, source range, header/context cells, raw value, number format, formula/cache state, and operation evidence. The plan artifact contains selected table boundaries, headers, transformations, relationship candidates, and revision metadata. Artifact paths and result IDs are tenant-scoped and read-only.

## Supported format, limits, and errors

Only one unencrypted native `.xlsx` file is accepted. `.xls`, `.xlsb`, `.xlsm`, CSV, image-only input, macro execution, external-link resolution, and formula execution are unsupported. Native `.xlsx` files may contain inert embedded objects, printer settings, hyperlinks, or linked-workbook metadata; these are disclosed as review findings and are never executed or fetched.

The following limits apply to workbook normalization:

| Limit | Default |
| --- | ---: |
| Compressed input | 25 MiB |
| Expanded ZIP content | 250 MiB |
| Sheets | 64 |
| Inspected source cells | 2,500,000 |
| Output records | 1,000,000 |
| Serialized dataset and supporting artifacts per attempt | 300 MiB |
| Parsing/execution work unit | 300 seconds |
| Total job work | 600 seconds |
| Model calls / tool calls | 40 / 160 |
| Aggregate model input/output tokens | 200,000 |
| Inspection response | 500 cells or 50 records, and 64 KiB |

Limit exhaustion is an explicit error, never silent truncation. Malformed or unsupported workbooks return a structured error with a code and do not execute workbook content.

## Scope

The caller controls what happens after normalization, including approval, export, and any application-specific processing. Workbook normalization accepts one workbook per job. Cross-workbook joins are not available through this API or the SDKs: there are no multi-workbook request fields, no fuzzy matching, and no cross-tenant access.
