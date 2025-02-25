# DocuDevs Python SDK

The DocuDevs Python SDK provides a convenient way to interact with the DocuDevs API from your Python applications.

## Installation

You can install the DocuDevs Python client using pip:

```bash
pip install docu-devs-api-client
```

## Authentication

To use the SDK, you'll need an API key from your DocuDevs account:

```python
from docudevs.docudevs_client import DocuDevsClient

# Initialize the client with your API key
client = DocuDevsClient(token="your_api_key_here")
```

## Basic Usage

### Listing Documents

```python
# Get all documents
documents = client.get_documents()
print(f"Found {len(documents)} documents")

# Get documents with filtering
filtered_docs = client.get_documents(filter={"status": "active"})
```

### Creating a Document

```python
new_document = client.create_document({
    "title": "Sample Contract",
    "template_id": "template_123",
    "content": {
        "customer_name": "Acme Inc.",
        "effective_date": "2023-01-01"
    }
})
print(f"Created document with ID: {new_document['id']}")
```

### Retrieving a Document

```python
document = client.get_document(document_id="doc_123")
print(f"Document title: {document['title']}")
```

### Updating a Document

```python
updated_document = client.update_document(
    document_id="doc_123",
    data={
        "status": "signed",
        "content": {
            "customer_name": "Acme Corporation"
        }
    }
)
```

### Deleting a Document

```python
client.delete_document(document_id="doc_123")
print("Document deleted successfully")
```

## Error Handling

The SDK will raise exceptions for API errors. You can handle these exceptions to provide a better user experience:

```python
from docudevs.exceptions import DocuDevsApiError

try:
    document = client.get_document(document_id="invalid_id")
except DocuDevsApiError as e:
    print(f"API Error: {e.message}")
    print(f"Status Code: {e.status_code}")
```

## Advanced Usage

For advanced use cases, you can configure the client behavior:

```python
client = DocuDevsClient(
    token="your_api_key_here",
    base_url="https://api.custom-domain.docudevs.com/v1",
    timeout=30,  # 30 second timeout
    retry_attempts=3
)
```

For more information, please refer to the [official API documentation](https://docs.docudevs.com/api).
