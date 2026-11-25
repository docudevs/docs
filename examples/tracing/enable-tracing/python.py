import asyncio
import os

from docudevs.docudevs_client import DocuDevsClient


async def main():
    client = DocuDevsClient(token=os.environ["API_KEY"])

    with open("invoice.pdf", "rb") as f:
        document_data = f.read()

    # Enable tracing when processing a document
    job_guid = await client.submit_and_process_document(
        document=document_data,
        document_mime_type="application/pdf",
        prompt="Extract invoice data",
        trace=True,
    )

    # Wait for processing to complete
    await client.wait_until_ready(job_guid)

    # Retrieve the trace
    trace = await client.get_trace(job_guid)
    if trace:
        print(f"Total tokens used: {trace['total_tokens']}")
        print(f"LLM calls: {trace['total_llm_calls']}")


asyncio.run(main())
