---
title: Pipeline Mode
description: Build dependent document-processing graphs that classify, branch, validate, repair, and assemble extraction results.
---

`ExtractionMode.PIPELINE` runs one shared OCR pass and then executes a small document-processing graph. It is intended for dependent extraction workflows where one node can route, repair, validate, or assemble the result for another node.

Pipeline mode is available through the public API and SDK facade helpers. The JSON contract is versioned by `pipeline.version`; the current schema is [`pipeline.schema.json`](./pipeline.schema.json).

## Python SDK

Use the manual SDK facade with raw JSON:

```python
guid = await client.process_pipeline_document(
    document=open("report.pdf", "rb"),
    document_mime_type="application/pdf",
    ocr="AUTO",
    pipeline=pipeline_definition,
    trace=True,
)

result = await client.wait_until_ready(guid)
sources = await client.get_source_locations(guid)
nodes = await client.get_pipeline_nodes(guid)
```

For an already-uploaded document, use `process_uploaded_pipeline_document(guid, pipeline=...)`. To save a named configuration, use `save_pipeline_configuration(...)`; retrieve the raw JSON with `get_pipeline_configuration(...)`.

The runnable Python notebook [Pipeline Extraction](https://github.com/docudevs/python-examples/blob/main/05-pipeline-extraction.ipynb) shows a classifier node routing an invoice or safety data sheet to the correct extraction branch.

## Java SDK

Use the Java facade with a Jackson `JsonNode` or JSON string:

```java
String guid = client.processPipelineDocument(
    new UploadRequest("report.pdf", "application/pdf", pdfBytes),
    pipelineDefinition,
    "AUTO"
);

JsonNode result = client.waitUntilReadyJson(guid, WaitOptions.defaults());
JsonNode nodes = client.getPipelineNodes(guid);
```

For an already-uploaded document, use `processUploadedPipelineDocument(guid, pipelineDefinition, "application/pdf", "AUTO")`. To save a named pipeline configuration, use `savePipelineConfiguration(...)`; retrieve the raw JSON with `getPipelineConfiguration(...)`.

## Minimal Definition

```json
{
  "version": "2026-05",
  "ocr": {
    "mode": "AUTO",
    "sourceLocations": true,
    "qualityArtifact": true
  },
  "nodes": [
    {
      "id": "extract_data",
      "type": "extract",
      "prompt": "Extract the requested fields.",
      "schema": {
        "type": "object",
        "properties": {
          "name": { "type": "string" }
        }
      },
      "sourceLocations": true
    },
    {
      "id": "final_result",
      "type": "final",
      "dependsOn": ["extract_data"],
      "output": "$nodes.extract_data.result"
    }
  ],
  "finals": [
    { "node": "final_result" }
  ]
}
```

## Classify and Route

The main reason to use pipeline mode is when a later extraction depends on an earlier decision. This example classifies the document first, then only runs the matching branch.

```json
{
  "version": "2026-05",
  "ocr": {
    "mode": "AUTO",
    "qualityArtifact": true
  },
  "concurrency": {
    "maxNodes": 2
  },
  "nodes": [
    {
      "id": "classify_document",
      "type": "extract",
      "source": "$ocr.content",
      "prompt": "Classify this document as invoice, safety_data_sheet, or other. Only classify the document.",
      "schema": {
        "type": "object",
        "properties": {
          "document_type": {
            "type": "string",
            "enum": ["invoice", "safety_data_sheet", "other"]
          },
          "confidence": {
            "type": "number"
          },
          "reason": {
            "type": "string"
          }
        },
        "required": ["document_type", "confidence", "reason"]
      },
      "llm": {
        "tier": "nano"
      }
    },
    {
      "id": "extract_invoice",
      "type": "extract",
      "dependsOn": ["classify_document"],
      "when": {
        "eq": ["$nodes.classify_document.result.document_type", "invoice"]
      },
      "source": "$ocr.content",
      "prompt": "Extract invoice fields from the document using the schema. Return null for fields that are genuinely missing.",
      "schema": {
        "type": "object",
        "properties": {
          "invoice_number": { "type": ["string", "null"] },
          "vendor_name": { "type": ["string", "null"] },
          "total_amount": { "type": ["number", "null"] },
          "currency": { "type": ["string", "null"] }
        }
      },
      "llm": {
        "tier": "mini"
      }
    },
    {
      "id": "extract_sds",
      "type": "extract",
      "dependsOn": ["classify_document"],
      "when": {
        "eq": ["$nodes.classify_document.result.document_type", "safety_data_sheet"]
      },
      "source": "$ocr.content",
      "prompt": "Extract product and safety information from the document using the schema.",
      "schema": {
        "type": "object",
        "properties": {
          "product_name": { "type": ["string", "null"] },
          "supplier": { "type": ["string", "null"] },
          "signal_word": { "type": ["string", "null"] }
        }
      },
      "llm": {
        "tier": "mini"
      }
    },
    {
      "id": "invoice_final",
      "type": "final",
      "dependsOn": ["classify_document", "extract_invoice"],
      "when": {
        "eq": ["$nodes.classify_document.result.document_type", "invoice"]
      },
      "output": {
        "branch": "invoice",
        "classification": "$nodes.classify_document.result",
        "extracted": "$nodes.extract_invoice.result"
      }
    },
    {
      "id": "sds_final",
      "type": "final",
      "dependsOn": ["classify_document", "extract_sds"],
      "when": {
        "eq": ["$nodes.classify_document.result.document_type", "safety_data_sheet"]
      },
      "output": {
        "branch": "safety_data_sheet",
        "classification": "$nodes.classify_document.result",
        "extracted": "$nodes.extract_sds.result"
      }
    },
    {
      "id": "other_final",
      "type": "final",
      "dependsOn": ["classify_document"],
      "when": {
        "eq": ["$nodes.classify_document.result.document_type", "other"]
      },
      "output": {
        "branch": "other",
        "classification": "$nodes.classify_document.result",
        "message": "No specialized extraction branch was selected."
      }
    }
  ],
  "finals": [
    { "node": "invoice_final" },
    { "node": "sds_final" },
    { "node": "other_final" }
  ]
}
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

## OCR Quality

When OCR quality is available, the worker stores `layout/ocr-quality.json` and exposes the normalized context to pipeline nodes as `$ocr.quality`. The stable routing fields are `category`, `score`, and `escalate`; use a `validate` node such as `bucket` when you need to route on numeric ranges without adding numeric operators to `when`.

## OCR_CORRECT

`OCR_CORRECT` nodes run the same pagewise OCR correction primitive as the standalone `ocr-correct` operation. Set `source` or `input` to the upstream content expression and choose `sourceMap` based on source-location needs:

- `preserve`: writes a projection map so downstream extract-node source locations can resolve through corrected content.
- `compress`: produces corrected content but downstream source locations are marked unresolved with a compressed-upstream reason.
- `none`: drops source-map projection for workflows that only need corrected text.

## Runtime Status

`GET /job/status/{guid}` includes a trimmed `pipeline` summary for pipeline jobs. Nodes include `id`, `type`, `status`, warnings, timestamps, and execution references such as `childOperationGuid` or `queuedTaskId` when present.

The main result still comes from `wait_until_ready()` / `GET /job/result/{guid}`. Source locations, when requested, are exposed through the existing `get_source_locations(guid)` Python helper or `GET /job/result/{guid}/source-locations`.

## Queue Mode

Inline execution remains the default. Queue-backed extract nodes can be enabled by deployment configuration or requested per pipeline with:

```json
{
  "execution": {
    "mode": "queue"
  }
}
```

In queue mode, extract nodes run as node-scoped AI tasks on the tiered worker pools. Node artifacts keep the same layout: `pipeline/nodes/{nodeId}/result.json` and optional `result.sources.json`.
