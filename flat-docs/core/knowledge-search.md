---
title: Knowledge Search
description: Enrich document extraction with case-specific lookup against your indexed knowledge bases.
sidebar_position: 5
---
<!-- Generated from docs/core/knowledge-search.md. Do not edit by hand. -->

Knowledge Search lets DocuDevs cite trusted facts from an indexed case while it extracts data from an incoming document. Instead of relying solely on the uploaded file, the extraction prompt can call a retrieval tool that queries your curated knowledge base (cases synchronized to the DocuDevs search index). This is ideal whenever documents contain shorthand identifiers, catalogue numbers, or sparse descriptions that must be cross-referenced.

Unlike standard document extraction—which only considers the uploaded content—Knowledge Search injects results from your case index directly into the prompt chain. The worker automatically keeps the organization context, so every search remains tenant-scoped.

## When to use it

Use Knowledge Search when:

- Product catalogues or policy libraries evolve faster than your documents, and you need authoritative reference data during extraction.
- You maintain a case per customer or brand and want cross-document consistency (e.g., canonical marketing names, compliance tags, localized variants).
- You already curated a case that feeds the DocuDevs knowledge index via the indexing worker.

Stay with normal extraction when the uploaded file already contains every fact you need, or when you do not have a corresponding knowledge base.

## How it works

- **Step 1 — Prepare a case:** Create a case and upload reference documents via the DocuDevs UI or CLI (`docudevs cases create`, `docudevs cases upload-document`). Promote it to a knowledge base with `docudevs knowledge-base add <case_id>`. See [Knowledge bases](../advanced/cases.md#knowledge-bases) for what promotion and demotion change and who can do them.
- **Step 2 — Discover the case ID:** `docudevs knowledge-base list` (or the API `GET /knowledge-bases`) returns the IDs you can target. The Python SDK exposes the same CRUD surface as `list_knowledge_bases`, `get_knowledge_base`, `promote_knowledge_base`, and `delete_knowledge_base` on `DocuDevsClient`.
- **Step 3 — Attach a tool descriptor:** Pass `{"type":"KNOWLEDGE_BASE_SEARCH","config":{"caseId":"<id>","topK":5}}` in the `tools` of your extraction request.
- **Step 4 — Process documents:** The worker resolves the descriptor into a knowledge search tool wired to your case-specific index.
- **Step 5 — Review search hits:** Download the job result or open the processing timeline in DocuDevs to confirm when the knowledge search tool ran and what context it injected.

> **Tip:** `topK` defaults to 5. Increase it when the knowledge base stores short entries (SKUs, abbreviations) and you want broader recall.

## Example scenario: Specialty catalogue enrichment

You maintain a "Product Catalogue" case (knowledge base `42`) that stores the canonical marketing copy, product group, and localization guidance for every SKU. When suppliers send intake spreadsheets, you want DocuDevs to fill in the approved names and product groups before the data reaches your product database. The example attaches the Knowledge Search tool to the extraction request.

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

CASE_ID=42  # id of the knowledge base, from GET /knowledge-bases

SCHEMA='{
  "type": "object",
  "properties": {
    "records": {
      "type": "array",
      "items": {
        "type": "object",
        "properties": {
          "sku": {"type": "string"},
          "canonical_name": {"type": "string"},
          "localized_display_name": {"type": "string", "maxLength": 40},
          "product_group": {"type": "string"},
          "marketing_summary": {"type": "string"}
        },
        "required": ["sku", "canonical_name", "localized_display_name"]
      }
    }
  },
  "required": ["records"]
}'

JOB_GUID=$(curl -s -S -f -X POST "https://api.docudevs.ai/document/upload" \
  -H "Authorization: $API_KEY" \
  -F "document=@products.csv" | jq -r '.guid')

jq -n \
  --arg schema "$SCHEMA" \
  --arg caseId "$CASE_ID" \
  '{
    mimeType: "text/csv",
    prompt: "Fill canonical_name, localized_display_name (at most 40 characters), product_group and marketing_summary for every SKU, using the Product Catalogue knowledge base.",
    schema: $schema,
    tools: [{type: "KNOWLEDGE_BASE_SEARCH", config: {caseId: $caseId, topK: 8}}]
  }' |
  curl -s -S -f -o /dev/null -X POST "https://api.docudevs.ai/document/process/$JOB_GUID" \
    -H "Authorization: $API_KEY" \
    -H "Content-Type: application/json" \
    --data-binary @-

