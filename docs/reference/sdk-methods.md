---
title: SDK Methods
description: Complete Python SDK reference for DocuDevs including document processing, pipeline mode, batch operations, cases, templates, configurations, OCR, map-reduce, and agent chat methods.
sidebar_position: 4
---

Complete reference for the DocuDevs Python SDK (`docudevs-sdk`).

## Client Initialization

### DocuDevsClient

Initialize the client with your API key.

```python
from docudevs.docudevs_client import DocuDevsClient
import os

client = DocuDevsClient(
    api_url="https://api.docudevs.ai",  # Optional, defaults to production
    token=os.getenv("DOCUDEVS_API_KEY") # Required
)
```

## Schema Helpers

Convenience functions for building JSON Schema strings from Pydantic models or dicts.

### json_schema

Convert a Pydantic model or dict to a JSON Schema string. Use for single-object extraction.

```python
from docudevs import json_schema
from pydantic import BaseModel, Field

class Invoice(BaseModel):
    number: str = Field(description="Invoice number")
    total: float

# From a Pydantic model
schema = json_schema(Invoice)

# From a dict
schema = json_schema({"type": "object", "properties": {"name": {"type": "string"}}})
```

### array_schema

Wrap a Pydantic model or dict as an array schema. Use for map-reduce extraction where each chunk produces multiple items.

```python
from docudevs import array_schema
from pydantic import BaseModel

class LineItem(BaseModel):
    sku: str
    description: str
    quantity: int

# Produces {"type": "array", "items": <LineItem schema>}
schema = array_schema(LineItem)

# From a dict
schema = array_schema({"type": "object", "properties": {"sku": {"type": "string"}}})
```

Both helpers accept:
- **Pydantic model classes** — calls `model_json_schema()` automatically
- **Dicts** — used as-is

## Document Processing

### submit_and_process_document

Upload and process a document for structured data extraction. Use `extract_figures=True` to store figure images and metadata. For fillable PDFs, `acro_form_metadata=True` stores AcroForm metadata as a separate job artifact that you can fetch later with `get_acroform_metadata(...)`.

```python
job_guid = await client.submit_and_process_document(
    document=document_bytes,
    document_mime_type="application/pdf",
    prompt="Extract invoice data",
    schema={...},  # Optional JSON schema
    ocr="PREMIUM", # Optional: DEFAULT, PREMIUM, LOW
    llm="HIGH",  # Optional: DEFAULT, MINI, HIGH
    extract_figures=True,
    acro_form_metadata=True,
    source_locations=True,
    source_location_granularity="block",
)
```

### submit_and_process_document_with_configuration

Process a document using a saved configuration.

```python
job_guid = await client.submit_and_process_document_with_configuration(
    document=document_bytes,
    document_mime_type="application/pdf",
    configuration_name="invoice-config"
)
```

### submit_and_ocr_document

Process a document with OCR only (no structured extraction). Use `extract_figures=True` to store figure images and metadata.

```python
job_guid = await client.submit_and_ocr_document(
    document=document_bytes,
    document_mime_type="application/pdf",
    ocr="PREMIUM",
    ocr_format="markdown", # markdown, plain, jsonl (for Excel)
    describe_figures=True,
    extract_figures=True
)
```

### analyze_document

Analyze document structure and return a job GUID.

```python
job_guid = await client.analyze_document(
    document=document_bytes,
    document_mime_type="application/pdf",
    ocr="PREMIUM"
)
```

### wait_until_ready

Wait for a job to complete and retrieve the result.

```python
result = await client.wait_until_ready(
    guid=job_guid,
    timeout=180,
    poll_interval=5.0,
    result_format="json" # json, csv, excel, or None (legacy object)
)
```

### Low-Level Document Processing

These primitives back the `submit_and_*` convenience helpers above. Use them directly when you need to separate upload from processing, or drive a custom flow.

### upload_document

Upload a single document and return the raw response (`parsed.guid` on success).

```python
response = await client.upload_document(body=UploadDocumentBody(document=file_obj))
guid = response.parsed.guid
```

### upload_files

Upload multiple files in one request.

```python
response = await client.upload_files(body=UploadFilesBody(...))
```

### process_document

Process an already-uploaded document.

```python
await client.process_document(guid, body=upload_command, depends_on=None)
```

`depends_on` is an optional parent job GUID; when set, the scheduler waits for the parent job before dispatching this one.

### process_document_with_configuration

Process an already-uploaded document using a named configuration.

```python
await client.process_document_with_configuration(guid, configuration="invoice-config")
```

### ocr_document

Process an already-uploaded document with OCR-only mode.

```python
await client.ocr_document(guid, body=ocr_command, ocr_format="markdown")
```

### generate_schema

Generate a JSON schema from a sample document using AI. Returns a job GUID whose result (via `wait_until_ready` or `result_json`) is the generated schema as a JSON string.

```python
job_guid = await client.generate_schema(
    document=sample_bytes,
    document_mime_type="application/pdf",
    instructions="Extract invoice header fields",
)
```

### analyze_document_and_wait

Analyze document structure and wait for the result in one call (combines `analyze_document` and `wait_until_ready`).

```python
result = await client.analyze_document_and_wait(
    document=document_bytes,
    document_mime_type="application/pdf",
    ocr="PREMIUM",
    timeout=180,
)
```

## Job Results and Status

### status

Get raw job status.

```python
response = await client.status(guid)
```

### result

Get the raw (legacy) job result response.

```python
response = await client.result(guid)
```

