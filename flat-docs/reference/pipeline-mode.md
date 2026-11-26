---
title: Pipeline Mode
description: Build dependent document-processing graphs that classify, branch, validate, repair, and assemble extraction results.
---
<!-- Generated from docs/reference/pipeline-mode.md. Do not edit by hand. -->

`ExtractionMode.PIPELINE` runs one shared OCR pass and then executes a small document-processing graph. It is intended for dependent extraction workflows where one node can route, repair, validate, or assemble the result for another node.

Pipeline mode is available through the public API and SDK facade helpers. Prefer the Python and Java SDK builders for application code; they serialize to the same versioned JSON wire contract documented in [`pipeline.schema.json`](./pipeline.schema.json).

## Python SDK

Use `Pipeline`, `P`, and condition helpers such as `eq` to build the pipeline. Node methods return `NodeRef` objects, so dependencies and result paths can refer to previous nodes directly. The example below builds a one-node extraction pipeline, submits it, waits for the result, and reads the source locations and node statuses. The Java variant builds the same pipeline with `PipelineDefinition`, `PipelineNode` and `PipelinePath`; `PipelineNodeRef` values can be used in `dependsOn(...)` and result outputs.

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

GUID=$(curl -s -S -f -X POST "https://api.docudevs.ai/document/upload" \
  -H "Authorization: $API_KEY" \
  -F "document=@report.pdf" | jq -r '.guid')

