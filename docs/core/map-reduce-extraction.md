---
title: Map-Reduce Extraction
sidebar_position: 4
---

Process very large or repetitive documents by breaking them into overlapping page chunks ("map" phase) and aggregating results ("reduce" phase) to improve coverage, consistency, and memory efficiency.

## When To Use

Use Map-Reduce when:

- Documents exceed typical context/token limits
- Key/value information appears multiple times (tables, repeated headers)
- You need higher recall by scanning overlapping windows

Prefer standard processing when the document is short (< 50 pages) and you extract single entities (such as all parties of a single contract)

If you have 1000 page product catalog, and you need to extract information of each SKU (and the data for SKU is always in the same area of document i.e. not spread over tens of pages), then use Map-Reduce

## Core Concepts

| Concept | Description |
|---------|-------------|
| pagesPerChunk | Number of sequential pages combined into a single chunk window |
| overlapPages | Number of trailing pages from previous chunk re-included at start of next chunk (sliding window) |
| dedupKey | Field/property path used to remove duplicate items across chunks (first occurrence wins) |
| totalFragments | Total number of chunk windows generated for the job |
| completedFragments | Number of chunks already processed |
| jobMode | `STANDARD` or `MAP_REDUCE` |

**Page Boundaries**: OCR markdown uses `<!-- PageBreak -->` markers. Pages are split on that token before forming chunks.

**Chunk Windows**: Windows advance by `pagesPerChunk - overlapPages` pages until the end of the document is reached.

## Parameter Rules

- `pagesPerChunk >= 1`
- `overlapPages >= 0`
- `overlapPages < pagesPerChunk`
- If `overlapPages > 0` then `dedupKey` is required (non-empty)
- If `pagesPerChunk == 1` then `overlapPages` must be 0 (implied)

## High-Level Flow

1. User enables Map-Reduce and sets parameters
2. Backend stores job with `job_mode = MAP_REDUCE`
3. Worker loads OCR markdown, splits into pages
4. Worker constructs chunk windows with overlap
5. For each chunk:
   - Combine page markdown into a single prompt input
   - Run extraction/LLM pass
   - Merge structured results into aggregate output
   - Perform deduplication (if configured) as items are added
   - Emit status update (`completedFragments`)
6. Final aggregated result stored like a standard job

## Example Chunking

Document has 9 pages, `pagesPerChunk=4`, `overlapPages=1`:

Chunks (page indices 1-based):

- Chunk 1: 1 2 3 4
- Chunk 2: 4 5 6 7 (page 4 overlapped)
- Chunk 3: 7 8 9 (remaining pages, shorter final window allowed)

`totalFragments = 3`, windows advanced by `4 - 1 = 3` pages.

## API Request (Multipart Upload + Process)

```bash
curl -X POST "https://api.docudevs.ai/document/process" \
  -H "Authorization: Bearer $API_KEY" \
  -F "document=@large.pdf" \
  -F 'metadata={
    "prompt": "Extract all invoice line items with SKU, description, quantity, unit price, total.",
    "mapReduce": {
      "enabled": true,
      "pagesPerChunk": 4,
      "overlapPages": 1,
      "dedupKey": "items.sku"
    }
  }'
```

## Python SDK Example

```python
from docudevs.docudevs_client import DocuDevsClient
import asyncio, json

async def run():
    client = DocuDevsClient(token="YOUR_API_KEY")
    with open("large.pdf", "rb") as f:
        data = f.read()

    job_id = await client.submit_and_process_document_map_reduce(
        document=data,
        document_mime_type="application/pdf",
        prompt="Extract all invoice line items with SKU, description, quantity, unit price, total.",
        pages_per_chunk=4,
        overlap_pages=1,
        dedup_key="items.sku"
    )

    result = await client.wait_until_ready(job_id)
    parsed = json.loads(result.result)
    print(parsed)

asyncio.run(run())
```

## Status Semantics

`GET /job/status/{guid}` will include:

```json
{
  "guid": "...",
  "jobMode": "MAP_REDUCE",
  "totalFragments": 5,
  "completedFragments": 2,
  "status": "PROCESSING"
}
```

Progress bar in UI = `completedFragments / totalFragments`.

## Deduplication Strategy

When `dedupKey` is provided:

- Each structured result item is inspected for the key path
- First occurrence of each unique key value is kept
- Subsequent duplicates are discarded
- Order of first appearance is preserved

If the key is missing on an item, that item is retained (assumed unique).

## Output Shape

Final result structure matches standard extraction output. No additional nesting is introduced; aggregation is transparent to consumers.

## Best Practices

| Scenario | Recommendation |
|----------|----------------|
| Long repetitive tables | Use overlap of 1–2 pages to catch row splits |
| Unique per-page sections | Keep `overlapPages=0` |
| Memory or token limits | Reduce `pagesPerChunk` until stable |
| Many near-duplicate items | Provide a precise `dedupKey` |
| Highly varied page lengths | Allow last chunk to be shorter |

### Choosing Parameters

Start with:

- `pagesPerChunk=4`
- `overlapPages=1`
- `dedupKey` targeting your primary item identifier

Adjust based on recall vs. cost trade-offs.

### Common Mistakes

| Mistake | Impact | Fix |
|---------|--------|-----|
| Overlap without dedupKey | Duplicate items | Provide `dedupKey` |
| Large pagesPerChunk + large overlap | Excess cost & latency | Reduce overlap or chunk size |
| Very high pagesPerChunk | Context overflows | Lower chunk size |
| Too small chunk size (1) | Lost multi-page context | Increase to 2–4 |

## Limitations

- Sequential processing (no parallel chunk execution yet)
- Dedup only supports a single key path currently
- Requires OCR format that preserves clear page markers (`<!-- PageBreak -->`)
- Aggregation currently keeps first occurrence only (no merge of partial objects)

## Cross-Feature Integration

You can chain Map-Reduce output into AI Analysis or Error Analysis just like a standard job—downstream features are unaware of chunk internals.

## Troubleshooting

| Symptom | Possible Cause | Action |
|---------|----------------|--------|
| `totalFragments` = 1 unexpectedly | Document shorter than chunk size | Reduce `pagesPerChunk` only if needed |
| Progress stalls | Long-running extraction on a large chunk | Use smaller `pagesPerChunk` |
| Missing expected items | Over-aggressive dedup | Verify `dedupKey` correctness |
| Duplicates remain | Key path not present on those items | Confirm data path; maybe post-filter |

## Migration / Backward Compatibility

- Existing jobs default to `STANDARD`
- New parameters ignored unless `mapReduce.enabled=true`
- No schema/table migrations beyond added columns (`job_mode`, `total_fragments`, `completed_fragments`)

## See Also

- [AI Document Analysis](./ai-analysis.md)
- [Schema Generation](./schema-generation.md)
