---
title: Classification and matching
description: Classify rows against a category tree or match them to records, route uncertain rows to review, export results and feed corrections back as evidence.
sidebar_position: 6
---

# Classification and matching

A **classify job** takes rows (a weekly product file, invoice lines) and assigns each one to a category of a tree, or to a record of a knowledge base. It is a batch of independent, parallel row decisions, not an agent conversation. For every row DocuDevs gathers candidates from your history, asks the model to pick one, validates the pick and stores the result with **flags** that tell you which rows need a person.

You need a records knowledge base first: a [records dataset](./records-datasets.md) with imported, labelled records and, for category classification, an [active category tree](./category-trees.md). The jobs below run against the records case, or against another case that names the knowledge base in `recordsKnowledgeBaseId`.

## Start a job

`POST /cases/{caseId}/classify` returns `202` with a `jobId`.

| Input | How |
| --- | --- |
| Up to 1,000 rows and 10 MB | `rows: {"source": "inline", "items": [...]}` in the request |
| A larger file | `rows: {"source": "upload", "format": "ndjson" or "csv"}`; upload the file in blocks of at most 32 MiB to `PUT .../classify/{jobId}/upload/blocks/{index}`, then `POST .../classify/{jobId}/commit?blockCount=n`, where `n` is the number of blocks. See [Importing records](./records-import.md#file-import-with-curl) for the block protocol. |
| Records already in the knowledge base | `keys: ["sku-1", "sku-2", ...]`, at most 10,000 |

An inline item is `{"key": "...", "text": {"title": "...", "description": "..."}, "metadata": {"brand": "..."}}`, using the **field names** of the dataset mapping. With `keys`, each record is left out of its own evidence, so the result shows how the record would be classified without its current label. An uploaded file uses your **source column names**, like a records import, or the columns you map with `inputMapping` (below). The job pins the tree version, the memo version and the records table version when it starts, so later edits do not change a running job.

```json
{
  "rows": {"source": "inline", "items": [
    {"key": "6410000999999", "text": {"title": "Running socks 3-pack", "description": "Cotton blend"}}
  ]},
  "config": {
    "target": "category",
    "llmLevel": "MINI",
    "targetDepth": [4, 5],
    "candidateStrategy": {
      "similarRecords": {"k": 30},
      "siblingExpansion": {"fromLevel": 4, "seeds": 3},
      "categoryMatches": {"k": 10},
      "widenWhenSourcesDisagree": {"extraCategoryMatches": 10, "extraSimilar": 20},
      "maxCandidates": 100,
      "leafOnly": true
    },
    "instructions": "Prefer the most specific category supported by the product facts.",
    "routing": {"reviewWhen": ["declined", "modelNamedDifferentBestFit", "sourcesDisagree"]}
  }
}
```

Invalid settings return `400`. `404` means the case, knowledge base or tree version is not readable, `409` means the knowledge base has no dataset, no imported records or no usable tree, and `413` means too many rows or keys.

### Config

| Setting | Meaning |
| --- | --- |
| `target` | `category` (default) or `record` (matching, below). |
| `treeVersionId` | Tree version to use; default is the active version. Category target only. |
| `targetDepth` | Tree levels to report besides the chosen leaf, for example `[4, 5]`. Category target only. Precedence: this value, then the case default (`classifyDefaults.targetDepth` on the [records dataset](./records-datasets.md)), then every level of the tree from 1 to its maximum depth. A shallower leaf carries the `shallow` flag. |
| `instructions` | Free text added to the prompt. It is stored in a blob and is never returned by the API. |
| `memo` | `use`: `active` (default) or `none`. `version` pins an older memo version. `include` lists ids of proposed memo entries to try in this run. See [Case memo](./case-memo.md). |
| `llmLevel` | `DEFAULT`, `NANO`, `MINI` (default) or `HIGH`. Bring-your-own models are honoured. |
| `concurrency` | Rows in parallel, 1 to 32 (default 8). |
| `chunkRows` | Rows per queue message (default 40). |
| `chunkSeconds` | Seconds of work per message before the job continues in a new one, 10 to 240 (default 150). Other jobs interleave between chunks. |
| `maxPushbackRequeues` | How many times a chunk may be requeued after model rate limiting before the job fails, 0 to 50 (default 5). |
| `maxTotalTokens` | Token budget of the job. Default is 8,000 tokens per row for inline rows and keys; required for uploads. When the budget is spent the remaining rows are marked `skipped` and the job still completes. |
| `perRowMaxPromptTokens` | Per-row prompt cap; the prompt is shrunk to fit. |
| `recordsKnowledgeBaseId` | Knowledge base to classify against. Required for `target: record`; otherwise the case itself. |
| `inputMapping` | Maps input columns onto dataset field names (see below). |
| `investigate` | `{enabled, playbook, maxRows, flags, maxTotalTokens}`. When `enabled` is true, a headless [agent run](./agent-runs.md) investigates the job's flagged rows after it completes. `maxTotalTokens` bounds that run; without it the run takes the server default of 300000 tokens. |

### Candidate strategy

The model only chooses among candidates, so the candidate set decides the best accuracy you can get. Candidates come from three sources:

1. **Similar records.** The `similarRecords.k` most similar labelled records of the knowledge base vote for their categories; their texts are shown as examples.
2. **Sibling expansion.** `siblingExpansion` takes the `seeds` top-voted categories and adds their siblings at `fromLevel`.
3. **Category matches.** `categoryMatches.k` categories whose own name and description match the row.

When the similar records and the category matches disagree about the family at the target level, `widenWhenSourcesDisagree` adds `extraCategoryMatches` and `extraSimilar` more candidates for that row. Retired categories are never offered.

`maxCandidates` caps what the model sees, 10 to 200 (default 100). `leafOnly` (default `true` for the category target) offers only leaf categories, so a pick at an intermediate level is invalid. Turn it off when you classify to an intermediate level. For the record target `leafOnly` is ignored and the tree settings are rejected.

## Follow the job

`GET /cases/{caseId}/classify/{jobId}` returns the status (`QUEUED`, `RUNNING`, `COMPLETED`, `FAILED`, `CANCELLED`), `counts` (`total`, `done`, `declined`, `flagged`, `failed`, `skipped`), token `usage`, and the pinned `treeVersionId`, `memoVersion` and `recordsTableVersion`. `POST .../cancel` stops the job at the next chunk boundary. A failed row is recorded with its reason and retried once at the end of the job; it does not fail the job.

A job started right after the first records import of a case waits for the case's search index to finish building. It stays `RUNNING` with no rows done meanwhile, and fails with a clear reason if the index is not ready within 10 minutes.

## Results

`GET /cases/{caseId}/classify/{jobId}/results` pages the rows (`limit` 1 to 1,000, default 100). A running job returns the rows written so far with `complete: false`. Follow `continuation` for the next page.

```json
{
  "rowId": 17,
  "key": "6410000999999",
  "chosen": {
    "code": "4.12.3.2",
    "path": ["Clothing", "Socks and tights", "Socks", "Sports socks"],
    "levels": {"4": "4.12.3", "5": "4.12.3.2"}
  },
  "declined": false,
  "alternativeBestFit": {"code": "4.12.3.1", "reason": "The name does not state the use."},
  "evidenceKeys": ["6410000123456", "6410000123999"],
  "memoVersion": 12,
  "treeVersionId": 7,
  "flags": {"declined": false, "modelNamedDifferentBestFit": true, "sourcesDisagree": false},
  "review": {"required": true, "reasons": ["modelNamedDifferentBestFit"]},
  "reviewDecision": null,
  "status": "ok",
  "usage": {"promptTokens": 4210, "completionTokens": 96}
}
```

`status` is `ok`, `failed` or `skipped`. `evidenceKeys` are the history records the candidates came from. `reviewDecision` is the latest review (`accept`, `correct`, `feedback`) or null. Declined rows have no `chosen`.

Filters: `flag`, `reviewRequired`, `level` with `code` (the chosen code at that level), `decision` (`accept`, `correct`, `feedback` or `none`) and `routing`. Filtered scans cover at most 50,000 rows per request; follow the continuation to go on.

### Flags and routing

Flags are stable facts about a row. Whether a row needs review is computed when you read it, from the flags and a **routing rule**, so you can change the rule without rerunning the job: set `config.routing.reviewWhen` for the job, or pass `routing=declined,sourcesDisagree` on a read to override it.

| Flag | Set when |
| --- | --- |
| `declined` | The model declined to choose, or its answer was invalid. |
| `modelNamedDifferentBestFit` | The model chose a category but named a different best fit. |
| `sourcesDisagree` | The similar records and the category matches point to different families at the target level. |
| `noEvidence` | No similar labelled record was found. |
| `invalidChoice` | The model named a code outside the candidates twice; the row is declined. |
| `rareCategory` | The chosen category has very few labelled records. |
| `memoTruncated` | The memo budget cut entries for this row. |
| `noCandidates` | No candidates were available; the row is declined. |

The default rule is `declined`, `modelNamedDifferentBestFit` and `sourcesDisagree`. Model confidence is not stored or used for routing. Check the coverage against accuracy trade-off on your own data before tightening the rule: routing more flags to review raises accuracy of the unreviewed rows and lowers the share that skips review.

### CSV export

`GET /cases/{caseId}/classify/{jobId}/results.csv` streams one row per result with the same filters and routing override. Columns: `rowId`, `key`, the input columns, `code` and `path`, a code and path per target level, `recordKey`, a column per flag, `alternativeCode`, `alternativeReason`, `evidenceKeys`, `memoVersion`, `reviewRequired`, `reviewReasons`, `reviewDecision` and `status`. Cells that start with `=`, `+`, `-`, `@` or a tab get a leading apostrophe so spreadsheets do not run them.

## Review

`POST /cases/{caseId}/classify/{jobId}/reviews` records a human decision on one row.

```json
{"rowId": 17, "decision": "correct", "code": "4.12.3.1", "reason": "Plain cotton sock, no sport use."}
```

| Decision | Effect |
| --- | --- |
| `accept` | Confirms the chosen code and writes it as the record's label with `labelSource: review`. |
| `correct` | Writes the code you supply the same way, and adds a correction memo entry with your reason. The code must be an active category of the pinned tree. |
| `feedback` | Stores a comment (`text`) as a memo entry. It never changes classification. |

`accept` and `correct` take effect directly: the product becomes labelled history, so it is evidence for later jobs. Model proposals are never written back, and rows nobody reviews are not written. If the row has no record yet, the correction creates it. Rows without a key, and matching jobs, record the review and memo entry but write no record.

The label write is asynchronous. The response carries `labelWrite` with an `importId` and a `status` you can follow with `GET /cases/{caseId}/records/imports/{importId}`; a job started after it completes sees the corrected label (usually within seconds to minutes). Repeating an identical request returns the same review. If the write fails, `POST /cases/{caseId}/records/imports/{importId}/retry` requeues it.

## Matching rows to records

With `"target": "record"` each row is matched to a record of the knowledge base instead of a category. Typical uses are invoice lines to a product catalogue, a supplier to the vendor master, and deduplication. Name the catalogue in `recordsKnowledgeBaseId`; the rows may belong to another case.

```json
{
  "rows": {"source": "inline", "items": [{"key": "line-1", "text": {"title": "M8 hex bolt 40 mm, 100 pcs"}}]},
  "config": {
    "target": "record",
    "recordsKnowledgeBaseId": 51,
    "candidateStrategy": {"similarRecords": {"k": 20}},
    "instructions": "Match the invoice line to the catalogue product with the same item and pack size.",
    "routing": {"reviewWhen": ["declined", "modelNamedDifferentBestFit"]}
  }
}
```

The result carries `chosen.recordKey` (with the record's label and attributes) instead of a category path. Flags, routing, CSV and review work the same way. A `correct` review names the right record key as `code`.

### Input mapping

Rows normally use the dataset mapping of the knowledge base. When your file has different columns, map them with `inputMapping`:

```json
"inputMapping": {
  "key": "line_id",
  "text": [{"column": "item_description", "name": "title"}, {"column": "pack", "name": "description"}],
  "metadata": {"brand": "supplier"}
}
```

`text[].name` must be a text field of the dataset mapping, `key` is optional (rows without one run, but their reviews write no record), and `metadata` maps a metadata name to a column. Names must not start with `_`.

## Python SDK

```python
job = await client.classify(
    case_id,
    rows,                                  # list of inline rows
    config={"llm_level": "MINI", "target_depth": [4, 5]},
    wait=True,
    on_progress=print,
)

async for row in client.get_classify_results(case_id, job["jobId"], review_required=True):
    print(row["rowId"], row["review"]["reasons"])

await client.download_classify_csv(case_id, job["jobId"], path="results.csv")
await client.review_classify(case_id, job["jobId"], 17, "correct", code="4.12.3.1", reason="Plain cotton sock")
```

- `classify` sends inline rows in one request up to 1,000 rows and 10 MB. Above that it uploads the rows as gzip ndjson through the API in blocks, maps the columns for you and sets a default token budget. `keys=[...]` classifies records of the knowledge base, and `path="weekly.csv"` uploads a file (`config` must then include `max_total_tokens`).
- `config` keys may be `snake_case` or `camelCase`.
- `wait_for_classify` raises `ClassifyJobFailed` when the job fails or is cancelled.
- `get_classify`, `cancel_classify` and `commit_classify` map one to one onto the endpoints. `get_classify_results_page` returns a single page with its `continuation`.

## CLI

```bash
docudevs classify start 42 --file weekly.csv --max-total-tokens 5000000 --config-file config.json --wait
docudevs classify status 42 9 --wait
docudevs classify results 42 9 --review-required --all
docudevs classify csv 42 9 --flag declined -o declined.csv
docudevs classify review 42 9 17 correct --code 4.12.3.1 --reason "Plain cotton sock"
docudevs classify cancel 42 9
```

`classify start` also takes `--rows-file` (a JSON list of inline rows) and `--keys-file` (one record key per line).

## Limits

Classification bills the model tokens of each row (prompt and completion) like any other model call. Each row also makes one query embedding, which is metered but is not charged tokens. Results and memo text are kept until the case is deleted; staged inputs expire after seven days. A running job's cost is bounded by `maxTotalTokens`.