curl -s -S -f -o /dev/null -X POST "https://api.docudevs.ai/document/process/$GUID" \
  -H "Authorization: $API_KEY" \
  -H "Content-Type: application/json" \
  --data-binary @- <<'JSON'
{
  "mimeType": "application/pdf",
  "ocr": "AUTO",
  "extractionMode": "PIPELINE",
  "trace": true,
  "pipeline": {
    "version": "2026-05",
    "ocr": {"mode": "AUTO", "sourceLocations": true, "qualityArtifact": true},
    "nodes": [
      {
        "id": "extract_data",
        "type": "extract",
        "source": "$ocr.content",
        "prompt": "Extract the requested fields.",
        "schema": {
          "type": "object",
          "properties": {"name": {"type": "string"}}
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
    "finals": [{"node": "final_result"}]
  }
}
JSON

wait_for_job "$GUID"

curl -s -S -f "https://api.docudevs.ai/job/result/$GUID/json" \
  -H "Authorization: $API_KEY" | jq .

curl -s -S -f "https://api.docudevs.ai/job/result/$GUID/source-locations" \
  -H "Authorization: $API_KEY" | jq .

curl -s -S -f "https://api.docudevs.ai/job/status/$GUID" \
  -H "Authorization: $API_KEY" | jq '.pipeline.nodes'
```

**Python**

```python
from pydantic import BaseModel

from docudevs import P, Pipeline

class ExtractedFields(BaseModel):
    name: str | None = None

pipeline = Pipeline().ocr(mode="AUTO", source_locations=True, quality_artifact=True)
extract_data = pipeline.extract(
    "extract_data",
    source=P.ocr.content,
    prompt="Extract the requested fields.",
    schema=ExtractedFields.model_json_schema(),
    source_locations=True,
)
pipeline.final(
    "final_result",
    depends_on=[extract_data],
    output=extract_data.result,
)

with open("report.pdf", "rb") as f:
    document_data = f.read()

guid = await client.process_pipeline_document(
    document=document_data,
    document_mime_type="application/pdf",
    ocr="AUTO",
    pipeline=pipeline,
    trace=True,
)

result = await client.wait_until_ready(guid, result_format="json")
print(json.dumps(result, indent=2))

sources = await client.get_source_locations(guid)
print(json.dumps(sources, indent=2))

nodes = await client.get_pipeline_nodes(guid)
print(json.dumps(nodes, indent=2))
```

**Java**

```java
import static ai.docudevs.client.PipelinePath.ocr;

import ai.docudevs.client.PipelineDefinition;
import ai.docudevs.client.PipelineNode;
import ai.docudevs.client.PipelineNodeRef;
import ai.docudevs.client.PipelineOcr;
import ai.docudevs.client.UploadRequest;
import ai.docudevs.client.WaitOptions;
import com.fasterxml.jackson.databind.JsonNode;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
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

byte[] documentData = Files.readAllBytes(Path.of("report.pdf"));

String guid = client.processPipelineDocument(
        new UploadRequest("report.pdf", "application/pdf", documentData),
        pipeline,
        "AUTO");

JsonNode result = client.waitUntilReadyJson(guid, WaitOptions.builder()
        .timeout(Duration.ofSeconds(180))
        .pollInterval(Duration.ofSeconds(5))
        .build());
System.out.println(result.toPrettyString());

JsonNode sources = client.getSourceLocations(guid);
System.out.println(sources.toPrettyString());

JsonNode nodes = client.getPipelineNodes(guid);
System.out.println(nodes.toPrettyString());
```

For an already-uploaded document, use `process_uploaded_pipeline_document(guid, pipeline=...)`. To save a named configuration, use `save_pipeline_configuration(...)`; retrieve the serialized pipeline JSON with `get_pipeline_configuration(...)`.

The runnable Python notebook [Pipeline Extraction](https://github.com/docudevs/python-examples/blob/main/05-pipeline-extraction.ipynb) shows a classifier node routing an invoice or safety data sheet to the correct extraction branch.

## Java SDK

The Java builders are shown in the Java variant of the example above. For an already-uploaded document, use `processUploadedPipelineDocument(guid, pipeline, "application/pdf", "AUTO")`. To save a named pipeline configuration, use `savePipelineConfiguration(...)`; retrieve the serialized pipeline JSON with `getPipelineConfiguration(...)`.

## Classify and Route

The main reason to use pipeline mode is when a later extraction depends on an earlier decision. This example classifies the document first, then only runs the matching branch.

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

GUID=$(curl -s -S -f -X POST "https://api.docudevs.ai/document/upload" \
  -H "Authorization: $API_KEY" \
  -F "document=@report.pdf" | jq -r '.guid')

curl -s -S -f -o /dev/null -X POST "https://api.docudevs.ai/document/process/$GUID" \
  -H "Authorization: $API_KEY" \
  -H "Content-Type: application/json" \
  --data-binary @- <<'JSON'
{
  "mimeType": "application/pdf",
  "ocr": "AUTO",
  "extractionMode": "PIPELINE",
  "pipeline": {
    "version": "2026-05",
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
              "enum": [
                "invoice",
                "safety_data_sheet",
                "other"
              ]
            },
            "confidence": {
              "type": "number"
            },
            "reason": {
              "type": "string"
            }
          },
          "required": [
            "document_type",
            "confidence",
            "reason"
          ]
        },
        "llm": {
          "tier": "nano"
        }
      },
      {
        "id": "extract_invoice",
        "type": "extract",
        "dependsOn": [
          "classify_document"
        ],
        "when": {
          "eq": [
            "$nodes.classify_document.result.document_type",
            "invoice"
          ]
        },
        "source": "$ocr.content",
        "prompt": "Extract invoice fields from the document. Return null for missing fields.",
        "schema": {
          "type": "object",
          "properties": {
            "invoice_number": {
              "type": [
                "string",
                "null"
              ]
            },
            "vendor_name": {
              "type": [
                "string",
                "null"
              ]
            },
            "total_amount": {
              "type": [
                "number",
                "null"
              ]
            },
            "currency": {
              "type": [
                "string",
                "null"
              ]
            }
          }
        },
        "llm": {
          "tier": "mini"
        }
      },
      {
        "id": "extract_sds",
        "type": "extract",
        "dependsOn": [
          "classify_document"
        ],
        "when": {
          "eq": [
            "$nodes.classify_document.result.document_type",
            "safety_data_sheet"
          ]
        },
        "source": "$ocr.content",
        "prompt": "Extract product and safety information from the document.",
        "schema": {
          "type": "object",
          "properties": {
            "product_name": {
              "type": [
                "string",
                "null"
              ]
            },
            "supplier": {
              "type": [
                "string",
                "null"
              ]
            },
            "signal_word": {
              "type": [
                "string",
                "null"
              ]
            }
          }
        },
        "llm": {
          "tier": "mini"
        }
      },
      {
        "id": "invoice_final",
        "type": "final",
        "dependsOn": [
          "classify_document",
          "extract_invoice"
        ],
        "when": {
          "eq": [
            "$nodes.classify_document.result.document_type",
            "invoice"
          ]
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
        "dependsOn": [
          "classify_document",
          "extract_sds"
        ],
        "when": {
          "eq": [
            "$nodes.classify_document.result.document_type",
            "safety_data_sheet"
          ]
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
        "dependsOn": [
          "classify_document"
        ],
        "when": {
          "eq": [
            "$nodes.classify_document.result.document_type",
            "other"
          ]
        },
        "output": {
          "branch": "other",
          "classification": "$nodes.classify_document.result",
          "message": "No specialized extraction branch was selected."
        }
      }
    ],
    "finals": [
      {
        "node": "invoice_final"
      },
      {
        "node": "sds_final"
      },
      {
        "node": "other_final"
      }
    ],
    "ocr": {
      "mode": "AUTO",
      "qualityArtifact": true
    },
    "concurrency": {
      "maxNodes": 2
    }
  }
}
JSON

wait_for_job "$GUID"

curl -s -S -f "https://api.docudevs.ai/job/result/$GUID/json" \
  -H "Authorization: $API_KEY" | jq .

curl -s -S -f "https://api.docudevs.ai/job/status/$GUID" \
  -H "Authorization: $API_KEY" | jq '.pipeline.nodes'
```

**Python**

```python
from typing import Literal

from pydantic import BaseModel

from docudevs import P, Pipeline, eq

class Classification(BaseModel):
    document_type: Literal["invoice", "safety_data_sheet", "other"]
    confidence: float
    reason: str

class InvoiceFields(BaseModel):
    invoice_number: str | None = None
    vendor_name: str | None = None
    total_amount: float | None = None
    currency: str | None = None

class SafetyDataSheetFields(BaseModel):
    product_name: str | None = None
    supplier: str | None = None
    signal_word: str | None = None

pipeline = Pipeline().ocr(mode="AUTO", quality_artifact=True).max_nodes(2)

classification = pipeline.extract(
    "classify_document",
    source=P.ocr.content,
    prompt="Classify this document as invoice, safety_data_sheet, or other. Only classify the document.",
    schema=Classification.model_json_schema(),
    llm_tier="nano",
)

invoice = pipeline.extract(
    "extract_invoice",
    depends_on=[classification],
    when=eq(classification.result.document_type, "invoice"),
    source=P.ocr.content,
    prompt="Extract invoice fields from the document. Return null for missing fields.",
    schema=InvoiceFields.model_json_schema(),
    llm_tier="mini",
)

sds = pipeline.extract(
    "extract_sds",
    depends_on=[classification],
    when=eq(classification.result.document_type, "safety_data_sheet"),
    source=P.ocr.content,
    prompt="Extract product and safety information from the document.",
    schema=SafetyDataSheetFields.model_json_schema(),
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

with open("report.pdf", "rb") as f:
    document_data = f.read()

guid = await client.process_pipeline_document(
    document=document_data,
    document_mime_type="application/pdf",
    ocr="AUTO",
    pipeline=pipeline,
)

result = await client.wait_until_ready(guid, result_format="json")
print(json.dumps(result, indent=2))

nodes = await client.get_pipeline_nodes(guid)
print(json.dumps(nodes, indent=2))
```

**Java**

```java
import static ai.docudevs.client.PipelinePath.ocr;

import ai.docudevs.client.PipelineCondition;
import ai.docudevs.client.PipelineDefinition;
import ai.docudevs.client.PipelineNode;
import ai.docudevs.client.PipelineNodeRef;
import ai.docudevs.client.PipelineOcr;
import ai.docudevs.client.PipelinePath;
import ai.docudevs.client.UploadRequest;
import ai.docudevs.client.WaitOptions;
import com.fasterxml.jackson.databind.JsonNode;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.util.List;
import java.util.Map;

PipelineDefinition.Builder pipelineBuilder = PipelineDefinition.builder()
        .ocr(PipelineOcr.builder()
                .mode("AUTO")
                .qualityArtifact(true)
                .build())
        .maxNodes(2);

PipelineNodeRef classification = pipelineBuilder.addNodeRef(PipelineNode.extract("classify_document")
        .source(ocr().content())
        .prompt("Classify this document as invoice, safety_data_sheet, or other. Only classify the document.")
        .schema(Map.of(
                "type", "object",
                "properties", Map.of(
                        "document_type", Map.of(
                                "type", "string",
                                "enum", List.of("invoice", "safety_data_sheet", "other")),
                        "confidence", Map.of("type", "number"),
                        "reason", Map.of("type", "string")),
                "required", List.of("document_type", "confidence", "reason")))
        .llmTier("nano")
        .build());

PipelinePath documentType = classification.result().field("document_type");

PipelineNodeRef invoice = pipelineBuilder.addNodeRef(PipelineNode.extract("extract_invoice")
        .dependsOn(classification)
        .when(PipelineCondition.eq(documentType, "invoice"))
        .source(ocr().content())
        .prompt("Extract invoice fields from the document. Return null for missing fields.")
        .schema(Map.of(
                "type", "object",
                "properties", Map.of(
                        "invoice_number", Map.of("type", List.of("string", "null")),
                        "vendor_name", Map.of("type", List.of("string", "null")),
                        "total_amount", Map.of("type", List.of("number", "null")),
                        "currency", Map.of("type", List.of("string", "null")))))
        .llmTier("mini")
        .build());

PipelineNodeRef sds = pipelineBuilder.addNodeRef(PipelineNode.extract("extract_sds")
        .dependsOn(classification)
        .when(PipelineCondition.eq(documentType, "safety_data_sheet"))
        .source(ocr().content())
        .prompt("Extract product and safety information from the document.")
        .schema(Map.of(
                "type", "object",
                "properties", Map.of(
                        "product_name", Map.of("type", List.of("string", "null")),
                        "supplier", Map.of("type", List.of("string", "null")),
                        "signal_word", Map.of("type", List.of("string", "null")))))
        .llmTier("mini")
        .build());

PipelineNodeRef invoiceFinal = pipelineBuilder.addNodeRef(PipelineNode.finalResult("invoice_final")
        .dependsOn(classification, invoice)
        .when(PipelineCondition.eq(documentType, "invoice"))
        .output(Map.of(
                "branch", "invoice",
                "classification", classification.result(),
                "extracted", invoice.result()))
        .build());

PipelineNodeRef sdsFinal = pipelineBuilder.addNodeRef(PipelineNode.finalResult("sds_final")
        .dependsOn(classification, sds)
        .when(PipelineCondition.eq(documentType, "safety_data_sheet"))
        .output(Map.of(
                "branch", "safety_data_sheet",
                "classification", classification.result(),
                "extracted", sds.result()))
        .build());

PipelineNodeRef otherFinal = pipelineBuilder.addNodeRef(PipelineNode.finalResult("other_final")
        .dependsOn(classification)
        .when(PipelineCondition.eq(documentType, "other"))
        .output(Map.of(
                "branch", "other",
                "classification", classification.result(),
                "message", "No specialized extraction branch was selected."))
        .build());

PipelineDefinition pipeline = pipelineBuilder.finalOrder(invoiceFinal, sdsFinal, otherFinal).build();

byte[] documentData = Files.readAllBytes(Path.of("report.pdf"));

String guid = client.processPipelineDocument(
        new UploadRequest("report.pdf", "application/pdf", documentData),
        pipeline,
        "AUTO");

JsonNode result = client.waitUntilReadyJson(guid,
        WaitOptions.builder().timeout(Duration.ofSeconds(180)).pollInterval(Duration.ofSeconds(5)).build());
System.out.println(result.toPrettyString());

JsonNode nodes = client.getPipelineNodes(guid);
System.out.println(nodes.toPrettyString());
```

Branch prompts should describe the extraction task itself, not the pipeline mechanics. The branch selection already happened through `when`; the extract node receives a normal extraction task over the selected source content.

Read the node statuses after the run to inspect routing (`get_pipeline_nodes(guid)` in Python, `getPipelineNodes(guid)` in Java, `pipeline.nodes` in `GET /job/status/{guid}`). Matching branches complete; non-matching branches are marked `SKIPPED`.

## Node Types

- `extract`: runs the existing structured extraction path with node-scoped prompt, schema, tools, images, LLM tier, and source-location settings.
- `validate`: runs platform validators such as `bucket`.
- `transform`: runs platform-owned transform handlers.
- `OCR_CORRECT`: runs OCR correction and can preserve, compress, or drop source maps.
- `operation`: submits a child operation job and consumes `operations/{childGuid}/result.json`.
- `final`: maps a completed node result into the public `{guid}/result.json`.

A job-level [`reasoningEffort`](https://docs.docudevs.ai/docs/basics/SimpleDocuments#reasoning-effort) request field applies to every node's LLM calls in the pipeline; there is no per-node reasoning-effort override — only the LLM tier (`llm_tier` in the Python builder) is settable per node.

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
- `clientArtifact`: read an artifact produced by a local step of an active client-pipeline run — see [Client Pipelines](https://docs.docudevs.ai/docs/core/client-pipelines).

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

**Python**

```python
from pydantic import BaseModel

from docudevs import P, Pipeline


class ExtractedFields(BaseModel):
    name: str | None = None


pipeline = Pipeline().ocr(mode="AUTO", quality_artifact=True)

quality = pipeline.validate_bucket(
    "ocr_quality",
    input=P.ocr.quality.score,
    buckets=[
        {"name": "good", "min": 0.85},
        {"name": "review"},
    ],
    default="review",
)

extracted = pipeline.extract(
    "extract_data",
    source=P.ocr.content,
    prompt="Extract the requested fields.",
    schema=ExtractedFields.model_json_schema(),
)

pipeline.final(
    "final_result",
    depends_on=[quality, extracted],
    output={"quality": quality.result, "extracted": extracted.result},
)

print(json.dumps(pipeline.to_dict(), indent=2))
```

**Java**

```java
import static ai.docudevs.client.PipelinePath.ocr;

import ai.docudevs.client.PipelineBucket;
import ai.docudevs.client.PipelineDefinition;
import ai.docudevs.client.PipelineNode;
import ai.docudevs.client.PipelineNodeRef;
import ai.docudevs.client.PipelineOcr;
import java.util.List;
import java.util.Map;

PipelineDefinition.Builder pipelineBuilder = PipelineDefinition.builder()
        .ocr(PipelineOcr.builder()
                .mode("AUTO")
                .qualityArtifact(true)
                .build());

PipelineNodeRef quality = pipelineBuilder.validateBucket(
        "ocr_quality",
        ocr().quality().score(),
        List.of(PipelineBucket.withMin("good", 0.85), PipelineBucket.named("review")),
        "review");

PipelineNodeRef extracted = pipelineBuilder.addNodeRef(PipelineNode.extract("extract_data")
        .source(ocr().content())
        .prompt("Extract the requested fields.")
        .schema(Map.of(
                "type", "object",
                "properties", Map.of(
                        "name", Map.of("anyOf", List.of(
                                Map.of("type", "string"),
                                Map.of("type", "null"))))))
        .build());

PipelineNodeRef finalResult = pipelineBuilder.addNodeRef(PipelineNode.finalResult("final_result")
        .dependsOn(quality, extracted)
        .output(Map.of("quality", quality.result(), "extracted", extracted.result()))
        .build());

PipelineDefinition pipeline = pipelineBuilder.finalOrder(finalResult).build();

System.out.println(pipeline.buildJsonNode().toPrettyString());
```

## OCR_CORRECT

`OCR_CORRECT` nodes run the same pagewise OCR correction primitive as the standalone `ocr-correct` operation. Set `source` or `input` to the upstream content expression and choose `sourceMap` based on source-location needs:

- `preserve`: writes a projection map so downstream extract-node source locations can resolve through corrected content.
- `compress`: produces corrected content but downstream source locations are marked unresolved with a compressed-upstream reason.
- `none`: drops source-map projection for workflows that only need corrected text.

**Python**

```python
from pydantic import BaseModel

from docudevs import P, Pipeline


class ExtractedFields(BaseModel):
    name: str | None = None


pipeline = Pipeline().ocr(mode="AUTO", source_locations=True)

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
    schema=ExtractedFields.model_json_schema(),
    source_locations=True,
)
pipeline.final("final_result", depends_on=[extracted], output=extracted.result)

print(json.dumps(pipeline.to_dict(), indent=2))
```

**Java**

```java
import static ai.docudevs.client.PipelinePath.ocr;

import ai.docudevs.client.PipelineDefinition;
import ai.docudevs.client.PipelineNode;
import ai.docudevs.client.PipelineNodeRef;
import ai.docudevs.client.PipelineOcr;
import java.util.List;
import java.util.Map;

PipelineDefinition.Builder pipelineBuilder = PipelineDefinition.builder()
        .ocr(PipelineOcr.builder()
                .mode("AUTO")
                .sourceLocations(true)
                .build());

PipelineNodeRef ocrCorrected = pipelineBuilder.addNodeRef(PipelineNode.ocrCorrect("ocr_corrected")
        .source(ocr().content())
        .mode("correction")
        .scope("pagewise")
        .sourceMap("preserve")
        .llmTier("mini")
        .build());

PipelineNodeRef extracted = pipelineBuilder.addNodeRef(PipelineNode.extract("extract_corrected")
        .dependsOn(ocrCorrected)
        .source(ocrCorrected.result().content())
        .prompt("Extract fields from the corrected OCR text.")
        .schema(Map.of(
                "type", "object",
                "properties", Map.of(
                        "name", Map.of("anyOf", List.of(
                                Map.of("type", "string"),
                                Map.of("type", "null"))))))
        .sourceLocations(true)
        .build());

PipelineNodeRef finalResult = pipelineBuilder.addNodeRef(PipelineNode.finalResult("final_result")
        .dependsOn(extracted)
        .output(extracted.result())
        .build());

PipelineDefinition pipeline = pipelineBuilder.finalOrder(finalResult).build();

System.out.println(pipeline.buildJsonNode().toPrettyString());
```

## Runtime Status

`GET /job/status/{guid}` includes a trimmed `pipeline` summary for pipeline jobs. Nodes include `id`, `type`, `status`, warnings, timestamps, and execution references such as `childOperationGuid` or `queuedTaskId` when present.

The main result still comes from `wait_until_ready()` / `GET /job/result/{guid}`. Source locations, when requested, are exposed through the existing `get_source_locations(guid)` Python helper or `GET /job/result/{guid}/source-locations`.

## Queue Mode

Request queue-backed extract nodes per pipeline with `Pipeline().execution_mode("queue")`. In queue mode, extract nodes run as separate node-scoped AI tasks. Node results and source locations are read the same way as in the default mode.
