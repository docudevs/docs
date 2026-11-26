---
title: Managed workflows API
description: Start, monitor, read the logs of and cancel runs of a saved managed workflow with the DocuDevs API.
slug: /integration/managed-workflows
---

# Managed workflows API

A managed workflow is a workflow saved in [Flow Builder](../guides/flow-builder.md) with the scope **Managed (DocuDevs)**. DocuDevs runs it on its own infrastructure. The managed-execution routes let your own code start a run, follow its status, read its log, cancel it and list past runs. The same runs appear under **Integrations → Flow Configs → Run History** in the web app.

## Availability

Starting a run needs the component that executes workflow jobs, which a deployment provides. When your deployment does not have it, `POST /managed-execution` answers `503` with the message `Managed workflow execution is unavailable`. In the web app, **Save and Run** shows the same message. Contact support to have managed runs enabled.

When the component is present but a run cannot be started, the call answers `502` with the message `Could not start the managed runner`. The run is recorded with the status `FAILED` and the error message `Could not start the managed runner`, so it shows up in the list.

Automatic runs from a **Schedule** node have their own condition. On a deployment where scheduling is not available, the builder shows **Managed scheduling is disabled on this deployment.** and the workflow only runs when you start it. Contact support to have scheduling enabled.

The routes that read, list, log and cancel runs answer for the runs your organization already has.

## Before you start

- Save the workflow in Flow Builder with **Managed (DocuDevs)** as its **Scope** and **Active** checked.
- Read its **flow config id**. Open **Integrations → Flow Configs** and click **Edit** next to the workflow. The address of the builder page ends in `/app/integrations/configs/builder?edit=12`; the number after `edit=` is the flow config id, here `12`. The workflow name shown in the **Flow** column is a different value: it is returned as `flowId` in run responses, and you do not send it to start a run.
- Authenticate with an organization API key in the `Authorization` header. All five routes accept an API key. A signed-in web app user calls the same routes.

The Python client and the command-line tool have no commands for these routes, so every example here is cURL.

## Start a run

`POST /managed-execution` queues a run of a saved workflow and returns its run id.

| Field | Type | Meaning |
| --- | --- | --- |
| `flowConfigId` | integer, required | The flow config id of an active workflow of your organization; use one saved with the scope **Managed (DocuDevs)**. |
| `resourceClass` | string | The runner size, `small` or `medium`. The default is `small`. |
| `timeoutSeconds` | integer | The time limit of the run in seconds, from 1 to 1800. The default is 300. |
| `inputs` | object | Values passed to the workflow as run inputs. The default is an empty object. |

<Example name="managed-workflows/start-run" />

The response carries `executionId`, `status` and `resourceClass`. A new run has the status `PENDING`. Keep the `executionId` for the other routes.

These requests are rejected:

| Status | Message or cause |
| --- | --- |
| `404` | No workflow with that flow config id exists in your organization. The response has no body. |
| `400` | `Flow config is disabled`: the workflow is not active. |
| `400` | `Unsupported managed runner size`: `resourceClass` is not `small` or `medium`. |
| `400` | `Managed run timeout must be between 1 and 1800 seconds`. |
| `400` | `Concurrent execution limit reached (5)`: your organization already has the maximum number of queued, running or cancelling runs. Five is the limit unless your deployment sets another one, and the message names the limit that applies. |
| `502` | `Could not start the managed runner`. See [Availability](#availability). |
| `503` | `Managed workflow execution is unavailable`. See [Availability](#availability). |

## Follow a run

`GET /managed-execution/{executionId}` returns one run. It answers `404`, with no body, when the run does not exist in your organization.

<Example name="managed-workflows/get-run" />

The example polls every five seconds and stops after ten minutes or when the run reaches a final status.

| Field | Meaning |
| --- | --- |
| `executionId` | The run id. |
| `flowId` | The workflow name. |
| `status` | The run status, see below. |
| `resourceClass` | The runner size. |
| `triggerType` | `manual` for a run started through the API or **Save and Run**, `schedule` for a scheduled run. |
| `startedAt`, `completedAt` | When the run started and finished, when known. |
| `durationSeconds` | The run time in seconds, once the run has started and finished. |
| `taskRunCount` | The number of tasks the run has executed. |
| `finalState` | The final state the runner reported. |
| `errorMessage` | Why the run failed, when it did. |
| `createdAt` | When the run was queued. |

Fields without a value are omitted from the response.

| Status | Meaning | Final |
| --- | --- | --- |
| `PENDING` | Queued; shown as **Queued** in the web app. | No |
| `RUNNING` | The runner has started the workflow. | No |
| `CANCELLING` | A cancellation was requested. | No |
| `COMPLETED` | The workflow ended successfully. | Yes |
| `FAILED` | The workflow ended with an error, or the runner could not be started. | Yes |
| `TIMEOUT` | The runner reported nothing by the deadline, which is five minutes after `timeoutSeconds`. | Yes |

A run that is past its deadline changes to `TIMEOUT` the next time you read it or list runs.

## Read the log

`GET /managed-execution/{executionId}/logs` returns the run's log entries in the order they were recorded. The list is empty while the run has logged nothing. The route answers `404`, with no body, for an unknown run id.

<Example name="managed-workflows/get-logs" />

Every entry can carry `timestamp`, `level`, `taskId`, `taskRunId`, `message`, `flowId` and `namespace`; entries omit the ones that have no value.

## List runs

`GET /managed-execution` returns your organization's runs, newest first, including scheduled runs. There is no paging. The optional `status` query parameter keeps only the runs with exactly that status, for example `FAILED`. To filter by workflow, compare `flowId` in your own code.

<Example name="managed-workflows/list-runs" />

## Cancel a run

`POST /managed-execution/{executionId}/cancel` asks for a `PENDING` or `RUNNING` run to stop. The call answers `200` with no body and the run's status becomes `CANCELLING`. The run reaches a final status when the runner reports its result, or `TIMEOUT` when it does not report by its deadline.

Cancelling a run that is already `CANCELLING` or final answers `400` with no body. An unknown run id answers `404`.

<Example name="managed-workflows/cancel-run" />
