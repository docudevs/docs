import asyncio
import os

from docudevs.docudevs_client import DocuDevsClient


async def main():
    client = DocuDevsClient(token=os.environ["API_KEY"])

    # docs:start
    with open("handwritten.pdf", "rb") as f:
        document_data = f.read()

    # ocr: "DEFAULT", "PREMIUM" or "AUTO". ocr_format: "markdown" or "plain"
    job_id = await client.submit_and_ocr_document(
        document=document_data,
        document_mime_type="application/pdf",
        ocr="PREMIUM",
        ocr_format="plain",
    )

    text = await client.wait_until_ready(job_id)
    print(text)
    # docs:end


asyncio.run(main())
