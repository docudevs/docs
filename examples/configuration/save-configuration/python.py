import asyncio
import json
import os

from docudevs.docudevs_client import DocuDevsClient
from docudevs.models import UploadCommand


async def main():
    client = DocuDevsClient(token=os.environ["API_KEY"])

    schema = {
        "type": "object",
        "properties": {
            "vendor_name": {"type": "string"},
            "invoice_number": {"type": "string"},
            "total_amount": {"type": "number"},
            "date": {"type": "string", "format": "date"},
        },
        "required": ["vendor_name", "total_amount"],
    }

    config = UploadCommand(
        prompt="Extract invoice data including vendor details, line items, and totals.",
        schema=json.dumps(schema),
        ocr="PREMIUM",
        llm="HIGH",
        extraction_mode="STEPS",
        barcodes=True,
    )

    # Save the configuration under the name "invoice-config"
    response = await client.save_configuration("invoice-config", config)
    print(f"Configuration saved (status {response.status_code})")


asyncio.run(main())
