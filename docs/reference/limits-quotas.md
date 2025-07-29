# Platform Limits and Quotas

Reference for all limits, quotas, and constraints in the DocuDevs platform.

## Document Limits

### File Size Limits

| File Type | Maximum Size | Notes |
|-----------|--------------|-------|
| PDF Documents | 10 MB | Recommended under 5 MB for faster processing |
| DOCX Documents | 10 MB | Text content extracted, images processed separately |
| Text Files | 1 MB | Plain text format |
| Images (JPEG/PNG) | 10 MB | Recommended under 2 MB for faster OCR |
| TIFF Images | 10 MB | Multi-page TIFF supported |

**Error Response for Exceeded Limits:**
```json
{
  "message": "File size exceeds maximum limit of 10MB",
  "status": 413
}
```

### Request Size Limits

| Request Type | Maximum Size | Notes |
|--------------|--------------|-------|
| Single File Upload | 10 MB | Per individual file |
| Multi-File Upload | 15 MB | Total request size including metadata |
| API Request Body | 15 MB | Including all form data and files |

### Supported File Formats

**Supported MIME Types:**
- `application/pdf`
- `application/vnd.openxmlformats-officedocument.wordprocessingml.document` (DOCX)
- `text/plain`
- `image/jpeg`
- `image/png`
- `image/tiff`

**Unsupported formats return:**
```json
{
  "message": "Unsupported file format: application/unknown",
  "status": 415
}
```

## Case Limits

### Documents per Case

| Limit | Value | Scope |
|-------|-------|-------|
| Maximum Documents per Case | 200 | Per individual case |
| Maximum Cases per Organization | Unlimited | No hard limit |
| Maximum Cases per User | Unlimited | No hard limit |

**Error when limit exceeded:**
```json
{
  "message": "Case document limit reached: 200",
  "status": 409
}
```

### Case Metadata Limits

| Field | Maximum Length | Notes |
|-------|----------------|-------|
| Case Name | 255 characters | Required field |
| Case Description | 2000 characters | Optional field |
| Document Filename | 255 characters | Original filename preserved |

## Processing Limits

### Job Processing

| Limit | Value | Notes |
|-------|-------|-------|
| Processing Timeout | 300 seconds (5 minutes) | Per individual job |
| Concurrent Jobs per Organization | 10 | Automatic queuing beyond limit |
| Job Result Retention | 30 days | Results deleted after 30 days |
| Job Status Retention | 90 days | Status history kept for 90 days |

### Instruction Limits

| Parameter | Maximum Length | Notes |
|-----------|----------------|-------|
| Processing Instructions | 4000 characters | LLM prompt instructions |
| Configuration Name | 100 characters | Named configuration identifier |
| Template Instructions | 4000 characters | Template processing instructions |

### OCR Limits

| Limit | Value | Notes |
|-------|-------|-------|
| Pages per Document | 50 pages | PDF and TIFF multi-page support |
| Image Resolution | 4096 x 4096 pixels | Higher resolutions downscaled |
| OCR Processing Timeout | 120 seconds | Per document OCR operation |

## API Limits

### Rate Limiting

| Tier | Requests per Minute | Burst Allowance | Notes |
|------|-------------------|-----------------|-------|
| Free Tier | 60 | 10 | Basic usage limits |
| Paid Tier | 600 | 50 | Higher throughput |
| Enterprise | Custom | Custom | Contact sales |

**Rate Limit Headers:**
```http
X-RateLimit-Limit: 60
X-RateLimit-Remaining: 45
X-RateLimit-Reset: 1704447600
```

**Rate Limit Exceeded Response:**
```json
{
  "message": "Rate limit exceeded. Try again later.",
  "status": 429,
  "retryAfter": 60
}
```

### Authentication Limits

| Limit | Value | Notes |
|-------|-------|-------|
| API Keys per Organization | 10 | Active keys limit |
| Token Expiration | No expiration | Manual revocation required |
| Failed Authentication Attempts | 10 per hour | Temporary IP blocking |

