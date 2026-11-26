import asyncio
import os

from docudevs.docudevs_client import DocuDevsClient


async def main():
    client = DocuDevsClient(token=os.environ["API_KEY"])

    # docs:start
    job_guid = "<job-guid>"  # GUID of a completed extraction job

    # invoice-config is a saved configuration that has an Excel template attached.
    # Name the output file .xlsm when the template is macro-enabled.
    await client.result_excel(job_guid, save_to="result.xlsx", configuration="invoice-config")
    # docs:end


asyncio.run(main())
