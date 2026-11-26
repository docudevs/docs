import asyncio
import os

from docudevs.docudevs_client import DocuDevsClient


async def main():
    client = DocuDevsClient(token=os.environ["API_KEY"])

    # docs:start
    case_id = 42  # id of the playbook's case

    listing = await client.list_evaluations(case_id)
    for evaluation in listing.parsed.get("evaluations", []):
        print(evaluation["evaluationId"], evaluation["set"], "complete" if evaluation["complete"] else "running")

    evaluation_id = "<evaluation-id>"  # evaluationId returned when the evaluation was started
    status = await client.get_evaluation(case_id, evaluation_id)
    print(f"Trials: {status.parsed.get('trialCounts', {})}")

    # Pass against=<evaluation id> or against="published" to compare with another evaluation
    response = await client.get_evaluation_report(case_id, evaluation_id)
    report = response.parsed
    totals = report["totals"]
    print(f"Complete: {report['complete']}")
    print(f"Passed {totals['passed']} of {totals['assertions']} assertions")
    print(f"Missed problems: {totals.get('missedProblems', 0)}, false alarms: {totals.get('falseAlarms', 0)}")
    for contract in report.get("contracts", []):
        if contract["trialStatus"] != "finished":
            print(f"{contract['name']}: {contract['trialStatus']} ({contract.get('failureReason')})")
    # docs:end


asyncio.run(main())
