---
title: Headless agent runs
description: Run the case agent without a person in the chat, from the API, a pipeline or a classify job. Bound each run with a token budget, read its report and get a webhook when it ends.
sidebar_position: 10
---

# Headless agent runs

A headless run is the [case agent](./case-agent.md) working without a chat. You start it from the API, from a pipeline, or automatically when a [classify job](./classification.md) completes. The run works through its playbook in bounded turns, saves a report, and ends. It cannot confirm anything: its writes wait in the [pending-action inbox](./agent-actions.md) for a person.

A run acts as the person who started it. Its agent token has the same limits as a chat: it cannot approve, confirm, reject, start another run or cancel a run.

## Playbooks

| Playbook | Needs | Does |
|----------|-------|------|
| `investigate-flagged` | `params.jobId`: a classify job | Works through the flagged rows of the job, the same way as the [chat playbook](./case-agent.md#playbook-investigate-flagged-rows). |
| `general` | `goal`, up to 2,000 characters | Works towards the goal with the general agent. |

## Start a run

`POST /cases/{caseId}/agent/runs` returns `202` with `{runId, jobGuid}`.

```json
{
  "playbook": "investigate-flagged",
  "params": {"jobId": 55, "maxRows": 10, "flags": ["declined", "sourcesDisagree"]},
  "budget": {"maxTotalTokens": 60000}
}
```

| Field | Meaning |
|-------|---------|
| `params.jobId` | Classify job whose flagged rows are investigated. Required by `investigate-flagged`. |
| `params.maxRows` | Rows the run investigates in total, 1 to 500. Playbook default when absent. |
| `params.flags` | Flags that make a row worth investigating. Playbook default when absent. |
| `budget.maxTotalTokens` | Tokens the run may use in total, language model and authorised classify work together. At least 1. |

A run continues in further turns until it has investigated `maxRows`, uses its budget, is cancelled, or reaches 100 turns. The 45-step limit applies to each turn, not to the run.

`400` means an unknown playbook, a missing goal or job, or invalid params or budget. `404` means the case does not exist. `403` means an agent token tried to start a run. `503` means the run could not be queued; retry.

## Budget

`maxTotalTokens` is checked **before each model call**, not only between turns. A turn refuses a further model call once the remaining budget is spent, so a run overshoots by **at most one model call**. Costly tools run inside the budget without a confirmation, and they stop when the budget is spent.

When the budget is used up, the run **completes** (status `COMPLETED`, not `FAILED`). The reason is in `error`, for example `Token budget exhausted: 61200 of 60000 tokens used over 3 turns; 4 rows investigated.` The API writes a report of the progress without another model call, so the work done is never lost. `remainingTokens` in the status shows what is left.

Without a budget, the run stops at `maxRows`, at the turn limit, or when it has nothing more to do.

## Read a run

`GET /cases/{caseId}/agent/runs/{runId}` returns the state:

| Field | Meaning |
|-------|---------|
| `status` | `QUEUED`, `RUNNING`, `COMPLETED`, `FAILED` or `CANCELLED`. |
| `trigger` | `api`, `classify` or `pipeline`. |
| `originJobId` | The classify job the run was started from, if any. |
| `turnsDone`, `rowsDone`, `tokensUsed`, `remainingTokens` | Progress and budget. `remainingTokens` is absent without a budget. |
| `cursor` | Where the next turn continues. |
| `report` | `summary`, `proposals` (by reference) and `questions`. Absent until the run saves one. |
| `error` | Why the run failed, or the budget reason when it stopped on the budget. |

`originJobId` (optional) names the classify job the run investigates. It must be a classify job of the same case; otherwise the request returns `404`. The run is then listed under that job.

`GET /cases/{caseId}/agent/runs` lists the runs of a case, newest first, with `limit` (1 to 200, default 50), `continuation`, `status` and `originJobId`. Filter by `originJobId` to see the runs of one classify job.

## Cancel a run

`POST /cases/{caseId}/agent/runs/{runId}/cancel` stops the run after its current turn. Cancelling a cancelled run returns the same state. A run that has already finished returns `409`.

## Classify trigger

A classify job can start a run when it completes. Add `investigate` to the job config:

```json
{
  "config": {
    "investigate": {
      "enabled": true,
      "playbook": "investigate-flagged",
      "maxRows": 10,
      "maxTotalTokens": 60000,
      "flags": ["declined", "modelNamedDifferentBestFit", "sourcesDisagree"]
    }
  }
}
```

`playbook` defaults to `investigate-flagged`, `maxRows` to 10 (1 to 500), and `flags` to `declined`, `modelNamedDifferentBestFit` and `sourcesDisagree`. The run's `originJobId` is the job, so the job's results and the run's proposals can be read together.

`maxTotalTokens` (optional, at least 1) bounds the run's tokens, as `budget.maxTotalTokens` does for a direct run. When it is absent the run takes the server's default, `AGENT_RUN_DEFAULT_BUDGET_TOKENS` (300000 tokens), so a triggered run is never unbounded. Its costly tools become pending actions for a person.

## Webhook

When the run's job ends, the organization's [webhook](../integration/webhooks.md) fires once, with the run's `jobGuid`, `status` and `error`. Use it as a signal and then read the run for its report.

## Python SDK

```python
started = await client.start_agent_run(
    case_id,
    "investigate-flagged",
    params={"job_id": 55, "max_rows": 10},
    budget={"max_total_tokens": 60000},
)

run = await client.wait_for_agent_run(
    case_id,
    started["runId"],
    timeout=1800,
    on_progress=lambda status: print(status["status"], status["turnsDone"]),
)
print(run["report"]["summary"])
for question in run["report"]["questions"]:
    print("question:", question)

runs = await client.list_agent_runs(case_id, origin_job_id=55, status="COMPLETED")
```

`wait_for_agent_run` raises `AgentRunFailed` when the run fails or is cancelled, and `TimeoutError` when it runs longer than `timeout`. Snake_case keys in `params` and `budget` are converted for you.

To start the investigation from a classify job:

```python
job = await client.classify(
    case_id,
    keys=keys,
    config={"maxTotalTokens": 400000, "investigate": {"enabled": True, "maxRows": 10}},
    wait=True,
)
```

## CLI

```bash
docudevs agent run start 42 investigate-flagged --params-file params.json --budget-tokens 60000 --wait
docudevs agent run status 42 9001 --wait
docudevs agent run cancel 42 9001
```

`params.json` holds `max_rows`, `flags` and `job_id`.
