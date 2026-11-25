---
title: Pipeline Mode
description: Build dependent document-processing graphs that classify, branch, validate, repair, and assemble extraction results.
---

`ExtractionMode.PIPELINE` runs one shared OCR pass and then executes a small document-processing graph. It is intended for dependent extraction workflows where one node can route, repair, validate, or assemble the result for another node.

Pipeline mode is available through the public API and SDK facade helpers. Prefer the Python and Java SDK builders for application code; they serialize to the same versioned JSON wire contract documented in [`pipeline.schema.json`](./pipeline.schema.json).

## Python SDK

Use `Pipeline`, `P`, and condition helpers such as `eq` to build the pipeline. Node methods return `NodeRef` objects, so dependencies and result paths can refer to previous nodes directly. The example below builds a one-node extraction pipeline, submits it, waits for the result, and reads the source locations and node statuses. The Java tab builds the same pipeline with `PipelineDefinition`, `PipelineNode`, `PipelinePath` and `PipelineCondition`; `PipelineNodeRef` values can be used in `dependsOn(...)` and result outputs.

<Example name="pipelines/process-pipeline-document" />

For an already-uploaded document, use `process_uploaded_pipeline_document(guid, pipeline=...)`. To save a named configuration, use `save_pipeline_configuration(...)`; retrieve the serialized pipeline JSON with `get_pipeline_configuration(...)`.

