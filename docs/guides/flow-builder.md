---
title: Build workflows in Flow Builder
description: Build an integration workflow on a canvas, bind it to saved connections, save it, run it by hand, put it on a schedule and read its run history.
slug: /guides/flow-builder
---

# Build workflows in Flow Builder

Flow Builder is the visual editor for integration workflows in the DocuDevs web app. A workflow connects nodes that fetch files or data, process them with DocuDevs, and deliver the result somewhere else. Save it once, then run it on DocuDevs infrastructure, on a schedule, or on your own [edge runtime](../integration/edge-runtimes.md).

This guide builds one small workflow and then covers saving, running, scheduling and run history. For a larger, complete example, follow [Send Google Drive invoices to SharePoint as Excel](./drive-to-sharepoint.md). To start and monitor managed runs from your own code, use [Managed workflows](../integration/managed-workflows.md).

## Open the builder

In the sidebar, open **Integrations → Flow Builder**. The page has three views, selected at the top: **Visual**, **YAML** and **How to Use**.

- The name of the workflow is the first field in the header, followed by its namespace. A new workflow starts as `my_integration` in `runtime.embedded.managed`.
- The **Nodes** panel on the left lists the available nodes in six groups. Search by name or description, drag a node onto the canvas, or select it to add it to the middle of the canvas. On a narrow screen, use the **Nodes** and **Configuration** buttons above the canvas.
- Selecting a node on the canvas opens its settings panel on the right.
- **Auto Layout** arranges the nodes on the canvas.

## Choose a scope

The **Scope** list in the settings bar decides where the workflow runs.

| Scope | Where it runs |
| --- | --- |
| **All Runtimes** | On every edge runtime of your organization. This is the default. |
| **Specific Runtime** | On one edge runtime, chosen in the **Runtime** field that appears. |
| **Managed (DocuDevs)** | On DocuDevs infrastructure. **Save and Run** and scheduling are available for this scope only. |

Select **Managed (DocuDevs)** for the walkthrough below.

## Build a small workflow

The example downloads PDFs from an Amazon S3 prefix, extracts data from them in one batch and uploads the result workbook to S3.

1. Drag **S3 Downloads**, **DocuDevs Batch Extract** and **S3 Upload** onto the canvas.
2. Connect the nodes in that order: drag from the output handle of one node to the input handle of the next. A connection also fills the receiving node's input, so **DocuDevs Batch Extract** receives the downloaded files and **S3 Upload** receives the result file.
3. Select **S3 Downloads**. Set **Prefix** to the folder that holds the PDFs and **File Pattern (regex)** to `.*\.pdf$`. **Max Files** limits how many files one run downloads.
4. Select **DocuDevs Batch Extract**. Choose **Excel** for **Result Format**, enter the **Extraction Prompt**, and paste a JSON schema into **Output Schema (JSON)**. To reuse a saved DocuDevs configuration instead, pick it in **Configuration**.
5. Select **S3 Upload** and enter an **Object Key**, for example `results/{{ execution.id }}/output.xlsx`.

A node with required fields that are still empty shows a **!** badge.

To delete a node, select it and choose the trash icon in its panel, or press Backspace. Use the **YAML** view to read the generated workflow. Editing the YAML by hand switches the workflow to YAML-only editing: the **Visual** view cannot be selected for it, and the canvas layout is dropped.

## Bind a connection

Most nodes that talk to an outside system have a **Connection** list at the top of their settings panel. It lists the [integration connections](../administration/integration-connections.md) of the matching system, for example your Amazon S3 connections on **S3 Downloads**. An organization admin creates connections under **Integrations → Connections**; see [Set up integration connections](./integration-connections.md).

1. Select the node and open **Connection**.
2. Choose a connection by name. Every field the connection supplies, such as the region, bucket or credentials, shows **Provided by** and the connection name instead of an input.
3. Fill in the fields the connection does not supply, such as **Prefix** or **Object Key**.

Choose **Manual configuration** to go back to entering the fields yourself. Fields without a connection show **From env** with the variable name that an edge runtime reads; select **Override** to type a value instead. On a managed workflow, bind a connection wherever the node offers one.

**DocuDevs Batch Extract** needs no connection on a managed workflow. Its settings show **Automatic authentication**: DocuDevs supplies the API URL and a credential for the run.

The following systems have connections:

| System | Nodes with a **Connection** list |
| --- | --- |
| Amazon S3 | S3 Downloads, S3 Upload |
| Azure Blob Storage | Azure Blob Downloads, Azure Blob Download, Azure Blob List, Azure Blob Upload |
| Google Cloud | GCS Downloads, GCS Upload, Google Drive Folder Downloads, Google Drive Download, Google Drive Export, Google Drive Upload, Google Sheets Read |
| Microsoft 365 | SharePoint Download, SharePoint List, SharePoint Upload, Outlook List Emails, Outlook Get Email, Outlook Send Email |
| PostgreSQL | PostgreSQL Query, PostgreSQL CopyIn |
| MySQL | MySQL Query |
| SQL Server | SQL Server Query |

## Save the workflow

1. Enter a **Description** in the settings bar if you want one.
2. Keep **Active** checked. A workflow that is not active cannot be run or scheduled.
3. Click **Save Config**.

For **All Runtimes** and **Specific Runtime**, saving returns you to **Integrations → Flow Configs**. For **Managed (DocuDevs)**, the builder stays open and shows **Config saved.**

Open **Integrations → Flow Configs** to see every saved workflow. The list shows its **Flow**, **Scope**, **Active** state and **Updated** time. **Edit** reopens a managed workflow in the builder and an edge workflow in a YAML editor, and **Delete** removes the workflow. **New Config** creates a workflow from YAML without the canvas.

## Run a managed workflow by hand