wait_for_job "$JOB_GUID"

curl -s -S -f "https://api.docudevs.ai/job/result/$JOB_GUID/json" \
  -H "Authorization: $API_KEY" | jq .
```

**Python**

```python
from pydantic import BaseModel, Field

from docudevs import json_schema

case_id = "42"  # id of the knowledge base, from list_knowledge_bases

class ProductRecord(BaseModel):
    sku: str
    canonical_name: str
    localized_display_name: str = Field(max_length=40)
    product_group: str | None = None
    marketing_summary: str | None = None

class ProductRecords(BaseModel):
    records: list[ProductRecord]

tool = {"type": "KNOWLEDGE_BASE_SEARCH", "config": {"caseId": case_id, "topK": 8}}

with open("products.csv", "rb") as f:
    document_data = f.read()

job_id = await client.submit_and_process_document(
    document=document_data,
    document_mime_type="text/csv",
    prompt=(
        "Fill canonical_name, localized_display_name (at most 40 characters), "
        "product_group and marketing_summary for every SKU, "
        "using the Product Catalogue knowledge base."
    ),
    schema=json_schema(ProductRecords),
    tools=[tool],
)

result = await client.wait_until_ready(job_id, result_format="json")
print(json.dumps(result, indent=2))
```

**Java**

```java
import ai.docudevs.client.ProcessOptions;
import ai.docudevs.client.ToolDescriptor;
import ai.docudevs.client.UploadRequest;
import ai.docudevs.client.WaitOptions;
import com.fasterxml.jackson.databind.JsonNode;

String caseId = "42"; // id of the knowledge base, from GET /knowledge-bases

String schema = """
        {
          "type": "object",
          "properties": {
            "records": {
              "type": "array",
              "items": {
                "type": "object",
                "properties": {
                  "sku": {"type": "string"},
                  "canonical_name": {"type": "string"},
                  "localized_display_name": {"type": "string", "maxLength": 40},
                  "product_group": {"type": "string"},
                  "marketing_summary": {"type": "string"}
                },
                "required": ["sku", "canonical_name", "localized_display_name"]
              }
            }
          },
          "required": ["records"]
        }
        """;

byte[] fileBytes = Files.readAllBytes(Path.of("products.csv"));
UploadRequest upload = new UploadRequest("products.csv", "text/csv", fileBytes);

ProcessOptions options = ProcessOptions.builder()
        .mimeType("text/csv")
        .prompt("Fill canonical_name, localized_display_name (at most 40 characters), "
                + "product_group and marketing_summary for every SKU, "
                + "using the Product Catalogue knowledge base.")
        .schema(schema)
        .tools(List.of(ToolDescriptor.knowledgeBaseSearch(caseId, 8)))
        .build();

String guid = client.submitAndProcessDocument(upload, options);
JsonNode result = client.waitUntilReadyJson(guid, WaitOptions.builder()
        .timeout(Duration.ofSeconds(180))
        .pollInterval(Duration.ofSeconds(5))
        .build());
System.out.println(result);
```

**CLI**

```bash
CASE_ID=42  # id of the knowledge base, from "docudevs knowledge-base list"

docudevs process products.csv \
  --mime-type text/csv \
  --prompt "Fill canonical_name, localized_display_name (at most 40 characters), product_group and marketing_summary for every SKU, using the Product Catalogue knowledge base." \
  --schema '{"type":"object","properties":{"records":{"type":"array","items":{"type":"object","properties":{"sku":{"type":"string"},"canonical_name":{"type":"string"},"localized_display_name":{"type":"string","maxLength":40},"product_group":{"type":"string"},"marketing_summary":{"type":"string"}},"required":["sku","canonical_name","localized_display_name"]}}},"required":["records"]}' \
  --tool '{"type":"KNOWLEDGE_BASE_SEARCH","config":{"caseId":"'"$CASE_ID"'","topK":8}}'
