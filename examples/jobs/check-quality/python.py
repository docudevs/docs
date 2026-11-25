import asyncio
import os

from docudevs.docudevs_client import DocuDevsClient


async def main():
    client = DocuDevsClient(token=os.environ["API_KEY"])

    # docs:start
    job_guid = "<job-guid>"  # GUID of a completed job

    status_response = await client.status(guid=job_guid)
    if status_response.status_code != 200:
        raise RuntimeError(f"HTTP {status_response.status_code}")

    job = status_response.parsed
    if isinstance(job.quality_score, float):
        print(f"Quality: {job.quality_score:.2f} ({job.quality_category})")
    else:
        print("No quality score for this job")
    # docs:end


asyncio.run(main())
