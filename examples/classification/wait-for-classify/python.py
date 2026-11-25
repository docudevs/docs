import asyncio
import os

from docudevs.docudevs_client import DocuDevsClient


async def main():
    client = DocuDevsClient(token=os.environ["API_KEY"])

    # docs:start
    case_id = 42  # id of a records case
    job_id = 9  # jobId returned when the job was started

    # Raises ClassifyJobFailed when the job fails or is cancelled, and TimeoutError after 3600 seconds
    job = await client.wait_for_classify(
        case_id, job_id, on_progress=lambda current: print(current["status"], current["counts"])
    )
    print(job["status"], job["counts"], job["usage"])
    # docs:end


asyncio.run(main())
