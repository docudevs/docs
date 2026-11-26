---
title: AI Document Analysis
description: Generate summaries, classify documents, answer questions, and transform content using AI generative tasks after OCR.
sidebar_position: 3
---
<!-- Generated from docs/core/ai-analysis.md. Do not edit by hand. -->

# AI Document Analysis

Generate intelligent insights from your documents using AI-powered generative tasks. Perfect for summarization, content transformation, and custom analysis workflows.

## Overview

AI Document Analysis uses generative AI models to understand and analyze document content after OCR processing. This enables you to:

- **Summarize** long documents into key points
- **Extract** specific information using natural language queries
- **Transform** content into different formats or styles
- **Classify** documents automatically
- **Answer** questions based on document content

## How It Works

1. **OCR Processing**: First, extract text from your document using OCR (for very large documents consider enabling Map-Reduce extraction to improve recall)
2. **AI Analysis**: Submit generative tasks with custom prompts
3. **Intelligent Response**: Get AI-generated insights based on document content

## Basic Usage

### Document Summarization

OCR the document first, then submit a generative task whose parent is the OCR job. The task result carries the generated text. The Java variant submits the task with `submitAndWaitForGenerativeTask`, which waits for the task's own result, and reads `generated_text` from it.

**cURL**

```bash
wait_for_job() {
  local deadline=$((SECONDS + 600))
  while [ "$SECONDS" -lt "$deadline" ]; do
    STATUS=$(curl -s -S -f "https://api.docudevs.ai/job/status/$1" \
      -H "Authorization: $API_KEY" | jq -r '.status')
    case "$STATUS" in
      COMPLETED) return 0 ;;
      PARTIAL) echo "Job $1 ended with status PARTIAL" >&2; return 0 ;;
      ERROR|FAILED|TIMEOUT) echo "Job $1 ended with status $STATUS" >&2; return 1 ;;
    esac
    sleep 5
  done
  echo "Job $1 did not finish within 600 seconds" >&2
  return 1
}

OCR_JOB_ID=$(curl -s -S -f -X POST "https://api.docudevs.ai/document/upload" \
  -H "Authorization: $API_KEY" \
  -F "document=@your-document.pdf" | jq -r '.guid')

curl -s -S -f -o /dev/null -X POST "https://api.docudevs.ai/document/ocr/$OCR_JOB_ID?format=markdown" \
  -H "Authorization: $API_KEY" \
  -H "Content-Type: application/json" \
  -d '{
    "ocr": "PREMIUM",
    "ocrFormat": "markdown",
    "mimeType": "application/pdf"
  }'
wait_for_job "$OCR_JOB_ID"

ANALYSIS_JOB_ID=$(curl -s -S -f -X POST "https://api.docudevs.ai/operation/$OCR_JOB_ID/generative-task" \
  -H "Authorization: $API_KEY" \
  -H "Content-Type: application/json" \
  -d '{
    "prompt": "Summarize this document and explain what type of document it is and its main purpose."
  }' | jq -r '.jobGuid')
wait_for_job "$ANALYSIS_JOB_ID"

echo "AI Analysis:"
curl -s -S -f "https://api.docudevs.ai/job/result/$ANALYSIS_JOB_ID" \
  -H "Authorization: $API_KEY" | jq -r '.generated_text'
```

**Python**

```python
with open("your-document.pdf", "rb") as f:
    document_data = f.read()

ocr_job_id = await client.submit_and_ocr_document(
    document=document_data,
    document_mime_type="application/pdf",
    ocr="PREMIUM",
    ocr_format="markdown",
)
await client.wait_until_ready(ocr_job_id)

analysis = await client.submit_and_wait_for_generative_task(
    parent_job_id=ocr_job_id,
    prompt="Summarize this document and explain what type of document it is and its main purpose.",
)

result_data = json.loads(analysis.result)
print("\nAI Analysis:")
print(result_data["generated_text"])
```

**Java**

