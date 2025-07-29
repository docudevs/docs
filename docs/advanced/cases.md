---
title: Cases - Document Collections
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

<Tabs
  defaultValue="curl"
  values={[
    {label: 'cURL', value: 'curl'},
    {label: 'Python SDK', value: 'python'},
    {label: 'CLI', value: 'cli'},
  ]}>
  <TabItem value="curl">
```bash
curl -X POST https://api.docudevs.ai/cases \
  -H "Authorization: Bearer $API_KEY" \
  -H "Content-Type: application/json" \
  -d '{
    "name": "Q4 2024 Vendor Invoices",
    "description": "All invoices from vendors for Q4 2024 processing"
  }'
```
  </TabItem>
  <TabItem value="python">
```python
from docudevs.docudevs_client import DocuDevsClient
import os

# Initialize the client
client = DocuDevsClient(token=os.getenv('API_KEY'))

# Create a new case
case_data = {
    "name": "Q4 2024 Vendor Invoices",
    "description": "All invoices from vendors for Q4 2024 processing"
}

response = await client.create_case(case_data)
case = response.parsed
print(f"Created case ID: {case.id}")
```
  </TabItem>
  <TabItem value="cli">
```bash
# Note: CLI support for cases is coming soon
# For now, use the API directly or Python SDK
```
  </TabItem>
</Tabs>

### Listing Cases

<Tabs
  defaultValue="curl"
  values={[
    {label: 'cURL', value: 'curl'},
    {label: 'Python SDK', value: 'python'},
  ]}>
  <TabItem value="curl">
```bash
curl -X GET https://api.docudevs.ai/cases \
  -H "Authorization: Bearer $API_KEY"
```
  </TabItem>
  <TabItem value="python">
```python
# List all cases for your organization
response = await client.list_cases()
cases = response.parsed

for case in cases:
    print(f"Case {case.id}: {case.name}")
    if case.description:
        print(f"  Description: {case.description}")
    print(f"  Created: {case.created_at}")
```
  </TabItem>
</Tabs>

### Getting Case Details

<Tabs
  defaultValue="curl"
  values={[
    {label: 'cURL', value: 'curl'},
    {label: 'Python SDK', value: 'python'},
  ]}>
  <TabItem value="curl">
```bash
curl -X GET https://api.docudevs.ai/cases/{case_id} \
  -H "Authorization: Bearer $API_KEY"
```
  </TabItem>
  <TabItem value="python">
```python
# Get specific case details
case_id = 123
response = await client.get_case(case_id)
case = response.parsed

print(f"Case: {case.name}")
print(f"Description: {case.description}")
print(f"Created: {case.created_at}")
print(f"Organization ID: {case.organization_id}")
```
  </TabItem>
</Tabs>

### Updating Cases

<Tabs
  defaultValue="curl"
  values={[
    {label: 'cURL', value: 'curl'},
    {label: 'Python SDK', value: 'python'},
  ]}>
  <TabItem value="curl">
```bash
curl -X PUT https://api.docudevs.ai/cases/{case_id} \
  -H "Authorization: Bearer $API_KEY" \
  -H "Content-Type: application/json" \
  -d '{
    "name": "Q4 2024 Vendor Invoices - Updated",
    "description": "Updated description with additional context"
  }'
```
  </TabItem>
  <TabItem value="python">
```python
# Update case details
case_id = 123
update_data = {
    "name": "Q4 2024 Vendor Invoices - Updated",
    "description": "Updated description with additional context"
}

response = await client.update_case(case_id, update_data)
updated_case = response.parsed
print(f"Updated case: {updated_case.name}")
```
  </TabItem>
</Tabs>

### Deleting Cases

<Tabs
  defaultValue="curl"
  values={[
    {label: 'cURL', value: 'curl'},
    {label: 'Python SDK', value: 'python'},
  ]}>
  <TabItem value="curl">
```bash
curl -X DELETE https://api.docudevs.ai/cases/{case_id} \
  -H "Authorization: Bearer $API_KEY"
```
  </TabItem>
  <TabItem value="python">
```python
# Delete a case (this will also delete all associated documents)
case_id = 123
response = await client.delete_case(case_id)

if response.status_code == 204:
    print("Case deleted successfully")
```
  </TabItem>
</Tabs>

:::warning
Deleting a case will also delete all documents associated with that case. This action cannot be undone.
:::

## Document Management within Cases

