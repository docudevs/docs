---
title: Agent actions and the pending-action inbox
description: How the case agent's writes wait for a person to confirm them, how to confirm or reject them, and why the agent cannot approve anything itself.
sidebar_position: 9
---
<!-- Generated from docs/advanced/agent-actions.md. Do not edit by hand. -->

# Agent actions and the pending-action inbox

Anything the [case agent](./case-agent.md) tries to change becomes a **pending action** first. Nothing is written until a person confirms it. The agent cannot confirm its own actions, and it cannot approve memo entries, accept or correct reviews, or change billing, credentials, members, SSO or settings.

## What needs confirmation

| Tool class | Examples | Behaviour |
|------------|----------|-----------|
| Inert writes | `add_feedback`, `propose_instruction` | Run immediately. Feedback does not change classification, and a proposed instruction is inactive until a person approves it. |
| Writes | `upsert_records`, `delete_records`, `activate_tree_version`, `approve_memo_entry`, `submit_review` | Create a pending action. Nothing executes until a person confirms it. |
| Costly tools | `classify_records`, `run_holdout_eval`, `compare_eval_runs` | Run immediately below a platform threshold of estimated tokens. Above it they are pending actions. Inside a headless run's budget they run without a pause. |
| Existing tools | `upload_document_to_case`, `save_configuration`, `promote_to_knowledge_base`, `create_case`, `process_document` | Pending only in a case-bound chat. Unbound flows keep their current behaviour. |

A pending action stores what the agent asked for: the tool, its class, a one-line summary, the HTTP method and path, and the request body. The row count and estimated tokens that the web app shows next to an action come from that stored body, where it holds them. The body is kept as written and hashed. Confirming replays the stored body, not a new one the model might produce.

## Pending-action states

| Status | Meaning |
|--------|---------|
| `pending` | Waiting for a person. |
| `confirmed` | Replayed and executed. The stored `resultStatus` and `result` show what the write returned. |
| `failed` | Replayed, but the write answered an error or could not be reached. `resultStatus` and `result` show why. A second confirm returns the stored result and does not retry. |
| `rejected` | A person declined it, optionally with a reason. |
| `expired` | Not decided within 24 hours. |

An action is `confirming` for the moment the write is replayed; read it again to see the outcome. The inbox `status` filter accepts `pending`, `confirmed`, `failed`, `rejected` and `expired`.

Confirming is idempotent: a second confirm of a confirmed action returns the stored result and does not replay the write.

## API

| Method | Path | Notes |
|--------|------|-------|
| `GET` | `/cases/{caseId}/agent/actions` | The case inbox, newest first. Filters: `status`, `limit` (1 to 50, default 20), `before` (pass `nextBefore` back). Past-expiry pending actions read as `expired`. |
| `GET` | `/agent/actions/{id}` | One action, with the request it would run. |
| `POST` | `/agent/actions/{id}/confirm` | Replays the stored request with your credentials. |
| `POST` | `/agent/actions/{id}/reject` | Optional body `{"reason": "..."}`. |

### Read the inbox

**cURL**

```bash
CASE_ID=42  # id returned when the case was created

curl -s -S -f "https://api.docudevs.ai/cases/$CASE_ID/agent/actions?status=pending&limit=20" \
  -H "Authorization: $API_KEY" | jq -r '.items[]? | "\(.id) \(.tool) \(.summary)"'
```

**Python**

```python
import asyncio
import os

from docudevs.docudevs_client import DocuDevsClient


async def main():
    client = DocuDevsClient(token=os.environ["API_KEY"])

    case_id = 42  # id returned by create_case

    pending = await client.list_agent_actions(case_id, status="pending", limit=20)
    for action in pending.get("items") or []:
        print(action["id"], action["tool"], action["summary"])


asyncio.run(main())
```

**Java**

```java
import ai.docudevs.client.DocuDevsClient;
import com.fasterxml.jackson.databind.JsonNode;

DocuDevsClient client = DocuDevsClient.builder()
        .apiKey(System.getenv("API_KEY"))
        .baseUrl("https://api.docudevs.ai")
        .build();

long caseId = 42L; // id returned when the case was created

JsonNode pending = client.listAgentActions(caseId, "pending", 20, null);
for (JsonNode action : pending.path("items")) {
    System.out.println(action.path("id").asText() + " " + action.path("tool").asText()
            + " " + action.path("summary").asText());
}
```

**CLI**

```bash
docudevs agent actions 42 --status pending --limit 20
```

The inbox answers `{items, nextBefore?}`; pass `nextBefore` back as `before` for the next page. Each item carries `id`, `tool`, `toolClass`, `summary`, `status`, `expiresAt` and, once decided, `resultStatus` and `result`. The Python SDK has no method for reading a single action by id; read it from the inbox, or call `GET /agent/actions/{id}` directly.

