import asyncio
import os
from pathlib import Path

from docudevs.docudevs_client import DocuDevsClient


async def main():
    client = DocuDevsClient(token=os.environ["API_KEY"])

    # docs:start
    batch_guid = "<batch-guid>"  # GUID of a batch whose documents are uploaded and not yet processed

    with open("vendors-2024.csv", "rb") as f:
        lookup_data = f.read()

    upload = await client.upload_batch_lookup_file(
        batch_guid, lookup_data, file_name="vendors-2024.csv"
    )
    print(f"Upload status: {upload.status_code}")

    downloaded = await client.get_batch_lookup_file(batch_guid)
    Path("vendors-downloaded.csv").write_bytes(downloaded)

    delete = await client.delete_batch_lookup_file(batch_guid)
    print(f"Delete status: {delete.status_code}")
    # docs:end


asyncio.run(main())
