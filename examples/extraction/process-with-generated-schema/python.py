import asyncio
import json
import os

# docs:start
from docudevs import json_schema
# docs:end
from docudevs.docudevs_client import DocuDevsClient


async def main():
    client = DocuDevsClient(token=os.environ["API_KEY"])

    # docs:start
    # invoice-schema.json holds the schema from the generation step, after review
    with open("invoice-schema.json") as f:
        schema = json.load(f)

    with open("new-invoice.pdf", "rb") as f:
        document_data = f.read()

    job_guid = await client.submit_and_process_document(
        document=document_data,
        document_mime_type="application/pdf",
        schema=json_schema(schema),
    )

    result = await client.wait_until_ready(job_guid, result_format="json")
    print(json.dumps(result, indent=2))
    # docs:end


asyncio.run(main())
