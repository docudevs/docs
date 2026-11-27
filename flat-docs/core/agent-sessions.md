---
title: Agent sessions
---
<!-- Generated from docs/core/agent-sessions.md. Do not edit by hand. -->

An agent session stores a conversation under an opaque `sessionId`. Save that ID against your own record and use it from any process authenticated as the same DocuDevs user. Send each new user message once and keep its client message ID for retries.

There are two session kinds:

- `platform-chat` is the agent in the DocuDevs web app. It is always available.
- `document-workspace` works on uploaded workbooks and can publish verified spreadsheet, Word and PDF results. It is available only on dedicated deployments that include the document workspace add-on. Contact support to find out whether yours does.

## Credentials

Agent sessions belong to a signed-in DocuDevs user. Calls use that user's access token, as the web app does, and the user must be an active Admin or Member of the organization. A user can read only their own sessions.

- An API key used for ordinary document processing receives `403` on the session API.
- A missing or invalid credential receives `401`.
- On dedicated deployments, a dedicated integration credential can own `document-workspace` sessions. It receives `403` on any other kind. Contact support to set one up.

An optional `caseId` on a `platform-chat` session requires current access to that case on each request.

## Create and resume

Send `POST /v1/agent/sessions` with the `Idempotency-Key` header (required, 1 to 128 characters) and a body such as `{"kind":"platform-chat"}`. The API returns `201` and the session snapshot. Repeat the same request after a timeout to recover the original ID.

| Response | Meaning |
| --- | --- |
| `400` | The key is malformed, the kind is unknown, or `caseId` was sent with a kind other than `platform-chat` |
| `404` | The `caseId` is not a case the user can read |
| `409` | The key was already used with a different kind or case |
| `503` | The session kind is not available on this deployment |

Use `GET /v1/agent/sessions/{sessionId}` to resume. The snapshot includes:

- `activeTurnId`, when a turn is in progress
- `recentTurns`, up to 25 of the latest turns
- `eventCursor`, the position to start the event stream from
- `links.turns`, the URL of the full turn history
- `capabilities`, the operations this session supports: `messages` and `files`, plus `artifacts` and `cancel` for `document-workspace`. When `messages` is absent, the session cannot accept a new message at the moment.

Page through the full turn history with `links.turns`, in chronological order, passing each `nextCursor` as `after`. List sessions with `GET /v1/agent/sessions?limit=25`, then pass the returned `nextCursor` as `after` until it is absent. `limit` defaults to 25 and accepts 1 to 100 on every paged route. Case-bound sessions appear only while the creator can access the case.

`DELETE /v1/agent/sessions/{sessionId}` returns `202` and immediately closes access to the session and its turns. Its files and history are then removed. Unknown or inaccessible session and turn IDs return `404`.

## Submit and poll

Send `POST /v1/agent/sessions/{sessionId}/messages` with a body such as `{"clientMessageId":"case-42-msg-1","text":"Summarize the discussion."}`. `clientMessageId` is 1 to 128 characters and `text` is 1 to 16,000 characters. A `202` response provides a `turnId` with status `QUEUED`. The turn can finish after the submitting process exits.

If the response is lost, retry with the same message ID, the same text and the same `inputFileIds`; the API returns the original turn.

| Response | Meaning |
| --- | --- |
| `400` | A field is missing or outside its limits |
| `409` | Another turn is still active, or the message ID was already used with different text or files |
| `503` | The session cannot run a turn at the moment |

Use `GET /v1/agent/sessions/{sessionId}/turns/{turnId}` to read the committed status. A turn progresses from `QUEUED` to `RUNNING` and finishes as `COMPLETED` or `FAILED`. A `document-workspace` turn can also finish as `CANCELLED`. Completed turns include `assistantText`. Failed turns include a `failureCode`:

| `failureCode` | Meaning |
| --- | --- |
| `EXECUTION_TIMEOUT` | The turn was not finished in time |
| `SOURCE_UNAVAILABLE` | A file selected for the turn was no longer available |
| `EXECUTION_FAILED` | The turn failed for another reason |

The Python client exposes these operations under `docudevs.api.agent_sessions`. The Java client exposes them as `AgentSessionsApi`.

## Work with files in platform chat

Upload a private attachment to `POST /v1/agent/sessions/{sessionId}/files` as multipart field `file`. An `Idempotency-Key` recovers the same file ID after a retry; reusing the key with a different file returns `409`. To link an existing document in the session's case, send `{"documentGuid":"…"}` to `POST /v1/agent/sessions/{sessionId}/files/links`. List files with `GET .../files` and download the original through `GET .../files/{fileId}/content`. `DELETE .../files/{fileId}` returns `204`; deleting a link leaves the case document in place.

Limits:

