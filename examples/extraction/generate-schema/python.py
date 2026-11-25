import asyncio
import json
import os

from docudevs.docudevs_client import DocuDevsClient


async def main():
    client = DocuDevsClient(token=os.environ["API_KEY"])

    # docs:start
    with open("sample-invoice.pdf", "rb") as f:
        document_data = f.read()

    job_guid = await client.generate_schema(
        document=document_data,
        document_mime_type="application/pdf",
        instructions="Focus on extracting the invoice number, date, and all line items with prices.",
    )

    schema = await client.wait_until_ready(job_guid, result_format="json")
    print("Generated schema:")
    print(json.dumps(schema, indent=2))
    # docs:end


asyncio.run(main())
