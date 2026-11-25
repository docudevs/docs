import asyncio
import json
import os
import time

from docudevs.docudevs_client import DocuDevsClient


async def main():
    client = DocuDevsClient(token=os.environ["API_KEY"])

    # docs:start
    job_guid = "<completed-contract-job>"  # jobGuid of a completed contract job
    knowledge_base_id = 42  # id of the case whose profile is published

    await client.submit_operation_with_parameters(
        job_guid,
        "contract-analysis",
        custom_parameters={"knowledgeBaseId": knowledge_base_id},
    )

    deadline = time.monotonic() + 120
    while True:
        operation = await client.get_operation_result(job_guid, "contract-analysis")
        if getattr(operation, "resultAvailable", False):
            break
        if getattr(operation, "status", None) in ("ERROR", "FAILED", "TIMEOUT"):
            raise RuntimeError("Contract analysis failed")
        if time.monotonic() > deadline:
            raise TimeoutError("Contract analysis did not finish in time")
        await asyncio.sleep(2)

    print(json.dumps(json.loads(operation.result), indent=2))
    # docs:end


asyncio.run(main())
