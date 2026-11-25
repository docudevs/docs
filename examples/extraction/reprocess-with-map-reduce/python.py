import asyncio
import os

# docs:start
from docudevs import array_schema
# docs:end
from docudevs.docudevs_client import DocuDevsClient


async def main():
    client = DocuDevsClient(token=os.environ["API_KEY"])

    # docs:start
    existing_job_id = "<job-guid>"  # GUID of a completed job from a previous run

    # Re-run with map-reduce on the same document: no upload, no new OCR
    result = await client.submit_and_wait_for_map_reduce(
        parent_job_id=existing_job_id,
        prompt="Extract all line items (sku, description, qty, total)",
        schema=array_schema({"type": "object"}),
        pages_per_chunk=5,
        overlap_pages=1,
        dedup_key="sku",
        parallel_processing=True,
        timeout=300,
    )

    print("Header:", result.get("header"))
    print(f"Extracted {len(result.get('records', []))} line items.")
    # docs:end


asyncio.run(main())
