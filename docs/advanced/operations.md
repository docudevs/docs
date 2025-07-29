# Operations

Perform post-processing operations on completed document jobs for error analysis and workflow optimization.

## Overview

Operations allow you to run additional analysis on documents that have already been processed. Currently, the main operation available is **Error Analysis**, which helps identify potential issues in extraction results and provides suggestions for improvement.

## Available Operations

### Error Analysis

Analyzes completed document processing jobs to identify:
- Potential extraction errors or inconsistencies
- Missing or incomplete data fields
- Confidence levels for extracted information
- Suggestions for improving accuracy

## How Operations Work

1. **Document Processing**: First, process a document normally
2. **Operation Submission**: Submit an operation request on the completed job
3. **Analysis**: AI analyzes the original processing results
4. **Results**: Get detailed analysis and recommendations

## API Endpoints

### Submit Operation

```http
POST /operation
```

**Request Body:**
```json
{
  "jobGuid": "uuid-of-completed-job",
  "type": "error-analysis",
  "parameters": {
    "llmType": "HIGH",
    "customParameters": {}
  }
}
```

**Response:**
```json
{
  "jobGuid": "new-operation-job-uuid",
  "operationType": "error-analysis",
  "status": "PENDING",
  "message": "Error analysis operation scheduled"
}
```

### Get Operation Status

```http
GET /operation/{parentJobGuid}
```

**Response:**
```json
{
  "parentJobGuid": "original-job-uuid",
  "operations": [
    {
      "jobGuid": "operation-job-uuid",
      "operationType": "error-analysis",
      "status": "COMPLETED",
      "createdAt": "2024-01-15T10:30:00Z",
      "updatedAt": "2024-01-15T10:32:15Z",
      "error": null
    }
  ],
  "totalOperations": 1
}
```

### Get Operation Result

```http
GET /operation/{parentJobGuid}/{operationType}
```

**Response:**
```json
{
  "jobGuid": "operation-job-uuid",
  "parentJobGuid": "original-job-uuid",
  "operationType": "error-analysis",
  "status": "COMPLETED",
  "createdAt": "2024-01-15T10:30:00Z",
  "updatedAt": "2024-01-15T10:32:15Z",
  "error": null,
  "resultAvailable": true,
  "result": "{\"analysis\": {...}, \"recommendations\": [...]}"
}
```

## SDK Examples

### Python

```python
from docudevs_client import DocuDevsClient

client = DocuDevsClient(token="your-api-key")

# Process a document first
response = await client.upload_and_process(
    document=document_data,
    document_mime_type="application/pdf",
    instructions="Extract invoice data including line items and totals"
)

# Wait for processing to complete
job_guid = response.parsed.guid
result = await client.wait_until_ready(job_guid)

# Submit error analysis operation
operation_response = await client.submit_operation(
    job_guid=job_guid,
    operation_type="error-analysis"
)

# Wait for operation to complete and get result
analysis_result = await client.submit_and_wait_for_error_analysis(
    job_guid=job_guid,
    timeout=120
)

print(f"Analysis result: {analysis_result}")
```

### Advanced Usage with Parameters

```python
# Submit operation with custom LLM and parameters
await client.submit_operation_with_parameters(
    job_guid=job_guid,
    operation_type="error-analysis",
    llm_type="HIGH",  # Use high-quality LLM for analysis
    custom_parameters={
        "focus_areas": ["numerical_accuracy", "date_formats"],
        "confidence_threshold": 0.8,
        "detailed_suggestions": True
    }
)

# Use convenience method with parameters
analysis_result = await client.submit_and_wait_for_operation_with_parameters(
    job_guid=job_guid,
    operation_type="error-analysis",
    llm_type="HIGH",
    custom_parameters={
        "analysis_depth": "comprehensive"
    },
    timeout=180
)
```

### CLI Usage

```bash
# Submit error analysis operation
docudevs operation submit $JOB_GUID error-analysis

# Check operation status
docudevs operation status $JOB_GUID

# Get operation result
docudevs operation result $JOB_GUID error-analysis
```

### cURL Examples

```bash
# Submit operation
curl -X POST "https://api.docudevs.ai/operation" \
  -H "Authorization: Bearer YOUR_API_KEY" \
  -H "Content-Type: application/json" \
  -d '{
    "jobGuid": "your-job-guid",
    "type": "error-analysis",
    "parameters": {
      "llmType": "HIGH"
    }
  }'

# Get operation status
curl -X GET "https://api.docudevs.ai/operation/your-job-guid" \
  -H "Authorization: Bearer YOUR_API_KEY"

# Get operation result
curl -X GET "https://api.docudevs.ai/operation/your-job-guid/error-analysis" \
  -H "Authorization: Bearer YOUR_API_KEY"
```