### Confirm an action

**cURL**

```bash
ACTION_ID=9001  # id of a pending action from the case inbox

RESULT=$(curl -s -S -f -X POST "https://api.docudevs.ai/agent/actions/$ACTION_ID/confirm" \
  -H "Authorization: $API_KEY")
echo "$RESULT" | jq '{status, resultStatus}'

# A write that answered an error ends as "failed", not "confirmed"
echo "$RESULT" | jq -e '.status == "confirmed"' > /dev/null
```

**Python**

```python
action_id = 9001  # id of a pending action from the case inbox

confirmed = await client.confirm_agent_action(action_id)
print(confirmed["status"], confirmed.get("resultStatus"))

# A write that answered an error ends as "failed", not "confirmed"
if confirmed["status"] != "confirmed":
    raise SystemExit(f"Action {action_id} {confirmed['status']}: {confirmed.get('result')}")
```

**Java**

```java
import com.fasterxml.jackson.databind.JsonNode;

long actionId = 9001L; // id of a pending action from the case inbox

JsonNode confirmed = client.confirmAgentAction(actionId);
System.out.println(confirmed.path("status").asText() + " " + confirmed.path("resultStatus").asText(null));

// A write that answered an error ends as "failed", not "confirmed"
if (!"confirmed".equals(confirmed.path("status").asText())) {
    throw new IllegalStateException("Action " + actionId + " " + confirmed.path("status").asText()
            + ": " + confirmed.path("result"));
}
```

**CLI**

```bash
# This command only prints the action and exits 0 even when it ends "failed"; read the status in the output
docudevs agent confirm 9001
```

The confirm call answers `200` with the action. Check its `status`: `confirmed` means the replayed write succeeded, and `failed` means it answered an error. The cURL, Python and Java variants fail when the action ends `failed`; the CLI only prints the action. The API replays a confirmed action with the credentials of your own request, so your permissions apply to the write exactly as they would if you sent it yourself.

### Reject an action

**cURL**

```bash
ACTION_ID=9002  # id of a pending action from the case inbox

curl -s -S -f -X POST "https://api.docudevs.ai/agent/actions/$ACTION_ID/reject" \
  -H "Authorization: $API_KEY" \
  -H "Content-Type: application/json" \
  -d '{"reason": "Use the 7.31 subtree instead"}' | jq '{id, status}'
```

**Python**

```python
action_id = 9002  # id of a pending action from the case inbox

rejected = await client.reject_agent_action(action_id, reason="Use the 7.31 subtree instead")
print(rejected["id"], rejected["status"])
```

**Java**

```java
import com.fasterxml.jackson.databind.JsonNode;

long actionId = 9002L; // id of a pending action from the case inbox

JsonNode rejected = client.rejectAgentAction(actionId, "Use the 7.31 subtree instead");
System.out.println(rejected.path("id").asText() + " " + rejected.path("status").asText());
```

**CLI**

```bash
docudevs agent reject 9002 --reason "Use the 7.31 subtree instead"
```

Rejecting is idempotent, but a confirmed, failed, expired or currently confirming action answers `409`.

### Who can confirm or reject

An action belongs to the person the agent acted for. A chat or run started with an organization API key has no person, so any API key of the organization can confirm or reject its actions. An action from the chat or run of a signed-in user can be confirmed or rejected only by that user; every other caller, an API key included, gets `403`. The reverse holds too: a signed-in user cannot decide an action made by a chat or run that was started with an API key.

Errors:

- `403`: the action belongs to another user.
- `404`: the action does not exist.
- `409`: the action expired, was rejected, is being confirmed right now, or its stored request failed verification. Verification checks the hash, the method, and that the path stays inside the case the action was created for.

## The agent cannot decide

The agent can read and propose, and nothing more. It is refused with `403` on:

- confirming or rejecting a pending action
- approving a memo entry
- starting a chat, and starting or cancelling a headless run (see [Headless agent runs](./agent-runs.md))
- billing, model credentials, connections, members, SSO and settings

Review accept and correct are refused for the agent as well.

## In the web app

The case page shows a pending-action inbox. Each card shows the summary, the row count and estimated tokens, a preview of the request, and **Confirm** and **Reject** buttons. After a confirm, the agent chat receives a short follow-up message so the agent can continue.

## Headless runs

A headless run never confirms. Its write tools always create pending actions, and the run's status lists the ids of the actions it created. Someone reads the case inbox and decides. Costly tools inside a run's budget are the one exception: the budget is authorised when the run starts.
