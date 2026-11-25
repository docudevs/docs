---
title: Workbook Normalization
description: Convert one native XLSX workbook and a prompt into a source-grounded relational JSON dataset, with inspectable plan, source-map, and summary artifacts.
---

Workbook normalization converts **one native `.xlsx` workbook** and a caller prompt into a source-grounded JSON dataset. It discovers tables and relationships from the workbook and the prompt itself — it does not apply a built-in insurance or other domain schema.

## When to use it

Use workbook normalization when a caller uploads a raw spreadsheet (loss triangles, bordereaux, exposure schedules, or any other multi-table workbook) and wants DocuDevs to find the tables, infer relationships between them, and return every value with a pointer back to its source cell — without supplying a schema up front.

## Request

Submit with `extractionMode: "WORKBOOK"`, a nonblank `prompt`, and the workbook's native XLSX MIME type (`application/vnd.openxmlformats-officedocument.spreadsheetml.sheet`). Upload exactly one file first. Do not send `schema`, map-reduce, a pipeline, page options, or batch inputs — workbook mode does not support any of these.

<Example name="workbooks/normalize-workbook" />

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
