import asyncio
import json
import os

from docudevs.docudevs_client import DocuDevsClient


async def main():
    client = DocuDevsClient(token=os.environ["API_KEY"])

    # docs:start
    job_guid = "<job-guid>"  # GUID of a completed extraction job

    # JSON: the stored result as it is, parsed
    result = await client.result_json(job_guid)
    with open("result.json", "w") as f:
        json.dump(result, f, indent=2)

    # CSV: one row per record, nested fields as dotted column names
    csv_text = await client.result_csv(job_guid)
    with open("result.csv", "w") as f:
        f.write(csv_text)

    # Excel: the same table as a workbook; save_to writes the file
    await client.result_excel(job_guid, save_to="result.xlsx")
    # docs:end


asyncio.run(main())
