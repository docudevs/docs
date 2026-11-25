import asyncio
import json
import os

from docudevs.docudevs_client import DocuDevsClient


async def main():
    client = DocuDevsClient(token=os.environ["API_KEY"])

    # docs:start
    with open("report.pdf", "rb") as f:
        document_data = f.read()

    # The document has 20 pages, but only pages 1-2, 5-6 and 10-11 are needed.
    # These 6 pages are chunked into 3 chunks of 2 pages each.
    job_guid = await client.submit_and_process_document_map_reduce(
        document=document_data,
        document_mime_type="application/pdf",
        prompt="Extract all items from these pages.",
        pages_per_chunk=2,
        page_range=[1, 2, 5, 6, 10, 11],
    )

    result = await client.wait_until_ready(job_guid, result_format="json")
    print(json.dumps(result, indent=2))
    # docs:end


asyncio.run(main())
