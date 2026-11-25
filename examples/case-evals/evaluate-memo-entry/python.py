import asyncio
import os

from docudevs.docudevs_client import DocuDevsClient


async def main():
    client = DocuDevsClient(token=os.environ["API_KEY"])

    # docs:start
    case_id = 42  # id of a records case with an active category tree
    entry_id = "m_88"  # id of a proposed memo entry

    started = await client.evaluate_memo_entry(
        case_id, entry_id, sample_size=200, seed=7, group_field="base_product_id"
    )

    # Raises EvalFailed when the evaluation fails, and TimeoutError after 3600 seconds
    finished = await client.wait_for_eval(case_id, started["evalId"], timeout=3600)

    for pair in finished["report"].get("comparisons", []):
        print(pair["baseline"], pair["candidate"], pair["target"])
    # docs:end


asyncio.run(main())