## Storage Limits

### Blob Storage

| Resource | Limit | Retention |
|----------|-------|-----------|
| Uploaded Documents | Unlimited size | 30 days |
| Processing Results | Unlimited size | 30 days |
| Operation Results | Unlimited size | 30 days |
| Template Files | 100 MB per template | Permanent until deleted |

### Database Limits

| Resource | Limit | Notes |
|----------|-------|-------|
| Organizations | Unlimited | No hard limit |
| Users per Organization | Unlimited | No hard limit |
| Jobs per Organization | Unlimited | Auto-cleanup after retention period |
| Configurations per Organization | 100 | Named configurations |
| Templates per Organization | 50 | Template library |

## Operation Limits

### Error Analysis Operations

| Limit | Value | Notes |
|-------|-------|-------|
| Operations per Job | Unlimited | Multiple operations allowed |
| Operation Processing Timeout | 180 seconds | Analysis timeout |
| Operation Result Retention | 30 days | Same as job results |
| Concurrent Operations | 5 per organization | Automatic queuing |

### Operation Parameters

| Parameter | Limit | Notes |
|-----------|-------|-------|
| Custom Parameters Size | 1000 characters | JSON serialized |
| LLM Type | Enum values only | LOW, MEDIUM, HIGH |
| Operation Types | Predefined only | Currently: error-analysis |

## Template Limits

### Template Files

| Limit | Value | Notes |
|-------|-------|-------|
| Template File Size | 5 MB | Per template file |
| Templates per Organization | 50 | Active templates |
| Template Name Length | 100 characters | Unique per organization |

### Template Processing

| Limit | Value | Notes |
|-------|-------|-------|
| Fill Operations per Template | Unlimited | No usage limits |
| Template Processing Timeout | 60 seconds | Fill operation timeout |
| Output Document Size | 20 MB | Generated document limit |

## Configuration Limits

### Named Configurations

| Limit | Value | Notes |
|-------|-------|-------|
| Configurations per Organization | 100 | Named configurations |
| Configuration Name Length | 100 characters | Unique per organization |
| Configuration Data Size | 2000 characters | JSON serialized parameters |

## Network and Connectivity

### Timeout Limits

| Operation | Timeout | Notes |
|-----------|---------|-------|
| API Request | 30 seconds | Standard HTTP timeout |
| File Upload | 120 seconds | Large file upload timeout |
| Processing Job | 300 seconds | Document processing timeout |
| Operation Job | 180 seconds | Post-processing operations |
| Template Fill | 60 seconds | Template generation timeout |

### Connection Limits

| Limit | Value | Notes |
|-------|-------|-------|
| Concurrent Connections | 100 per IP | HTTP connection pooling |
| Request Size | 15 MB | Total request including files |
| Response Size | 50 MB | Maximum response payload |

## Monitoring and Quotas

### Usage Tracking

The platform tracks usage across multiple dimensions:

```json
{
  "organization_id": 123,
  "current_month": {
    "documents_processed": 1250,
    "api_requests": 5678,
    "storage_used_mb": 2048,
    "processing_minutes": 180
  },
  "limits": {
    "monthly_documents": 10000,
    "storage_gb": 10,
    "api_requests_per_day": 10000
  }
}
```

### Quota Warnings

**80% Usage Warning:**
```json
{
  "message": "Approaching monthly document limit: 8000/10000 used",
  "warning_level": "medium"
}
```

**95% Usage Warning:**
```json
{
  "message": "Near monthly document limit: 9500/10000 used",
  "warning_level": "high"
}
```

**Quota Exceeded:**
```json
{
  "message": "Monthly document quota exceeded",
  "status": 403,
  "quota_reset": "2024-02-01T00:00:00Z"
}
```

## Performance Guidelines

### Optimization Recommendations

| Scenario | Recommendation | Impact |
|----------|----------------|--------|
| Large Documents | Split into smaller sections | Faster processing, better accuracy |
| Batch Processing | Use concurrent uploads, sequential processing | Higher throughput |
| High-Volume Usage | Implement client-side rate limiting | Avoid 429 errors |
| Long Documents | Use streaming for results | Reduce memory usage |

