import asyncio
import os

# docs:start
from pydantic import BaseModel

from docudevs import json_schema
from docudevs.models import ExcelTemplateConfig, UploadCommand
# docs:end
from docudevs.docudevs_client import DocuDevsClient


async def main():
    client = DocuDevsClient(token=os.environ["API_KEY"])

    # docs:start
    class Invoice(BaseModel):
        vendor_name: str | None = None
        invoice_number: str | None = None
        total_amount: float

    config = UploadCommand(
        prompt="Extract the vendor, invoice number and total of each invoice.",
        schema=json_schema(Invoice),
        # start_row and start_col are 0-based cell coordinates: 0 and 0 target cell A1
        excel_template=ExcelTemplateConfig(
            start_row=0,
            start_col=0,
            include_headers=True,
            sheet_name="Data",
        ),
    )

    response = await client.save_configuration("invoice-config", config)
    print(f"Configuration saved (status {response.status_code})")
    # docs:end


asyncio.run(main())
