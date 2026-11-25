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
    with open("your-document.pdf", "rb") as f:
        document_data = f.read()

    schema = {
        "type": "object",
        "properties": {
            "invoice_number": {"type": "string"},
            "date": {"type": "string", "format": "date"},
            "vendor": {"type": "string"},
            "total": {"type": "number"},
            "currency": {"type": "string"},
        },
        "required": ["invoice_number", "total"],
    }

    job_id = await client.submit_and_process_document(
        document=document_data,
        document_mime_type="application/pdf",
        prompt="Extract invoice data according to the schema",
        schema=json_schema(schema),
    )

    data = await client.wait_until_ready(job_id, result_format="json")
    print(json.dumps(data, indent=2))
    # docs:end


asyncio.run(main())