### result_json

Get a job result explicitly as JSON via `/job/result/{uuid}/json`. Falls back to the legacy result endpoint on a 404 (older server) or 415 (non-JSON result).

```python
data = await client.result_json(guid)
```

### result_csv

Get a job result as CSV text via `/job/result/{uuid}/csv`. Raises if the result is not JSON-backed (415).

```python
csv_text = await client.result_csv(guid)
```

### result_excel

Get a job result as XLSX bytes via `/job/result/{uuid}/excel`. Pass `configuration` to inject the result into a configuration's uploaded Excel template.

```python
xlsx_bytes = await client.result_excel(guid, save_to="result.xlsx", configuration="invoice-config")
```

### get_source_locations

Get resolved source locations for a structured-result job (requires `source_locations=True` at submission).

```python
locations = await client.get_source_locations(guid)
```

### wait_until_ready_with_source_locations

Wait for completion, then return both the JSON result and the source-location artifact as a `StructuredResultWithSources(result=..., source_locations=...)`.

```python
bundle = await client.wait_until_ready_with_source_locations(guid, timeout=180)
print(bundle.result, bundle.source_locations)
```

### submit_and_wait_for_document_with_source_locations

Convenience helper: submit a document with `source_locations=True`, then wait and return both the result and source-location artifact.

```python
bundle = await client.submit_and_wait_for_document_with_source_locations(
    document=document_bytes,
    document_mime_type="application/pdf",
    prompt="Extract invoice data",
)
```

## Workbook

Workbook artifacts are published by `WORKBOOK` extraction-mode jobs (single XLSX input, no target schema). See [Workbook Normalization](/docs/core/workbook-normalization) for the extraction-mode contract.

### get_workbook_plan

Fetch the caller-inspectable transformation plan for a workbook result.

```python
plan = await client.get_workbook_plan(guid)
```

### get_workbook_source_map

Fetch the caller-inspectable source map for a workbook result.

```python
source_map = await client.get_workbook_source_map(guid)
```

### get_workbook_summary

Fetch the published summary artifact for a workbook result.

```python
summary = await client.get_workbook_summary(guid)
```

## Client Pipelines

Client pipelines run a hybrid graph: remote DocuDevs segments interleaved with local Python callables that execute in your own process. See [Client Pipelines](/docs/core/client-pipelines) for the execution model and constraints.

### run_client_pipeline

Run a hybrid client pipeline end to end: uploads the document, then executes the graph, calling back into local functions between remote segments. Not resumable — if the process stops, a later attempt runs the graph from the beginning.

```python
result = await client.run_client_pipeline(
    document=document_bytes,
    document_mime_type="application/pdf",
    pipeline=my_hybrid_pipeline,
    timeout=900.0,
    poll_interval=2.0,
    on_event=None,
)
```

## Pipeline Processing

Pipeline mode uploads one document, runs one shared OCR pass, and executes a graph of nodes. Use the SDK builder so dependencies, source paths, and final outputs refer to previous nodes directly.

### process_pipeline_document

Upload and process a document with a pipeline builder definition.

```python
from docudevs import P, Pipeline

pipeline = Pipeline().ocr(mode="AUTO")
extract_data = pipeline.extract(
    "extract_data",
    source=P.ocr.content,
    prompt="Extract the requested fields.",
    schema={
        "type": "object",
        "properties": {"name": {"type": "string"}},
    },
)
pipeline.final(
    "final_result",
    depends_on=[extract_data],
    output=extract_data.result,
)

job_guid = await client.process_pipeline_document(
    document=document_bytes,
    document_mime_type="application/pdf",
    pipeline=pipeline,
    ocr="AUTO",
    trace=True
)

result = await client.wait_until_ready(job_guid, result_format="json")
nodes = await client.get_pipeline_nodes(job_guid)
```

### process_uploaded_pipeline_document

Run pipeline processing on a document that was already uploaded.

```python
await client.process_uploaded_pipeline_document(
    guid=uploaded_guid,
    pipeline=pipeline,
    mime_type="application/pdf",
    ocr="AUTO"
)
```

### save_pipeline_configuration and get_pipeline_configuration

Save and retrieve named pipeline configurations.

```python
await client.save_pipeline_configuration(
    "router-pipeline",
    pipeline=pipeline,
    mime_type="application/pdf",
    ocr="AUTO"
)

saved_pipeline = await client.get_pipeline_configuration("router-pipeline")
```

### get_pipeline_nodes

Read node-level runtime status from a pipeline job.

```python
nodes = await client.get_pipeline_nodes(job_guid)
for node in nodes:
    print(node["id"], node["status"])
```

For the full pipeline JSON contract, node types, and branching examples, see [Pipeline Mode](/docs/reference/pipeline-mode).

### build_upload_command_pipeline

Build the raw `UploadCommand` for pipeline extraction without submitting it. Used internally by `process_pipeline_document` and `process_uploaded_pipeline_document`; call it directly when you need the command object itself (for example to save it as a configuration with `save_pipeline_configuration`).

```python
command = client.build_upload_command_pipeline(
    pipeline=pipeline,
    mime_type="application/pdf",
    ocr="AUTO",
)
```

## Batch Processing

### submit_and_process_batch

Upload and process multiple documents as a batch.

```python
batch_guid = await client.submit_and_process_batch(
    documents=[file1_bytes, file2_bytes],
    document_mime_type="application/pdf",
    prompt="Extract data",
    max_concurrency=5
)
```

