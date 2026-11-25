---
title: AI Document Analysis
description: Generate summaries, classify documents, answer questions, and transform content using AI generative tasks after OCR.
sidebar_position: 3
---

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

OCR the document first, then submit a generative task whose parent is the OCR job. The task result carries the generated text.

<Example name="quick-start/analyze-document" />

### Question Answering

Ask questions about a document that has already been through OCR. Each question is its own generative task on the OCR job, so replace `<job-guid>` with the GUID of the OCR job from the summarization step.

<Example name="operations/ask-document-questions" />

## Advanced Use Cases

### Several Analyses of One Document

OCR runs once; every analysis is a separate generative task on the OCR job. The example runs four different prompts and collects the answers by name.

<Example name="extraction/analyze-document-with-several-prompts" />

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

A generative task accepts optional `model`, `temperature` and `maxTokens` fields next to the prompt. In the Python SDK they are the `model`, `temperature` and `max_tokens` arguments of `submit_and_wait_for_generative_task`; the CLI has `--model`, `--temperature` and `--max-tokens`.

```json
{
  "prompt": "Summarize the main points",
  "model": "DEFAULT",
  "temperature": 0.1,
  "maxTokens": 150
}
```

- **`model`**: the model to use; `DEFAULT` selects the platform default.
- **`temperature`**: from `0.0` (conservative, factual) to `1.0` (creative, varied).
- **`maxTokens`**: the maximum length of the generated text. Use a small value for brief answers and a larger one for detailed analysis.

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

Every generative task is an independent job under its OCR parent, so you can OCR many documents first, submit the tasks for each document, and wait for them concurrently. To process a large collection of similar documents with one configuration, use [Batch Processing](/docs/core/batch-processing) instead.

## Integration with Other Features

### Combine with Error Analysis

Run error analysis on a completed extraction job to review its quality, and use generative tasks on the same document for a free-form quality review. Replace `<job-guid>` with the GUID of the extraction job.

<Example name="operations/error-analysis" />

## Next Steps

- [Learn about Operations](/docs/advanced/operations) - Explore all operation types including error analysis
- [Work with Cases](/docs/advanced/cases) - Organize and batch process documents
- [SDK Reference](/docs/reference/sdk-methods) - Complete API documentation
- [Best Practices](/docs/integration/best-practices) - Production deployment guidelines
