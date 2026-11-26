---
title: Records datasets and field mapping
description: Turn a case into a searchable records dataset and map your source columns to key, text, label and metadata.
sidebar_position: 2
---
<!-- Generated from docs/advanced/records-datasets.md. Do not edit by hand. -->

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

**cURL**

```bash
CASE_ID=42  # id of a case that holds no documents

curl -s -S -f -X PUT "https://api.docudevs.ai/cases/$CASE_ID/dataset" \
  -H "Authorization: $API_KEY" \
  -H "Content-Type: application/json" \
  -d '{
    "datasetType": "records",
    "fieldMapping": {
      "key": "sku",
      "text": [
        {"column": "title", "name": "title"},
        {"column": "desc", "name": "description"}
      ],
      "label": "category",
      "metadata": {"brand": "brand"}
    },
    "embeddedText": {"template": "{title}\n{description}"},
    "indexSettings": {"topK": 10, "hybrid": false}
  }' | jq '{datasetType, mappingVersion}'
```

**Python**

```python
import asyncio
import os

from docudevs.docudevs_client import DocuDevsClient


async def main():
    client = DocuDevsClient(token=os.environ["API_KEY"])

    case_id = 42  # id of a case that holds no documents

    # Raises RecordsApiError when the request is refused
    dataset = await client.set_dataset(
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
    print(f"Mapping version: {dataset['mappingVersion']}")


asyncio.run(main())
```

**Java**

```java
import ai.docudevs.client.DatasetOptions;
import com.fasterxml.jackson.databind.JsonNode;

long caseId = 42L; // id of a case that holds no documents

// Throws DocuDevsApiException when the request is refused
JsonNode dataset = client.setDataset(
        caseId,
        Map.of(
                "key", "sku",
                "text", Map.of("title", "title", "desc", "description"),
                "label", "category",
                "metadata", Map.of("brand", "brand")),
        DatasetOptions.builder()
                .embeddedText(Map.of("template", "{title}\n{description}"))
                .indexSettings(Map.of("top_k", 10, "hybrid", false))
                .build());
System.out.println("Mapping version: " + dataset.path("mappingVersion").asText());
```

**CLI**

```bash
CASE_ID=42  # id of a case that holds no documents

cat > dataset.json <<'JSON'
{
  "datasetType": "records",
  "fieldMapping": {
    "key": "sku",
    "text": [
      {"column": "title", "name": "title"},
      {"column": "desc", "name": "description"}
    ],
    "label": "category",
    "metadata": {"brand": "brand"}
  },
  "embeddedText": {"template": "{title}\n{description}"},
  "indexSettings": {"topK": 10, "hybrid": false}
}
JSON

docudevs records dataset-set "$CASE_ID" dataset.json
```

The example is a reduced form of the configuration shown above: it keeps the key, both text fields, the label, the `brand` metadata, the embedded text template and the `topK` and `hybrid` index settings, and leaves the other settings at their defaults. `text` maps a source column to a field name. The Python client accepts `{column: name}` or a list of `{"column", "name"}` entries, and it takes option names in `snake_case`. A change that needs confirmation is refused with `409` until you repeat it with `reindex` (`?reindex=true`, the `reindex` keyword argument, or `--reindex` on the CLI).

Read the configuration back with `GET /cases/{caseId}/dataset`. The response adds the `mappingVersion` and, once a category tree is active, `activeTreeVersionId`.

**cURL**

```bash
CASE_ID=42  # id of a records case

curl -s -S -f "https://api.docudevs.ai/cases/$CASE_ID/dataset" \
  -H "Authorization: $API_KEY" | jq '{mappingVersion, activeTreeVersionId, fieldMapping}'
```

**Python**

```python
case_id = 42  # id of a records case

dataset = await client.get_dataset(case_id)
print(f"Mapping version: {dataset['mappingVersion']}")
print(f"Active tree version: {dataset.get('activeTreeVersionId')}")
```

**Java**

```java
import com.fasterxml.jackson.databind.JsonNode;

long caseId = 42L; // id of a records case

JsonNode dataset = client.getDataset(caseId);
System.out.println("Mapping version: " + dataset.path("mappingVersion").asText());
System.out.println("Active tree version: " + dataset.path("activeTreeVersionId").asText(null));
```

**CLI**

