import asyncio
import os

from docudevs.docudevs_client import DocuDevsClient


async def main():
    client = DocuDevsClient(token=os.environ["API_KEY"])

    # docs:start
    operation_job_guid = "<operation-job-guid>"  # operation job GUID returned when the conversion was submitted

    pdf_bytes = await client.get_pdf_acroform(operation_job_guid, save_to="fillable.pdf")
    print(f"Saved fillable.pdf ({len(pdf_bytes)} bytes)")
    # docs:end


asyncio.run(main())
