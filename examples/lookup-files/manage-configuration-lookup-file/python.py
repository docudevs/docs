import asyncio
import os
from pathlib import Path

from docudevs.docudevs_client import DocuDevsClient


async def main():
    client = DocuDevsClient(token=os.environ["API_KEY"])

    # docs:start
    with open("vendors.csv", "rb") as f:
        lookup_data = f.read()

    upload = await client.upload_configuration_lookup_file(
        "invoice-config", lookup_data, file_name="vendors.csv"
    )
    print(f"Upload status: {upload.status_code}")

    downloaded = await client.get_configuration_lookup_file("invoice-config")
    Path("vendors-downloaded.csv").write_bytes(downloaded)

    delete = await client.delete_configuration_lookup_file("invoice-config")
    print(f"Delete status: {delete.status_code}")
    # docs:end


asyncio.run(main())
