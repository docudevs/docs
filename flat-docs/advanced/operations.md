---
title: Operations
description: Post-processing operations on completed jobs including error analysis, generative tasks, image selection, and AcroForm PDF generation.
sidebar_position: 2
---
<!-- Generated from docs/advanced/operations.md. Do not edit by hand. -->

# Operations

Perform post-processing operations on completed document jobs for error analysis, generative tasks, image selection, and AcroForm PDF generation.

## Overview

Operations allow you to run additional analysis and processing on documents that have already been processed. Available operations include:

- **Error Analysis**: Identify potential issues in extraction results.
- **Generative Tasks**: Generate summaries, translations, or custom AI responses based on processed documents.
- **Image Selection**: Pick relevant figures from extracted images based on a prompt.
- **PDF AcroForm Conversion**: Turn a normal PDF into a generated fillable AcroForm PDF, inspect generated field metadata, and reapply reviewed field definitions.
- **Document Outline**: (Re)build a structural outline artifact for a completed job without rerunning the extraction.

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

Selects the most relevant extracted figures based on a prompt. The parent job must have figure artifacts, which are created by running document processing or OCR with `extractFigures` (or `describeFigures`) enabled.

### PDF AcroForm Conversion

Converts a completed PDF job into a generated fillable AcroForm PDF. This is useful when the source document has visible entry areas but no native AcroForm fields.

The conversion workflow has two companion artifacts:

- `GET /job/result/{operationJobGuid}/acroform-metadata` returns the generated PDF's embedded widget metadata in PDF page-point coordinates.
- `GET /job/result/{operationJobGuid}/pdf-acroform-field-definitions` returns normalized editable field definitions with `entryBbox` values in `0..1` page-image coordinates.

Use the field-definition artifact for review UIs or manual edits, then send the reviewed list back through `pdf-acroform/apply` to regenerate the PDF without rerunning visual detection.

### Document Outline

