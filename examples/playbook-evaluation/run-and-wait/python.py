import asyncio
import os

from docudevs.docudevs_client import DocuDevsClient


async def main():
    client = DocuDevsClient(token=os.environ["API_KEY"])

    # docs:start
    case_id = 42  # id of the playbook's case

    # The client creates the idempotency key; pass idempotency_key to retry a start safely
    started = await client.start_evaluation(case_id, set="tuning")
    evaluation_id = started.parsed["evaluationId"]
    print(f"Evaluation {evaluation_id} started: {len(started.parsed.get('caseIds', []))} cases")

    # Polls every 15 seconds and raises TimeoutError after 3600 seconds
    finished = await client.wait_for_evaluation(
        case_id, evaluation_id, poll_seconds=15, timeout=3600
    )
    print(f"Evaluation {evaluation_id} complete: {finished.parsed.get('trialCounts', {})}")
    # docs:end


asyncio.run(main())
