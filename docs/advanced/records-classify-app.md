---
title: Records and classify in the app
description: Manage records, category trees, classify jobs, review and the case memo from the DocuDevs web app.
sidebar_position: 8
---

# Records and classify in the app

The web app covers the same ground as the [records](./records-datasets.md), [category tree](./category-trees.md), [classify](./classification.md) and [memo](./case-memo.md) APIs. Everything here is also available through the API, the SDKs and the CLI.

## Where to find it

Open a case. A case with a records dataset shows links to **Records**; every case shows a link to the **Memo**. The Records area has four tabs:

| Tab      | What it does                                                                                                                                                            |
| -------- | ----------------------------------------------------------------------------------------------------------------------------------------------------------------------- |
| Overview | The dataset configuration, per-category statistics (with a badge on rare categories), a records browser with filters, semantic record search and a record detail panel. |
| Imports  | The import history with live progress, and a dialog to start a new import.                                                                                              |
| Trees    | Tree versions, an upload form, a tree browser and search, and activation.                                                                                               |
| Classify | The classify jobs of the case, and a form to start a new one.                                                                                                           |

Changing a dataset setting that needs a reindex asks you to confirm before it resends the change with the reindex flag.

## Importing records

Start an import from a file or from a source URL, then commit it. Files are streamed through the app to storage and are limited by the size the app server accepts. Larger files go through the SDK or the CLI. A snapshot sync runs as a wizard: a dry run, a summary with counts, samples and conflicts, then a confirmation. If the delete guard trips, you must tick a large-delete box before confirming. A failed import can be retried.

## Category trees

Upload a tree version as JSON or CSV. A CSV is mapped to tree fields in the browser, and problems the server finds are listed grouped by kind. The tree browser loads children on demand and works with the keyboard. Activating a version asks for a successor for each retired category (or allows missing ones) and then shows a report: valid, retired and missing counts, and memo scopes that no longer match, each linking to the memo.

## Starting a classify job

The start form uses the same defaults as the service: the active tree, leaf-only choices and the MINI level. Give rows either as record keys (up to 10,000) or as inline CSV (up to 1,000 rows or 10 MB), and pick which columns feed the model. A job with `target` set to record matches rows against a knowledge base instead of a category tree. A token budget is filled in for you from the row count.

The job page shows counts, token usage, the pinned tree and memo versions, progress, and a cancel button while it runs.

## Reading results

The results table lists the chosen path, flags, review state and any review decision. Filters cover flag, review required, level, code, decision and a routing override. A job that is still running shows the rows written so far. The **Download CSV** link carries the current filters. Open a row to see its input columns, the decision and alternative, flags and reasons, candidates, the evidence records and the memo entries it used (both load on request), token usage and errors.

## Reviewing a result

The row panel has three review actions.

- **Accept** asks you to confirm. For a category job with a record key, accepting writes the label as evidence for later jobs.
- **Correct** asks for the right category and a reason. The category picker searches the tree the job used, shows breadcrumbs, and disables retired categories with a hint about their successor. For a record-matching job you enter the record key instead.
- **Feedback** stores a comment as a memo entry.

After a review the panel shows the decision and the state of the label write, which updates until it finishes. A row without a key, a record-matching job and feedback do not write a record; the panel then says that the review was recorded and no record was written. A declined row cannot be accepted, only corrected. Reviews made from the app always count as a human decision.

### Keyboard review

| Key       | Action                                    |
| --------- | ----------------------------------------- |
| `j` / `k` | Next or previous row                      |
| `Enter`   | Open the row panel                        |
| `a`       | Accept the label (opens the confirmation) |
| `c`       | Correct the label                         |
| `f`       | Send feedback                             |
| `?`       | Show the shortcut list                    |

Shortcuts are off while you type in a field.

The **Review queue** button filters the results to rows that need review and have no decision yet. In the queue, finishing a review moves to the next undecided row.

## The memo

The memo page lists entries with filters by type, status and scope, shows a read-only snapshot of any earlier memo version, and lets you add feedback or propose an instruction (up to 8,000 characters, with a scope picker). Approving an instruction or retiring an entry asks for confirmation, and each entry has a revision history.

Results and memo entries link to each other:

- A memo entry created from a review links to the result row (`records/classify/{jobId}?row={rowId}`).
- A result row links to the memo entries it used (`memo?entry={entryId}`), and a review links to the entry it created.

A row link opens the row when it is on the first page of results. Otherwise the page tells you to use the filters or the paging to find it.
