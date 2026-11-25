import asyncio
import os

from docudevs.docudevs_client import DocuDevsClient


async def main():
    client = DocuDevsClient(token=os.environ["API_KEY"])

    case_id = 42  # id of a records case with an active category tree
    entry_id = "m_88"  # id of a proposed memo entry

    variants = [
        {"name": "baseline"},
        {"name": "with-rule", "memo_include": [entry_id]},
    ]

    started = await client.start_holdout_eval(
        case_id,
        variants,
        sample_size=200,
        seed=7,
        grouping={"mode": "nearDuplicate", "threshold": 0.98},
        config={"llmLevel": "MINI"},
    )

    # Raises EvalFailed when the evaluation fails, and TimeoutError after 3600 seconds
    finished = await client.wait_for_eval(case_id, started["evalId"], timeout=3600)

    for variant in finished["report"].get("variants", []):
        print(variant["name"], variant["score"]["targetAccuracy"].get("all"))


asyncio.run(main())
