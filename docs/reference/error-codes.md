# Error Codes Reference

Complete reference for HTTP status codes and error responses in the DocuDevs API.

## HTTP Status Codes

### Success Codes (2xx)

#### 200 OK
Successful request with response data.

**Used by:**
- GET requests that return data
- POST requests that complete successfully
- Processing operations that finish

**Example Response:**
```json
{
  "guid": "uuid-string",
  "status": "COMPLETED",
  "result": {...}
}
```

#### 201 Created
Resource successfully created.

**Used by:**
- Creating new cases
- Uploading documents
- Saving configurations

**Example Response:**
```json
{
  "id": 123,
  "name": "New Case",
  "createdAt": "2024-01-15T10:30:00Z"
}
```

### Client Error Codes (4xx)

#### 400 Bad Request
Invalid request parameters or malformed data.

**Common Causes:**
- Missing required fields
- Invalid parameter values
- Malformed JSON
- Unsupported file formats
- Invalid operation types

**Example Response:**
```json
{
  "message": "Invalid operation type: invalid-operation",
  "status": 400
}
```

**Specific Cases:**

**Invalid Operation Type:**
```bash
POST /operation
{
  "jobGuid": "valid-guid",
  "type": "invalid-operation"  # Unknown operation type
}
# Returns: 400 Bad Request
```

**Parent Job Not Completed:**
```bash
POST /operation
{
  "jobGuid": "pending-job-guid",  # Job still processing
  "type": "error-analysis"
}
# Returns: 400 Bad Request
```

**Missing Required Fields:**
```bash
POST /cases
{
  "description": "Missing name field"
}
# Returns: 400 Bad Request
```

#### 401 Unauthorized
Authentication failed or missing.

**Common Causes:**
- Missing API key
- Invalid API key
- Expired token
- Malformed Authorization header

**Example Response:**
```json
{
  "message": "Unauthorized",
  "status": 401
}
```

**Authentication Examples:**
```bash
# Missing Authorization header
GET /cases
# Returns: 401 Unauthorized

# Invalid API key
GET /cases
Authorization: Bearer invalid-key
# Returns: 401 Unauthorized
```

#### 403 Forbidden
Valid authentication but insufficient permissions.

**Common Causes:**
- API key doesn't have required permissions
- Accessing resources from different organization
- Quota or limit exceeded

**Example Response:**
```json
{
  "message": "Access denied to organization resources",
  "status": 403
}
```

#### 404 Not Found
Requested resource doesn't exist.

**Common Causes:**
- Invalid GUID
- Job not found
- Case not found
- Configuration not found
- Template not found
- Resource belongs to different organization

**Example Response:**
```json
{
  "message": "Resource not found",
  "status": 404
}
```

**Specific Cases:**

**Job Not Found:**
```bash
GET /job/status/invalid-guid
# Returns: 404 Not Found
```

**Case Not Found:**
```bash
GET /cases/99999
# Returns: 404 Not Found
```

**Configuration Not Found:**
```bash
GET /configuration/nonexistent-config
# Returns: 404 Not Found
```

**Operation Not Found:**
```bash
GET /operation/invalid-guid/error-analysis
# Returns: 404 Not Found
```

#### 409 Conflict
Resource conflict or state mismatch.

**Common Causes:**
- Configuration name already exists
- Template name already exists
- Document limit exceeded
- Concurrent modification

**Example Response:**
```json
{
  "message": "Configuration name already exists",
  "status": 409
}
```

**Specific Cases:**

**Case Document Limit:**
```bash
POST /cases/123/documents
# When case already has 200 documents
# Returns: 409 Conflict - "Case document limit reached: 200"
```

**Duplicate Configuration:**
```bash
POST /configuration/existing-name
# Returns: 409 Conflict
```

#### 413 Payload Too Large
Request body or file too large.

**Common Causes:**
- Document exceeds size limits
- Request body too large

**Limits:**
- Maximum document size: 10 MB
- Maximum request size: 15 MB

**Example Response:**
```json
{
  "message": "File size exceeds maximum limit of 10MB",
  "status": 413
}
```

#### 415 Unsupported Media Type
Unsupported file format or content type.

**Supported Formats:**
- `application/pdf`
- `application/vnd.openxmlformats-officedocument.wordprocessingml.document` (DOCX)
- `text/plain`
- `image/jpeg`
- `image/png`
- `image/tiff`

**Example Response:**
```json
{
  "message": "Unsupported file format: application/unknown",
  "status": 415
}
```

#### 422 Unprocessable Entity
Valid request format but semantic errors.

**Common Causes:**
- Invalid file content
- Corrupted document
- Missing document content
- Invalid schema format

**Example Response:**
```json
{
  "message": "Document appears to be corrupted or unreadable",
  "status": 422
}
```

### Server Error Codes (5xx)

#### 500 Internal Server Error
Unexpected server error.

