# Operations

import Tabs from '@theme/Tabs';
import TabItem from '@theme/TabItem';

Perform post-processing operations on completed document jobs for error analysis, generative tasks, and workflow optimization.

## Overview

Operations allow you to run additional analysis and processing on documents that have already been processed. Available operations include:

- **Error Analysis**: Identify potential issues in extraction results
- **Generative Tasks**: Generate summaries, translations, or custom AI responses based on processed documents

## Available Operations

### Error Analysis

Analyzes completed document processing jobs to identify:

- Potential extraction errors or inconsistencies
- Missing or incomplete data fields
- Confidence levels for extracted information
- Suggestions for improving accuracy

### Generative Tasks

Generates custom AI responses based on processed document content:

- Document summaries and explanations
- Content transformation (e.g., format conversion)
- Question answering based on document content
- Custom analysis with user-defined prompts

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

### Submit Generative Task

```http
POST /operation/{parentJobId}/generative-task
```

**Request Body:**

```json
{
  "prompt": "Summarize this document and explain its main purpose",
  "model": "DEFAULT",
  "temperature": 0.7,
  "maxTokens": 500
}
```

**Response:**

```json
{
  "jobGuid": "new-operation-job-uuid",
  "operationType": "generative-task",
  "status": "PENDING",
  "message": "Generative task operation scheduled"
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

## SDK & CLI Examples

### Error Analysis

<Tabs
  defaultValue="python"
  values={[
    {label: 'Python SDK', value: 'python'},
    {label: 'CLI', value: 'cli'},
    {label: 'cURL', value: 'curl'},
  ]}>
  <TabItem value="python">

```python
from docudevs.docudevs_client import DocuDevsClient

client = DocuDevsClient(token="your-api-key")

# Process a document first
job_guid = await client.submit_and_process_document(
  document=document_data,
  document_mime_type="application/pdf",
  prompt="Extract invoice data including line items and totals"
)

# Wait for processing to complete
await client.wait_until_ready(job_guid)

# Submit error analysis operation and wait for the result
analysis_result = await client.submit_and_wait_for_error_analysis(
    job_guid=job_guid,
    timeout=120
)

print(f"Analysis result: {analysis_result}")
```

  </TabItem>
  <TabItem value="cli">
```bash
# Submit error analysis and wait for completion
docudevs operations error-analysis JOB_GUID --timeout 180

# Check status of operations on the job (optional)
docudevs operations status JOB_GUID
```

  </TabItem>
  <TabItem value="curl">
```bash
curl -X POST https://api.docudevs.ai/operation \
  -H "Authorization: Bearer $API_KEY" \
  -H "Content-Type: application/json" \
  -d '{"jobGuid": "JOB_GUID", "type": "error-analysis"}'

# Poll for operation status
curl -X GET https://api.docudevs.ai/operation/JOB_GUID \
  -H "Authorization: Bearer $API_KEY"
```

  </TabItem>
</Tabs>

### Generative Tasks

<Tabs
  defaultValue="python"
  values={[
    {label: 'Python SDK', value: 'python'},
    {label: 'CLI', value: 'cli'},
    {label: 'cURL', value: 'curl'},
  ]}>
  <TabItem value="python">

```python
from docudevs.docudevs_client import DocuDevsClient

client = DocuDevsClient(token="your-api-key")

# Process the document with OCR first
ocr_job_id = await client.submit_and_ocr_document(
    document=document_data,
    document_mime_type="application/pdf",
    ocr="PREMIUM",
    ocr_format="markdown"
)

await client.wait_until_ready(ocr_job_id)

summary = await client.submit_and_wait_for_generative_task(
    parent_job_id=ocr_job_id,
    prompt="Summarize the main findings",
    model="DEFAULT"
)

print(summary.generated_text)
```

  </TabItem>
  <TabItem value="cli">
```bash
# Create a generative task that waits for completion
docudevs operations generative-task PARENT_JOB_GUID \
  --prompt "Summarize the main findings" \
  --model DEFAULT --timeout 180

# Retrieve the operation result if needed later
docudevs operations result PARENT_JOB_GUID --type generative-task
```

  </TabItem>
  <TabItem value="curl">
```bash
curl -X POST https://api.docudevs.ai/operation/PARENT_JOB_GUID/generative-task \
  -H "Authorization: Bearer $API_KEY" \
  -H "Content-Type: application/json" \
  -d '{"prompt": "Summarize the main findings", "model": "DEFAULT"}'

