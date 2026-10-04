---
title: Case memo
description: Keep scoped instructions, feedback and corrections for a case, with revisions and human approval, and have classify jobs use them.
sidebar_position: 7
---

# Case memo

The memo is where people teach a case how it should classify: filing rules, comments on results and corrections. It is a set of **scoped entries** with a revision history, available on any case. Classify jobs load the entries that apply to each row and record which memo version they used, so every result can be traced to the rules behind it.

## Entries

| Field | Values |
| --- | --- |
| `type` | `instruction` (a rule), `feedback` (a comment on a result), `correction` (created by a review; a label fix with its reason) |
| `scope.kind` | `global`; `subtree` with a category `code` (that category and its descendants); `record` with a record `key`; `document` with a `documentId` |
| `status` | `proposed`, `active`, `retired` |
| `text` | Up to 8,000 characters. Stored in blob storage, not in the database. |
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
| `POST` | `/cases/{caseId}/memo/entries/{entryId}/approve` | Approve a proposed instruction. Human credentials only. `409` if the entry is not a proposed instruction. |
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

Any organization member with access to the knowledge base can write entries. Requests that carry the header `X-DocuDevs-Actor: agent` are treated as agent actions: an agent can propose instructions and leave feedback, but approving returns `403`. Treat this as a guard against accidents rather than a security boundary, because the header comes from the caller.

## How a classify job uses the memo

A job does not load the whole memo. For each row it loads the **active** entries that apply: global entries, subtree entries whose subtree contains a candidate category of the row, entries scoped to the row's own key, and entries scoped to the row's document. Active corrections scoped to a record or subtree are loaded like instructions; feedback never is. Order is global first, then by subtree depth.

A token budget caps the memo text per row (about 1,500 tokens). If it is exceeded, the more specific entries win and the row gets the `memoTruncated` flag. The job pins the memo version when it starts (`config.memo.version` pins an older one, `use: "none"` ignores the memo), and every result stores the memo version and the ids of the entries it used.

When a job classifies rows of one case against another case's knowledge base, the knowledge base's entries are loaded first, then the work case's global and document entries.

To try a proposed instruction before approving it, list its id in `config.memo.include`, run the job, and compare the results with a run that leaves it out.

When a category tree is activated, the activation report lists memo entries whose scope names a retired or missing category. DocuDevs does not remap them: they keep working on the old codes until you update or retire them.

## Python SDK

```python
entry = await client.add_memo_entry(
    case_id,
    "instruction",
    {"kind": "subtree", "code": "7.31"},
    "Fiction books are filed by genre when the description names one.",
)
await client.approve_memo_entry(case_id, entry["id"])

memo = await client.list_memo(case_id, status="active")
history = await client.list_memo_revisions(case_id, entry["id"])
await client.retire_memo_entry(case_id, entry["id"])
```

## CLI

```bash
docudevs memo add 42 instruction "Fiction books are filed by genre." --scope subtree --code 7.31
docudevs memo list 42 --status proposed
docudevs memo approve 42 m_88
docudevs memo revisions 42 m_88
docudevs memo retire 42 m_88
```