### submit_and_process_batch_with_configuration

Upload and process multiple documents as a batch using a saved configuration.

```python
batch_guid = await client.submit_and_process_batch_with_configuration(
    documents=[file1_bytes, file2_bytes],
    document_mime_type="application/pdf",
    configuration_name="invoice-config",
    max_concurrency=5,
)
```

### Low-Level Batch Processing

These primitives back the `submit_and_process_batch*` convenience helpers above.

### create_batch

Create a new batch job and return its GUID.

```python
batch_guid = await client.create_batch(max_concurrency=5)
```

### upload_batch_document

Upload a single document into an existing batch.

```python
info = await client.upload_batch_document(batch_guid, document_bytes, "application/pdf", file_name="invoice.pdf")
```

### process_batch

Finalize and start processing a batch that has already had documents uploaded.

```python
await client.process_batch(
    batch_guid,
    prompt="Extract data",
    schema="",
    mime_type="application/pdf",
)
```

### process_batch_with_configuration

Finalize and start processing a batch using a saved configuration.

```python
await client.process_batch_with_configuration(batch_guid, "invoice-config")
```

### schedule_batch

Call the concurrency-aware scheduling endpoint for a batch explicitly.

```python
await client.schedule_batch(batch_guid)
```

## Lookup Files

Lookup files are reference data (for example a product catalog or vendor list) attached to a configuration or a single batch so extraction prompts can resolve against it. See [Lookup Files](/docs/core/lookup-files) for the extraction-time behavior.

### upload_configuration_lookup_file

Attach a lookup file to a named configuration. Applies to every job that uses the configuration until replaced or deleted.

```python
await client.upload_configuration_lookup_file("invoice-config", file_bytes, file_name="vendors.csv")
```

### get_configuration_lookup_file

Download the lookup file attached to a configuration.

```python
data = await client.get_configuration_lookup_file("invoice-config")
```

### delete_configuration_lookup_file

Remove the lookup file attached to a configuration.

```python
await client.delete_configuration_lookup_file("invoice-config")
```

### upload_batch_lookup_file

Attach a lookup file scoped to one batch. Overrides any configuration-level lookup file for that batch only.

```python
await client.upload_batch_lookup_file(batch_guid, file_bytes, file_name="vendors.csv")
```

### get_batch_lookup_file

Download the lookup file attached to a batch.

```python
data = await client.get_batch_lookup_file(batch_guid)
```

### delete_batch_lookup_file

Remove the lookup file attached to a batch.

```python
await client.delete_batch_lookup_file(batch_guid)
```

## Configurations

### save_configuration

Save a named configuration.

```python
from docudevs.models import UploadCommand

config = UploadCommand(
    prompt="Extract invoice data",
    ocr="PREMIUM"
)
await client.save_configuration("invoice-config", config)
```

### list_configurations

List all saved configurations.

```python
configs = await client.list_configurations()
```

### get_configuration

Get details of a specific configuration.

```python
config = await client.get_configuration("invoice-config")
```

### delete_configuration

Delete a configuration.

```python
await client.delete_configuration("invoice-config")
```

### upload_excel_template

Attach an Excel template to a named configuration. When `result_excel(...)` (or `wait_until_ready(..., result_format="excel")`) is later called with that configuration, the result is injected into the template at its configured cell offset, preserving the template's formatting and formulas.

```python
await client.upload_excel_template("invoice-config", template=excel_bytes)
```

## Templates

### upload_template

Upload a new document template.

```python
with open("form.pdf", "rb") as f:
    response = await client.upload_template(
        name="form-template",
        document=f.read(),
        file_name="form.pdf",
        mime_type="application/pdf"
    )
```

### list_templates

List all available templates.

```python
templates = await client.list_templates()
```

### metadata

Get metadata (fields) for a template.

For PDF templates uploaded moments earlier, the raw endpoint may still be preparing metadata. In that case prefer `wait_for_template_metadata(...)`.

```python
meta = await client.metadata("form-template")
```

### wait_for_template_metadata

Wait until PDF template metadata is ready and return the decoded field list.

```python
fields = await client.wait_for_template_metadata(
    "form-template",
    timeout=60,
    poll_interval=1,
)
```

### fill

Fill a template with data.

```python
from docudevs.models import TemplateFillRequest

request = TemplateFillRequest(fields={"name": "John"})
response = await client.fill("form-template", request)
```

### fill_with_retry

Fill a template while retrying transient readiness errors that can happen immediately after upload.

```python
request = TemplateFillRequest(fields={"name": "John"})
response = await client.fill_with_retry(
    "form-template",
    request,
    timeout=30,
    poll_interval=1,
)
```

### delete_template

Delete a template.

```python
await client.delete_template("form-template")
```

### upload_template_images

Upload images (logos, signatures) to blob storage for use in template filling. Returns a dict mapping each image key to its blob path reference, for use with `image_ref(...)` in a `fill(...)` call.

```python
paths = await client.upload_template_images("invoice", {
    "logo": "path/to/logo.png",
    "signature": signature_bytes,
})
```

### image_ref

Static helper. Build an image reference for a `TemplateFillRequest` field from a path returned by `upload_template_images(...)`.

```python
from docudevs.docudevs_client import DocuDevsClient

request = TemplateFillRequest(fields={
    "company_name": "ACME Corp",
    "logo": DocuDevsClient.image_ref(paths["logo"], width_mm=50),
})
await client.fill("invoice", request)
```

## LLM and OCR Providers

