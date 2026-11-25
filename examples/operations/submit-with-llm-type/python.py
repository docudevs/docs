import asyncio
import os

from docudevs.docudevs_client import DocuDevsClient


async def main():
    client = DocuDevsClient(token=os.environ["API_KEY"])

    # docs:start
    job_guid = "<job-guid>"  # GUID of a completed document job

    response = await client.submit_operation_with_parameters(
        job_guid=job_guid,
        operation_type="error-analysis",
        llm_type="HIGH",  # use the high-quality model tier for the analysis
    )

    print(f"Operation job GUID: {response.job_guid}")
    print(f"Status: {response.status}")
    # docs:end


asyncio.run(main())
