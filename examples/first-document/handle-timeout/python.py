import asyncio
import os

from docudevs.docudevs_client import DocuDevsClient


async def main():
    client = DocuDevsClient(token=os.environ["API_KEY"])

    # docs:start
    try:
        with open("sample_invoice.pdf", "rb") as f:
            document_data = f.read()
    except FileNotFoundError:
        print("Document file not found: sample_invoice.pdf")
        return

    job_id = await client.submit_and_process_document(
        document=document_data,
        document_mime_type="application/pdf",
        prompt="Extract key information from this document",
    )
    print(f"Processing job {job_id}...")

    try:
        # Wait up to 5 minutes for the result
        result = await client.wait_until_ready(job_id, timeout=300, result_format="json")
        print("Processing successful!")
        print(result)
    except TimeoutError:
        status_response = await client.status(job_id)
        print(f"Processing timed out for job {job_id}")
        print(f"Current status: {status_response.parsed.status}")
    except Exception as error:
        print(f"Processing failed: {error}")
    # docs:end


asyncio.run(main())
