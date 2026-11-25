import asyncio
import os

from docudevs.docudevs_client import DocuDevsClient


async def main():
    client = DocuDevsClient(token=os.environ["API_KEY"])

    # docs:start
    run_id = "<run-id>"  # runId returned when the run was created
    response = await client.get_contract_analysis_run(run_id)
    run = response.parsed

    print(f"Status: {run['status']}")
    print(f"Result available: {run['resultAvailable']}")
    # docs:end


asyncio.run(main())
