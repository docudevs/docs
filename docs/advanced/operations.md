---
title: Operations
description: Post-processing operations on completed jobs including error analysis, generative tasks, image selection, and AcroForm PDF generation.
sidebar_position: 2
---

import Tabs from '@theme/Tabs';
import TabItem from '@theme/TabItem';

# Operations

Perform post-processing operations on completed document jobs for error analysis, generative tasks, image selection, and AcroForm PDF generation.

## Overview

Operations allow you to run additional analysis and processing on documents that have already been processed. Available operations include:

- **Error Analysis**: Identify potential issues in extraction results.
- **Generative Tasks**: Generate summaries, translations, or custom AI responses based on processed documents.
- **Image Selection**: Pick relevant figures from extracted images based on a prompt.
- **PDF AcroForm Conversion**: Turn a normal PDF into a generated fillable AcroForm PDF, inspect generated field metadata, and reapply reviewed field definitions.

## Available Operations

### Error Analysis

Analyzes completed document processing jobs to identify:

- Potential extraction errors or inconsistencies.
- Missing or incomplete data fields.
- Confidence levels for extracted information.
- Suggestions for improving accuracy.

### Generative Tasks

Generates custom AI responses based on processed document content:

- Document summaries and explanations.
- Content transformation (e.g., format conversion).
- Question answering based on document content.
- Custom analysis with user-defined prompts.

### Image Selection

Selects the most relevant extracted figures based on a prompt. The parent job must have figure artifacts, which are created by running document processing or OCR with `extract_figures=True` (or `describe_figures=True`).

### PDF AcroForm Conversion

Converts a completed PDF job into a generated fillable AcroForm PDF. This is useful when the source document has visible entry areas but no native AcroForm fields.

The conversion workflow has two companion artifacts:

- `get_acroform_metadata(operation_job_guid)` returns the generated PDF's embedded widget metadata in PDF page-point coordinates.
- `get_pdf_acroform_field_definitions(operation_job_guid)` returns normalized editable field definitions with `entryBbox` values in `0..1` page-image coordinates.

Use the field-definition artifact for review UIs or manual edits, then send the reviewed list back through `pdf-acroform/apply` to regenerate the PDF without rerunning visual detection.

## How Operations Work

1. **Document Processing**: First, process a document normally.
2. **Operation Submission**: Submit an operation request on the completed job.
3. **Analysis**: AI analyzes the original processing results.
4. **Results**: Get detailed analysis, generated content, or selected images.

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

## SDK & CLI Examples

### Error Analysis

<Tabs
  defaultValue="python"
  values={[
    {label: 'Python SDK', value: 'python'},
    {label: 'Java SDK', value: 'java'},
    {label: 'CLI', value: 'cli'},
    {label: 'cURL', value: 'curl'},
  ]}>
  <TabItem value="python">

```python
from docudevs.docudevs_client import DocuDevsClient
import os
import asyncio

client = DocuDevsClient(token=os.getenv('API_KEY'))

async def analyze_errors(job_guid):
    # Submit error analysis operation and wait for the result
    analysis_result = await client.submit_and_wait_for_error_analysis(
        job_guid=job_guid,
        timeout=120
    )
    print(f"Analysis result: {analysis_result}")

# Usage
# asyncio.run(analyze_errors("your-job-guid"))
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
  <TabItem value="java">

```java
import ai.docudevs.client.DocuDevsClient;
import ai.docudevs.client.WaitOptions;
import com.fasterxml.jackson.databind.JsonNode;

DocuDevsClient client = DocuDevsClient.builder()
    .apiKey(System.getenv("API_KEY"))
    .build();

String parentJobGuid = "JOB_GUID";

client.submitOperation(parentJobGuid, "error-analysis");

