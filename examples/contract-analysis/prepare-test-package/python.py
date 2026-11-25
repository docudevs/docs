import asyncio
import os

from docudevs.docudevs_client import DocuDevsClient


async def main():
    client = DocuDevsClient(token=os.environ["API_KEY"])

    # docs:start
    case_id = 42  # id of the case that holds the playbook documents
    anchor_job_guid = "<completed-contract-job>"  # jobGuid of a completed contract job

    package = await client.prepare_contract_analysis_test_package(
        case_id,
        anchor_job_guid=anchor_job_guid,
        idempotency_key="package-1",
        context={"asOfDate": "2026-09-22"},
    )
    print(f"Package {package.parsed['packageId']}, fingerprint {package.parsed['fingerprint']}")
    # docs:end


asyncio.run(main())
