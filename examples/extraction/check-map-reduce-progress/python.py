import asyncio
import os

from docudevs.docudevs_client import DocuDevsClient


async def main():
    client = DocuDevsClient(token=os.environ["API_KEY"])

    # docs:start
    job_id = "<job-guid>"  # GUID of a running map-reduce job

    response = await client.status(job_id)
    if response.status_code != 200:
        raise RuntimeError(f"HTTP {response.status_code}")

    progress = response.parsed.map_reduce_status
    if progress:
        print(f"Progress: {progress.completed_chunks} / {progress.total_chunks}")
    # docs:end


asyncio.run(main())
