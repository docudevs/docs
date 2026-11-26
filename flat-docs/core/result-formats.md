---
title: Result Formats
description: Download a job result as JSON, CSV or Excel, read the schema a job was processed with, and know what each route answers for OCR and batch jobs.
---
<!-- Generated from docs/core/result-formats.md. Do not edit by hand. -->

# Result Formats

A completed job keeps its result. You can read it as it was stored, or ask DocuDevs to turn it into a table as CSV or Excel. All of these routes work on the job GUID and only see jobs of your organization.

| Route | Answers with |
| --- | --- |
| `GET /job/result/{guid}` | The stored result: JSON for an extraction, the text itself for an OCR job, an overview for a batch |
| `GET /job/result/{guid}/json` | The result as JSON |
| `GET /job/result/{guid}/csv` | The result as a CSV table, sent as an attachment |
| `GET /job/result/{guid}/excel` | The result as an Excel workbook, sent as an attachment |
| `GET /job/result/{guid}/schema` | The schema the job was processed with |

Wait until the job status is `COMPLETED` before you read a result (except the batch overview below). A job without a stored result, and a GUID that does not belong to your organization, both answer `404`.

## Download JSON, CSV and Excel

The example downloads the three formats of one completed extraction job.

**cURL**

```bash
JOB_GUID="<job-guid>"  # GUID of a completed extraction job

# JSON: the stored result as it is
curl -s -S -f "https://api.docudevs.ai/job/result/$JOB_GUID/json" \
  -H "Authorization: $API_KEY" \
  --output result.json

# CSV: one row per record, nested fields as dotted column names
curl -s -S -f "https://api.docudevs.ai/job/result/$JOB_GUID/csv" \
  -H "Authorization: $API_KEY" \
  --output result.csv

# Excel: the same table as a workbook
curl -s -S -f "https://api.docudevs.ai/job/result/$JOB_GUID/excel" \
  -H "Authorization: $API_KEY" \
  --output result.xlsx
```

**Python**

```python
job_guid = "<job-guid>"  # GUID of a completed extraction job

# JSON: the stored result as it is, parsed
result = await client.result_json(job_guid)
with open("result.json", "w") as f:
    json.dump(result, f, indent=2)

# CSV: one row per record, nested fields as dotted column names
csv_text = await client.result_csv(job_guid)
with open("result.csv", "w") as f:
    f.write(csv_text)

# Excel: the same table as a workbook; save_to writes the file
await client.result_excel(job_guid, save_to="result.xlsx")
```

**Java**

```java
import com.fasterxml.jackson.databind.JsonNode;

String jobGuid = "<job-guid>"; // GUID of a completed extraction job

// JSON: the stored result as it is, parsed
JsonNode result = client.fetchResultJson(jobGuid);
Files.writeString(Path.of("result.json"), result.toPrettyString());

// CSV: one row per record, nested fields as dotted column names
byte[] csv = client.fetchResultCsvBytes(jobGuid);
Files.write(Path.of("result.csv"), csv);

// Excel: the same table as a workbook
byte[] workbook = client.fetchResultExcelBytes(jobGuid);
Files.write(Path.of("result.xlsx"), workbook);
```

The CSV and Excel routes flatten the result into a table:

- An object becomes one row. A list of objects becomes one row per object.
- Nested objects become dotted column names, such as `customer.name`.
- A list inside a record is not expanded into rows. It is written into its cell as JSON text.
- The columns of a list of objects are the union of the keys of all objects, in the order they first appear. A missing value is an empty cell.
- For a map-reduce job the rows are the `records` of the result.

