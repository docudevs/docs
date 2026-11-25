import asyncio
import json
import os

from docudevs.docudevs_client import DocuDevsClient


async def main():
    client = DocuDevsClient(token=os.environ["DOCUDEVS_API_KEY"])

    # docs:start
    with open("your-document.pdf", "rb") as f:
        document_data = f.read()

    ocr_job_id = await client.submit_and_ocr_document(
        document=document_data,
        document_mime_type="application/pdf",
        ocr="PREMIUM",
        ocr_format="markdown",
    )
    await client.wait_until_ready(ocr_job_id)

    analysis = await client.submit_and_wait_for_generative_task(
        parent_job_id=ocr_job_id,
        prompt="Summarize this document and explain what type of document it is and its main purpose.",
    )

    result_data = json.loads(analysis.result)
    print("\nAI Analysis:")
    print(result_data["generated_text"])
    # docs:end


asyncio.run(main())
