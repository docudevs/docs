import asyncio
import json
import os

from docudevs.docudevs_client import DocuDevsClient


async def main():
    client = DocuDevsClient(token=os.environ["API_KEY"])

    # docs:start
    parent_job_guid = "<job-guid>"  # GUID of a completed document job

    summary = await client.submit_and_wait_for_generative_task(
        parent_job_id=parent_job_guid,
        prompt="Summarize the main findings",
        model="DEFAULT",
    )

    # The result is a JSON string inside the response object
    result_data = json.loads(summary.result)
    print(result_data["generated_text"])
    # docs:end


asyncio.run(main())
