import asyncio
import os

from docudevs.docudevs_client import DocuDevsClient


async def main():
    client = DocuDevsClient(token=os.environ["API_KEY"])

    job_guid = "<job-guid>"  # GUID of a completed document job

    result = await client.submit_and_wait_for_error_analysis(
        job_guid=job_guid,
        timeout=120,
    )
    print(result.result)


asyncio.run(main())
