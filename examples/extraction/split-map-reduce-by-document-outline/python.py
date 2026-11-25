import asyncio
import json
import os

from docudevs.docudevs_client import DocuDevsClient


async def main():
    client = DocuDevsClient(token=os.environ["API_KEY"])

    # docs:start
    with open("contract.pdf", "rb") as f:
        document_data = f.read()

    job_id = await client.submit_and_process_document_map_reduce(
        document=document_data,
        document_mime_type="application/pdf",
        prompt="Extract obligations by section.",
        split_type="document_outline",
        pages_per_chunk=3,
        parallel_processing=True,
    )

    result = await client.wait_until_ready(job_id, result_format="json")
    print(json.dumps(result, indent=2))

    outline = await client.get_document_outline(job_id)
    print(json.dumps(outline, indent=2))
    # docs:end


asyncio.run(main())
