import asyncio
import os

from docudevs.docudevs_client import DocuDevsClient


async def main():
    client = DocuDevsClient(token=os.environ["API_KEY"])

    # docs:start
    job_guid = "<job-guid>"  # GUID of a completed document job

    outline_result = await client.submit_and_wait_for_document_outline_operation(
        job_guid,
        pages_per_chunk=3,
        overlap_pages=1,
        parallel_processing=True,
        timeout=180,
    )

    print(outline_result.result)
    print(outline_result.document_outline)
    # docs:end


asyncio.run(main())
