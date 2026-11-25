import asyncio
import json
import os

from docudevs.docudevs_client import DocuDevsClient


async def main():
    client = DocuDevsClient(token=os.environ["API_KEY"])

    # docs:start
    with open("invoice.pdf", "rb") as f:
        document_data = f.read()

    # Process the document using the saved configuration
    job_guid = await client.submit_and_process_document_with_configuration(
        document=document_data,
        document_mime_type="application/pdf",
        configuration_name="invoice-config",
    )

    result = await client.wait_until_ready(job_guid, result_format="json")
    print(json.dumps(result, indent=2))
    # docs:end


asyncio.run(main())
