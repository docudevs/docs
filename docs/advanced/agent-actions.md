---
title: Agent actions and the pending-action inbox
description: How the case agent's writes wait for a person to confirm them, how to confirm or reject them, and why the agent cannot approve anything itself.
sidebar_position: 9
---

# Agent actions and the pending-action inbox

Anything the [case agent](./case-agent.md) tries to change becomes a **pending action** first. Nothing is written until a person confirms it. The agent cannot confirm its own actions, and it cannot approve memo entries, accept or correct reviews, or change billing, credentials, members, SSO or settings.

## What needs confirmation

| Tool class | Examples | Behaviour |
|------------|----------|-----------|
| Inert writes | `add_feedback`, `propose_instruction` | Run immediately. Feedback does not change classification, and a proposed instruction is inactive until a person approves it. |
| Writes | `upsert_records`, `delete_records`, `activate_tree_version`, `approve_memo_entry`, `submit_review` | Create a pending action. Nothing executes until a person confirms it. |
| Costly tools | `classify_records`, `run_holdout_eval`, `compare_eval_runs` | Run immediately below the platform threshold of 400,000 estimated tokens. Above it they are pending actions. Inside a headless run's budget they run without a pause. |
| Existing tools | `upload_document_to_case`, `save_configuration`, `promote_to_knowledge_base`, `create_case`, `process_document` | Pending only in a case-bound chat. Unbound flows keep their current behaviour. |

A pending action stores what the agent asked for: the tool, its class, a summary (counts and cost where known), the HTTP method and path, and the request body. The body is kept as written and hashed. Confirming replays the stored body, not a new one the model might produce.

## Pending-action states

| Status | Meaning |
|--------|---------|
| `pending` | Waiting for a person. |
| `confirmed` | Replayed and executed. The stored `resultStatus` and `result` show what the write returned. |
| `rejected` | A person declined it, optionally with a reason. |
| `expired` | Not decided within 24 hours. |

Confirming is idempotent: a second confirm of a confirmed action returns the stored result and does not replay the write.

## API

| Method | Path | Notes |
|--------|------|-------|
| `GET` | `/cases/{caseId}/agent/actions` | The case inbox, newest first. Filters: `status`, `limit`, `before` (pass `nextBefore` back). Past-expiry pending actions read as `expired`. |
| `GET` | `/agent/actions/{id}` | One action, with the request it would run. |
| `POST` | `/agent/actions/{id}/confirm` | Replays the stored request with your credentials. Only the person the agent acted for can confirm. |
| `POST` | `/agent/actions/{id}/reject` | Optional body `{"reason": "..."}`. |

Errors:

- `403`: the caller is an agent token, or the action belongs to another user.
- `404`: the action does not exist.
- `409`: the action expired, was rejected, is being confirmed right now, or its stored request failed verification. Verification checks the hash, the method, and that the path stays inside the case the action was created for.

The API replays a confirmed action over loopback with your own token. Your permissions apply to the write exactly as they would if you sent it yourself.

## Agent tokens cannot decide

Chat and runs use a short-lived agent token. It lets the agent read and propose, and nothing more. `RoleAccessPolicy` answers `403` to an agent token on these routes:

- `POST /agent/actions/{id}/confirm` and `/reject`
- `POST /cases/{caseId}/memo/entries/{entryId}/approve`
- `POST /agent/chat` and agent run start and cancel (headless runs, see [Headless agent runs](./agent-runs.md))
- billing, model credentials, connections, members, SSO and settings

Review accept and correct are refused for agents as well. Agent tokens expire after 30 minutes and cannot be revoked before then.

## Python SDK

```python
pending = await client.list_agent_actions(case_id, status="pending", limit=20)

for action in pending["items"]:
    print(action["id"], action["tool"], action["summary"])

confirmed = await client.confirm_agent_action(action_id)
print(confirmed["status"], confirmed.get("resultStatus"))

await client.reject_agent_action(other_id, reason="Use the 7.31 subtree instead")
```

`list_agent_actions` returns `{items, nextBefore?}`; pass `nextBefore` back as `before` for the next page.

## CLI

```bash
docudevs agent actions 42 --status pending
docudevs agent confirm 9001
docudevs agent reject 9002 --reason "Use the 7.31 subtree instead"
```

## In the web app

The case page shows a pending-action inbox. Each card shows the summary, the counts and cost, a preview of the request, and **Confirm** and **Reject** buttons. After a confirm, the agent chat receives a short follow-up message so the agent can continue.

## Headless runs

A headless run never confirms. Its write tools always create pending actions, and the run's status lists the ids of the actions it created. Someone reads the case inbox and decides. Costly tools inside a run's budget are the one exception: the budget is authorised when the run starts.
