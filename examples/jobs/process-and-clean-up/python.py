import asyncio
import json
import os

from docudevs.docudevs_client import DocuDevsClient


async def main():
    client = DocuDevsClient(token=os.environ["API_KEY"])

    # docs:start
    with open("invoice.pdf", "rb") as f:
        document_data = f.read()

    job_guid = await client.submit_and_process_document(
        document=document_data,
        document_mime_type="application/pdf",
        prompt="Extract invoice data",
    )

    result = await client.wait_until_ready(job_guid, result_format="json")

    # Save the result locally before deleting the job
    with open(f"{job_guid}_result.json", "w") as f:
        json.dump(result, f, indent=2)

    # Clean up the job from DocuDevs
    delete_result = await client.delete_job(job_guid)
    if delete_result.status_code == 200:
        print(f"Cleaned up job: {delete_result.parsed['jobsDeleted']} deleted")
    # docs:end


asyncio.run(main())
