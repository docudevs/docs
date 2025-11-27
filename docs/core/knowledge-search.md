---
title: Knowledge Search
description: Enrich document extraction with case-specific lookup against your indexed knowledge bases.
sidebar_position: 5
---

import Tabs from '@theme/Tabs';
import TabItem from '@theme/TabItem';

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
- **Step 2 — Discover the case ID:** `docudevs knowledge-base list` (or the API `GET /knowledge-base`) returns the IDs you can target.
- **Step 3 — Attach a tool descriptor:** Pass `{"type":"KNOWLEDGE_BASE_SEARCH","config":{"caseId":"<id>","topK":5}}` alongside your extraction request.
- **Step 4 — Process documents:** The worker resolves the descriptor into a knowledge search tool wired to your case-specific index.
- **Step 5 — Review search hits:** Download the job result or open the processing timeline in DocuDevs to confirm when the knowledge search tool ran and what context it injected.

> **Tip:** `topK` defaults to 5. Increase it when the knowledge base stores short entries (SKUs, abbreviations) and you want broader recall.

## Example scenario: Specialty catalog enrichment

You maintain a "Signature Appliances" case (`kb-kitchen-pro`) that stores the canonical marketing copy, safety class, and localization guidance for every SKU. When suppliers send intake spreadsheets, you want DocuDevs to fill in the approved names and regulatory labels before the data hits your PIM. The following snippets show how to attach the Knowledge Search tool via REST, Python (with Pydantic models), and the DocuDevs CLI.

<Tabs
  defaultValue="python"
  values={[
    {label: 'Python SDK', value: 'python'},
    {label: 'CLI', value: 'cli'},
    {label: 'cURL', value: 'curl'},
  ]}>
  <TabItem value="python">

```python
import asyncio
from pathlib import Path
from pydantic import BaseModel, Field
import json

from docudevs.docudevs_client import DocuDevsClient

class CatalogRow(BaseModel):
    sku: str = Field(pattern=r"^SKU-\d{5}$")
    canonical_name: str = Field(max_length=80)
    localized_display_name: str = Field(max_length=40)
    regulatory_class: str
    marketing_summary: str | None = None

class CatalogResult(BaseModel):
    records: list[CatalogRow]

async def enrich_products():
    client = DocuDevsClient(token="YOUR_API_KEY")
    payload = Path("samples/products.csv").read_bytes()

    # Define the knowledge search tool as a simple dict
    tool = {
        "type": "KNOWLEDGE_BASE_SEARCH",
        "config": {
            "caseId": "kb-kitchen-pro",
            "topK": 8,
        }
    }

    job_id = await client.submit_and_process_document(
        document=payload,
        document_mime_type="text/csv",
        prompt=(
            "Fill canonical_name, localized_display_name (<=40 chars), regulatory_class, "
            "and marketing_summary using the Signature Appliances knowledge base."
        ),
        schema=json.dumps(CatalogResult.model_json_schema()),
        tools=[tool],
    )

    raw = await client.wait_until_ready(job_id, result_format="json")
    result = CatalogResult(**raw)
    print(result.records[0])

asyncio.run(enrich_products())
```

The Pydantic models ensure downstream code only sees validated names and compliance metadata. If a record fails validation (e.g., localized name exceeds 40 characters) you can catch the exception and decide whether to re-run with different prompts.

  </TabItem>
  <TabItem value="cli">

```bash
DOCUDEVS_API_URL="https://api.docudevs.ai"
DOCUDEVS_API_KEY="<your-api-key>"
CASE_ID="kb-kitchen-pro"

docudevs process ./samples/products.csv \
  --mime-type text/csv \
  --prompt "Enrich canonical name, localized display name, regulatory class, and marketing summary using the Signature Appliances case." \
  --schema-file schemas/appliance-catalog.json \
  --tool '{"type":"KNOWLEDGE_BASE_SEARCH","config":{"caseId":"'"$CASE_ID"'","topK":8}}'
```

You can repeat `--tool` to stack multiple descriptors (e.g., knowledge search plus other orchestration helpers).

Combine with `docudevs knowledge-base list` to fetch available case IDs at runtime.

  </TabItem>
  <TabItem value="curl">

```bash
API_URL="https://api.docudevs.ai"
API_KEY="<your-api-key>"
CASE_ID="kb-kitchen-pro"

curl -X POST "$API_URL/document/upload-files" \
  -H "Authorization: Bearer $API_KEY" \
  -F "document=@./samples/products.csv;type=text/csv" \
  -F 'payload={
    "prompt": "For each SKU, fill canonicalName, localizedDisplayName (<=40 chars), regulatoryClass, and marketingSummary based on the Signature Appliances knowledge base.",
    "schema": {
      "type": "object",
      "properties": {
        "records": {
          "type": "array",
          "items": {
            "type": "object",
            "properties": {
              "sku": {"type": "string"},
              "canonicalName": {"type": "string"},
              "localizedDisplayName": {"type": "string", "maxLength": 40},
              "regulatoryClass": {"type": "string"},
              "marketingSummary": {"type": "string"}
            },
            "required": ["sku", "canonicalName", "localizedDisplayName"]
          }
        }
      },
      "required": ["records"]
    },
    "tools": [
      {
        "type": "KNOWLEDGE_BASE_SEARCH",
        "config": {
          "caseId": "'"$CASE_ID"'",
          "topK": 8
        }
      }
    ]
  }'
```

The `tools` array can include multiple descriptors; Knowledge Search simply becomes one of them.

Server-side validation ensures the case belongs to your organization before executing the search.

  </TabItem>
</Tabs>

## Operational notes

- **Security:** Every tool invocation is scoped to the authenticated organization and the supplied `caseId`. There is no cross-tenant leakage.
- **Fallback behavior:** If the worker cannot resolve the tool (missing case ID, search credentials), the job still runs but skips retrieval.

Once your case index is healthy, Knowledge Search becomes a drop-in upgrade for high-precision extractions—no need to redesign prompts or templates. Attach the tool descriptor, deploy, and watch your enrichment accuracy climb.
