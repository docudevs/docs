import asyncio
import os

from docudevs.docudevs_client import DocuDevsClient


async def main():
    client = DocuDevsClient(token=os.environ["DOCUDEVS_API_KEY"])

    # docs:start
    case_id = 42  # id of the case that holds the playbook documents
    anchor_job_guid = "<completed-contract-job>"  # jobGuid of a completed contract job
    suite_revision = 3  # revision returned by the test-suite read

    estimate = await client.estimate_contract_analysis_trial(
        case_id,
        anchor_job_guid=anchor_job_guid,
        suite_revision=suite_revision,
        trial_kind="scored",
    )

    trial = await client.create_contract_analysis_trial(
        case_id,
        anchor_job_guid=anchor_job_guid,
        idempotency_key="trial-key",
        suite_revision=suite_revision,
        trial_kind="scored",
        estimate_fingerprint=estimate.parsed["estimateFingerprint"],
        configuration_fingerprint=estimate.parsed["executionFingerprint"],
    )
    print(f"Trial ID: {trial.parsed['trialId']}")
    # docs:end


asyncio.run(main())
