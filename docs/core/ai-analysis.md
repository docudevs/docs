---
title: AI Document Analysis
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

1. **OCR Processing**: First, extract text from your document using OCR
2. **AI Analysis**: Submit generative tasks with custom prompts
3. **Intelligent Response**: Get AI-generated insights based on document content

## Basic Usage

### Document Summarization

```python
import asyncio
from docudevs.docudevs_client import DocuDevsClient

async def summarize_document():
    client = DocuDevsClient(token="your-api-key")
    
    # Step 1: Process document with OCR
    with open("report.pdf", "rb") as f:
        document_data = f.read()
    
    ocr_job_id = await client.submit_and_ocr_document(
        document=document_data,
        document_mime_type="application/pdf",
        ocr="PREMIUM",
        ocr_format="markdown"
    )
    
    # Wait for OCR to complete
    await client.wait_until_ready(ocr_job_id)
    
    # Step 2: Generate summary
    summary = await client.submit_and_wait_for_generative_task(
        parent_job_id=ocr_job_id,
        prompt="Summarize this document in 2-3 paragraphs, highlighting the key findings and recommendations."
    )
    
    # Parse and display result
    import json
    result_data = json.loads(summary.result)
    print("Document Summary:")
    print(result_data['generated_text'])

asyncio.run(summarize_document())
```

### Question Answering

```python
async def ask_questions_about_document():
    client = DocuDevsClient(token="your-api-key")
    
    # Process document with OCR first
    with open("contract.pdf", "rb") as f:
        document_data = f.read()
    
    ocr_job_id = await client.submit_and_ocr_document(
        document=document_data,
        document_mime_type="application/pdf"
    )
    
    await client.wait_until_ready(ocr_job_id)
    
    # Ask multiple questions
    questions = [
        "What is the contract start date and end date?",
        "Who are the parties involved in this contract?",
        "What are the main obligations of each party?",
        "Are there any penalty clauses or termination conditions?"
    ]
    
    for question in questions:
        answer = await client.submit_and_wait_for_generative_task(
            parent_job_id=ocr_job_id,
            prompt=f"Based on this document, answer the following question: {question}"
        )
        
    result_data = json.loads(answer.result)
        print(f"Q: {question}")
        print(f"A: {result_data['generated_text']}\n")

asyncio.run(ask_questions_about_document())
```

## Advanced Use Cases

### Multi-Language Document Analysis

```python
async def analyze_multilingual_document():
    client = DocuDevsClient(token="your-api-key")
    
    # Process document
    ocr_job_id = await client.submit_and_ocr_document(
        document=document_data,
        document_mime_type="application/pdf"
    )
    
    await client.wait_until_ready(ocr_job_id)
    
    # Generate analysis in multiple languages
    languages = ["English", "Spanish", "French"]
    
    for language in languages:
        analysis = await client.submit_and_wait_for_generative_task(
            parent_job_id=ocr_job_id,
            prompt=f"Provide a summary of this document in {language}. Include the main points and conclusions."
        )
        
    result_data = json.loads(analysis.result)
        print(f"Summary in {language}:")
        print(result_data['generated_text'])
        print("---\n")
```

### Document Classification and Routing

```python
async def classify_document(document_path):
    client = DocuDevsClient(token="your-api-key")
    
    with open(document_path, "rb") as f:
        document_data = f.read()
    
    # Process with OCR
    ocr_job_id = await client.submit_and_ocr_document(
        document=document_data,
        document_mime_type="application/pdf"
    )
    
    await client.wait_until_ready(ocr_job_id)
    
    # Classify document type
    classification = await client.submit_and_wait_for_generative_task(
        parent_job_id=ocr_job_id,
        prompt="""Classify this document into one of these categories:
        - Invoice
        - Contract
        - Report
        - Form
        - Receipt
        - Other
        
        Respond with just the category name and a brief explanation of why."""
    )
    
    result_data = json.loads(classification.result)
    return result_data['generated_text']

# Batch classify multiple documents
document_paths = ["doc1.pdf", "doc2.pdf", "doc3.pdf"]
for path in document_paths:
    classification = await classify_document(path)
    print(f"{path}: {classification}")
```

### Content Transformation

```python
async def transform_document_content():
    client = DocuDevsClient(token="your-api-key")
    
    # Process academic paper
    with open("research_paper.pdf", "rb") as f:
        document_data = f.read()
    
    ocr_job_id = await client.submit_and_ocr_document(
        document=document_data,
        document_mime_type="application/pdf"
    )
    
    await client.wait_until_ready(ocr_job_id)
    
    # Transform into different formats
    transformations = [
        {
            "name": "Executive Summary",
            "prompt": "Create an executive summary suitable for business stakeholders, focusing on practical implications and key findings."
        },
        {
            "name": "Technical Abstract",
            "prompt": "Write a technical abstract that highlights the methodology, key results, and conclusions for a scientific audience."
        },
        {
            "name": "Plain Language Summary",
            "prompt": "Explain the main findings and importance of this research in simple language that a general audience can understand."
        }
    ]
    
    results = {}
    for transformation in transformations:
        result = await client.submit_and_wait_for_generative_task(
            parent_job_id=ocr_job_id,
            prompt=transformation["prompt"]
        )
        
    result_data = json.loads(result.result)
        results[transformation["name"]] = result_data['generated_text']
    
    return results
```