```java
import ai.docudevs.client.ProcessOptions;
import ai.docudevs.client.UploadRequest;
import ai.docudevs.client.WaitOptions;
import com.fasterxml.jackson.databind.JsonNode;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;

byte[] documentData = Files.readAllBytes(Path.of("your-document.pdf"));

String ocrJobId = client.uploadDocument(
        new UploadRequest("your-document.pdf", "application/pdf", documentData));
client.ocrDocument(
        ocrJobId,
        ProcessOptions.builder().ocr("PREMIUM").mimeType("application/pdf").build(),
        "markdown");
client.waitUntilReadyText(
        ocrJobId,
        WaitOptions.builder()
                .timeout(Duration.ofSeconds(180))
                .pollInterval(Duration.ofSeconds(5))
                .build());

// One call submits the generative task and waits for it; it returns the task's result document
JsonNode analysis = client.submitAndWaitForGenerativeTask(
        ocrJobId,
        "Summarize this document and explain what type of document it is and its main purpose.",
        WaitOptions.builder()
                .timeout(Duration.ofSeconds(120))
                .pollInterval(Duration.ofSeconds(2))
                .build());

System.out.println("\nAI Analysis:");
System.out.println(analysis.path("generated_text").asText());
```

**CLI**

```bash
docudevs ocr-only your-document.pdf \
  --ocr PREMIUM \
  --format markdown \
  --no-wait > ocr-submit.txt
OCR_JOB_ID=$(sed -n 's/.*GUID: //p' ocr-submit.txt)

docudevs wait "$OCR_JOB_ID" --timeout 180 > /dev/null

docudevs operations generative-task "$OCR_JOB_ID" \
  --prompt "Summarize this document and explain what type of document it is and its main purpose."
```

### Question Answering

