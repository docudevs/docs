import asyncio
import os

from docudevs.docudevs_client import DocuDevsClient


async def main():
    client = DocuDevsClient(token=os.environ["API_KEY"])

    # docs:start
    with open("large-report.pdf", "rb") as f:
        document_data = f.read()

    job_guid = await client.submit_and_process_document_map_reduce(
        document=document_data,
        document_mime_type="application/pdf",
        prompt="Extract line items",
        pages_per_chunk=5,
        trace=True,  # Traces every chunk
    )

    await client.wait_until_ready(job_guid)

    trace = await client.get_trace(job_guid)
    if trace:
        print(f"Processed in {len(trace.get('events', []))} events")
    # docs:end


asyncio.run(main())
