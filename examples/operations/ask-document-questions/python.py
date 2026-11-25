import asyncio
import json
import os

from docudevs.docudevs_client import DocuDevsClient


async def main():
    client = DocuDevsClient(token=os.environ["API_KEY"])

    # docs:start
    parent_job_guid = "<job-guid>"  # GUID of a completed document job

    questions = [
        "What is the contract duration?",
        "Who are the parties involved?",
        "What are the key obligations?",
    ]

    for question in questions:
        answer = await client.submit_and_wait_for_generative_task(
            parent_job_id=parent_job_guid,
            prompt=f"Based on this document, answer the following question: {question}",
        )

        result_data = json.loads(answer.result)
        print(f"Q: {question}")
        print(f"A: {result_data['generated_text']}\n")
    # docs:end


asyncio.run(main())
