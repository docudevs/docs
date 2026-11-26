import asyncio
import os

from docudevs.docudevs_client import DocuDevsClient


async def main():
    client = DocuDevsClient(token=os.environ["API_KEY"])

    # docs:start
    with open("invoice.pdf", "rb") as f:
        document_data = f.read()

    # extraction_mode="OCR" returns the text only; barcodes=True turns on the scan
    job_id = await client.submit_and_process_document(
        document=document_data,
        document_mime_type="application/pdf",
        ocr="DEFAULT",
        extraction_mode="OCR",
        barcodes=True,
    )

    text = await client.wait_until_ready(job_id, timeout=600)
    print(text)
    # docs:end


asyncio.run(main())
