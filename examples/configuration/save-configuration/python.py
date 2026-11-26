import asyncio
import datetime
import os

from pydantic import BaseModel

from docudevs import json_schema
from docudevs.docudevs_client import DocuDevsClient
from docudevs.models import UploadCommand


async def main():
    client = DocuDevsClient(token=os.environ["API_KEY"])

    class Invoice(BaseModel):
        vendor_name: str
        invoice_number: str | None = None
        total_amount: float
        date: datetime.date | None = None

    config = UploadCommand(
        prompt="Extract invoice data including vendor details, line items, and totals.",
        schema=json_schema(Invoice),
        ocr="PREMIUM",
        llm="HIGH",
        extraction_mode="STEPS",
        barcodes=True,
    )

    # Save the configuration under the name "invoice-config"
    response = await client.save_configuration("invoice-config", config)
    print(f"Configuration saved (status {response.status_code})")


asyncio.run(main())
