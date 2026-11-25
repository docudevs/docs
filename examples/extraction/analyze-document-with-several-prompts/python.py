import asyncio
import json
import os

from docudevs.docudevs_client import DocuDevsClient


async def main():
    client = DocuDevsClient(token=os.environ["API_KEY"])

    # docs:start
    with open("report.pdf", "rb") as f:
        document_data = f.read()

    ocr_job_id = await client.submit_and_ocr_document(
        document=document_data,
        document_mime_type="application/pdf",
        ocr="PREMIUM",
        ocr_format="markdown",
    )
    await client.wait_until_ready(ocr_job_id)

    prompts = {
        "document_type": "What type of document is this? (invoice, contract, report, form, etc.)",
        "key_information": "Extract the most important information: dates, names, amounts, addresses, and key terms.",
        "summary": "Provide a concise summary of this document's main content and purpose.",
        "tone": "Analyze the tone of this document. Is it formal, informal, positive, negative, urgent?",
    }

    results = {}
    for name, prompt in prompts.items():
        task = await client.submit_and_wait_for_generative_task(
            parent_job_id=ocr_job_id,
            prompt=prompt,
        )
        results[name] = json.loads(task.result)["generated_text"]

    print(json.dumps(results, indent=2))
    # docs:end


asyncio.run(main())
