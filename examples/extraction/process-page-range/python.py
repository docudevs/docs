import asyncio
import json
import os

from docudevs.docudevs_client import DocuDevsClient


async def main():
    client = DocuDevsClient(token=os.environ["API_KEY"])

    with open("document.pdf", "rb") as f:
        document_data = f.read()

    # Process only page 1
    job_guid = await client.submit_and_process_document(
        document=document_data,
        document_mime_type="application/pdf",
        prompt="Extract the title and summary.",
        page_range=[1],
    )

    result = await client.wait_until_ready(job_guid, result_format="json")
    print(json.dumps(result, indent=2))


asyncio.run(main())
