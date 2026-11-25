import asyncio
import os

from docudevs.docudevs_client import DocuDevsClient


async def main():
    client = DocuDevsClient(token=os.environ["API_KEY"])

    parent_job_guid = "<job-guid>"  # GUID of a completed PDF job whose page images exist (processed with ocr PREMIUM)

    result = await client.submit_and_wait_for_pdf_acroform_operation(
        parent_job_guid,
        ocr="PREMIUM",
        page_range=[1],
        min_confidence=0.6,
        max_fields_per_page=300,
        timeout=600,
        save_to="fillable.pdf",
    )

    print(f"Operation job GUID: {result.operation_job_guid}")


asyncio.run(main())
