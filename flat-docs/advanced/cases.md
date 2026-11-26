---
title: Cases - Document Collections
description: Organize related documents into cases for grouped management, scoped processing, and document lifecycle tracking.
sidebar_position: 1
---
<!-- Generated from docs/advanced/cases.md. Do not edit by hand. -->

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

**cURL**

```bash
CASE_ID=$(curl -s -S -f -X POST "https://api.docudevs.ai/cases" \
  -H "Authorization: $API_KEY" \
  -H "Content-Type: application/json" \
  -d '{
    "name": "Q4 2024 Vendor Invoices",
    "description": "All invoices from vendors for Q4 2024 processing"
  }' | jq -r '.id')
echo "Created case ID: $CASE_ID"
```

**Python**

```python
import asyncio
import os

from docudevs.docudevs_client import DocuDevsClient


async def main():
    client = DocuDevsClient(token=os.environ["API_KEY"])

    response = await client.create_case(
        name="Q4 2024 Vendor Invoices",
        description="All invoices from vendors for Q4 2024 processing",
    )
    if response.status_code != 201:
        raise SystemExit(f"Create case failed: {response.status_code}")

    case = response.parsed
    print(f"Created case ID: {case.id}")


asyncio.run(main())
```

**Java**

```java
import ai.docudevs.client.DocuDevsClient;
import com.fasterxml.jackson.databind.JsonNode;

public class Example {
    public static void main(String[] args) throws Exception {
        DocuDevsClient client = DocuDevsClient.builder()
                .apiKey(System.getenv("API_KEY"))
                .baseUrl("https://api.docudevs.ai")
                .build();

        JsonNode created = client.createCase(
                "Q4 2024 Vendor Invoices",
                "All invoices from vendors for Q4 2024 processing");

        System.out.println("Created case ID: " + created.path("id").asLong());
    }
}
```

**CLI**

```bash
docudevs cases create \
  --name "Q4 2024 Vendor Invoices" \
  --description "All invoices from vendors for Q4 2024 processing"
```

### Listing Cases

**cURL**

```bash
curl -s -S -f -X GET "https://api.docudevs.ai/cases" \
  -H "Authorization: $API_KEY" | jq -r '.[] | "Case \(.id): \(.name)"'
```

**Python**

```python
response = await client.list_cases()
if response.status_code != 200:
    raise SystemExit(f"List cases failed: {response.status_code}")

cases = response.parsed

for case in cases:
    print(f"Case {case.id}: {case.name}")
    if case.description:
        print(f"  Description: {case.description}")
    print(f"  Created: {case.created_at}")
```

**Java**

```java
import com.fasterxml.jackson.databind.JsonNode;

JsonNode cases = client.listCases();

for (JsonNode item : cases) {
    System.out.println("Case " + item.path("id").asLong() + ": " + item.path("name").asText());
}
```

**CLI**

```bash
docudevs cases list
```

### Getting Case Details

**cURL**

```bash
CASE_ID=123  # id returned when the case was created

curl -s -S -f -X GET "https://api.docudevs.ai/cases/$CASE_ID" \
  -H "Authorization: $API_KEY" | jq .
```

**Python**

```python
case_id = 123  # id returned by create_case
response = await client.get_case(case_id)
if response.status_code != 200:
    raise SystemExit(f"Get case failed: {response.status_code}")

case = response.parsed

print(f"Case: {case.name}")
print(f"Description: {case.description}")
print(f"Created: {case.created_at}")
print(f"Organization ID: {case.organization_id}")
```

**Java**

```java
import com.fasterxml.jackson.databind.JsonNode;

long caseId = 123L; // id returned by createCase
JsonNode caseDetails = client.getCase(caseId);

System.out.println("Case: " + caseDetails.path("name").asText());
System.out.println("Description: " + caseDetails.path("description").asText());
System.out.println("Created: " + caseDetails.path("createdAt").asText());
System.out.println("Organization ID: " + caseDetails.path("organizationId").asLong());
```

**CLI**

```bash
docudevs cases get 123
```

### Updating Cases

