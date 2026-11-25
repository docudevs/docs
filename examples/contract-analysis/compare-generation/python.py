import asyncio
import os

from docudevs.docudevs_client import DocuDevsClient


async def main():
    client = DocuDevsClient(token=os.environ["API_KEY"])

    # docs:start
    case_id = 42  # id of the case that holds the playbook documents
    generation_job_guid = "<generation-job-guid>"  # jobGuid returned by the generate request

    response = await client.get_contract_analysis_generation_comparison(case_id, generation_job_guid)
    comparison = response.parsed

    print(f"Classification: {comparison['classification']}")
    print(f"Current draft revision: {comparison['currentDraftRevision']}")
    print(f"Elements to review: {len(comparison['elements'])}")
    # docs:end


asyncio.run(main())
