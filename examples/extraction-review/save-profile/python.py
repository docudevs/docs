import asyncio
import json
import os

from docudevs.docudevs_client import DocuDevsClient


async def main():
    client = DocuDevsClient(token=os.environ["API_KEY"])

    # docs:start
    # Preview and save need a signed-in administrator: an organization API key is rejected.
    schema = {"type": "object", "properties": {"total": {"type": "number"}}}

    preview = await client.preview_extraction_review_profile(
        "invoice-v1",
        {"schemaJson": json.dumps(schema), "overlay": None},
    )
    saved = await client.save_extraction_review_profile(
        "invoice-v1",
        {
            "expectedRevision": preview.parsed["revision"],
            "enabled": True,
            "schemaJson": preview.parsed["schemaJson"],
            "overlay": None,
        },
    )
    print(f"Saved revision {saved.parsed['revision']}, enabled: {saved.parsed['enabled']}")
    # docs:end


asyncio.run(main())
