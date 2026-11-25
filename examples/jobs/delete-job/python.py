import asyncio
import os

from docudevs.docudevs_client import DocuDevsClient


async def main():
    client = DocuDevsClient(token=os.environ["API_KEY"])

    job_guid = "<job-guid>"  # GUID of a completed job

    result = await client.delete_job(job_guid)
    if result.status_code == 200:
        print(f"Deleted {result.parsed['jobsDeleted']} job(s)")


asyncio.run(main())
