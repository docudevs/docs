---
title: Cases - Document Collections
description: Organize related documents into cases for grouped management, scoped processing, and document lifecycle tracking.
sidebar_position: 1
---

import Tabs from '@theme/Tabs';
import TabItem from '@theme/TabItem';

# Cases - Document Collections

Cases are a powerful feature that allows you to organize related documents into collections for better management and processing. Think of a case as a folder that can contain multiple documents that belong together - like all documents for a specific customer, project, or business process.

## What are Cases?

A **Case** is a logical container for grouping related documents. Each case has:

- **Name**: A descriptive identifier for the case
- **Description**: Optional detailed information about the case purpose
- **Organization Scope**: Cases belong to your organization
- **User Scope**: Cases can be user-specific or organization-wide
- **Documents**: Multiple documents can be uploaded to each case
- **Metadata**: Rich metadata and processing status for each document

## When to Use Cases

Cases are ideal for scenarios where you need to:

- **Process multiple documents together** (invoices from the same vendor, contract amendments, etc.)
- **Organize documents by project** (real estate transaction, insurance claim, etc.)
- **Track processing status** across multiple related documents
- **Maintain document relationships** and context
- **Batch process** documents that share similar characteristics

### Common Use Cases

- **Invoice Processing**: Group all invoices from a specific vendor or time period
- **Contract Management**: Organize contract, amendments, and related documents
- **Insurance Claims**: Collect all documents related to a single claim
- **Real Estate Transactions**: Group property documents, inspections, and contracts
- **Compliance Audits**: Organize documents required for regulatory compliance

## Case Management

### Creating Cases

<Example name="cases/create" />

### Listing Cases

<Example name="cases/list" />

### Getting Case Details

<Example name="cases/get" />

### Updating Cases

<Example name="cases/update" />

### Deleting Cases

<Example name="cases/delete" />

:::warning
Deleting a case will also delete all documents associated with that case. This action cannot be undone.
:::

Deleting a [records](./records-datasets.md) case also removes its records, search documents and stored files. The call returns `409` while an import or tree activation is running for the case; wait for it to finish and retry. A records case does not accept document uploads (`409`): import records instead.

## Document Management within Cases

### Uploading Documents to Cases

<Example name="cases/upload-document" />

### Listing Documents in a Case

<Example name="cases/list-documents" />

### Getting Document Details

<Example name="cases/get-document" />

### Deleting Documents from Cases

<Example name="cases/delete-document" />

## Document Summaries

When documents are uploaded to a case, DocuDevs automatically generates AI-powered summaries for each document. These summaries include a text overview, document type classification, key entities, and a section breakdown — useful for quick triage, compliance checks, and building evaluation workflows.

### Getting a Document Summary

Retrieve the AI-generated summary for a specific document:

<Example name="cases/get-summary" />

The response includes:

| Field | Description |
|-------|-------------|
| `documentId` | The document GUID |
| `filename` | Original filename |
| `summary` | AI-generated summary object (see below), or `null` if not yet generated |

The `summary` object contains:

| Field | Description |
|-------|-------------|
| `summary` | Text overview of the document content |
| `document_type` | Classified document type (e.g., "invoice", "contract") |
| `key_entities` | Important entities extracted from the document |
| `section_overview` | Breakdown of document sections |
| `word_count` | Approximate word count |

### Listing All Case Summaries

Retrieve summaries for every document in a case at once — useful for getting a birds-eye view of all documents:

<Example name="cases/list-summaries" />

:::tip
Summaries are generated asynchronously after document upload. If a document was just uploaded, the `summary` field may be `null` until processing completes.
:::

## User vs Organization Scope

Cases support two levels of scope:

### Organization-wide Cases

- **Created by**: Any user in the organization
- **Visible to**: All users in the organization  
- **Use case**: Shared projects, company-wide document processing

### User-specific Cases

- **Created by**: A specific user
- **Visible to**: Only the creating user (and organization admins)
- **Use case**: Personal projects, user-specific workflows

The scope is automatically determined based on your API key and user context when creating cases.

## Processing Status Tracking

Each document in a case tracks its processing status:

- **`PENDING`**: Document uploaded, waiting to be processed
- **`PROCESSING`**: Document is currently being processed
- **`COMPLETED`**: Processing completed successfully
- **`FAILED`**: Processing failed due to an error

You can monitor processing progress by checking the `processing_status` field when listing documents.

## Integration with Document Processing

Documents uploaded to cases can be processed just like standalone documents:

```python
from docudevs.models import CasesControllerCreateCaseRequest, UploadCaseDocumentBody
from docudevs.types import File
import json

# Step 1: Create a case first
case_request = CasesControllerCreateCaseRequest(
    name="Contract Review",
    description="Contract documents for review"
)
case_response = await client.create_case(case_request)
case = case_response.parsed
case_id = case.id

# Step 2: Upload document to case
with open("contract.pdf", "rb") as f:
    document_data = f.read()

upload_body = UploadCaseDocumentBody(
    document=File(payload=document_data, file_name="contract.pdf")
)
upload_response = await client.upload_case_document(case_id=case_id, body=upload_body)
document = upload_response.parsed

print(f"Uploaded document {document.document_id} to case {case_id}")
```

:::note
Case documents are stored and organized within the case. To process them with AI extraction,
you would process them separately using the standard document processing API with
`submit_and_process_document()`.
:::

## Best Practices

### Case Organization

- Use **descriptive names** that clearly indicate the case purpose
- Include **relevant dates** in case names (e.g., "Q4 2024 Invoices")
- Add **detailed descriptions** for complex cases
- Use **consistent naming conventions** across your organization

### Document Management

- **Upload documents immediately** when they become available
- **Check processing status** regularly for large batches
- **Use meaningful filenames** that help identify document contents
- **Include metadata** in document uploads when relevant

### Performance Optimization

- **Batch upload** multiple documents to the same case when possible
- **Monitor processing status** to avoid overwhelming the system
- **Use pagination** when listing large numbers of documents
- **Delete unnecessary cases** to keep your workspace organized

## Error Handling

Always implement proper error handling when working with cases:

```python
try:
    response = await client.create_case(case_data)
    case = response.parsed
    print(f"Created case: {case.id}")
except Exception as e:
    print(f"Error creating case: {e}")
    # Handle error appropriately

try:
    response = await client.upload_case_document(case_id, document_data, filename)
    document = response.parsed
    print(f"Uploaded document: {document.document_id}")
except Exception as e:
    print(f"Error uploading document: {e}")
    # Handle error appropriately
```

## What's Next?

- Learn about [Job Management](/docs/advanced/job-management) for cleanup and data retention
- Learn about [Operations](/docs/advanced/operations) for advanced document processing workflows
- Explore [Named Configurations](/docs/configuration/) for consistent processing settings
- Check out [Use Cases](/docs/integration/use-cases) for real-world implementation examples
- Review [Best Practices](/docs/integration/best-practices) for production deployment guidelines
