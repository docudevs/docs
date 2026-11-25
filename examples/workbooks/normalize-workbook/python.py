import asyncio
import json
import os

from docudevs.docudevs_client import DocuDevsClient


async def main():
    client = DocuDevsClient(token=os.environ["API_KEY"])

    # docs:start
    with open("submission.xlsx", "rb") as f:
        workbook_data = f.read()

    job_id = await client.submit_and_process_document(
        document=workbook_data,
        document_mime_type="application/vnd.openxmlformats-officedocument.spreadsheetml.sheet",
        prompt="Find related tables. Preserve source values, units, row grain, and source references.",
        extraction_mode="WORKBOOK",
    )

    dataset = await client.wait_until_ready(job_id, result_format="json")
    print(json.dumps(dataset, indent=2))

    # The plan, source map and summary are separate artifacts of the same job
    plan = await client.get_workbook_plan(job_id)
    source_map = await client.get_workbook_source_map(job_id)
    summary = await client.get_workbook_summary(job_id)
    print(json.dumps(plan, indent=2))
    print(json.dumps(source_map, indent=2))
    print(json.dumps(summary, indent=2))
    # docs:end


asyncio.run(main())
