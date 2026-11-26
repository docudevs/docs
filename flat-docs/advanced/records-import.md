---
title: Importing records
description: Load records with batches, file uploads, source URLs and snapshot sync with a dry run.
sidebar_position: 3
---
<!-- Generated from docs/advanced/records-import.md. Do not edit by hand. -->

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

**cURL**

```bash
CASE_ID=42  # id of a records case with a configured dataset
FILE=products.csv  # columns as named in the dataset field mapping

wait_for_import() {
  local deadline=$((SECONDS + 1800))  # same limit as the Python timeout
  while [ "$SECONDS" -lt "$deadline" ]; do
    IMPORT=$(curl -s -S -f "https://api.docudevs.ai/cases/$CASE_ID/records/imports/$1" \
      -H "Authorization: $API_KEY")
    case "$(echo "$IMPORT" | jq -r '.status')" in
      COMPLETED) return 0 ;;
      FAILED) echo "Import $1 failed" >&2; return 1 ;;
    esac
    sleep 2
  done
  echo "Import $1 did not complete in time" >&2
  return 1
}

gzip -k -f "$FILE"

IMPORT_ID=$(curl -s -S -f -X POST "https://api.docudevs.ai/cases/$CASE_ID/records/imports" \
  -H "Authorization: $API_KEY" \
  -H "Content-Type: application/json" \
  -d '{"mode": "upsert", "format": "csv", "gzip": true}' | jq -r '.importId')

# Blocks of at most 32 MiB, numbered from 0; the file is the blocks in index order
rm -f block-*
split -b 32m "$FILE.gz" block-
BLOCKS=0
for BLOCK in block-*; do
  curl -s -S -f -o /dev/null -X PUT "https://api.docudevs.ai/cases/$CASE_ID/records/imports/$IMPORT_ID/upload/blocks/$BLOCKS" \
    -H "Authorization: $API_KEY" \
    -H "Content-Type: application/octet-stream" \
    --data-binary @"$BLOCK"
  BLOCKS=$((BLOCKS + 1))
done

curl -s -S -f -o /dev/null -X POST "https://api.docudevs.ai/cases/$CASE_ID/records/imports/$IMPORT_ID/commit?blockCount=$BLOCKS" \
  -H "Authorization: $API_KEY"

wait_for_import "$IMPORT_ID"
echo "$IMPORT" | jq '{status, rowsReceived, rowsRejected, recordsAdded, recordsUpdated, recordsUnchanged}'
```

**Python**

```python
case_id = 42  # id of a records case with a configured dataset

# Compresses the file and uploads it in blocks. Raises RecordsImportFailed when the
# import fails, and TimeoutError after 1800 seconds.
result = await client.bulk_upsert_records(
    case_id,
    "products.csv",  # columns as named in the dataset field mapping
    wait=True,
    on_progress=print,
)
print(result["status"], result["rowsReceived"], result["rowsRejected"])
print(result["recordsAdded"], result["recordsUpdated"], result["recordsUnchanged"])
```

**CLI**

```bash
CASE_ID=42  # id of a records case with a configured dataset

docudevs records bulk "$CASE_ID" products.csv --wait
```

The Python client and the CLI choose the path for you:

- Up to 1,000 rows (and 10 MB) of ndjson or row dictionaries are sent as one batch. Python also accepts a list of row dictionaries instead of a file path.
- CSV files and anything larger become a file import. The client gzip-compresses the input, uploads it through the API in blocks and commits it.
- A mapping overrides the dataset mapping for one call (`mapping` in Python, `--mapping-file` on the CLI, `mapping` in the request).
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

**cURL**

```bash
CASE_ID=42  # id of a records case with a configured dataset

# The worker fetches the file, so the URL must be https and publicly reachable.
# The import is queued when it is created; there is no commit step.
curl -s -S -f -X POST "https://api.docudevs.ai/cases/$CASE_ID/records/imports" \
  -H "Authorization: $API_KEY" \
  -H "Content-Type: application/json" \
  -d '{
    "mode": "upsert",
    "format": "csv",
    "gzip": false,
    "sourceUrl": "https://example.com/exports/products.csv"
  }' | jq '{importId, status}'
# Read GET /cases/$CASE_ID/records/imports/<importId> until its status is COMPLETED or FAILED
```

**Python**

```python
case_id = 42  # id of a records case with a configured dataset

# The worker fetches the file, so the URL must be https and publicly reachable
result = await client.import_records_from_url(
    case_id,
    "https://example.com/exports/products.csv",
    format="csv",
    wait=True,
)
print(result["status"], result["rowsReceived"], result["recordsAdded"])
```

**CLI**

```bash
CASE_ID=42  # id of a records case with a configured dataset

docudevs records import-url "$CASE_ID" "https://example.com/exports/products.csv" --format csv --wait
```

