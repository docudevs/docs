import asyncio
import os

from docudevs.docudevs_client import DocuDevsClient


async def main():
    client = DocuDevsClient(token=os.environ["API_KEY"])

    # docs:start
    job_guid = "<job-guid>"  # GUID of a completed extraction job

    xlsx_bytes = await client.result_excel(
        job_guid, save_to="invoices.xlsx", configuration="invoice-config"
    )
    print(f"Saved {len(xlsx_bytes)} bytes to invoices.xlsx")
    # docs:end


asyncio.run(main())
