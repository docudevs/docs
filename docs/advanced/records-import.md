---
title: Importing records
description: Load records with batches, file uploads, source URLs and snapshot sync with a dry run.
sidebar_position: 3
---

# Importing records

Pick the path that fits the size of your data. All of them return an `importId` to follow (see [Import status](#import-status)) and, once the import is queued, a `jobGuid` you can poll with `GET /job/status/{guid}`. With an organization API key, the case must be one that the key can use; see [Which cases you can use](./case-agent.md#which-cases-you-can-use).

| Input | Use |
| --- | --- |
| Up to 1,000 records or 10 MB | `POST /cases/{caseId}/records:batch` |
| A larger file on your machine | [File import](#file-import): create, upload in blocks, commit |
| A file you host | Import with `sourceUrl`; DocuDevs fetches it |
| A full export that should become the dataset | Snapshot sync (`mode: sync`), with a dry run first |

## File import

Rows are keyed by your source columns, as described by the dataset field mapping. A file for the dataset of the [records datasets](./records-datasets.md#configure-the-dataset) page could look like this:

```csv
sku,title,desc,category,brand
1000000000001,Soft sock 3-pack,"Cotton, size 40-45",4.12.3,Foo
1000000000002,Wool sock,"Merino, size 36-40",4.12.3,Foo
```

<Example name="records/import-file" />

The Python and Java clients and the CLI choose the path for you:

- Up to 1,000 rows (and 10 MB) of ndjson or row dictionaries are sent as one batch. The Python and Java clients also accept a list of rows (dictionaries or maps) instead of a file path.
- CSV files and anything larger become a file import. The client gzip-compresses the input, uploads it through the API in blocks and commits it.
- A mapping overrides the dataset mapping for one call (`mapping` in Python and in the Java options, `--mapping-file` on the CLI, `mapping` in the request).
- Waiting polls until the import completes and returns the final import, including counters.

The cURL variant shows the underlying flow, which is the same for every client:

1. **Create the import** with `POST /cases/{caseId}/records/imports`. The response carries the `importId`, the `status` (`AWAITING_UPLOAD`) and `expiresAt`:

   ```json
   {"importId": 302, "status": "AWAITING_UPLOAD",
    "expiresAt": "2026-10-01T12:00:00Z"}
   ```

2. **Upload the blocks.** Split the file into blocks of at most 32 MiB and upload them through the API with `PUT /cases/{caseId}/records/imports/{importId}/upload/blocks/{index}`. Each block is a raw `application/octet-stream` body. Block indexes start at 0 and count up by one, and the file is the blocks concatenated in index order. Re-sending an index replaces that block, so a failed block is retried on its own. The upload window is 24 hours from the import's creation (`expiresAt`).

3. **Commit** with `POST /cases/{caseId}/records/imports/{importId}/commit?blockCount=n`, where `n` is the number of blocks. Always send `blockCount`. The commit then uses exactly blocks `0` to `blockCount - 1` and discards blocks left over from an earlier, abandoned upload attempt. Without it, every uncommitted block is used. The commit returns `409` when a block is missing (the message names the first missing index), when the upload window has expired or when nothing was uploaded. It returns `413` when the file is larger than 2 GiB.

`format` is `ndjson` or `csv`. `gzip` states that the uploaded file is gzip-compressed. CSV and ndjson rows are mapped with the dataset field mapping, or with a `mapping` object in the request. Committing is idempotent: committing an upload that is already committed is harmless. A file that is not committed before `expiresAt` cannot be committed; start a new import.

## Import from a URL

<Example name="records/import-from-url" />

The Python client and the CLI wait for the import, and so do the Java methods ending in `AndWait`; the cURL variant returns after creating it, so read the import status until it is final. The URL must be `https`, must not contain user information and must not point at a private or local address. DocuDevs enforces a size cap. The import is queued when it is created; there is no commit step.

## Import status

`GET /cases/{caseId}/records/imports/{importId}` returns `status`, `mode`, `dryRun`, `jobGuid`, and the counters `rowsReceived`, `rowsRejected`, `recordsAdded`, `recordsUpdated`, `recordsDeleted` and `recordsUnchanged`.

<Example name="records/get-import" />

| Status | Meaning |
| --- | --- |
| `STAGING` | The request is being stored. |
| `AWAITING_UPLOAD` | Waiting for the file's blocks and the commit. |
| `COMMITTING`, `QUEUED_PENDING`, `QUEUED` | Accepted and waiting for the worker. |
| `RUNNING` | The worker is applying it. |
| `COMPLETED` | Applied. |
| `FAILED` | Not applied. |

Only `COMPLETED` and `FAILED` are final: keep reading until the import reaches one of them. The job behind an import reports its progress with `GET /job/status/{guid}`; the response carries a `progress` object (`phase`, `done`, `total`, `failed`).

<Example name="records/wait-for-import" />

The client waits and calls your progress callback with that `progress` object; the cURL variant reads the import itself.

### Retry a failed import

`POST /cases/{caseId}/records/imports/{importId}/retry` queues a failed import again with a new job (`202`). The worker resumes from its saved progress and does not re-apply table changes that were already merged. The staged input is kept for 7 days after a failure. The call returns `409` when the import has not failed, when the system retries it itself, or when its staged input is gone or changed, and `503` when it could not be queued.

<Example name="records/retry-import" />

Rows that cannot be mapped (for example a missing key) are rejected and counted in `rowsRejected`; the other rows are still applied. Writes to a case are applied one at a time, in order.

## Snapshot sync

When you only have a full export, a sync makes the dataset equal to it. It is **a dry run by default** and changes nothing until you confirm.

<Example name="records/sync-snapshot" />

The first run is the dry run and prints its report. The second run applies the same file and references the dry run by its import id.

- `syncSummary.counts` holds `received`, `rejected`, `inserted`, `reembed`, `metadataOnly`, `deleted`, `unchanged` and `keptReviewed`. `samples` lists at most 20 record keys for each of `toInsert`, `reembed`, `metadataOnly`, `toDelete` and `keptReviewed`. `inputContentHash` identifies the input. `guard` compares the share of records the sync would delete with the limit (`existingRecords`, `deleteFraction`, `maxDeleteFraction`, `exceedsLimit`).
- A real run must reference the completed dry run (`reportId`, the import id of the dry run) and the same input. It is refused when the share of records it would delete exceeds `maxDeleteFraction` (default 0.2), unless `confirmLargeDelete` is true. This protects against a truncated export wiping the dataset.
- Records with `labelSource: review` are kept even when absent from the snapshot (`reviewedRecords: keep`, the default; `delete` removes them). If the snapshot carries a different label for a reviewed record, the review label wins (`reviewedLabelPolicy: reviewWins`, the default; `importWins` overrides it) and the conflict is listed in `labelConflicts`.
