---
title: Templates
description: Upload reusable document templates and fill them programmatically with extracted or provided data.
sidebar_position: 3
---

# Templates

Upload and reuse document templates (PDF, Word, Excel) to automatically fill them with data.

## Overview

Templates allow you to treat documents as reusable forms. You upload a document once, and DocuDevs analyzes it to find fillable fields. You can then fill these templates repeatedly with new data via the API.

For PDFs, there are three distinct workflows:

- **Template workflow**: upload a reusable AcroForm PDF, inspect field names, and fill it repeatedly.
- **Metadata workflow**: inspect AcroForm fields, widgets, and page coordinates for an existing PDF without creating a reusable template. See [AcroForm Metadata for Existing PDFs](#acroform-metadata-for-existing-pdfs).
- **Conversion workflow**: start from a normal PDF, run the `pdf-acroform` operation to generate a fillable AcroForm PDF, inspect editable field definitions, and optionally regenerate the PDF after review. See [PDF AcroForm Conversion](../advanced/operations.md#pdf-acroform-conversion).

**Supported Formats:**

- **PDF Forms**: Standard AcroForms.
- **Word Documents**: `.docx` files with placeholders or content controls.
- **Excel Spreadsheets**: `.xlsx` files.
- **PowerPoint Presentations**: `.pptx` files.

## Managing Templates

### Uploading a Template

Upload a document to create a new template. DocuDevs stores the template immediately, then prepares PDF field metadata in the background when the file is an AcroForm PDF.

<Example name="templates/upload-template" />

### Upload and Fill Responses

`POST /template/{name}` uploads or replaces a template. A PDF upload returns a `guid`, a `status` of `completed`, `processing`, or `error`, and the extracted `formFields` when available; an error response can include an `error` message. A non-PDF upload returns the job `guid` and `status` in an `UploadResponse`.

`POST /template/fill/{name}` accepts a JSON `fields` object and returns the filled file as either `application/msword` or `application/pdf`. The success response is binary: keep it as a download, because it is not a JSON response.

Both endpoints can return:

- **409 Conflict**: the request conflicts with an active review run.
- **503 Service Unavailable**: the service is busy. Retry the request after a short delay.

### Listing Templates

Get a list of all available templates in your organization.

<Example name="templates/list-templates" />

### Inspecting Template Metadata

Retrieve details about a specific template, including its fillable fields.

For PDF templates, `GET /template/metadata/{name}` returns `202 Accepted` with `Retry-After: 1` while field extraction is still pending. The cURL example polls until the metadata is ready, and the Python SDK helper `wait_for_template_metadata(...)` does the same for you.

<Example name="templates/get-template-metadata" />

### Deleting a Template

Remove a template when it is no longer needed.

<Example name="templates/delete-template" />

## Filling Templates

Once a template is uploaded, you can fill it with data. The data structure depends on the template type.

If you fill a template immediately after upload, prefer `fill_with_retry(...)` in the Python SDK. It retries transient readiness races while background template preparation finishes.

### Filling a PDF Form

PDF forms typically use a flat dictionary of field names and values.

<Example name="templates/fill-pdf-form" />

### Filling a Word Template with Tables

Word templates support nested data structures, allowing you to fill tables and repeated sections.

<Example name="templates/fill-word-template" />

## AcroForm Metadata for Existing PDFs

Use AcroForm metadata when you need field IDs, widget IDs, page numbers, or page-anchor coordinates from an existing fillable PDF.

- Use the **direct metadata endpoint** when you only need the AcroForm structure.
- Use the **async job flow** when you also need processed-job images, source locations, extraction, or overlays tied to a job GUID.

The direct endpoint accepts PDF uploads only. Non-PDF uploads return `400 Bad Request`. A normal PDF without an AcroForm still returns `200 OK` with `fields: []`.

If the source PDF is visually form-like but `fields` comes back empty, the document does not already contain embedded AcroForm widgets. In that case, use the `pdf-acroform` operation to generate a new fillable PDF and inspect its editable field definitions. The full conversion and review loop is documented in [Operations](../advanced/operations.md#pdf-acroform-conversion).

Direct metadata for an existing PDF:

<Example name="templates/acroform-metadata" />

To get AcroForm metadata and source locations tied to a processed job, request them when you submit the document, wait for the job, and then fetch the stored artifacts:

<Example name="templates/acroform-job-metadata" />

For OCR-backed evidence overlays on the same job, combine `acro_form_metadata=True` with `source_locations=True` and then read [Source Locations](/docs/core/source-locations).

## Best Practices

- **Field Naming**: Use clear, consistent names for your form fields (e.g., camelCase like `customerName`).
- **Testing**: Upload your template and inspect the metadata to ensure all fields are detected correctly.
- **Data Types**: Ensure the data you send matches the expected type (e.g., booleans for checkboxes).
- **Configurations**: Combine templates with [Named Configurations](../configuration/Configuration.md) to standardize the filling process if you have complex logic.
