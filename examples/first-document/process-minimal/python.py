import asyncio
import json
import os

from docudevs.docudevs_client import DocuDevsClient


async def main():
    client = DocuDevsClient(token=os.environ["API_KEY"])

    with open("sample_invoice.pdf", "rb") as f:
        document_data = f.read()

    job_id = await client.submit_and_process_document(
        document=document_data,
        document_mime_type="application/pdf",
    )
    print(f"Job submitted: {job_id}")

    result = await client.wait_until_ready(job_id, result_format="json", poll_interval=2)

    print("Basic Processing Result:")
    print(json.dumps(result, indent=2))


asyncio.run(main())