Manage bring-your-own LLM and OCR providers and the logical key bindings that route requests to them. See [Bring Your Own LLM](/docs/administration/bring-your-own-llm) and [Bring Your Own OCR](/docs/administration/bring-your-own-ocr) for the provider model and setup steps.

:::warning
`create_llm_provider`, `update_llm_provider`, `create_ocr_provider`, and `update_ocr_provider` are thin convenience wrappers that currently send outdated field names and do not match the current provider contract. Use the generated API functions (see [Bring Your Own LLM](/docs/administration/bring-your-own-llm)) or call the REST API directly instead of these four methods.
:::

### list_llm_providers

List LLM providers for the organization.

```python
providers = await client.list_llm_providers()
```

### create_llm_provider

Create an LLM provider. See the warning above before using this method.

```python
async def create_llm_provider(self, name: str, type_: str, base_url: str | None = None, api_key: str | None = None, model: str | None = None, description: str | None = None)
```

### get_llm_provider

Get a single LLM provider by id.

```python
provider = await client.get_llm_provider(provider_id)
```

### update_llm_provider

Patch update LLM provider fields (only sends provided keys). See the warning above before using this method.

```python
async def update_llm_provider(self, provider_id: int, *, name: str | None = None, base_url: str | None = None, model: str | None = None, description: str | None = None)
```

### delete_llm_provider

Soft delete an LLM provider.

```python
await client.delete_llm_provider(provider_id)
```

### list_llm_keys

List logical LLM key bindings.

```python
keys = await client.list_llm_keys()
```

### update_llm_key_binding

Assign or clear the provider bound to a logical LLM key. Pass `provider_id=None` to clear the binding.

```python
await client.update_llm_key_binding("DEFAULT", provider_id)
```

### list_ocr_providers

List OCR providers for the organization.

```python
providers = await client.list_ocr_providers()
```

### create_ocr_provider

Create an OCR provider (Azure Document Intelligence configuration). See the warning above before using this method.

```python
async def create_ocr_provider(self, name: str, endpoint: str | None = None, api_key: str | None = None, model: str | None = None, description: str | None = None)
```

### get_ocr_provider

Get an OCR provider by id.

```python
provider = await client.get_ocr_provider(provider_id)
```

### update_ocr_provider

Patch update OCR provider fields. See the warning above before using this method.

```python
async def update_ocr_provider(self, provider_id: int, *, name: str | None = None, endpoint: str | None = None, model: str | None = None, description: str | None = None)
```

### delete_ocr_provider

Soft delete an OCR provider.

```python
await client.delete_ocr_provider(provider_id)
```

### list_ocr_keys

List OCR key bindings.

```python
keys = await client.list_ocr_keys()
```

### update_ocr_key_binding

Assign or clear the provider bound to an OCR key binding.

```python
await client.update_ocr_key_binding("DEFAULT", provider_id)
```

## Embeddings

Manage bring-your-own embedding providers, the organization's default embedding binding, and per-case reindexing. See [Bring Your Own Embeddings](/docs/administration/bring-your-own-embeddings) for the provider/revision/generation model.

### list_embedding_providers

List embedding providers and their revisions for the organization.

```python
providers = await client.list_embedding_providers()
```

### create_embedding_provider

Create an embedding provider and its initial embedding revision. Changing endpoint, model, dimensions, or metric later creates a new revision; existing cases keep their current generation until an explicit reindex is started.

```python
provider = await client.create_embedding_provider(
    name="Org Azure OpenAI embeddings",
    provider_type="azure-openai",
    api_url="https://my-resource.openai.azure.com",
    deployment_name="text-embedding-3-large",
    dimensions=3072,
    similarity_metric="cosine",
    credential_id=credential_id,
)
```

### get_embedding_provider

Get an embedding provider with all of its revisions.

```python
provider = await client.get_embedding_provider(provider_id)
```

### update_embedding_provider

Update embedding provider fields. Semantic changes (`provider_type`, `api_url`, `deployment_name`, `dimensions`, `similarity_metric`, `request_options`) create a new revision and report that existing cases need an explicit reindex; changing only the credential reference does not reindex existing cases.

```python
async def update_embedding_provider(self, provider_id: int, *, name=None, provider_type=None, api_url=None, deployment_name=None, dimensions=None, similarity_metric=None, request_options=None, credential_id=None, enabled=None, force_disable=False, set_as_default=False)
```

### delete_embedding_provider

Delete an embedding provider. Its disabled revisions become unavailable.

```python
await client.delete_embedding_provider(provider_id)
```

### get_default_embedding_binding

Get the organization's default embedding binding and rollout counters.

```python
binding = await client.get_default_embedding_binding()
```

### set_default_embedding_binding

Bind the organization default to an existing embedding revision. The binding applies to new case generations only; existing cases keep their active generation until reindexed.

```python
await client.set_default_embedding_binding(revision_id)
```

### get_embedding_overview

Get the organization embedding rollout overview (counts of cases by revision).

```python
overview = await client.get_embedding_overview()
```

### get_case_embedding_status

Get embedding generation status for a case, including its active and any in-progress candidate generation.

```python
status = await client.get_case_embedding_status(case_id)
```

### start_case_embedding_reindex

Start reindexing a case with the organization's default revision. Documents uploaded during the reindex are dual-written to both the active and candidate generations; cutover is atomic per case.

```python
await client.start_case_embedding_reindex(case_id)
```

### cancel_case_embedding_reindex

Cancel a running case reindex; the active generation keeps serving.

