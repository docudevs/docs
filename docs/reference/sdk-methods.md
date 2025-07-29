# SDK Method Reference

Complete reference for all DocuDevs Python SDK methods.

## Client Initialization

### DocuDevsClient

```python
from docudevs_client import DocuDevsClient

client = DocuDevsClient(
    api_url="https://api.docudevs.ai",  # Optional, defaults to production
    token="your-api-key"                # Required
)
```

## Configuration Management

### list_configurations()

List all saved configurations.

```python
configurations = await client.list_configurations()
```

**Returns:** List of configuration objects

### get_configuration(name)

Get a specific configuration by name.

```python
config = await client.get_configuration("my-config")
```

**Parameters:**
- `name` (str): Configuration name

**Returns:** Configuration object

### save_configuration(name, body)

Save a new configuration.

```python
from docudevs.models import UploadCommand

body = UploadCommand(
    instructions="Extract invoice data",
    llm="gpt-4"
)

response = await client.save_configuration("invoice-config", body)
```

**Parameters:**
- `name` (str): Configuration name
- `body` (UploadCommand): Configuration details

### delete_configuration(name)

Delete a configuration.

```python
response = await client.delete_configuration("old-config")
```

## Document Upload and Processing

### upload_files(body)

Upload files with processing parameters.

```python
from docudevs.models import UploadFilesBody
from docudevs.types import File

document_file = File(
    payload=document_bytes,
    file_name="document.pdf",
    mime_type="application/pdf"
)

body = UploadFilesBody(
    files=[document_file],
    instructions="Extract key information"
)

response = await client.upload_files(body)
```

### upload_document(body)

Upload a single document.

```python
from docudevs.models import UploadDocumentBody

body = UploadDocumentBody(document=document_file)

response = await client.upload_document(body)
```

### upload_and_process()

Convenience method to upload and process a document in one call.

```python
response = await client.upload_and_process(
    document=document_bytes,
    document_mime_type="application/pdf",
    instructions="Extract invoice data",
    llm="gpt-4"
)
```

**Parameters:**
- `document` (bytes): Document content
- `document_mime_type` (str): MIME type
- `instructions` (str, optional): Processing instructions
- `llm` (str, optional): LLM to use
- `ocr` (str, optional): OCR type
- `ocr_format` (str, optional): OCR output format

### upload_and_process_ocr_only()

Upload and process with OCR only (no LLM extraction).

```python
guid = await client.upload_and_process_ocr_only(
    document=document_bytes,
    document_mime_type="application/pdf",
    ocr="advanced",
    ocr_format="markdown"
)
```

## Job Management

### status(uuid)

Get job status.

```python
status_response = await client.status(uuid="job-guid")
```

### result(uuid)

Get job result.

```python
result_response = await client.result(uuid="job-guid")
```

### wait_until_ready()

Wait for a job to complete and return the result.

```python
result = await client.wait_until_ready(
    guid="job-guid",
    timeout=60,
    poll_interval=1.0
)
```

**Parameters:**
- `guid` (str): Job GUID
- `timeout` (int): Maximum wait time in seconds
- `poll_interval` (float): Polling interval in seconds

## Cases Management

### create_case()

Create a new document case.

```python
# Create a new document case
case = await client.create_case(
    name="Invoice Processing Q1",
    description="Q1 2024 invoices"
)
```

**Parameters:**
- `name` (str): Case name
- `description` (str, optional): Case description

### list_cases()

List all cases.

```python
cases = await client.list_cases()
```

### get_case(case_id)

Get case details.

```python
case = await client.get_case(case_id=123)
```

### upload_case_document()

Upload a document to a case.

```python
response = await client.upload_case_document(
    case_id=123,
    document=document_bytes,
    document_mime_type="application/pdf",
    filename="invoice.pdf"
)
```

## Operations

### submit_operation()

Submit an operation for a completed job.

```python
response = await client.submit_operation(
    job_guid="completed-job-guid",
    operation_type="error-analysis"
)
```

### submit_operation_with_parameters()

Submit operation with custom parameters.

```python
response = await client.submit_operation_with_parameters(
    job_guid="job-guid",
    operation_type="error-analysis",
    llm_type="HIGH",
    custom_parameters={"focus": "numerical_data"}
)
```

### get_operation_status()

Get status of operations for a job.

```python
status = await client.get_operation_status(job_guid="job-guid")
```

### get_operation_result()

Get result of a specific operation.

```python
result = await client.get_operation_result(
    job_guid="job-guid",
    operation_type="error-analysis"
)
```

### submit_and_wait_for_operation()

Submit operation and wait for completion.