### Uploading Documents to Cases

<Tabs
  defaultValue="curl"
  values={[
    {label: 'cURL', value: 'curl'},
    {label: 'Python SDK', value: 'python'},
  ]}>
  <TabItem value="curl">
```bash
curl -X POST https://api.docudevs.ai/cases/{case_id}/documents \
  -H "Authorization: Bearer $API_KEY" \
  -F "document=@invoice_001.pdf"
```
  </TabItem>
  <TabItem value="python">
```python
# Upload a document to a specific case
case_id = 123

with open("invoice_001.pdf", "rb") as f:
    file_data = f.read()

response = await client.upload_case_document(
    case_id=case_id,
    document=file_data,
    filename="invoice_001.pdf"
)

document = response.parsed
print(f"Uploaded document {document.document_id} to case {case_id}")
print(f"Status: {document.processing_status}")
```
  </TabItem>
</Tabs>

### Listing Documents in a Case

<Tabs
  defaultValue="curl"
  values={[
    {label: 'cURL', value: 'curl'},
    {label: 'Python SDK', value: 'python'},
  ]}>
  <TabItem value="curl">
```bash
# List documents with pagination
curl -X GET "https://api.docudevs.ai/cases/{case_id}/documents?page=0&size=20" \
  -H "Authorization: Bearer $API_KEY"
```
  </TabItem>
  <TabItem value="python">
```python
# List all documents in a case with pagination
case_id = 123
page = 0
size = 20

response = await client.list_case_documents(
    case_id=case_id,
    page=page,
    size=size
)

documents_page = response.parsed
print(f"Total documents: {documents_page.total_elements}")
print(f"Current page: {documents_page.number}/{documents_page.total_pages}")

for document in documents_page.content:
    print(f"Document: {document.filename}")
    print(f"  ID: {document.document_id}")
    print(f"  Status: {document.processing_status}")
    print(f"  Size: {document.size_bytes} bytes")
    print(f"  Uploaded: {document.created_at}")
```
  </TabItem>
</Tabs>

### Getting Document Details

<Tabs
  defaultValue="curl"
  values={[
    {label: 'cURL', value: 'curl'},
    {label: 'Python SDK', value: 'python'},
  ]}>
  <TabItem value="curl">
```bash
curl -X GET https://api.docudevs.ai/cases/{case_id}/documents/{document_id} \
  -H "Authorization: Bearer $API_KEY"
```
  </TabItem>
  <TabItem value="python">
```python
# Get specific document details
case_id = 123
document_id = "doc-uuid-here"

response = await client.get_case_document(case_id, document_id)
document = response.parsed

print(f"Document: {document.filename}")
print(f"Content Type: {document.content_type}")
print(f"Processing Status: {document.processing_status}")
print(f"Metadata: {document.metadata}")
```
  </TabItem>
</Tabs>

### Deleting Documents from Cases

<Tabs
  defaultValue="curl"
  values={[
    {label: 'cURL', value: 'curl'},
    {label: 'Python SDK', value: 'python'},
  ]}>
  <TabItem value="curl">
```bash
curl -X DELETE https://api.docudevs.ai/cases/{case_id}/documents/{document_id} \
  -H "Authorization: Bearer $API_KEY"
```
  </TabItem>
  <TabItem value="python">
```python
# Delete a document from a case
case_id = 123
document_id = "doc-uuid-here"

response = await client.delete_case_document(case_id, document_id)

if response.status_code == 204:
    print("Document deleted successfully")
```
  </TabItem>
</Tabs>

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
# Upload document to case
case_id = 123
response = await client.upload_case_document(case_id, document_data, "contract.pdf")
document = response.parsed

# Process the document with instructions
document_id = document.document_id
processing_instructions = {
    "instruction": "Extract all key contract terms, dates, and parties",
    "schema": {
        "contract_date": "Date of the contract",
        "parties": "List of all parties involved",
        "key_terms": "Important contract terms and conditions"
    }
}

# Process using the standard document processing API
result = await client.process_document(document_id, processing_instructions)
```

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

- Learn about [Operations](/docs/advanced/operations) for advanced document processing workflows
- Explore [Named Configurations](/docs/configuration/Configuration) for consistent processing settings
- Check out [Use Cases](/docs/integration/use-cases) for real-world implementation examples
- Review [Best Practices](/docs/integration/best-practices) for production deployment guidelines