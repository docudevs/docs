import asyncio
import os

from docudevs.docudevs_client import DocuDevsClient


async def main():
    client = DocuDevsClient(token=os.environ["API_KEY"])

    # docs:start
    response = await client.upload_excel_template("invoice-config", "template.xlsx")
    print(f"Upload status: {response.status_code}")
    # docs:end


asyncio.run(main())
