---
title: Workbook Normalization
description: Convert one native XLSX workbook and a prompt into a source-grounded relational JSON dataset, with inspectable plan, source-map, and summary artifacts.
---

import Tabs from '@theme/Tabs';
import TabItem from '@theme/TabItem';

Workbook normalization converts **one native `.xlsx` workbook** and a caller prompt into a source-grounded JSON dataset. It discovers tables and relationships from the workbook and the prompt itself — it does not apply a built-in insurance or other domain schema.

## When to use it

Use workbook normalization when a caller uploads a raw spreadsheet (loss triangles, bordereaux, exposure schedules, or any other multi-table workbook) and wants DocuDevs to find the tables, infer relationships between them, and return every value with a pointer back to its source cell — without supplying a schema up front.

## Request

Submit with `extractionMode: "WORKBOOK"`, a nonblank `prompt`, and the workbook's native XLSX MIME type (`application/vnd.openxmlformats-officedocument.spreadsheetml.sheet`). Upload exactly one file first. Do not send `schema`, map-reduce, a pipeline, page options, or batch inputs — workbook mode does not support any of these.

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

client = DocuDevsClient(token=os.getenv('API_KEY'))

with open("submission.xlsx", "rb") as f:
    guid = await client.submit_and_process_document(
        f,
        "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet",
        prompt="Find related tables. Preserve source values, units, row grain, and source references.",
        extraction_mode="WORKBOOK",
    )

dataset = await client.wait_until_ready(guid, result_format="json")
plan = await client.get_workbook_plan(guid)
source_map = await client.get_workbook_source_map(guid)
summary = await client.get_workbook_summary(guid)
```

`dataset` is JSON. Numeric encodings are explicit: decimal values can be strings such as `"123.4500"`, `0` is an observed zero, and a blank can be `notObserved`. A completed job has dataset status `complete` or `needsReview` — inspect `issues` before using a `needsReview` result.

  </TabItem>
  <TabItem value="java">

```java
import ai.docudevs.client.DocuDevsClient;
import ai.docudevs.client.ProcessOptions;
import ai.docudevs.client.UploadRequest;
import ai.docudevs.client.WaitOptions;
import com.fasterxml.jackson.databind.JsonNode;

DocuDevsClient client = DocuDevsClient.builder()
    .apiKey(System.getenv("API_KEY"))
    .build();

byte[] workbookBytes = java.nio.file.Files.readAllBytes(java.nio.file.Path.of("submission.xlsx"));
UploadRequest workbook = new UploadRequest(
    "submission.xlsx",
    "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet",
    workbookBytes
);
ProcessOptions options = ProcessOptions.builder()
    .mimeType(workbook.mimeType())
    .prompt("Find related tables. Preserve source values, units, row grain, and source references.")
    .extractionMode("WORKBOOK")
    .build();

String guid = client.submitAndProcessDocument(workbook, options);
JsonNode dataset = client.waitUntilReadyJson(guid, WaitOptions.builder().build());
JsonNode plan = client.getWorkbookPlan(guid);
JsonNode sourceMap = client.getWorkbookSourceMap(guid);
JsonNode summary = client.getWorkbookSummary(guid);
```

  </TabItem>
  <TabItem value="cli">

```bash
docudevs process submission.xlsx \
  --extraction-mode WORKBOOK \
  --prompt 'Find related tables. Preserve source values, units, row grain, and source references.'

# Poll and retrieve JSON using the returned job GUID:
docudevs result JOB_GUID --format json
```

The CLI has no command for the `plan`, `source-map`, or `summary` artifacts — only the Python and Java SDKs expose those getters. Use the CLI's authenticated configuration for the API URL and token.

  </TabItem>
  <TabItem value="curl">

```bash
GUID=$(curl -s -X POST https://api.docudevs.ai/document/upload \
  -H "Authorization: $API_KEY" \
  -F "document=@submission.xlsx" | jq -r .guid)

curl -X POST "https://api.docudevs.ai/document/process/$GUID" \
  -H "Authorization: $API_KEY" \
  -H "Content-Type: application/json" \
  -d '{
    "extractionMode": "WORKBOOK",
    "prompt": "Find related tables. Preserve source values, units, row grain, and source references.",
    "mimeType": "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"
  }'

# Poll GET /job/result/$GUID until the job is no longer processing, then fetch the
# dataset and its supporting artifacts:
curl "https://api.docudevs.ai/job/result/$GUID" -H "Authorization: $API_KEY"
curl "https://api.docudevs.ai/job/result/$GUID/workbook/plan" -H "Authorization: $API_KEY"
curl "https://api.docudevs.ai/job/result/$GUID/workbook/source-map" -H "Authorization: $API_KEY"
curl "https://api.docudevs.ai/job/result/$GUID/workbook/summary" -H "Authorization: $API_KEY"
```

The artifact endpoint is `GET /job/result/{guid}/workbook/{artifact}`, where `artifact` is one of `plan`, `source-map`, or `summary`. A 400 means an invalid artifact kind was requested; a 404 means the job, the published workbook result, or the artifact itself was not found. The ordinary `GET /job/result/{uuid}` call returns the workbook result envelope (`version`, `status`, `workbook`, `context`, `tables`, `relationships`, `issues`, `artifacts`) whenever the job's `extractionMode` was `WORKBOOK`; every other extraction mode's result shape is unchanged.

  </TabItem>
</Tabs>

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

Large-workbook mode keeps source cells in a private, disk-backed store and does not materialize the full cell tuple or coordinate list in memory, so raising the inspected-cell ceiling does not require holding the whole workbook in memory.

Limit exhaustion is an explicit error, never silent truncation. Malformed or unsupported workbooks return a structured error with a code and do not execute workbook content.

## Scope and future work

The caller controls what happens after normalization, including approval, export, and any application-specific processing. Version 1 deliberately accepts one workbook per job. **Cross-workbook joins are required future work** — they are not available through this API or SDK, and v1 does not add multi-workbook request fields, fuzzy matching, or cross-tenant access.
