import asyncio
import os

from docudevs.docudevs_client import DocuDevsClient


async def main():
    client = DocuDevsClient(token=os.environ["DOCUDEVS_API_KEY"])

    # docs:start
    case_id = 42  # id of the case that holds the playbook documents
    generation_job_guid = "<generation-job-guid>"  # jobGuid returned by the generate request

    response = await client.get_contract_analysis_generation_comparison(case_id, generation_job_guid)
    comparison = response.parsed

    applied = await client.apply_contract_analysis_generation(
        case_id,
        generation_job_guid,
        expected_draft_revision=comparison["currentDraftRevision"],
        expected_draft_fingerprint=comparison["currentDraftFingerprint"],
        decisions={"hours": {"action": "accept_proposed"}},
    )
    print(f"New draft revision: {applied.parsed['draftRevision']}")
    # docs:end


asyncio.run(main())
