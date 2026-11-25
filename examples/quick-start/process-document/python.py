import asyncio
import json
import os

from docudevs.docudevs_client import DocuDevsClient


async def main():
    client = DocuDevsClient(token=os.environ["API_KEY"])

    with open("your-document.pdf", "rb") as f:
        document_data = f.read()

    job_id = await client.submit_and_process_document(
        document=document_data,
        document_mime_type="application/pdf",
        prompt="Extract all key information from this document",
    )
    print(f"Document submitted for processing. Job ID: {job_id}")

    data = await client.wait_until_ready(job_id, result_format="json")
    print("Processing complete!")

    print("\nExtracted Data:")
    print(json.dumps(data, indent=2))


asyncio.run(main())
