---
title: Error Codes
description: Reference for DocuDevs API error codes, HTTP status codes, and troubleshooting guidance.
sidebar_position: 6
---
<!-- Generated from docs/reference/error-codes.md. Do not edit by hand. -->

# Error Codes Reference

Complete reference for HTTP status codes, error responses, and troubleshooting in the DocuDevs API.

## Quick Reference

| Status Code | Description | Common Cause |
| :--- | :--- | :--- |
| **200** | OK | Successful request |
| **201** | Created | Resource created successfully |
| **400** | Bad Request | Invalid parameters or malformed data |
| **401** | Unauthorized | Missing or invalid API key |
| **403** | Forbidden | Insufficient permissions or quota exceeded |
| **404** | Not Found | Resource does not exist |
| **409** | Conflict | Resource already exists or state conflict |
| **413** | Payload Too Large | File size exceeds limit (10MB) |
| **415** | Unsupported Media Type | File format not supported |
| **422** | Unprocessable Entity | Valid request but semantic error (e.g. corrupted file) |
| **429** | Too Many Requests | Rate limit exceeded |
| **500** | Internal Server Error | Unexpected server error |
| **503** | Service Unavailable | System maintenance or overload |

## HTTP Status Codes

### Success Codes (2xx)

#### 200 OK

Successful request with response data.

**Used by:**

- GET requests that return data
- POST requests that complete successfully
- Processing operations that finish

#### 201 Created

Resource successfully created.

**Used by:**

- Creating new cases
- Uploading documents
- Saving configurations

### Client Error Codes (4xx)

#### 400 Bad Request

Invalid request parameters or malformed data.

**Common Causes:**

- Missing required fields
- Invalid parameter values
- Malformed JSON
- Unsupported file formats
- Invalid operation types

Example response:

```json
{
  "message": "Invalid operation type: invalid-operation",
  "status": 400
}
```

#### 401 Unauthorized

Authentication failed or missing.

**Common Causes:**

- Missing API key
- Invalid API key
- Expired token
- Malformed Authorization header

**cURL**

```bash
HTTP_STATUS=$(curl -s -S -o /dev/null -w "%{http_code}" "https://api.docudevs.ai/cases" \
  -H "Authorization: invalid-key")

if [ "$HTTP_STATUS" = "401" ]; then
  echo "Authentication failed (401)"
fi
```

**Python**

```python
import asyncio

from docudevs.docudevs_client import DocuDevsClient


async def main():
    client = DocuDevsClient(token="invalid-key")

    response = await client.list_cases()
    if response.status_code == 401:
        print("Authentication failed (401)")


asyncio.run(main())
```

#### 403 Forbidden

Valid authentication but insufficient permissions.

**Common Causes:**

- API key doesn't have required permissions
- Accessing resources from different organization
- Quota or limit exceeded

#### 404 Not Found

Requested resource doesn't exist.

**Common Causes:**

- Invalid GUID
- Job not found
- Case not found
- Configuration not found
- Template not found
- Resource belongs to different organization

**cURL**

```bash
HTTP_STATUS=$(curl -s -S -o /dev/null -w "%{http_code}" "https://api.docudevs.ai/cases/99999" \
  -H "Authorization: $API_KEY")

if [ "$HTTP_STATUS" = "404" ]; then
  echo "Case not found"
fi
```

**Python**

```python
response = await client.get_case(case_id=99999)
if response.status_code == 404:
    print("Case not found")
```

#### 409 Conflict

Resource conflict or state mismatch.

**Common Causes:**

- Configuration name already exists
- Template name already exists
- Document limit exceeded
- Concurrent modification
- Batch job not found or not a batch job (when accessing batch endpoints)

**Specific Cases:**

- **Case Document Limit:** "Case document limit reached: 200"
- **Duplicate Configuration:** "Configuration name already exists"

#### 413 Payload Too Large

Request body or file too large.

**Limits:**

- Maximum document size: 10 MB
- Maximum request size: 15 MB

#### 415 Unsupported Media Type

Unsupported file format or content type.

**Supported Formats:**

- `application/pdf`
- `application/vnd.openxmlformats-officedocument.wordprocessingml.document` (DOCX)
- `text/plain`
- `image/jpeg`
- `image/png`
- `image/tiff`

#### 422 Unprocessable Entity

Valid request format but semantic errors.

**Common Causes:**

- Invalid file content
- Corrupted document
- Missing document content
- Invalid schema format

### Server Error Codes (5xx)

#### 500 Internal Server Error

Unexpected server error. Please contact support if this persists.

#### 503 Service Unavailable

Service temporarily unavailable due to maintenance or high load.

## Error Response Format

All error responses follow a consistent format:

