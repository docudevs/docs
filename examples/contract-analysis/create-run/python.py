import asyncio
import os

from docudevs.docudevs_client import DocuDevsClient


async def main():
    client = DocuDevsClient(token=os.environ["DOCUDEVS_API_KEY"])

    # docs:start
    created = await client.create_contract_analysis_run(
        "<agreement-job>",
        knowledge_base_id=42,
        idempotency_key="request-1",
        mode="published",
        package_members=[
            {"jobGuid": "<agreement-job>", "role": "agreement"},
            {"jobGuid": "<amendment-job>", "role": "amendment"},
        ],
    )
    print(f"Run ID: {created.parsed['runId']}")
    # docs:end


asyncio.run(main())
