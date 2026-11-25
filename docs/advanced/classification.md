---
title: Classification and matching
description: Classify rows against a category tree or match them to records, route uncertain rows to review, export results and feed corrections back as evidence.
sidebar_position: 6
---

# Classification and matching

A **classify job** takes rows (a weekly product file, invoice lines) and assigns each one to a category of a tree, or to a record of a knowledge base. It is a batch of independent, parallel row decisions, not an agent conversation. For every row DocuDevs gathers candidates from your history, asks the model to pick one, validates the pick and stores the result with **flags** that tell you which rows need a person.

You need a records knowledge base first: a [records dataset](./records-datasets.md) with imported, labelled records and, for category classification, an [active category tree](./category-trees.md). The jobs below run against the records case, or against another case that names the knowledge base in `recordsKnowledgeBaseId`. With an organization API key, the cases must be ones that the key can use; see [Which cases you can use](./case-agent.md#which-cases-you-can-use).

## Start a job

`POST /cases/{caseId}/classify` returns `202` with a `jobId`.

| Input | How |
| --- | --- |
| Up to 1,000 rows and 10 MB | `rows: {"source": "inline", "items": [...]}` in the request |
| A larger file | `rows: {"source": "upload", "format": "ndjson" or "csv"}`; upload the file in blocks of at most 32 MiB to `PUT .../classify/{jobId}/upload/blocks/{index}`, then `POST .../classify/{jobId}/commit?blockCount=n`, where `n` is the number of blocks. See [Importing records](./records-import.md#file-import) for the block protocol. |
| Records already in the knowledge base | `keys: ["sku-1", "sku-2", ...]`, at most 10,000 |

An inline item is `{"key": "...", "text": {"title": "...", "description": "..."}, "metadata": {"brand": "..."}}`, using the **field names** of the dataset mapping. With `keys`, each record is left out of its own evidence, so the result shows how the record would be classified without its current label. An uploaded file uses your **source column names**, like a records import, or the columns you map with `inputMapping` (below). The job pins the tree version, the memo version and the records table version when it starts, so later edits do not change a running job.

Start a job with inline rows:

<Example name="classification/start-classify" />

Upload a file of rows when there are more than 1,000 or the file is large. The client compresses the file, uploads it in blocks and commits it; the cURL variant shows the three calls and does not wait for the job, while the Python client and the CLI wait:

<Example name="classification/classify-file" />

The request body of the first example sets only the target, the model level and the target depth. A request that sets most options looks like this:

```json
{
  "rows": {"source": "inline", "items": [
    {"key": "1000000000003", "text": {"title": "Running socks 3-pack", "description": "Cotton blend"}}
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
| `instructions` | Free text added to the prompt. It is never returned by the API. |
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
| `investigate` | `{enabled, playbook, maxRows, flags, maxTotalTokens}`. When `enabled` is true, a headless [agent run](./agent-runs.md) investigates the job's flagged rows after it completes. `maxTotalTokens` bounds that run; without it the run uses a default budget. |

### Candidate strategy

The model only chooses among candidates, so the candidate set decides the best accuracy you can get. Candidates come from three sources:

1. **Similar records.** Always on. The `similarRecords.k` most similar labelled records of the knowledge base vote for their categories; their texts are shown as examples. `k` defaults to 30 (20 for the record target).
2. **Sibling expansion.** `siblingExpansion` takes the `seeds` top-voted categories and adds their siblings at `fromLevel`. Off unless you set `siblingExpansion`; `seeds` defaults to 3.
3. **Category matches.** `categoryMatches.k` categories whose own name and description match the row. Off unless you set `categoryMatches`; `k` defaults to 10.

When the similar records and the category matches disagree about the family at the target level, `widenWhenSourcesDisagree` adds `extraCategoryMatches` and `extraSimilar` more candidates for that row (defaults 10 and 20). Widening is off unless you set `widenWhenSourcesDisagree`. Retired categories are never offered.

`maxCandidates` caps what the model sees, 10 to 200 (default 100). `leafOnly` (default `true` for the category target) offers only leaf categories, so a pick at an intermediate level is invalid. Turn it off when you classify to an intermediate level. For the record target `leafOnly` is ignored and the tree settings are rejected.

## Follow the job

`GET /cases/{caseId}/classify/{jobId}` returns the status, `counts` (`total`, `done`, `declined`, `flagged`, `failed`, `skipped`), token `usage`, and the pinned `treeVersionId`, `memoVersion` and `recordsTableVersion`. `POST .../cancel` stops the job at the next chunk boundary and answers `409` when the job has already finished. A failed row is recorded with its reason and retried once at the end of the job; it does not fail the job.

<Example name="classification/wait-for-classify" />

The Python client and the CLI wait until the job completes; the Python client calls your progress callback with the job status and raises `ClassifyJobFailed` when the job fails or is cancelled. The cURL variant reads the job until it is final and fails on `FAILED` or `CANCELLED`. Cancel a job that you no longer need:

<Example name="classification/cancel-classify" />

The status is `QUEUED` and then `RUNNING` while the job works; an uploaded job is `AWAITING_UPLOAD` until you commit it and `STAGING` while the commit is processed. `COMPLETED`, `FAILED` and `CANCELLED` are final, so keep reading until the job reaches one of them.

A job started right after the first records import of a case waits for the case's search index to finish building. It stays `RUNNING` with no rows done meanwhile, and fails with a clear reason if the index is not ready within 10 minutes.

## Results

`GET /cases/{caseId}/classify/{jobId}/results` pages the rows (`limit` 1 to 1,000, default 100). A running job returns the rows written so far with `complete: false`. Follow `continuation` for the next page.

```json
{
  "rowId": 17,
  "key": "1000000000003",
  "chosen": {
    "code": "4.12.3.2",
    "path": ["Clothing", "Socks and tights", "Socks", "Sports socks"],
    "levels": {"4": "4.12.3", "5": "4.12.3.2"}
  },
  "declined": false,
  "alternativeBestFit": {"code": "4.12.3.1", "reason": "The name does not state the use."},
  "evidenceKeys": ["1000000000001", "1000000000004"],
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

<Example name="classification/get-classify-results" />

The cURL variant reads one page; the Python client and the CLI (`--all`) follow the continuation tokens for you.

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

<Example name="classification/download-classify-csv" />

## Review

`POST /cases/{caseId}/classify/{jobId}/reviews` records a human decision on one row.

<Example name="classification/review-classify-row" />

The request names the `rowId`, the `decision` and, depending on the decision, a `code`, a `reason` or a `text`:

| Decision | Effect |
| --- | --- |
| `accept` | Confirms the chosen code and writes it as the record's label with `labelSource: review`. |
| `correct` | Writes the code you supply the same way, and adds a correction memo entry with your reason. The code must be an active category of the pinned tree. |
| `feedback` | Stores a comment (`text`) as a memo entry. It never changes classification. |

`accept` and `correct` take effect directly: the product becomes labelled history, so it is evidence for later jobs. Model proposals are never written back, and rows nobody reviews are not written. If the row has no record yet, the correction creates it. Rows without a key, and matching jobs, record the review and memo entry but write no record.

The label write is asynchronous. The response carries `labelWrite` with an `importId` and a `status` you can follow with `GET /cases/{caseId}/records/imports/{importId}`; a job started after it completes sees the corrected label (usually within seconds to minutes). Repeating an identical request returns the same review. If the write fails, `POST /cases/{caseId}/records/imports/{importId}/retry` requeues it.

## Matching rows to records

With `"target": "record"` each row is matched to a record of the knowledge base instead of a category. Typical uses are invoice lines to a product catalogue, a supplier to the vendor master, and deduplication. Name the catalogue in `recordsKnowledgeBaseId`; the rows may belong to another case.

<Example name="classification/match-records" />

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

## Client helpers

- `classify` (Python) and `classify start` (CLI) send inline rows in one request up to 1,000 rows and 10 MB. Above that they upload the rows as gzip ndjson through the API in blocks, map the columns for you and set a default token budget. Classify records of the knowledge base with a list of keys (`keys=[...]`, `--keys-file` with one key per line), and a file with `path="weekly.csv"` (`--file`); the config must then include the token budget (`max_total_tokens`, `--max-total-tokens`).
- The Python `config` keys may be `snake_case` or `camelCase`; the CLI reads the config as a JSON file (`--config-file`) with the request's `camelCase` names. `--rows-file` takes a JSON list of inline rows.
- The Python client's `get_classify`, `cancel_classify` and `commit_classify` map one to one onto the endpoints, and `get_classify_results_page` returns a single page with its `continuation`.

## Limits

Classification bills the model tokens of each row (prompt and completion) like any other model call. Each row also makes one query embedding, which is metered but is not charged tokens. Results and memo text are kept until the case is deleted; staged inputs expire after seven days. A running job's cost is bounded by `maxTotalTokens`.
