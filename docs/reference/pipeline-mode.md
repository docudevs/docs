---
title: Pipeline Mode
description: Build dependent document-processing graphs that classify, branch, validate, repair, and assemble extraction results.
---

`ExtractionMode.PIPELINE` runs one shared OCR pass and then executes a small document-processing graph. It is intended for dependent extraction workflows where one node can route, repair, validate, or assemble the result for another node.

Pipeline mode is available through the public API and SDK facade helpers. Prefer the Python and Java SDK builders for application code; they serialize to the same versioned JSON wire contract documented in [`pipeline.schema.json`](./pipeline.schema.json).

## Python SDK

Use `Pipeline`, `P`, and condition helpers such as `eq` to build the pipeline. Node methods return `NodeRef` objects, so dependencies and result paths can refer to previous nodes directly.

```python
from docudevs import DocuDevsClient, P, Pipeline, eq

client = DocuDevsClient(token="YOUR_API_KEY")

pipeline = Pipeline().ocr(mode="AUTO", source_locations=True, quality_artifact=True)
extract_data = pipeline.extract(
    "extract_data",
    source=P.ocr.content,
    prompt="Extract the requested fields.",
    schema={
        "type": "object",
        "properties": {"name": {"type": "string"}},
    },
    source_locations=True,
)
pipeline.final(
    "final_result",
    depends_on=[extract_data],
    output=extract_data.result,
)

guid = await client.process_pipeline_document(
    document=open("report.pdf", "rb"),
    document_mime_type="application/pdf",
    ocr="AUTO",
    pipeline=pipeline,
    trace=True,
)

result = await client.wait_until_ready(guid)
sources = await client.get_source_locations(guid)
nodes = await client.get_pipeline_nodes(guid)
```

For an already-uploaded document, use `process_uploaded_pipeline_document(guid, pipeline=...)`. To save a named configuration, use `save_pipeline_configuration(...)`; retrieve the serialized pipeline JSON with `get_pipeline_configuration(...)`.