- A file can be up to 20 MiB. A larger file returns `413` and an empty file returns `400`.
- A session holds up to 20 files. A further upload returns `409`.
- A turn can select up to 10 files in `inputFileIds`.

The agent can read these private attachments: UTF-8 text (`.txt`), Markdown, CSV, PDF with a text layer, and DOCX. Text beyond 65,536 characters of a file is not read. Other file types upload successfully, but the agent cannot read them; this includes images, scanned PDFs and XLSX.

A linked case document uses the OCR the case already has. If that OCR has not completed, submitting a turn that selects the document returns `409`.

Ask for a saved extraction configuration by name and the agent applies its prompt and schema to the selected text. The completed turn lists each result in `extractionResults` with `fileId`, `configurationName`, `configurationRevision` and the `resultJson`. Only plain extraction configurations can be used: a configuration that needs a pipeline, lookup files, an Excel template, tools, map-reduce, barcodes, figures, source locations, AcroForm metadata or a page range is rejected. Private attachments cannot be used for batch extraction, PDF comparison or workbook normalization.

In a case-bound conversation, eligible case write proposals appear as `pendingActions` after their turn commits. The creator confirms or rejects them as described in [Agent actions and the pending-action inbox](../advanced/agent-actions.md).

## Document workspace workbooks

Where the `document-workspace` kind is available, create a session of that kind and upload value-only XLSX files using the same session file routes. Files that pass validation return `READY`. Use `inputFileIds` to select the files for a turn.

| Upload response | Meaning |
| --- | --- |
| `400` | The workbook is malformed, has more than 50,000 cells, expands to more than 16 MiB, or contains formulas, macros, or embedded or external content |
| `413` | The file is larger than 5 MiB |
| `415` | The file is not an XLSX workbook |

## Download workspace results

A completed turn can publish up to five files in value-only XLSX, DOCX or PDF format, with a limit of 5 MiB per file and 15 MiB per turn. For example, one turn can produce an XLSX reconciliation, a DOCX report and a PDF copy. DocuDevs performs format-specific checks and publishes the verified files together with the turn's reply. A correction creates a new revision ID.

- Use `GET /v1/agent/sessions/{sessionId}/artifacts` to list the session's published `revisions`. To find every file from a turn, match each revision's `turnId` to the completed turn ID and pass each `nextCursor` as `after` until there are no more pages.
- Each revision has `revisionId`, `turnId`, `name`, `mediaType`, `sizeBytes`, `sha256`, `passedChecks`, `accepted` and a `download` URL. GET the `download` URL with the same session credential. It returns the immutable bytes as an `application/octet-stream` attachment. Use `name` and `mediaType` from the revision to identify the file, and compare its SHA-256 digest with `sha256` to verify the downloaded bytes.
- The user who owns the session can mark a revision accepted with `POST /v1/agent/sessions/{sessionId}/artifacts/{revisionId}/accept`. An integration credential receives `403`. Acceptance applies to that revision and is distinct from its verification checks.
- Use `POST /v1/agent/sessions/{sessionId}/turns/{turnId}/cancel` to prevent a queued or running turn from publishing. Cancelling an already cancelled turn returns `CANCELLED`. A turn that already completed or failed returns `409`. Cancel exists only for `document-workspace` sessions.

In the Python SDK, use `list_agent_workspace_artifacts` and `download_agent_workspace_artifact` from `docudevs.api.agent_sessions` with an `AuthenticatedClient`. The `sync_detailed` and `asyncio_detailed` variants provide the HTTP status. On a successful download, the parsed file's `payload` contains the bytes. In the Java SDK, `AgentSessionsApi` exposes `listAgentWorkspaceArtifacts` and `downloadAgentWorkspaceArtifact`, which returns a `File`.

Workspace sessions are kept for a limited time from creation, 30 days unless your deployment sets another period. After that the session returns `404`, and its files, revisions and conversation content are removed.

## Replay after disconnect

Connect to `GET /v1/agent/sessions/{sessionId}/events` with `Accept: text/event-stream` and the same credential. Without a cursor, the stream starts with the oldest retained event. Send the last processed SSE event ID as `Last-Event-ID` to resume after it. To receive only events newer than a snapshot, start with the snapshot's `eventCursor`. Treat cursors as opaque and reuse them only in the session that issued them; a cursor from another session returns `400`.

Each turn event carries `turnId` and the turn's committed `status`. Poll that turn to obtain the assistant text. Ignore unrecognized event names and keep processing. Heartbeats carry no event ID. Connections close within a minute so clients reconnect with current credentials.

A session keeps its latest events, 1,000 by default. A replay cursor older than that returns `410` with `agent_session.replay_unavailable`. Fetch a new snapshot and reconnect from its cursor. Use the turn history to recover earlier messages and results.
