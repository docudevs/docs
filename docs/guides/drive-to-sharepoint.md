---
title: Send Google Drive invoices to SharePoint as Excel
description: Build a managed workflow that downloads PDFs from Google Drive, extracts invoice numbers in one batch, and uploads an Excel workbook to SharePoint.
slug: /guides/drive-to-sharepoint
---

Use Flow Builder to download invoice PDFs from a Google Drive folder, extract their invoice numbers in one DocuDevs batch, and upload the workbook to a SharePoint document library.

![Flow Builder canvas connecting Google Drive Folder Downloads to DocuDevs Batch Extract and SharePoint Upload](./assets/drive-to-sharepoint/visual-workflow.png)

## Set up the shared connections

Start with a Drive folder containing two invoice PDFs with different invoice numbers and a SharePoint document library where you can check the result. Use a DocuDevs deployment with managed runs enabled.

An organization admin creates the following connections under **Integrations → Connections → New connection**. See the provider setup instructions for [Google Workspace](./integration-connections.md#google-workspace) and [SharePoint](./integration-connections.md#sharepoint).

1. **Google Workspace**. Enter the service-account JSON and share the source folder with the service account. Save the connection as `drive-invoices`. Copy the folder ID from the part of its Drive URL after `/folders/`.
2. **SharePoint**. Enter the Entra tenant ID, client ID, and client secret for an app with write access to the site. Save the site ID and library drive ID on the connection. Name it `sharepoint-invoices`. If the workbook goes in an existing subfolder, copy its Graph folder item ID.

## Build the workflow

Open **Integrations → Flow Builder**, name the flow `drive_to_sharepoint`, and set **Scope** to **Managed (DocuDevs)**.

### Download the PDFs

Drag **Google Drive Folder Downloads** onto the canvas. Select `drive-invoices` as the **Connection** and enter the **Folder ID**. Keep **File MIME Type** set to `application/pdf` and **Concurrent Downloads** set to `4`.

The node lists the folder on every run and downloads matching files directly in that folder. New PDFs are picked up on the next run. An empty source folder skips extraction and upload.

![Google Drive Folder Downloads configured with a shared connection, PDF filter, and four concurrent downloads](./assets/drive-to-sharepoint/drive-folder-downloads.png)

### Extract the invoice numbers

Add **DocuDevs Batch Extract** and connect the output of **Google Drive Folder Downloads** to its input. Leave **Input Files** blank so the builder passes the downloaded files to the batch.

Choose **Excel** for **Result Format** and **Simple** for **Extraction Mode**. Enter `Extract the invoice number from each document.` as the **Extraction Prompt**, and paste the following JSON into **Output Schema**.

```json
{
  "type": "object",
  "properties": {
    "invoice_number": { "type": "string" }
  },
  "required": ["invoice_number"]
}
```

Batch Extract shows **Automatic authentication** in managed scope. DocuDevs supplies the API URL and a short-lived credential for the run.

![Batch Extract settings showing automatic authentication, Excel output, and Simple extraction](./assets/drive-to-sharepoint/batch-extract.png)

### Upload the workbook

Add **SharePoint Upload** and connect the output of **DocuDevs Batch Extract** to its input. Select `sharepoint-invoices` as the **Connection** and set **Destination Filename** to `drive-batch.xlsx`. Leave **Folder Item ID** blank to upload to the library root, or enter the subfolder item ID.

![SharePoint Upload settings with the shared connection, destination filename, and folder item ID](./assets/drive-to-sharepoint/sharepoint-upload.png)

## Run the flow and check the workbook

1. Click **Save and Run** and wait for the execution to show **COMPLETED**.
2. Open `drive-batch.xlsx` in the destination SharePoint library or folder. Check that the `invoice_number` column contains the two expected invoice numbers.
3. Add a third invoice PDF to the Drive folder and run the flow again. Check that the new workbook includes its invoice number.

![The completed managed execution in Flow Builder](./assets/drive-to-sharepoint/managed-run.png)

If a run fails or produces no workbook, check the execution status, the source folder ID and service-account access, and the SharePoint app's write permission on the destination site.