Ask questions about a document that has already been through OCR. Each question is its own generative task on the OCR job (the Java variant submits each question with `submitAndWaitForGenerativeTask`, which waits for that task's own result), so replace `<job-guid>` with the GUID of the OCR job from the summarization step.

**cURL**

```bash
wait_for_job() {
  local deadline=$((SECONDS + 600))
  while [ "$SECONDS" -lt "$deadline" ]; do
    STATUS=$(curl -s -S -f "https://api.docudevs.ai/job/status/$1" \
      -H "Authorization: $API_KEY" | jq -r '.status')
    case "$STATUS" in
      COMPLETED) return 0 ;;
      PARTIAL) echo "Job $1 ended with status PARTIAL" >&2; return 0 ;;
      ERROR|FAILED|TIMEOUT) echo "Job $1 ended with status $STATUS" >&2; return 1 ;;
    esac
    sleep 5
  done
  echo "Job $1 did not finish within 600 seconds" >&2
  return 1
}

PARENT_JOB_GUID="<job-guid>"  # GUID of a completed document job

QUESTIONS=(
  "What is the contract duration?"
  "Who are the parties involved?"
  "What are the key obligations?"
)

for QUESTION in "${QUESTIONS[@]}"; do
  BODY=$(jq -n --arg question "$QUESTION" \
    '{prompt: "Based on this document, answer the following question: \($question)"}')

  TASK_JOB_GUID=$(curl -s -S -f -X POST "https://api.docudevs.ai/operation/$PARENT_JOB_GUID/generative-task" \
    -H "Authorization: $API_KEY" \
    -H "Content-Type: application/json" \
    -d "$BODY" | jq -r '.jobGuid')

  wait_for_job "$TASK_JOB_GUID"

  ANSWER=$(curl -s -S -f "https://api.docudevs.ai/job/result/$TASK_JOB_GUID" \
    -H "Authorization: $API_KEY" | jq -r '.generated_text')
  echo "Q: $QUESTION"
  echo "A: $ANSWER"
  echo
done
```

**Python**

```python
parent_job_guid = "<job-guid>"  # GUID of a completed document job

questions = [
    "What is the contract duration?",
    "Who are the parties involved?",
    "What are the key obligations?",
]

for question in questions:
    answer = await client.submit_and_wait_for_generative_task(
        parent_job_id=parent_job_guid,
        prompt=f"Based on this document, answer the following question: {question}",
    )

    result_data = json.loads(answer.result)
    print(f"Q: {question}")
    print(f"A: {result_data['generated_text']}\n")
```

**Java**

```java
import ai.docudevs.client.WaitOptions;
import com.fasterxml.jackson.databind.JsonNode;
import java.time.Duration;
import java.util.List;

String parentJobGuid = "<job-guid>"; // GUID of a completed document job

List<String> questions = List.of(
        "What is the contract duration?",
        "Who are the parties involved?",
        "What are the key obligations?");

for (String question : questions) {
    // One call submits the task and waits for it; it returns the task's result document
    JsonNode answer = client.submitAndWaitForGenerativeTask(
            parentJobGuid,
            "Based on this document, answer the following question: " + question,
            WaitOptions.builder()
                    .timeout(Duration.ofSeconds(120))
                    .pollInterval(Duration.ofSeconds(2))
                    .build());

    System.out.println("Q: " + question);
    System.out.println("A: " + answer.path("generated_text").asText() + "\n");
}
```

## Advanced Use Cases

### Several Analyses of One Document

OCR runs once; every analysis is a separate generative task on the OCR job. The example runs four different prompts and collects the answers by name.

**cURL**

```bash
wait_for_job() {
  local deadline=$((SECONDS + 600))
  while [ "$SECONDS" -lt "$deadline" ]; do
    STATUS=$(curl -s -S -f "https://api.docudevs.ai/job/status/$1" \
      -H "Authorization: $API_KEY" | jq -r '.status')
    case "$STATUS" in
      COMPLETED) return 0 ;;
      PARTIAL) echo "Job $1 ended with status PARTIAL" >&2; return 0 ;;
      ERROR|FAILED|TIMEOUT) echo "Job $1 ended with status $STATUS" >&2; return 1 ;;
    esac
    sleep 5
  done
  echo "Job $1 did not finish within 600 seconds" >&2
  return 1
}

OCR_JOB_ID=$(curl -s -S -f -X POST "https://api.docudevs.ai/document/upload" \
  -H "Authorization: $API_KEY" \
  -F "document=@report.pdf" | jq -r '.guid')

curl -s -S -f -o /dev/null -X POST "https://api.docudevs.ai/document/ocr/$OCR_JOB_ID?format=markdown" \
  -H "Authorization: $API_KEY" \
  -H "Content-Type: application/json" \
  -d '{
    "ocr": "PREMIUM",
    "ocrFormat": "markdown",
    "mimeType": "application/pdf"
  }'
wait_for_job "$OCR_JOB_ID"

NAMES=(document_type key_information summary tone)
PROMPTS=(
  "What type of document is this? (invoice, contract, report, form, etc.)"
  "Extract the most important information: dates, names, amounts, addresses, and key terms."
  "Provide a concise summary of this document's main content and purpose."
  "Analyze the tone of this document. Is it formal, informal, positive, negative, urgent?"
)

RESULTS='{}'
for INDEX in "${!NAMES[@]}"; do
  BODY=$(jq -n --arg prompt "${PROMPTS[$INDEX]}" '{prompt: $prompt}')

  TASK_JOB_GUID=$(curl -s -S -f -X POST "https://api.docudevs.ai/operation/$OCR_JOB_ID/generative-task" \
    -H "Authorization: $API_KEY" \
    -H "Content-Type: application/json" \
    -d "$BODY" | jq -r '.jobGuid')
  wait_for_job "$TASK_JOB_GUID"

  TEXT=$(curl -s -S -f "https://api.docudevs.ai/job/result/$TASK_JOB_GUID" \
    -H "Authorization: $API_KEY" | jq -r '.generated_text')
  RESULTS=$(jq --arg name "${NAMES[$INDEX]}" --arg text "$TEXT" '. + {($name): $text}' <<< "$RESULTS")
done

echo "$RESULTS" | jq .
```

**Python**

```python
with open("report.pdf", "rb") as f:
    document_data = f.read()

ocr_job_id = await client.submit_and_ocr_document(
    document=document_data,
    document_mime_type="application/pdf",
    ocr="PREMIUM",
    ocr_format="markdown",
)
await client.wait_until_ready(ocr_job_id)

prompts = {
    "document_type": "What type of document is this? (invoice, contract, report, form, etc.)",
    "key_information": "Extract the most important information: dates, names, amounts, addresses, and key terms.",
    "summary": "Provide a concise summary of this document's main content and purpose.",
    "tone": "Analyze the tone of this document. Is it formal, informal, positive, negative, urgent?",
}

results = {}
for name, prompt in prompts.items():
    task = await client.submit_and_wait_for_generative_task(
        parent_job_id=ocr_job_id,
        prompt=prompt,
    )
    results[name] = json.loads(task.result)["generated_text"]

print(json.dumps(results, indent=2))
```

**Java**

```java
import ai.docudevs.client.ProcessOptions;
import ai.docudevs.client.UploadRequest;
import ai.docudevs.client.WaitOptions;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.util.LinkedHashMap;
import java.util.Map;

byte[] documentData = Files.readAllBytes(Path.of("report.pdf"));

String ocrJobId = client.uploadDocument(
        new UploadRequest("report.pdf", "application/pdf", documentData));
client.ocrDocument(
        ocrJobId,
        ProcessOptions.builder().ocr("PREMIUM").mimeType("application/pdf").build(),
        "markdown");
client.waitUntilReadyText(
        ocrJobId,
        WaitOptions.builder()
                .timeout(Duration.ofSeconds(180))
                .pollInterval(Duration.ofSeconds(5))
                .build());

Map<String, String> prompts = new LinkedHashMap<>();
prompts.put("document_type", "What type of document is this? (invoice, contract, report, form, etc.)");
prompts.put("key_information", "Extract the most important information: dates, names, amounts, addresses, and key terms.");
prompts.put("summary", "Provide a concise summary of this document's main content and purpose.");
prompts.put("tone", "Analyze the tone of this document. Is it formal, informal, positive, negative, urgent?");

ObjectMapper mapper = new ObjectMapper();
ObjectNode results = mapper.createObjectNode();
for (Map.Entry<String, String> prompt : prompts.entrySet()) {
    // One call submits the generative task and waits for it; it returns the task's result document
    JsonNode task = client.submitAndWaitForGenerativeTask(
            ocrJobId,
            prompt.getValue(),
            WaitOptions.builder()
                    .timeout(Duration.ofSeconds(120))
                    .pollInterval(Duration.ofSeconds(2))
                    .build());
    results.put(prompt.getKey(), task.path("generated_text").asText());
}

System.out.println(results.toPrettyString());
```

Change the entries in the prompt list for other analyses; the recipes below show typical prompts.

### Multi-Language Document Analysis

Ask for the same summary in each language you need, one task per language:

- `Provide a summary of this document in English. Include the main points and conclusions.`
- `Provide a summary of this document in Spanish. Include the main points and conclusions.`
- `Provide a summary of this document in French. Include the main points and conclusions.`

### Document Classification and Routing

Run a classification prompt per document and route on the answer:

```text
Classify this document into one of these categories:
- Invoice
- Contract
- Report
- Form
- Receipt
- Other

Respond with just the category name and a brief explanation of why.
```

### Content Transformation

Turn one document into several audience-specific texts, one task per audience:

- **Executive summary**: `Create an executive summary suitable for business stakeholders, focusing on practical implications and key findings.`
- **Technical abstract**: `Write a technical abstract that highlights the methodology, key results, and conclusions for a scientific audience.`
- **Plain language summary**: `Explain the main findings and importance of this research in simple language that a general audience can understand.`

## Configuration Options

A generative task accepts an optional `model` field next to the prompt. It selects the model tier and takes one of `DEFAULT`, `NANO`, `MINI` or `HIGH`, the same values as the LLM type. In the Python SDK it is the `model` argument of `submit_and_wait_for_generative_task`; the CLI has `--model`.

```json
{
  "prompt": "Summarize the main points",
  "model": "DEFAULT"
}
```

## Best Practices

### Effective Prompt Writing

**Be Specific and Clear**

Good prompt:

```text
Extract the contract start date, end date, parties involved, and key obligations. Format as a bulleted list.
```

Avoid vague prompts such as `Tell me about this document.`

**Provide Context and Format Instructions**

```text
Analyze this financial report and provide:
1. Revenue trends (increase/decrease and percentages)
2. Key financial metrics (profit margin, ROI, etc.)  
3. Risk factors mentioned
4. Management outlook/guidance

Format your response with clear headings for each section.
```

### Error Handling

The Python helpers raise `TimeoutError` when a job does not finish within `timeout` seconds and raise an exception when the job ends in an error status; wrap the calls in `try`/`except` where a failed analysis must not stop your program. The cURL examples stop when `wait_for_job` returns a non-zero status. Always check that the OCR job completed before you submit generative tasks on it.

### Many Documents and Prompts

Every generative task is an independent job under its OCR parent, so you can OCR many documents first, submit the tasks for each document, and wait for them concurrently. To process a large collection of similar documents with one configuration, use [Batch Processing](https://docs.docudevs.ai/docs/core/batch-processing) instead.

## Integration with Other Features

### Combine with Error Analysis

Run error analysis on a completed extraction job to review its quality. Replace `<job-guid>` with the GUID of the extraction job.

**cURL**

```bash
wait_for_job() {
  local deadline=$((SECONDS + 600))
  while [ "$SECONDS" -lt "$deadline" ]; do
    STATUS=$(curl -s -S -f "https://api.docudevs.ai/job/status/$1" \
      -H "Authorization: $API_KEY" | jq -r '.status')
    case "$STATUS" in
      COMPLETED) return 0 ;;
      PARTIAL) echo "Job $1 ended with status PARTIAL" >&2; return 0 ;;
      ERROR|FAILED|TIMEOUT) echo "Job $1 ended with status $STATUS" >&2; return 1 ;;
    esac
    sleep 5
  done
  echo "Job $1 did not finish within 600 seconds" >&2
  return 1
}

JOB_GUID="<job-guid>"  # GUID of a completed document job

OPERATION_JOB_GUID=$(curl -s -S -f -X POST "https://api.docudevs.ai/operation" \
  -H "Authorization: $API_KEY" \
  -H "Content-Type: application/json" \
  -d "{\"jobGuid\": \"$JOB_GUID\", \"type\": \"error-analysis\"}" | jq -r '.jobGuid')

wait_for_job "$OPERATION_JOB_GUID"

curl -s -S -f "https://api.docudevs.ai/job/result/$OPERATION_JOB_GUID" \
  -H "Authorization: $API_KEY" | jq .
```

**Python**

```python
import asyncio
import os

from docudevs.docudevs_client import DocuDevsClient


async def main():
    client = DocuDevsClient(token=os.environ["API_KEY"])

    job_guid = "<job-guid>"  # GUID of a completed document job

    result = await client.submit_and_wait_for_error_analysis(
        job_guid=job_guid,
        timeout=120,
    )
    print(result.result)


asyncio.run(main())
```

**Java**

```java
import ai.docudevs.client.DocuDevsClient;
import ai.docudevs.client.WaitOptions;
import com.fasterxml.jackson.databind.JsonNode;
import java.time.Duration;

public class Example {
    public static void main(String[] args) throws Exception {
        DocuDevsClient client = DocuDevsClient.builder()
                .apiKey(System.getenv("API_KEY"))
                .baseUrl("https://api.docudevs.ai")
                .build();

        String jobGuid = "<job-guid>"; // GUID of a completed document job

        client.submitOperation(jobGuid, "error-analysis");

        JsonNode result = client.waitForOperation(
                jobGuid, "error-analysis", WaitOptions.builder()
                        .timeout(Duration.ofSeconds(120))
                        .pollInterval(Duration.ofSeconds(2))
                        .build());
        System.out.println(result.path("result"));
    }
}
```

**CLI**

```bash
JOB_GUID="<job-guid>"  # GUID of a completed document job

docudevs operations error-analysis "$JOB_GUID" --timeout 120

docudevs operations status "$JOB_GUID"
```

## Next Steps

- [Learn about Operations](https://docs.docudevs.ai/docs/advanced/operations) - Explore all operation types including error analysis
- [Work with Cases](https://docs.docudevs.ai/docs/advanced/cases) - Organize and batch process documents
- [SDK Reference](https://docs.docudevs.ai/docs/reference/sdk-methods) - Complete API documentation
- [Best Practices](https://docs.docudevs.ai/docs/integration/best-practices) - Production deployment guidelines
