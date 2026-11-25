import asyncio
import json
import os

# docs:start
from docudevs.models import ExcelTemplateConfig, UploadCommand
# docs:end
from docudevs.docudevs_client import DocuDevsClient


async def main():
    client = DocuDevsClient(token=os.environ["API_KEY"])

    # docs:start
    schema = {
        "type": "object",
        "properties": {
            "vendor_name": {"type": "string"},
            "invoice_number": {"type": "string"},
            "total_amount": {"type": "number"},
        },
    }

    config = UploadCommand(
        prompt="Extract the vendor, invoice number and total of each invoice.",
        schema=json.dumps(schema),
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