```python
await client.cancel_case_embedding_reindex(case_id)
```

### retry_case_embedding_reindex

Retry a failed case reindex candidate.

```python
await client.retry_case_embedding_reindex(case_id)
```

## Model Credentials

Shared credentials referenced by embedding providers (and other bring-your-own integrations) by id, so secrets are never round-tripped back to callers. See [Model Credentials](/docs/administration/model-credentials).

### list_model_credentials

List safe metadata (no secrets) for shared model credentials.

```python
credentials = await client.list_model_credentials()
```

### create_model_credential

Create a shared API-key or Entra client-secret credential. Secrets are write-only; the returned object contains metadata and the credential ID, which can be passed to embedding-provider methods.

```python
credential = await client.create_model_credential(
    name="Org Azure OpenAI key",
    secret="...",
    auth_type="API_KEY",
)
```

### rotate_model_credential

Rotate a shared credential's secret without changing embedding vectors.

```python
await client.rotate_model_credential(credential_id, secret="new-secret")
```

### delete_model_credential

Delete a shared credential that is no longer referenced by any provider.

```python
await client.delete_model_credential(credential_id)
```

## AcroForm PDF Metadata

### extract_acroform_metadata

Upload a PDF directly to `/document/acroform-metadata` and return the rich AcroForm metadata structure without creating a job GUID.

```python
metadata = await client.extract_acroform_metadata(
    document=pdf_bytes,
    file_name="fillable-form.pdf",
    mime_type="application/pdf",
)
```

### get_acroform_metadata

Fetch the stored AcroForm metadata artifact from an async processed job or a `pdf-acroform` operation job.

```python
job_guid = await client.submit_and_process_document(
    document=pdf_bytes,
    document_mime_type="application/pdf",
    acro_form_metadata=True,
)

metadata = await client.get_acroform_metadata(job_guid)
```

The direct helper is for metadata-only PDF uploads. The async helper is for workflows where the same job also needs source locations, rendered page images, extraction, or overlays.

For generated AcroForm PDFs, this metadata reflects the widgets embedded in the output PDF. Use `get_pdf_acroform_field_definitions(...)` when you need the normalized editable boxes that were used to generate those widgets.

## Agent Chat

### agent_chat

Send a chat message to the agent and receive a job GUID.

```python
response = await client.agent_chat(
    messages=[{"role": "user", "content": "Help me extract invoice line items"}],
    session_id="session-123"
)
```

### agent_status

Check the status of an agent chat job.

```python
status = await client.agent_status(response["jobGuid"])
```

### agent_chat_and_wait

Send a message and wait for the completed response.

```python
result = await client.agent_chat_and_wait(
    messages=[{"role": "user", "content": "Create a schema for insurance claims"}],
    session_id="session-123"
)
print(result["response"]["message"])
```

## Cases

### create_case

Create a new case (collection of documents).

```python
from docudevs.models import CreateCaseBody

case = await client.create_case(
    body=CreateCaseBody(name="Q1 Invoices")
)
```

### list_cases

List all cases.

```python
cases = await client.list_cases()
```

### get_case

Get a specific case. Accepts an int or str case id (coerced to int).

```python
case = await client.get_case(123)
```

### update_case

Update an existing case.

```python
await client.update_case(123, body)
```

### delete_case

Delete a case.

```python
await client.delete_case(123)
```

### list_case_documents

List documents within a case (paginated).

```python
page = await client.list_case_documents(123, page=0, size=20)
```

### get_case_document

Get details for a document stored in a case.

```python
document = await client.get_case_document(123, "doc-uuid")
```

### delete_case_document

Delete a document from a case.

```python
await client.delete_case_document(123, "doc-uuid")
```

### upload_case_document

Upload a document to a case.

```python
from docudevs.models import UploadCaseDocumentBody
from docudevs.types import File

await client.upload_case_document(
    case_id=123,
    body=UploadCaseDocumentBody(
        document=File(payload=data, file_name="doc.pdf")
    )
)
```

### get_document_summary

Get the AI-generated summary for a specific document in a case.

```python
result = await client.get_document_summary(case_id=123, document_id="doc-uuid")
summary = result.parsed
print(summary["filename"])  # "invoice.pdf"
print(summary["summary"]["document_type"])  # "invoice"
```

### list_case_summaries

List AI-generated summaries for all documents in a case.

```python
result = await client.list_case_summaries(case_id=123)
for doc in result.parsed:
    print(f"{doc['filename']}: {doc['summary']['document_type']}")
```

## Knowledge Bases

A knowledge base is a case promoted for retrieval and downstream features such as contract analysis. See [Cases](/docs/advanced/cases).

### list_knowledge_bases

List cases marked as knowledge bases.

```python
knowledge_bases = await client.list_knowledge_bases()
```

### promote_knowledge_base

Mark a case as a knowledge base.

```python
await client.promote_knowledge_base(case_id)
```

### get_knowledge_base

Retrieve a knowledge base by its case id.

```python
kb = await client.get_knowledge_base(case_id)
```

### delete_knowledge_base

Remove the knowledge base designation from a case. The case itself is not deleted.

```python
await client.delete_knowledge_base(case_id)
```

## Contract Analysis

Contract analysis compiles a knowledge base into a reviewable profile, runs immutable analysis runs against it, and supports expert review and test-suite curation. See [Contract Analysis](/docs/core/contract-analysis) for the profile/draft/publish lifecycle and the run/review model.

### get_contract_analysis_profile

Get a contract-analysis profile draft or its currently published version.

