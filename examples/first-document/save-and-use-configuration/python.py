import asyncio
import json
import os

from docudevs.docudevs_client import DocuDevsClient
# docs:start
import datetime

from pydantic import BaseModel

from docudevs import json_schema
from docudevs.models import UploadCommand
# docs:end


async def main():
    client = DocuDevsClient(token=os.environ["API_KEY"])

    # docs:start
    config_name = "invoice_processing_standard"

    class Invoice(BaseModel):
        invoice_number: str | None = None
        date: datetime.date | None = None
        vendor: str | None = None
        total: float | None = None

    # Save the configuration under a name
    save_response = await client.save_configuration(
        config_name,
        UploadCommand(
            prompt="Extract comprehensive invoice data",
            schema=json_schema(Invoice),
            llm="HIGH",
        ),
    )
    print(f"Configuration saved: {config_name} (status {save_response.status_code})")

    # Upload a document and process it with the saved configuration
    with open("sample_invoice.pdf", "rb") as f:
        document_data = f.read()

    job_id = await client.submit_and_process_document_with_configuration(
        document=document_data,
        document_mime_type="application/pdf",
        configuration_name=config_name,
    )

    result = await client.wait_until_ready(job_id, result_format="json")
    print("Processing with Named Configuration Result:")
    print(json.dumps(result, indent=2))
    # docs:end


asyncio.run(main())
