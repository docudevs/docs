import asyncio
import json
import os

from docudevs.docudevs_client import DocuDevsClient


async def main():
    client = DocuDevsClient(token=os.environ["API_KEY"])

    # docs:start
    with open("safety-data-sheet.pdf", "rb") as f:
        document_data = f.read()

    # Upload one document per extraction and process only the pages that hold the section
    async def extract_pages(page_range, prompt):
        job_guid = await client.submit_and_process_document(
            document=document_data,
            document_mime_type="application/pdf",
            prompt=prompt,
            page_range=page_range,
        )
        return await client.wait_until_ready(job_guid, result_format="json")

    # Section 1: Identification (page 1)
    identification = await extract_pages(
        [1], "Extract product name, manufacturer, and emergency contact."
    )

    # Section 9: Physical properties (pages 8-9)
    properties = await extract_pages(
        [8, 9], "Extract appearance, odor, boiling point, and flash point."
    )

    # Sections 14-16: Transport and regulatory (pages 12-14)
    regulatory = await extract_pages(
        [12, 13, 14], "Extract transport classification and regulatory information."
    )

    print(json.dumps([identification, properties, regulatory], indent=2))
    # docs:end


asyncio.run(main())