The runnable Python notebook [Pipeline Extraction](https://github.com/docudevs/python-examples/blob/main/05-pipeline-extraction.ipynb) shows a classifier node routing an invoice or safety data sheet to the correct extraction branch.

## Java SDK

Use `PipelineDefinition`, `PipelineNode`, `PipelinePath`, and `PipelineCondition` to build the pipeline. `PipelineNodeRef` values can be used in `dependsOn(...)` and result outputs.

```java
import static ai.docudevs.client.PipelinePath.ocr;

import ai.docudevs.client.PipelineDefinition;
import ai.docudevs.client.PipelineNode;
import ai.docudevs.client.PipelineNodeRef;
import ai.docudevs.client.PipelineOcr;
import ai.docudevs.client.UploadRequest;
import java.util.Map;

PipelineDefinition.Builder pipelineBuilder = PipelineDefinition.builder()
    .ocr(PipelineOcr.builder()
        .mode("AUTO")
        .sourceLocations(true)
        .qualityArtifact(true)
        .build());

PipelineNodeRef extractData = pipelineBuilder.addNodeRef(PipelineNode.extract("extract_data")
    .source(ocr().content())
    .prompt("Extract the requested fields.")
    .schema(Map.of(
        "type", "object",
        "properties", Map.of("name", Map.of("type", "string"))))
    .sourceLocations(true)
    .build());

PipelineNodeRef finalResult = pipelineBuilder.addNodeRef(PipelineNode.finalResult("final_result")
    .dependsOn(extractData)
    .output(extractData.result())
    .build());

PipelineDefinition pipeline = pipelineBuilder.finalOrder(finalResult).build();

String guid = client.processPipelineDocument(
    new UploadRequest("report.pdf", "application/pdf", pdfBytes),
    pipeline,
    "AUTO"
);

JsonNode result = client.waitUntilReadyJson(guid, WaitOptions.defaults());
JsonNode nodes = client.getPipelineNodes(guid);
```

For an already-uploaded document, use `processUploadedPipelineDocument(guid, pipeline, "application/pdf", "AUTO")`. To save a named pipeline configuration, use `savePipelineConfiguration(...)`; retrieve the serialized pipeline JSON with `getPipelineConfiguration(...)`.

## Minimal Builder

```python
pipeline = Pipeline().ocr(mode="AUTO", source_locations=True, quality_artifact=True)

extract_data = pipeline.extract(
    "extract_data",
    source=P.ocr.content,
    prompt="Extract the requested fields.",
    schema={"type": "object", "properties": {"name": {"type": "string"}}},
    source_locations=True,
)

pipeline.final(
    "final_result",
    depends_on=[extract_data],
    output=extract_data.result,
)
```

## Classify and Route

The main reason to use pipeline mode is when a later extraction depends on an earlier decision. This example classifies the document first, then only runs the matching branch.

```python
pipeline = Pipeline().ocr(mode="AUTO", quality_artifact=True).max_nodes(2)

classification = pipeline.extract(
    "classify_document",
    source=P.ocr.content,
    prompt="Classify this document as invoice, safety_data_sheet, or other. Only classify the document.",
    schema={
        "type": "object",
        "properties": {
            "document_type": {
                "type": "string",
                "enum": ["invoice", "safety_data_sheet", "other"],
            },
            "confidence": {"type": "number"},
            "reason": {"type": "string"},
        },
        "required": ["document_type", "confidence", "reason"],
    },
    llm_tier="nano",
)

invoice = pipeline.extract(
    "extract_invoice",
    depends_on=[classification],
    when=eq(classification.result.document_type, "invoice"),
    source=P.ocr.content,
    prompt="Extract invoice fields from the document. Return null for missing fields.",
    schema={
        "type": "object",
        "properties": {
            "invoice_number": {"type": ["string", "null"]},
            "vendor_name": {"type": ["string", "null"]},
            "total_amount": {"type": ["number", "null"]},
            "currency": {"type": ["string", "null"]},
        },
    },
    llm_tier="mini",
)

sds = pipeline.extract(
    "extract_sds",
    depends_on=[classification],
    when=eq(classification.result.document_type, "safety_data_sheet"),
    source=P.ocr.content,
    prompt="Extract product and safety information from the document.",
    schema={
        "type": "object",
        "properties": {
            "product_name": {"type": ["string", "null"]},
            "supplier": {"type": ["string", "null"]},
            "signal_word": {"type": ["string", "null"]},
        },
    },
    llm_tier="mini",
)

invoice_final = pipeline.final_candidate(
    "invoice_final",
    depends_on=[classification, invoice],
    when=eq(classification.result.document_type, "invoice"),
    output={
        "branch": "invoice",
        "classification": classification.result,
        "extracted": invoice.result,
    },
)
sds_final = pipeline.final_candidate(
    "sds_final",
    depends_on=[classification, sds],
    when=eq(classification.result.document_type, "safety_data_sheet"),
    output={
        "branch": "safety_data_sheet",
        "classification": classification.result,
        "extracted": sds.result,
    },
)
other_final = pipeline.final_candidate(
    "other_final",
    depends_on=[classification],
    when=eq(classification.result.document_type, "other"),
    output={
        "branch": "other",
        "classification": classification.result,
        "message": "No specialized extraction branch was selected.",
    },
)
pipeline.final_order(invoice_final, sds_final, other_final)
```

Branch prompts should describe the extraction task itself, not the pipeline mechanics. The branch selection already happened through `when`; the extract node receives a normal extraction task over the selected source content.

Use `get_pipeline_nodes(guid)` to inspect routing after the run. Matching branches complete; non-matching branches are marked `SKIPPED`.

## Node Types

- `extract`: runs the existing structured extraction path with node-scoped prompt, schema, tools, images, LLM tier, and source-location settings.
- `validate`: runs platform validators such as `bucket`.
- `transform`: runs platform-owned transform handlers.
- `OCR_CORRECT`: runs OCR correction and can preserve, compress, or drop source maps.
- `operation`: submits a child operation job and consumes `operations/{childGuid}/result.json`.
- `final`: maps a completed node result into the public `{guid}/result.json`.

A job-level [`reasoningEffort`](/docs/basics/SimpleDocuments#reasoning-effort) request field applies to every node's LLM calls in the pipeline; there is no per-node reasoning-effort override — only `llm_tier` is settable per node.

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

Declared resources are raw pipeline JSON only — the Python `Pipeline` builder has no `.resource()`/`.resources()` helper today. Build the JSON directly (or via `pipeline.raw_node(...)`) and pass it to `save_pipeline_configuration(...)` or `process_pipeline_document(...)`.

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
    "validatedGenes": { "kind": "literal", "value": ["TP53", "EGFR"] }
  },
  "nodes": [
    {
      "id": "extract_variants",
      "type": "extract",
      "source": "$ocr.content",
      "input": "$inputs.validatedGenes",
      "prompt": "Extract variant calls for these validated genes.",
      "schema": { "type": "object" }
    }
  ],
  "finals": [{ "node": "extract_variants" }]
}
```

An `extract` node's `input` field is distinct from `source`: `source` supplies the primary document text the model reads, while `input` hands the model additional structured JSON delivered as delimited data alongside the source — not as the primary document text. Use it for reference data the model should treat as context, such as a validated list of values or a prior node's structured result.

Like declared resources, `inputs` and node-level `input` are raw pipeline JSON — the Python `Pipeline` builder's `extract()`/`ocr_correct()` methods do not take an `input=` keyword yet. The full JSON Schema for both fields is in [`pipeline.schema.json`](./pipeline.schema.json).

## OCR Quality

When OCR quality is available, the worker stores `layout/ocr-quality.json` and exposes the normalized context to pipeline nodes as `$ocr.quality`. The stable routing fields are `category`, `score`, and `escalate`; use a `validate` node such as `bucket` when you need to route on numeric ranges without adding numeric operators to `when`.

```python
quality = pipeline.validate_bucket(
    "ocr_quality",
    input=P.ocr.quality.score,
    buckets=[
        {"name": "good", "min": 0.85},
        {"name": "review"},
    ],
    default="review",
)
```

## OCR_CORRECT

`OCR_CORRECT` nodes run the same pagewise OCR correction primitive as the standalone `ocr-correct` operation. Set `source` or `input` to the upstream content expression and choose `sourceMap` based on source-location needs:

- `preserve`: writes a projection map so downstream extract-node source locations can resolve through corrected content.
- `compress`: produces corrected content but downstream source locations are marked unresolved with a compressed-upstream reason.
- `none`: drops source-map projection for workflows that only need corrected text.

```python
ocr_corrected = pipeline.ocr_correct(
    "ocr_corrected",
    source=P.ocr.content,
    mode="correction",
    scope="pagewise",
    source_map="preserve",
    llm_tier="mini",
)

extracted = pipeline.extract(
    "extract_corrected",
    depends_on=[ocr_corrected],
    source=ocr_corrected.result.content,
    prompt="Extract fields from the corrected OCR text.",
    schema={"type": "object", "properties": {"name": {"type": "string"}}},
    source_locations=True,
)
```

## Runtime Status

`GET /job/status/{guid}` includes a trimmed `pipeline` summary for pipeline jobs. Nodes include `id`, `type`, `status`, warnings, timestamps, and execution references such as `childOperationGuid` or `queuedTaskId` when present.

The main result still comes from `wait_until_ready()` / `GET /job/result/{guid}`. Source locations, when requested, are exposed through the existing `get_source_locations(guid)` Python helper or `GET /job/result/{guid}/source-locations`.

## Queue Mode

Inline execution remains the default. Request queue-backed extract nodes per pipeline with:

```python
pipeline = Pipeline().execution_mode("queue")
```

In queue mode, extract nodes run as node-scoped AI tasks on the tiered worker pools. Node artifacts keep the same layout: `pipeline/nodes/{nodeId}/result.json` and optional `result.sources.json`.
