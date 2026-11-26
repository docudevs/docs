---
title: Track Changes
description: Read the tracked insertions, deletions and comments of a Word document, with author and date, in one request.
---

# Track Changes

Word records an edit that is made with Track Changes on as a tracked change, together with the author and the time. The track changes route reads those records out of a `.docx` file and returns them as JSON: the tracked insertions, the tracked deletions and the comments, each with its author and date.

The route is synchronous. You upload the file and get the answer in the same response; no job is created, so there is nothing to poll.

This is different from [Redline (document comparison)](/docs/core/document-compare). A redline compares two PDF versions of a document and works out the differences itself. The track changes route compares nothing: it reports the changes that Word already recorded inside one Word file.

## Request

```text
POST /document/track-changes
Content-Type: multipart/form-data

document = contract.docx
```

Only `.docx` files are accepted. The check looks at the file name: a name that does not end in `.docx` (in any letter case) answers `400 Bad Request`.

The route has no SDK method and no CLI command, so call it over HTTP, as in the example.

<Example name="track-changes/extract-track-changes" />

## Response

```json
{
  "insertions": [
    { "text": "within thirty days", "author": "A. Reviewer", "date": "2025-03-04T09:15:00Z" }
  ],
  "deletions": [
    { "text": "within sixty days", "author": "A. Reviewer", "date": "2025-03-04T09:15:00Z" }
  ],
  "comments": [
    { "id": "0", "text": "Align with the payment terms.", "author": "B. Editor", "date": "2025-03-05T14:02:00Z", "initials": "BE" }
  ],
  "summary": {
    "totalInsertions": 1,
    "totalDeletions": 1,
    "totalComments": 1,
    "authors": ["A. Reviewer", "B. Editor"]
  }
}
```

- `insertions` and `deletions` list the tracked insertions and deletions in document order, each with the `text` that was inserted or deleted. An insertion or deletion with no text, or only whitespace, is left out.
- `comments` lists every comment with its own `text`, its `id` and the `initials` of its author. The text of a comment is the comment itself, not the passage it is attached to.
- `author`, `date`, `id` and `initials` are present only when the document records them. A date that is a valid timestamp is returned as an ISO 8601 timestamp; any other value is returned as it is stored.
- `summary` counts the entries and lists, once each and in alphabetical order, the authors of all insertions, deletions and comments.
- Empty lists and missing fields are left out of the response, so read `insertions`, `deletions`, `comments` and `summary.authors` as optional.
- Only tracked insertions, tracked deletions and comments are reported. Other kinds of tracked change, such as formatting changes and moved text, are not.