```python
profile = await client.get_contract_analysis_profile(case_id, view="draft")
```

### generate_contract_analysis_profile

Start profile compilation from the eligible knowledge-base documents. Returns a generation job whose proposal can be compared and applied.

```python
generation = await client.generate_contract_analysis_profile(case_id)
```

### get_contract_analysis_generation_comparison

Get the immutable three-way comparison (previous / current / proposed) for a generated proposal.

```python
comparison = await client.get_contract_analysis_generation_comparison(case_id, generation_job_guid)
```

### apply_contract_analysis_generation

Apply an explicitly reviewed generation against the exact draft snapshot it was compared to.

```python
async def apply_contract_analysis_generation(self, case_id, generation_job_guid, *, expected_draft_revision: int, expected_draft_fingerprint: str, decisions: Mapping[str, Mapping[str, Any]])
```

### get_contract_analysis_source_authority

Get the source-authority decision history for a profile.

```python
history = await client.get_contract_analysis_source_authority(case_id)
```

### record_contract_analysis_source_authority_decision

Record a source-authority decision at an observed decision revision.

```python
async def record_contract_analysis_source_authority_decision(self, case_id, *, observed_revision: int, decision_type: str, content: Mapping[str, Any], reason: str, element_lineage_id: str | None = None, element_action: str | None = None, tombstone: bool = False, generation_job_guid: str | None = None, draft_revision: int | None = None)
```

### update_contract_analysis_draft

Replace the draft profile at the observed revision (optimistic concurrency on `draft_revision`).

```python
await client.update_contract_analysis_draft(case_id, draft_revision=3, profile=updated_profile)
```

### publish_contract_analysis_profile

Publish a validated draft profile at the observed revision.

```python
await client.publish_contract_analysis_profile(
    case_id,
    draft_revision=3,
    acknowledged_warning_codes=["missing_termination_clause"],
)
```

### get_contract_analysis_test_suite

Get the current immutable test-suite revision for a knowledge base.

```python
suite = await client.get_contract_analysis_test_suite(case_id)
```

### update_contract_analysis_test_suite

Create the next suite revision from the observed revision and a list of test cases.

```python
await client.update_contract_analysis_test_suite(case_id, revision=2, cases=[...])
```

### create_contract_analysis_trial

Explicitly launch an exploratory or scored draft-profile trial.

```python
trial = await client.create_contract_analysis_trial(
    case_id,
    anchor_job_guid=job_guid,
    idempotency_key="trial-1",
    trial_kind="scored",
)
```

### get_contract_analysis_trial

Read status and the immutable report for a trial.

```python
trial = await client.get_contract_analysis_trial(case_id, trial_id)
```

### attest_contract_analysis_trial

Record an authenticated expert's approval of a passing trial report.

```python
await client.attest_contract_analysis_trial(case_id, trial_id, report_id=report_id)
```

### submit_contract_analysis

Submit contract analysis for a completed parent job against a published knowledge-base profile.

```python
submission = await client.submit_contract_analysis(job_guid, knowledge_base_id=knowledge_base_id)
```

### submit_and_wait_for_contract_analysis

Submit contract analysis and poll until its result is available.

```python
result = await client.submit_and_wait_for_contract_analysis(
    job_guid,
    knowledge_base_id=knowledge_base_id,
    timeout=120,
)
```

### create_contract_analysis_run

Create an immutable, multi-document contract-analysis run and return its run identity. `mode` is `"published"` (default) or `"draft_trial"`.

```python
run = await client.create_contract_analysis_run(
    anchor_job_guid,
    knowledge_base_id=knowledge_base_id,
    idempotency_key="run-1",
)
```

### get_contract_analysis_run

Get status for exactly one immutable run.

```python
run = await client.get_contract_analysis_run(run_id)
```

### get_contract_analysis_run_result

Get the result for exactly one immutable run.

```python
result = await client.get_contract_analysis_run_result(run_id)
```

### get_contract_analysis_run_reviews

Get current human review dispositions for an immutable run.

```python
reviews = await client.get_contract_analysis_run_reviews(run_id)
```

### put_contract_analysis_run_review

Append an expert disposition (accepted, corrected, dismissed, unresolved) to one captured finding without changing the machine result.

```python
await client.put_contract_analysis_run_review(
    run_id, criterion_id, target_key,
    result_fingerprint=fingerprint,
    disposition="accepted",
    expected_review_revision=0,
    idempotency_key="review-1",
)
```

### propose_contract_analysis_finding_test_case

Add an unapproved finding expectation to a new test-suite revision, seeded from one immutable run finding.

```python
async def propose_contract_analysis_finding_test_case(self, case_id: int, *, run_id: str, result_fingerprint: str, criterion_id: str, target_key: str, name: str, expected_assessment: str, reason: str, expected_suite_revision: int, idempotency_key: str, scenario_tags: list[str] | None = None)
```

## Operations

### submit_and_wait_for_error_analysis

Run error analysis on a completed job.

```python
analysis = await client.submit_and_wait_for_error_analysis(job_guid)
```

### submit_operation

Submit an operation for a completed job without parameters or waiting.

```python
response = await client.submit_operation(job_guid, "error-analysis")
```

### submit_operation_with_parameters

Submit an operation with an optional LLM type override and custom parameters, without waiting.

```python
async def submit_operation_with_parameters(self, job_guid: str, operation_type: str, llm_type: Optional[str] = None, custom_parameters: Optional[dict] = None)
```