```bash
CASE_ID=42  # id of a records case

docudevs records dataset-get "$CASE_ID"
```

## Create a records case in one call

The Python and Java clients can create the case and configure its dataset in one call (`createRecordsCase` in Java, which takes the options in a builder). `create_records_case` takes the case name, the source column of the key (`key`), the text columns (`text`, a list of names or a `{column: name}` map) and optionally `label`, `metadata`, `embedded_text`, `target_depth`, `index_settings` and `description`. Without `embedded_text` the embedded text is the text fields, one per line, with empty fields skipped; a string is used as the template. `target_depth` becomes `classifyDefaults.targetDepth`. The other languages make the two requests the example shows.

**cURL**

```bash
# Two requests: create the case, then configure its records dataset.
CASE_ID=$(curl -s -S -f -X POST "https://api.docudevs.ai/cases" \
  -H "Authorization: $API_KEY" \
  -H "Content-Type: application/json" \
  -d '{"name": "Product catalogue", "description": "Reference products for matching"}' | jq -r '.id')

curl -s -S -f -X PUT "https://api.docudevs.ai/cases/$CASE_ID/dataset" \
  -H "Authorization: $API_KEY" \
  -H "Content-Type: application/json" \
  -d '{
    "datasetType": "records",
    "fieldMapping": {
      "key": "sku",
      "text": [
        {"column": "title", "name": "title"},
        {"column": "desc", "name": "description"}
      ],
      "label": "category",
      "metadata": {"brand": "brand"}
    },
    "embeddedText": {"template": "{title}\n{description}", "skipEmptyFields": true},
    "indexSettings": {"topK": 10, "hybrid": false}
  }' > /dev/null

echo "Records case $CASE_ID"
```

**Python**

```python
# One call creates the case and configures its records dataset.
# Raises RecordsApiError when the case or the dataset is refused.
case = await client.create_records_case(
    "Product catalogue",
    description="Reference products for matching",
    key="sku",
    text={"title": "title", "desc": "description"},
    label="category",
    metadata={"brand": "brand"},
    embedded_text="{title}\n{description}",
    index_settings={"top_k": 10, "hybrid": False},
)
print(f"Records case {case.id}")
```

**Java**

```java
import ai.docudevs.client.RecordsCase;
import ai.docudevs.client.RecordsCaseOptions;

// One call creates the case and configures its records dataset.
// Throws DocuDevsApiException when the case or the dataset is refused.
RecordsCase records = client.createRecordsCase(
        "Product catalogue",
        "sku",
        Map.of("title", "title", "desc", "description"),
        RecordsCaseOptions.builder()
                .description("Reference products for matching")
                .label("category")
                .metadata(Map.of("brand", "brand"))
                .embeddedText("{title}\n{description}")
                .indexSettings(Map.of("top_k", 10, "hybrid", false))
                .build());
System.out.println("Records case " + records.id());
```

**CLI**

```bash
# Two commands: create the case, then configure its records dataset.
CASE_ID=$(docudevs cases create \
  --name "Product catalogue" \
  --description "Reference products for matching" | jq -r '.id')

cat > dataset.json <<'JSON'
{
  "datasetType": "records",
  "fieldMapping": {
    "key": "sku",
    "text": [
      {"column": "title", "name": "title"},
      {"column": "desc", "name": "description"}
    ],
    "label": "category",
    "metadata": {"brand": "brand"}
  },
  "embeddedText": {"template": "{title}\n{description}", "skipEmptyFields": true},
  "indexSettings": {"topK": 10, "hybrid": false}
}
JSON

docudevs records dataset-set "$CASE_ID" dataset.json
```

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