**Common Causes:**
- Database connection issues
- Storage service unavailable
- Unexpected application errors

**Example Response:**
```json
{
  "message": "Internal server error",
  "status": 500
}
```

#### 502 Bad Gateway
Upstream service unavailable.

**Common Causes:**
- AI processing service down
- Storage service unavailable
- External API failures

#### 503 Service Unavailable
Service temporarily unavailable.

**Common Causes:**
- Maintenance mode
- System overload
- Rate limiting

#### 504 Gateway Timeout
Request timeout from upstream services.

**Common Causes:**
- Long-running AI processing
- Storage operation timeout
- Network connectivity issues

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

Jobs can have error states in their status:

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

## Operation Errors

Operations can fail with specific error messages:

```json
{
  "jobGuid": "operation-guid",
  "operationType": "error-analysis",
  "status": "FAILED",
  "error": "Parent job result not available for analysis",
  "createdAt": "2024-01-15T10:30:00Z",
  "updatedAt": "2024-01-15T10:32:00Z"
}
```

## SDK Error Handling

### Python SDK Examples

```python
from http import HTTPStatus
from docudevs_client import DocuDevsClient

client = DocuDevsClient(token="your-api-key")

try:
    response = await client.upload_and_process(
        document=document_data,
        document_mime_type="application/pdf"
    )
    
    # Check HTTP status
    if response.status_code == HTTPStatus.UNAUTHORIZED:
        print("Invalid API key")
    elif response.status_code == HTTPStatus.NOT_FOUND:
        print("Resource not found")
    elif response.status_code == HTTPStatus.BAD_REQUEST:
        print(f"Bad request: {response.content}")
    elif response.status_code != HTTPStatus.OK:
        print(f"Error {response.status_code}: {response.content}")
    else:
        # Success - process result
        result = await client.wait_until_ready(response.parsed.guid)
        
except Exception as e:
    print(f"Request failed: {e}")
```

### Handling Specific Errors

```python
# Handle case document limit
try:
    response = await client.upload_case_document(
        case_id=case_id,
        document=document_data,
        document_mime_type="application/pdf"
    )
except Exception as e:
    if "limit reached" in str(e):
        print("Case is full - cannot add more documents")
    else:
        print(f"Upload failed: {e}")

# Handle operation errors
try:
    analysis = await client.submit_and_wait_for_error_analysis(job_guid)
except TimeoutError:
    print("Analysis timed out - try again later")
except Exception as e:
    if "not found" in str(e).lower():
        print("Job not found or not accessible")
    elif "not completed" in str(e).lower():
        print("Job must be completed before running analysis")
    else:
        print(f"Analysis failed: {e}")
```

## CLI Error Handling

The CLI returns appropriate exit codes:

- `0`: Success
- `1`: General error
- `2`: Authentication error
- `3`: Not found error
- `4`: Permission error
- `5`: Server error

```bash
# Check exit code
docudevs process document.pdf
if [ $? -eq 2 ]; then
    echo "Authentication failed - check API key"
elif [ $? -eq 4 ]; then
    echo "Permission denied"
fi
```

## Rate Limiting

While not strictly error codes, rate limiting affects API usage:

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

## Troubleshooting Common Errors

### Authentication Issues

**Error:** 401 Unauthorized
**Solutions:**
1. Verify API key is correct
2. Check Authorization header format: `Bearer YOUR_API_KEY`
3. Ensure API key hasn't expired
4. Verify API key has required permissions

### File Upload Issues

**Error:** 413 Payload Too Large
**Solutions:**
1. Reduce file size (max 10MB)
2. Compress images before upload
3. Split large documents

**Error:** 415 Unsupported Media Type
**Solutions:**
1. Convert to supported format (PDF, DOCX, TXT, images)
2. Verify MIME type is correct
3. Check file isn't corrupted

### Job Processing Issues

**Error:** Job status "FAILED"
**Solutions:**
1. Check job error message for specific cause
2. Verify document is readable/not corrupted
3. Try with different processing parameters
4. Contact support if error persists

### Resource Not Found

**Error:** 404 Not Found
**Solutions:**
1. Verify GUID/ID is correct
2. Check resource belongs to your organization
3. Ensure resource hasn't been deleted
4. Wait for job completion before accessing results

### Operation Errors

**Error:** 400 Bad Request on operation submission
**Solutions:**
1. Verify parent job is completed
2. Check operation type spelling
3. Ensure valid parameters
4. Verify job belongs to your organization

## Getting Help

When encountering persistent errors:

1. **Check API status page** for service outages
2. **Review error message details** for specific causes
3. **Verify request format** against API documentation
4. **Test with minimal examples** to isolate issues
5. **Contact support** with error details and request IDs

For support requests, include:
- Error message and status code
- Request details (endpoint, parameters)
- Timestamp of the error
- Job GUID (if applicable)
- Steps to reproduce