For numbers that should arrive in Excel as numbers, make the field required in the schema (see [Example: Strict Invoice Schema](../basics/SimpleDocuments.md#example-strict-invoice-schema)).

In Python, `result_json`, `result_csv` and `result_excel` wrap these routes. `result_csv` and `result_excel` raise `FileNotFoundError` when the result is not found. `wait_until_ready` returns the same formats when you pass `result_format` as `"json"`, `"csv"` or `"excel"`. The CLI command `docudevs result <guid>` reads the stored result and has no format option.

### Excel with a template

When a saved configuration has an Excel template attached, name it in the `configuration` query parameter and the result is written into the template at the cell offset set in that configuration. See [Configuration](https://docs.docudevs.ai/docs/configuration/) for saving a configuration. If the configuration does not exist or has no template, the route returns the plain workbook. The file type follows the template, so a macro-enabled template gives an `.xlsm` file.

**cURL**

```bash
JOB_GUID="<job-guid>"  # GUID of a completed extraction job

# invoice-config is a saved configuration that has an Excel template attached.
# Name the output file .xlsm when the template is macro-enabled.
curl -s -S -f "https://api.docudevs.ai/job/result/$JOB_GUID/excel?configuration=invoice-config" \
  -H "Authorization: $API_KEY" \
  --output result.xlsx
```

**Python**

```python
job_guid = "<job-guid>"  # GUID of a completed extraction job

# invoice-config is a saved configuration that has an Excel template attached.
# Name the output file .xlsm when the template is macro-enabled.
await client.result_excel(job_guid, save_to="result.xlsx", configuration="invoice-config")
```

**Java**

```java
String jobGuid = "<job-guid>"; // GUID of a completed extraction job

// invoice-config is a saved configuration that has an Excel template attached.
// Name the output file .xlsm when the template is macro-enabled.
byte[] workbook = client.fetchResultExcelBytes(jobGuid, "invoice-config");
Files.write(Path.of("result.xlsx"), workbook);
```

A workbook normalization result is not a flat table: its CSV and Excel routes answer `400`. See [Workbook normalization](https://docs.docudevs.ai/docs/core/workbook-normalization).

## Results that are not JSON

The job result of an OCR-only job is text, not JSON. For these jobs:

- `GET /job/result/{guid}` returns the text, as `text/markdown` or `text/plain` depending on the OCR output format.
- `GET /job/result/{guid}/json`, `/csv` and `/excel` answer `415 Unsupported Media Type`.
- `GET /job/result/{guid}/schema` answers `404`, because no schema was sent.

**cURL**

```bash
JOB_GUID="<job-guid>"  # GUID of a completed OCR-only job

# Print the HTTP status of each result route instead of failing on it
for ROUTE in "" "/json" "/csv" "/excel" "/schema"; do
  CODE=$(curl -s -S -o /dev/null -w "%{http_code}" "https://api.docudevs.ai/job/result/$JOB_GUID$ROUTE" \
    -H "Authorization: $API_KEY")
  echo "/job/result/{guid}$ROUTE -> $CODE"
done
```

The Python `result_json` does not report the `415`: it falls back to the plain route and returns the text. `result_csv` and `result_excel` raise an exception for it. The Java client reads the stored text with `fetchResult`, but it has no method for the schema route. For these reasons the example has no Python or Java variant.

## Schema of a job

The schema route returns the schema that was sent with the processing request, as JSON. A job processed without a schema answers `404`. For a batch it returns the schema given when the batch was processed.

**cURL**

```bash
JOB_GUID="<job-guid>"  # GUID of a completed job that was processed with a schema

curl -s -S -f "https://api.docudevs.ai/job/result/$JOB_GUID/schema" \
  -H "Authorization: $API_KEY" | jq .
```

Neither the Python SDK nor the Java SDK has a method for this route, so it is called over HTTP.

## Batch results

A batch is a single job that holds one result per document. Its two result routes answer differently:

- `GET /job/result/{guid}` returns an overview as soon as the batch exists, also while it is still processing: `jobGuid`, `isBatch`, `total`, `completed`, `failed`, `state` and a `documents` list. Each entry of `documents` has the `index` in upload order, a `status` and the `result` or the `error` of that document.
- `GET /job/result/{guid}/json` returns only the results, as a list in upload order. A document without a result is `null`.

The `status` of a document is `COMPLETED`, `FAILED`, `PENDING` while it is still waiting, or `MISSING` when the batch has finished and no result was stored for it. `state` is `COMPLETED` when every document completed, `FAILED` when every document failed, and `PARTIAL` when some did and some did not. Until every document has finished it is `RUNNING` or the current job status. Once every document has finished, the job status is `COMPLETED` whatever `state` says, even when every document failed; `PARTIAL` and `FAILED` appear only in `state`.

The CSV and Excel routes of a batch work on the list of results: a document without a result becomes an empty row. In the CLI, `docudevs batch process --format json|csv|excel` waits for the batch and saves the result in that format to the `--output` file, or to a file named after the batch when you give none.

**cURL**

```bash
BATCH_GUID="<batch-guid>"  # GUID of a batch whose processing has finished

# Batch overview: state and one entry per document
curl -s -S -f "https://api.docudevs.ai/job/result/$BATCH_GUID" \
  -H "Authorization: $API_KEY" \
  | jq -r '"state: \(.state), completed: \(.completed // 0), failed: \(.failed // 0)", ((.documents // [])[] | "document \(.index): \(.status)")'

# Only the results, in upload order; a document without a result is null
curl -s -S -f "https://api.docudevs.ai/job/result/$BATCH_GUID/json" \
  -H "Authorization: $API_KEY" | jq .
```

**Python**

```python
batch_guid = "<batch-guid>"  # GUID of a batch whose processing has finished

# Batch overview: state and one entry per document
response = await client.result(batch_guid)
if response.status_code != 200:
    raise RuntimeError(f"HTTP {response.status_code}")
overview = json.loads(response.content)
print(f"state: {overview.get('state')}, completed: {overview.get('completed', 0)}, failed: {overview.get('failed', 0)}")
for document in overview.get("documents", []):
    print(f"document {document['index']}: {document['status']}")

# Only the results, in upload order; a document without a result is None
results = await client.result_json(batch_guid)
print(json.dumps(results, indent=2))
```

**Java**

```java
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

String batchGuid = "<batch-guid>"; // GUID of a batch whose processing has finished

// Batch overview: state and one entry per document
JsonNode overview = new ObjectMapper().readTree(client.fetchResult(batchGuid));
System.out.println("state: " + overview.path("state").asText()
        + ", completed: " + overview.path("completed").asInt(0)
        + ", failed: " + overview.path("failed").asInt(0));
for (JsonNode document : overview.path("documents")) {
    System.out.println("document " + document.path("index").asInt()
            + ": " + document.path("status").asText());
}

// Only the results, in upload order; a document without a result is null
JsonNode results = client.fetchResultJson(batchGuid);
System.out.println(results.toPrettyString());
```

The API leaves null and empty fields out of its responses, so read the lists and optional fields as optional, as the example does. See [Batch processing](https://docs.docudevs.ai/docs/core/batch-processing) for creating and processing a batch.
