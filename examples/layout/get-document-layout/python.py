import asyncio
import json
import os

from docudevs.docudevs_client import DocuDevsClient


async def main():
    client = DocuDevsClient(token=os.environ["API_KEY"])

    # docs:start
    job_guid = "<job-guid>"  # GUID of a completed job that was processed with OCR

    # Only page 1, and only its words and paragraphs
    layout = await client.get_document_layout(job_guid, pages="1", include=["words", "paragraphs"])
    quality = await client.get_ocr_quality(job_guid)

    # Both methods return None when the artifact does not exist
    print(json.dumps(layout, indent=2))
    print(json.dumps(quality, indent=2))
    # docs:end


asyncio.run(main())
