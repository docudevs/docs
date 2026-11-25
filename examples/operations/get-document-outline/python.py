import asyncio
import json
import os

from docudevs.docudevs_client import DocuDevsClient


async def main():
    client = DocuDevsClient(token=os.environ["API_KEY"])

    # docs:start
    job_guid = "<job-guid>"  # GUID of a job that already has an outline artifact

    outline = await client.get_document_outline(job_guid)
    print(json.dumps(outline, indent=2))
    # docs:end


asyncio.run(main())