```

The `tools` array can include several descriptors; Knowledge Search is one of them. The CLI repeats `--tool` to stack descriptors. The server checks that the case belongs to your organization before it runs the search.

## Knowledge Base Evaluation

For deeper analysis scenarios — such as compliance checking, document completeness audits, or cross-document reasoning — use the `KNOWLEDGE_BASE_EVALUATE` tool type. Unlike the lightweight search tool, evaluation gives the LLM access to full document summaries and content, with a strategy pre-phase that determines the optimal approach.

### Configuration

```json
{
  "tools": [
    {
      "type": "KNOWLEDGE_BASE_EVALUATE",
      "config": {
        "caseId": "123",
        "strategy": "auto"
      }
    }
  ]
}
```

### Available Strategies

| Strategy | Description | Best for |
|----------|-------------|----------|
| `auto` | LLM reviews document summaries and picks the best strategy | General use — recommended default |
| `full_read` | Browses summaries, then reads full document text | Small document sets, compliance audits |
| `search` | Semantic + vector search only | Large cases, specific fact retrieval |
| `hybrid` | All tools available (browse, read, search) | Complex multi-document reasoning |

### How Auto Strategy Works

1. The system first retrieves AI-generated summaries of all documents in the case
2. A strategy-resolution LLM reviews the summaries and the extraction task
3. Based on document count, types, and the task requirements, it selects `full_read`, `search`, or `hybrid`
4. Only the selected tools are made available to the extraction LLM

### Tools Provided to the LLM

| Tool | Strategies | Description |
|------|-----------|-------------|
| `kb_browse_summaries` | full_read, hybrid | Lists AI-generated summaries for all case documents |
| `kb_read_document` | full_read, hybrid | Reads the full OCR text of a specific document by ID |
| `kb_search` | search, hybrid | Semantic + vector search across all case documents |

:::tip When to use Evaluate vs Search
Use **KNOWLEDGE_BASE_SEARCH** when you need fast, targeted lookups (e.g., "find the price for item X"). Use **KNOWLEDGE_BASE_EVALUATE** when the LLM needs to reason across documents or discover what's missing (e.g., "verify all required compliance sections are present").
:::

## Records Search

When the reference data is a [records dataset](../advanced/records-datasets.md) (a catalogue, a chart of accounts, a vendor master) use the `RECORDS_SEARCH` tool type. The model looks up a value from the document among your records and gets back the most similar ones with their category labels, so a schema field such as the GL account of an invoice line or the catalogue product of an order line can be resolved while the document is extracted.

```json
{
  "tools": [
    {
      "type": "RECORDS_SEARCH",
      "config": {
        "caseId": "51",
        "topK": 10,
        "subtree": "4.12",
        "attributes": {"brand": "Acme"}
      }
    }
  ]
}
```

| Config key | Meaning |
|------------|---------|
| `caseId` | Required. A records case of your organization that the organization can reach: organization-wide or a knowledge base. |
| `topK` | Hits per lookup, 1 to 50 (default 10). The model can ask for fewer, never more. |
| `subtree` | Optional category code; only records labelled in that subtree are returned. |
| `attributes` | Optional `{name: value}` map of strings; only records with matching metadata are returned. |

Unknown keys are rejected. The request fails with `400` when the case has no records dataset, `404` when it is not found or is not one the organization can reach (see [Cases](../advanced/cases.md#knowledge-bases)). Each hit contains the record `key`, the matched text (`chunk`), the `label` and its `labelPath`, and the `score`. Search uses the knowledge base's active embedding provider and its search settings, and fails instead of falling back when the provider cannot be resolved.

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

CASE_ID=51  # id of the records knowledge base, from GET /knowledge-bases

SCHEMA='{
  "type": "object",
  "properties": {
    "lines": {
      "type": "array",
      "items": {
        "type": "object",
        "properties": {
          "description": {"type": "string"},
          "product_key": {"type": "string"}
        }
      }
    }
  }
}'

JOB_GUID=$(curl -s -S -f -X POST "https://api.docudevs.ai/document/upload" \
  -H "Authorization: $API_KEY" \
  -F "document=@invoice.pdf" | jq -r '.guid')

jq -n \
  --arg schema "$SCHEMA" \
  --arg caseId "$CASE_ID" \
  '{
    mimeType: "application/pdf",
    prompt: "For each invoice line, fill product_key with the catalogue record that matches the item.",
    schema: $schema,
    tools: [{type: "RECORDS_SEARCH", config: {caseId: $caseId, topK: 5}}]
  }' |
  curl -s -S -f -o /dev/null -X POST "https://api.docudevs.ai/document/process/$JOB_GUID" \
    -H "Authorization: $API_KEY" \
    -H "Content-Type: application/json" \
    --data-binary @-

wait_for_job "$JOB_GUID"

curl -s -S -f "https://api.docudevs.ai/job/result/$JOB_GUID/json" \
  -H "Authorization: $API_KEY" | jq .
```

**Python**

