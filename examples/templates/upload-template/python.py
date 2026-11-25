import asyncio
import os

from docudevs.docudevs_client import DocuDevsClient


async def main():
    client = DocuDevsClient(token=os.environ["API_KEY"])

    with open("invoice_template.pdf", "rb") as f:
        template_bytes = f.read()

    response = await client.upload_template(
        name="invoice",
        document=template_bytes,
        file_name="invoice_template.pdf",
        mime_type="application/pdf",
    )
    print(f"Upload status: {response.status_code}")


asyncio.run(main())
