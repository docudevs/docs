import asyncio
import os

from docudevs.docudevs_client import DocuDevsClient


async def main():
    client = DocuDevsClient(token=os.environ["API_KEY"])

    # docs:start
    case_id = 42  # id of a records case with an active category tree
    baseline_eval_id = 301  # ids of two completed holdout evaluations
    candidate_eval_id = 302  # that drew the same sample

    # Scoring is asynchronous: the answer carries the id of a new evaluation
    comparison = await client.compare_holdout_evals(
        case_id,
        baseline_eval_id=baseline_eval_id,
        candidate_eval_id=candidate_eval_id,
        baseline_variant="baseline",
        candidate_variant="with-rule",
    )

    # Raises EvalFailed when the scoring fails, and TimeoutError after 3600 seconds
    scored = await client.wait_for_eval(case_id, comparison["evalId"], timeout=3600)

    for pair in scored["report"].get("comparisons", []):
        print(pair["baseline"], pair["candidate"], pair["target"])
    # docs:end


asyncio.run(main())