**cURL**

```bash
CASE_ID=123  # id returned when the case was created

curl -s -S -f -X PUT "https://api.docudevs.ai/cases/$CASE_ID" \
  -H "Authorization: $API_KEY" \
  -H "Content-Type: application/json" \
  -d '{
    "name": "Q4 2024 Vendor Invoices - Updated",
    "description": "Updated description with additional context"
  }' | jq .
```

**Python**

```python
from docudevs.models import CasesControllerUpdateCaseRequest

case_id = 123  # id returned by create_case
update_request = CasesControllerUpdateCaseRequest(
    name="Q4 2024 Vendor Invoices - Updated",
    description="Updated description with additional context",
)

response = await client.update_case(case_id, update_request)
if response.status_code != 200:
    raise SystemExit(f"Update case failed: {response.status_code}")

updated_case = response.parsed
print(f"Updated case: {updated_case.name}")
```

**Java**

```java
import com.fasterxml.jackson.databind.JsonNode;

long caseId = 123L; // id returned by createCase
JsonNode updated = client.updateCase(
        caseId,
        "Q4 2024 Vendor Invoices - Updated",
        "Updated description with additional context");

System.out.println("Updated case: " + updated.path("name").asText());
```

**CLI**

```bash
docudevs cases update 123 \
  --name "Q4 2024 Vendor Invoices - Updated" \
  --description "Updated description with additional context"
```

### Deleting Cases

**cURL**

```bash
CASE_ID=123  # id returned when the case was created

curl -s -S -f -o /dev/null -X DELETE "https://api.docudevs.ai/cases/$CASE_ID" \
  -H "Authorization: $API_KEY"
echo "Case deleted successfully"
```

**Python**

```python
case_id = 123  # id returned by create_case
response = await client.delete_case(case_id)

if response.status_code == 204:
    print("Case deleted successfully")
```

**Java**

```java
long caseId = 123L; // id returned by createCase
client.deleteCase(caseId);
System.out.println("Case deleted successfully");
```

**CLI**

```bash
docudevs cases delete 123
```

:::warning
Deleting a case will also delete all documents associated with that case. This action cannot be undone.
:::

Deleting a [records](./records-datasets.md) case also removes its records, search documents and stored files. The call returns `409` while an import or tree activation is running for the case; wait for it to finish and retry. A records case does not accept document uploads (`409`): import records instead.

## Document Management within Cases

### Uploading Documents to Cases

**cURL**

```bash
CASE_ID=123  # id returned when the case was created

curl -s -S -f -o /dev/null -w "Upload status: %{http_code}\n" -X POST "https://api.docudevs.ai/cases/$CASE_ID/documents" \
  -H "Authorization: $API_KEY" \
  -F "document=@invoice_001.pdf"
```

**Python**

```python
from docudevs.models import UploadCaseDocumentBody
from docudevs.types import File

case_id = 123  # id returned by create_case

with open("invoice_001.pdf", "rb") as f:
    file_data = f.read()

body = UploadCaseDocumentBody(
    document=File(payload=file_data, file_name="invoice_001.pdf", mime_type="application/pdf")
)
response = await client.upload_case_document(case_id=case_id, body=body)
print(f"Upload status: {response.status_code}")
```

**Java**

```java
import ai.docudevs.client.UploadRequest;

long caseId = 123L; // id returned by createCase

byte[] fileBytes = Files.readAllBytes(Path.of("invoice_001.pdf"));
UploadRequest upload = new UploadRequest("invoice_001.pdf", "application/pdf", fileBytes);

client.uploadCaseDocument(caseId, upload);
System.out.println("Upload status: OK");
```

**CLI**

```bash
docudevs cases upload-document 123 invoice_001.pdf
```

### Listing Documents in a Case

**cURL**

```bash
CASE_ID=123  # id returned when the case was created

curl -s -S -f -X GET "https://api.docudevs.ai/cases/$CASE_ID/documents?page=0&size=20" \
  -H "Authorization: $API_KEY" | jq .
```

**Python**