### submit_and_wait_for_operation

Submit an operation (no extra parameters) and poll until it completes.

```python
result = await client.submit_and_wait_for_operation(job_guid, "error-analysis", timeout=120)
```

### get_operation_status

Get the status of all operations submitted for a job.

```python
statuses = await client.get_operation_status(job_guid)
```

### get_operation_result

Get the result of one specific operation type for a job.

```python
result = await client.get_operation_result(job_guid, "error-analysis")
```

### create_generative_task

Create a generative task for a completed job without waiting for it. Prefer `submit_and_wait_for_generative_task` unless you need to poll independently.

```python
async def create_generative_task(self, parent_job_id: str, prompt: str, model: Optional[str] = None, temperature: Optional[float] = None, max_tokens: Optional[int] = None)
```

### submit_and_wait_for_generative_task

Run a generative AI task on a completed job.

```python
task = await client.submit_and_wait_for_generative_task(
    parent_job_id=job_guid,
    prompt="Summarize this document",
    model="DEFAULT"
)
# Result is in task.result (JSON string)
```

### submit_and_wait_for_operation_with_parameters

Run an operation with custom parameters.

```python
result = await client.submit_and_wait_for_operation_with_parameters(
    job_guid=job_guid,
    operation_type="error-analysis",
    llm_type="HIGH",
    custom_parameters={"focus": "dates"}
)
```

### submit_and_wait_for_image_selection

Select relevant figures from a completed job that extracted figures.

```python
selection = await client.submit_and_wait_for_image_selection(
    job_guid,
    prompt="Return all diagrams from the document",
    top_k=5,
    match_mode="all",
    use_vision=False
)

import json
selection_payload = json.loads(selection.result)
selected = selection_payload.get("selected", [])
```

### submit_pdf_acroform_operation

Queue `pdf-acroform` on a completed PDF job.

```python
response = await client.submit_pdf_acroform_operation(
    job_guid=parent_job_guid,
    llm_type="DEFAULT",
    ocr="PREMIUM",
    page_range=[1, 2],
    min_confidence=0.35,
    max_fields_per_page=300,
)

print(response.parsed["jobGuid"])
```

### submit_and_wait_for_pdf_acroform_operation

Run `pdf-acroform`, wait for completion, and download the generated fillable PDF.

```python
conversion = await client.submit_and_wait_for_pdf_acroform_operation(
    parent_job_guid,
    ocr="PREMIUM",
    min_confidence=0.35,
    max_fields_per_page=300,
    save_to="fillable.pdf",
)

print(conversion.operation_job_guid)
```

### get_pdf_acroform

Download the generated PDF bytes for a `pdf-acroform` or `pdf-acroform-apply` operation job.

```python
pdf_bytes = await client.get_pdf_acroform(operation_job_guid, save_to="fillable.pdf")
```

### get_pdf_acroform_field_definitions

Fetch the normalized editable field definitions for a `pdf-acroform` operation.

`entryBbox` uses normalized page-image coordinates from `0..1`, which makes this artifact the correct input for review tooling and manual edits.

```python
definitions = await client.get_pdf_acroform_field_definitions(operation_job_guid)
fields = definitions["fieldDefinitions"] if definitions else []
```

### submit_pdf_acroform_apply_operation

Queue reviewed field definitions to regenerate the PDF without rerunning visual detection.

```python
response = await client.submit_pdf_acroform_apply_operation(
    parent_job_guid,
    field_definitions=edited_fields,
    source_operation_guid=operation_job_guid,
)

print(response.parsed["jobGuid"])
```

### submit_and_wait_for_pdf_acroform_apply_operation

Submit reviewed field definitions, wait for completion, and download the regenerated PDF.

```python
applied = await client.submit_and_wait_for_pdf_acroform_apply_operation(
    parent_job_guid,
    field_definitions=edited_fields,
    source_operation_guid=operation_job_guid,
    save_to="fillable-reviewed.pdf",
)

print(applied.operation_job_guid)
```

## Document Outline

Document-outline extraction produces a structural outline of a document (sections/headers), either as a map-reduce split strategy or as a standalone follow-up operation on an existing job. See [Operations](/docs/advanced/operations).

### submit_document_outline_operation

Submit a document-outline-only follow-up operation for an existing OCR/extraction job.

```python
async def submit_document_outline_operation(self, job_guid: str, *, llm_type: Optional[str] = None, pages_per_chunk: Optional[int] = None, overlap_pages: Optional[int] = None, parallel_processing: Optional[bool] = None)
```

### submit_and_wait_for_document_outline_operation

Submit an outline-only operation and wait for the outline artifact.

```python
outline = await client.submit_and_wait_for_document_outline_operation(job_guid, timeout=180)
print(outline.document_outline)
```

### get_document_outline

Get the document-outline artifact for a `DOCUMENT_OUTLINE` map-reduce job or outline operation.

```python
outline = await client.get_document_outline(guid)
```

### wait_until_ready_with_document_outline

Wait for completion, then return both the JSON result and the document-outline artifact as a `StructuredResultWithDocumentOutline(result=..., document_outline=...)`.

```python
bundle = await client.wait_until_ready_with_document_outline(guid, timeout=180)
```

## Map-Reduce Helpers

### submit_and_process_document_map_reduce

Process large documents using map-reduce strategy.

```python
job_guid = await client.submit_and_process_document_map_reduce(
    document=doc_bytes,
    document_mime_type="application/pdf",
    prompt="Extract line items",
    split_type="page",
    pages_per_chunk=5,
    overlap_pages=1,
    dedup_key="sku",
    parallel_processing=True
)
```

