---
title: Batch Processing
description: Process multiple documents in a single batch with configurable concurrency, shared prompts, and aggregated results.
sidebar_position: 5
---

# Batch Processing

Coordinate the processing of multiple homogeneous documents under a single parent job.

Batches let you upload several files, run one configuration across all of them, and retrieve an ordered list of per-document results.

## When To Use

Choose batches when:

- You have dozens or hundreds of similar documents that share the same prompt, schema, or template.
- You want consolidated progress tracking and a single job identifier.
- You want to reprocess a collection without re-uploading each file.

## Lifecycle Overview

1. **Create** an empty batch job to reserve a GUID.
2. **Upload** documents one at a time (each upload receives an index).
3. **Process** the batch by providing the extraction configuration.
4. **Monitor** progress and completion.
5. **Fetch** results as a list aligned with the upload order.

## Step-by-Step Guide

The example creates a batch with a concurrency limit of three, uploads three invoices, starts processing with a prompt and a schema, waits for the batch to finish and prints the result for each document. The results list follows the upload order; a document that failed or has no stored result is `null` in the list (see [Best Practices](#best-practices) and [Result Formats](/docs/core/result-formats#batch-results)).

<Example name="quick-start/batch-process" />

## Core Concepts

| Concept | Description |
|---------|-------------|
| `isBatch` | Flag on the parent job that identifies batch processing. |
| `totalDocuments` | Count of documents currently attached to the batch. |
| `completedDocuments` | Number of documents that finished successfully. |
| `failedDocuments` | Number of documents that completed with errors. |
| `maxConcurrency` | Upper bound of documents processed in parallel. |

## Best Practices

- **Concurrency**: Set `maxConcurrency` between 3 and 8 for optimal throughput. Lower it for very large files.
- **Validation**: Ensure all documents in a batch are of the same type (e.g., all invoices) so they share the same schema.
- **Error Handling**: Batch jobs continue even if individual documents fail. A failed or missing document is `null` in the results list, and an empty row in CSV or Excel. Check each entry for `null`; the `status` and `error` of each document are in `documents` of `GET /job/result/{guid}`. See [Result Formats](/docs/core/result-formats#batch-results).

