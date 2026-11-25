import asyncio
import json
import os

from docudevs.docudevs_client import DocuDevsClient


async def main():
    client = DocuDevsClient(token=os.environ["API_KEY"])

    # docs:start
    run_id = "<run-id>"  # runId returned when the run was created
    response = await client.get_contract_analysis_run_reviews(run_id)

    print(json.dumps(response.parsed, indent=2))
    # docs:end


asyncio.run(main())
