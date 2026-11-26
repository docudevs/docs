---
title: Result Formats
description: Download a job result as JSON, CSV or Excel, read the schema a job was processed with, and know what each route answers for OCR and batch jobs.
---

# Result Formats

A completed job keeps its result. You can read it as it was stored, or ask DocuDevs to turn it into a table as CSV or Excel. All of these routes work on the job GUID and only see jobs of your organization.

| Route | Answers with |
| --- | --- |
| `GET /job/result/{guid}` | The stored result: JSON for an extraction, the text itself for an OCR job, an overview for a batch |
| `GET /job/result/{guid}/json` | The result as JSON |
| `GET /job/result/{guid}/csv` | The result as a CSV table, sent as an attachment |
| `GET /job/result/{guid}/excel` | The result as an Excel workbook, sent as an attachment |
| `GET /job/result/{guid}/schema` | The schema the job was processed with |

Wait until the job status is `COMPLETED` before you read a result. A job without a stored result, and a GUID that does not belong to your organization, both answer `404`.

## Download JSON, CSV and Excel

The example downloads the three formats of one completed extraction job.

<Example name="results/download-result-formats" />

The CSV and Excel routes flatten the result into a table:

- An object becomes one row. A list of objects becomes one row per object.
- Nested objects become dotted column names, such as `customer.name`.
- A list inside a record is not expanded into rows. It is written into its cell as JSON text.
- The columns of a list of objects are the union of the keys of all objects, in the order they first appear. A missing value is an empty cell.
- For a map-reduce job the rows are the `records` of the result.

In Python, `result_json`, `result_csv` and `result_excel` wrap these routes. `result_csv` and `result_excel` raise `FileNotFoundError` when the result is not found. `wait_until_ready` returns the same formats when you pass `result_format` as `"json"`, `"csv"` or `"excel"`. The CLI command `docudevs result <guid>` reads the stored result and has no format option.

### Excel with a template

When a saved configuration has an Excel template attached, name it in the `configuration` query parameter and the result is written into the template at the cell offset set in that configuration. See [Configuration](/docs/configuration/) for saving a configuration. If the configuration does not exist or has no template, the route returns the plain workbook. The file type follows the template, so a macro-enabled template gives an `.xlsm` file.

<Example name="results/download-excel-with-template" />

A workbook normalization result is not a flat table: its CSV and Excel routes answer `400`. See [Workbook normalization](/docs/core/workbook-normalization).

## Results that are not JSON

The job result of an OCR-only job is text, not JSON. For these jobs:

- `GET /job/result/{guid}` returns the text, as `text/markdown` or `text/plain` depending on the OCR output format.
- `GET /job/result/{guid}/json`, `/csv` and `/excel` answer `415 Unsupported Media Type`.
- `GET /job/result/{guid}/schema` answers `404`, because no schema was sent.

<Example name="results/check-non-json-result" />

The Python `result_json` does not report the `415`: it falls back to the plain route and returns the text. `result_csv` and `result_excel` raise an exception for it. For that reason the example has no Python variant.

## Schema of a job

The schema route returns the schema that was sent with the processing request, as JSON. A job processed without a schema answers `404`. For a batch it returns the schema given when the batch was processed.

<Example name="results/get-result-schema" />

The Python SDK has no method for this route, so it is called over HTTP.

## Batch results

A batch is a single job that holds one result per document. Its two result routes answer differently:

- `GET /job/result/{guid}` returns an overview as soon as the batch exists, also while it is still processing: `jobGuid`, `isBatch`, `total`, `completed`, `failed`, `state` and a `documents` list. Each entry of `documents` has the `index` in upload order, a `status` and the `result` or the `error` of that document.
- `GET /job/result/{guid}/json` returns only the results, as a list in upload order. A document without a result is `null`.

The `status` of a document is `COMPLETED`, `FAILED`, `PENDING` while it is still waiting, or `MISSING` when the batch has finished and no result was stored for it. `state` is `COMPLETED` when every document completed, `FAILED` when every document failed, and `PARTIAL` when some did and some did not. Until every document has finished it is `RUNNING` or the current job status. Once every document has finished, the job status is `COMPLETED` whatever `state` says, even when every document failed; `PARTIAL` and `FAILED` appear only in `state`.

The CSV and Excel routes of a batch work on the list of results: a document without a result becomes an empty row. In the CLI, `docudevs batch process --format json|csv|excel` waits for the batch and saves the result in that format to the `--output` file, or to a file named after the batch when you give none.

<Example name="results/get-batch-result" />

The API leaves null and empty fields out of its responses, so read the lists and optional fields as optional, as the example does. See [Batch processing](/docs/core/batch-processing) for creating and processing a batch.
