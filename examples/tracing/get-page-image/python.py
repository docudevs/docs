import asyncio
import os

from docudevs.docudevs_client import DocuDevsClient


async def main():
    client = DocuDevsClient(token=os.environ["API_KEY"])

    # docs:start
    job_guid = "<job-guid>"  # GUID of a completed job

    # Get the thumbnail of page 0
    image_bytes = await client.get_image(job_guid, page_index=0)
    if image_bytes:
        with open("page_0.png", "wb") as f:
            f.write(image_bytes)
    # docs:end


asyncio.run(main())