```python
from pydantic import BaseModel

from docudevs import json_schema

case_id = "51"  # id of the records knowledge base, from list_knowledge_bases

class InvoiceLine(BaseModel):
    description: str | None = None
    product_key: str | None = None

class Invoice(BaseModel):
    lines: list[InvoiceLine] = []

tool = {"type": "RECORDS_SEARCH", "config": {"caseId": case_id, "topK": 5}}

with open("invoice.pdf", "rb") as f:
    document_data = f.read()

job_id = await client.submit_and_process_document(
    document=document_data,
    document_mime_type="application/pdf",
    prompt="For each invoice line, fill product_key with the catalogue record that matches the item.",
    schema=json_schema(Invoice),
    tools=[tool],
)

result = await client.wait_until_ready(job_id, result_format="json")
print(json.dumps(result, indent=2))
```

**Java**

```java
import ai.docudevs.client.ProcessOptions;
import ai.docudevs.client.ToolDescriptor;
import ai.docudevs.client.UploadRequest;
import ai.docudevs.client.WaitOptions;
import com.fasterxml.jackson.databind.JsonNode;

String caseId = "51"; // id of the records knowledge base, from listKnowledgeBases

String schema = """
        {
          "type": "object",
          "properties": {
            "lines": {
              "type": "array",
              "items": {
                "type": "object",
                "properties": {
                  "description": {"type": "string"},
                  "product_key": {"type": "string"}
                }
              }
            }
          }
        }
        """;

ToolDescriptor tool = new ToolDescriptor("RECORDS_SEARCH", null, Map.of("caseId", caseId, "topK", 5));

byte[] documentData = Files.readAllBytes(Path.of("invoice.pdf"));

String jobId = client.submitAndProcessDocument(
        new UploadRequest("invoice.pdf", "application/pdf", documentData),
        ProcessOptions.builder()
                .mimeType("application/pdf")
                .prompt("For each invoice line, fill product_key with the catalogue record that matches the item.")
                .schema(schema)
                .tools(List.of(tool))
                .build());

JsonNode result = client.waitUntilReadyJson(
        jobId,
        WaitOptions.builder()
                .timeout(Duration.ofSeconds(600))
                .pollInterval(Duration.ofSeconds(5))
                .build());
System.out.println(result.toPrettyString());
```

**CLI**

```bash
CASE_ID=51  # id of the records knowledge base, from "docudevs knowledge-base list"

docudevs process invoice.pdf \
  --mime-type application/pdf \
  --prompt "For each invoice line, fill product_key with the catalogue record that matches the item." \
  --schema '{"type":"object","properties":{"lines":{"type":"array","items":{"type":"object","properties":{"description":{"type":"string"},"product_key":{"type":"string"}}}}}}' \
  --tool '{"type":"RECORDS_SEARCH","config":{"caseId":"'"$CASE_ID"'","topK":5}}'
```

To match many values in one run, use a [classify job with the record target](../advanced/classification.md#matching-rows-to-records).

## Records Match

`RECORDS_MATCH` resolves one value to one record of a records case: the GL account of an invoice line, or the vendor a supplier name refers to. The model compares the value with the most similar records and returns the chosen record, or `declined` with a reason.

```json
{"type": "RECORDS_MATCH", "config": {"caseId": "51", "topK": 10, "llmLevel": "MINI"}}
```

| Config key | Meaning |
|------------|---------|
| `caseId` | Required. A records case of your organization that the organization can reach: organization-wide or a knowledge base. |
| `topK` | Candidate records the model compares, 1 to 50 (default 10). |
| `llmLevel` | `DEFAULT`, `NANO`, `MINI` (default) or `HIGH`. |

The result holds `key`, `label`, `labelPath`, `declined` and `reason`. Matching writes nothing: it creates no records, labels or memo entries. When the knowledge base has no active embedding generation the tool is not offered, and nothing falls back to another vector space. To match many values at once, use a [classify job with the record target](../advanced/classification.md#matching-rows-to-records).

## Operational notes

- **Security:** Every tool invocation is scoped to the authenticated organization and the supplied `caseId`. There is no cross-tenant leakage.
- **Fallback behavior:** If the worker cannot resolve the tool (missing case ID, search credentials), the job still runs but skips retrieval.

Once your case index is healthy, Knowledge Search becomes a drop-in upgrade for high-precision extractions—no need to redesign prompts or templates. Attach the tool descriptor, deploy, and watch your enrichment accuracy climb.
