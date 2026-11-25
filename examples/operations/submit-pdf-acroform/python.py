import asyncio
import os

from docudevs.docudevs_client import DocuDevsClient


async def main():
    client = DocuDevsClient(token=os.environ["API_KEY"])

    # docs:start
    parent_job_guid = "<job-guid>"  # GUID of a completed PDF job whose page images exist (processed with ocr PREMIUM)

    response = await client.submit_pdf_acroform_operation(parent_job_guid)

    print(f"Operation job GUID: {response.job_guid}")
    print(f"Status: {response.status}")
    # docs:end


asyncio.run(main())