```python
result = await client.submit_and_wait_for_operation(
    job_guid="job-guid",
    operation_type="error-analysis",
    timeout=120,
    poll_interval=2.0
)
```

### submit_and_wait_for_error_analysis()

Convenience method for error analysis.

```python
analysis = await client.submit_and_wait_for_error_analysis(
    job_guid="job-guid",
    timeout=120
)
```

## Templates

### list_templates()

List all templates.

```python
templates = await client.list_templates()
```

### upload_template()

Upload a new template.

```python
response = await client.upload_template(
    name="invoice-template",
    template_file=template_bytes,
    instructions="Extract invoice data"
)
```

### metadata(template_id)

Get template metadata.

```python
metadata = await client.metadata(template_id="template-name")
```

### fill_template()

Fill a template with data.

```python
from docudevs.models import TemplateFillRequest

request = TemplateFillRequest(
    document_data={"name": "John Doe", "amount": 1000}
)

response = await client.fill_template(
    template_id="template-name",
    request=request
)
```

### delete_template()

Delete a template.

```python
response = await client.delete_template(name="old-template")
```

## Schema Generation

### generate_schema()

Generate JSON schema from a document.

```python
response = await client.generate_schema(
    document=document_bytes,
    document_mime_type="application/pdf",
    instructions="Focus on financial data"
)
```

## Processing Methods

### process_document()

Process an uploaded document.

```python
response = await client.process_document(
    guid="uploaded-doc-guid",
    instructions="Extract all data",
    llm="gpt-4"
)
```

### process_document_with_configuration()

Process using a saved configuration.

```python
response = await client.process_document_with_configuration(
    guid="uploaded-doc-guid",
    configuration_name="invoice-config"
)
```

### ocr_document()

Process document with OCR only.

```python
from docudevs.models import OcrCommand

ocr_body = OcrCommand(
    ocr="advanced",
    ocr_format="markdown",
    mime_type="application/pdf"
)

response = await client.ocr_document(
    guid="uploaded-doc-guid",
    body=ocr_body
)
```

## Error Handling

All SDK methods can raise exceptions. Common patterns:

```python
from http import HTTPStatus

try:
    response = await client.upload_and_process(
        document=document_bytes,
        document_mime_type="application/pdf"
    )
    
    if response.status_code != HTTPStatus.OK:
        print(f"Error: {response.content}")
        return
        
    result = await client.wait_until_ready(response.parsed.guid)
    
except TimeoutError as e:
    print(f"Processing timed out: {e}")
except Exception as e:
    print(f"Processing failed: {e}")
```

## Response Objects

### Standard Response Structure

Most methods return response objects with:

```python
response.status_code  # HTTP status code
response.content      # Raw response content
response.parsed       # Parsed response object (if successful)
```

### Common Response Types

**UploadResponse:**
```python
response.parsed.guid    # Job GUID
response.parsed.status  # Job status
```

**Job Status:**
```python
status.status          # PENDING, PROCESSING, COMPLETED, FAILED
status.created_at      # Creation timestamp
status.updated_at      # Last update timestamp
```

**Job Result:**
```python
result.result          # Extracted data (JSON string)
result.metadata        # Processing metadata
```

## Constants and Enums

### LLM Types
- `"gpt-3.5-turbo"`
- `"gpt-4"`
- `"gpt-4-turbo"`

### OCR Types
- `"basic"`
- `"advanced"`

### OCR Formats
- `"plain"`
- `"markdown"`
- `"json"`

### Operation Types
- `"error-analysis"`

## Best Practices

### Authentication
```python
import os

# Use environment variables for API keys
client = DocuDevsClient(
    token=os.getenv("DOCUDEVS_API_KEY")
)
```

### Error Handling
```python
async def safe_process_document(document_data, mime_type):
    try:
        response = await client.upload_and_process(
            document=document_data,
            document_mime_type=mime_type
        )
        
        if response.status_code != 200:
            return None, f"Upload failed: {response.content}"
            
        result = await client.wait_until_ready(
            response.parsed.guid,
            timeout=120
        )
        
        return result, None
        
    except Exception as e:
        return None, str(e)
```

### Batch Processing
```python
async def process_documents_batch(documents):
    tasks = []
    for doc_data, mime_type in documents:
        task = client.upload_and_process(
            document=doc_data,
            document_mime_type=mime_type
        )
        tasks.append(task)
    
    # Upload all documents concurrently
    responses = await asyncio.gather(*tasks)
    
    # Wait for all to complete
    results = []
    for response in responses:
        if response.status_code == 200:
            result = await client.wait_until_ready(response.parsed.guid)
            results.append(result)
    
    return results
```