JsonNode result = client.waitForOperation(
    parentJobGuid, "error-analysis", WaitOptions.builder().build()
);
System.out.println(result.path("result"));
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
    {label: 'Java SDK', value: 'java'},
    {label: 'CLI', value: 'cli'},
    {label: 'cURL', value: 'curl'},
  ]}>
  <TabItem value="python">

```python
async def summarize_document(job_guid):
    summary = await client.submit_and_wait_for_generative_task(
        parent_job_id=job_guid,
        prompt="Summarize the main findings",
        model="DEFAULT"
    )
    # The result is a JSON string inside the response object
    import json
    result_data = json.loads(summary.result)
    print(result_data['generated_text'])
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
  <TabItem value="java">

```java
import ai.docudevs.client.DocuDevsClient;
import ai.docudevs.client.WaitOptions;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

DocuDevsClient client = DocuDevsClient.builder()
    .apiKey(System.getenv("API_KEY"))
    .build();

ObjectMapper mapper = new ObjectMapper();
String parentJobGuid = "PARENT_JOB_GUID";

client.createGenerativeTask(parentJobGuid, "Summarize the main findings", "DEFAULT");

JsonNode result = client.waitForGenerativeTask(
    parentJobGuid, WaitOptions.builder().build()
);
JsonNode payload = mapper.readTree(result.path("result").asText());
System.out.println(payload.path("generated_text").asText());
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

### Image Selection

<Tabs
  defaultValue="python"
  values={[
    {label: 'Python SDK', value: 'python'},
    {label: 'Java SDK', value: 'java'},
    {label: 'CLI', value: 'cli'},
    {label: 'cURL', value: 'curl'},
  ]}>
  <TabItem value="python">

```python
import json

job_guid = await client.submit_and_process_document(
    document=document_bytes,
    document_mime_type="application/pdf",
    prompt="Extract key information",
    extract_figures=True
)
await client.wait_until_ready(job_guid)

selection = await client.submit_and_wait_for_image_selection(
    job_guid,
    prompt="Return all diagrams from the document",
    top_k=5,
    match_mode="all",
    use_vision=False
)

selection_payload = json.loads(selection.result)
first = selection_payload["selected"][0]
image_bytes = await client.get_figure_image(job_guid, first["id"])
```

  </TabItem>
  <TabItem value="cli">

```bash
docudevs operations submit JOB_GUID \
  --type image-select \
  --parameter prompt="Return all diagrams from the document" \
  --parameter topK=5 \
  --parameter matchMode=all \
  --parameter useVision=false \
  --wait

docudevs operations result JOB_GUID --type image-select
```

  </TabItem>
  <TabItem value="java">

```java
import ai.docudevs.client.DocuDevsClient;
import ai.docudevs.client.WaitOptions;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;

DocuDevsClient client = DocuDevsClient.builder()
    .apiKey(System.getenv("API_KEY"))
    .build();

ObjectMapper mapper = new ObjectMapper();
String jobGuid = "JOB_GUID";

client.submitOperation(jobGuid, "image-select", null,
    Map.of(
        "prompt", "Return all diagrams from the document",
        "topK", 5,
        "matchMode", "all",
        "useVision", false
    )
);

JsonNode result = client.waitForOperation(
    jobGuid, "image-select", WaitOptions.builder().build()
);

JsonNode payload = mapper.readTree(result.path("result").asText());
JsonNode first = payload.path("selected").get(0);
if (first != null) {
    String imageId = first.path("id").asText();
    byte[] imageBytes = client.getFigureImage(jobGuid, imageId);
    Files.write(Path.of("selected_figure.png"), imageBytes);
}
System.out.println(payload.toPrettyString());
```

  </TabItem>

  <TabItem value="curl">

```bash
curl -X POST https://api.docudevs.ai/operation \
  -H "Authorization: Bearer $API_KEY" \
  -H "Content-Type: application/json" \
  -d '{
        "jobGuid": "JOB_GUID",
        "type": "image-select",
        "parameters": {
          "customParameters": {
            "prompt": "Return all diagrams from the document",
            "topK": 5,
            "matchMode": "all",
            "useVision": false
          }
        }
      }'
```

  </TabItem>