### submit_and_wait_for_map_reduce

Run map-reduce on an already-processed job without re-uploading or re-running OCR. This is the map-reduce equivalent of `submit_and_wait_for_generative_task`.

```python
# First: process a document normally (OCR runs here)
job_guid = await client.submit_and_process_document(
    document=doc_bytes,
    document_mime_type="application/pdf",
    prompt="Extract summary",
)
await client.wait_until_ready(job_guid)

# Later: re-run with map-reduce — no new upload, reuses OCR from parent job
result = await client.submit_and_wait_for_map_reduce(
    parent_job_id=job_guid,
    prompt="Extract all line items (sku, description, quantity, total)",
    schema='{"type":"array","items":{"type":"object"}}',
    split_type="page",
    pages_per_chunk=5,
    overlap_pages=1,
    dedup_key="sku",
    parallel_processing=True,
    timeout=300,
    result_format="json"
)
print(result["records"])
```

**Parameters:**

| Parameter | Type | Default | Description |
|-----------|------|---------|-------------|
| `parent_job_id` | str | *required* | GUID of the completed job whose document to re-process. |
| `prompt` | str | `""` | Extraction instructions. |
| `schema` | str | `""` | JSON schema for structured extraction. |
| `split_type` | str | `"page"` | Chunk strategy: `"page"` or `"markdown_header"`. |
| `split_header_level` | int | `2` (markdown mode) | Header level (`1` or `2`) used when `split_type="markdown_header"`. |
| `pages_per_chunk` | int | `1` | Pages per chunk. |
| `overlap_pages` | int | `0` | Overlapping pages between chunks. |
| `dedup_key` | str | `None` | Required when `overlap_pages > 0`. |
| `parallel_processing` | bool | `False` | Run chunks in parallel. |
| `mime_type` | str | `"application/pdf"` | Document MIME type. |
| `timeout` | int | `180` | Max seconds to wait. |
| `poll_interval` | float | `5.0` | Seconds between status polls. |
| `result_format` | str | `"json"` | `"json"`, `"csv"`, `"excel"`, or `None`. |

When `split_type="markdown_header"`, `overlap_pages` and `dedup_key` are not supported.

All other map-reduce parameters (`header_options`, `header_schema`, `header_prompt`, `stop_when_empty`, `empty_chunk_grace`, `ocr`, `llm`, `trace`, `page_range`, `tools`, etc.) are also accepted.

### build_upload_command_map_reduce

Build the raw `UploadCommand` with map-reduce parameters without submitting it. Used internally by `submit_and_process_document_map_reduce` and `process_document_map_reduce`.

```python
command = client.build_upload_command_map_reduce(
    mime_type="application/pdf",
    prompt="Extract line items",
    pages_per_chunk=5,
)
```

### process_document_map_reduce

Process an already-uploaded document using map-reduce chunking parameters. Takes the same map-reduce parameters as `submit_and_process_document_map_reduce`, but skips the upload step.

```python
await client.process_document_map_reduce(
    guid,
    prompt="Extract line items",
    pages_per_chunk=5,
    overlap_pages=1,
)
```

## LLM Tracing

### get_trace

Get the LLM trace for a completed job (only available if `trace=True` was set).

```python
trace = await client.get_trace(job_guid)
if trace:
    print(f"Total tokens: {trace['total_tokens']}")
    print(f"LLM calls: {trace['total_llm_calls']}")
    for event in trace['events']:
        print(f"  {event['type']}: {event['name']}")
```

### get_image

Get a page thumbnail image from a processed job.

```python
image_bytes = await client.get_image(job_guid, page_index=0)
if image_bytes:
    with open("page_0.png", "wb") as f:
        f.write(image_bytes)
```

### get_figures_metadata

Get extracted figure metadata for a job.

```python
metadata = await client.get_figures_metadata(job_guid)
images = metadata.get("images", []) if metadata else []
```

### get_figure_image

Download a figure image by ID.

```python
figure_id = images[0]["id"]
image_bytes = await client.get_figure_image(job_guid, figure_id)
if image_bytes:
    with open("figure_0.png", "wb") as f:
        f.write(image_bytes)
```

## Job Management

### delete_job

Delete a job and its associated data. Jobs must be in a terminal state (COMPLETED, ERROR, TIMEOUT, or PARTIAL). Jobs older than 14 days are automatically purged, so this method is primarily for cleaning up recent jobs.

```python
result = await client.delete_job(job_guid)
if result.status_code == 200:
    print(f"Deleted {result.parsed['jobsDeleted']} job(s)")
```

**Parameters:**

- `guid` (str): The job GUID to delete

**Returns:** A response object with:

- `status_code` (int): HTTP status (200 on success, 404 if not found)
- `parsed` (dict): Contains `jobsDeleted`, `errors` on success

**Note:** Deleting a job removes all associated data including uploaded documents, OCR results, and extracted data. Usage/billing records are preserved but disassociated from the deleted job.

### Enabling Tracing

Pass `trace=True` to any processing method:

```python
job_guid = await client.submit_and_process_document(
    document=doc_bytes,
    document_mime_type="application/pdf",
    prompt="Extract data",
    trace=True  # Enable LLM tracing
)
```

## Error Handling

The SDK raises exceptions for API errors.

```python
try:
    await client.get_configuration("non-existent")
except Exception as e:
    print(f"Error: {e}")
```
