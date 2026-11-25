import asyncio
import os

from docudevs.docudevs_client import DocuDevsClient


async def main():
    client = DocuDevsClient(token=os.environ["API_KEY"])

    # docs:start
    case_id = 42  # id of the case that holds the playbook documents
    run_id = "<run-id>"  # runId returned when the run was created
    result_fingerprint = "<result-fingerprint>"  # resultFingerprint of that run's result
    suite_revision = 4  # revision returned by the test-suite read

    confirmed = await client.confirm_contract_analysis_findings(
        case_id,
        run_id=run_id,
        result_fingerprint=result_fingerprint,
        expected_suite_revision=suite_revision,
        confirmations=[
            {
                "clause_id": "term",
                "expected_verdict": "flagged",
                "expected_red_flags": {"auto-renewal": "triggered"},
                "importance": "mandatory",
                "notes": "Confirmed by review.",
            },
        ],
    )
    print(f"Suite case: {confirmed.parsed['suiteCaseId']}")
    # docs:end


asyncio.run(main())
