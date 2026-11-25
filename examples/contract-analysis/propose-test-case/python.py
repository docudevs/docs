import asyncio
import os

from docudevs.docudevs_client import DocuDevsClient


async def main():
    client = DocuDevsClient(token=os.environ["API_KEY"])

    # docs:start
    case_id = 42  # id of the case that holds the playbook documents
    run_id = "<run-id>"  # runId returned when the run was created
    result_fingerprint = "<result-fingerprint>"  # resultFingerprint of that run's result

    suite = (await client.get_contract_analysis_test_suite(case_id)).parsed

    proposal = await client.propose_contract_analysis_finding_test_case(
        case_id,
        run_id=run_id,
        result_fingerprint=result_fingerprint,
        clause_id="hours",
        name="Hours-clause omission",
        expected_verdict="flagged",
        reason="Reviewer correction",
        expected_suite_revision=suite["revision"],
        idempotency_key="proposal-1",
    )
    print(f"Proposed case: {proposal.parsed['proposedCaseId']}")
    # docs:end


asyncio.run(main())
