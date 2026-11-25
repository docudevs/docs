import asyncio
import os

from docudevs.docudevs_client import DocuDevsClient


async def main():
    client = DocuDevsClient(token=os.environ["API_KEY"])

    # docs:start
    fields = await client.wait_for_template_metadata(
        "invoice",
        timeout=60,
        poll_interval=1,
    )

    for field in fields:
        print(f"- {field['name']} ({field['type']})")
    # docs:end


asyncio.run(main())
