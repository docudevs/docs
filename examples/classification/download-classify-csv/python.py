import asyncio
import os

from docudevs.docudevs_client import DocuDevsClient


async def main():
    client = DocuDevsClient(token=os.environ["API_KEY"])

    # docs:start
    case_id = 42  # id of a records case
    job_id = 9  # jobId returned when the job was started

    path = await client.download_classify_csv(case_id, job_id, flag="declined", path="declined.csv")
    print(f"Wrote {path}")
    # docs:end


asyncio.run(main())
