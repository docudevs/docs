import asyncio
import json
import os

from docudevs.docudevs_client import DocuDevsClient


async def main():
    client = DocuDevsClient(token=os.environ["API_KEY"])

    # docs:start
    with open("fillable-form.pdf", "rb") as f:
        pdf_bytes = f.read()

    metadata = await client.extract_acroform_metadata(
        document=pdf_bytes,
        file_name="fillable-form.pdf",
        mime_type="application/pdf",
    )

    fields = metadata.get("fields", [])
    print(f"Direct fields: {len(fields)}")
    print(json.dumps(fields[:3], indent=2))
    # docs:end


asyncio.run(main())
