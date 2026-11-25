import asyncio
import json
import os

from docudevs.docudevs_client import DocuDevsClient


async def main():
    client = DocuDevsClient(token=os.environ["API_KEY"])

    # docs:start
    with open("fillable-form.pdf", "rb") as f:
        pdf_bytes = f.read()

    job_guid = await client.submit_and_process_document(
        document=pdf_bytes,
        document_mime_type="application/pdf",
        acro_form_metadata=True,
        source_locations=True,
        source_location_granularity="block",
    )
    await client.wait_until_ready(job_guid, result_format="json")

    job_metadata = await client.get_acroform_metadata(job_guid)
    source_locations = await client.get_source_locations(job_guid)

    if job_metadata is None:
        raise SystemExit("No AcroForm metadata found for this job")
    if source_locations is None:
        raise SystemExit("No source locations found for this job")

    print(f"Async metadata fields: {len(job_metadata.get('fields', []))}")
    print(json.dumps(source_locations, indent=2))
    # docs:end


asyncio.run(main())
