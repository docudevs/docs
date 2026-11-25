import asyncio
import os

from docudevs.docudevs_client import DocuDevsClient


async def main():
    client = DocuDevsClient(token=os.environ["API_KEY"])

    # docs:start
    job_guid = "<job-guid>"  # GUID of a job processed with the saved configuration

    runs = await client.list_extraction_review_runs(job_guid)
    for descriptor in runs.parsed:
        print(descriptor["runId"], descriptor["availability"], descriptor["profileRevision"])

    run_id = "<run-id>"  # runId of the descriptor you selected from the list above

    registered = await client.register_extraction_review_run(run_id)
    print(registered.parsed["identity"])
    print(f"Review revision: {registered.parsed['reviewRevision']}")
    # docs:end


asyncio.run(main())