Rebuilds the structural-outline artifact for an already-completed job as a follow-up operation, instead of re-running the whole extraction. This is the same outline-building logic used by the `DOCUMENT_OUTLINE` map-reduce split type — see [Map-Reduce Extraction](https://docs.docudevs.ai/docs/core/map-reduce-extraction#document-outline-split-type) — exposed here as a standalone operation so you can get or regenerate just the outline.

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

## Examples

The examples below start from a completed job: replace `<job-guid>` with the GUID of a job you processed earlier. The cURL tabs poll the operation job until it completes; the Python SDK helpers and the CLI wait for you.

### Error Analysis

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

public class Example {
    public static void main(String[] args) throws Exception {
        DocuDevsClient client = DocuDevsClient.builder()
                .apiKey(System.getenv("API_KEY"))
                .baseUrl("https://api.docudevs.ai")
                .build();

        String jobGuid = "<job-guid>"; // GUID of a completed document job

        client.submitOperation(jobGuid, "error-analysis");

        JsonNode result = client.waitForOperation(
                jobGuid, "error-analysis", WaitOptions.builder().build());
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

### Generative Tasks

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

TASK_JOB_GUID=$(curl -s -S -f -X POST "https://api.docudevs.ai/operation/$PARENT_JOB_GUID/generative-task" \
  -H "Authorization: $API_KEY" \
  -H "Content-Type: application/json" \
  -d '{"prompt": "Summarize the main findings", "model": "DEFAULT"}' | jq -r '.jobGuid')

wait_for_job "$TASK_JOB_GUID"

curl -s -S -f "https://api.docudevs.ai/job/result/$TASK_JOB_GUID" \
  -H "Authorization: $API_KEY" | jq -r '.generated_text'
```

**Python**

```python
parent_job_guid = "<job-guid>"  # GUID of a completed document job

summary = await client.submit_and_wait_for_generative_task(
    parent_job_id=parent_job_guid,
    prompt="Summarize the main findings",
    model="DEFAULT",
)

# The result is a JSON string inside the response object
result_data = json.loads(summary.result)
print(result_data["generated_text"])
```

**Java**

```java
import ai.docudevs.client.WaitOptions;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

ObjectMapper mapper = new ObjectMapper();
String parentJobGuid = "<job-guid>"; // GUID of a completed document job

client.createGenerativeTask(parentJobGuid, "Summarize the main findings", "DEFAULT");

JsonNode result = client.waitForGenerativeTask(
        parentJobGuid, WaitOptions.builder().build());
JsonNode payload = mapper.readTree(result.path("result").asText());
System.out.println(payload.path("generated_text").asText());
```

**CLI**

```bash
PARENT_JOB_GUID="<job-guid>"  # GUID of a completed document job

docudevs operations generative-task "$PARENT_JOB_GUID" \
  --prompt "Summarize the main findings" \
  --model DEFAULT \
  --timeout 120

docudevs operations result "$PARENT_JOB_GUID" --type generative-task
```

### Image Selection

The parent job must have figure artifacts, so process the document with figure extraction (`extractFigures` or `describeFigures`) first.

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

JOB_GUID="<job-guid>"  # GUID of a completed job processed with figure extraction

OPERATION_JOB_GUID=$(curl -s -S -f -X POST "https://api.docudevs.ai/operation" \
  -H "Authorization: $API_KEY" \
  -H "Content-Type: application/json" \
  -d "{
        \"jobGuid\": \"$JOB_GUID\",
        \"type\": \"image-select\",
        \"parameters\": {
          \"customParameters\": {
            \"prompt\": \"Return all diagrams from the document\",
            \"topK\": 5,
            \"matchMode\": \"all\",
            \"useVision\": false
          }
        }
      }" | jq -r '.jobGuid')

wait_for_job "$OPERATION_JOB_GUID"

PAYLOAD=$(curl -s -S -f "https://api.docudevs.ai/job/result/$OPERATION_JOB_GUID" \
  -H "Authorization: $API_KEY")
echo "$PAYLOAD" | jq .

IMAGE_ID=$(echo "$PAYLOAD" | jq -r '.selected[0].id // empty')
if [ -n "$IMAGE_ID" ]; then
  curl -s -S -f "https://api.docudevs.ai/job/figure/$JOB_GUID/$IMAGE_ID" \
    -H "Authorization: $API_KEY" \
    --output selected_figure.png
fi
```

**Python**

```python
job_guid = "<job-guid>"  # GUID of a completed job processed with figure extraction

selection = await client.submit_and_wait_for_image_selection(
    job_guid,
    prompt="Return all diagrams from the document",
    top_k=5,
    match_mode="all",
    use_vision=False,
)

payload = json.loads(selection.result)
print(json.dumps(payload, indent=2))

selected = payload.get("selected", [])
if selected:
    image_bytes = await client.get_figure_image(job_guid, selected[0]["id"])
    if image_bytes:
        with open("selected_figure.png", "wb") as f:
            f.write(image_bytes)
```

**Java**

```java
import ai.docudevs.client.WaitOptions;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.Map;

ObjectMapper mapper = new ObjectMapper();
String jobGuid = "<job-guid>"; // GUID of a completed job processed with figure extraction

client.submitOperation(jobGuid, "image-select", null,
        Map.of(
                "prompt", "Return all diagrams from the document",
                "topK", 5,
                "matchMode", "all",
                "useVision", false));

JsonNode result = client.waitForOperation(
        jobGuid, "image-select", WaitOptions.builder().build());

JsonNode payload = mapper.readTree(result.path("result").asText());
System.out.println(payload.toPrettyString());

JsonNode first = payload.path("selected").get(0);
if (first != null) {
    String imageId = first.path("id").asText();
    byte[] imageBytes = client.getFigureImage(jobGuid, imageId);
    Files.write(Path.of("selected_figure.png"), imageBytes);
}
```

**CLI**

```bash
JOB_GUID="<job-guid>"  # GUID of a completed job processed with figure extraction

docudevs operations submit "$JOB_GUID" \
  --type image-select \
  --parameter prompt="Return all diagrams from the document" \
  --parameter topK=5 \
  --parameter matchMode=all \
  --parameter useVision=false \
  --wait

docudevs operations result "$JOB_GUID" --type image-select
```

### PDF AcroForm Conversion

Run `pdf-acroform` on a completed PDF job to generate a fillable AcroForm PDF, then inspect or edit the generated field definitions.

For the full parameter list and the REST download endpoint, see [PDF AcroForm](./pdf-acroform.md).

The parent job must point to a PDF. The operation also needs rendered page images, so the safest setup is to create the parent job with `ocr="PREMIUM"`, or set `forceOcr` on the operation when thumbnails are missing.

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

PARENT_JOB_GUID="<job-guid>"  # GUID of a completed PDF job whose page images exist (processed with ocr PREMIUM)

OPERATION_JOB_GUID=$(curl -s -S -f -X POST "https://api.docudevs.ai/operation" \
  -H "Authorization: $API_KEY" \
  -H "Content-Type: application/json" \
  -d "{
    \"jobGuid\": \"$PARENT_JOB_GUID\",
    \"type\": \"pdf-acroform\",
    \"parameters\": {
      \"llmType\": \"DEFAULT\",
      \"customParameters\": {
        \"ocr\": \"PREMIUM\",
        \"fieldNameStyle\": \"source_snake_case\",
        \"pageRange\": [1],
        \"minConfidence\": 0.6,
        \"maxFieldsPerPage\": 300
      }
    }
  }" | jq -r '.jobGuid')

wait_for_job "$OPERATION_JOB_GUID"

curl -s -S -f "https://api.docudevs.ai/job/result/$OPERATION_JOB_GUID/pdf-acroform" \
  -H "Authorization: $API_KEY" \
  --output fillable.pdf

echo "Operation job GUID: $OPERATION_JOB_GUID"
```

**Python**

```python
import asyncio
import os

from docudevs.docudevs_client import DocuDevsClient


async def main():
    client = DocuDevsClient(token=os.environ["API_KEY"])

    parent_job_guid = "<job-guid>"  # GUID of a completed PDF job whose page images exist (processed with ocr PREMIUM)

    result = await client.submit_and_wait_for_pdf_acroform_operation(
        parent_job_guid,
        ocr="PREMIUM",
        page_range=[1],
        min_confidence=0.6,
        max_fields_per_page=300,
        timeout=600,
        save_to="fillable.pdf",
    )

    print(f"Operation job GUID: {result.operation_job_guid}")


asyncio.run(main())
```

**Java**

```java
import ai.docudevs.client.PdfAcroformOptions;
import ai.docudevs.client.WaitOptions;
import com.fasterxml.jackson.databind.JsonNode;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.util.List;

String parentJobGuid = "<job-guid>"; // GUID of a completed PDF job whose page images exist (processed with ocr PREMIUM)

JsonNode result = client.submitAndWaitForPdfAcroformOperation(
        parentJobGuid,
        PdfAcroformOptions.builder()
                .ocr("PREMIUM")
                .pageRange(List.of(1))
                .minConfidence(0.6)
                .maxFieldsPerPage(300)
                .build(),
        WaitOptions.builder()
                .timeout(Duration.ofSeconds(600))
                .pollInterval(Duration.ofSeconds(5))
                .build());

// The wait returns the operation job GUID and its JSON result; the PDF is a separate download
String operationJobGuid = result.path("operationJobGuid").asText();
Files.write(Path.of("fillable.pdf"), client.downloadPdfAcroform(operationJobGuid));

System.out.println("Operation job GUID: " + operationJobGuid);
```

**CLI**

```bash
PARENT_JOB_GUID="<job-guid>"  # GUID of a completed PDF job whose page images exist (processed with ocr PREMIUM)

docudevs operations pdf-acroform "$PARENT_JOB_GUID" \
  --ocr PREMIUM \
  --page 1 \
  --min-confidence 0.6 \
  --max-fields-per-page 300 \
  --timeout 600 \
  --output fillable.pdf
```

The CLI prints the `operationJobGuid`. To fetch `acroform-metadata` and `pdf-acroform-field-definitions`, or to submit reviewed field definitions back through `pdf-acroform/apply`, use the Python SDK or the HTTP API:

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

PARENT_JOB_GUID="<job-guid>"  # GUID of the completed PDF job that was converted
OPERATION_JOB_GUID="<operation-job-guid>"  # operation job GUID returned by the conversion

curl -s -S -f "https://api.docudevs.ai/job/result/$OPERATION_JOB_GUID/acroform-metadata" \
  -H "Authorization: $API_KEY" | jq '(.fields // [])[:3]'

DEFINITIONS=$(curl -s -S -f "https://api.docudevs.ai/job/result/$OPERATION_JOB_GUID/pdf-acroform-field-definitions" \
  -H "Authorization: $API_KEY")
echo "$DEFINITIONS" | jq '(.fieldDefinitions // [])[:3]'

# Mark every generated field as reviewed and accepted
REVIEWED=$(echo "$DEFINITIONS" | jq '(.fieldDefinitions // []) | map(. + {reviewStatus: "accepted"})')

APPLY_JOB_GUID=$(jq -n \
  --arg source "$OPERATION_JOB_GUID" \
  --argjson fields "$REVIEWED" \
  '{sourceOperationGuid: $source, fieldDefinitions: $fields, minConfidence: 0.0, maxFieldsPerPage: 300}' |
  curl -s -S -f -X POST "https://api.docudevs.ai/job/$PARENT_JOB_GUID/pdf-acroform/apply" \
    -H "Authorization: $API_KEY" \
    -H "Content-Type: application/json" \
    -d @- | jq -r '.jobGuid')

wait_for_job "$APPLY_JOB_GUID"

curl -s -S -f "https://api.docudevs.ai/job/result/$APPLY_JOB_GUID/pdf-acroform" \
  -H "Authorization: $API_KEY" \
  --output reviewed.pdf

echo "Operation job GUID: $APPLY_JOB_GUID"
```

**Python**

```python
parent_job_guid = "<job-guid>"  # GUID of the completed PDF job that was converted
operation_job_guid = "<operation-job-guid>"  # operation job GUID returned by the conversion

generated_metadata = await client.get_acroform_metadata(operation_job_guid)
definitions_artifact = await client.get_pdf_acroform_field_definitions(operation_job_guid)

print(json.dumps(generated_metadata.get("fields", [])[:3], indent=2))
print(json.dumps(definitions_artifact.get("fieldDefinitions", [])[:3], indent=2))

# Mark every generated field as reviewed and accepted
reviewed_fields = [dict(field) for field in definitions_artifact.get("fieldDefinitions", [])]
for field in reviewed_fields:
    field["reviewStatus"] = "accepted"

applied = await client.submit_and_wait_for_pdf_acroform_apply_operation(
    parent_job_guid,
    field_definitions=reviewed_fields,
    source_operation_guid=operation_job_guid,
    save_to="reviewed.pdf",
)

print(f"Operation job GUID: {applied.operation_job_guid}")
```

**Java**

```java
import ai.docudevs.client.WaitOptions;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

String parentJobGuid = "<job-guid>"; // GUID of the completed PDF job that was converted
String operationJobGuid = "<operation-job-guid>"; // operation job GUID returned by the conversion

ObjectMapper mapper = new ObjectMapper();

JsonNode generatedMetadata = client.getAcroformMetadata(operationJobGuid);
JsonNode definitionsArtifact = client.getPdfAcroformFieldDefinitions(operationJobGuid);

ArrayNode firstMetadataFields = mapper.createArrayNode();
ArrayNode firstDefinitions = mapper.createArrayNode();
generatedMetadata.path("fields").forEach(field -> {
    if (firstMetadataFields.size() < 3) {
        firstMetadataFields.add(field);
    }
});
definitionsArtifact.path("fieldDefinitions").forEach(definition -> {
    if (firstDefinitions.size() < 3) {
        firstDefinitions.add(definition);
    }
});
System.out.println(firstMetadataFields.toPrettyString());
System.out.println(firstDefinitions.toPrettyString());

// Mark every generated field as reviewed and accepted
List<Map<String, Object>> reviewedFields = new ArrayList<>();
for (JsonNode definition : definitionsArtifact.path("fieldDefinitions")) {
    Map<String, Object> field = mapper.convertValue(definition, new TypeReference<Map<String, Object>>() { });
    field.put("reviewStatus", "accepted");
    reviewedFields.add(field);
}

JsonNode applied = client.submitAndWaitForPdfAcroformApplyOperation(
        parentJobGuid,
        reviewedFields,
        operationJobGuid,
        0.0,
        300,
        WaitOptions.builder()
                .timeout(Duration.ofSeconds(180))
                .pollInterval(Duration.ofSeconds(5))
                .build());

// The wait returns the operation job GUID and its JSON result; the PDF is a separate download
String appliedJobGuid = applied.path("operationJobGuid").asText();
Files.write(Path.of("reviewed.pdf"), client.downloadPdfAcroform(appliedJobGuid));

System.out.println("Operation job GUID: " + appliedJobGuid);
```

The generated PDF from either the initial conversion or the reviewed apply flow is always downloaded through `GET /job/result/{guid}/pdf-acroform`.

### Document Outline

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
  -d "{
    \"jobGuid\": \"$JOB_GUID\",
    \"type\": \"document-outline\",
    \"parameters\": {
      \"customParameters\": {
        \"pagesPerChunk\": 3,
        \"overlapPages\": 1,
        \"parallelProcessing\": true
      }
    }
  }" | jq -r '.jobGuid')

