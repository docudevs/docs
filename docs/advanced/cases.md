---
title: Cases - Document Collections
description: Organize related documents into cases for grouped management, scoped processing, and document lifecycle tracking.
sidebar_position: 1
---

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

- **Created by**: An organization API key
- **Visible to**: All users in the organization, and to organization API keys
- **Use case**: Shared projects, company-wide document processing

### User-specific Cases

- **Created by**: A signed-in user
- **Visible to**: Only the creating user. Other users, organization admins included, and organization API keys do not see them (an API key gets `404`). Knowledge bases are the exception; see [Which cases you can use](./case-agent.md#which-cases-you-can-use).
- **Use case**: Personal projects, user-specific workflows

A signed-in user sees their own cases plus the organization-wide ones. The scope is set by how you create the case: with an API key it is organization-wide, as a signed-in user it is yours alone. See [which cases you can use](./case-agent.md#which-cases-you-can-use).

## Knowledge bases

A knowledge base is a case that you have promoted so that the whole organization can use it as reference content. A [records](./records-datasets.md) case or a case of documents can be promoted.

### Promoting a case

`POST /knowledge-bases` with the `caseId` promotes a case. It answers `201` with the case, whose `isKnowledgeBase` is `true`. Promoting a case that already is a knowledge base changes nothing. It answers `404` when the case does not exist or you may not change it.

<Example name="cases/promote-knowledge-base" />

Promotion changes who can reach the case. A case created by a signed-in user is visible only to its creator. Once it is promoted, every member of the organization and every organization API key can list and read it, and search it. Its documents, records and settings are not copied or moved. See [Which cases you can use](./case-agent.md#which-cases-you-can-use).

An organization API key can promote organization-wide cases. A signed-in user can promote their own cases and organization-wide ones; a case created by another user answers `404`, organization admins included. Users with the submission-portal role cannot call the knowledge-base routes. Demotion follows the same rules.

To start a contract-analysis playbook, pass `profileType` `"contract-analysis"` (`profile_type` in the Python client): the case is promoted and gets an empty contract-analysis profile. Any other value is refused with `400`. See [Contract analysis](../core/contract-analysis.md).

### Listing knowledge bases

`GET /knowledge-bases` lists every knowledge base of the organization, whoever created it, and `GET /knowledge-bases/{caseId}` returns one (`404` when the case is not a knowledge base). Each entry has the case `id`, `name`, `description`, `isKnowledgeBase`, `userId` (absent for an organization-wide case) and timestamps. A contract-analysis playbook also carries its `profileType`, `profileStatus` and `publishedRevision`.

<Example name="cases/list-knowledge-base" />

### Demoting a knowledge base

`DELETE /knowledge-bases/{caseId}` answers `204` and clears the designation. It answers `404` when the case is not a knowledge base or you may not change it.

<Example name="cases/demote-knowledge-base" />

Demotion keeps the case, its documents, records and trees, and a contract-analysis profile with its revisions; promoting the case again with `profileType` `"contract-analysis"` restores that profile. The case is visible again only as its scope allows: a case created by a signed-in user is back to its creator alone, and an organization-wide case stays organization-wide. `GET /knowledge-bases/{caseId}` and the contract-analysis run routes answer `404` for it until it is promoted again.

### What needs a knowledge base

- Contract analysis runs and test packages work on a knowledge base and answer `404` for a case that is not one.
- The `RECORDS_SEARCH` and `RECORDS_MATCH` extraction tools name a case with a records dataset that is organization-wide or a knowledge base; a case created by a signed-in user answers `404` until you promote it. See [Knowledge search](../core/knowledge-search.md).

## Processing Status Tracking

Each document in a case tracks its processing status:

- **`PENDING`**: Document uploaded, waiting to be processed
- **`PROCESSING`**: Document is currently being processed
- **`COMPLETED`**: Processing completed successfully
- **`FAILED`**: Processing failed due to an error

You can monitor processing progress by checking the `processing_status` field when listing documents.

## Integration with Document Processing

Documents uploaded to cases can be processed just like standalone documents:

<Example name="cases/create-and-upload" />

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

<Example name="cases/handle-errors" />

## What's Next?

- Learn about [Job Management](/docs/advanced/job-management) for cleanup and data retention
- Learn about [Operations](/docs/advanced/operations) for advanced document processing workflows
- Explore [Named Configurations](/docs/configuration/) for consistent processing settings
- Check out [Use Cases](/docs/integration/use-cases) for real-world implementation examples
- Review [Best Practices](/docs/integration/best-practices) for production deployment guidelines
