---
title: Submission Portals
description: Give business users a lightweight upload page for a specific processing configuration, without full app access.
---
<!-- Generated from docs/integration/submission-portals.md. Do not edit by hand. -->

# Submission Portals

A submission portal is a lightweight, single-purpose upload page backed by one of your processing configurations. Instead of sending occasional submitters into the full DocuDevs workbench, you give them a link like `/submit/<slug>` where they upload a document (or a batch of documents) and get a receipt code back — no configuration UI, no jobs list, no case browsing.

## Creating a portal

Admins and members create and manage portals from the **Submission Portals** entry in the app sidebar (`/app/submission-portals`). A portal wraps an existing processing configuration with display text and behavior:

- **Title, description, submit button label, success message** — the copy shown on the submit page.
- **Input mode** — `SINGLE_FILE` or `MULTI_FILE`.
- **Result mode** — `DOWNLOAD` (the submitter gets the result file directly) or `THANK_YOU` (a confirmation message only).
- **Download format** — `AUTO`, `EXCEL`, `CSV`, or `JSON`, used when result mode is `DOWNLOAD`.
- **Default for configuration** — marks this portal as the canonical entry point for its processing configuration, resolvable by configuration name instead of slug.

Each portal has a unique `slug` that forms its public-facing URL.

## Who can submit

`/submit/<slug>` is not an anonymous public form. It sits outside `/app/`, so it doesn't require the full app shell, but the page still forwards the visitor's signed-in session to the API — submitting through the page requires a signed-in member of the organization that owns the portal. An organization API key can call every portal route as well, including submitting documents and reading receipts.

DocuDevs has a dedicated role for this: `SUBMISSION_PORTAL_USER`. Accounts with this role land directly in the submission experience instead of the main document workspace, and can only read `/submission-portals*` (to see available portals) and submit to `/submission-portals/{id}/submissions` — they cannot create, edit, or delete portals, and cannot access the main workbench. `ADMIN` and `MEMBER` accounts can also submit, in addition to managing portals.

## Receipt codes

Every submission returns a `receiptCode` — an opaque string the submitter can use later to check status and retrieve the result without needing access to the jobs list:

- `GET /submission-records/{receiptCode}` — status and metadata.
- `GET /submission-records/{receiptCode}/download` — the raw result, once the job has completed.

The receipt page at `/submit/receipt/<receiptCode>` gives submitters a bookmarkable link for this.

## REST reference

| Method | Path | Notes |
| --- | --- | --- |
| GET | `/submission-portals` | List portals for the organization |
| POST | `/submission-portals` | Create a portal |
| GET | `/submission-portals/by-slug/{slug}` | Look up a portal by its public slug |
| GET | `/submission-portals/default-for-configuration/{configurationName}` | Look up the default portal for a processing configuration |
| GET | `/submission-portals/{id}` | Get a portal |
| PUT | `/submission-portals/{id}` | Update a portal |
| DELETE | `/submission-portals/{id}` | Delete a portal |
| POST | `/submission-portals/{id}/submissions` | Submit documents (multipart, field `documents`) — returns a receipt |
| GET | `/submission-records/{receiptCode}` | Check submission status |
| GET | `/submission-records/{receiptCode}/download` | Download the result |

`CreateSubmissionPortalRequest` fields: `slug`, `configurationName`, `title`, `description?`, `submitButtonLabel?`, `successMessage?`, `inputMode`, `resultMode`, `downloadFormat`, `isDefaultForConfiguration`, `active`.

`SubmissionSubmitResponse` fields: `receiptCode`, `jobGuid`, `jobType`, `status`, `resultMode`, `downloadFormat`, `fileCount`.

`SubmissionRecordResponse` adds `portalSlug`, `originalFileNames`, `errorMessage`, `createdAt`, `completedAt` to the submission status. `status` is one of `CREATED`, `UPLOADING`, `PROCESSING`, `COMPLETED`, `FAILED`, `PARTIAL`.

## Examples

Create a portal for an existing processing configuration:

**cURL**

```bash
# The portal wraps an existing processing configuration, here "invoice-extractor".
curl -s -S -f -X POST "https://api.docudevs.ai/submission-portals" \
  -H "Authorization: $API_KEY" \
  -H "Content-Type: application/json" \
  -d '{
    "slug": "invoices",
    "configurationName": "invoice-extractor",
    "title": "Submit an invoice",
    "inputMode": "SINGLE_FILE",
    "resultMode": "DOWNLOAD",
    "downloadFormat": "EXCEL",
    "isDefaultForConfiguration": true,
    "active": true
  }'
```

Submit a document to the portal by its numeric `id`. The response carries the `receiptCode`:

**cURL**

```bash
# PORTAL_ID is the "id" returned when the portal was created or listed.
PORTAL_ID=42

# The response holds the receiptCode and the jobGuid
curl -s -S -f -X POST "https://api.docudevs.ai/submission-portals/$PORTAL_ID/submissions" \
  -H "Authorization: $API_KEY" \
  -F "documents=@invoice.pdf"
```

Check the submission and download the result:

**cURL**

```bash
# RECEIPT_CODE is the "receiptCode" returned by the submission.
RECEIPT_CODE=AB12CD34

# Check the status: CREATED, UPLOADING, PROCESSING, COMPLETED, FAILED or PARTIAL
curl -s -S -f "https://api.docudevs.ai/submission-records/$RECEIPT_CODE" \
  -H "Authorization: $API_KEY"

# Download the result once the status is COMPLETED (409 while it is not ready)
curl -s -S -f "https://api.docudevs.ai/submission-records/$RECEIPT_CODE/download" \
  -H "Authorization: $API_KEY" \
  -o result.xlsx
```

The Python SDK has no helpers for the submission portal routes, so these examples are cURL only.