wait_for_job "$OPERATION_JOB_GUID"

# The job result, then the outline artifact
curl -s -S -f "https://api.docudevs.ai/job/result/$OPERATION_JOB_GUID" \
  -H "Authorization: $API_KEY" | jq .

curl -s -S -f "https://api.docudevs.ai/job/result/$OPERATION_JOB_GUID/document-outline" \
  -H "Authorization: $API_KEY" | jq .
```

**Python**

```python
job_guid = "<job-guid>"  # GUID of a completed document job

outline_result = await client.submit_and_wait_for_document_outline_operation(
    job_guid,
    pages_per_chunk=3,
    overlap_pages=1,
    parallel_processing=True,
    timeout=180,
)

print(outline_result.result)
print(outline_result.document_outline)
```

**Java**

```java
import ai.docudevs.client.DocumentOutlineOptions;
import ai.docudevs.client.WaitOptions;
import com.fasterxml.jackson.databind.JsonNode;
import java.time.Duration;

String jobGuid = "<job-guid>"; // GUID of a completed document job

JsonNode outlineResult = client.submitAndWaitForDocumentOutlineOperation(
        jobGuid,
        DocumentOutlineOptions.builder()
                .pagesPerChunk(3)
                .overlapPages(1)
                .parallelProcessing(true)
                .build(),
        WaitOptions.builder()
                .timeout(Duration.ofSeconds(180))
                .pollInterval(Duration.ofSeconds(5))
                .build());

