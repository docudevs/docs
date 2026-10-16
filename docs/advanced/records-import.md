---
title: Importing records
description: Load records with batches, file uploads, source URLs and snapshot sync with a dry run.
sidebar_position: 3
---

# Importing records

Pick the path that fits the size of your data. All of them return a `jobGuid` you can poll with `GET /job/status/{guid}`; the status response carries a `progress` object (`phase`, `done`, `total`, `failed`).

| Input | Use |
| --- | --- |
| Up to 1,000 records or 10 MB | `POST /cases/{caseId}/records:batch` |
| A larger file on your machine | File import: create, upload in blocks, commit |
| A file you host | Import with `sourceUrl`; DocuDevs fetches it |
| A full export that should become the dataset | Snapshot sync (`mode: sync`), with a dry run first |

## Python SDK

`bulk_upsert_records` chooses for you. Rows are dictionaries keyed by your source columns, as described by the dataset field mapping.

```python
# list of rows, or a path to .ndjson, .csv or a .gz of either
result = await client.bulk_upsert_records(case_id, "products.csv", wait=True,
                                          on_progress=print)
```

- Up to 1,000 rows (and 10 MB) of ndjson or row dictionaries are sent as one batch.
- CSV files and anything larger become a file import. The SDK gzip-compresses the input, uploads it through the API in blocks and commits it.
- `mapping=` overrides the dataset mapping for one call.
- `wait=True` polls until the import completes and returns the final import, including counters.

The raw flow is also available:

```python
imp = await client.get_import(case_id, import_id)
imp = await client.wait_for_import(case_id, import_id, on_progress=print)
```

## File import with curl

Set `API` to the API base URL and `API_KEY` to your API key.

1. Create the import.

   ```bash
   curl -X POST "$API/cases/42/records/imports" \
     -H "Authorization: Bearer $API_KEY" -H "Content-Type: application/json" \
     -d '{"mode":"upsert","format":"csv","gzip":true}'
   ```

   ```json
   {"importId": 302, "status": "AWAITING_UPLOAD",
    "expiresAt": "2026-10-01T12:00:00Z"}
   ```

2. Split the file into blocks of at most 32 MiB and upload them through the API. Each block is a raw `application/octet-stream` body. Block indexes start at 0 and count up by one, and the file is the blocks concatenated in index order. Re-sending an index replaces that block, so a failed block is retried on its own. The upload window is 24 hours by default from the import's creation (`expiresAt`).

   ```bash
   rm -f block-*
   split -b 32m products.csv.gz block-
   i=0; for f in block-*; do
     curl --fail-with-body -X PUT "$API/cases/42/records/imports/302/upload/blocks/$i" \
       -H "Authorization: Bearer $API_KEY" -H "Content-Type: application/octet-stream" --data-binary @"$f" || break
     i=$((i+1))
   done
   ```

3. Commit with the number of blocks. After the loop, `$i` holds the block count.

   ```bash
   curl -X POST "$API/cases/42/records/imports/302/commit?blockCount=$i" -H "Authorization: Bearer $API_KEY"
   ```

   Always send `blockCount`. The commit then uses exactly blocks `0` to `blockCount - 1` and discards blocks left over from an earlier, abandoned upload attempt. Without it, every uncommitted block is used. The commit returns `409` when a block is missing (the message names the first missing index), when the upload window has expired or when nothing was uploaded. It returns `413` when the file is larger than 2 GiB.

`format` is `ndjson` or `csv`. `gzip` states that the uploaded file is gzip-compressed. CSV and ndjson rows are mapped with the dataset field mapping, or with a `mapping` object in the request. Committing is idempotent: committing an upload that is already committed is harmless. A file that is not committed before `expiresAt` cannot be committed; start a new import.

## Import from a URL

```python
imp = await client.import_records_from_url(
    case_id, "https://example.blob.core.windows.net/exports/products.csv?sv=...",
    format="csv", wait=True)
```

The URL must be `https`, must not contain user information and must not point at a private or local address. DocuDevs enforces a size cap. The import is queued when it is created; there is no commit step.

## Import status

`GET /cases/{caseId}/records/imports/{importId}` returns `status`, `mode`, `dryRun`, `jobGuid`, and the counters `rowsReceived`, `rowsRejected`, `recordsAdded`, `recordsUpdated`, `recordsDeleted` and `recordsUnchanged`.

| Status | Meaning |
| --- | --- |
| `AWAITING_UPLOAD` | Waiting for the file's blocks and the commit. |
| `COMMITTING`, `QUEUED` | Accepted and waiting for the worker. |
| `RUNNING` | The worker is applying it. |
| `COMPLETED` | Applied. |
| `FAILED` | Not applied. |

### Retry a failed import

`POST /cases/{caseId}/records/imports/{importId}/retry` queues a failed import again with a new job (`202`). The worker resumes from its saved progress and does not re-apply table changes that were already merged. The staged input is kept for 7 days after a failure. The call returns `409` when the import has not failed, when the system retries it itself, or when its staged input is gone or changed, and `503` when it could not be queued.

```python
await client.retry_import(case_id, import_id)
```

```bash
docudevs records import-retry 42 301
```

Rows that cannot be mapped (for example a missing key) are rejected and counted in `rowsRejected`; the other rows are still applied. Writes to a case are applied one at a time, in order.

## Snapshot sync

When you only have a full export, a sync makes the dataset equal to it. It is **a dry run by default** and changes nothing until you confirm.

```python
dry = await client.sync_records(case_id, "snapshot.ndjson", wait=True)
print(dry["syncSummary"]["counts"], dry["syncSummary"]["guard"])

# After reviewing the report, apply the same file for real.
await client.confirm_sync_records(case_id, "snapshot.ndjson", report_id=dry["importId"], wait=True)
```

```bash
docudevs records sync 42 snapshot.ndjson            # dry run, prints the report
docudevs records sync 42 snapshot.ndjson --confirm 301
```

- `syncSummary.counts` holds `received`, `rejected`, `inserted`, `reembed`, `metadataOnly`, `deleted`, `unchanged` and `keptReviewed`. `samples` lists at most 20 record keys for each of `toInsert`, `reembed`, `metadataOnly`, `toDelete` and `keptReviewed`. `inputContentHash` identifies the input.
- A real run must reference the completed dry run (`reportId`, the import id of the dry run) and the same input. It is refused when the share of records it would delete exceeds `maxDeleteFraction` (default 0.2), unless `confirmLargeDelete` is true. This protects against a truncated export wiping the dataset.
- Records with `labelSource: review` are kept even when absent from the snapshot (`reviewedRecords: keep`, the default; `delete` removes them). If the snapshot carries a different label for a reviewed record, the review label wins (`reviewedLabelPolicy: reviewWins`, the default; `importWins` overrides it) and the conflict is listed in `labelConflicts`.

## CLI

```bash
docudevs records bulk 42 products.csv --wait
docudevs records import-url 42 "https://.../products.csv" --format csv --wait
docudevs records import-status 42 301 --wait
```
