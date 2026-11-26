---
title: Edge Runtimes
description: Run Flow Builder workflows inside your own network with an edge runtime that connects to DocuDevs over HTTPS.
sidebar_position: 4
---
<!-- Generated from docs/integration/edge-runtimes.md. Do not edit by hand. -->

# Edge Runtimes

An edge runtime is a container you run in your own network. It connects to DocuDevs over HTTPS, receives the workflows saved for it, runs them next to your internal systems, and reports its status back. Use one when a workflow has to reach files or databases that must not be exposed to the public internet. For workflows that run on DocuDevs infrastructure instead, see [Managed workflows](./managed-workflows.md).

An edge runtime belongs to one organization, and the **Runtimes** page lists only your organization's runtimes.

## Run a workflow on an edge runtime

1. Open **Integrations → Flow Builder** and build the workflow as described in [Build workflows in Flow Builder](../guides/flow-builder.md). The **Local File Read** node reads files from the `/in` directory of the runtime.
2. Set **Scope** to **All Runtimes** to run the workflow on every edge runtime of your organization. Keep **All Runtimes** until your runtime appears under **Integrations → Runtimes**; then, to run the workflow on that one only, open the saved workflow's edit page (**Integrations → Flow Configs**), set **Scope** to **RUNTIME (specific runtime)** and choose the runtime in **Runtime ID**. (The builder labels the same choices **All Runtimes** and **Specific Runtime**.)
3. Open the **How to Use** view in the builder before you save. For a workflow that is not managed, it shows two things to copy: a `.env` file and a `docker run` command. The `.env` file holds `API_TOKEN`, where you enter an organization API key, and one line for every environment variable the workflow's nodes read, such as storage credentials. Fields that a saved connection supplies are not listed. Create the file, fill in the values, and run the command on a machine in your network.
4. Click **Save Config**. The builder returns to **Integrations → Flow Configs**, where the workflow is listed with its scope. Editing a saved edge workflow opens its YAML source instead of the builder.
5. Mount the folder that holds your input files at `/in` and the folder that should receive results at `/out`, as the copied command shows.

The runtime connects to DocuDevs, pulls the workflows saved for it and runs them with the environment variables you provided. Saving or changing a workflow with a runtime scope refreshes the runtimes it applies to.

## See your runtimes

Open **Integrations → Runtimes**. The table lists one row per runtime:

| Column | Meaning |
| --- | --- |
| **Runtime** | The runtime name, with its id underneath. |
| **Status** | **Online**, **Degraded** or **Offline**. A runtime that has not been seen for some time shows **Offline**. |
| **Last Seen** | When the runtime last connected or reported an event, or **Never**. |
| **Connections** | How many times the runtime has connected to DocuDevs. |
| **Capabilities** | The capabilities the runtime announced, or **none**. |

The page shows **No runtimes connected yet** until the first runtime has connected. Runs on DocuDevs infrastructure do not appear here; they are listed in [Run History](../guides/flow-builder.md#read-the-run-history).

To remove a runtime from the list, click **Delete** in its row and confirm.

## Change or remove a workflow

**Integrations → Flow Configs** lists the workflows saved for runtimes together with their **Scope** and **Active** state. **Edit** opens the workflow's YAML; change it and click **Save Changes**. **Delete** removes the workflow. A workflow with **Active** cleared is not sent to runtimes.