The next step is to fill the dataset: see [Importing records](./records-import.md#file-import).

## Single records and batches

**cURL**

```bash
CASE_ID=42  # id of a records case

curl -s -S -f -X PUT "https://api.docudevs.ai/cases/$CASE_ID/records/1000000000001" \
  -H "Authorization: $API_KEY" \
  -H "Content-Type: application/json" \
  -d '{
    "text": {"title": "Soft sock 3-pack", "description": "Cotton, size 40-45"},
    "label": "4.12.3",
    "labelSource": "import",
    "metadata": {"brand": "Foo"}
  }' | jq -r '"Import \(.importId): \(.status)"'
```

**Python**

```python
case_id = 42  # id of a records case

# Raises RecordsApiError when the request is refused
accepted = await client.upsert_record(
    case_id,
    "1000000000001",
    {"title": "Soft sock 3-pack", "description": "Cotton, size 40-45"},
    label="4.12.3",
    metadata={"brand": "Foo"},
)
print(f"Import {accepted['importId']}: {accepted['status']}")
```

**Java**

```java
import com.fasterxml.jackson.databind.JsonNode;

long caseId = 42L; // id of a records case

// Throws DocuDevsApiException when the request is refused
JsonNode accepted = client.upsertRecord(
        caseId,
        "1000000000001",
        Map.of("title", "Soft sock 3-pack", "description", "Cotton, size 40-45"),
        "4.12.3",
        null,
        Map.of("brand", "Foo"));
System.out.println("Import " + accepted.path("importId").asText() + ": " + accepted.path("status").asText());
```

**CLI**

```bash
CASE_ID=42  # id of a records case

docudevs records upsert "$CASE_ID" 1000000000001 \
  --text title="Soft sock 3-pack" \
  --text description="Cotton, size 40-45" \
  --label 4.12.3 \
  --metadata brand=Foo
```

Each write returns `{"importId", "jobGuid", "status"}`. The label source is `import` (default) or `review` (`labelSource` in the request, `label_source` in the Python client); records marked `review` are treated as human decisions by snapshot syncs (see [Importing records](./records-import.md#snapshot-sync)).

**cURL**

```bash
CASE_ID=42  # id of a records case

curl -s -S -f -X DELETE "https://api.docudevs.ai/cases/$CASE_ID/records/1000000000001" \
  -H "Authorization: $API_KEY" | jq -r '"Import \(.importId): \(.status)"'
```

**Python**

```python
case_id = 42  # id of a records case

# Raises RecordsApiError when the request is refused
accepted = await client.delete_record(case_id, "1000000000001")
print(f"Import {accepted['importId']}: {accepted['status']}")
```

**Java**

```java
import com.fasterxml.jackson.databind.JsonNode;

long caseId = 42L; // id of a records case

// Throws DocuDevsApiException when the request is refused
JsonNode accepted = client.deleteRecord(caseId, "1000000000001");
System.out.println("Import " + accepted.path("importId").asText() + ": " + accepted.path("status").asText());
```

**CLI**

```bash
CASE_ID=42  # id of a records case

docudevs records delete "$CASE_ID" 1000000000001
```

A batch (`POST /cases/{caseId}/records:batch`) holds up to 1,000 upserts and deletes and 10 MB. Larger inputs are refused with `413`; use a [file import](./records-import.md#file-import). The command-line client has no batch command; `docudevs records bulk` imports a file instead.

**cURL**

```bash
CASE_ID=42  # id of a records case

curl -s -S -f -X POST "https://api.docudevs.ai/cases/$CASE_ID/records:batch" \
  -H "Authorization: $API_KEY" \
  -H "Content-Type: application/json" \
  -d '{
    "upserts": [
      {
        "key": "1000000000002",
        "text": {"title": "Wool sock", "description": "Merino, size 36-40"},
        "label": "4.12.3",
        "metadata": {"brand": "Foo"}
      }
    ],
    "deletes": ["1000000000001"]
  }' | jq -r '"Import \(.importId): \(.status)"'
```

**Python**

```python
case_id = 42  # id of a records case

# Raises RecordsApiError when the request is refused
accepted = await client.batch_records(
    case_id,
    upserts=[
        {
            "key": "1000000000002",
            "text": {"title": "Wool sock", "description": "Merino, size 36-40"},
            "label": "4.12.3",
            "metadata": {"brand": "Foo"},
        },
    ],
    deletes=["1000000000001"],
)
print(f"Import {accepted['importId']}: {accepted['status']}")
```

**Java**

```java
import com.fasterxml.jackson.databind.JsonNode;

long caseId = 42L; // id of a records case

// Throws DocuDevsApiException when the request is refused
JsonNode accepted = client.batchRecords(
        caseId,
        List.of(Map.of(
                "key", "1000000000002",
                "text", Map.of("title", "Wool sock", "description", "Merino, size 36-40"),
                "label", "4.12.3",
                "metadata", Map.of("brand", "Foo"))),
        List.of("1000000000001"));
System.out.println("Import " + accepted.path("importId").asText() + ": " + accepted.path("status").asText());
```

## Reading records

**cURL**

```bash
CASE_ID=42  # id of a records case

curl -s -S -f "https://api.docudevs.ai/cases/$CASE_ID/records/1000000000001" \
  -H "Authorization: $API_KEY" | jq -r '.label // "-", ((.labelPath // []) | join(" > ")), .labelSource // "-", .chunk // "-"'
```

**Python**

```python
case_id = 42  # id of a records case

record = await client.get_record(case_id, "1000000000001")
print(record.get("label"), record.get("labelPath", []), record.get("labelSource"))
print(record.get("chunk"))
```

**Java**

```java
import com.fasterxml.jackson.databind.JsonNode;

long caseId = 42L; // id of a records case

JsonNode record = client.getRecord(caseId, "1000000000001");
System.out.println(record.path("label").asText(null) + " " + record.path("labelPath")
        + " " + record.path("labelSource").asText(null));
System.out.println(record.path("chunk"));
```

**CLI**

```bash
CASE_ID=42  # id of a records case

docudevs records get "$CASE_ID" 1000000000001
```

`GET /cases/{caseId}/records/{key}` returns the record with its `label`, `labelPath`, `labelSource` and the embedded text (`chunk`).

**cURL**

```bash
CASE_ID=42  # id of a records case

# One page (limit 1 to 200, default 50); pass .continuation back as
# --data-urlencode "continuation=<token>" to read the next page. The Python
# client follows the tokens for you.
curl -s -S -f -G "https://api.docudevs.ai/cases/$CASE_ID/records" \
  -H "Authorization: $API_KEY" \
  --data-urlencode "subtree=4.12" \
  --data-urlencode "attribute=brand=Foo" | jq -r '(.records // [])[] | "\(.key) \(.label // "-")"'
```

**Python**

```python
case_id = 42  # id of a records case

# Follows the continuation tokens until every matching record was read
async for record in client.list_records(case_id, subtree="4.12", attributes={"brand": "Foo"}):
    print(record["key"], record.get("label"))
```

**Java**

```java
import ai.docudevs.client.RecordListOptions;
import com.fasterxml.jackson.databind.JsonNode;

long caseId = 42L; // id of a records case

// Follows the continuation tokens until every matching record was read
JsonNode records = client.listRecords(
        caseId,
        RecordListOptions.builder()
                .subtree("4.12")
                .attributes(Map.of("brand", "Foo"))
                .build());
for (JsonNode record : records) {
    System.out.println(record.path("key").asText() + " " + record.path("label").asText(null));
}
```

**CLI**

```bash
CASE_ID=42  # id of a records case

docudevs records list "$CASE_ID" --subtree 4.12 --attribute brand=Foo --all
```

`GET /cases/{caseId}/records` pages through records ordered by key. Filter with `label`, `subtree`, `labelSource` and repeated `attribute=name=value`. `limit` is 1-200 (default 50). Pass the returned `continuation` token to fetch the next page; there is no depth limit. The cURL variant reads one page; the Python and Java clients and the CLI (`--all`) follow the tokens for you.

**cURL**

```bash
CASE_ID=42  # id of a records case

curl -s -S -f "https://api.docudevs.ai/cases/$CASE_ID/records/stats" \
  -H "Authorization: $API_KEY" | jq -r '"\(.records) records, \(.unlabelled) without a label", ((.labels // {}) | to_entries[] | "\(.key) \(.value)")'
```

**Python**

```python
case_id = 42  # id of a records case

stats = await client.record_stats(case_id)
print(f"{stats['records']} records, {stats['unlabelled']} without a label")
for code, counts in stats.get("labels", {}).items():
    print(code, counts)
```

**Java**

```java
import com.fasterxml.jackson.databind.JsonNode;

long caseId = 42L; // id of a records case

JsonNode stats = client.recordStats(caseId);
System.out.println(stats.path("records").asText() + " records, "
        + stats.path("unlabelled").asText() + " without a label");
for (var counts : stats.path("labels").properties()) {
    System.out.println(counts.getKey() + " " + counts.getValue());
}
```

**CLI**

```bash
CASE_ID=42  # id of a records case

docudevs records stats "$CASE_ID"
```

`GET /cases/{caseId}/records/stats` returns record counts per label, split by `labelSource`: for each code, `import`, `review` and `total`.