```python
case_id = 123  # id returned by create_case

response = await client.list_case_documents(case_id=case_id, page=0, size=20)
if response.status_code != 200:
    raise SystemExit(f"List documents failed: {response.status_code}")

documents_page = response.parsed

print(f"Total documents: {documents_page.total_size}")
print(f"Page: {documents_page.page_number}/{documents_page.total_pages}")

for document in documents_page.content:
    print(f"{document.filename} [{document.document_id}]")
    print(f"  status={document.processing_status}")
    print(f"  size={document.size_bytes} bytes, uploaded={document.created_at}")
```

**Java**

```java
import com.fasterxml.jackson.databind.JsonNode;

long caseId = 123L; // id returned by createCase

JsonNode documentsPage = client.listCaseDocuments(caseId, 0, 20);

System.out.println("Total documents: " + documentsPage.path("totalSize").asInt());
System.out.println("Page: " + documentsPage.path("pageNumber").asInt() + "/" + documentsPage.path("totalPages").asInt());

for (JsonNode document : documentsPage.path("content")) {
    System.out.println(document.path("filename").asText() + " [" + document.path("documentId").asText() + "]");
    System.out.println("  status=" + document.path("processingStatus").asText());
}
```

**CLI**

```bash
docudevs cases list-documents 123 --page 0 --size 20
```

### Getting Document Details

**cURL**

```bash
CASE_ID=123  # id returned when the case was created
DOCUMENT_ID="doc-uuid-here"  # documentId from the list-documents response

curl -s -S -f -X GET "https://api.docudevs.ai/cases/$CASE_ID/documents/$DOCUMENT_ID" \
  -H "Authorization: $API_KEY" | jq .
```

**Python**

```python
case_id = 123  # id returned by create_case
document_id = "doc-uuid-here"  # documentId from list_case_documents

response = await client.get_case_document(case_id, document_id)
if response.status_code != 200:
    raise SystemExit(f"Get document failed: {response.status_code}")

document = response.parsed

print(f"Document: {document.filename}")
print(f"Content Type: {document.content_type}")
print(f"Processing Status: {document.processing_status}")
print(f"Metadata: {document.metadata}")
```

**Java**

```java
import com.fasterxml.jackson.databind.JsonNode;

long caseId = 123L; // id returned by createCase
String documentId = "doc-uuid-here"; // documentId from listCaseDocuments

JsonNode document = client.getCaseDocument(caseId, documentId);

System.out.println("Document: " + document.path("filename").asText());
System.out.println("Content Type: " + document.path("contentType").asText());
System.out.println("Processing Status: " + document.path("processingStatus").asText());
System.out.println("Metadata: " + document.path("metadata"));
```

**CLI**

```bash
docudevs cases get-document 123 doc-uuid-here
```

### Deleting Documents from Cases

**cURL**

```bash
CASE_ID=123  # id returned when the case was created
DOCUMENT_ID="doc-uuid-here"  # documentId from the list-documents response

curl -s -S -f -o /dev/null -X DELETE "https://api.docudevs.ai/cases/$CASE_ID/documents/$DOCUMENT_ID" \
  -H "Authorization: $API_KEY"
echo "Document deleted successfully"
```

**Python**

```python
case_id = 123  # id returned by create_case
document_id = "doc-uuid-here"  # documentId from list_case_documents

response = await client.delete_case_document(case_id, document_id)

if response.status_code == 200:
    print("Document deleted successfully")
```

**Java**

```java
long caseId = 123L; // id returned by createCase
String documentId = "doc-uuid-here"; // documentId from listCaseDocuments

client.deleteCaseDocument(caseId, documentId);
System.out.println("Document deleted successfully");
```

**CLI**

```bash
docudevs cases delete-document 123 doc-uuid-here
```

## Document Summaries

When documents are uploaded to a case, DocuDevs automatically generates AI-powered summaries for each document. These summaries include a text overview, document type classification, key entities, and a section breakdown — useful for quick triage, compliance checks, and building evaluation workflows.

### Getting a Document Summary

Retrieve the AI-generated summary for a specific document:

**cURL**

```bash
CASE_ID=123  # id returned when the case was created
DOCUMENT_ID="doc-uuid-here"  # documentId from the list-documents response

curl -s -S -f -X GET "https://api.docudevs.ai/cases/$CASE_ID/documents/$DOCUMENT_ID/summary" \
  -H "Authorization: $API_KEY" | jq .
```

**Python**

```python
case_id = 123  # id returned by create_case
document_id = "doc-uuid-here"  # documentId from list_case_documents

result = await client.get_document_summary(case_id=case_id, document_id=document_id)
if result.status_code != 200:
    raise SystemExit(f"Get summary failed: {result.status_code}")

summary = result.parsed
print(summary["filename"])  # "invoice-q4.pdf"
details = summary.get("summary") or {}  # absent until the document is summarised
print(details.get("document_type"))  # "invoice"
print(details.get("summary"))  # "Monthly invoice from Acme Corp..."
```

**Java**

```java
import com.fasterxml.jackson.databind.JsonNode;

long caseId = 123L; // id returned by createCase
String documentId = "doc-uuid-here"; // documentId from listCaseDocuments

JsonNode summary = client.getDocumentSummary(caseId, documentId);
System.out.println(summary.path("filename").asText());
System.out.println(summary.path("summary").path("document_type").asText());
System.out.println(summary.path("summary").path("summary").asText());
```

**CLI**

```bash
docudevs cases get-summary 123 doc-uuid-here
```

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

**cURL**

```bash
CASE_ID=123  # id returned when the case was created

curl -s -S -f -X GET "https://api.docudevs.ai/cases/$CASE_ID/summaries" \
  -H "Authorization: $API_KEY" | jq -r ".[] | \"\(.filename): \(.summary.document_type)\""
```

**Python**

```python
case_id = 123  # id returned by create_case

result = await client.list_case_summaries(case_id=case_id)
if result.status_code != 200:
    raise SystemExit(f"List summaries failed: {result.status_code}")

for doc in result.parsed:
    print(f"{doc['filename']}: {(doc.get('summary') or {}).get('document_type')}")
```

**Java**

```java
import com.fasterxml.jackson.databind.JsonNode;

long caseId = 123L; // id returned by createCase

JsonNode summaries = client.listCaseSummaries(caseId);
for (JsonNode doc : summaries) {
    System.out.println(doc.path("filename").asText() + ": "
            + doc.path("summary").path("document_type").asText());
}
```

**CLI**

```bash
docudevs cases list-summaries 123
```

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

**cURL**

```bash
CASE_ID=42  # id of an organization-wide case, created with an API key

curl -s -S -f -X POST "https://api.docudevs.ai/knowledge-bases" \
  -H "Authorization: $API_KEY" \
  -H "Content-Type: application/json" \
  -d "{\"caseId\": $CASE_ID}" | jq '{id, name, isKnowledgeBase}'
```

**Python**

```python
case_id = 42  # id of an organization-wide case, created with an API key

response = await client.promote_knowledge_base(case_id)
if response.status_code != 201:
    raise SystemExit(f"Promote failed: {response.status_code}")

case = response.parsed
print(f"Case {case['id']} ({case['name']}): knowledge base = {case['isKnowledgeBase']}")
```

**Java**

```java
import com.fasterxml.jackson.databind.JsonNode;

long caseId = 42L; // id of an organization-wide case, created with an API key

JsonNode promoted = client.promoteKnowledgeBase(caseId);

System.out.println("Case " + promoted.path("id").asLong() + " (" + promoted.path("name").asText()
        + "): knowledge base = " + promoted.path("isKnowledgeBase").asBoolean());
```

**CLI**

```bash
docudevs knowledge-base add 42
```