```json
{
  "message": "Human-readable error description",
  "status": 400,
  "timestamp": "2024-01-15T10:30:00Z",
  "path": "/api/endpoint",
  "error": "BadRequest"
}
```

## Job Status Errors

Jobs can have error states in their status, separate from HTTP errors.

```json
{
  "guid": "job-guid",
  "status": "FAILED",
  "error": "Processing failed: Document format not supported",
  "createdAt": "2024-01-15T10:30:00Z",
  "updatedAt": "2024-01-15T10:35:00Z"
}
```

**Common Job Errors:**

- `"Processing failed: Document format not supported"`
- `"Processing failed: Document appears to be corrupted"`
- `"Processing failed: Text extraction failed"`
- `"Processing failed: AI service unavailable"`
- `"Processing timeout after 300 seconds"`

## SDK Error Handling

The Python SDK methods that wrap a single API call return the HTTP response instead of raising on an error status, so check `status_code` before you use the response:

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

The helpers that submit a document or wait for a job raise instead: `wait_until_ready` raises `TimeoutError` when the job does not finish in time, and an `Exception` carrying the error message when the job fails.

**cURL**

```bash
if [ ! -f sample_invoice.pdf ]; then
  echo "Document file not found: sample_invoice.pdf" >&2
  exit 1
fi

JOB_ID=$(curl -s -S -f -X POST "https://api.docudevs.ai/document/upload" \
  -H "Authorization: $API_KEY" \
  -F "document=@sample_invoice.pdf" | jq -r '.guid')

curl -s -S -f -o /dev/null -X POST "https://api.docudevs.ai/document/process/$JOB_ID" \
  -H "Authorization: $API_KEY" \
  -H "Content-Type: application/json" \
  -d '{
    "mimeType": "application/pdf",
    "prompt": "Extract key information from this document"
  }'
echo "Processing job $JOB_ID..."

# Wait up to 5 minutes for the result
DEADLINE=$((SECONDS + 300))
while true; do
  STATUS=$(curl -s -S -f "https://api.docudevs.ai/job/status/$JOB_ID" \
    -H "Authorization: $API_KEY" | jq -r '.status')
  if [ "$STATUS" = "COMPLETED" ]; then
    echo "Processing successful!"
    curl -s -S -f "https://api.docudevs.ai/job/result/$JOB_ID/json" \
      -H "Authorization: $API_KEY" | jq .
    break
  fi
  if [ "$STATUS" = "ERROR" ] || [ "$STATUS" = "FAILED" ] || [ "$STATUS" = "TIMEOUT" ]; then
    echo "Processing failed for job $JOB_ID with status $STATUS" >&2
    exit 1
  fi
  if [ "$SECONDS" -ge "$DEADLINE" ]; then
    echo "Processing timed out for job $JOB_ID" >&2
    echo "Current status: $STATUS" >&2
    exit 1
  fi
  sleep 5
done
```

**Python**

```python
try:
    with open("sample_invoice.pdf", "rb") as f:
        document_data = f.read()
except FileNotFoundError:
    print("Document file not found: sample_invoice.pdf")
    return

job_id = await client.submit_and_process_document(
    document=document_data,
    document_mime_type="application/pdf",
    prompt="Extract key information from this document",
)
print(f"Processing job {job_id}...")

try:
    # Wait up to 5 minutes for the result
    result = await client.wait_until_ready(job_id, timeout=300, result_format="json")
    print("Processing successful!")
    print(result)
except TimeoutError:
    status_response = await client.status(job_id)
    print(f"Processing timed out for job {job_id}")
    print(f"Current status: {status_response.parsed.status}")
except Exception as error:
    print(f"Processing failed: {error}")
```

## CLI Error Handling

The CLI writes error messages to stderr. Some commands report a failed API call with an error message and still exit with status `0`, so a script that must detect failures should check stderr, or call the HTTP API and check the status code.

## Rate Limiting

**Headers:**

- `X-RateLimit-Limit`: Requests per time window
- `X-RateLimit-Remaining`: Remaining requests
- `X-RateLimit-Reset`: Reset time (Unix timestamp)

**Response when rate limited:**

```json
{
  "message": "Rate limit exceeded. Try again later.",
  "status": 429,
  "retryAfter": 60
}
```

## Troubleshooting

### Authentication Issues

- **401 Unauthorized**: Check your API key. Ensure the `API_KEY` environment variable is set to a valid key (the CLI also accepts `--token`).

### File Upload Issues

- **413 Payload Too Large**: Compress your PDF or split it into smaller files.
- **415 Unsupported Media Type**: Convert your file to PDF or a supported image format.

### Job Processing Issues

- **Job FAILED**: Check the `error` field in the job status. If the document is password protected, remove the password and try again.

### Resource Not Found

- **404 Not Found**: Verify the GUID/ID. Ensure you are using the correct API key for the organization that owns the resource.
