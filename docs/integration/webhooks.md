---
title: Webhooks
description: Get an HTTP callback when a DocuDevs processing job reaches a terminal status.
---

# Webhooks

DocuDevs can call a URL you configure whenever a processing job finishes, instead of you polling the job status endpoint. Configure it once at **Settings > Webhooks** (`/app/settings/webhooks`) — there is a single webhook URL per organization.

## When it fires

DocuDevs sends a `POST` request to your URL only when a job reaches a **terminal** status: `COMPLETED`, `ERROR`, or `FAILED`. Intermediate status transitions (queued, processing, and so on) do not trigger a call.

### Records import, tree activation and classify jobs

The same webhook fires for `RECORD_IMPORT`, `TREE_ACTIVATION`, `CLASSIFY` and `AGENT_RUN` jobs, once per job, when the job finishes. A headless agent run fires it when it completes, fails or is cancelled ([Headless agent runs](../advanced/agent-runs.md#webhook)). The payload has the same shape; `qualityScore` and `qualityCategory` are always `null` for these job types.

| Job type | `status` | `error` |
| --- | --- | --- |
| `RECORD_IMPORT` (imports, syncs and rebuilds) | `COMPLETED` or `ERROR` | The failure reason when `ERROR`, for example when the import could not be queued or stopped reporting progress |
| `TREE_ACTIVATION` | `COMPLETED` or `ERROR` | The failure reason when `ERROR` |
| `CLASSIFY` | `COMPLETED` or `ERROR` | The failure reason when `ERROR`; a cancelled classify job reports `ERROR` with a cancellation reason |

A job that is already finished never sends a second notification, and a failed delivery never changes the job's outcome.

Delivery is best-effort: a failed delivery is logged on the DocuDevs side and is not retried, and it never affects the underlying job. Treat the webhook as a convenience notification, not a guaranteed delivery channel — poll the job or case status if you need certainty.

## Payload

```json
{
  "jobGuid": "3fa85f64-5717-4562-b3fc-2c963f66afa6",
  "status": "COMPLETED",
  "error": null,
  "qualityScore": 0.94,
  "qualityCategory": "HIGH"
}
```

| Field | Type | Notes |
| --- | --- | --- |
| `jobGuid` | string | The job's identifier |
| `status` | string | `COMPLETED`, `ERROR`, or `FAILED` |
| `error` | string \| null | Present when `status` is `ERROR` or `FAILED` |
| `qualityScore` | number \| null | Present when a quality score was computed for the job |
| `qualityCategory` | string \| null | Present alongside `qualityScore` |

## Configuring the URL

| Method | Path | Notes |
| --- | --- | --- |
| GET | `/settings/webhook` | Returns `{url}`, `null` if unset |
| PUT | `/settings/webhook` | Sets or replaces `{url}` |

```bash
curl -X PUT https://api.docudevs.ai/settings/webhook \
  -H "Authorization: $API_KEY" -H "Content-Type: application/json" \
  -d '{"url":"https://example.com/hooks/docudevs"}'

curl https://api.docudevs.ai/settings/webhook -H "Authorization: $API_KEY"
```

## Minimal receiver

Your endpoint just needs to accept the `POST` and return a `2xx` response. For example, in Python with Flask:

```python
from flask import Flask, request

app = Flask(__name__)

@app.post("/hooks/docudevs")
def docudevs_webhook():
    payload = request.get_json()
    job_guid = payload["jobGuid"]
    status = payload["status"]

    if status == "COMPLETED":
        # fetch the result, e.g. via the job result endpoint or SDK
        pass
    else:
        # status is ERROR or FAILED; payload.get("error") has details
        pass

    return "", 204
```

Because delivery isn't retried, treat the webhook as a signal to go fetch the job's current state rather than as the sole source of truth for the result itself.
