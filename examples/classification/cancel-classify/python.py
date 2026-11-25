import asyncio
import os

from docudevs.docudevs_client import DocuDevsClient


async def main():
    client = DocuDevsClient(token=os.environ["API_KEY"])

    # docs:start
    case_id = 42  # id of a records case
    job_id = 9  # jobId returned when the job was started

    # Raises RecordsApiError (409) when the job has already finished
    job = await client.cancel_classify(case_id, job_id)
    print(job["status"])
    # docs:end


asyncio.run(main())