Promotion changes who can reach the case. A case created by a signed-in user is visible only to its creator. Once it is promoted, every member of the organization and every organization API key can list and read it, and search it. Its documents, records and settings are not copied or moved. See [Which cases you can use](./case-agent.md#which-cases-you-can-use).

An organization API key can promote organization-wide cases. A signed-in user can promote their own cases and organization-wide ones; a case created by another user answers `404`, organization admins included. Users with the submission-portal role cannot call the knowledge-base routes. Demotion follows the same rules.

To start a contract-analysis playbook, pass `profileType` `"contract-analysis"` (`profile_type` in the Python client): the case is promoted and gets an empty contract-analysis profile. Any other value is refused with `400`. See [Contract-analysis playbooks](../core/contract-analysis-playbooks.md).

### Listing knowledge bases

`GET /knowledge-bases` lists every knowledge base of the organization, whoever created it, and `GET /knowledge-bases/{caseId}` returns one (`404` when the case is not a knowledge base). Each entry has the case `id`, `name`, `description`, `isKnowledgeBase`, `userId` (absent for an organization-wide case) and timestamps. A contract-analysis playbook also carries its `profileType`, `profileStatus` and `publishedRevision`.

**cURL**

```bash
curl -s -S -f "https://api.docudevs.ai/knowledge-bases" \
  -H "Authorization: $API_KEY" | jq -r '(. // [])[] | "Knowledge base \(.id): \(.name)"'
```

**Python**

```python
response = await client.list_knowledge_bases()
if response.status_code != 200:
    raise SystemExit(f"List knowledge bases failed: {response.status_code}")

for knowledge_base in response.parsed or []:
    print(f"Knowledge base {knowledge_base['id']}: {knowledge_base['name']}")
```

**Java**

```java
import com.fasterxml.jackson.databind.JsonNode;

JsonNode knowledgeBases = client.listKnowledgeBases();

for (JsonNode knowledgeBase : knowledgeBases) {
    System.out.println("Knowledge base " + knowledgeBase.path("id").asLong() + ": "
            + knowledgeBase.path("name").asText());
}
```

**CLI**

```bash
docudevs knowledge-base list
```

### Demoting a knowledge base

`DELETE /knowledge-bases/{caseId}` answers `204` and clears the designation. It answers `404` when the case is not a knowledge base or you may not change it.

**cURL**

```bash
CASE_ID=42  # id of a knowledge base

curl -s -S -f -o /dev/null -X DELETE "https://api.docudevs.ai/knowledge-bases/$CASE_ID" \
  -H "Authorization: $API_KEY"
echo "Case $CASE_ID is no longer a knowledge base"
```

**Python**

```python
case_id = 42  # id of a knowledge base

response = await client.delete_knowledge_base(case_id)
if response.status_code >= 300:
    raise SystemExit(f"Demote failed: {response.status_code}")

print(f"Case {case_id} is no longer a knowledge base")
```

**Java**

```java
long caseId = 42L; // id of a knowledge base

// Returns nothing; an error status raises DocuDevsApiException
client.deleteKnowledgeBase(caseId);

System.out.println("Case " + caseId + " is no longer a knowledge base");
```

**CLI**

```bash
docudevs knowledge-base remove 42
```

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

**cURL**

```bash
# Step 1: Create a case first
CASE_ID=$(curl -s -S -f -X POST "https://api.docudevs.ai/cases" \
  -H "Authorization: $API_KEY" \
  -H "Content-Type: application/json" \
  -d '{
    "name": "Contract Review",
    "description": "Contract documents for review"
  }' | jq -r '.id')

# Step 2: Upload document to case
curl -s -S -f -o /dev/null -X POST "https://api.docudevs.ai/cases/$CASE_ID/documents" \
  -H "Authorization: $API_KEY" \
  -F "document=@contract.pdf"

echo "Uploaded document to case $CASE_ID"
```

**Python**

```python
from docudevs.models import UploadCaseDocumentBody
from docudevs.types import File

# Step 1: Create a case first
case_response = await client.create_case(
    name="Contract Review",
    description="Contract documents for review",
)
if case_response.status_code != 201:
    raise SystemExit(f"Creating the case failed: HTTP {case_response.status_code}")
case_id = case_response.parsed.id

# Step 2: Upload document to case
with open("contract.pdf", "rb") as f:
    document_data = f.read()

upload_body = UploadCaseDocumentBody(
    document=File(payload=document_data, file_name="contract.pdf", mime_type="application/pdf")
)
upload_response = await client.upload_case_document(case_id=case_id, body=upload_body)

print(f"Uploaded document to case {case_id}, status {upload_response.status_code}")
```

**Java**

```java
import ai.docudevs.client.UploadRequest;
import com.fasterxml.jackson.databind.JsonNode;

// Step 1: Create a case first
JsonNode created = client.createCase("Contract Review", "Contract documents for review");
long caseId = created.path("id").asLong();

// Step 2: Upload document to case
byte[] documentData = Files.readAllBytes(Path.of("contract.pdf"));
client.uploadCaseDocument(caseId, new UploadRequest("contract.pdf", "application/pdf", documentData));

System.out.println("Uploaded document to case " + caseId);
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

**cURL**

```bash
if ! CASE_ID=$(curl -s -S -f -X POST "https://api.docudevs.ai/cases" \
  -H "Authorization: $API_KEY" \
  -H "Content-Type: application/json" \
  -d '{"name": "Contract Review"}' | jq -r '.id'); then
  echo "Error creating case" >&2
  exit 1
fi
echo "Created case: $CASE_ID"

if ! curl -s -S -f -o /dev/null -X POST "https://api.docudevs.ai/cases/$CASE_ID/documents" \
  -H "Authorization: $API_KEY" \
  -F "document=@contract.pdf"; then
  echo "Error uploading document" >&2
  exit 1
fi
echo "Uploaded document to case"
```

**Python**

```python
from docudevs.models import UploadCaseDocumentBody
from docudevs.types import File

case_id = None
try:
    response = await client.create_case(name="Contract Review")
    if response.status_code != 201:
        raise RuntimeError(f"HTTP {response.status_code}")
    case_id = response.parsed.id
    print(f"Created case: {case_id}")
except Exception as e:
    print(f"Error creating case: {e}")
    # Handle error appropriately

if case_id is not None:
    try:
        with open("contract.pdf", "rb") as f:
            document_data = f.read()

        body = UploadCaseDocumentBody(
            document=File(payload=document_data, file_name="contract.pdf", mime_type="application/pdf")
        )
        response = await client.upload_case_document(case_id=case_id, body=body)
        if response.status_code != 201:
            raise RuntimeError(f"HTTP {response.status_code}")
        print("Uploaded document to case")
    except Exception as e:
        print(f"Error uploading document: {e}")
        # Handle error appropriately
```

**Java**

```java
import ai.docudevs.client.DocuDevsApiException;
import ai.docudevs.client.DocuDevsClient;
import ai.docudevs.client.UploadRequest;
import com.fasterxml.jackson.databind.JsonNode;

long caseId;
try {
    JsonNode created = client.createCase("Contract Review", null);
    caseId = created.path("id").asLong();
    System.out.println("Created case: " + caseId);
} catch (DocuDevsApiException e) {
    System.out.println("Error creating case: HTTP " + e.getStatusCode() + " " + e.getResponseBody());
    return;
}

try {
    byte[] documentData = Files.readAllBytes(Path.of("contract.pdf"));
    client.uploadCaseDocument(caseId, new UploadRequest("contract.pdf", "application/pdf", documentData));
    System.out.println("Uploaded document to case");
} catch (DocuDevsApiException e) {
    System.out.println("Error uploading document: HTTP " + e.getStatusCode() + " " + e.getResponseBody());
}
```

## What's Next?

- Learn about [Job Management](https://docs.docudevs.ai/docs/advanced/job-management) for cleanup and data retention
- Learn about [Operations](https://docs.docudevs.ai/docs/advanced/operations) for advanced document processing workflows
- Explore [Named Configurations](https://docs.docudevs.ai/docs/configuration/) for consistent processing settings
- Check out [Use Cases](https://docs.docudevs.ai/docs/integration/use-cases) for real-world implementation examples
- Review [Best Practices](https://docs.docudevs.ai/docs/integration/best-practices) for production deployment guidelines