# Fetch the generative task result
curl -X GET https://api.docudevs.ai/operation/PARENT_JOB_GUID/generative-task \
  -H "Authorization: Bearer $API_KEY"
```

  </TabItem>
</Tabs>

#### Document Question Answering

```python
# Process document with OCR first
ocr_job_id = await client.submit_and_ocr_document(
    document=contract_pdf,
    document_mime_type="application/pdf"
)

await client.wait_until_ready(ocr_job_id)

# Ask specific questions about the document
questions = [
    "What is the contract duration?",
    "Who are the parties involved?",
    "What are the key obligations?"
]

for question in questions:
    answer = await client.submit_and_wait_for_generative_task(
        parent_job_id=ocr_job_id,
        prompt=f"Based on this document, answer the following question: {question}"
    )
    
  result_data = json.loads(answer.result)
    print(f"Q: {question}")
    print(f"A: {result_data['generated_text']}\n")
```

#### Multiple Tasks on Same Document

```python
# Process document once
ocr_job_id = await client.submit_and_ocr_document(
    document=document_data,
    document_mime_type="application/pdf"
)

await client.wait_until_ready(ocr_job_id)

# Create multiple generative tasks
tasks = [
    "Summarize this document in one paragraph.",
    "Extract the key dates mentioned in this document.",
    "Identify any risks or concerns mentioned."
]

results = []
for i, prompt in enumerate(tasks):
    result = await client.submit_and_wait_for_generative_task(
        parent_job_id=ocr_job_id,
        prompt=prompt
    )
    
  result_data = json.loads(result.result)
    results.append({
        'task': i + 1,
        'prompt': prompt,
        'response': result_data['generated_text']
    })

for result in results:
    print(f"Task {result['task']}: {result['prompt']}")
    print(f"Response: {result['response']}\n")
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

# Submit generative task
docudevs operation generative-task $PARENT_JOB_ID "Summarize this document"

# Check operation status
docudevs operation status $JOB_GUID

# Get operation result
docudevs operation result $JOB_GUID error-analysis
docudevs operation result $JOB_GUID generative-task
```

### cURL Examples

```bash
# Submit error analysis operation
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

# Submit generative task
curl -X POST "https://api.docudevs.ai/operation/your-parent-job-id/generative-task" \
  -H "Authorization: Bearer YOUR_API_KEY" \
  -H "Content-Type: application/json" \
  -d '{
    "prompt": "Summarize this document and explain its main purpose",
    "model": "gpt-4",
    "temperature": 0.7,
    "maxTokens": 500
  }'

# Get operation status
curl -X GET "https://api.docudevs.ai/operation/your-job-guid" \
  -H "Authorization: Bearer YOUR_API_KEY"

# Get operation result
curl -X GET "https://api.docudevs.ai/operation/your-job-guid/error-analysis" \
  -H "Authorization: Bearer YOUR_API_KEY"

curl -X GET "https://api.docudevs.ai/operation/your-job-guid/generative-task" \
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

## Generative Task Output

Generative task operations return responses containing AI-generated content:

```json
{
  "generated_text": "This document is a household contents insurance policy from AXA. It offers modular insurance coverage with two main options: BASIC for essential protection and COMFORT for comprehensive coverage. The policy covers personal property including digital assets, pets, e-bikes, jewelry, and monetary assets against risks like fire, theft, water damage, and natural disasters. Additional modules are available for specific items and services.",
  "model_used": "DEFAULT",
  "prompt": "Summarize this document and explain what type of document this is.",
  "processing_time_ms": 3245,
  "token_usage": {
    "prompt_tokens": 2847,
    "completion_tokens": 94,
    "total_tokens": 2941
  }
}
```

### Response Structure

- `generated_text`: The AI-generated response to your prompt
- `model_used`: Which AI model was used for generation
- `prompt`: The original prompt that was submitted
- `processing_time_ms`: Time taken to generate the response
- `token_usage`: Token usage statistics for the generation

## Operation Parameters

### LLM Types (for Error Analysis)

- `LOW`: Faster, cost-effective analysis
- `MEDIUM`: Balanced speed and quality
- `HIGH`: Thorough, detailed analysis

### Model Levels (for Generative Tasks)

- `DEFAULT`: Standard AI model, good balance of speed and quality
- `HIGH`: Premium AI model, slower but higher quality responses

### Generative Task Parameters

Generative tasks support the following parameters:

- `prompt` (required): The instruction for the AI model
- `model` (optional): AI model level to use ("DEFAULT", "HIGH")
- `temperature` (optional): Creativity level (0.0-1.0, default: 0.7)
- `max_tokens` (optional): Maximum response length (default: varies by model)

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
- **Document Summarization**: Use generative tasks for quick insights
- **Content Transformation**: Convert documents to different formats or styles
- **Question Answering**: Extract specific information from processed documents

### Operation Workflow

1. **Process documents normally first** (OCR or structured extraction)
2. **Run operations on completed jobs**
3. **Review results and apply insights**
4. **Adjust processing parameters if needed**
5. **Re-process if necessary**

### Generative Task Best Practices

```python
# Use specific, clear prompts
good_prompt = "Summarize the key financial terms in this contract, including payment amounts, due dates, and penalties."
avoid_prompt = "Tell me about this document."

# Chain multiple tasks for comprehensive analysis
ocr_job_id = await client.submit_and_ocr_document(document, mime_type)
await client.wait_until_ready(ocr_job_id)

# Get overview first
summary = await client.submit_and_wait_for_generative_task(
    parent_job_id=ocr_job_id,
    prompt="Provide a brief summary of this document's purpose and main content."
)

# Then ask specific questions
details = await client.submit_and_wait_for_generative_task(
    parent_job_id=ocr_job_id,
    prompt="List all important dates, amounts, and contact information found in this document."
)
```

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
job_id = await client.submit_and_process_document(
  document=invoice_pdf,
  document_mime_type="application/pdf",
  prompt="Extract invoice number, date, items, and total"
)

# Analyze for accuracy
analysis = await client.submit_and_wait_for_error_analysis(job_guid=job_id)

# Check for numerical accuracy issues
if "numerical_accuracy" in analysis.potential_issues:
    print("Consider reviewing numerical extractions manually")
```

### Contract Review Workflow

```python
# Process contract with OCR first
contract_result = await client.submit_and_ocr_document(
    document=contract_doc,
    document_mime_type="application/pdf",
    ocr="PREMIUM",
    ocr_format="markdown"
)

await client.wait_until_ready(contract_result)

# Generate contract summary
summary = await client.submit_and_wait_for_generative_task(
    parent_job_id=contract_result,
    prompt="Summarize this contract including parties involved, main obligations, and key terms."
)

# Extract specific information
key_dates = await client.submit_and_wait_for_generative_task(
    parent_job_id=contract_result,
    prompt="List all important dates in this contract including start date, end date, and any milestone dates."
)

# Analyze with high-quality LLM for errors
analysis = await client.submit_and_wait_for_operation_with_parameters(
    job_guid=contract_result,
    operation_type="error-analysis",
    llm_type="HIGH",
    custom_parameters={"focus_areas": ["date_formats", "legal_terms"]}
)
```

### Document Translation and Localization

```python
# Process document with OCR
document_job = await client.submit_and_ocr_document(
    document=document_data,
    document_mime_type="application/pdf"
)

await client.wait_until_ready(document_job)

# Translate to different languages
languages = ["Spanish", "French", "German"]
translations = {}

for language in languages:
    translation = await client.submit_and_wait_for_generative_task(
        parent_job_id=document_job,
        prompt=f"Translate the main content of this document to {language}. Maintain the original structure and formatting."
    )
    
  result_data = json.loads(translation.result)
    translations[language] = result_data['generated_text']

print(f"Translated to {len(translations)} languages")
```

### Research Document Analysis

```python
# Process research paper
research_job = await client.submit_and_ocr_document(
    document=research_pdf,
    document_mime_type="application/pdf",
    ocr="PREMIUM",
    ocr_format="markdown"
)

await client.wait_until_ready(research_job)

# Extract key information
abstract = await client.submit_and_wait_for_generative_task(
    parent_job_id=research_job,
    prompt="Extract and summarize the abstract and main findings of this research paper."
)

methodology = await client.submit_and_wait_for_generative_task(
    parent_job_id=research_job,
    prompt="Describe the methodology used in this research paper."
)

conclusions = await client.submit_and_wait_for_generative_task(
    parent_job_id=research_job,
    prompt="What are the main conclusions and future research directions mentioned?"
)

# Compile research summary
research_summary = {
  'abstract': json.loads(abstract.result)['generated_text'],
  'methodology': json.loads(methodology.result)['generated_text'],
  'conclusions': json.loads(conclusions.result)['generated_text']
}
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