</Tabs>

### PDF AcroForm Conversion

Run `pdf-acroform` on a completed PDF job to generate a fillable AcroForm PDF, then inspect or edit the generated field definitions.

The parent job must point to a PDF. The operation also needs rendered page images, so the safest setup is to create the parent job with `ocr="PREMIUM"` or pass `force_ocr=True` when thumbnails are missing.

<Tabs
  defaultValue="python"
  values={[
    {label: 'Python SDK', value: 'python'},
    {label: 'CLI', value: 'cli'},
    {label: 'cURL', value: 'curl'},
  ]}>
  <TabItem value="python">

```python
import json

parent_job_guid = await client.submit_and_process_document(
    document=pdf_bytes,
    document_mime_type="application/pdf",
    ocr="PREMIUM",
    extraction_mode="OCR",
    acro_form_metadata=True,
)
await client.wait_until_ready(parent_job_guid, timeout=900, poll_interval=5)

conversion = await client.submit_and_wait_for_pdf_acroform_operation(
    parent_job_guid,
    llm_type="DEFAULT",
    ocr="PREMIUM",
    min_confidence=0.35,
    max_fields_per_page=300,
    timeout=1200,
    poll_interval=10,
    save_to="medical-examination-form-fillable.pdf",
)

generated_metadata = await client.get_acroform_metadata(conversion.operation_job_guid)
definitions_artifact = await client.get_pdf_acroform_field_definitions(
    conversion.operation_job_guid
)

print(json.dumps(generated_metadata.get("fields", [])[:3], indent=2))
print(json.dumps(definitions_artifact["fieldDefinitions"][:3], indent=2))

reviewed_fields = [dict(field) for field in definitions_artifact["fieldDefinitions"]]
for field in reviewed_fields:
    field["reviewStatus"] = "accepted"

applied = await client.submit_and_wait_for_pdf_acroform_apply_operation(
    parent_job_guid,
    field_definitions=reviewed_fields,
    source_operation_guid=conversion.operation_job_guid,
    save_to="medical-examination-form-reviewed.pdf",
)

print(applied.operation_job_guid)
```

  </TabItem>
  <TabItem value="cli">

```bash
docudevs operations pdf-acroform PARENT_JOB_GUID \
  --ocr PREMIUM \
  --min-confidence 0.35 \
  --max-fields-per-page 300 \
  --timeout 1200 \
  --poll-interval 10 \
  --output medical-examination-form-fillable.pdf
```

The CLI prints the `operationJobGuid`. Use the SDK or raw HTTP API to fetch `acroform-metadata` and `pdf-acroform-field-definitions`, or to submit reviewed field definitions back through `pdf-acroform/apply`.

  </TabItem>
  <TabItem value="curl">

```bash
curl -X POST https://api.docudevs.ai/operation \
  -H "Authorization: Bearer $API_KEY" \
  -H "Content-Type: application/json" \
  -d '{
    "jobGuid": "PARENT_JOB_GUID",
    "type": "pdf-acroform",
    "parameters": {
      "llmType": "DEFAULT",
      "customParameters": {
        "ocr": "PREMIUM",
        "fieldNameStyle": "source_snake_case",
        "minConfidence": 0.35,
        "maxFieldsPerPage": 300
      }
    }
  }'

curl -X GET https://api.docudevs.ai/job/result/OPERATION_JOB_GUID/pdf-acroform \
  -H "Authorization: Bearer $API_KEY" \
  --output medical-examination-form-fillable.pdf

curl -X GET https://api.docudevs.ai/job/result/OPERATION_JOB_GUID/acroform-metadata \
  -H "Authorization: Bearer $API_KEY"

curl -X GET https://api.docudevs.ai/job/result/OPERATION_JOB_GUID/pdf-acroform-field-definitions \
  -H "Authorization: Bearer $API_KEY"

curl -X POST https://api.docudevs.ai/job/PARENT_JOB_GUID/pdf-acroform/apply \
  -H "Authorization: Bearer $API_KEY" \
  -H "Content-Type: application/json" \
  -d '{
    "sourceOperationGuid": "OPERATION_JOB_GUID",
    "fieldDefinitions": [],
    "minConfidence": 0.0,
    "maxFieldsPerPage": 300
  }'
```

  </TabItem>
