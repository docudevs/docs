import asyncio
import os

from docudevs.docudevs_client import DocuDevsClient


async def main():
    client = DocuDevsClient(token=os.environ["API_KEY"])

    # docs:start
    run_id = "<run-id>"  # runId returned when the run was created
    response = await client.get_contract_analysis_run_result(run_id)
    result = response.parsed

    print(f"Result fingerprint: {result.get('resultFingerprint')}")
    # docs:end


asyncio.run(main())