The runnable Python notebook [Pipeline Extraction](https://github.com/docudevs/python-examples/blob/main/05-pipeline-extraction.ipynb) shows a classifier node routing an invoice or safety data sheet to the correct extraction branch.

## Java SDK

The Java builders are shown in the Java tab of the example above. For an already-uploaded document, use `processUploadedPipelineDocument(guid, pipeline, "application/pdf", "AUTO")`. To save a named pipeline configuration, use `savePipelineConfiguration(...)`; retrieve the serialized pipeline JSON with `getPipelineConfiguration(...)`.

## Classify and Route

The main reason to use pipeline mode is when a later extraction depends on an earlier decision. This example classifies the document first, then only runs the matching branch.

<Example name="pipelines/route-by-document-type" />

Branch prompts should describe the extraction task itself, not the pipeline mechanics. The branch selection already happened through `when`; the extract node receives a normal extraction task over the selected source content.

Read the node statuses after the run to inspect routing (`get_pipeline_nodes(guid)` in Python, `getPipelineNodes(guid)` in Java, `pipeline.nodes` in `GET /job/status/{guid}`). Matching branches complete; non-matching branches are marked `SKIPPED`.

## Node Types

- `extract`: runs the existing structured extraction path with node-scoped prompt, schema, tools, images, LLM tier, and source-location settings.
- `validate`: runs platform validators such as `bucket`.
- `transform`: runs platform-owned transform handlers.
- `OCR_CORRECT`: runs OCR correction and can preserve, compress, or drop source maps.
- `operation`: submits a child operation job and consumes `operations/{childGuid}/result.json`.
- `final`: maps a completed node result into the public `{guid}/result.json`.

A job-level [`reasoningEffort`](/docs/basics/SimpleDocuments#reasoning-effort) request field applies to every node's LLM calls in the pipeline; there is no per-node reasoning-effort override — only the LLM tier (`llm_tier` in the Python builder) is settable per node.

## Declared Resources

A pipeline can declare reusable named resources in a top-level `resources` array and reference them from a node's `tools[].resource` field instead of repeating the same tool configuration on every node.

```json
{
  "version": "2026-05",
  "resources": [
    { "id": "holiday-calendar", "type": "DATE_RANGE_LOOKUP", "caseId": "case-123" }
  ],
  "nodes": [
    {
      "id": "extract_dates",
      "type": "extract",
      "source": "$ocr.content",
      "prompt": "Extract event dates.",
      "schema": { "type": "object" },
      "tools": [{ "type": "DATE_RANGE_LOOKUP", "resource": "holiday-calendar" }]
    }
  ],
  "finals": [{ "node": "extract_dates" }]
}
```

At execution time the worker merges every field from the named resource (everything except `id` and `type`) into the tool descriptor that references it. A node whose `tools[].resource` names an id that is not declared in `resources` fails at execution with a pipeline execution error, and pipeline submission is rejected up front for the same mismatch.

Declared resources are raw pipeline JSON only — the Python `Pipeline` builder has no `.resource()`/`.resources()` helper. Build the JSON directly (or via `pipeline.raw_node(...)`) and pass it to `save_pipeline_configuration(...)` or `process_pipeline_document(...)`.

## Inputs and Structured Node Input

A pipeline can declare a top-level `inputs` map that resolves named values once per job (or, for a client pipeline, once per segment) and exposes them to node expressions as `$inputs` (the whole map) or `$inputs.<name>` (and deeper dotted paths under it).

Each entry has a `kind`:

- `literal`: use `value` verbatim.
- `nodeArtifact`: read a prior node's own result from `pipeline/nodes/<nodeId>/result.json` in the same job.
- `clientArtifact`: read an artifact produced by a local step of an active client-pipeline run — see [Client Pipelines](/docs/core/client-pipelines).

An optional RFC 6901 JSON `pointer` (max depth 32) can drill into the resolved value before it is bound to `$inputs.<name>`.

```json
{
  "version": "2026-05",
  "inputs": {
    "approvedVendors": { "kind": "literal", "value": ["Acme Supplies", "Globex"] }
  },
  "nodes": [
    {
      "id": "extract_line_items",
      "type": "extract",
      "source": "$ocr.content",
      "input": "$inputs.approvedVendors",
      "prompt": "Extract the line items billed by these approved vendors.",
      "schema": { "type": "object" }
    }
  ],
  "finals": [{ "node": "extract_line_items" }]
}
```

An `extract` node's `input` field is distinct from `source`: `source` supplies the primary document text the model reads, while `input` hands the model additional structured JSON delivered as delimited data alongside the source — not as the primary document text. Use it for reference data the model should treat as context, such as a validated list of values or a prior node's structured result.

Like declared resources, `inputs` and node-level `input` are raw pipeline JSON — the Python `Pipeline` builder's `extract()`/`ocr_correct()` methods do not take an `input=` keyword. The full JSON Schema for both fields is in [`pipeline.schema.json`](./pipeline.schema.json).

## OCR Quality

When OCR quality is available, the worker stores `layout/ocr-quality.json` and exposes the normalized context to pipeline nodes as `$ocr.quality`. The stable routing fields are `category`, `score`, and `escalate`; use a `validate` node such as `bucket` when you need to route on numeric ranges without adding numeric operators to `when`. The example buckets the numeric score into `good` and `review`, builds the pipeline and prints its JSON.

<Example name="pipelines/bucket-ocr-quality" />

## OCR_CORRECT

`OCR_CORRECT` nodes run the same pagewise OCR correction primitive as the standalone `ocr-correct` operation. Set `source` or `input` to the upstream content expression and choose `sourceMap` based on source-location needs:

- `preserve`: writes a projection map so downstream extract-node source locations can resolve through corrected content.
- `compress`: produces corrected content but downstream source locations are marked unresolved with a compressed-upstream reason.
- `none`: drops source-map projection for workflows that only need corrected text.

<Example name="pipelines/correct-ocr-text" />

## Runtime Status

`GET /job/status/{guid}` includes a trimmed `pipeline` summary for pipeline jobs. Nodes include `id`, `type`, `status`, warnings, timestamps, and execution references such as `childOperationGuid` or `queuedTaskId` when present.

The main result still comes from `wait_until_ready()` / `GET /job/result/{guid}`. Source locations, when requested, are exposed through the existing `get_source_locations(guid)` Python helper or `GET /job/result/{guid}/source-locations`.

## Queue Mode

Request queue-backed extract nodes per pipeline with `Pipeline().execution_mode("queue")`. In queue mode, extract nodes run as node-scoped AI tasks on the tiered worker pools. Node artifacts keep the same layout: `pipeline/nodes/{nodeId}/result.json` and optional `result.sources.json`.