The Python client and the CLI wait for the import; the cURL variant returns after creating it, so read the import status until it is final. The URL must be `https`, must not contain user information and must not point at a private or local address. DocuDevs enforces a size cap. The import is queued when it is created; there is no commit step.

## Import status

`GET /cases/{caseId}/records/imports/{importId}` returns `status`, `mode`, `dryRun`, `jobGuid`, and the counters `rowsReceived`, `rowsRejected`, `recordsAdded`, `recordsUpdated`, `recordsDeleted` and `recordsUnchanged`.

**cURL**

```bash
CASE_ID=42  # id of a records case
IMPORT_ID=301  # importId returned when the import was created

curl -s -S -f "https://api.docudevs.ai/cases/$CASE_ID/records/imports/$IMPORT_ID" \
  -H "Authorization: $API_KEY" | jq '{status, rowsReceived, rowsRejected, recordsAdded, recordsUpdated, recordsDeleted, recordsUnchanged}'
```

**Python**

```python
case_id = 42  # id of a records case
import_id = 301  # importId returned when the import was created

current = await client.get_import(case_id, import_id)
print(current["status"], current["rowsReceived"], current["rowsRejected"])
print(current["recordsAdded"], current["recordsUpdated"], current["recordsDeleted"])
```

**CLI**

```bash
CASE_ID=42  # id of a records case
IMPORT_ID=301  # importId returned when the import was created

docudevs records import-status "$CASE_ID" "$IMPORT_ID"
```

| Status | Meaning |
| --- | --- |
| `STAGING` | The request is being stored. |
| `AWAITING_UPLOAD` | Waiting for the file's blocks and the commit. |
| `COMMITTING`, `QUEUED_PENDING`, `QUEUED` | Accepted and waiting for the worker. |
| `RUNNING` | The worker is applying it. |
| `COMPLETED` | Applied. |
| `FAILED` | Not applied. |

Only `COMPLETED` and `FAILED` are final: keep reading until the import reaches one of them. The job behind an import reports its progress with `GET /job/status/{guid}`; the response carries a `progress` object (`phase`, `done`, `total`, `failed`).

**cURL**

```bash
CASE_ID=42  # id of a records case
IMPORT_ID=301  # importId returned when the import was created

DEADLINE=$((SECONDS + 1800))  # same limit as the Python timeout
while [ "$SECONDS" -lt "$DEADLINE" ]; do
  IMPORT=$(curl -s -S -f "https://api.docudevs.ai/cases/$CASE_ID/records/imports/$IMPORT_ID" \
    -H "Authorization: $API_KEY")
  STATUS=$(echo "$IMPORT" | jq -r '.status')
  case "$STATUS" in
    COMPLETED) echo "$IMPORT" | jq '{status, recordsAdded, recordsUpdated}'; exit 0 ;;
    FAILED) echo "Import $IMPORT_ID failed" >&2; exit 1 ;;
  esac
  sleep 2
done
echo "Import $IMPORT_ID did not complete in time" >&2
exit 1
```

**Python**

```python
case_id = 42  # id of a records case
import_id = 301  # importId returned when the import was created

# Raises RecordsImportFailed when the import fails, and TimeoutError after 1800 seconds
finished = await client.wait_for_import(case_id, import_id, on_progress=print)
print(finished["status"], finished["recordsAdded"], finished["recordsUpdated"])
```

**CLI**

```bash
CASE_ID=42  # id of a records case
IMPORT_ID=301  # importId returned when the import was created

docudevs records import-status "$CASE_ID" "$IMPORT_ID" --wait
```

The client waits and calls your progress callback with that `progress` object; the cURL variant reads the import itself.

### Retry a failed import

`POST /cases/{caseId}/records/imports/{importId}/retry` queues a failed import again with a new job (`202`). The worker resumes from its saved progress and does not re-apply table changes that were already merged. The staged input is kept for 7 days after a failure. The call returns `409` when the import has not failed, when the system retries it itself, or when its staged input is gone or changed, and `503` when it could not be queued.

**cURL**

```bash
CASE_ID=42  # id of a records case
IMPORT_ID=301  # id of the import that failed

curl -s -S -f -X POST "https://api.docudevs.ai/cases/$CASE_ID/records/imports/$IMPORT_ID/retry" \
  -H "Authorization: $API_KEY" | jq '{importId, status}'
```

**Python**

```python
case_id = 42  # id of a records case
import_id = 301  # id of the import that failed

# Raises RecordsApiError (409) when the import has not failed
accepted = await client.retry_import(case_id, import_id)
print(f"Import {accepted['importId']}: {accepted['status']}")
```

**CLI**

```bash
CASE_ID=42  # id of a records case
IMPORT_ID=301  # id of the import that failed

docudevs records import-retry "$CASE_ID" "$IMPORT_ID"
```

Rows that cannot be mapped (for example a missing key) are rejected and counted in `rowsRejected`; the other rows are still applied. Writes to a case are applied one at a time, in order.

## Snapshot sync

