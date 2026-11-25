import asyncio
import os

from docudevs.docudevs_client import DocuDevsClient


async def main():
    client = DocuDevsClient(token=os.environ["API_KEY"])

    # docs:start
    case_id = 42  # id returned by create_case
    classify_job_id = 55  # id of a classify job of this case

    runs = await client.list_agent_runs(case_id, origin_job_id=classify_job_id, status="COMPLETED")
    for run in runs.get("items", []):
        print(run["runId"], run["status"], run["tokensUsed"])
    # docs:end


asyncio.run(main())
