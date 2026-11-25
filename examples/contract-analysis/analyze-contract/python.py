import asyncio
import json
import os

from docudevs.docudevs_client import DocuDevsClient


async def main():
    client = DocuDevsClient(token=os.environ["API_KEY"])

    # docs:start
    job_guid = "<completed-contract-job>"  # jobGuid of a completed contract job
    knowledge_base_id = 42  # id of the case whose profile is published

    submission = await client.submit_operation_with_parameters(
        job_guid,
        "contract-analysis",
        custom_parameters={"knowledgeBaseId": knowledge_base_id},
    )
    if submission is None or not getattr(submission, "job_guid", None):
        raise SystemExit("Contract analysis was not accepted")

    # The submission carries the job_guid of the analysis job, not of the contract job
    result = await client.wait_until_ready(
        submission.job_guid, timeout=600, poll_interval=5, result_format="json"
    )
    print(json.dumps(result, indent=2))
    # docs:end


asyncio.run(main())
