import asyncio
import os

from docudevs.docudevs_client import DocuDevsClient


async def main():
    client = DocuDevsClient(token=os.environ["API_KEY"])

    # docs:start
    job_guid = "<job-guid>"  # GUID of a completed job that was processed with source_locations enabled

    sources = await client.get_source_locations(job_guid)
    layout = await client.get_document_layout(job_guid, include=["paragraphs"])

    # Print each pointer, its source reference and the paragraph text for that reference.
    # References to tables or figures print an empty string.
    if sources and layout:
        paragraph_text = {p["id"]: p["content"] for p in layout.get("paragraphs", [])}
        for pointer, location in sources.get("locations", {}).items():
            for ref in location.get("sourceRefs", []):
                print(pointer, ref, paragraph_text.get(ref, ""))
    # docs:end


asyncio.run(main())