With **Managed (DocuDevs)** selected and **Active** checked, click **Save and Run**. The builder saves the workflow, starts a run with a 1800-second limit, and shows a status line with the run id and its status. The status refreshes every few seconds while the run is active. Select **View logs** on that line to open the run's log in [Run History](#read-the-run-history).

If **Save and Run** reports an error, the message shows what the server answered. When managed runs are not available on your deployment, the message reads **Run failed. Managed workflow execution is unavailable**; see [Availability](../integration/managed-workflows.md#availability).

## Run on a schedule

A managed workflow can start itself on a schedule. Add the **Schedule** node to the canvas; it applies to the whole workflow.

| Setting | What it does |
| --- | --- |
| **Frequency** | Presets such as **Every 5 minutes**, **Every hour**, **Every day at 08:00**, **Weekdays at 08:00** and **Mondays at 08:00**. A preset fills in the cron expression. **Custom expression** is shown for any other value. |
| **Cron Expression** | Five fields (minute, hour, day of month, month, day of week) at minute precision. The default is `*/15 * * * *`. |
| **Time Zone** | A time zone such as `Europe/Zurich`. A managed schedule uses UTC when you leave it empty. |
| **Schedule timeout (seconds)** | Appears under the settings bar when the managed workflow has a Schedule node. A whole number from 1 to 1800; the default is 1800. |

A managed workflow supports one enabled Schedule node. **Enable Seconds** is not supported for managed schedules: the cron expression must have five fields.

Save the workflow with **Active** checked. The builder shows **Next scheduled run** in UTC. Clear **Active** and save again to pause the schedule; the builder then shows **Schedule paused after saving.** When scheduling is not available on your deployment, the builder shows **Managed scheduling is disabled on this deployment.** and the workflow does not start by itself; you can still run it with **Save and Run**.

A scheduled run does not start while an earlier run of the same workflow is still queued, running or cancelling.

## Read the run history

Open **Integrations → Flow Configs** and click **Run History**, or click **Run history** next to a managed workflow to filter the list to it. **Run History** lists managed runs of your organization, newest first.

| Column | Meaning |
| --- | --- |
| **Workflow** | The workflow name. A failed run shows its error message underneath. |
| **Created (UTC)** | When the run was created. |
| **Trigger** | **Manual** or **Scheduled**. |
| **Status** | **Queued**, **Running**, **Completed**, **Failed**, **Cancelling**, **Cancelled** or **Timed out**. |
| **Duration** | The run time in seconds, or a dash while it has none. |
| **Logs** | **View logs** opens the run's log entries below the filter, with timestamp, level, task and message. |

Use the **Workflow** list to filter by workflow and **Refresh** to reload. The page refreshes itself every ten seconds while it is visible. **Completed**, **Failed**, **Cancelled** and **Timed out** are final states.

## Nodes

The **Nodes** panel groups the nodes as follows. The tables give each node's name as shown in the panel.

### Triggers

| Node | Purpose |
| --- | --- |
| Schedule | Run on a cron schedule |

### Sources

| Node | Purpose |
| --- | --- |
| Azure Blob Downloads | Download files from Azure Blob Storage |
| Azure Blob Download | Download a single file from Azure Blob Storage |
| Azure Blob List | List files in Azure Blob Storage |
| S3 Downloads | Download files from AWS S3 |
| GCS Downloads | Download files from Google Cloud Storage |
| Google Drive Folder Downloads | Find and download matching files from a Google Drive folder |
| Google Drive Download | Download a file from Google Drive |
| Google Drive Export | Export a native Google document, spreadsheet, or presentation |
| Google Sheets Read | Read data from Google Sheets |
| SharePoint Download | Download a file from a SharePoint document library |
| SharePoint List | List items in a SharePoint document library |
| Outlook List Emails | List Outlook emails from a mailbox or folder |
| Outlook Get Email | Fetch a specific Outlook email message |
| HTTP Request | Make an HTTP request |
| Local File Read | Read files from the edge runtime filesystem (`/in`); for workflows on an edge runtime |
| SFTP Downloads | Download files from an SFTP server |

### Processing

| Node | Purpose |
| --- | --- |
| DocuDevs Extract | Extract data from a single document with AI |
| DocuDevs Batch Extract | Batch extract data from multiple documents with AI |

### Transform / ETL

| Node | Purpose |
| --- | --- |
| DuckDB Query | Run SQL queries on CSV or Excel files |
| PostgreSQL Query | Execute a SQL query against PostgreSQL |
| MySQL Query | Execute a SQL query against MySQL |
| SQL Server Query | Execute a SQL query against Microsoft SQL Server |
| Python Script | Run a Python script for data transformation |
| Convert to CSV | Convert query results to CSV |
| Parse CSV | Read tabular CSV data for conversion to Excel or further processing |
| Convert to Excel | Write parsed CSV or query results to an Excel workbook |
| Extract ZIP | Extract files from a ZIP archive for batch processing |

### Destinations

| Node | Purpose |
| --- | --- |
| Azure Blob Upload | Upload a file to Azure Blob Storage |
| S3 Upload | Upload a file to AWS S3 |
| GCS Upload | Upload a file to Google Cloud Storage |
| Google Drive Archive | Move successfully extracted files after the SharePoint workbook uploads |
| Google Drive Upload | Upload a file to Google Drive |
| SharePoint Upload | Upload a file to a SharePoint document library |
| Outlook Send Email | Send an email through Outlook |
| PostgreSQL CopyIn | Append a CSV file to an existing PostgreSQL table |
| SFTP Upload | Upload a file to an SFTP server |

### Control Flow

| Node | Purpose |
| --- | --- |
| If / Condition | Conditional branch based on an expression |
| Allow Failure | Continue the workflow even if this step fails |
