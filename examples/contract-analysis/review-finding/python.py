import asyncio
import os

from docudevs.docudevs_client import DocuDevsClient


async def main():
    client = DocuDevsClient(token=os.environ["DOCUDEVS_API_KEY"])

    # docs:start
    run_id = "<run-id>"  # runId returned when the run was created
    result_fingerprint = "<result-fingerprint>"  # resultFingerprint of that run's result

    review = await client.put_contract_analysis_run_review(
        run_id,
        "hours",
        result_fingerprint=result_fingerprint,
        disposition="corrected",
        corrected_verdict="flagged",
        expected_review_revision=0,
        idempotency_key="review-1",
        reason="Confirmed the omission against the source clause",
    )
    print(f"Review revision: {review.parsed['reviewRevision']}")
    # docs:end


asyncio.run(main())
