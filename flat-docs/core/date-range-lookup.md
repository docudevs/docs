---
title: Date-Range Lookup (Calendars)
description: Resolve calendar entries such as public holidays that fall between two extracted dates using a knowledge base calendar dataset.
---
<!-- Generated from docs/core/date-range-lookup.md. Do not edit by hand. -->

The `DATE_RANGE_LOOKUP` tool returns calendar entries — for example public holidays — whose date or date span overlaps an inclusive `[start, end]` range. Upload a tabular calendar to a [knowledge base case](./knowledge-search.md), flag it as a calendar dataset, and bind the tool during extraction. The model extracts the two bound dates from the document and calls the tool to resolve matching entries deterministically, instead of reasoning about holiday dates itself.

## Prepare a calendar dataset

Provide a table (CSV, XLSX, or JSON) with one row per entry. Column names are matched case-insensitively:

| Purpose | Primary column | Accepted aliases |
| --- | --- | --- |
| Entry date (required) | `date` | `day`, `holiday_date` |
| Label | `label` | `name`, `title`, `description` |
| End date (optional, multi-day spans) | `endDate` | `end_date`, `end`, `until`, `to` |

Dates use ISO `YYYY-MM-DD`. Rows without a parseable date are skipped. Any additional columns are preserved under each entry's `payload`.

Example `public-holidays-2024.csv`:

```csv
date,label,endDate
2024-12-25,Christmas Day,
2024-12-24,Holiday Break,2024-12-26
```

## Upload the calendar to a knowledge base

Upload the file to a knowledge base case, flagged as a calendar dataset with the `datasetType=calendar` part (Python `dataset_type="calendar"`). The CLI has no option for the dataset type.

**cURL**

```bash
CASE_ID=42  # id of the knowledge base, from GET /knowledge-bases

curl -s -S -f -o /dev/null -w "Upload status: %{http_code}\n" -X POST "https://api.docudevs.ai/cases/$CASE_ID/documents" \
  -H "Authorization: $API_KEY" \
  -F "document=@public-holidays-2024.csv" \
  -F "datasetType=calendar"
```

**Python**

```python
from docudevs.models import UploadCaseDocumentBody
from docudevs.types import File

case_id = 42  # id of the knowledge base, from list_knowledge_bases

with open("public-holidays-2024.csv", "rb") as f:
    file_data = f.read()

body = UploadCaseDocumentBody(
    document=File(payload=file_data, file_name="public-holidays-2024.csv", mime_type="text/csv")
)
response = await client.upload_case_document(case_id=case_id, body=body, dataset_type="calendar")
print(f"Upload status: {response.status_code}")
```

The worker recognizes the calendar dataset, parses it into a date index, and skips text embedding. Deleting the document removes its calendar entries.

## Use the tool during extraction

Bind the `DATE_RANGE_LOOKUP` tool in the extraction request's `tools`, the same way as other knowledge base tools. The `caseId` in `config` points at the knowledge base holding the calendar:

```json
{
  "extractionMode": "SIMPLE",
  "prompt": "Extract the stay's start and end dates. Using the date_range_lookup tool, resolve the public holidays between those two dates and return them in holidays.",
  "tools": [
    { "type": "DATE_RANGE_LOOKUP", "config": { "caseId": "42" } }
  ]
}
```

The `config` key is `caseId`: the knowledge base case id that holds the calendar, as a string or a number.

## Tool contract

The model calls the tool with:

| Argument | Type | Description |
| --- | --- | --- |
| `start` | string | Inclusive ISO `YYYY-MM-DD` start date. |
| `end` | string | Inclusive ISO `YYYY-MM-DD` end date. |
| `inclusive` | boolean | Whether the range bounds are inclusive (default `true`). |

It returns:

```json
{
  "range": { "start": "2024-12-23", "end": "2024-12-27", "inclusive": true },
  "matches": [
    { "date": "2024-12-24", "endDate": "2024-12-26", "label": "Holiday Break", "payload": {} },
    { "date": "2024-12-25", "endDate": "2024-12-25", "label": "Christmas Day", "payload": {} }
  ],
  "count": 2,
  "any": true
}
```

`matches` are sorted by date then label. `count` and `any` answer "how many" and "are there any" in a single call. When the knowledge base has no calendar data, the result includes a `note` field with an empty `matches` list; invalid bounds (unparseable dates, or `start` after `end`) return an `error` field instead.