## Error Analysis Output

Error analysis operations return detailed JSON with:

```json
{
  "analysis": {
    "overall_confidence": 0.85,
    "field_confidence": {
      "invoice_number": 0.95,
      "total_amount": 0.90,
      "line_items": 0.75
    },
    "potential_issues": [
      {
        "field": "line_items",
        "issue": "Some quantities may be misread",
        "confidence": 0.75,
        "suggestion": "Review items with quantities > 100"
      }
    ]
  },
  "recommendations": [
    {
      "type": "processing_improvement",
      "description": "Consider using higher quality OCR settings",
      "impact": "Could improve numerical accuracy by 5-10%"
    },
    {
      "type": "instruction_refinement",
      "description": "Add specific format requirements for monetary values",
      "example": "Extract amounts as numbers without currency symbols"
    }
  ],
  "quality_metrics": {
    "text_clarity": 0.88,
    "structure_detection": 0.92,
    "data_completeness": 0.80
  }
}
```

## Operation Parameters

### LLM Types

- `LOW`: Faster, cost-effective analysis
- `MEDIUM`: Balanced speed and quality
- `HIGH`: Thorough, detailed analysis

### Custom Parameters

Error analysis supports various custom parameters:

```json
{
  "focus_areas": ["numerical_accuracy", "date_formats", "text_extraction"],
  "confidence_threshold": 0.8,
  "detailed_suggestions": true,
  "include_alternatives": true,
  "analysis_depth": "comprehensive"
}
```

## Best Practices

### When to Use Operations

- **Quality Assurance**: Run error analysis on critical documents
- **Process Improvement**: Analyze batches to identify patterns
- **Template Refinement**: Use insights to improve extraction instructions
- **Debugging**: Investigate unexpected results

### Operation Workflow

1. **Process documents normally first**
2. **Run error analysis on representative samples**
3. **Review analysis results and recommendations**
4. **Adjust instructions or processing parameters**
5. **Re-process if necessary**

### Error Analysis Best Practices

```python
# Analyze multiple similar documents
job_guids = ["guid1", "guid2", "guid3"]
analyses = []

for guid in job_guids:
    analysis = await client.submit_and_wait_for_error_analysis(guid)
    analyses.append(analysis)

# Look for common patterns across analyses
common_issues = find_common_issues(analyses)
```

## Common Use Cases

### Invoice Processing QA

```python
# Process invoice
invoice_result = await client.upload_and_process(
    document=invoice_pdf,
    instructions="Extract invoice number, date, items, and total"
)

# Analyze for accuracy
analysis = await client.submit_and_wait_for_error_analysis(
    job_guid=invoice_result.parsed.guid
)

# Check for numerical accuracy issues
if "numerical_accuracy" in analysis.potential_issues:
    print("Consider reviewing numerical extractions manually")
```

### Contract Review Workflow

```python
# Process contract
contract_result = await client.upload_and_process(
    document=contract_doc,
    instructions="Extract key terms, dates, and obligations"
)

# Analyze with high-quality LLM
analysis = await client.submit_and_wait_for_operation_with_parameters(
    job_guid=contract_result.parsed.guid,
    operation_type="error-analysis",
    llm_type="HIGH",
    custom_parameters={"focus_areas": ["date_formats", "legal_terms"]}
)
```

## Error Handling

```python
try:
    # Submit operation
    operation_response = await client.submit_operation(
        job_guid=job_guid,
        operation_type="error-analysis"
    )
    
    if not operation_response.job_guid:
        raise Exception("No operation job GUID returned")
    
    # Wait for completion
    result = await client.submit_and_wait_for_operation(
        job_guid=job_guid,
        operation_type="error-analysis",
        timeout=180
    )
    
except TimeoutError as e:
    print(f"Operation timed out: {e}")
except Exception as e:
    print(f"Operation failed: {e}")
```

## Troubleshooting

### Common Issues

**Operation Not Found (404)**
- Verify the parent job GUID exists and is completed
- Check that you have access to the job (organization scope)

**Operation Failed**
- Check the operation status for error details
- Verify the parent job completed successfully
- Ensure sufficient processing credits

**No Result Available**
- Wait longer for operation to complete
- Check operation status for errors
- Verify result storage permissions

### Status Codes

- `PENDING`: Operation queued for processing
- `PROCESSING`: Analysis in progress
- `COMPLETED`: Operation finished successfully
- `FAILED`: Operation encountered an error

## Next Steps

- [Learn about document processing](../basics/SimpleDocuments.md)
- [Work with document collections](../advanced/cases.md)
- [Use templates for consistent extraction](../templates/Templates.md)