System.out.println(outlineResult.path("result"));
System.out.println(outlineResult.path("documentOutline"));
```

If the job already has an outline artifact (for example, a job originally processed with `splitType=DOCUMENT_OUTLINE`), fetch it directly without submitting a new operation:

**cURL**

```bash
JOB_GUID="<job-guid>"  # GUID of a job that already has an outline artifact

curl -s -S -f "https://api.docudevs.ai/job/result/$JOB_GUID/document-outline" \
  -H "Authorization: $API_KEY" | jq .
```

**Python**

```python
job_guid = "<job-guid>"  # GUID of a job that already has an outline artifact

outline = await client.get_document_outline(job_guid)
print(json.dumps(outline, indent=2))
```

**Java**

```java
import com.fasterxml.jackson.databind.JsonNode;

String jobGuid = "<job-guid>"; // GUID of a job that already has an outline artifact

JsonNode outline = client.getDocumentOutline(jobGuid);
System.out.println(outline.toPrettyString());
```

In Python, `wait_until_ready_with_document_outline` waits for a still-running job and returns its result and outline in one call.

`pagesPerChunk` must be at least 1, `overlapPages` must be at least 0, and `overlapPages` must be less than `pagesPerChunk`. There is no CLI command and no Java SDK method for this operation: use the Python SDK or the raw HTTP API directly.

## Advanced Usage

### Document Question Answering

You can use Generative Tasks to ask specific questions about a processed document.

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

### Advanced Parameters

Set `llmType` to choose the model tier for an operation. Operation-specific settings go in `customParameters`; the parameters each operation accepts are described with that operation above.

**cURL**

```bash
JOB_GUID="<job-guid>"  # GUID of a completed document job

