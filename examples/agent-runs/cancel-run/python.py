import asyncio
import os

from docudevs.docudevs_client import DocuDevsClient


async def main():
    client = DocuDevsClient(token=os.environ["API_KEY"])

    # docs:start
    case_id = 42  # id returned by create_case
    run_id = 9001  # id returned when the run was started

    cancelled = await client.cancel_agent_run(case_id, run_id)
    print(cancelled["runId"], cancelled["status"])
    # docs:end


asyncio.run(main())