## Configuration Options

### Model Selection

Choose the appropriate AI model for your task:

```python
# For quick analysis (faster, lower cost)
await client.submit_and_wait_for_generative_task(
    parent_job_id=ocr_job_id,
    prompt="Quick summary of main points",
    model="DEFAULT"
)

# For detailed analysis (slower, higher quality)
await client.submit_and_wait_for_generative_task(
    parent_job_id=ocr_job_id,
    prompt="Detailed analysis with insights and recommendations",
    model="HIGH"
)
```

### Temperature and Creativity

Control the creativity of responses:

```python
# Conservative, factual responses
await client.submit_and_wait_for_generative_task(
    parent_job_id=ocr_job_id,
    prompt="Extract factual information",
    temperature=0.1
)

# Creative, varied responses
await client.submit_and_wait_for_generative_task(
    parent_job_id=ocr_job_id,
    prompt="Generate creative insights and ideas",
    temperature=0.8
)
```

### Response Length

Control the length of generated content:

```python
# Brief responses
await client.submit_and_wait_for_generative_task(
    parent_job_id=ocr_job_id,
    prompt="Brief summary",
    max_tokens=150
)

# Detailed responses
await client.submit_and_wait_for_generative_task(
    parent_job_id=ocr_job_id,
    prompt="Comprehensive analysis",
    max_tokens=1000
)
```

## Best Practices

### Effective Prompt Writing

**Be Specific and Clear**

```python
# Good prompt
prompt = "Extract the contract start date, end date, parties involved, and key obligations. Format as a bulleted list."

# Avoid vague prompts
prompt = "Tell me about this document."
```

**Provide Context and Format Instructions**

```python
prompt = """Analyze this financial report and provide:
1. Revenue trends (increase/decrease and percentages)
2. Key financial metrics (profit margin, ROI, etc.)  
3. Risk factors mentioned
4. Management outlook/guidance

Format your response with clear headings for each section."""
```

### Error Handling

```python
async def safe_document_analysis(document_data, mime_type, prompt):
    client = DocuDevsClient(token="your-api-key")
    
    try:
        # Process with OCR
        ocr_job_id = await client.submit_and_ocr_document(
            document=document_data,
            document_mime_type=mime_type
        )
        
        ocr_result = await client.wait_until_ready(ocr_job_id, timeout=120)
        
        # Basic success check
        if not ocr_result:
            return None, "OCR processing failed"
        
        # Submit generative task
        analysis = await client.submit_and_wait_for_generative_task(
            parent_job_id=ocr_job_id,
            prompt=prompt,
            timeout=180
        )
        
        if analysis:
            result_data = json.loads(analysis.result)
            return result_data['generated_text'], None
        else:
            return None, "Analysis generation failed"
            
    except Exception as e:
        return None, f"Error during analysis: {str(e)}"

# Usage
text, error = await safe_document_analysis(
    document_data, 
    "application/pdf", 
    "Summarize key points"
)

if error:
    print(f"Analysis failed: {error}")
else:
    print(f"Analysis result: {text}")
```

### Batch Processing

```python
async def analyze_document_batch(documents, prompts):
    """Analyze multiple documents with multiple prompts efficiently"""
    client = DocuDevsClient(token="your-api-key")
    
    # Step 1: Process all documents with OCR
    ocr_jobs = []
    for doc_data, mime_type in documents:
        job_id = await client.submit_and_ocr_document(
            document=doc_data,
            document_mime_type=mime_type
        )
        ocr_jobs.append(job_id)
    
    # Step 2: Wait for all OCR jobs to complete
    for job_id in ocr_jobs:
        await client.wait_until_ready(job_id)
    
    # Step 3: Submit all generative tasks
    analysis_tasks = []
    for job_id in ocr_jobs:
        for prompt in prompts:
            task = client.submit_and_wait_for_generative_task(
                parent_job_id=job_id,
                prompt=prompt
            )
            analysis_tasks.append(task)
    
    # Step 4: Wait for all analyses to complete
    results = await asyncio.gather(*analysis_tasks)
    
    # Step 5: Organize results
    organized_results = []
    result_index = 0
    for doc_index, job_id in enumerate(ocr_jobs):
        doc_results = {}
        for prompt_index, prompt in enumerate(prompts):
            result_data = json.loads(results[result_index].result)
            doc_results[f"prompt_{prompt_index}"] = {
                "prompt": prompt,
                "response": result_data['generated_text']
            }
            result_index += 1
        organized_results.append(doc_results)
    
    return organized_results
```