</Tabs>

The generated PDF from either the initial conversion or the reviewed apply flow is always downloaded through `GET /job/result/{guid}/pdf-acroform`.

## Advanced Usage

### Document Question Answering

You can use Generative Tasks to ask specific questions about a processed document.

```python
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

### Advanced Parameters

<Tabs
  defaultValue="python"
  values={[
    {label: 'Python SDK', value: 'python'},
    {label: 'Java SDK', value: 'java'},
    {label: 'CLI', value: 'cli'},
    {label: 'cURL', value: 'curl'},
  ]}>
  <TabItem value="python">

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
```

  </TabItem>
  <TabItem value="cli">

```bash
# Submit error analysis with HIGH quality model
docudevs operations error-analysis <JOB_GUID> \
  --llm-type HIGH \
  --timeout 180
```

  </TabItem>
  <TabItem value="java">

```java
import ai.docudevs.client.DocuDevsClient;
import com.fasterxml.jackson.databind.JsonNode;
import java.util.List;
import java.util.Map;

DocuDevsClient client = DocuDevsClient.builder()
    .apiKey(System.getenv("API_KEY"))
    .build();

JsonNode response = client.submitOperation(
    "JOB_GUID",
    "error-analysis",
    "HIGH",
    Map.of(
        "focus_areas", List.of("numerical_accuracy", "date_formats"),
        "confidence_threshold", 0.8,
        "detailed_suggestions", true
    )
);

System.out.println("Operation job GUID: " + response.path("jobGuid").asText());
System.out.println("Status: " + response.path("status").asText());
```

  </TabItem>

  <TabItem value="curl">

```bash
curl -X POST https://api.docudevs.ai/operation \
  -H "Authorization: Bearer $API_KEY" \
  -H "Content-Type: application/json" \
  -d '{
    "jobGuid": "<JOB_GUID>",
    "type": "error-analysis",
    "parameters": {
      "llmType": "HIGH",
      "customParameters": {
        "focus_areas": ["numerical_accuracy", "date_formats"],
        "confidence_threshold": 0.8
      }
    }
  }'
```

  </TabItem>
</Tabs>

## Output Formats

### Error Analysis Output

```json
{
  "analysis": {
    "overall_confidence": 0.85,
    "field_confidence": {
      "invoice_number": 0.95,
      "total_amount": 0.90
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
      "description": "Consider using higher quality OCR settings"
    }
  ]
}
```

### Generative Task Output

```json
{
  "generated_text": "This document is a household contents insurance policy...",
  "model_used": "DEFAULT",
  "prompt": "Summarize this document...",
  "processing_time_ms": 3245,
  "token_usage": {
    "prompt_tokens": 2847,
    "completion_tokens": 94,
    "total_tokens": 2941
  }
}
```

## Best Practices

- **Quality Assurance**: Run error analysis on critical documents or when confidence scores are low.
- **Summarization**: Use generative tasks to create quick summaries for document lists.
- **Translation**: Use generative tasks to translate document content while preserving context.
- **Chain Operations**: Process a document, then run error analysis, then summarize it—all via API.
- **Map-Reduce Re-processing**: Use `submit_and_wait_for_map_reduce(parent_job_id=...)` to run map-reduce on a previously processed document without re-uploading or re-running OCR. See [Map-Reduce Extraction](/docs/core/map-reduce-extraction#re-running-map-reduce-on-an-existing-job) for details.
