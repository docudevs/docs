import asyncio
import os

from docudevs.docudevs_client import DocuDevsClient


async def main():
    client = DocuDevsClient(token=os.environ["API_KEY"])

    # docs:start
    run_id = "<run-id>"  # runId of the registered run

    run = await client.get_extraction_review_run(run_id)
    identity = run.parsed["identity"]

    claim = await client.submit_extraction_review_claims(
        identity["runId"],
        reference={
            "jobGuid": identity["jobGuid"],
            "artifactId": identity["artifactId"],
            "resultFingerprint": identity["resultFingerprint"],
        },
        expected_review_revision=run.parsed["reviewRevision"],
        idempotency_key="evaluator-2026-10-06-0001",
        claims=[{
            "claimId": "check-total-0001",
            "claimRevision": 1,
            "identity": {
                "reporterId": "invoice-checker",
                "externalIssueId": "check-0001",
                "referenceVersion": "dataset-2026-10",
                "comparatorVersion": "2",
            },
            "address": {"scope": "BASE", "pointer": "/total"},
            "observed": {"present": True, "valueJson": "0"},
            "proposedValueJson": "125.5",
            "rationaleJson": '"reference differs from extracted value"',
            "priority": "HIGH",
        }],
        audit={"evidenceArtifactPaths": [], "exampleEligibility": False},
    )
    print(f"Review run: {claim.parsed['event']['runId']}")
    # docs:end


asyncio.run(main())
