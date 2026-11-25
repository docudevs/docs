---
title: Headless agent runs
description: Run the case agent without a person in the chat, from the API, a pipeline or a classify job. Bound each run with a token budget, read its report and get a webhook when it ends.
sidebar_position: 10
---

# Headless agent runs

A headless run is the [case agent](./case-agent.md) working without a chat. You start it from the API, from a pipeline, or automatically when a [classify job](./classification.md) completes. The run works through its playbook in bounded turns, saves a report, and ends. It cannot confirm anything: its writes wait in the [pending-action inbox](./agent-actions.md) for a person.

A run acts as whoever started it: a signed-in user, or your organization when you start it with an API key. The agent in a run has the same limits as in a chat: it cannot approve, confirm, reject, start another run or cancel a run.

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

<Example name="agent-runs/start-run" />

The cURL and Python variants start the run, wait for it to end and print its report; the CLI starts it and, with `--wait`, waits too. The wait ends when the run is `COMPLETED`, and fails when it is `FAILED` or `CANCELLED`. The Python SDK takes `max_rows` and `job_id` in `params` and `max_total_tokens` in `budget`; the request itself uses `maxRows`, `jobId` and `maxTotalTokens`. The CLI reads `max_rows`, `flags` and `job_id` from the params file.

A run continues in further turns until it has investigated `maxRows`, uses its budget, is cancelled, or reaches 100 turns. The 45-step limit applies to each turn, not to the run.

`400` means an unknown playbook, a missing goal or job, or invalid params or budget. `404` means the case does not exist. `503` means the run could not be queued; retry.

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
| `pendingActionIds` | Every [pending action](./agent-actions.md) the run proposed, in any status. |
| `report` | `summary`, `proposals` (by reference) and `questions`. Absent until the run saves one. |
| `error` | Why the run failed, or the budget reason when it stopped on the budget. |

`originJobId` (optional) names the classify job the run investigates. It must be a classify job of the same case; otherwise the request returns `404`. The run is then listed under that job.

`GET /cases/{caseId}/agent/runs` lists the runs of a case, newest first, with `limit` (1 to 200, default 50), `continuation`, `status` and `originJobId`. Filter by `originJobId` to see the runs of one classify job.

<Example name="agent-runs/list-runs" />

## Cancel a run

`POST /cases/{caseId}/agent/runs/{runId}/cancel` marks the run `CANCELLED` immediately; a turn already in progress may still finish. Cancelling a cancelled run returns the same state. A run that has already finished returns `409`.

<Example name="agent-runs/cancel-run" />

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

`maxTotalTokens` (optional, at least 1) bounds the run's tokens, as `budget.maxTotalTokens` does for a direct run. When it is absent the run takes a platform default budget, so a triggered run is never unbounded. Its costly tools become pending actions for a person.

The run starts when the classify job completes. A job that fails or is cancelled starts no run.

<Example name="agent-runs/investigate-after-classify" />

The example sends the `investigate` settings in the job's `config` and waits for the job. The job's own `maxTotalTokens` bounds the classify work only; the run takes its own budget from `investigate.maxTotalTokens`. Read the run afterwards by listing the runs of the job, as above.

## Webhook

When the run's job ends, the organization's [webhook](../integration/webhooks.md) fires once, with the run's `jobGuid`, `status` and `error`. Use it as a signal and then read the run for its report.
