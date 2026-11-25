import asyncio
import os

from docudevs.docudevs_client import DocuDevsClient


async def main():
    client = DocuDevsClient(token=os.environ["API_KEY"])

    # docs:start
    case_id = 42  # id of a records case with an active category tree
    keys = ["SKU-1001", "SKU-1002", "SKU-1003"]  # record keys of the case's records

    # Raises ClassifyJobFailed when the job fails or is cancelled, and TimeoutError after 3600 seconds
    job = await client.classify(
        case_id,
        keys=keys,
        config={"maxTotalTokens": 400000, "investigate": {"enabled": True, "maxRows": 10}},
        wait=True,
    )
    print(f"Classify job {job['jobId']} completed; the investigation run starts from it")
    # docs:end


asyncio.run(main())
