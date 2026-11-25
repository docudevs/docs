import asyncio
import json
import os

from docudevs.docudevs_client import DocuDevsClient


async def main():
    client = DocuDevsClient(token=os.environ["API_KEY"])

    # docs:start
    parent_job_guid = "<job-guid>"  # GUID of the completed PDF job that was converted
    operation_job_guid = "<operation-job-guid>"  # operation job GUID returned by the conversion

    generated_metadata = await client.get_acroform_metadata(operation_job_guid)
    definitions_artifact = await client.get_pdf_acroform_field_definitions(operation_job_guid)

    print(json.dumps(generated_metadata.get("fields", [])[:3], indent=2))
    print(json.dumps(definitions_artifact.get("fieldDefinitions", [])[:3], indent=2))

    # Mark every generated field as reviewed and accepted
    reviewed_fields = [dict(field) for field in definitions_artifact.get("fieldDefinitions", [])]
    for field in reviewed_fields:
        field["reviewStatus"] = "accepted"

    applied = await client.submit_and_wait_for_pdf_acroform_apply_operation(
        parent_job_guid,
        field_definitions=reviewed_fields,
        source_operation_guid=operation_job_guid,
        save_to="reviewed.pdf",
    )

    print(f"Operation job GUID: {applied.operation_job_guid}")
    # docs:end


asyncio.run(main())
