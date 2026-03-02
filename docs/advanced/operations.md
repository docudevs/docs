---
title: Operations
description: Post-processing operations on completed jobs including generative tasks, error analysis, and multi-step document workflows.
sidebar_position: 2
---

import Tabs from '@theme/Tabs';
import TabItem from '@theme/TabItem';

# Operations

Perform post-processing operations on completed document jobs for error analysis, generative tasks, and image selection.

## Overview

Operations allow you to run additional analysis and processing on documents that have already been processed. Available operations include:

- **Error Analysis**: Identify potential issues in extraction results.
- **Generative Tasks**: Generate summaries, translations, or custom AI responses based on processed documents.
- **Image Selection**: Pick relevant figures from extracted images based on a prompt.

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
import ai.docudevs.client.generated.api.OperationsApi;
import ai.docudevs.client.generated.internal.ApiClient;
import ai.docudevs.client.generated.model.OperationResultResponse;
import ai.docudevs.client.generated.model.SubmitOperationRequest;

ApiClient apiClient = new ApiClient();
apiClient.updateBaseUri("https://api.docudevs.ai");
apiClient.setRequestInterceptor(req ->
    req.header("Authorization", "Bearer " + System.getenv("API_KEY"))
);

OperationsApi operationsApi = new OperationsApi(apiClient);
String parentJobGuid = "JOB_GUID";

operationsApi.submitOperation(
    new SubmitOperationRequest()
        .jobGuid(parentJobGuid)
        .type("error-analysis")
);

while (true) {
    OperationResultResponse result = operationsApi.getOperationResult(parentJobGuid, "error-analysis");
    if ("COMPLETED".equals(result.getStatus())) {
        System.out.println(result.getResult());
        break;
    }
    if ("ERROR".equals(result.getStatus()) || "FAILED".equals(result.getStatus())) {
        throw new IllegalStateException("Operation failed: " + result.getError());
    }
    Thread.sleep(2000);
}
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
import ai.docudevs.client.generated.api.OperationsApi;
import ai.docudevs.client.generated.internal.ApiClient;
import ai.docudevs.client.generated.model.GenerativeTaskRequest;
import ai.docudevs.client.generated.model.OperationResultResponse;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

ApiClient apiClient = new ApiClient();
apiClient.updateBaseUri("https://api.docudevs.ai");
apiClient.setRequestInterceptor(req ->
    req.header("Authorization", "Bearer " + System.getenv("API_KEY"))
);

OperationsApi operationsApi = new OperationsApi(apiClient);
ObjectMapper mapper = new ObjectMapper();
String parentJobGuid = "PARENT_JOB_GUID";

operationsApi.createGenerativeTask(
    parentJobGuid,
    new GenerativeTaskRequest()
        .prompt("Summarize the main findings")
        .model("DEFAULT")
);

while (true) {
    OperationResultResponse result = operationsApi.getOperationResult(parentJobGuid, "generative-task");
    if ("COMPLETED".equals(result.getStatus())) {
        JsonNode payload = mapper.readTree(result.getResult());
        System.out.println(payload.path("generated_text").asText());
        break;
    }
    if ("ERROR".equals(result.getStatus()) || "FAILED".equals(result.getStatus())) {
        throw new IllegalStateException("Generative task failed: " + result.getError());
    }
    Thread.sleep(2000);
}
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
import ai.docudevs.client.generated.api.OperationsApi;
import ai.docudevs.client.generated.internal.ApiClient;
import ai.docudevs.client.generated.model.OperationParameters;
import ai.docudevs.client.generated.model.OperationResultResponse;
import ai.docudevs.client.generated.model.SubmitOperationRequest;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;

ApiClient apiClient = new ApiClient();
apiClient.updateBaseUri("https://api.docudevs.ai");
apiClient.setRequestInterceptor(req ->
    req.header("Authorization", "Bearer " + System.getenv("API_KEY"))
);

OperationsApi operationsApi = new OperationsApi(apiClient);
ObjectMapper mapper = new ObjectMapper();
String jobGuid = "JOB_GUID";

operationsApi.submitOperation(
    new SubmitOperationRequest()
        .jobGuid(jobGuid)
        .type("image-select")
        .parameters(
            new OperationParameters().customParameters(
                Map.of(
                    "prompt", "Return all diagrams from the document",
                    "topK", 5,
                    "matchMode", "all",
                    "useVision", false
                )
            )
        )
);

while (true) {
    OperationResultResponse result = operationsApi.getOperationResult(jobGuid, "image-select");
    if ("COMPLETED".equals(result.getStatus())) {
        JsonNode payload = mapper.readTree(result.getResult());
        JsonNode first = payload.path("selected").get(0);
        if (first != null) {
            String imageId = first.path("id").asText();
            HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create("https://api.docudevs.ai/job/figure/" + jobGuid + "/" + imageId))
                .header("Authorization", "Bearer " + System.getenv("API_KEY"))
                .GET()
                .build();
            HttpResponse<byte[]> image = HttpClient.newHttpClient().send(
                request,
                HttpResponse.BodyHandlers.ofByteArray()
            );
            if (image.statusCode() / 100 == 2) {
                Files.write(Path.of("selected_figure.png"), image.body());
            }
        }
        System.out.println(payload.toPrettyString());
        break;
    }
    if ("ERROR".equals(result.getStatus()) || "FAILED".equals(result.getStatus())) {
        throw new IllegalStateException("Image selection failed: " + result.getError());
    }
    Thread.sleep(2000);
}
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
import ai.docudevs.client.generated.api.OperationsApi;
import ai.docudevs.client.generated.internal.ApiClient;
import ai.docudevs.client.generated.model.LlmType;
import ai.docudevs.client.generated.model.OperationParameters;
import ai.docudevs.client.generated.model.SubmitOperationResponse;
import ai.docudevs.client.generated.model.SubmitOperationRequest;
import java.util.List;
import java.util.Map;

ApiClient apiClient = new ApiClient();
apiClient.updateBaseUri("https://api.docudevs.ai");
apiClient.setRequestInterceptor(req ->
    req.header("Authorization", "Bearer " + System.getenv("API_KEY"))
);

OperationsApi operationsApi = new OperationsApi(apiClient);

OperationParameters parameters = new OperationParameters()
    .llmType(LlmType.HIGH)
    .customParameters(
        Map.of(
            "focus_areas", List.of("numerical_accuracy", "date_formats"),
            "confidence_threshold", 0.8,
            "detailed_suggestions", true
        )
    );

SubmitOperationResponse response = operationsApi.submitOperation(
    new SubmitOperationRequest()
        .jobGuid("JOB_GUID")
        .type("error-analysis")
        .parameters(parameters)
);

System.out.println("Operation job GUID: " + response.getJobGuid());
System.out.println("Status: " + response.getStatus());
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
