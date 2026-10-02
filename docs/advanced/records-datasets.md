---
title: Records datasets and field mapping
description: Turn a case into a searchable records dataset and map your source columns to key, text, label and metadata.
sidebar_position: 2
---

# Records datasets and field mapping

A **records dataset** stores structured rows (for example a product catalogue) in a case and indexes them for semantic search. Each record has a unique key, one or more text fields that are embedded, an optional category label, and optional metadata. A dataset can only be configured on a case that has no documents, and a records case is meant to hold records only: do not upload documents to it.

Writes are asynchronous: DocuDevs stages your request and a worker applies it. Reads and search are synchronous. All endpoints live under `/cases/{caseId}` and use the same API key as the rest of the API.

## Configure the dataset

`PUT /cases/{caseId}/dataset` configures the dataset of a case that has no documents. `GET /cases/{caseId}/dataset` returns the current configuration.

```json
{
  "datasetType": "records",
  "fieldMapping": {
    "key": "sku",
    "text": [
      {"column": "title", "name": "title"},
      {"column": "desc", "name": "description"}
    ],
    "label": "category",
    "metadata": {"brand": "brand", "customsGroup": "customs_group"}
  },
  "embeddedText": {"template": "{title}\n{description}", "skipEmptyFields": true},
  "examplePolicy": {"mode": "perLabelCap", "perLabelCap": 3, "overFetch": 4},
  "indexSettings": {"topK": 10, "hybrid": false, "oversampling": 10}
}
```

| Part | Meaning |
| --- | --- |
| `fieldMapping.key` | Source column holding the unique record key (at most 512 characters). |
| `fieldMapping.text` | Source columns that are embedded. `name` is the field name used in the embedded text template and in `text` of single and batch writes. Names are unique. |
| `fieldMapping.label` | Optional column holding the category code. |
| `fieldMapping.metadata` | Optional `{name: column}` map. Metadata is filterable in search. Names must not start with `_`; that prefix is reserved for DocuDevs. |
| `embeddedText.template` | Text that is embedded, using `{name}` placeholders. The default is one line per text field. With `skipEmptyFields` (default), a template line whose placeholders are empty is dropped. |
| `examplePolicy` | `mode` is `full` (default) or `perLabelCap`, which keeps at most `perLabelCap` hits per label in search results and fetches `overFetch` times `topK` hits first. |
| `indexSettings` | Default search settings: `topK` 1-200 (default 10), `exhaustive`, `oversampling` 1.0-20.0 (default 10), `hybrid` (default false), `semanticRerank` (default false). A search call can override them. |

Changing the text fields or the embedded text template of a dataset that already holds records changes what future writes embed. The call is refused with `409` unless you confirm with `?reindex=true`. Confirming records the change (the dataset's `mappingVersion` increases) but does not re-embed existing records: they keep their previous embedded text until you write them again, for example with a snapshot sync of the full export, which re-embeds every record whose embedded text changed. Changing only the label, metadata, example policy or index settings needs no confirmation.

A label-only or metadata-only change to a record never calls the embedding model: DocuDevs hashes the embedded text and re-embeds only when the hash changes.

## Python SDK

```python
from docudevs import DocuDevsClient

client = DocuDevsClient(api_url="https://api.docudevs.ai", token=token)

await client.set_dataset(
    case_id,
    {
        "key": "sku",
        "text": {"title": "title", "desc": "description"},
        "label": "category",
        "metadata": {"brand": "brand"},
    },
    embedded_text={"template": "{title}\n{description}"},
    index_settings={"top_k": 10, "hybrid": False},
)
dataset = await client.get_dataset(case_id)
```

`text` accepts `{column: name}` or a list of `{"column", "name"}` entries. Option names may use `snake_case`.

## curl

```bash
curl -X PUT "https://api.docudevs.ai/cases/42/dataset" \
  -H "Authorization: Bearer $TOKEN" -H "Content-Type: application/json" \
  -d '{"datasetType":"records","fieldMapping":{"key":"sku","text":[{"column":"title","name":"title"}],"label":"category"}}'
```

## Single records and batches

```python
await client.upsert_record(
    case_id, "6410000123456",
    {"title": "Soft sock 3-pack", "description": "Cotton, size 40-45"},
    label="4.12.3", metadata={"brand": "Foo"},
)
await client.delete_record(case_id, "6410000123456")
await client.batch_records(case_id, upserts=[...], deletes=["k1", "k2"])
```

Each write returns `{"importId", "jobGuid", "status"}`. `label_source` is `import` (default) or `review`; records marked `review` are treated as human decisions by snapshot syncs (see [Importing records](/docs/advanced/records-import)).

```bash
curl -X PUT "https://api.docudevs.ai/cases/42/records/6410000123456" \
  -H "Authorization: Bearer $TOKEN" -H "Content-Type: application/json" \
  -d '{"text":{"title":"Soft sock 3-pack"},"label":"4.12.3","labelSource":"import","metadata":{"brand":"Foo"}}'
```

A batch (`POST /cases/{caseId}/records:batch`) holds up to 1,000 upserts and deletes and 10 MB. Larger inputs are refused with `413`; use a file import.

## Reading records

```python
record = await client.get_record(case_id, "6410000123456")

async for record in client.list_records(case_id, subtree="4.12", attributes={"brand": "Foo"}):
    ...

stats = await client.record_stats(case_id)
```

- `GET /cases/{caseId}/records/{key}` returns the record with its `label`, `labelPath`, `labelSource` and the embedded text (`chunk`).
- `GET /cases/{caseId}/records` pages through records ordered by key. Filter with `label`, `subtree`, `labelSource` and repeated `attribute=name=value`. `limit` is 1-200 (default 50). Pass the returned `continuation` token to fetch the next page; there is no depth limit. `list_records` follows the tokens for you.
- `GET /cases/{caseId}/records/stats` returns record counts per label, split by `labelSource`.

## CLI

```bash
docudevs records dataset-set 42 dataset.json
docudevs records dataset-get 42
docudevs records upsert 42 6410000123456 --text title="Soft sock" --label 4.12.3
docudevs records list 42 --subtree 4.12 --all
docudevs records stats 42
```