When you only have a full export, a sync makes the dataset equal to it. It is **a dry run by default** and changes nothing until you confirm.

**cURL**

```bash
CASE_ID=42  # id of a records case with a configured dataset
FILE=snapshot.ndjson  # the full export, with columns as named in the dataset field mapping

wait_for_import() {
  local deadline=$((SECONDS + 1800))  # same limit as the Python timeout
  while [ "$SECONDS" -lt "$deadline" ]; do
    IMPORT=$(curl -s -S -f "https://api.docudevs.ai/cases/$CASE_ID/records/imports/$1" \
      -H "Authorization: $API_KEY")
    case "$(echo "$IMPORT" | jq -r '.status')" in
      COMPLETED) return 0 ;;
      FAILED) echo "Import $1 failed" >&2; return 1 ;;
    esac
    sleep 2
  done
  echo "Import $1 did not complete in time" >&2
  return 1
}

# Creates the import from the options in $1, uploads the file in blocks of at
# most 32 MiB, commits it and waits for it
sync_file() {
  IMPORT_ID=$(curl -s -S -f -X POST "https://api.docudevs.ai/cases/$CASE_ID/records/imports" \
    -H "Authorization: $API_KEY" \
    -H "Content-Type: application/json" \
    -d "$1" | jq -r '.importId')
  rm -f block-*
  split -b 32m "$FILE.gz" block-
  BLOCKS=0
  for BLOCK in block-*; do
    curl -s -S -f -o /dev/null -X PUT "https://api.docudevs.ai/cases/$CASE_ID/records/imports/$IMPORT_ID/upload/blocks/$BLOCKS" \
      -H "Authorization: $API_KEY" \
      -H "Content-Type: application/octet-stream" \
      --data-binary @"$BLOCK"
    BLOCKS=$((BLOCKS + 1))
  done
  curl -s -S -f -o /dev/null -X POST "https://api.docudevs.ai/cases/$CASE_ID/records/imports/$IMPORT_ID/commit?blockCount=$BLOCKS" \
    -H "Authorization: $API_KEY"
  wait_for_import "$IMPORT_ID"
}

gzip -k -f "$FILE"

sync_file '{"mode": "sync", "format": "ndjson", "gzip": true, "dryRun": true}'
DRY_RUN_ID=$IMPORT_ID
echo "$IMPORT" | jq '.syncSummary | {counts, guard}'

# After reviewing the report, apply the same file for real
sync_file '{"mode": "sync", "format": "ndjson", "gzip": true, "dryRun": false, "reportId": "'"$DRY_RUN_ID"'"}'
echo "$IMPORT" | jq '{status, recordsAdded, recordsUpdated, recordsDeleted, recordsUnchanged}'
```

**Python**

```python
case_id = 42  # id of a records case with a configured dataset

# Raises RecordsImportFailed when an import fails, and TimeoutError after 1800 seconds
dry = await client.sync_records(case_id, "snapshot.ndjson", wait=True)
summary = dry.get("syncSummary")
if summary is None:
    raise RuntimeError("The dry run produced no report")
print(summary["counts"])
print(summary.get("guard"))

# After reviewing the report, apply the same file for real
applied = await client.confirm_sync_records(
    case_id, "snapshot.ndjson", report_id=dry["importId"], wait=True
)
print(applied["status"], applied["recordsAdded"], applied["recordsDeleted"])
```

**CLI**

```bash
CASE_ID=42  # id of a records case with a configured dataset

# A dry run: it changes nothing and prints the report
docudevs records sync "$CASE_ID" snapshot.ndjson > dry-run.json
jq '.syncSummary | {counts, guard}' dry-run.json

# After reviewing the report, apply the same file for real
docudevs records sync "$CASE_ID" snapshot.ndjson --confirm "$(jq -r '.importId' dry-run.json)"
```

The first run is the dry run and prints its report. The second run applies the same file and references the dry run by its import id.

- `syncSummary.counts` holds `received`, `rejected`, `inserted`, `reembed`, `metadataOnly`, `deleted`, `unchanged` and `keptReviewed`. `samples` lists at most 20 record keys for each of `toInsert`, `reembed`, `metadataOnly`, `toDelete` and `keptReviewed`. `inputContentHash` identifies the input. `guard` compares the share of records the sync would delete with the limit (`existingRecords`, `deleteFraction`, `maxDeleteFraction`, `exceedsLimit`).
- A real run must reference the completed dry run (`reportId`, the import id of the dry run) and the same input. It is refused when the share of records it would delete exceeds `maxDeleteFraction` (default 0.2), unless `confirmLargeDelete` is true. This protects against a truncated export wiping the dataset.
- Records with `labelSource: review` are kept even when absent from the snapshot (`reviewedRecords: keep`, the default; `delete` removes them). If the snapshot carries a different label for a reviewed record, the review label wins (`reviewedLabelPolicy: reviewWins`, the default; `importWins` overrides it) and the conflict is listed in `labelConflicts`.
