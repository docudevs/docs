---
title: DocuDevs - Turn Documents into Data
description: DocuDevs transforms unstructured documents into structured data using AI. Process invoices, contracts, forms, and more with the Python SDK, CLI, or REST API.
slug: intro
sidebar_position: 1
---
<!-- Generated from docs/intro.md. Do not edit by hand. -->

# DocuDevs - Turn Documents into Data

**DocuDevs is currently in private beta, and this documentation is work in progress**

DocuDevs transforms your unstructured documents into structured data using advanced AI. Whether you're processing invoices, contracts, forms, or any other document type, our platform extracts the information you need in the format you want.

## Quick Navigation

### 🚀 New to DocuDevs?

Start here for a smooth onboarding experience:

- **[5-Minute Quick Start](https://docs.docudevs.ai/docs/getting-started/quick-start)** - Process your first document in minutes
- **[Installation Guide](https://docs.docudevs.ai/docs/basics/install)** - Set up the SDK and get your API key  
- **[First Document Walkthrough](https://docs.docudevs.ai/docs/getting-started/first-document)** - Detailed tutorial with examples
- **[Python notebooks with examples](https://github.com/docudevs/python-examples)**

### 📋 Core Features

Learn about the main functionality:

- **[Simple Documents](https://docs.docudevs.ai/docs/basics/SimpleDocuments)** - Basic document processing
- **[OCR Processing](https://docs.docudevs.ai/docs/basics/PlainOCR)** - Text extraction from images and scans
- **[Templates](https://docs.docudevs.ai/docs/templates/)** - Reusable document processing templates
- **[Schema Generation](https://docs.docudevs.ai/docs/core/schema-generation)** - AI-powered output structure creation
- **[Contract Analysis](https://docs.docudevs.ai/docs/core/contract-analysis)** - Review contracts clause by clause against a playbook
- **[Workbook Normalization](https://docs.docudevs.ai/docs/core/workbook-normalization)** - Turn messy spreadsheets into structured tables

### ⚡ Advanced Features

Power user functionality for complex workflows:

- **[Cases](https://docs.docudevs.ai/docs/advanced/cases)** - Organize documents into collections ⭐ *Most Popular*
- **[Named Configurations](https://docs.docudevs.ai/docs/configuration/)** - Save and reuse processing settings
- **[Operations](https://docs.docudevs.ai/docs/advanced/operations)** - Error analysis and post-processing workflows
- **[Client Pipelines](https://docs.docudevs.ai/docs/core/client-pipelines)** - Mix server pipeline steps with your own local Python functions

### 🛡️ Administration

Organization-admin workflows in the DocuDevs UI:

- **[Members and Roles](https://docs.docudevs.ai/docs/administration/members-and-roles)** - Invite teammates, assign roles, suspend access, and protect the last admin
- **[Single Sign-On](https://docs.docudevs.ai/docs/administration/single-sign-on)** - Set up SSO, verify domains, and control how users join
- **[Bring Your Own LLM](https://docs.docudevs.ai/docs/administration/bring-your-own-llm)**, **[OCR](https://docs.docudevs.ai/docs/administration/bring-your-own-ocr)**, and **[Embeddings](https://docs.docudevs.ai/docs/administration/bring-your-own-embeddings)** - Run processing on your own model accounts
- **[Model Credentials](https://docs.docudevs.ai/docs/administration/model-credentials)** - Shared API-key and Microsoft Entra credentials for your providers
- **[Integration Connections](https://docs.docudevs.ai/docs/administration/integration-connections)** - Store connector credentials for integration flows

### 🔧 Integration & Production

Ready to deploy? These guides will help:

- **[Webhooks](https://docs.docudevs.ai/docs/integration/webhooks)** - Get notified when jobs finish
- **[Submission Portals](https://docs.docudevs.ai/docs/integration/submission-portals)** - Let people submit documents through a hosted form
- **[Use Cases](https://docs.docudevs.ai/docs/integration/use-cases)** - Real-world implementation examples
- **[Best Practices](https://docs.docudevs.ai/docs/integration/best-practices)** - Production deployment guidelines  
- **[Troubleshooting](https://docs.docudevs.ai/docs/integration/troubleshooting)** - Debug common issues

### 📖 Reference

Technical documentation and complete API coverage:

- **[SDK Methods](https://docs.docudevs.ai/docs/reference/sdk-methods)** - Complete Python SDK reference
- **[OpenAPI Specification](https://docs.docudevs.ai/files/docudevs.yaml)** - REST API documentation
- **[Error Codes](https://docs.docudevs.ai/docs/reference/error-codes)** - Error handling reference

## What Makes DocuDevs Different?

### 🤖 AI-Powered Intelligence

- **Context-aware extraction** that understands document structure and meaning
- **Flexible schema generation** - define your output format or let AI determine the best structure
- **Multi-format support** - PDFs, Word docs, images, and more

### 📦 Developer-Friendly

- **Simple SDK** with intuitive async methods
- **Comprehensive examples** for every feature
- **Clear documentation** with runnable code samples

### 🏢 Enterprise-Ready

- **Scalable architecture** built for high-volume processing
- **Secure processing** with enterprise-grade security
- **Flexible deployment** options for any infrastructure

## Popular Use Cases

### Invoice Processing

Extract invoice numbers, dates, line items, and totals from vendor invoices for automated accounting workflows.

### Form Digitization

Convert paper forms and applications into structured data for digital processing and storage.

### Contract Analysis

Extract key terms, dates, parties, and obligations from legal contracts and agreements.

### Receipt Management

Process expense receipts to extract merchant, date, items, and amounts for expense reporting.

## Getting Started Paths

Choose the path that best fits your needs:

### For Developers

1. [Install the SDK](https://docs.docudevs.ai/docs/basics/install)
2. [Quick Start Tutorial](https://docs.docudevs.ai/docs/getting-started/quick-start)
3. [Explore Cases](https://docs.docudevs.ai/docs/advanced/cases) for document organization
4. [Review SDK Methods](https://docs.docudevs.ai/docs/reference/sdk-methods) for advanced usage

### For Business Users

1. [Quick Start Tutorial](https://docs.docudevs.ai/docs/getting-started/quick-start) - understand the basics
2. [Use Cases](https://docs.docudevs.ai/docs/integration/use-cases) - see real-world applications
3. [Templates](https://docs.docudevs.ai/docs/templates/) - create reusable workflows
4. [Best Practices](https://docs.docudevs.ai/docs/integration/best-practices) - optimize for your needs

### For System Integrators

1. [Installation Guide](https://docs.docudevs.ai/docs/basics/install)
2. [First Document Guide](https://docs.docudevs.ai/docs/getting-started/first-document) - comprehensive walkthrough
3. [Best Practices](https://docs.docudevs.ai/docs/integration/best-practices) - production deployment
4. [OpenAPI Docs](https://docs.docudevs.ai/docs/openapi/docudevs-api) - direct API integration

## Download Resources

- **[OpenAPI Specification](https://docs.docudevs.ai/files/docudevs.yaml)** - Complete API specification
- **[Python SDK on PyPI](https://pypi.org/project/docu-devs-api-client/)** - Install via pip

## Need Help?

- **📧 Email**: [support@docudevs.ai](mailto:support@docudevs.ai)
- **💬 Feedback**: We welcome feedback on this documentation
- **🐛 Issues**: Found a bug? Let us know!

---

**Ready to get started?** Begin with our [5-minute quick start guide](https://docs.docudevs.ai/docs/getting-started/quick-start) and have your first document processed in minutes!
