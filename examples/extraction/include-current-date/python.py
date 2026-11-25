import asyncio
import json
import os

from docudevs import json_schema
from docudevs.docudevs_client import DocuDevsClient


async def main():
    client = DocuDevsClient(token=os.environ["API_KEY"])

    with open("registration.pdf", "rb") as f:
        document_data = f.read()

    schema = {
        "type": "object",
        "properties": {
            "registration_number": {"type": "string"},
            "first_registration_date": {"type": "string", "format": "date"},
            "vehicle_age_years": {"type": "number"},
        },
    }

    job_guid = await client.submit_and_process_document(
        document=document_data,
        document_mime_type="application/pdf",
        prompt="Extract vehicle registration details including vehicle age in years",
        schema=json_schema(schema),
        include_current_date=True,
    )

    result = await client.wait_until_ready(job_guid, result_format="json")
    print(json.dumps(result, indent=2))


asyncio.run(main())