### Best Practices

**File Size Optimization:**
```python
# Check file size before upload
import os

file_size = os.path.getsize("document.pdf")
if file_size > 10 * 1024 * 1024:  # 10MB
    print("File too large, consider splitting or compressing")
```

**Rate Limit Handling:**
```python
import time
from http import HTTPStatus

async def upload_with_retry(client, document, max_retries=3):
    for attempt in range(max_retries):
        try:
            response = await client.upload_document(document)
            if response.status_code == HTTPStatus.TOO_MANY_REQUESTS:
                # Wait and retry
                retry_after = int(response.headers.get('Retry-After', 60))
                await asyncio.sleep(retry_after)
                continue
            return response
        except Exception as e:
            if attempt == max_retries - 1:
                raise
            await asyncio.sleep(2 ** attempt)  # Exponential backoff
```

**Case Management:**
```python
# Monitor case document count
case = await client.get_case(case_id)
if case.document_count >= 190:  # Near limit
    print("Case nearly full, consider creating new case")
```

## Error Handling for Limits

### Common Limit Errors

```python
from docudevs_client import DocuDevsClient

client = DocuDevsClient(token="your-api-key")

try:
    response = await client.upload_case_document(
        case_id=case_id,
        document=large_document
    )
except Exception as e:
    error_message = str(e).lower()
    
    if "file size exceeds" in error_message:
        print("Document too large - compress or split file")
    elif "limit reached" in error_message:
        print("Case full - create new case or remove documents")
    elif "rate limit" in error_message:
        print("Rate limited - wait before retrying")
    elif "quota exceeded" in error_message:
        print("Monthly quota exceeded - upgrade plan or wait for reset")
    else:
        print(f"Unexpected error: {e}")
```

### Proactive Limit Checking

```python
# Check file size before upload
def check_file_limits(file_path):
    file_size = os.path.getsize(file_path)
    max_size = 10 * 1024 * 1024  # 10MB
    
    if file_size > max_size:
        raise ValueError(f"File size {file_size} exceeds limit {max_size}")
    
    # Check file type
    mime_type = mimetypes.guess_type(file_path)[0]
    supported_types = [
        'application/pdf',
        'application/vnd.openxmlformats-officedocument.wordprocessingml.document',
        'text/plain',
        'image/jpeg',
        'image/png',
        'image/tiff'
    ]
    
    if mime_type not in supported_types:
        raise ValueError(f"Unsupported file type: {mime_type}")

# Check case capacity before adding documents
async def check_case_capacity(client, case_id, documents_to_add):
    case = await client.get_case(case_id)
    current_count = case.document_count
    
    if current_count + documents_to_add > 200:
        raise ValueError(
            f"Adding {documents_to_add} documents would exceed "
            f"case limit (current: {current_count}, limit: 200)"
        )
```

## Increasing Limits

### Contact Sales

For higher limits contact sales:
- Enterprise processing quotas
- Custom rate limits
- Extended retention periods
- Dedicated infrastructure

### Plan Upgrades

| Plan | Document Limit | Rate Limit | Storage |
|------|----------------|------------|---------|
| Free | 100/month | 60/min | 1 GB |
| Pro | 10,000/month | 600/min | 100 GB |
| Enterprise | Custom | Custom | Custom |

## Monitoring Usage

### API Headers

Monitor usage via response headers:
```http
X-Daily-Usage: 1250
X-Daily-Limit: 10000
X-Monthly-Usage: 5678
X-Monthly-Limit: 100000
```

### Usage API (Future)

Planned API endpoint for usage monitoring:
```http
GET /usage/current
```

Expected response:
```json
{
  "current_month": {
    "documents": 1250,
    "api_calls": 5678,
    "storage_mb": 2048
  },
  "limits": {
    "documents": 10000,
    "api_calls": 100000,
    "storage_gb": 100
  }
}
```