## Common Patterns

### Document Intelligence Workflow

```python
async def document_intelligence_pipeline(document_path):
    """Complete document intelligence workflow"""
    client = DocuDevsClient(token="your-api-key")
    
    with open(document_path, "rb") as f:
        document_data = f.read()
    
    # 1. OCR Processing
    ocr_job_id = await client.submit_and_ocr_document(
        document=document_data,
        document_mime_type="application/pdf",
        ocr="PREMIUM",
        ocr_format="markdown"
    )
    
    await client.wait_until_ready(ocr_job_id)
    
    # 2. Parallel analysis tasks
    tasks = [
        # Document classification
        client.submit_and_wait_for_generative_task(
            parent_job_id=ocr_job_id,
            prompt="What type of document is this? (invoice, contract, report, form, etc.)"
        ),
        
        # Key information extraction
        client.submit_and_wait_for_generative_task(
            parent_job_id=ocr_job_id,
            prompt="Extract the most important information: dates, names, amounts, addresses, and key terms."
        ),
        
        # Summary generation
        client.submit_and_wait_for_generative_task(
            parent_job_id=ocr_job_id,
            prompt="Provide a concise summary of this document's main content and purpose."
        ),
        
        # Sentiment/tone analysis
        client.submit_and_wait_for_generative_task(
            parent_job_id=ocr_job_id,
            prompt="Analyze the tone and sentiment of this document. Is it formal, informal, positive, negative, urgent, etc.?"
        )
    ]
    
    # Execute all tasks concurrently
    doc_type, key_info, summary, sentiment = await asyncio.gather(*tasks)
    
    # Parse results
    import json
    return {
        'document_type': json.loads(doc_type.result)['generated_text'],
        'key_information': json.loads(key_info.result)['generated_text'],
        'summary': json.loads(summary.result)['generated_text'],
        'sentiment_analysis': json.loads(sentiment.result)['generated_text']
    }

# Usage
intelligence = await document_intelligence_pipeline("contract.pdf")
print(f"Document Type: {intelligence['document_type']}")
print(f"Summary: {intelligence['summary']}")
print(f"Key Info: {intelligence['key_information']}")
print(f"Sentiment: {intelligence['sentiment_analysis']}")
```

## Integration with Other Features

### Combine with Cases

```python
async def analyze_case_documents(case_id):
    """Analyze all documents in a case"""
    client = DocuDevsClient(token="your-api-key")
    
    # Get case documents
    case_docs = await client.get_case_documents(case_id)
    
    # Process each document
    analyses = []
    for doc in case_docs:
        # OCR first
        ocr_job = await client.submit_and_ocr_document(
            document=doc.content,
            document_mime_type=doc.mime_type
        )
        await client.wait_until_ready(ocr_job)
        
        # Analyze
        analysis = await client.submit_and_wait_for_generative_task(
            parent_job_id=ocr_job,
            prompt="Analyze this document and identify key themes, entities, and important information."
        )
        
        result_data = json.loads(analysis.result)
        analyses.append({
            'document_name': doc.filename,
            'analysis': result_data['generated_text']
        })
    
    return analyses
```

### Combine with Error Analysis

```python
async def comprehensive_document_review(document_data, mime_type):
    """Complete document review including AI analysis and error checking"""
    client = DocuDevsClient(token="your-api-key")
    
    # First, do structured extraction
    extraction_job_id = await client.submit_and_process_document(
        document=document_data,
        document_mime_type=mime_type,
        prompt="Extract all structured data from this document"
    )
    
    extraction_result = await client.wait_until_ready(extraction_job_id)
    
    # Then do error analysis on extraction
    error_analysis = await client.submit_and_wait_for_error_analysis(
        job_guid=extraction_job.parsed.guid
    )
    
    # Also do OCR for AI analysis
    ocr_job = await client.submit_and_ocr_document(
        document=document_data,
        document_mime_type=mime_type
    )
    await client.wait_until_ready(ocr_job)
    
    # AI-powered quality review
    quality_review = await client.submit_and_wait_for_generative_task(
        parent_job_id=ocr_job,
        prompt="Review this document for completeness, accuracy, and potential issues. Highlight any missing information or inconsistencies."
    )
    
    return {
        'extracted_data': extraction_result,
        'error_analysis': error_analysis,
        'ai_quality_review': json.loads(quality_review.result)['generated_text']
    }
```

## Next Steps

- [Learn about Operations](/docs/advanced/operations) - Explore all operation types including error analysis
- [Work with Cases](/docs/advanced/cases) - Organize and batch process documents
- [SDK Reference](/docs/reference/sdk-methods) - Complete API documentation
- [Best Practices](/docs/integration/best-practices) - Production deployment guidelines
