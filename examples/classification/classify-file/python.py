import asyncio
import os

from docudevs.docudevs_client import DocuDevsClient


async def main():
    client = DocuDevsClient(token=os.environ["API_KEY"])

    # docs:start
    case_id = 42  # id of a records case with an active category tree

    # Compresses the file and uploads it in blocks. The columns are the source column names
    # of the dataset, and an uploaded file needs a token budget.
    # Raises ClassifyJobFailed when the job fails or is cancelled, and TimeoutError after 3600 seconds
    job = await client.classify(
        case_id,
        path="weekly.csv",
        config={"max_total_tokens": 5000000},
        wait=True,
    )
    print(job["jobId"], job["status"], job["counts"])
    # docs:end


asyncio.run(main())