curl -s -S -f -X POST "https://api.docudevs.ai/operation" \
  -H "Authorization: $API_KEY" \
  -H "Content-Type: application/json" \
  -d "{
    \"jobGuid\": \"$JOB_GUID\",
    \"type\": \"error-analysis\",
    \"parameters\": {
      \"llmType\": \"HIGH\"
    }
  }" | jq '{jobGuid, status}'
```

**Python**

```python
job_guid = "<job-guid>"  # GUID of a completed document job

response = await client.submit_operation_with_parameters(
    job_guid=job_guid,
    operation_type="error-analysis",
    llm_type="HIGH",  # use the high-quality model tier for the analysis
)

print(f"Operation job GUID: {response.job_guid}")
print(f"Status: {response.status}")
```

**Java**

```java
import com.fasterxml.jackson.databind.JsonNode;
import java.util.Map;

String jobGuid = "<job-guid>"; // GUID of a completed document job

JsonNode response = client.submitOperation(
        jobGuid,
        "error-analysis",
        "HIGH", // use the high-quality model tier for the analysis
        Map.of());

System.out.println("Operation job GUID: " + response.path("jobGuid").asText());
System.out.println("Status: " + response.path("status").asText());
```

**CLI**

```bash
JOB_GUID="<job-guid>"  # GUID of a completed document job

