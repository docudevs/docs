import asyncio
import os

from docudevs.docudevs_client import DocuDevsClient

# docs:start
from docudevs.models import TemplateFillRequest
# docs:end


async def main():
    client = DocuDevsClient(token=os.environ["API_KEY"])

    # docs:start
    fill_request = TemplateFillRequest(
        fields={
            "customerName": "Acme Corp",
            "invoiceNumber": "INV-2024-001",
            "totalAmount": "1500.00",
            "paid": True,
        }
    )

    # Retries while a freshly uploaded template finishes preparing
    response = await client.fill_with_retry(
        name="invoice",
        body=fill_request,
        timeout=30,
        poll_interval=1,
    )

    if response.status_code != 200:
        raise SystemExit(f"Fill failed: {response.status_code}")

    with open("filled_invoice.pdf", "wb") as f:
        f.write(response.content)
    print("Saved filled_invoice.pdf")
    # docs:end


asyncio.run(main())
