---
title: Agent sessions
---

An agent session stores a conversation under an opaque `sessionId`. Save that ID against your own record and use it from any process authenticated as the same DocuDevs principal. Send each new user message once and keep its client message ID for retries.

The beta agent in the DocuDevs UI uses `platform-chat` sessions by default. The optional `document-workspace` session kind is not available on all installations; contact support to enable it.

## Credentials

Agent sessions belong to a signed-in DocuDevs user, so calls use that user's access token (a user JWT, as the web app does). An optional `caseId` on `platform-chat` requires current access to that case on each request. A user can read their own sessions. An API key used for ordinary document processing receives `403` on the session API.

## Create and resume

Send `POST /v1/agent/sessions` with the `Idempotency-Key` header and a body such as `{"kind":"platform-chat"}`. The API returns `201` and the session snapshot. Repeat the same request after a timeout to recover the original ID. Reusing a key with a changed request returns `409`.

Use `GET /v1/agent/sessions/{sessionId}` to resume. The snapshot includes any active turn, up to 25 recent turns, and an event cursor. Use its `links.turns` URL to page through the full turn history in chronological order, passing each `nextCursor` as `after`. List sessions with `GET /v1/agent/sessions?limit=25`, then pass the returned `nextCursor` as `after` until it is absent. Case-bound sessions appear only while the creator can access the case.

`DELETE /v1/agent/sessions/{sessionId}` returns `202` and immediately closes access to the session and its turns. The workspace retention job removes the files and history after deletion. Unknown or inaccessible session and turn IDs return `404`.

## Submit and poll

With the `messages` capability, send `POST /v1/agent/sessions/{sessionId}/messages` with a body such as `{"clientMessageId":"case-42-msg-1","text":"Summarize the discussion."}`. A `202` response provides a `turnId`. The turn can finish after the submitting process exits. If the response is lost, retry with the same message ID and exact text. A different message while a turn is active or different text under a used message ID returns `409`. A session kind that is not available returns `503` on the respective create or submit request.

Use `GET /v1/agent/sessions/{sessionId}/turns/{turnId}` to read the committed status. A turn progresses from `QUEUED` to `RUNNING` and finishes as `COMPLETED` or `FAILED`. Completed turns include `assistantText`. Failed turns include a `failureCode`.

The generated Python client exposes these operations under `docudevs.api.agent_sessions`. The Java client generates `AgentSessionsApi` from the same OpenAPI specification.

## Work with files in platform chat

Upload a private attachment to `POST /v1/agent/sessions/{sessionId}/files` as multipart field `file`. An `Idempotency-Key` can recover the same file ID after a retry. To link an existing document in the session's case, send `{"documentGuid":"…"}` to `POST /v1/agent/sessions/{sessionId}/files/links`. List them with `GET .../files` and download the original through `GET .../files/{fileId}/content`. Deleting the link leaves the case document in place.

Submit selected file IDs in `inputFileIds` on a turn. Private attachments support UTF-8 text, Markdown and CSV, plus text-layer PDF and DOCX. A case document can use its existing OCR when the case reports it ready. Private images, scanned PDFs requiring cloud OCR, and raw XLSX are not supported.

## Document workspace workbooks

Where the `document-workspace` kind is available, create a session of that kind and upload value-only XLSX files using the same session file routes. Each workbook may be up to 5 MiB and 50,000 cells. Files that pass validation return `READY`. Use `inputFileIds` to select the files for a turn.

- Use `GET /v1/agent/sessions/{sessionId}/artifacts` to list published revisions. Its `limit` and `after` query parameters work with the returned `nextCursor`.
- Use a revision's `download` URL for the exact bytes covered by its `sha256` and `passedChecks`. Every published revision has an XLSX structure check. A reconciliation check may be added as a separate check. A correction returns a new revision ID.
- A human session owner can mark a revision accepted with `POST /v1/agent/sessions/{sessionId}/artifacts/{revisionId}/accept`. Acceptance applies to that revision and is distinct from its verification report.
- Use `POST /v1/agent/sessions/{sessionId}/turns/{turnId}/cancel` to prevent a queued or running turn from publishing. Retrying a cancelled turn's cancellation returns `CANCELLED`. A turn that already completed or failed returns `409`.

Workspace sessions are retained for 30 days from creation. Expiry hides the session immediately, and files, revisions and conversation content are then removed.

Ask for a saved SIMPLE extraction by name. The worker applies the configuration's prompt and schema to the selected text. The completed turn records the file ID, configuration name and revision hash, and downloadable JSON in `extractionResults`. Batch extraction, PDF comparison and workbook normalization require their own session adapters for these private inputs. In a case-bound conversation, eligible case write proposals appear as `pendingActions` after their turn commits. The creator confirms or rejects them through the existing case action API.

## Replay after disconnect

Connect to `GET /v1/agent/sessions/{sessionId}/events` with `Accept: text/event-stream` and the same user access token. Without a cursor, the stream starts with the oldest retained event. Send the last processed SSE event ID as `Last-Event-ID` to resume after it. To receive only events newer than a snapshot, start with the snapshot's `eventCursor`. Treat cursors as opaque and reuse them only in the session that issued them.

Turn events include the turn ID and committed state. Poll that turn to obtain the assistant text. Ignore unrecognized event names and keep processing. Heartbeats carry no event ID. Connections close periodically so clients can reconnect with current credentials. Each session retains its latest 1,000 events. A stale replay cursor returns `410` with `agent_session.replay_unavailable`. Fetch a new snapshot and reconnect from its cursor. Use the turn history to recover earlier messages and results.