docudevs operations error-analysis "$JOB_GUID" --llm-type HIGH --no-wait
```

## Output Formats

### Error Analysis Output

```json
{
  "extraction_quality": "good",
  "overall_confidence": 0.85,
  "field_analysis": [
    {
      "field_name": "total_amount",
      "extracted_value": "1250.00",
      "text_evidence": "Total due: 1,250.00 EUR",
      "confidence": 0.9,
      "issues": [],
      "suggestions": [],
      "recommend_human_review": false
    },
    {
      "field_name": "invoice_number",
      "extracted_value": "INV-0O42",
      "text_evidence": "Invoice no. INV-0042",
      "confidence": 0.6,
      "issues": ["Letter O read instead of digit 0"],
      "suggestions": ["Normalise the invoice number format after extraction"],
      "recommend_human_review": true
    }
  ],
  "ocr_issues": [
    {
      "type": "character_confusion",
      "description": "The digit 0 and the letter O are confused in the invoice number",
      "original_text_location": "Invoice no. INV-0042",
      "confidence": 0.7
    }
  ],
  "derived_data_issues": [],
  "recommendations": [
    "Review fields flagged with recommend_human_review",
    "Consider using higher quality OCR settings"
  ]
}
```

`extraction_quality` is one of `excellent`, `good`, `fair`, `poor` or `failed`. `field_analysis` has one entry per extracted field, `ocr_issues` and `derived_data_issues` list problems found in the source text and in calculated values, and `recommendations` is a list of strings.

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
- **Map-Reduce Re-processing**: Run map-reduce on a previously processed document without re-uploading or re-running OCR (in Python, `submit_and_wait_for_map_reduce(parent_job_id=...)`). See [Map-Reduce Extraction](https://docs.docudevs.ai/docs/core/map-reduce-extraction#re-running-map-reduce-on-an-existing-job) for details.
