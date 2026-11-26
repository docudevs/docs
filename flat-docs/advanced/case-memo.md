---
title: Case memo
description: Keep scoped instructions, feedback and corrections for a case, with revisions and human approval, and have classify jobs use them.
sidebar_position: 7
---
<!-- Generated from docs/advanced/case-memo.md. Do not edit by hand. -->

# Case memo

The memo is where people teach a case how it should classify: filing rules, comments on results and corrections. It is a set of **scoped entries** with a revision history, available on any case. Classify jobs load the entries that apply to each row and record which memo version they used, so every result can be traced to the rules behind it.

## Entries

| Field | Values |
| --- | --- |
| `type` | `instruction` (a rule), `feedback` (a comment on a result), `correction` (created by a review; a label fix with its reason) |
| `scope.kind` | `global`; `subtree` with a category `code` (that category and its descendants); `record` with a record `key`; `document` with a `documentId` |
| `status` | `proposed`, `active`, `retired` |
| `text` | Up to 8,000 characters. |
| `links` | Optional `classificationResults` (`jobId`, `rowId`) and `recordKeys` the entry relates to |

`subtree` and `record` scopes only make sense on a records case. Every change is a new **revision**; the **memo version** is the highest revision of the case, and the entries active at any version can be read back.

Lifecycle:

- An **instruction** starts as `proposed` and becomes `active` only when a human approves it. Nothing changes in classification until then.
- **Feedback** is stored active and is inert: it does not change classification. It exists to be turned into an instruction.
- A **correction** is created by a [classify review](./classification.md#review) and applies directly.
- **Retiring** an entry adds a revision with status `retired`; earlier versions stay readable.

## API

| Method | Path | Notes |
| --- | --- | --- |
| `GET` | `/cases/{caseId}/memo` | Filters `type`, `scope`, `status`; `version=` reads the snapshot at that memo version. Returns `memoVersion` and `items`. |
| `POST` | `/cases/{caseId}/memo/entries` | Create an `instruction` or `feedback` entry. Returns `201` with `id`, `status`, `revision`, `memoVersion`. |
| `POST` | `/cases/{caseId}/memo/entries/{entryId}/approve` | Approve a proposed instruction. Your API key or a signed-in user can approve; the agent cannot. `409` if the entry is not a proposed instruction. |
| `POST` | `/cases/{caseId}/memo/entries/{entryId}/retire` | Retire an entry. `409` if it is already retired. |
| `GET` | `/cases/{caseId}/memo/entries/{entryId}/revisions` | Every revision of an entry. |

```json
POST /cases/42/memo/entries
{
  "type": "instruction",
  "scope": {"kind": "subtree", "code": "7.31"},
  "text": "Fiction books are filed by genre when the description names one; otherwise use 7.31.99.",
  "links": {"classificationResults": [{"jobId": "55", "rowId": 17}]}
}
```

```json
{"id": "m_88", "status": "proposed", "revision": 41, "memoVersion": 41}
```

Any organization member with access to the knowledge base can write entries, and so can an organization API key. The case agent can propose instructions and leave feedback, but approving as the agent answers `403`. See [Agent actions](./agent-actions.md).

### Add and approve an instruction

**cURL**

```bash
CASE_ID=42  # id of a records case

ENTRY_ID=$(curl -s -S -f -X POST "https://api.docudevs.ai/cases/$CASE_ID/memo/entries" \
  -H "Authorization: $API_KEY" \
  -H "Content-Type: application/json" \
  -d '{
    "type": "instruction",
    "scope": {"kind": "subtree", "code": "7.31"},
    "text": "Fiction books are filed by genre when the description names one."
  }' | jq -r '.id')
echo "Proposed entry $ENTRY_ID"

# Nothing changes in classification until the instruction is approved
curl -s -S -f -X POST "https://api.docudevs.ai/cases/$CASE_ID/memo/entries/$ENTRY_ID/approve" \
  -H "Authorization: $API_KEY" | jq '{id, status, memoVersion}'
```

**Python**

```python
import asyncio
import os

from docudevs.docudevs_client import DocuDevsClient


async def main():
    client = DocuDevsClient(token=os.environ["API_KEY"])

    case_id = 42  # id of a records case

    entry = await client.add_memo_entry(
        case_id,
        "instruction",
        {"kind": "subtree", "code": "7.31"},
        "Fiction books are filed by genre when the description names one.",
    )
    print("Proposed entry", entry["id"])

    # Nothing changes in classification until the instruction is approved
    approved = await client.approve_memo_entry(case_id, entry["id"])
    print(approved["id"], approved["status"], approved["memoVersion"])


asyncio.run(main())
```

**Java**

```java
import com.fasterxml.jackson.databind.JsonNode;

long caseId = 42L; // id of a records case

JsonNode entry = client.addMemoEntry(
        caseId,
        "instruction",
        Map.of("kind", "subtree", "code", "7.31"),
        "Fiction books are filed by genre when the description names one.");
System.out.println("Proposed entry " + entry.path("id").asText());

// Nothing changes in classification until the instruction is approved
JsonNode approved = client.approveMemoEntry(caseId, entry.path("id").asText());
System.out.println(approved.path("id").asText() + " " + approved.path("status").asText()
        + " " + approved.path("memoVersion").asText());
```

**CLI**

```bash
docudevs memo add 42 instruction "Fiction books are filed by genre when the description names one." --scope subtree --code 7.31

# Nothing changes in classification until the instruction is approved; use the id the add command printed
docudevs memo approve 42 m_88
```

The `subtree` scope needs a records case. Use `{"kind": "global"}` on any case. The CLI prints the new entry; use its `id` in the approve command.

### Read, review and retire entries

**cURL**

```bash
CASE_ID=42  # id of a records case
ENTRY_ID=m_88  # id returned when the entry was created

curl -s -S -f "https://api.docudevs.ai/cases/$CASE_ID/memo?status=active" \
  -H "Authorization: $API_KEY" | jq -r '.items[]? | "\(.id) \(.type) \(.status)"'

curl -s -S -f "https://api.docudevs.ai/cases/$CASE_ID/memo/entries/$ENTRY_ID/revisions" \
  -H "Authorization: $API_KEY" | jq -r '.items[]? | "revision \(.revision): \(.status)"'

# Retiring adds a revision; earlier versions stay readable
curl -s -S -f -X POST "https://api.docudevs.ai/cases/$CASE_ID/memo/entries/$ENTRY_ID/retire" \
  -H "Authorization: $API_KEY" | jq '{id, status, memoVersion}'
```

**Python**

```python
case_id = 42  # id of a records case
entry_id = "m_88"  # id returned when the entry was created

memo = await client.list_memo(case_id, status="active")
for entry in memo.get("items") or []:
    print(entry["id"], entry["type"], entry["status"])

history = await client.list_memo_revisions(case_id, entry_id)
for revision in history.get("items") or []:
    print(f"revision {revision['revision']}: {revision['status']}")

# Retiring adds a revision; earlier versions stay readable
retired = await client.retire_memo_entry(case_id, entry_id)
print(retired["id"], retired["status"], retired["memoVersion"])
```

**Java**

```java
import ai.docudevs.client.MemoListOptions;
import com.fasterxml.jackson.databind.JsonNode;

long caseId = 42L; // id of a records case
String entryId = "m_88"; // id returned when the entry was created

JsonNode memo = client.listMemo(caseId, MemoListOptions.builder().status("active").build());
for (JsonNode entry : memo.path("items")) {
    System.out.println(entry.path("id").asText() + " " + entry.path("type").asText()
            + " " + entry.path("status").asText());
}

JsonNode history = client.listMemoRevisions(caseId, entryId);
for (JsonNode revision : history.path("items")) {
    System.out.println("revision " + revision.path("revision").asText() + ": " + revision.path("status").asText());
}

// Retiring adds a revision; earlier versions stay readable
JsonNode retired = client.retireMemoEntry(caseId, entryId);
System.out.println(retired.path("id").asText() + " " + retired.path("status").asText()
        + " " + retired.path("memoVersion").asText());
```

**CLI**

```bash
docudevs memo list 42 --status active
docudevs memo revisions 42 m_88

# Retiring adds a revision; earlier versions stay readable
docudevs memo retire 42 m_88
```

## How a classify job uses the memo

A job does not load the whole memo. For each row it loads the **active** entries that apply: global entries, subtree entries whose subtree contains a candidate category of the row, entries scoped to the row's own key, and entries scoped to the row's document. Active corrections scoped to a record or subtree are loaded like instructions; feedback never is. Order is global first, then by subtree depth.

A token budget caps the memo text per row (about 1,500 tokens). If it is exceeded, the more specific entries win and the row gets the `memoTruncated` flag. The job pins the memo version when it starts (`config.memo.version` pins an older one, `use: "none"` ignores the memo), and every result stores the memo version and the ids of the entries it used.

When a job classifies rows of one case against another case's knowledge base, the knowledge base's entries are loaded first, then the work case's global and document entries.

To try a proposed instruction before approving it, list its id in `config.memo.include`, run the job, and compare the results with a run that leaves it out. To measure the effect over a grouped holdout sample before approving, use the [memo A/B evaluation](./case-evals.md#test-a-proposed-memo-entry).

When a category tree is activated, the activation report lists memo entries whose scope names a retired or missing category. DocuDevs does not remap them: they keep working on the old codes until you update or retire them.
