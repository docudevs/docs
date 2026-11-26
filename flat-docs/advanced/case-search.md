---
title: Case search
description: Search the records or documents of a case with one synchronous call, with settings, filters and limits.
sidebar_position: 5
---
<!-- Generated from docs/advanced/case-search.md. Do not edit by hand. -->

# Case search

`POST /cases/{caseId}/search` embeds your query with the case's embedding provider and searches the index in one call. It is not a job; the results come back in the response. `kind` selects what to search:

| `kind` | Searches |
| --- | --- |
| `records` | The records dataset of the case. Results carry the record `key`, its text (`chunk`), `label`, `labelPath`, `labelSource` and `attributes`. |
| `documents` | The documents of a case without a records dataset. Results carry `documentId`, `chunk`, `filename` and `pageNumber`. |

A `records` search on a case without a dataset, or a `documents` search on a records case, returns `409`. So does a `records` search on a case that has no active embedding generation.

## Request

```json
{
  "kind": "records",
  "query": "Soft sports sock 3-pack",
  "topK": 10,
  "hybrid": false,
  "oversampling": 10,
  "filters": {
    "subtree": "4.12",
    "attributes": {"brand": "Foo"},
    "labelSource": "review"
  }
}
```

Settings are flat fields on the request and override the dataset's `indexSettings`; omit them to use the defaults.

| Field | Meaning | Limits |
| --- | --- | --- |
| `topK` | Number of results. | 1-200 |
| `exhaustive` | Exhaustive vector search instead of the approximate index. | Refused above 1,000,000 records; slower with no quality gain in measurements. |
| `oversampling` | Rescoring oversampling. | 1.0-20.0 |
| `hybrid` | Combine text and vector retrieval. | Default off for records, on for documents. |
| `semanticRerank` | Apply the semantic ranker. | Default off for records, on for documents. |

Filters:

- `subtree`: a category code; matches records whose label is that category or one of its descendants (needs an activated tree).
- `attributes`: `{name: value}` metadata equality filters.
- `labelSource`: `import` or `review`.
- `documentIds`: for `documents`, restrict to these job ids.

Out-of-range settings or an empty query return `400`. Errors from the embedding provider or the search index return `502`; missing search or credential setup returns `503`.

## Response

```json
{
  "kind": "records",
  "generationId": 9,
  "results": [
    {"key": "1000000000001", "score": 0.83, "chunk": "Soft sock 3-pack\nCotton, size 40-45",
     "label": "4.12.3", "labelPath": ["4", "4.12", "4.12.3"], "labelSource": "import",
     "attributes": ["brand=Foo"]}
  ],
  "settingsUsed": {"topK": 10, "exhaustive": false, "oversampling": 10.0, "hybrid": false, "semanticRerank": false}
}
```

`settingsUsed` shows the settings that were applied after defaults. With `examplePolicy.mode = perLabelCap` the results hold at most that many records per label.

## Search a records case

As with the other case calls, an API key reaches organization-wide cases and knowledge bases; a case created by a signed-in user answers `404` (see [Case agent](./case-agent.md#which-cases-you-can-use)).

**cURL**

```bash
CASE_ID=42  # id of a records case

curl -s -S -f -X POST "https://api.docudevs.ai/cases/$CASE_ID/search" \
  -H "Authorization: $API_KEY" \
  -H "Content-Type: application/json" \
  -d '{
    "kind": "records",
    "query": "Soft sports sock",
    "topK": 10,
    "filters": {"subtree": "4.12", "attributes": {"brand": "Foo"}}
  }' | jq -r '.results[]? | "\(.key) \(.label) \(.score)"'
```

**Python**

```python
import asyncio
import os

from docudevs.docudevs_client import DocuDevsClient


async def main():
    client = DocuDevsClient(token=os.environ["API_KEY"])

    case_id = 42  # id of a records case

    hits = await client.search_records(
        case_id,
        "Soft sports sock",
        top_k=10,
        filters={"subtree": "4.12", "attributes": {"brand": "Foo"}},
    )

    for result in hits.get("results") or []:
        print(result["key"], result.get("label"), result.get("score"))


asyncio.run(main())
```

**Java**

```java
import ai.docudevs.client.CaseSearchOptions;
import com.fasterxml.jackson.databind.JsonNode;

long caseId = 42L; // id of a records case

JsonNode hits = client.searchRecords(
        caseId,
        "Soft sports sock",
        CaseSearchOptions.builder()
                .topK(10)
                .subtree("4.12")
                .attributes(Map.of("brand", "Foo"))
                .build());

for (JsonNode result : hits.path("results")) {
    System.out.println(result.path("key").asText() + " " + result.path("label").asText(null)
            + " " + result.path("score").asText(null));
}
```

**CLI**

```bash
docudevs records search 42 "Soft sports sock" --kind records --top-k 10 --subtree 4.12 --attribute brand=Foo
```

The Python SDK takes the settings as keyword arguments (`top_k`, `exhaustive`, `oversampling`, `hybrid`, `semantic_rerank`) and `filters` as a dictionary. The SDK also has a `search` method that takes the kind as an argument. The Java client takes the settings and filters in `CaseSearchOptions` (`topK`, `exhaustive`, `oversampling`, `hybrid`, `semanticRerank`, `subtree`, `attributes`) and has `searchRecords`, `searchDocuments` and `search`.

## Search the documents of a case

**cURL**

```bash
CASE_ID=42  # id of a case that holds documents and no records dataset

curl -s -S -f -X POST "https://api.docudevs.ai/cases/$CASE_ID/search" \
  -H "Authorization: $API_KEY" \
  -H "Content-Type: application/json" \
  -d '{"kind": "documents", "query": "termination notice period", "topK": 5}' \
  | jq -r '.results[]? | "\(.filename) page \(.pageNumber)"'
```

**Python**

```python
case_id = 42  # id of a case that holds documents and no records dataset

docs = await client.search_documents(case_id, "termination notice period", top_k=5)

for result in docs.get("results") or []:
    print(result.get("filename"), "page", result.get("pageNumber"))
```

**Java**

```java
import ai.docudevs.client.CaseSearchOptions;
import com.fasterxml.jackson.databind.JsonNode;

long caseId = 42L; // id of a case that holds documents and no records dataset

JsonNode docs = client.searchDocuments(
        caseId,
        "termination notice period",
        CaseSearchOptions.builder().topK(5).build());

for (JsonNode result : docs.path("results")) {
    System.out.println(result.path("filename").asText(null) + " page " + result.path("pageNumber").asText(null));
}
```

**CLI**

```bash
docudevs records search 42 "termination notice period" --kind documents --top-k 5
```

## Billing

Each search embeds the query once and records one embedding usage event for your organization, measured in characters. Query embedding is not charged against your credits.
