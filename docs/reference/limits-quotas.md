# Platform Limits and Quotas

Reference for all limits, quotas, and constraints in the DocuDevs platform.

## Document Limits

### File Size Limits

| Maximum Size | Notes |
|--------------|-------|
| **50 MB** | Maximum file size for uploads |

**Error Response for Exceeded Limits:**
```json
{
  "message": "File size exceeds maximum limit of 50MB",
  "status": 413
}
```

### Page Limits

| Document Type | Maximum Pages |
|---------------|---------------|
| PDF Documents | 2,000 pages |
| TIFF Documents | 2,000 pages |

### Supported File Formats

**Supported file types:**
- PDF documents (`.pdf`)
- Word documents (`.docx`)
- Plain text files (`.txt`)
- Images (`.jpg`, `.jpeg`, `.png`, `.tiff`, `.bmp`, `.heif`)

**Unsupported formats return:**
```json
{
  "message": "Unsupported file format",
  "status": 415
}
```

### Image Requirements

| Constraint | Value |
|------------|-------|
| Minimum Image Size | 50 x 50 pixels |
| Maximum Image Size | 10,000 x 10,000 pixels |
| Minimum Text Height | 12 pixels |

## Case Limits

### Documents per Case

| Limit | Value |
|-------|-------|
| **Maximum Documents per Case** | **200** |

**Error when limit exceeded:**
```json
{
  "message": "Case document limit reached: 200",
  "status": 409
}
```

## Error Handling

### File Size Validation

```python
import os

def check_file_size(file_path):
    file_size = os.path.getsize(file_path)
    max_size = 50 * 1024 * 1024  # 50MB
    
    if file_size > max_size:
        raise ValueError(f"File size {file_size} exceeds limit {max_size}")
```

### Case Document Count Validation

```python
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

## Best Practices

### File Size Optimization

For best performance:
- Keep files under 10 MB when possible
- Compress large PDFs before uploading
- Use appropriate image formats (JPEG for photos, PNG for documents)

### Case Management

- Monitor document count as you approach the 200 document limit
- Create new cases when approaching capacity
- Use descriptive case names to organize your documents effectively