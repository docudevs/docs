import asyncio
import os

from docudevs.docudevs_client import DocuDevsClient


async def main():
    client = DocuDevsClient(token=os.environ["API_KEY"])

    # docs:start
    case_id = 42  # id of the playbook's case

    # Both calls raise when the API answers with an error status
    suite = await client.get_contract_analysis_test_suite(case_id)
    saved = await client.split_test_suite(
        case_id,
        holdout=0.3,
        seed=1,
        expected_suite_revision=suite.parsed["revision"],
    )

    cases = saved.parsed.get("cases", [])
    tuning = sum(1 for case in cases if case.get("set") == "tuning")
    holdout = sum(1 for case in cases if case.get("set") == "holdout")
    print(
        f"Suite revision {saved.parsed['revision']} saved: {tuning} tuning, "
        f"{holdout} holdout, {len(cases) - tuning - holdout} unassigned."
    )
    # docs:end


asyncio.run(main())
