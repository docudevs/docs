import asyncio
import os

from docudevs.docudevs_client import DocuDevsClient


async def main():
    client = DocuDevsClient(token=os.environ["API_KEY"])

    # docs:start
    anchor_job_guid = "<completed-contract-job>"  # jobGuid of a completed contract job
    draft_revision = 4  # draftRevision returned by the last import or draft save

    run = await client.create_contract_analysis_run(
        anchor_job_guid,
        knowledge_base_id=42,
        idempotency_key="draft-run-1",
        mode="draft_trial",
        draft_revision=draft_revision,
    )
    print(f"Run ID: {run.parsed['runId']}")
    # docs:end


asyncio.run(main())
