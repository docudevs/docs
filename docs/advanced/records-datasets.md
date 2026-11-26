---
title: Records datasets and field mapping
description: Turn a case into a searchable records dataset and map your source columns to key, text, label and metadata.
sidebar_position: 2
---

# Records datasets and field mapping

A **records dataset** stores structured rows (for example a product catalogue) in a case and indexes them for semantic search. Each record has a unique key, one or more text fields that are embedded, an optional category label, and optional metadata. A dataset can only be configured on a case that has no documents, and a records case is meant to hold records only: do not upload documents to it.

Writes are asynchronous: DocuDevs stages your request and a worker applies it. Reads and search are synchronous. All endpoints live under `/cases/{caseId}` and use the same API key as the rest of the API. With an organization API key, the case must be one that the key can use; see [Which cases you can use](./case-agent.md#which-cases-you-can-use).

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
    "metadata": {"brand": "brand", "productGroup": "product_group"}
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
| `classifyDefaults.targetDepth` | Tree levels a category classify job returns when it names none, for example `[4, 5]`. A non-empty list of distinct whole numbers of at least 1. Changing it does not reindex. A PUT that omits it clears it. See [Classification](./classification.md#config). |

Changing the text fields or the embedded text template of a dataset that already holds records changes what future writes embed. The call is refused with `409` unless you confirm with `?reindex=true`. Confirming records the change (the dataset's `mappingVersion` increases) but does not re-embed existing records: they keep their previous embedded text until you write them again, for example with a snapshot sync of the full export, which re-embeds every record whose embedded text changed. Changing only the label, metadata, example policy or index settings needs no confirmation.

A label-only or metadata-only change to a record never calls the embedding model: DocuDevs hashes the embedded text and re-embeds only when the hash changes.

<Example name="records/configure-dataset" />

The example is a reduced form of the configuration shown above: it keeps the key, both text fields, the label, the `brand` metadata, the embedded text template and the `topK` and `hybrid` index settings, and leaves the other settings at their defaults. `text` maps a source column to a field name. The Python client accepts `{column: name}` or a list of `{"column", "name"}` entries, and it takes option names in `snake_case`. A change that needs confirmation is refused with `409` until you repeat it with `reindex` (`?reindex=true`, the `reindex` keyword argument, or `--reindex` on the CLI).

Read the configuration back with `GET /cases/{caseId}/dataset`. The response adds the `mappingVersion` and, once a category tree is active, `activeTreeVersionId`.

<Example name="records/get-dataset" />

## Create a records case in one call

The Python client can create the case and configure its dataset in one call. `create_records_case` takes the case name, the source column of the key (`key`), the text columns (`text`, a list of names or a `{column: name}` map) and optionally `label`, `metadata`, `embedded_text`, `target_depth`, `index_settings` and `description`. Without `embedded_text` the embedded text is the text fields, one per line, with empty fields skipped; a string is used as the template. `target_depth` becomes `classifyDefaults.targetDepth`. The other languages make the two requests the example shows.

<Example name="records/create-records-case" />

The call returns a `RecordsCase` handle. `client.records_case(case_id)` returns the same kind of handle for an existing case and makes no request. The handle binds the case id to the methods you use most:

| Method | What it does |
| --- | --- |
| `import_records(rows)`, `import_records_and_wait(rows)` | Upsert records from a file path or from rows keyed by the dataset's source columns. The first returns the import, the second waits for it to finish. |
| `search(query, **settings)` | Search the records of the case. |
| `get_record(key)` | Read one record with its label, label source and indexed text. |
| `upload_tree(nodes, name=None)`, `activate_tree(version_or_nodes, ...)`, `set_tree_and_wait(nodes, ...)` | Upload a category tree, activate a version (or upload a node list and activate it), or do both and wait. See [Category trees](./category-trees.md). |
| `classify(rows, keys, ...)`, `classify_and_wait(rows, keys, ...)` | Start a [classify job](./classification.md), with or without waiting. |
| `holdout_eval(variants, ...)`, `holdout_eval_and_wait(variants, ...)` | Start a holdout evaluation, with or without waiting. See [Case evaluations](./case-evals.md). |
| `agent_run(playbook, ...)`, `agent_run_and_wait(playbook, ...)`, `conversation(...)`, `pending_actions()` | Work with the [case agent](./case-agent.md). |
| `memo` | The case's [classification memo](./case-memo.md). |
| `delete()` | Delete the case. It raises an error when the server does not delete it, for example while an import or activation is running. |

Methods that start a job return a handle to it; the `_and_wait` variants block until the job finishes and raise an error when it fails.

## Single records and batches

<Example name="records/upsert-record" />

Each write returns `{"importId", "jobGuid", "status"}`. The label source is `import` (default) or `review` (`labelSource` in the request, `label_source` in the Python client); records marked `review` are treated as human decisions by snapshot syncs (see [Importing records](./records-import.md#snapshot-sync)).

<Example name="records/delete-record" />

A batch (`POST /cases/{caseId}/records:batch`) holds up to 1,000 upserts and deletes and 10 MB. Larger inputs are refused with `413`; use a [file import](./records-import.md#file-import). The command-line client has no batch command; `docudevs records bulk` imports a file instead.

<Example name="records/batch-records" />

## Reading records

<Example name="records/get-record" />

`GET /cases/{caseId}/records/{key}` returns the record with its `label`, `labelPath`, `labelSource` and the embedded text (`chunk`).

<Example name="records/list-records" />

`GET /cases/{caseId}/records` pages through records ordered by key. Filter with `label`, `subtree`, `labelSource` and repeated `attribute=name=value`. `limit` is 1-200 (default 50). Pass the returned `continuation` token to fetch the next page; there is no depth limit. The cURL variant reads one page; the Python client and the CLI (`--all`) follow the tokens for you.

<Example name="records/record-stats" />

`GET /cases/{caseId}/records/stats` returns record counts per label, split by `labelSource`: for each code, `import`, `review` and `total`.
