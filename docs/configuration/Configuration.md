---
title: Named Configurations
description: Save and reuse processing settings as named configurations for consistent document extraction across multiple jobs.
sidebar_position: 1
---

# Named Configurations

Save and reuse processing settings, prompts, and schemas across multiple document processing requests.

## Overview

Named configurations allow you to define a set of processing parameters once and reuse them by name. This ensures consistency across your application and simplifies your API calls.

**Key Benefits:**

- **Consistency**: Ensure all documents of a certain type are processed with the exact same settings.
- **Schema Management**: Centralize your JSON schemas to guarantee consistent output formats.
- **Simplicity**: Pass a single configuration name instead of a complex JSON object with every request.

## Managing Configurations

### Saving a Configuration

To create or update a configuration, you define the processing parameters (OCR, LLM, Prompt, Schema) and save them under a unique name. The `schema` is passed as a JSON string.

<Example name="configuration/save-configuration" />

### Listing Configurations

Retrieve a list of all saved configurations in your organization.

<Example name="configuration/list-configurations" />

### Retrieving a Configuration

Get the details of a specific configuration to inspect its settings.

<Example name="configuration/get-configuration" />

### Deleting a Configuration

Remove a configuration when it is no longer needed.

<Example name="configuration/delete-configuration" />

## Using Configurations

Once saved, you can use a configuration to process documents. This applies all the settings defined in the configuration to the processing job.

<Example name="configuration/process-with-configuration" />

## Configuration Parameters

When creating a configuration, you can specify the following parameters:

| Parameter | Type | Description |
| --- | --- | --- |
| `prompt` | String | Custom instructions for data extraction. |
| `schema` | String | JSON Schema, serialized as a string, defining the exact output structure. |
| `ocr` | Enum | OCR mode (`DEFAULT`, `PREMIUM`, `LOW`, `NONE`, `AUTO`, `EXCEL`). |
| `llm` | Enum | LLM model quality (`DEFAULT`, `NANO`, `MINI`, `HIGH`). |
| `extractionMode` | Enum | Extraction method (`SIMPLE`, `STEPS`, `OCR`, `GENERATIVE`, `PIPELINE`, `WORKBOOK`). |
| `barcodes` | Boolean | Enable barcode and QR code scanning. |
| `describeFigures` | Boolean | Enable AI description of figures and images. |
| `extractFigures` | Boolean | Store figure images and metadata for later download. |

## Best Practices

- **Version Your Configurations**: If you change your schema significantly, consider creating a new configuration (e.g., `invoice-v2`) to avoid breaking existing integrations.
- **Use Schemas**: Always define a strict JSON schema to ensure predictable API responses.
- **Test Prompts**: Experiment with different prompts in the playground or via temporary configurations before finalizing them.
- **Optimize Costs**: Use `DEFAULT` OCR and LLM for simple documents and `HIGH` only when necessary.
