import asyncio
import os
from pathlib import Path

from docudevs.docudevs_client import DocuDevsClient


async def main():
    client = DocuDevsClient(token=os.environ["API_KEY"])

    # docs:start
    # descriptor.json is written by the previous example.
    # Preview and save need a signed-in administrator: an organization API key is rejected.
    descriptor_json = Path("descriptor.json").read_text()

    preview = await client.preview_extraction_review_profile(
        "invoice-v1",
        {"clientPipelineJson": descriptor_json},
    )
    saved = await client.save_extraction_review_profile(
        "invoice-v1",
        {
            "expectedRevision": preview.parsed["revision"],
            "clientPipelineJson": preview.parsed["clientPipelineJson"],
            "enabled": True,
        },
    )
    print(f"Saved revision {saved.parsed['revision']}")
    print(f"Producer fingerprint: {saved.parsed['producerFingerprint']}")
    # docs:end


asyncio.run(main())
