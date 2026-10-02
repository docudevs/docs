---
title: Case search
description: Search the records or documents of a case with one synchronous call, with settings, filters and limits.
sidebar_position: 5
---

# Case search

`POST /cases/{caseId}/search` embeds your query with the case's embedding provider and searches the index in one call. It is not a job; the results come back in the response. `kind` selects what to search:

| `kind` | Searches |
| --- | --- |
| `records` | The records dataset of the case. Results carry the record `key`, its text (`chunk`), `label`, `labelPath`, `labelSource` and `attributes`. |
| `documents` | The documents of a case without a records dataset. Results carry `documentId`, `chunk`, `filename` and `pageNumber`. |

A `records` search on a case without a dataset, or a `documents` search on a records case, returns `409`.

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
    {"key": "6410000123456", "score": 0.83, "chunk": "Soft sock 3-pack\nCotton, size 40-45",
     "label": "4.12.3", "labelPath": ["4", "4.12", "4.12.3"], "labelSource": "import",
     "attributes": ["brand=Foo"]}
  ],
  "settingsUsed": {"topK": 10, "exhaustive": false, "oversampling": 10.0, "hybrid": false, "semanticRerank": false}
}
```

`settingsUsed` shows the settings that were applied after defaults. With `examplePolicy.mode = perLabelCap` the results hold at most that many records per label.

## Python SDK and CLI

```python
hits = await client.search_records(case_id, "Soft sports sock", top_k=10,
                                   filters={"subtree": "4.12", "attributes": {"brand": "Foo"}})
docs = await client.search_documents(case_id, "termination notice period")
# or: await client.search(case_id, "records", "query", hybrid=False)
```

```bash
docudevs records search 42 "Soft sports sock" --kind records --top-k 10 --subtree 4.12 --attribute brand=Foo
```

```bash
curl -X POST "https://api.docudevs.ai/cases/42/search" \
  -H "Authorization: Bearer $TOKEN" -H "Content-Type: application/json" \
  -d '{"kind":"records","query":"Soft sports sock","topK":5}'
```

## Billing

Each search embeds the query once and records one embedding usage event for your organization, measured in characters. Searches that use a provider you bring yourself are not debited from DocuDevs token balances.
