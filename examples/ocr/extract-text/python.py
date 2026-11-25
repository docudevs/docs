import asyncio
import os

from docudevs.docudevs_client import DocuDevsClient


async def main():
    client = DocuDevsClient(token=os.environ["API_KEY"])

    with open("document.pdf", "rb") as f:
        document_data = f.read()

    job_id = await client.submit_and_ocr_document(
        document=document_data,
        document_mime_type="application/pdf",
        ocr="DEFAULT",
        ocr_format="markdown",
    )

    text = await client.wait_until_ready(job_id)
    print(text)


asyncio.run(main())
