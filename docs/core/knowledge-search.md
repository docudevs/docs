---
title: Knowledge Search
description: Enrich document extraction with case-specific lookup against your indexed knowledge bases.
sidebar_position: 5
---

Knowledge Search lets DocuDevs cite trusted facts from an indexed case while it extracts data from an incoming document. Instead of relying solely on the uploaded file, the extraction prompt can call a retrieval tool that queries your curated knowledge base (cases synchronized to the DocuDevs search index). This is ideal whenever documents contain shorthand identifiers, catalog numbers, or sparse descriptions that must be cross-referenced.

Unlike standard document extraction—which only considers the uploaded content—Knowledge Search injects results from your case index directly into the prompt chain. The worker automatically keeps the organization context, so every search remains tenant-scoped.

## When to use it

Use Knowledge Search when:

- Product catalogs or policy libraries evolve faster than your documents, and you need authoritative reference data during extraction.
- You maintain a case per customer or brand and want cross-document consistency (e.g., canonical marketing names, compliance tags, localized variants).
- You already curated a case that feeds the DocuDevs knowledge index via the indexing worker.

Stay with normal extraction when the uploaded file already contains every fact you need, or when you do not have a corresponding knowledge base.

## How it works

- **Step 1 — Prepare a case:** Create a case and upload reference documents via the DocuDevs UI or CLI (`docudevs cases create`, `docudevs cases upload-document`). Promote it to a knowledge base with `docudevs knowledge-base add <case_id>`.
- **Step 2 — Discover the case ID:** `docudevs knowledge-base list` (or the API `GET /knowledge-bases`) returns the IDs you can target. The Python SDK exposes the same CRUD surface as `list_knowledge_bases`, `get_knowledge_base`, `promote_knowledge_base`, and `delete_knowledge_base` on `DocuDevsClient`.
- **Step 3 — Attach a tool descriptor:** Pass `{"type":"KNOWLEDGE_BASE_SEARCH","config":{"caseId":"<id>","topK":5}}` in the `tools` of your extraction request.
- **Step 4 — Process documents:** The worker resolves the descriptor into a knowledge search tool wired to your case-specific index.
- **Step 5 — Review search hits:** Download the job result or open the processing timeline in DocuDevs to confirm when the knowledge search tool ran and what context it injected.

> **Tip:** `topK` defaults to 5. Increase it when the knowledge base stores short entries (SKUs, abbreviations) and you want broader recall.

## Example scenario: Specialty catalog enrichment

You maintain a "Signature Appliances" case (knowledge base `42`) that stores the canonical marketing copy, safety class, and localization guidance for every SKU. When suppliers send intake spreadsheets, you want DocuDevs to fill in the approved names and regulatory labels before the data hits your PIM. The example attaches the Knowledge Search tool to the extraction request.

<Example name="knowledge-search/extract-with-knowledge-base" />

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
| `caseId` | Required. A records knowledge base of your organization. |
| `topK` | Hits per lookup, 1 to 50 (default 10). The model can ask for fewer, never more. |
| `subtree` | Optional category code; only records labelled in that subtree are returned. |
| `attributes` | Optional `{name: value}` map of strings; only records with matching metadata are returned. |

Unknown keys are rejected. The request fails with `400` when the case is not a records knowledge base, `404` when it is not found. Each hit contains the record `key`, the matched text (`chunk`), the `label` and its `labelPath`, and the `score`. Search uses the knowledge base's active embedding provider and its search settings, and fails instead of falling back when the provider cannot be resolved.

<Example name="knowledge-search/extract-with-records-search" />

To match many values in one run, use a [classify job with the record target](../advanced/classification.md#matching-rows-to-records).

## Records Match

`RECORDS_MATCH` resolves one value to one record of a records knowledge base: the GL account of an invoice line, or the vendor a supplier name refers to. The model compares the value with the most similar records and returns the chosen record, or `declined` with a reason.

```json
{"type": "RECORDS_MATCH", "config": {"caseId": "51", "topK": 10, "llmLevel": "MINI"}}
```

| Config key | Meaning |
|------------|---------|
| `caseId` | Required. A records knowledge base of your organization. |
| `topK` | Candidate records the model compares, 1 to 50 (default 10). |
| `llmLevel` | `DEFAULT`, `NANO`, `MINI` (default) or `HIGH`. |

The result holds `key`, `label`, `labelPath`, `declined` and `reason`. Matching writes nothing: it creates no records, labels or memo entries. When the knowledge base has no active embedding generation the tool is not offered, and nothing falls back to another vector space. To match many values at once, use a [classify job with the record target](../advanced/classification.md#matching-rows-to-records).

## Operational notes

- **Security:** Every tool invocation is scoped to the authenticated organization and the supplied `caseId`. There is no cross-tenant leakage.
- **Fallback behavior:** If the worker cannot resolve the tool (missing case ID, search credentials), the job still runs but skips retrieval.

Once your case index is healthy, Knowledge Search becomes a drop-in upgrade for high-precision extractions—no need to redesign prompts or templates. Attach the tool descriptor, deploy, and watch your enrichment accuracy climb.
