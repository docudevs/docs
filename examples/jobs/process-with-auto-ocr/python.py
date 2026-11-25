import asyncio
import os

from docudevs.docudevs_client import DocuDevsClient


async def main():
    client = DocuDevsClient(token=os.environ["API_KEY"])

    # docs:start
    with open("scan.pdf", "rb") as f:
        document_data = f.read()

    # AUTO starts with the default OCR and switches to PREMIUM when the quality is low
    job_guid = await client.submit_and_process_document(
        document=document_data,
        document_mime_type="application/pdf",
        prompt="Extract the key fields",
        ocr="AUTO",
    )
    await client.wait_until_ready(job_guid, result_format="json")

    status_response = await client.status(guid=job_guid)
    if status_response.status_code != 200:
        raise RuntimeError(f"HTTP {status_response.status_code}")

    job = status_response.parsed
    print(f"Quality: {job.quality_score} ({job.quality_category})")
    # docs:end


asyncio.run(main())
