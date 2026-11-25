import asyncio
import os

from docudevs.docudevs_client import DocuDevsClient


async def main():
    client = DocuDevsClient(token=os.environ["API_KEY"])

    case_id = 42  # id returned by create_case
    classify_job_id = 55  # id of a completed classify job of this case

    started = await client.start_agent_run(
        case_id,
        "investigate-flagged",
        params={"job_id": classify_job_id, "max_rows": 10},
        budget={"max_total_tokens": 60000},
    )

    # Raises AgentRunFailed when the run fails or is cancelled, and TimeoutError after 1800 seconds
    run = await client.wait_for_agent_run(
        case_id,
        started["runId"],
        timeout=1800,
        on_progress=lambda status: print(status["status"], status["turnsDone"]),
    )

    report = run.get("report") or {}
    print(report.get("summary"))
    for question in report.get("questions") or []:
        print("question:", question)


asyncio.run(main())
