import asyncio
import json
import os

from docudevs.docudevs_client import DocuDevsClient


async def main():
    client = DocuDevsClient(token=os.environ["API_KEY"])

    # docs:start
    batch_guid = await client.create_batch(max_concurrency=3)

    for filename in ["invoice1.pdf", "invoice2.pdf"]:
        with open(filename, "rb") as f:
            await client.upload_batch_document(
                batch_guid=batch_guid,
                document=f.read(),
                mime_type="application/pdf",
                file_name=filename,
            )

    # The batch lookup file overrides the lookup file of invoice-config for this batch
    with open("vendors-2024.csv", "rb") as f:
        await client.upload_batch_lookup_file(batch_guid, f.read(), file_name="vendors-2024.csv")

    await client.process_batch_with_configuration(batch_guid, "invoice-config")

    results = await client.wait_until_ready(batch_guid, timeout=600, result_format="json")
    print(json.dumps(results, indent=2))
    # docs:end


asyncio.run(main())
