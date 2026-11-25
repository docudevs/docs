import asyncio
import os

from docudevs.docudevs_client import DocuDevsClient


async def main():
    client = DocuDevsClient(token=os.environ["API_KEY"])

    # docs:start
    case_id = 42  # id of a records case
    job_id = 9  # jobId returned when the job was started

    # Follows the continuation tokens until every matching row was read
    async for row in client.get_classify_results(case_id, job_id, review_required=True):
        chosen = row.get("chosen") or {}
        print(row["rowId"], row.get("key"), chosen.get("code"), row["review"].get("reasons", []))
    # docs:end


asyncio.run(main())
